package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * The smallest real warehouse for GameTests: a rail run along x starting at
 * {@code origin} (stone below), a Heavy Duty Rack north of the first rail
 * facing it, a Shuttle Garage with one Mk1 shuttle north of the second rail
 * and the controller north of the third. South of the rail there is room for
 * a Packing Station with a terminal on top ({@link #withTerminal()}) at x + 1
 * and an Inbound Station ({@link #withInbound()}) at x + 2.
 *
 * <pre>
 *   z - 1:  rack   garage  controller
 *   z    :  rail   rail    rail
 *   z + 1:         packing inbound
 * </pre>
 */
public final class MiniWarehouse {

    /** Ticks a Mk1 shuttle needs from the garage to the rack and on to a station beside the rail, braking included. */
    public static final int TRIP_TICKS = 120;

    private final GameTestHelper helper;
    private final BlockPos origin;
    private final int rails;

    private MiniWarehouse(GameTestHelper helper, BlockPos origin, int rails) {
        this.helper = helper;
        this.origin = origin;
        this.rails = rails;
    }

    /** Controller, three rails, a rack and a garage with one shuttle. */
    public static MiniWarehouse build(GameTestHelper helper, BlockPos origin) {
        return build(helper, origin, true);
    }

    /** Like {@link #build(GameTestHelper, BlockPos)}; without a garage when {@code garage} is false. */
    public static MiniWarehouse build(GameTestHelper helper, BlockPos origin, boolean garage) {
        MiniWarehouse warehouse = new MiniWarehouse(helper, origin, 3);
        for (int i = 0; i < warehouse.rails; i++) {
            helper.setBlock(warehouse.railPos(i).below(), Blocks.STONE.defaultBlockState());
            TerminalWiring.rail(helper, warehouse.railPos(i));
        }
        helper.setBlock(warehouse.rackPos(), ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState()
                .setValue(HeavyRackBlock.FACING, Direction.SOUTH));
        helper.setBlock(warehouse.controllerPos(), ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        if (garage) {
            warehouse.addGarage();
        }
        return warehouse;
    }

    /** Places the garage with one shuttle, for tests that start without it. */
    public ShuttleGarageBlockEntity addGarage() {
        helper.setBlock(garagePos(), ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState()
                .setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(garagePos(), ShuttleGarageBlockEntity.class);
        helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        return garage;
    }

    /** A Packing Station south of the second rail with a data duct and a Storage Terminal on top. */
    public MiniWarehouse withTerminal() {
        TerminalWiring.place(helper, packingPos());
        return this;
    }

    /** A Packing Station south of the second rail, without a terminal. */
    public MiniWarehouse withPackingStation() {
        helper.setBlock(packingPos(), ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        return this;
    }

    /** An Inbound Station south of the third rail, front to the south. */
    public InboundStationBlockEntity withInbound() {
        helper.setBlock(inboundPos(), ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.SOUTH));
        return helper.getBlockEntity(inboundPos(), InboundStationBlockEntity.class);
    }

    /** Fills the rack's slots with the stacks, as test setup. */
    public MiniWarehouse stock(ItemStack... stacks) {
        stock(helper, rackPos(), stacks);
        return this;
    }

    /** Fills the slots of the rack at {@code rack} with the stacks, as test setup. */
    public static void stock(GameTestHelper helper, BlockPos rack, ItemStack... stacks) {
        HeavyRackBlockEntity entity = helper.getBlockEntity(rack, HeavyRackBlockEntity.class);
        for (ItemStack stack : stacks) {
            ItemStack left = stack.copy();
            for (int slot = 0; slot < entity.items().getSlots() && !left.isEmpty(); slot++) {
                left = entity.items().insertItem(slot, left, false);
            }
            helper.assertTrue(left.isEmpty(), "the rack takes the test stock");
        }
    }

    /** How many of the item the rack holds. */
    public int rackCount(Item item) {
        return count(rack(), item);
    }

    /** How many of the item the rack holds. */
    public static int count(HeavyRackBlockEntity rack, Item item) {
        int total = 0;
        for (int slot = 0; slot < rack.items().getSlots(); slot++) {
            ItemStack stack = rack.items().getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public BlockPos railPos(int index) {
        return origin.east(index);
    }

    public BlockPos rackPos() {
        return origin.north();
    }

    public BlockPos garagePos() {
        return origin.east().north();
    }

    public BlockPos controllerPos() {
        return origin.east(2).north();
    }

    public BlockPos packingPos() {
        return origin.east().south();
    }

    public BlockPos terminalPos() {
        return TerminalWiring.terminalPos(packingPos());
    }

    public BlockPos inboundPos() {
        return origin.east(2).south();
    }

    public HeavyRackBlockEntity rack() {
        return helper.getBlockEntity(rackPos(), HeavyRackBlockEntity.class);
    }

    public StorageTerminalBlockEntity terminal() {
        return TerminalWiring.at(helper, packingPos());
    }

    public WarehouseControllerBlockEntity controller() {
        return helper.getBlockEntity(controllerPos(), WarehouseControllerBlockEntity.class);
    }

    public WarehouseNetwork network() {
        return controller().network().orElseThrow();
    }
}
