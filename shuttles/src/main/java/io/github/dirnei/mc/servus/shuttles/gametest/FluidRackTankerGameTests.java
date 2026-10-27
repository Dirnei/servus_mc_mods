package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
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
 * Fluid Rack sections in a warehouse: stock only through the port's rail,
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

    private static ZoneDefinition waterZone() {
        return new ZoneDefinition("Cooling", new ZoneFilter(List.of(new ItemStack(Items.WATER_BUCKET)), List.of()));
    }

    private static void zone(GameTestHelper helper, BlockPos bay, ZoneDefinition zone) {
        FluidRackBlock.stampSection(helper.getLevel(), helper.absolutePos(bay), zone);
    }

    /** A rail only in front of a bay without the port leaves the section out; a rail at the port makes it stock. */
    @ServusGameTest(timeoutTicks = 100)
    public static void stockOnlyThroughPortRail(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, TankerFixtures.CONTROLLER);
        GantryFixtures.railAlongZ(helper, TankerFixtures.CONTROLLER.getX(), 2, 7);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, bays[1], Fluids.WATER, 5000);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!helper.getBlockState(bays[0].east()).getValue(RailBlock.dock(Direction.WEST)), "the rail shows no dock");
                helper.assertTrue(water(helper) == 0, "the section is no stock, got " + water(helper));
                helper.assertTrue(TankerFixtures.network(helper).rackViews().isEmpty(), "no section is listed");
                WarehouseFixtures.railOnStone(helper, bays[1].east());
                Waits.when(helper, () -> water(helper) == 5000, 40, () -> "the section counts once its port has a rail, got " + water(helper), () -> {
                    helper.assertTrue(helper.getBlockState(bays[1].east()).getValue(RailBlock.dock(Direction.WEST)), "the rail docks to the port");
                    helper.succeed();
                });
            });
        });
    }

    /** Water filled into a docked section, as a bucket does, reaches the warehouse's stock. */
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

    /** The fluids command adds a Storage gantry's water and a section's water and lists the section. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fluidsCommandListsRacks(GameTestHelper helper) {
        TankerFixtures.warehouse(helper);
        FluidGantryBlockEntity gantry = TankerFixtures.gantry(helper, TankerFixtures.NEAR, GantryMode.STORAGE);
        BlockPos[] bays = bays(9, 11);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(gantry, 0, Fluids.WATER, 3000);
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
                helper.assertTrue(text.contains("Fluid Rack sections:") && text.contains("Water 5000 / 48000 mB"), "the section is listed: " + text);
                helper.succeed();
            });
        });
    }

    /** A tanker brings an Import gantry's water to the only fluid store, an empty unzoned section. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void tankerPutsAwayIntoRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        TankerFixtures.fill(source, 0, Fluids.WATER, 4000);
        Waits.when(helper, () -> FluidRackFixtures.amount(helper, bays[0]) == 4000 && TankerFixtures.out(helper) == 0, RUN,
                () -> "the water reaches the rack, got " + FluidRackFixtures.amount(helper, bays[0]), () -> {
            helper.assertTrue(TankerFixtures.total(source) == 0, "the Import gantry is empty");
            helper.succeed();
        });
    }

    /** A kept Export tank is restocked from the water in a section. */
    @ServusGameTest(timeoutTicks = RUN * 2)
    public static void restockFromRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity export = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.EXPORT);
        export.setLock(0, Fluids.WATER);
        export.setKeep(0, 8000);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, bays[0], Fluids.WATER, 20000);
            Waits.when(helper, () -> TankerFixtures.amount(export, 0) == 8000 && TankerFixtures.out(helper) == 0, RUN * 2 - 10,
                    () -> "the Export tank is restocked to 8000 mB, got " + TankerFixtures.amount(export, 0), () -> {
                helper.assertTrue(FluidRackFixtures.amount(helper, bays[0]) == 12000, "the section gave 8000 mB, holds "
                        + FluidRackFixtures.amount(helper, bays[0]));
                helper.succeed();
            });
        });
    }

    /** Water goes to a farther section zoned for water rather than a nearer unzoned one. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void waterPrefersWaterZonedRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        BlockPos near = new BlockPos(3, 1, 7);
        BlockPos far = new BlockPos(3, 1, 11);
        FluidRackFixtures.row(helper, FRONT, near, far);
        helper.runAfterDelay(2, () -> {
            zone(helper, far, waterZone());
            TankerFixtures.fill(source, 0, Fluids.WATER, 2000);
            Waits.when(helper, () -> FluidRackFixtures.amount(helper, far) == 2000 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "the water reaches the zoned section, got " + FluidRackFixtures.amount(helper, far), () -> {
                helper.assertTrue(FluidRackFixtures.content(helper, near).isEmpty(), "the nearer unzoned section stays empty");
                helper.succeed();
            });
        });
    }

    /** Lava has no room when the only section is zoned for water: no tanker drives and the Import gantry names lava. */
    @ServusGameTest(timeoutTicks = 200)
    public static void waterZoneRefusesLavaByTanker(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            zone(helper, bays[0], waterZone());
            TankerFixtures.fill(source, 0, Fluids.LAVA, 2000);
            Waits.when(helper, () -> source.noRoom() == Fluids.LAVA, 150, () -> "the Import gantry names lava as without room", () -> {
                helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no run is queued");
                helper.assertTrue(FluidRackFixtures.content(helper, bays[0]).isEmpty(), "the section takes no lava");
                helper.succeed();
            });
        });
    }

    /** The target section's port bay breaks while the tanker brings water: the water still ends in a fluid store. */
    @ServusGameTest(timeoutTicks = RUN * 2)
    public static void brokenTargetRackReroutes(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.NEAR, GantryMode.STORAGE);
        BlockPos[] bays = bays(9, 11);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            zone(helper, bays[0], waterZone());
            TankerFixtures.fill(source, 0, Fluids.WATER, 4000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 1 && TankerFixtures.total(source) == 0, RUN,
                    () -> "the tanker loads the water", () -> {
                helper.destroyBlock(bays[1]);
                Waits.when(helper, () -> TankerFixtures.out(helper) == 0, RUN, () -> "the tanker comes home", () -> {
                    int stored = TankerFixtures.total(storage) + FluidRackFixtures.amount(helper, bays[0]) + FluidRackFixtures.amount(helper, bays[2]);
                    helper.assertTrue(stored + TankerFixtures.total(source) == 4000, "all 4000 mB of water are in a fluid store, got " + stored);
                    helper.succeed();
                });
            });
        });
    }

    /** While a tanker pumps at a section's port its view carries the coupling toward the rack; it is gone after the stop. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void couplingWhilePumpingAtRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        BlockPos[] bays = bays(7, 9);
        FluidRackFixtures.row(helper, FRONT, bays);
        TankerFixtures.fill(source, 0, Fluids.WATER, 4000);
        BlockPos garage = helper.absolutePos(TankerFixtures.GARAGE);
        List<Direction> seen = new ArrayList<>();
        Waits.when(helper, () -> {
            for (ShuttleView view : TankerFixtures.network(helper).shuttleViews(garage)) {
                view.coupling().map(ShuttleView.Coupling::side).ifPresent(seen::add);
            }
            return FluidRackFixtures.amount(helper, bays[0]) == 4000;
        }, RUN, () -> "the water reaches the rack", () -> {
            helper.assertTrue(!seen.isEmpty() && seen.stream().allMatch(side -> side == Direction.WEST),
                    "the coupling slid out toward the rack, west of the rail, got " + seen);
            Optional<ShuttleView> coupled = TankerFixtures.network(helper).shuttleViews(garage).stream()
                    .filter(view -> view.coupling().isPresent()).findFirst();
            helper.assertTrue(coupled.isEmpty(), "no shuttle shows a coupling once the stop is over");
            helper.succeed();
        });
    }
}
