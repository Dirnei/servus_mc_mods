package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Kind;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Forms a Brewhouse from loose parts around a clicked one. */
public final class BrewhouseForming {

    private BrewhouseForming() {
    }

    /** Forms the Brewhouse through the loose part at {@code clicked} with its front toward {@code front}; false when no layout fits. */
    public static boolean tryForm(Level level, BlockPos clicked, Direction front) {
        if (!(level.getBlockState(clicked).getBlock() instanceof BrewhousePartBlock block)) {
            return false;
        }
        Direction right = front.getCounterClockWise();
        Direction back = front.getOpposite();
        Optional<Cell> found = BrewhouseLayout.find(block.kind(),
                (columns, rows, layers, kind) -> isLoose(level, offset(clicked, right, back, columns, rows, layers), kind));
        if (found.isEmpty()) {
            return false;
        }
        Cell clickedAs = found.get();
        BlockPos master = offset(clicked, right, back, -clickedAs.column(), -clickedAs.row(), -clickedAs.layer());
        form(level, master, front, right, back);
        return true;
    }

    private static BlockPos offset(BlockPos from, Direction right, Direction back, int columns, int rows, int layers) {
        return from.relative(right, columns).relative(back, rows).above(layers);
    }

    private static boolean isLoose(Level level, BlockPos pos, Kind kind) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BrewhousePartBlock part && part.kind() == kind && !state.getValue(BrewhousePartBlock.FORMED);
    }

    private static void form(Level level, BlockPos master, Direction front, Direction right, Direction back) {
        for (Cell cell : Cell.values()) {
            BlockPos at = offset(master, right, back, cell.column(), cell.row(), cell.layer());
            BlockState state = level.getBlockState(at);
            BlockState formed = state.setValue(BrewhousePartBlock.FORMED, true).setValue(BrewhousePartBlock.FACING, front);
            formed = cell.kind() == Kind.BRICK
                    ? formed.setValue(BrewhouseBricksBlock.PART, BrewhousePart.of(cell)).setValue(BrewhouseBricksBlock.LIT, false)
                    .setValue(BrewhouseBricksBlock.TRAY, TrayLook.EMPTY)
                    : formed.setValue(BrewKettleBlock.PART, BrewhousePart.of(cell)).setValue(BrewKettleBlock.BREWING, false);
            level.setBlock(at, formed, Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof BrewhouseBlockEntity part) {
                if (cell == BrewhouseLayout.MASTER) {
                    part.becomeMaster();
                } else {
                    part.joinMaster(master);
                }
            }
        }
        for (Cell cell : Cell.values()) {
            level.invalidateCapabilities(offset(master, right, back, cell.column(), cell.row(), cell.layer()));
        }
        level.playSound(null, master, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }
}
