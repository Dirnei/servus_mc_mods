package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Gives GameTest warehouses power: shuttles, wakes and Crafting Stations draw
 * FE from the controller's buffer, so every test that runs them fills the
 * buffer right after placing the controller and goes on testing what it tested.
 */
public final class PowerFixtures {

    private PowerFixtures() {
    }

    /** Fills the buffer of the controller at that test position. */
    public static void fill(GameTestHelper helper, BlockPos controller) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(controller)) instanceof WarehouseControllerBlockEntity entity) {
            fill(entity);
        }
    }

    /**
     * Lets the controller's shuttles, wakes and Crafting Stations run without
     * taking FE, for tests that count the FE of wireless transfers.
     */
    public static void freeDraws(WarehouseControllerBlockEntity controller) {
        controller.setFreeDrawsForTests(true);
    }

    /**
     * Lets a Crafting Station that stands without a warehouse on its data line
     * craft, for tests of the station itself.
     */
    public static void freeStation(CraftingStationBlockEntity station) {
        station.setFreePowerForTests(true);
    }

    /** Fills a controller's buffer. */
    public static void fill(WarehouseControllerBlockEntity controller) {
        controller.setEnergy(controller.energy().capacity());
    }
}
