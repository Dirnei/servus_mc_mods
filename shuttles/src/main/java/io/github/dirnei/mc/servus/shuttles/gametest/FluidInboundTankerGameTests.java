package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.menu.FluidInboundMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks.Activity;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Fluid Inbound in a warehouse: it joins only through the rail in front
 * of its lower half, is no stock, is a source of tanker put-away runs, shows
 * red lamps for no room and no tanker, sends a tanker home empty when it
 * breaks during a run, and takes the tanker's coupling at its port. The
 * station and Fluid Racks stand west of the {@link TankerFixtures} rail facing east.
 */
public final class FluidInboundTankerGameTests {

    private static final FluidKey WATER = FluidKey.of(new FluidStack(Fluids.WATER, 1));
    private static final Direction FRONT = Direction.EAST;
    private static final BlockPos STATION = new BlockPos(3, 1, 6);
    private static final int RUN = 400;

    private FluidInboundTankerGameTests() {
    }

    /** Fluid Rack bays at x 3 from z 9 to 11, facing the rail. */
    private static BlockPos[] racks(GameTestHelper helper) {
        BlockPos[] bays = {new BlockPos(3, 1, 9), new BlockPos(3, 1, 10), new BlockPos(3, 1, 11)};
        FluidRackFixtures.row(helper, FRONT, bays);
        return bays;
    }

    private static FluidInboundBlockEntity station(GameTestHelper helper) {
        return FluidStationFixtures.place(helper, STATION, FRONT);
    }

