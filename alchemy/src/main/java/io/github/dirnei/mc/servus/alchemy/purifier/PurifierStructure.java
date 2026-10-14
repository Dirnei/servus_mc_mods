package io.github.dirnei.mc.servus.alchemy.purifier;

import io.github.dirnei.mc.servus.alchemy.purifier.PurifierLayout.Cell;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierLayout.Role;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.alchemy.structure.FormingEffect;
import io.github.dirnei.mc.servus.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forms and unforms the Purifier following {@link PurifierLayout}: the Leaching Vat on a log, the
 * Catch Basin and the Residue Crock beside the log. Checking all four facings also
 * accepts the mirrored build. The Vat keeps the facing; the Basin and the Crock face the log. The log
 * stays vanilla and is checked by its formed neighbours. Server thread only, except
 * the position lookups, which only read block states.
 */
public final class PurifierStructure {

    private static boolean changing;

    private PurifierStructure() {
    }

    public static BlockPos pos(BlockPos vat, Cell cell, Facing facing) {
        return Structures.pos(vat, cell.right(), cell.up(), facing);
    }

    /** The block a cell shows in the message and the schematic. */
    public static Block shown(Role role) {
        return switch (role) {
            case VAT -> ServusAlchemyBlocks.PURIFIER.get();
            case BASIN -> ServusAlchemyBlocks.CATCH_BASIN.get();
            case CROCK -> ServusAlchemyBlocks.RESIDUE_CROCK.get();
            case LOG -> Blocks.OAK_LOG;
        };
    }

    private static boolean matches(BlockState state, Cell cell, boolean formed) {
        return switch (cell.role()) {
            case VAT -> state.is(ServusAlchemyBlocks.PURIFIER.get()) && state.getValue(PurifierBlock.FORMED) == formed;
            case BASIN -> state.is(ServusAlchemyBlocks.CATCH_BASIN.get()) && state.getValue(PurifierSideBlock.FORMED) == formed;
            case CROCK -> state.is(ServusAlchemyBlocks.RESIDUE_CROCK.get()) && state.getValue(PurifierSideBlock.FORMED) == formed;
            case LOG -> state.is(BlockTags.LOGS);
        };
    }

    /** The first wrong cell along {@code facing}, or empty when the build is complete there. */
    private static Optional<Cell> firstWrong(Level level, BlockPos vat, Facing facing, boolean formed) {
        return PurifierLayout.cells().stream().filter(cell -> !matches(level.getBlockState(pos(vat, cell, facing)), cell, formed)).findFirst();
    }

    /** The Journal's click on the Vat: forms along the first facing that is complete, else names what is missing. */
    public static boolean tryForm(Level level, BlockPos vat, Player player) {
        if (level.getBlockState(vat).getValue(PurifierBlock.FORMED)) {
            return true;
        }
        Structures.Check<Cell> check = Structures.check(PurifierLayout.FACINGS, PurifierLayout.cells(),
                (facing, cell) -> matches(level.getBlockState(pos(vat, cell, facing)), cell, false));
        if (check.complete()) {
            form(level, vat, check.facing());
            FormingEffect.play(level, vat, cells(vat, check.facing()));
            if (level.getBlockEntity(vat) instanceof PurifierBlockEntity purifier) {
                purifier.setOwner(player.getUUID());
            }
            player.sendOverlayMessage(Component.translatable("message.servus_alchemy.purifier.formed"));
            return true;
        }
        Cell wrong = check.firstWrong().orElseThrow();
        player.sendSystemMessage(Component.translatable("message.servus_alchemy.purifier.missing", shown(wrong.role()).getName(),
                Component.translatable("message.servus_alchemy.purifier.at." + PurifierLayout.placement(wrong).replace(' ', '_'))));
        return false;
    }

    /** Every position of the build along {@code facing}. */
    public static List<BlockPos> cells(BlockPos vat, Facing facing) {
        return PurifierLayout.cells().stream().map(cell -> pos(vat, cell, facing)).toList();
    }

    public static void form(Level level, BlockPos vat, Facing facing) {
        changing = true;
        try {
            Direction front = Structures.direction(facing);
            Direction right = front.getCounterClockWise();
            level.setBlock(vat, level.getBlockState(vat).setValue(PurifierBlock.FORMED, true).setValue(PurifierBlock.FACING, front), Block.UPDATE_ALL);
            // The Basin stands to the left of the log, so it faces right; the Crock faces left.
            formSide(level, pos(vat, PurifierLayout.cell(Role.BASIN), facing), right);
            formSide(level, pos(vat, PurifierLayout.cell(Role.CROCK), facing), right.getOpposite());
            level.invalidateCapabilities(vat);
        } finally {
            changing = false;
        }
    }

