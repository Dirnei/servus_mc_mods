package io.github.dirnei.mc.servus.alchemy.venter;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.alchemy.structure.FormingEffect;
import io.github.dirnei.mc.servus.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.alchemy.venter.VenterFrameBlock.FramePiece;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Cell;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Piece;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Role;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forms and unforms the Venter Equinus following {@link VenterLayout}: the Cucurbit in the middle of a
 * 3 by 3 bed, the Bed Hatch in the middle of the front and Dung Beds on the other cells. The hatch
 * decides the front, so the bed is checked in all four facings. Server thread only, except the
 * position lookups, which only read block states.
 */
public final class VenterStructure {

    private static boolean changing;

    private VenterStructure() {
    }

    public static BlockPos pos(BlockPos cucurbit, Cell cell, Facing front) {
        return Structures.groundPos(cucurbit, cell.right(), cell.forward(), front);
    }

    /** The block a cell shows in the message and the schematic. */
    public static Block shown(Role role) {
        return switch (role) {
            case CUCURBIT -> ServusAlchemyBlocks.CUCURBIT.get();
            case HATCH -> ServusAlchemyBlocks.BED_HATCH.get();
            case BED -> ServusAlchemyBlocks.DUNG_BED.get();
        };
    }

    private static boolean matches(BlockState state, Cell cell) {
        if (!state.is(shown(cell.role()))) {
            return false;
        }
        return cell.role() == Role.CUCURBIT ? !state.getValue(CucurbitBlock.FORMED) : !state.getValue(VenterFrameBlock.FORMED);
    }

    /** Every position of the bed for a front facing {@code front}. */
    public static List<BlockPos> cells(BlockPos cucurbit, Facing front) {
        return VenterLayout.cells().stream().map(cell -> pos(cucurbit, cell, front)).toList();
    }

    /** The Journal's click on the Cucurbit: forms along the first complete facing, else names what is missing. */
    public static boolean tryForm(Level level, BlockPos cucurbit, Player player) {
        BlockState state = level.getBlockState(cucurbit);
        if (state.getValue(CucurbitBlock.FORMED)) {
            return true;
        }
        Structures.Check<Cell> check = Structures.check(state.getValue(CucurbitBlock.FACING), VenterLayout.cells(),
                (facing, cell) -> matches(level.getBlockState(pos(cucurbit, cell, facing)), cell));
        if (check.complete()) {
            form(level, cucurbit, check.facing());
            FormingEffect.play(level, cucurbit, cells(cucurbit, check.facing()));
            if (level.getBlockEntity(cucurbit) instanceof VenterEquinusBlockEntity venter) {
                venter.setOwner(player.getUUID());
            }
            player.sendOverlayMessage(Component.translatable("message.servus_alchemy.venter_equinus.formed"));
            return true;
        }
        Cell wrong = check.firstWrong().orElseThrow();
        player.sendSystemMessage(Component.translatable("message.servus_alchemy.venter_equinus.missing", shown(wrong.role()).getName(),
                Component.translatable(placementKey(wrong))));
        return false;
    }

    /** The language key of where {@code cell} sits. */
    public static String placementKey(Cell cell) {
        return "message.servus_alchemy.venter_equinus.at." + VenterLayout.placement(cell).replace(' ', '_');
    }

    public static void form(Level level, BlockPos cucurbit, Facing front) {
        changing = true;
        try {
            level.setBlock(cucurbit, level.getBlockState(cucurbit).setValue(CucurbitBlock.FORMED, true)
                    .setValue(CucurbitBlock.FACING, Structures.direction(front)), Block.UPDATE_ALL);
            level.invalidateCapabilities(cucurbit);
            for (Cell cell : VenterLayout.cells()) {
                if (cell.piece() == Piece.NONE) {
                    continue;
                }
                BlockPos pos = pos(cucurbit, cell, front);
                level.setBlock(pos, level.getBlockState(pos).setValue(VenterFrameBlock.FORMED, true)
                        .setValue(VenterFrameBlock.FACING, Structures.direction(VenterLayout.pieceFacing(cell, front)))
                        .setValue(VenterFrameBlock.PIECE, cell.piece() == Piece.CORNER ? FramePiece.CORNER : FramePiece.EDGE), Block.UPDATE_ALL);
                level.invalidateCapabilities(pos);
            }
        } finally {
            changing = false;
        }
    }

    /** Unforms the Venter Equinus whose Cucurbit is at {@code cucurbit}; {@code skip} is being broken and is left alone. */
    public static void unform(Level level, BlockPos cucurbit, BlockPos skip) {
        if (changing) {
            return;
        }
        changing = true;
        try {
            BlockState cucurbitState = level.getBlockState(cucurbit);
            if (!cucurbit.equals(skip) && cucurbitState.getBlock() instanceof CucurbitBlock && cucurbitState.getValue(CucurbitBlock.FORMED)) {
                level.setBlock(cucurbit, cucurbitState.setValue(CucurbitBlock.FORMED, false).setValue(CucurbitBlock.WORKING, false), Block.UPDATE_ALL);
                level.invalidateCapabilities(cucurbit);
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = cucurbit.offset(dx, 0, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!pos.equals(skip) && state.getBlock() instanceof VenterFrameBlock && state.getValue(VenterFrameBlock.FORMED)
                            && VenterFrameBlock.cucurbit(pos, state).equals(cucurbit)) {
                        level.setBlock(pos, state.setValue(VenterFrameBlock.FORMED, false), Block.UPDATE_ALL);
                        level.invalidateCapabilities(pos);
                    }
                }
            }
        } finally {
            changing = false;
        }
    }

    /** The Cucurbit of a formed Cucurbit, Dung Bed or Bed Hatch, or empty. */
    public static Optional<BlockPos> cucurbitOf(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof CucurbitBlock && state.getValue(CucurbitBlock.FORMED)) {
            return Optional.of(pos);
        }
        if (state.getBlock() instanceof VenterFrameBlock && state.getValue(VenterFrameBlock.FORMED)) {
            return Optional.of(VenterFrameBlock.cucurbit(pos, state));
        }
        return Optional.empty();
    }

    /** A formed block of the bed is replaced: unform the rest. */
    public static void broken(Level level, BlockState state, BlockPos pos) {
        if (changing || level.isClientSide()) {
            return;
        }
        cucurbitOf(state, pos).ifPresent(cucurbit -> unform(level, cucurbit, pos));
    }

    /** The block entity of the formed Venter Equinus that the formed block at {@code pos} belongs to. */
    public static Optional<VenterEquinusBlockEntity> formedCucurbit(Level level, BlockPos pos, BlockState state) {
        return cucurbitOf(state, pos)
                .map(level::getBlockEntity)
                .filter(VenterEquinusBlockEntity.class::isInstance)
                .map(VenterEquinusBlockEntity.class::cast)
                .filter(VenterEquinusBlockEntity::formed);
    }

    /** The bedding insert only view of a formed Dung Bed. */
    public static LegacyItemHandler bedHandler(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof DungBedBlock
                ? formedCucurbit(level, pos, state).map(VenterEquinusBlockEntity::beddingPort).orElse(null) : null;
    }

    /** The extract only view of a formed Bed Hatch. */
    public static LegacyItemHandler hatchHandler(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof BedHatchBlock
                ? formedCucurbit(level, pos, state).map(VenterEquinusBlockEntity::hatchPort).orElse(null) : null;
    }
}