    private static String waitingKey(GameTestHelper helper, FluidInboundBlockEntity station) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Component line = new FluidInboundMenu(1, player.getInventory(), station).waitingLine();
        return line != null && line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
    }

    /** A station whose front lower half faces the rail joins and docks; one touching the rail with its side stays out. */
    @ServusGameTest(timeoutTicks = 100)
    public static void joinsOnlyByFrontRail(GameTestHelper helper) {
        TankerFixtures.warehouse(helper);
        FluidInboundBlockEntity front = station(helper);
        BlockPos sidePos = new BlockPos(3, 1, 9);
        FluidInboundBlockEntity side = FluidStationFixtures.place(helper, sidePos, Direction.NORTH);
        Waits.when(helper, () -> WarehouseFixtures.lit(helper, STATION), 40, () -> "the station's connection lamp lights", () -> {
            helper.assertTrue(front.network().orElse(null) == TankerFixtures.network(helper), "the station is a member of the warehouse");
            helper.assertTrue(helper.getBlockState(STATION.relative(FRONT)).getValue(RailBlock.dock(FRONT.getOpposite())),
                    "the rail docks toward the station");
            helper.assertTrue(!WarehouseFixtures.lit(helper, sidePos), "the station touching the rail with its side stays dark");
            helper.assertTrue(side.network().orElse(null) != TankerFixtures.network(helper), "it is no member of the warehouse");
            helper.succeed();
        });
    }

    /** 8000 mB of water in a station of the warehouse are not part of its fluid stock. */
    @ServusGameTest(timeoutTicks = 100)
    public static void notStock(GameTestHelper helper) {
        TankerFixtures.warehouse(helper);
        FluidInboundBlockEntity station = station(helper);
        FluidStationFixtures.fill(station, 0, Fluids.WATER, 8000);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(TankerFixtures.network(helper).fluidStations().contains(helper.absolutePos(STATION)), "the station is listed");
            helper.assertTrue(TankerFixtures.network(helper).fluidIndex().count(WATER) == 0, "the water is no stock");
            helper.assertTrue(TankerFixtures.network(helper).fluidStores().isEmpty(), "the station is no fluid store");
            helper.succeed();
        });
    }

    /** Water piped into the station goes to an empty Fluid Rack section by tanker. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void pumpToRackRun(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity station = station(helper);
        BlockPos[] bays = racks(helper);
        Transfer.fill(FluidStationFixtures.capability(helper, STATION.above(), FRONT.getOpposite()), new FluidStack(Fluids.WATER, 4000), false);
        Waits.when(helper, () -> FluidRackFixtures.amount(helper, bays[0]) == 4000 && TankerFixtures.out(helper) == 0, RUN,
                () -> "the water reaches the rack, got " + FluidRackFixtures.amount(helper, bays[0]), () -> {
            helper.assertTrue(station.isEmpty(), "the station is empty");
            helper.succeed();
        });
    }

    /** Lava with no store taking it: no tanker drives, the lamp is red and the GUI names lava. */
    @ServusGameTest(timeoutTicks = 200)
    public static void redLampNoRoom(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity station = station(helper);
        BlockPos[] bays = racks(helper);
        helper.runAfterDelay(2, () -> {
            FluidRackBlock.stampSection(helper.getLevel(), helper.absolutePos(bays[0]),
                    new ZoneDefinition("Cooling", new ZoneFilter(List.of(new ItemStack(Items.WATER_BUCKET)), List.of())));
            FluidStationFixtures.fill(station, 0, Fluids.LAVA, 2000);
            Waits.when(helper, () -> WarehouseFixtures.blocked(helper, STATION), 150, () -> "the station's lamp turns red", () -> {
                helper.assertTrue(station.fluidActivity().noRoom() == Fluids.LAVA, "the station names lava as without room");
                helper.assertTrue(waitingKey(helper, station).equals("gui.servus_shuttles.fluid_station.state.no_room"), "the GUI says there is no room");
                helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no tanker drives");
                helper.assertTrue(station.tank(0).getFluidAmount() == 2000, "the lava stays in the station");
                helper.succeed();
            });
        });
    }

    /** Water with no Tanker shuttle in the warehouse: the lamp is red and the GUI says no tanker reaches the station. */
    @ServusGameTest(timeoutTicks = 200)
    public static void redLampNoTanker(GameTestHelper helper) {
        TankerFixtures.warehouse(helper);
        FluidInboundBlockEntity station = station(helper);
        racks(helper);
        FluidStationFixtures.fill(station, 0, Fluids.WATER, 2000);
        Waits.when(helper, () -> WarehouseFixtures.blocked(helper, STATION), 150, () -> "the station's lamp turns red", () -> {
            helper.assertTrue(station.fluidActivity().noTanker(), "the station waits for a tanker");
            helper.assertTrue(waitingKey(helper, station).equals("gui.servus_shuttles.fluid_station.state.no_tanker"), "the GUI says no tanker comes");
            helper.succeed();
        });
    }

    /** A tank holding less than the min load waits: no run, and the lamp is orange. */
    @ServusGameTest(timeoutTicks = 200)
    public static void smallAmountWaitsOrange(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity station = station(helper);
        racks(helper);
        FluidStationFixtures.fill(station, 0, Fluids.WATER, 300);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(WarehouseFixtures.activity(helper, STATION) == Activity.WORKING, "the lamp is orange, got "
                    + WarehouseFixtures.activity(helper, STATION));
            helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no run is queued");
            helper.succeed();
        });
    }

    /** The station breaks while a tanker is on its way to it: the tanker comes home empty and the rack gets nothing. */
    @ServusGameTest(timeoutTicks = RUN * 2)
    public static void stationBrokenMidRun(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity station = station(helper);
        BlockPos[] bays = racks(helper);
        FluidStationFixtures.fill(station, 0, Fluids.WATER, 4000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            helper.destroyBlock(STATION);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 0, RUN, () -> "the tanker comes home", () -> {
                helper.assertTrue(FluidRackFixtures.content(helper, bays[0]).isEmpty(), "the rack got nothing");
                helper.assertTrue(TankerFixtures.network(helper).queuedJobs() == 0, "no run is queued again");
                helper.assertTrue(FluidStationFixtures.dropped(helper, STATION).size() == 1, "the station dropped its item");
                helper.succeed();
            });
        });
    }

    /** While a tanker pumps at the station its view carries the coupling toward it; the target gantry takes no coupling. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void couplingAtStation(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity station = station(helper);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        FluidStationFixtures.fill(station, 0, Fluids.WATER, 4000);
        BlockPos garage = helper.absolutePos(TankerFixtures.GARAGE);
        List<Direction> seen = new ArrayList<>();
        Waits.when(helper, () -> {
            for (ShuttleView view : TankerFixtures.network(helper).shuttleViews(garage)) {
                view.coupling().map(ShuttleView.Coupling::side).ifPresent(seen::add);
            }
            return TankerFixtures.total(storage) == 4000 && TankerFixtures.out(helper) == 0;
        }, RUN, () -> "the water reaches the Storage gantry", () -> {
            helper.assertTrue(!seen.isEmpty() && seen.stream().allMatch(side -> side == FRONT.getOpposite()),
                    "the coupling slid out toward the station, west of the rail, got " + seen);
            helper.assertTrue(station.isEmpty(), "the station is empty");
            helper.succeed();
        });
    }
}
