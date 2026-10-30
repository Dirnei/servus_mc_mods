package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidOutboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Fluid Rack shelves in a warehouse: stock only through the port's rail,
 * the stock following buckets, the fluids command, tanker put-aways and
 * restocks with racks, zones ranking and refusing fluids, a run whose rack
 * breaks, and the coupling a tanker slides into the port. Racks stand west
 * of the {@link TankerFixtures} rail facing east.
 */
public final class FluidRackTankerGameTests {

    private static final FluidKey WATER = FluidKey.of(new FluidStack(Fluids.WATER, 1));
    private static final Direction FRONT = Direction.EAST;
    private static final int RUN = 400;

    private FluidRackTankerGameTests() {
    }

    /** Bays at x 3 from {@code fromZ} to {@code toZ}, facing the rail. */
    private static BlockPos[] bays(int fromZ, int toZ) {
        BlockPos[] bays = new BlockPos[toZ - fromZ + 1];
        for (int z = fromZ; z <= toZ; z++) {
            bays[z - fromZ] = new BlockPos(3, 1, z);
        }
        return bays;
    }

    private static long water(GameTestHelper helper) {
        return TankerFixtures.network(helper).fluidIndex().count(WATER);
    }

    private static int waterZone(GameTestHelper helper) {
        return ZoneFixtures.zone(helper, "Cooling", new ZoneFilter(List.of(new ItemStack(Items.WATER_BUCKET)), List.of()));
    }

    private static void zone(GameTestHelper helper, BlockPos bay, int zone) {
        FluidRackBlock.stampShelf(helper.getLevel(), helper.absolutePos(bay), zone);
    }

