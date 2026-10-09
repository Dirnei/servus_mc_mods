package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Offset;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Placement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forms and unforms the Calciner structure in the world, following {@link CalcinerLayout}.
 * Server thread only. Nothing here scans: forming is a click and unforming follows a removal.
 */
public final class CalcinerStructure {

    /** Set while this class swaps blocks, so the removals it causes do not unform again. */
    private static boolean changing;

    /** The result of checking the structure in its best facing. */
    public record Check(Facing facing, Optional<Cell> firstWrong) {

        public boolean complete() {
            return firstWrong.isEmpty();
        }
    }

    private CalcinerStructure() {
    }

    public static Facing facing(Direction direction) {
        return switch (direction) {
            case EAST -> Facing.EAST;
            case SOUTH -> Facing.SOUTH;
            case WEST -> Facing.WEST;
            default -> Facing.NORTH;
        };
    }

    public static Direction direction(Facing facing) {
        return switch (facing) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
        };
    }

    /** The world position of {@code cell} of the Calciner at {@code controller}. */
    public static BlockPos pos(BlockPos controller, Cell cell, Facing facing) {
        Offset offset = CalcinerLayout.worldOffset(cell, facing);
        return controller.offset(offset.x(), offset.y(), offset.z());
    }

    /** The block a cell needs while unformed. */
    public static Block expected(Role role) {
        return switch (role) {
            case CALCINER -> ServusManufacturingBlocks.CALCINER.get();
            case BRICK -> Blocks.BRICKS;
            case FIREBOX -> ServusManufacturingBlocks.FIREBOX.get();
            case ASH_PAN -> ServusManufacturingBlocks.ASH_PAN.get();
            case ALEMBIC -> ServusManufacturingBlocks.ALEMBIC.get();
            case RECEIVER -> ServusManufacturingBlocks.RECEIVER.get();
            case HOOD -> ServusManufacturingBlocks.SULFUR_HOOD.get();
        };
    }

    /** Checks every facing, the Calciner's own first, and returns a complete one or the one that matched most. */
    public static Check check(Level level, BlockPos controller) {
        List<Facing> order = new ArrayList<>(List.of(Facing.values()));
        Facing own = facing(level.getBlockState(controller).getValue(MachineBlock.FACING));
        order.remove(own);
        order.addFirst(own);
        Check best = null;
        int bestMatches = -1;
        for (Facing facing : order) {
            Optional<Cell> firstWrong = Optional.empty();
            int matches = 0;
            for (Cell cell : CalcinerLayout.cells()) {
                if (matches(level.getBlockState(pos(controller, cell, facing)), cell)) {
                    matches++;
                } else if (firstWrong.isEmpty()) {
                    firstWrong = Optional.of(cell);
                }
            }
            if (firstWrong.isEmpty()) {
                return new Check(facing, firstWrong);
            }
            if (matches > bestMatches) {
                bestMatches = matches;
                best = new Check(facing, firstWrong);
            }
        }
        return best;
    }

    private static boolean matches(BlockState state, Cell cell) {
        if (!state.is(expected(cell.role()))) {
            return false;
        }
        if (cell.role() == Role.CALCINER) {
            return true;
        }
        return cell.role() == Role.BRICK || !state.getValue(CalcinerPartBlock.FORMED);
    }

    /**
     * The Journal's click on the Calciner: forms the structure when it is complete and makes the
     * player its owner, otherwise names the first block that is missing.
     */
    public static boolean tryForm(Level level, BlockPos controller, Player player) {
        if (level.getBlockState(controller).getValue(CalcinerBlock.FORMED)) {
            return true;
        }
        Check check = check(level, controller);
        if (!check.complete()) {
            Cell wrong = check.firstWrong().orElseThrow();
            player.displayClientMessage(Component.translatable("message.servus_manufacturing.calciner.missing",
                    expected(wrong.role()).getName(), where(wrong)), false);
            return false;
        }
        form(level, controller, check.facing());
        if (level.getBlockEntity(controller) instanceof CalcinerBlockEntity calciner) {
            calciner.setOwner(player.getUUID());
        }
        player.displayClientMessage(Component.translatable("message.servus_manufacturing.calciner.formed"), true);
        return true;
    }

    /** "right of the Alembic", for the message when forming fails. */
    private static Component where(Cell cell) {
        Placement placement = CalcinerLayout.placement(cell);
        return Component.translatable("message.servus_manufacturing.calciner." + placement.relation().name().toLowerCase(Locale.ROOT),
                expected(placement.anchor()).getName());
    }

    /** Forms the checked structure: bricks become shells, parts and the Calciner turn formed and face {@code facing}. */
    public static void form(Level level, BlockPos controller, Facing facing) {
        Direction direction = direction(facing);
        changing = true;
        try {
            for (Cell cell : CalcinerLayout.cells()) {
                BlockPos pos = pos(controller, cell, facing);
                BlockState state = level.getBlockState(pos);
                BlockState formed = switch (cell.role()) {
                    case CALCINER -> state.setValue(MachineBlock.FACING, direction).setValue(CalcinerBlock.FORMED, true);
                    case BRICK -> ServusManufacturingBlocks.CALCINER_SHELL.get().defaultBlockState()
                            .setValue(CalcinerShellBlock.FACING, direction).setValue(CalcinerShellBlock.CELL, cell.index());
                    default -> state.setValue(CalcinerPartBlock.FACING, direction).setValue(CalcinerPartBlock.FORMED, true);
                };
                level.setBlock(pos, formed, Block.UPDATE_ALL);
                // The ports are capabilities by block state; a changed property alone keeps stale caches.
                level.invalidateCapabilities(pos);
            }
        } finally {
            changing = false;
        }
    }

    /**
     * Unforms the Calciner at {@code controller}: shells become Bricks, parts and the Calciner turn
     * unformed. The position {@code skip} is being broken and is left alone.
     */
    public static void unform(Level level, BlockPos controller, Direction direction, BlockPos skip) {
        if (changing) {
            return;
        }
        Facing facing = facing(direction);
        changing = true;
        try {
            for (Cell cell : CalcinerLayout.cells()) {
                BlockPos pos = pos(controller, cell, facing);
                if (pos.equals(skip)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                BlockState unformed = null;
                if (cell.role() == Role.CALCINER) {
                    if (state.getBlock() instanceof CalcinerBlock && state.getValue(CalcinerBlock.FORMED)) {
                        unformed = state.setValue(CalcinerBlock.FORMED, false).setValue(MachineBlock.LIT, false);
                    }
                } else if (cell.role() == Role.BRICK) {
                    if (state.getBlock() instanceof CalcinerShellBlock) {
                        unformed = Blocks.BRICKS.defaultBlockState();
                    }
                } else if (state.getBlock() instanceof CalcinerPartBlock && state.getValue(CalcinerPartBlock.FORMED)) {
                    unformed = state.setValue(CalcinerPartBlock.FORMED, false).setValue(CalcinerPartBlock.LIT, false);
                }
                if (unformed != null) {
                    level.setBlock(pos, unformed, Block.UPDATE_ALL);
                    level.invalidateCapabilities(pos);
                }
            }
        } finally {
            changing = false;
        }
    }

    /** True while forming or unforming swaps blocks. */
    public static boolean changing() {
        return changing;
    }

    /** The Calciner position of a formed part at {@code pos}, from its cell and facing. */
    public static BlockPos controller(BlockPos pos, Cell cell, Direction direction) {
        Offset offset = CalcinerLayout.controllerOffset(cell, facing(direction));
        return pos.offset(offset.x(), offset.y(), offset.z());
    }

    /** The Calciner position of a formed part or shell, or empty when {@code state} is not a formed part of it. */
    public static Optional<BlockPos> controllerOf(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof CalcinerShellBlock) {
            return Optional.of(controller(pos, CalcinerLayout.cell(state.getValue(CalcinerShellBlock.CELL)), state.getValue(CalcinerShellBlock.FACING)));
        }
        if (state.getBlock() instanceof CalcinerPartBlock part && state.getValue(CalcinerPartBlock.FORMED)) {
            return Optional.of(controller(pos, CalcinerLayout.part(part.role()), state.getValue(CalcinerPartBlock.FACING)));
        }
        if (state.getBlock() instanceof CalcinerBlock && state.getValue(CalcinerBlock.FORMED)) {
            return Optional.of(pos);
        }
        return Optional.empty();
    }

    /**
     * Called when a formed block at {@code pos} is replaced by another block: unforms the rest of
     * the structure. The Calciner block entity must already be gone or be unformed by this.
     */
    public static void broken(Level level, BlockState state, BlockPos pos) {
        if (changing || level.isClientSide()) {
            return;
        }
        controllerOf(state, pos).ifPresent(controller -> {
            Direction direction = state.getBlock() instanceof CalcinerShellBlock ? state.getValue(CalcinerShellBlock.FACING)
                    : state.getBlock() instanceof CalcinerPartBlock ? state.getValue(CalcinerPartBlock.FACING)
                    : state.getValue(MachineBlock.FACING);
            unform(level, controller, direction, pos);
        });
    }
}
