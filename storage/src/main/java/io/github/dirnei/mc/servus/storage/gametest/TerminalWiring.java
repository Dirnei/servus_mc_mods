package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Places a Storage Terminal on the data line of a warehouse: a Packing Station,
 * a data duct on top of it and the terminal on top of the duct. Stations join
 * only through rails, so unless a rail already touches the Packing Station a rail
 * is laid under it and under every storage block beside it.
 */
public final class TerminalWiring {

    private TerminalWiring() {
    }

    /** Packing Station at {@code pos}, data duct above, terminal two blocks up, linked to the warehouse by rail. */
    public static void place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        dataDuct(helper, pos.above());
        helper.setBlock(terminalPos(pos), ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        railUnder(helper, pos);
        // Some tests place the controller after the terminal; link again once they did.
        helper.runAfterDelay(1, () -> railUnder(helper, pos));
    }

    /**
     * Unless a rail already touches the station at {@code pos}, lays a rail under it and under
     * every storage block beside it, so the station joins their warehouse the only way it can.
     */
    public static void railUnder(GameTestHelper helper, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (helper.getBlockState(pos.relative(side)).is(ServusStorageBlocks.RAIL.get())) {
                return;
            }
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = pos.relative(side);
            if (helper.getLevel().getBlockEntity(helper.absolutePos(beside)) instanceof StorageNodeBlockEntity node
                    && node.kind() == WarehouseConnectionRule.NodeKind.STOCK
                    && !helper.getBlockState(beside.below()).is(ServusStorageBlocks.RAIL.get())) {
                WarehouseFixtures.rail(helper, beside.below());
            }
        }
        if (!helper.getBlockState(pos.below()).is(ServusStorageBlocks.RAIL.get())) {
            WarehouseFixtures.rail(helper, pos.below());
        }
    }

    /** Where {@link #place} put the terminal. */
    public static BlockPos terminalPos(BlockPos pos) {
        return pos.above(2);
    }

    /** The terminal {@link #place} put above the Packing Station at {@code pos}. */
    public static StorageTerminalBlockEntity at(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(terminalPos(pos), StorageTerminalBlockEntity.class);
    }

    /** A fresh menu of the terminal {@link #place} put above {@code pos}, opened by {@code player}. */
    public static StorageTerminalMenu menu(GameTestHelper helper, Player player, BlockPos pos) {
        return new StorageTerminalMenu(1, player.getInventory(), at(helper, pos));
    }

    /** How many of the item the menu's stock list shows. */
    public static long stockOf(StorageTerminalMenu menu, Item item) {
        long total = 0;
        for (StockEntry entry : menu.stock().entries()) {
            if (entry.stack().is(item)) {
                total += entry.count();
            }
        }
        return total;
    }

    /** A Utility Duct carrying only the data line. */
    public static UtilityDuctBlockEntity dataDuct(GameTestHelper helper, BlockPos pos) {
        GameTestSteps.duct(helper, pos, Line.DATA);
        return helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
    }
}