    /** A rail only in front of a bay without the port leaves the shelf out; a rail at the port makes it stock. */
    @ServusGameTest(timeoutTicks = 100)
    public static void stockOnlyThroughPortRail(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, TankerFixtures.CONTROLLER);
        RailFixtures.railAlongZ(helper, TankerFixtures.CONTROLLER.getX(), 2, 7);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, bays[1], Fluids.WATER, 5000);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!helper.getBlockState(bays[0].east()).getValue(RailBlock.dock(Direction.WEST)), "the rail shows no dock");
                helper.assertTrue(water(helper) == 0, "the shelf is no stock, got " + water(helper));
                helper.assertTrue(TankerFixtures.network(helper).rackViews().isEmpty(), "no shelf is listed");
                WarehouseFixtures.railOnStone(helper, bays[1].east());
                Waits.when(helper, () -> water(helper) == 5000, 40, () -> "the shelf counts once its port has a rail, got " + water(helper), () -> {
                    helper.assertTrue(helper.getBlockState(bays[1].east()).getValue(RailBlock.dock(Direction.WEST)), "the rail docks to the port");
                    helper.succeed();
                });
            });
        });
    }

    /** Water filled into a docked shelf, as a bucket does, reaches the warehouse's stock. */
    @ServusGameTest(timeoutTicks = 100)
    public static void bucketReachesStock(GameTestHelper helper) {
        TankerFixtures.warehouse(helper);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(water(helper) == 0, "no water yet");
            FluidRackFixtures.fill(helper, bays[2], Fluids.WATER, 1000);
            Waits.when(helper, () -> water(helper) == 1000, 20, () -> "the stock grows by 1000 mB, got " + water(helper), helper::succeed);
        });
    }

    /** The fluids command adds the water of two shelves, leaves a Fluid Inbound's water out and lists the shelves. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fluidsCommandListsRacks(GameTestHelper helper) {
        TankerFixtures.warehouse(helper);
        FluidInboundBlockEntity inbound = TankerFixtures.inbound(helper);
        BlockPos[] bays = bays(9, 11);
        FluidRackFixtures.row(helper, FRONT, bays);
        BlockPos single = new BlockPos(3, 1, 7);
        FluidRackFixtures.place(helper, single, FRONT, FluidRackFixtures.item());
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(inbound, Fluids.WATER, 2000);
            FluidRackFixtures.fill(helper, single, Fluids.WATER, 3000);
            FluidRackFixtures.fill(helper, bays[0], Fluids.WATER, 5000);
            Waits.when(helper, () -> water(helper) == 8000, 40, () -> "8000 mB of water in stock, got " + water(helper), () -> {
                List<String> lines = new ArrayList<>();
                CommandSource collector = new CommandSource() {
                    @Override
                    public void sendSystemMessage(Component message) {
                        lines.add(message.getString());
                    }

                    @Override
                    public boolean acceptsSuccess() {
                        return true;
                    }

                    @Override
                    public boolean acceptsFailure() {
                        return true;
                    }

                    @Override
                    public boolean shouldInformAdmins() {
                        return false;
                    }
                };
                CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()).withSource(collector);
                BlockPos at = helper.absolutePos(TankerFixtures.CONTROLLER);
                helper.getLevel().getServer().getCommands().performPrefixedCommand(source,
                        "servus_warehouse fluids " + at.getX() + " " + at.getY() + " " + at.getZ());
                String text = String.join("\n", lines);
                helper.assertTrue(text.contains("Water: 8000 mB"), "the total is listed: " + text);
                helper.assertTrue(text.contains("Fluid Rack shelves:") && text.contains("Water 5000 / 48000 mB"), "the shelf is listed: " + text);
                helper.succeed();
            });
        });
    }

    /** A tanker brings a Fluid Inbound's water to the only fluid store, an empty unzoned shelf. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void tankerPutsAwayIntoRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        TankerFixtures.fill(source, Fluids.WATER, 4000);
        Waits.when(helper, () -> FluidRackFixtures.amount(helper, bays[0]) == 4000 && TankerFixtures.out(helper) == 0, RUN,
                () -> "the water reaches the rack, got " + FluidRackFixtures.amount(helper, bays[0]), () -> {
            helper.assertTrue(TankerFixtures.amount(source) == 0, "the Inbound is empty");
            helper.succeed();
        });
    }

    /** A kept Fluid Outbound tank is restocked from the water in a shelf. */
    @ServusGameTest(timeoutTicks = RUN * 2)
    public static void restockFromRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidOutboundBlockEntity export = TankerFixtures.outbound(helper, Fluids.WATER, 8000);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, bays[0], Fluids.WATER, 20000);
            Waits.when(helper, () -> TankerFixtures.amount(export) == 8000 && TankerFixtures.out(helper) == 0, RUN * 2 - 10,
                    () -> "the kept tank is restocked to 8000 mB, got " + TankerFixtures.amount(export), () -> {
                helper.assertTrue(FluidRackFixtures.amount(helper, bays[0]) == 12000, "the shelf gave 8000 mB, holds "
                        + FluidRackFixtures.amount(helper, bays[0]));
                helper.succeed();
            });
        });
    }

    /** Water goes to a farther shelf zoned for water rather than a nearer unzoned one. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void waterPrefersWaterZonedRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        BlockPos near = new BlockPos(3, 1, 7);
        BlockPos far = new BlockPos(3, 1, 11);
        FluidRackFixtures.row(helper, FRONT, near, far);
        helper.runAfterDelay(2, () -> {
            zone(helper, far, waterZone(helper));
            TankerFixtures.fill(source, Fluids.WATER, 2000);
            Waits.when(helper, () -> FluidRackFixtures.amount(helper, far) == 2000 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "the water reaches the zoned shelf, got " + FluidRackFixtures.amount(helper, far), () -> {
                helper.assertTrue(FluidRackFixtures.content(helper, near).isEmpty(), "the nearer unzoned shelf stays empty");
                helper.succeed();
            });
        });
    }

    /** Lava has no room when the only shelf is zoned for water: no tanker drives and the Inbound names lava. */
    @ServusGameTest(timeoutTicks = 200)
    public static void waterZoneRefusesLavaByTanker(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            zone(helper, bays[0], waterZone(helper));
            TankerFixtures.fill(source, Fluids.LAVA, 2000);
            Waits.when(helper, () -> source.fluidActivity().noRoom() == Fluids.LAVA, 150, () -> "the Inbound names lava as without room", () -> {
                helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no run is queued");
                helper.assertTrue(FluidRackFixtures.content(helper, bays[0]).isEmpty(), "the shelf takes no lava");
                helper.succeed();
            });
        });
    }

    /** The target shelf's port bay breaks while the tanker brings water: the water still ends in a fluid store. */
    @ServusGameTest(timeoutTicks = RUN * 2)
    public static void brokenTargetRackReroutes(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        BlockPos other = new BlockPos(3, 1, 7);
        FluidRackFixtures.place(helper, other, FRONT, FluidRackFixtures.item());
        BlockPos[] bays = bays(9, 11);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            zone(helper, bays[0], waterZone(helper));
            TankerFixtures.fill(source, Fluids.WATER, 4000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 1 && TankerFixtures.amount(source) == 0, RUN,
                    () -> "the tanker loads the water", () -> {
                helper.destroyBlock(bays[1]);
                Waits.when(helper, () -> TankerFixtures.out(helper) == 0, RUN, () -> "the tanker comes home", () -> {
                    int stored = FluidRackFixtures.amount(helper, other) + FluidRackFixtures.amount(helper, bays[0])
                            + FluidRackFixtures.amount(helper, bays[2]);
                    helper.assertTrue(stored + TankerFixtures.amount(source) == 4000, "all 4000 mB of water are in a fluid store, got " + stored);
                    helper.succeed();
                });
            });
        });
    }

    /**
     * While a tanker pumps at a shelf's port its view carries the coupling
     * toward the rack, west of the rail; at the Inbound east of it the
     * station's hose arm docks instead and no coupling slides out. The
     * coupling is gone after the stop.
     */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void couplingWhilePumpingAtRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        TankerFixtures.fill(source, Fluids.WATER, 4000);
        BlockPos garage = helper.absolutePos(TankerFixtures.GARAGE);
        List<Direction> seen = new ArrayList<>();
        boolean[] armSeen = {false};
        Waits.when(helper, () -> {
            for (ShuttleView view : TankerFixtures.network(helper).shuttleViews(garage)) {
                view.coupling().map(ShuttleView.Coupling::side).ifPresent(seen::add);
            }
            armSeen[0] |= source.armOut();
            return FluidRackFixtures.amount(helper, bays[0]) == 4000;
        }, RUN, () -> "the water reaches the rack", () -> {
            helper.assertTrue(armSeen[0], "the Inbound's hose arm docked onto the tanker");
            helper.assertTrue(seen.contains(Direction.WEST) && seen.stream().allMatch(side -> side == Direction.WEST),
                    "the coupling slid out only toward the rack, got " + seen);
            Optional<ShuttleView> coupled = TankerFixtures.network(helper).shuttleViews(garage).stream()
                    .filter(view -> view.coupling().isPresent()).findFirst();
            helper.assertTrue(coupled.isEmpty(), "no shuttle shows a coupling once the stop is over");
            helper.succeed();
        });
    }
}
