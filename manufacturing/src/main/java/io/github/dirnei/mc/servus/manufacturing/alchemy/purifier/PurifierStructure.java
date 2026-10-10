package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Forms and unforms the Purifier following {@link PurifierLayout}: the Leaching Vat over the Catch
 * Basin between planks and logs. The build reads the same from both sides, so it is checked along
 * the two axes and the Vat keeps the axis. Planks and logs stay vanilla and are checked by their
 * formed neighbours. Server thread only.
 */
public final class PurifierStructure {

    private static boolean changing;

    private PurifierStructure() {
    }

    public static BlockPos pos(BlockPos vat, Cell cell, Facing facing) {
        return Structures.pos(vat, cell.right(), cell.up(), facing);
    }

    private static Direction.Axis axis(Facing facing) {
        return facing == Facing.NORTH || facing == Facing.SOUTH ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static Facing facing(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Facing.NORTH : Facing.EAST;
    }

    /** The block a cell shows in the message and the schematic. */
    public static Block shown(Role role) {
        return switch (role) {
            case VAT -> ServusManufacturingBlocks.PURIFIER.get();
            case BASIN -> ServusManufacturingBlocks.CATCH_BASIN.get();
            case PLANKS -> Blocks.OAK_PLANKS;
            case LOGS -> Blocks.OAK_LOG;
        };
    }

    private static boolean matches(BlockState state, Cell cell) {
        return switch (cell.role()) {
            case VAT -> state.is(ServusManufacturingBlocks.PURIFIER.get()) && !state.getValue(PurifierBlock.FORMED);
            case BASIN -> state.is(ServusManufacturingBlocks.CATCH_BASIN.get()) && !state.getValue(CatchBasinBlock.FORMED);
            case PLANKS -> state.is(BlockTags.PLANKS);
            case LOGS -> state.is(BlockTags.LOGS);
        };
    }

    /** The first wrong cell along {@code facing}, or empty when the build is complete there. */
    private static Optional<Cell> firstWrong(Level level, BlockPos vat, Facing facing) {
        return PurifierLayout.cells().stream().filter(cell -> !matches(level.getBlockState(pos(vat, cell, facing)), cell)).findFirst();
    }

    /** The Journal's click on the Vat: forms along the first axis that is complete, else names what is missing. */
    public static boolean tryForm(Level level, BlockPos vat, Player player) {
        if (level.getBlockState(vat).getValue(PurifierBlock.FORMED)) {
            return true;
        }
        Optional<Cell> best = Optional.empty();
        long bestMatches = -1;
        for (Facing facing : PurifierLayout.AXES) {
            Optional<Cell> wrong = firstWrong(level, vat, facing);
            if (wrong.isEmpty()) {
                form(level, vat, facing);
                if (level.getBlockEntity(vat) instanceof PurifierBlockEntity purifier) {
                    purifier.setOwner(player.getUUID());
                }
                player.displayClientMessage(Component.translatable("message.servus_manufacturing.purifier.formed"), true);
                return true;
            }
            long matches = PurifierLayout.cells().stream().filter(cell -> matches(level.getBlockState(pos(vat, cell, facing)), cell)).count();
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

    public static void form(Level level, BlockPos vat, Facing facing) {
        changing = true;
        try {
            BlockState vatState = level.getBlockState(vat);
            level.setBlock(vat, vatState.setValue(PurifierBlock.FORMED, true).setValue(BlockStateProperties.HORIZONTAL_AXIS, axis(facing)),
                    Block.UPDATE_ALL);
            BlockPos basin = vat.below();
            level.setBlock(basin, level.getBlockState(basin).setValue(CatchBasinBlock.FORMED, true), Block.UPDATE_ALL);
            level.invalidateCapabilities(vat);
            level.invalidateCapabilities(basin);
        } finally {
            changing = false;
        }
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
            BlockPos basin = vat.below();
            BlockState basinState = level.getBlockState(basin);
            if (!basin.equals(skip) && basinState.getBlock() instanceof CatchBasinBlock && basinState.getValue(CatchBasinBlock.FORMED)) {
                level.setBlock(basin, basinState.setValue(CatchBasinBlock.FORMED, false), Block.UPDATE_ALL);
                level.invalidateCapabilities(basin);
            }
        } finally {
            changing = false;
        }
    }

    /** The Vat position of a formed Vat or Basin, or empty. */
    public static Optional<BlockPos> vatOf(Level level, BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof PurifierBlock && state.getValue(PurifierBlock.FORMED)) {
            return Optional.of(pos);
        }
        if (state.getBlock() instanceof CatchBasinBlock && state.getValue(CatchBasinBlock.FORMED)) {
            return Optional.of(pos.above());
        }
        return Optional.empty();
    }

    /** A formed Vat or Basin is replaced: unform the rest. */
    public static void broken(Level level, BlockState state, BlockPos pos) {
        if (changing || level.isClientSide()) {
            return;
        }
        vatOf(level, state, pos).ifPresent(vat -> unform(level, vat, pos));
    }

    /** A neighbour of a formed Vat or Basin changed: the planks and logs have no hook of their own, so check them. */
    public static void neighbourChanged(Level level, BlockState state, BlockPos pos) {
        if (changing || level.isClientSide()) {
            return;
        }
        vatOf(level, state, pos).ifPresent(vat -> {
            BlockState vatState = level.getBlockState(vat);
            if (!(vatState.getBlock() instanceof PurifierBlock)) {
                unform(level, vat, null);
                return;
            }
            Facing facing = facing(vatState.getValue(BlockStateProperties.HORIZONTAL_AXIS));
            for (Cell cell : PurifierLayout.cells()) {
                if ((cell.role() == Role.PLANKS || cell.role() == Role.LOGS) && !matches(level.getBlockState(pos(vat, cell, facing)), cell)) {
                    unform(level, vat, null);
                    return;
                }
            }
        });
    }

    /** The Salt drain only view of a formed Basin. */
    public static IFluidHandler basinHandler(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CatchBasinBlock) || !state.getValue(CatchBasinBlock.FORMED)) {
            return null;
        }
        return level.getBlockEntity(pos.above()) instanceof PurifierBlockEntity purifier && purifier.formed() ? purifier.saltPort() : null;
    }
}
