package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.FormingEffect;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
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
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;

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
            case VAT -> ServusManufacturingBlocks.PURIFIER.get();
            case BASIN -> ServusManufacturingBlocks.CATCH_BASIN.get();
            case CROCK -> ServusManufacturingBlocks.RESIDUE_CROCK.get();
            case LOG -> Blocks.OAK_LOG;
        };
    }

    private static boolean matches(BlockState state, Cell cell, boolean formed) {
        return switch (cell.role()) {
            case VAT -> state.is(ServusManufacturingBlocks.PURIFIER.get()) && state.getValue(PurifierBlock.FORMED) == formed;
            case BASIN -> state.is(ServusManufacturingBlocks.CATCH_BASIN.get()) && state.getValue(PurifierSideBlock.FORMED) == formed;
            case CROCK -> state.is(ServusManufacturingBlocks.RESIDUE_CROCK.get()) && state.getValue(PurifierSideBlock.FORMED) == formed;
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
        Optional<Cell> best = Optional.empty();
        long bestMatches = -1;
        for (Facing facing : PurifierLayout.FACINGS) {
            Optional<Cell> wrong = firstWrong(level, vat, facing, false);
            if (wrong.isEmpty()) {
                form(level, vat, facing);
                FormingEffect.play(level, vat, cells(vat, facing));
                if (level.getBlockEntity(vat) instanceof PurifierBlockEntity purifier) {
                    purifier.setOwner(player.getUUID());
                }
                player.sendOverlayMessage(Component.translatable("message.servus_manufacturing.purifier.formed"));
                return true;
            }
            long matches = PurifierLayout.cells().stream().filter(cell -> matches(level.getBlockState(pos(vat, cell, facing)), cell, false)).count();
            if (matches > bestMatches) {
                bestMatches = matches;
                best = wrong;
            }
        }
        Cell wrong = best.orElseThrow();
        player.displayClientMessage(Component.translatable("message.servus_manufacturing.purifier.missing", shown(wrong.role()).getName(),
                Component.translatable("message.servus_manufacturing.purifier.at." + PurifierLayout.placement(wrong).replace(' ', '_'))), false);
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
    public static FluidView basinHandler(Level level, BlockPos pos, BlockState state) {
        return formedVat(level, pos, state, CatchBasinBlock.class).map(PurifierBlockEntity::saltPort).orElse(null);
    }

    /** The residue extract only view of a formed Crock. */
    public static ItemView crockHandler(Level level, BlockPos pos, BlockState state) {
        return formedVat(level, pos, state, ResidueCrockBlock.class).map(PurifierBlockEntity::residuePort).orElse(null);
    }
}
