package io.github.dirnei.mc.servus.alchemy.coagulator;

import io.github.dirnei.mc.servus.alchemy.CoagulatorBlock;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorLayout.Cell;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorLayout.Role;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.alchemy.structure.FormingEffect;
import io.github.dirnei.mc.servus.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.alchemy.structure.Structures.Check;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forms and unforms the Coagulator structure following {@link CoagulatorLayout}. The Stone Bricks
 * stay vanilla blocks and are checked by their formed neighbours. Server thread only.
 */
public final class CoagulatorStructure {

    private static boolean changing;

    private CoagulatorStructure() {
    }

    public static BlockPos pos(BlockPos controller, Cell cell, Facing facing) {
        return Structures.pos(controller, cell.right(), cell.up(), facing);
    }

    public static Block expected(Role role) {
        return switch (role) {
            case COAGULATOR -> ServusAlchemyBlocks.COAGULATOR.get();
            case STONE -> Blocks.STONE_BRICKS;
            case VAT -> ServusAlchemyBlocks.VAT.get();
        };
    }

    private static boolean matches(BlockState state, Cell cell) {
        if (!state.is(expected(cell.role()))) {
            return false;
        }
        return cell.role() != Role.VAT || !state.getValue(VatBlock.FORMED);
    }

    /** Checks every facing, the Coagulator's own first, and returns a complete one or the one that matched most. */
    public static Check<Cell> check(Level level, BlockPos controller) {
        return Structures.check(level.getBlockState(controller).getValue(MachineBlock.FACING), CoagulatorLayout.cells(),
                (facing, cell) -> matches(level.getBlockState(pos(controller, cell, facing)), cell));
    }

    /** The Journal's click: forms a complete structure and makes the player its owner, else names what is missing. */
    public static boolean tryForm(Level level, BlockPos controller, Player player) {
        if (level.getBlockState(controller).getValue(CoagulatorBlock.FORMED)) {
            return true;
        }
        Check<Cell> check = check(level, controller);
        if (!check.complete()) {
            Cell wrong = check.firstWrong().orElseThrow();
            player.sendSystemMessage(Component.translatable("message.servus_manufacturing.coagulator.missing",
                    expected(wrong.role()).getName(), Component.translatable("message.servus_manufacturing.coagulator.at."
                            + CoagulatorLayout.placement(wrong).replace(' ', '_'))));
            return false;
        }
        form(level, controller, check.facing());
        FormingEffect.play(level, controller, CoagulatorLayout.cells().stream().map(cell -> pos(controller, cell, check.facing())).toList());
        if (level.getBlockEntity(controller) instanceof CoagulatorBlockEntity coagulator) {
            coagulator.setOwner(player.getUUID());
        }
        player.sendOverlayMessage(Component.translatable("message.servus_manufacturing.coagulator.formed"));
        return true;
    }

    public static void form(Level level, BlockPos controller, Facing facing) {
        Direction direction = Structures.direction(facing);
        changing = true;
        try {
            for (Cell cell : CoagulatorLayout.cells()) {
                BlockPos pos = pos(controller, cell, facing);
                BlockState state = level.getBlockState(pos);
                BlockState formed = switch (cell.role()) {
                    case COAGULATOR -> state.setValue(MachineBlock.FACING, direction).setValue(CoagulatorBlock.FORMED, true);
                    case VAT -> state.setValue(VatBlock.FACING, direction).setValue(VatBlock.FORMED, true)
                            .setValue(VatBlock.PIECE, cell.index() - CoagulatorLayout.FIRST_VAT);
                    case STONE -> null;
                };
                if (formed != null) {
                    level.setBlock(pos, formed, Block.UPDATE_ALL);
                    level.invalidateCapabilities(pos);
                }
            }
        } finally {
            changing = false;
        }
    }

    /** Unforms the Coagulator at {@code controller}; {@code skip} is being broken and is left alone. */
    public static void unform(Level level, BlockPos controller, Direction direction, BlockPos skip) {
        if (changing) {
            return;
        }
        Facing facing = Structures.facing(direction);
        changing = true;
        try {
            for (Cell cell : CoagulatorLayout.cells()) {
                BlockPos pos = pos(controller, cell, facing);
                if (pos.equals(skip)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                BlockState unformed = null;
                if (cell.role() == Role.COAGULATOR && state.getBlock() instanceof CoagulatorBlock && state.getValue(CoagulatorBlock.FORMED)) {
                    unformed = state.setValue(CoagulatorBlock.FORMED, false).setValue(MachineBlock.LIT, false);
                } else if (cell.role() == Role.VAT && state.getBlock() instanceof VatBlock && state.getValue(VatBlock.FORMED)) {
                    unformed = state.setValue(VatBlock.FORMED, false).setValue(VatBlock.PIECE, 0);
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

    /** The Coagulator position of a formed Vat or Coagulator, or empty. */
    public static Optional<BlockPos> controllerOf(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof VatBlock && state.getValue(VatBlock.FORMED)) {
            Cell cell = CoagulatorLayout.vat(state.getValue(VatBlock.PIECE));
            return Optional.of(Structures.pos(pos, CoagulatorLayout.controllerOffset(cell, Structures.facing(state.getValue(VatBlock.FACING)))));
        }
        if (state.getBlock() instanceof CoagulatorBlock && state.getValue(CoagulatorBlock.FORMED)) {
            return Optional.of(pos);
        }
        return Optional.empty();
    }

    private static Direction facingOf(BlockState state) {
        return state.getBlock() instanceof VatBlock ? state.getValue(VatBlock.FACING) : state.getValue(MachineBlock.FACING);
    }

    /** A formed block is replaced: unform the rest. */
    public static void broken(Level level, BlockState state, BlockPos pos) {
        if (changing || level.isClientSide()) {
            return;
        }
        controllerOf(state, pos).ifPresent(controller -> unform(level, controller, facingOf(state), pos));
    }

    /** A neighbour of a formed block changed: the vanilla Stone Bricks have no hook of their own, so check them. */
    public static void neighbourChanged(Level level, BlockState state, BlockPos pos) {
        if (changing || level.isClientSide()) {
            return;
        }
        controllerOf(state, pos).ifPresent(controller -> {
            Direction direction = facingOf(state);
            Facing facing = Structures.facing(direction);
            for (Cell cell : CoagulatorLayout.cells()) {
                if (cell.role() == Role.STONE && !matches(level.getBlockState(pos(controller, cell, facing)), cell)) {
                    unform(level, controller, direction, null);
                    return;
                }
            }
        });
    }

    /** The fill only tank view of a formed Vat: its principle into the Coagulator. */
    public static LegacyFluidHandler vatHandler(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof VatBlock) || !state.getValue(VatBlock.FORMED)) {
            return null;
        }
        return controllerOf(state, pos)
                .map(controller -> level.getBlockEntity(controller) instanceof CoagulatorBlockEntity coagulator ? coagulator : null)
                .map(coagulator -> coagulator.tanks().view(CoagulatorLayout.principle(CoagulatorLayout.vat(state.getValue(VatBlock.PIECE)))))
                .orElse(null);
    }
}
