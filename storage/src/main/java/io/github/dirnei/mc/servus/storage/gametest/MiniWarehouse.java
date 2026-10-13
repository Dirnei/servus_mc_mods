package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

    private static final int RAILS = 3;

    private final GameTestHelper helper;
    private final BlockPos origin;

    private MiniWarehouse(GameTestHelper helper, BlockPos origin) {
        this.helper = helper;
        this.origin = origin;
    }

    /** Controller, three rails, a rack and a garage with one shuttle. */
    public static MiniWarehouse build(GameTestHelper helper, BlockPos origin) {
        return build(helper, origin, true);
    }

    /** Like {@link #build(GameTestHelper, BlockPos)}; without a garage when {@code garage} is false. */
    public static MiniWarehouse build(GameTestHelper helper, BlockPos origin, boolean garage) {
        MiniWarehouse warehouse = new MiniWarehouse(helper, origin);
        for (int i = 0; i < RAILS; i++) {
            WarehouseFixtures.railOnStone(helper, warehouse.railPos(i));
        }
        WarehouseFixtures.rack(helper, warehouse.rackPos(), Direction.SOUTH);
        WarehouseFixtures.controller(helper, warehouse.controllerPos());
        if (garage) {
            warehouse.addGarage();
        }
        return warehouse;
    }

    /** Places the garage with one shuttle, for tests that start without it. */
    public ShuttleGarageBlockEntity addGarage() {
        return WarehouseFixtures.garage(helper, garagePos(), Direction.NORTH, 1);
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
        return WarehouseFixtures.inbound(helper, inboundPos(), Direction.SOUTH);
    }

    /**
     * The warehouse many line tests share, built under a controller at
     * {@code controller} whose Packing Station stands south of it: rails at
     * y - 1 under both, two racks west of those rails facing east and a garage
     * north of the controller's rail with one shuttle. Places the controller,
     * not the Packing Station.
     *
     * @return the rack west of the controller's rail, then the one west of the Packing Station's
     */
    public static List<BlockPos> under(GameTestHelper helper, BlockPos controller) {
        WarehouseFixtures.controller(helper, controller);
        BlockPos railA = controller.below();
        BlockPos railB = railA.south();
        WarehouseFixtures.rail(helper, railA);
        WarehouseFixtures.rail(helper, railB);
        List<BlockPos> racks = List.of(railA.west(), railB.west());
        for (BlockPos rack : racks) {
            WarehouseFixtures.rack(helper, rack, Direction.EAST);
        }
        WarehouseFixtures.garage(helper, railA.north(), Direction.NORTH, 1);
        return racks;
    }

    /** Fills the rack's slots with the stacks, as test setup. */
    public MiniWarehouse stock(ItemStack... stacks) {
        WarehouseFixtures.stock(helper, rackPos(), stacks);
        return this;
    }

    /** How many of the item the rack holds. */
    public int rackCount(Item item) {
        return WarehouseFixtures.count(rack(), item);
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