    private static void formSide(Level level, BlockPos pos, Direction towardLog) {
        level.setBlock(pos, level.getBlockState(pos).setValue(PurifierSideBlock.FORMED, true).setValue(PurifierSideBlock.FACING, towardLog),
                Block.UPDATE_ALL);
        level.invalidateCapabilities(pos);
    }

    /** Unforms the Purifier whose Vat is at {@code vat}; {@code skip} is being broken and is left alone. */
    public static void unform(Level level, BlockPos vat, BlockPos skip) {
        if (changing) {
            return;
        }
        changing = true;
        try {
            BlockState vatState = level.getBlockState(vat);
            if (!vat.equals(skip) && vatState.getBlock() instanceof PurifierBlock && vatState.getValue(PurifierBlock.FORMED)) {
                level.setBlock(vat, vatState.setValue(PurifierBlock.FORMED, false), Block.UPDATE_ALL);
                level.invalidateCapabilities(vat);
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos pos = vat.below().relative(side);
                BlockState state = level.getBlockState(pos);
                if (!pos.equals(skip) && state.getBlock() instanceof PurifierSideBlock && state.getValue(PurifierSideBlock.FORMED)
                        && PurifierSideBlock.vat(pos, state).equals(vat)) {
                    level.setBlock(pos, state.setValue(PurifierSideBlock.FORMED, false), Block.UPDATE_ALL);
                    level.invalidateCapabilities(pos);
                }
            }
        } finally {
            changing = false;
        }
    }

    /** The Vat position of a formed Vat, Basin or Crock, or empty. */
    public static Optional<BlockPos> vatOf(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof PurifierBlock && state.getValue(PurifierBlock.FORMED)) {
            return Optional.of(pos);
        }
        if (state.getBlock() instanceof PurifierSideBlock && state.getValue(PurifierSideBlock.FORMED)) {
            return Optional.of(PurifierSideBlock.vat(pos, state));
        }
        return Optional.empty();
    }

    /** Where the block of {@code role} of the formed Purifier at {@code vat} stands. Reads only block states. */
    public static BlockPos partPos(BlockGetter level, BlockPos vat, Role role) {
        Facing facing = Structures.facing(level.getBlockState(vat).getValue(PurifierBlock.FACING));
        return pos(vat, PurifierLayout.cell(role), facing);
    }

    /** A formed Vat, Basin or Crock is replaced: unform the rest. */
    public static void broken(Level level, BlockState state, BlockPos pos) {
        if (changing || level.isClientSide()) {
            return;
        }
        vatOf(state, pos).ifPresent(vat -> unform(level, vat, pos));
    }

    /** A neighbour of a formed block changed: the log has no hook of its own, so check the whole build. */
    public static void neighbourChanged(Level level, BlockState state, BlockPos pos) {
        if (changing || level.isClientSide()) {
            return;
        }
        vatOf(state, pos).ifPresent(vat -> {
            BlockState vatState = level.getBlockState(vat);
            if (!(vatState.getBlock() instanceof PurifierBlock) || !vatState.getValue(PurifierBlock.FORMED)
                    || firstWrong(level, vat, Structures.facing(vatState.getValue(PurifierBlock.FACING)), true).isPresent()) {
                unform(level, vat, null);
            }
        });
    }

    private static Optional<PurifierBlockEntity> formedVat(Level level, BlockPos pos, BlockState state, Class<? extends PurifierSideBlock> type) {
        if (!type.isInstance(state.getBlock()) || !state.getValue(PurifierSideBlock.FORMED)) {
            return Optional.empty();
        }
        return level.getBlockEntity(PurifierSideBlock.vat(pos, state)) instanceof PurifierBlockEntity purifier && purifier.formed()
                ? Optional.of(purifier) : Optional.empty();
    }

    /** The Salt drain only view of a formed Basin. */
    public static LegacyFluidHandler basinHandler(Level level, BlockPos pos, BlockState state) {
        return formedVat(level, pos, state, CatchBasinBlock.class).map(PurifierBlockEntity::saltPort).orElse(null);
    }

    /** The residue extract only view of a formed Crock. */
    public static LegacyItemHandler crockHandler(Level level, BlockPos pos, BlockState state) {
        return formedVat(level, pos, state, ResidueCrockBlock.class).map(PurifierBlockEntity::residuePort).orElse(null);
    }
}
