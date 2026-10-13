package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The small farm of the Harvester and Spreader GameTests: a controller,
 * Warehouse Rails x 1 to 3 and farm rails x 4 to 7 along z 6, a garage and a
 * Heavy Duty Rack on the Warehouse Rails and the Field Head east of the farm
 * rails. The field's plots are x 0 to 11, z 2 to 10 at y 1, the farmland under
 * them at y 0.
 */
final class FarmFixtures {

    static final int RAIL_Z = 6;
    static final BlockPos CONTROLLER = new BlockPos(0, 1, RAIL_Z);
    static final BlockPos GARAGE = new BlockPos(1, 1, RAIL_Z + 1);
    static final BlockPos RACK = new BlockPos(2, 1, RAIL_Z - 1);
    static final BlockPos HEAD = new BlockPos(8, 1, RAIL_Z);

    record Farm(FieldHeadBlockEntity head, ShuttleGarageBlockEntity garage, HeavyRackBlockEntity rack) {
    }

    private FarmFixtures() {
    }

    /** A Mk1 shuttle with {@code tool} in its tool slot. */
    static ItemStack shuttleWith(Item tool) {
        ItemStack shuttle = new ItemStack(ServusStorageItems.SHUTTLE_MK1.get());
        ShuttleModules.set(shuttle, ShuttleModules.EMPTY.withTool(Optional.of(tool)));
        return shuttle;
    }

    /** Builds the farm with that shuttle in the garage, quick field work and quick field checks. */
    static Farm farm(GameTestHelper helper, ItemStack shuttle) {
        StorageStatsManager.INSTANCE.setFieldWorkTicks(2);
        StorageStatsManager.INSTANCE.setFieldCheckInterval(5);
        WarehouseFixtures.controller(helper, CONTROLLER);
        for (int x = 1; x <= 7; x++) {
            FieldFixtures.rail(helper, new BlockPos(x, 1, RAIL_Z), x <= 3 ? ServusStorageBlocks.RAIL.get() : ServusStorageBlocks.FARM_RAIL.get());
        }
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, GARAGE, Direction.SOUTH, 0);
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, RACK, Direction.SOUTH);
        FieldHeadBlockEntity head = FieldFixtures.head(helper, HEAD);
        garage.shuttles().setStackInSlot(0, shuttle);
        return new Farm(head, garage, rack);
    }

    static WarehouseNetwork network(GameTestHelper helper) {
        return helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().orElseThrow();
    }

    /** How many shuttles of the farm's garage are out. */
    static int out(GameTestHelper helper) {
        return network(helper).garageOut(helper.absolutePos(GARAGE));
    }

    static boolean noItemsLying(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).isEmpty();
    }
}
