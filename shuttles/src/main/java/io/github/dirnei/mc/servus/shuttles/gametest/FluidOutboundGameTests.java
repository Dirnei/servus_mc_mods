package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidKeep;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidOutboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.menu.FluidOutboundMenu;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * The Fluid Outbound as a block: placing, the drop keeping the keep row,
 * tanks guarded by their keep fluid, sides that hand fluid out and take
 * none in, not being stock, the keep row in the GUI, buckets and the potato.
 * Stations here face north, so a player in front sees east on the left; the
 * station's back is south.
 */
public final class FluidOutboundGameTests {

    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos STATION = new BlockPos(4, 1, 4);
    private static final int PLUS_ONE = 1;
    private static final int PLUS_TEN = 3;

    private FluidOutboundGameTests() {
    }

    private static FluidOutboundBlockEntity place(GameTestHelper helper) {
        return FluidStationFixtures.outbound(helper, STATION, FRONT);
    }

    /** A station keeping that fluid in tank 1 and holding that much of it. */
    private static FluidOutboundBlockEntity keeping(GameTestHelper helper, Fluid fluid, int amount) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(0, fluid);
        FluidStationFixtures.fill(station, 0, fluid, amount);
        return station;
    }

    /** Placed on stone it stands two blocks high, pushes through its back on All and keeps every other side Off. */
    @ServusGameTest(timeoutTicks = 40)
    public static void placedWithBackOnPush(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        BlockState upper = helper.getBlockState(STATION.above());
        helper.assertTrue(upper.getBlock() instanceof FluidStationBlock && upper.getValue(FluidStationBlock.HALF) == DoubleBlockHalf.UPPER,
                "the upper half stands above the lower one");
        helper.assertTrue(station.tankCount() == ShuttlesStatsManager.INSTANCE.fluidStationTanks(), "the station has the configured tanks");
        helper.assertTrue(station.fluidSides().mode(RelativeSide.BACK) == SideMode.PUSH && station.fluidSides().tank(RelativeSide.BACK) == FluidSides.ALL,
                "the back pushes on All");
        for (RelativeSide side : List.of(RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.TOP, RelativeSide.BOTTOM)) {
            helper.assertTrue(station.fluidSides().mode(side) == SideMode.DISABLED, side + " is Off");
        }
        helper.assertTrue(station.fluidSides().isLocked(RelativeSide.FRONT), "the front is fixed");
        helper.assertTrue(!station.keep(0).isSet(), "a new station keeps nothing");
        helper.succeed();
    }

    /** The dropped item keeps 8000 mB of water in tank 1 with its 5000 mB; placed again, the station keeps and holds them. */
    @ServusGameTest(timeoutTicks = 60)
    public static void breakKeepsKeepRow(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 5000);
        station.stepKeep(0, 8000);
        helper.destroyBlock(STATION.above());
        helper.runAfterDelay(1, () -> {
            List<ItemStack> drops = FluidStationFixtures.dropped(helper, STATION);
            helper.assertTrue(drops.size() == 1, "one item drops, got " + drops);
            FluidOutboundBlockEntity placed = FluidStationFixtures.outbound(helper, new BlockPos(8, 1, 8), FRONT, drops.getFirst().copy());
            helper.assertTrue(placed.keep(0).equals(new FluidKeep<>(Fluids.WATER, 8000)), "tank 1 still keeps 8000 mB of water, got " + placed.keep(0));
            helper.assertTrue(placed.tank(0).getFluidAmount() == 5000, "tank 1 still holds 5000 mB");
            helper.assertTrue(placed.fluidSides().mode(RelativeSide.BACK) == SideMode.PUSH, "the back still pushes");
            helper.succeed();
        });
    }

    /** Tank 1 keeps water: a lava bucket puts nothing in, a water bucket fills it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void keepFluidGuardsTheTank(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(0, Fluids.WATER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBucket(helper, player, new ItemStack(Items.LAVA_BUCKET), STATION);
        helper.assertTrue(station.isEmpty(), "no tank takes the lava");
        helper.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET), "the player keeps the lava bucket");
        useBucket(helper, player, new ItemStack(Items.WATER_BUCKET), STATION.above());
        helper.assertTrue(station.tank(0).getFluid().is(Fluids.WATER) && station.tank(0).getFluidAmount() == 1000, "tank 1 takes the water");
        helper.succeed();
    }

    /** Tank 2 holds 3000 mB of lava when its keep is cleared: the lava stays and drains, and nothing new goes in. */
    @ServusGameTest(timeoutTicks = 40)
    public static void clearedKeepKeepsContent(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(1, Fluids.LAVA);
        FluidStationFixtures.fill(station, 1, Fluids.LAVA, 3000);
        station.setKeepFluid(1, null);
        helper.assertTrue(station.tank(1).getFluidAmount() == 3000, "the lava stays in tank 2");
        helper.assertTrue(station.tank(1).fill(new FluidStack(Fluids.LAVA, 1000), false) == 0, "no more lava goes in");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBucket(helper, player, new ItemStack(Items.BUCKET), STATION);
        helper.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET) && station.tank(1).getFluidAmount() == 2000,
                "an empty bucket still drains the lava");
        helper.succeed();
    }

    /** Water in tank 1 flows into a Fluid Tank behind the lower half through the back on Push. */
    @ServusGameTest(timeoutTicks = 60)
    public static void pushIntoFluidTank(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 4000);
        BlockPos behind = STATION.relative(FRONT.getOpposite());
        helper.setBlock(behind, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        Waits.when(helper, () -> station.tank(0).isEmpty(), 50, () -> "the water is pushed out, " + station.tank(0).getFluidAmount() + " mB left", () -> {
            FluidStack pushed = Transfer.fluidInTank(FluidStationFixtures.capability(helper, behind, Direction.UP), 0);
            helper.assertTrue(pushed.is(Fluids.WATER) && pushed.getAmount() == 4000, "the Fluid Tank holds the water, got " + pushed);
            helper.succeed();
        });
    }

    /** The left side on Output for tank 2 lets a pipe drain its lava from both halves and takes nothing in. */
    @ServusGameTest(timeoutTicks = 40)
    public static void outputLetsPipesDrain(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(1, Fluids.LAVA);
        FluidStationFixtures.fill(station, 1, Fluids.LAVA, 4000);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.OUTPUT, 1);
        Direction left = FRONT.getClockWise();
        ResourceHandler<FluidResource> lower = FluidStationFixtures.capability(helper, STATION, left);
        ResourceHandler<FluidResource> upper = FluidStationFixtures.capability(helper, STATION.above(), left);
        helper.assertTrue(Transfer.drain(lower, 1000, false).is(Fluids.LAVA), "a pipe drains lava from the lower half");
        helper.assertTrue(Transfer.drain(upper, new FluidStack(Fluids.LAVA, 1000), false).getAmount() == 1000, "and from the upper half");
        helper.assertTrue(Transfer.fill(lower, new FluidStack(Fluids.LAVA, 1000), false) == 0, "nothing flows in through the Output side");
        helper.assertTrue(station.tank(1).getFluidAmount() == 2000, "tank 2 holds the rest");
        helper.succeed();
    }

    /** With every side on Push or Output and tank 1 keeping water, no side takes water in. */
    @ServusGameTest(timeoutTicks = 40)
    public static void noFillThroughAnySide(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(0, Fluids.WATER);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.OUTPUT, FluidSides.ALL);
        FluidStationFixtures.setSide(station, RelativeSide.RIGHT, SideMode.PUSH, FluidSides.ALL);
        FluidStationFixtures.setSide(station, RelativeSide.TOP, SideMode.OUTPUT, FluidSides.ALL);
        FluidStationFixtures.setSide(station, RelativeSide.BOTTOM, SideMode.OUTPUT, FluidSides.ALL);
        for (Direction face : List.of(FRONT.getClockWise(), FRONT.getCounterClockWise(), FRONT.getOpposite())) {
            for (BlockPos half : List.of(STATION, STATION.above())) {
                ResourceHandler<FluidResource> side = FluidStationFixtures.capability(helper, half, face);
                helper.assertTrue(side != null && Transfer.fill(side, new FluidStack(Fluids.WATER, 1000), false) == 0,
                        "no water goes in through " + face + " of " + half);
            }
        }
        helper.assertTrue(Transfer.fill(FluidStationFixtures.capability(helper, STATION.above(), Direction.UP), new FluidStack(Fluids.WATER, 1000),
                false) == 0, "none through the top");
        helper.assertTrue(Transfer.fill(FluidStationFixtures.capability(helper, STATION, Direction.DOWN), new FluidStack(Fluids.WATER, 1000),
                false) == 0, "none through the bottom");
        helper.assertTrue(station.isEmpty(), "the station stays empty");
        helper.succeed();
    }

    /** 6000 mB of water in a Fluid Outbound of a warehouse are not part of its fluid stock. */
    @ServusGameTest(timeoutTicks = 100)
    public static void notStock(GameTestHelper helper) {
        TankerFixtures.warehouse(helper);
        FluidOutboundBlockEntity station = TankerFixtures.outbound(helper, Fluids.WATER, 8000);
        TankerFixtures.fill(station, 0, Fluids.WATER, 6000);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(TankerFixtures.network(helper).fluidStations().contains(helper.absolutePos(TankerFixtures.OUTBOUND)),
                    "the station is a member of the warehouse");
            helper.assertTrue(TankerFixtures.network(helper).fluidIndex().count(FluidKey.of(new FluidStack(Fluids.WATER, 1))) == 0,
                    "the water is no stock");
            helper.assertTrue(TankerFixtures.network(helper).fluidStores().isEmpty(), "the station is no fluid store");
            helper.succeed();
        });
    }

    /** A water bucket on the cursor sets tank 1's keep and stays on the cursor; eight +1 B make 8000 mB. */
    @ServusGameTest(timeoutTicks = 40)
    public static void keepSetByCellWithBucket(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidOutboundMenu menu = new FluidOutboundMenu(1, player.getInventory(), station);
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.setKeepFromCarried(0);
        for (int press = 0; press < 8; press++) {
            menu.clickMenuButton(player, FluidOutboundMenu.keepButton(0, PLUS_ONE));
        }
        helper.assertTrue(station.keep(0).equals(new FluidKeep<>(Fluids.WATER, 8000)), "tank 1 keeps 8000 mB of water, got " + station.keep(0));
        helper.assertTrue(menu.getCarried().is(Items.WATER_BUCKET), "the water bucket stays on the cursor");
        helper.assertTrue(menu.keep(0).equals(station.keep(0)), "the menu shows the keep");
        menu.setCarried(ItemStack.EMPTY);
        menu.setKeepFromCarried(0);
        helper.assertTrue(!station.keep(0).isSet(), "an empty cursor clears the keep");
        helper.succeed();
    }

    /** At 8000 mB in a tank of 8000 mB, +10 B keeps it there, and the steps do nothing without a keep fluid. */
    @ServusGameTest(timeoutTicks = 40)
    public static void stepsClampedToCapacity(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        int capacity = station.tank(0).getCapacity();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidOutboundMenu menu = new FluidOutboundMenu(1, player.getInventory(), station);
        menu.clickMenuButton(player, FluidOutboundMenu.keepButton(0, PLUS_TEN));
        helper.assertTrue(station.keep(0).amount() == 0, "no keep fluid, no amount");
        station.setKeepFluid(0, Fluids.WATER);
        menu.clickMenuButton(player, FluidOutboundMenu.keepButton(0, PLUS_TEN));
        menu.clickMenuButton(player, FluidOutboundMenu.keepButton(0, PLUS_TEN));
        helper.assertTrue(station.keep(0).amount() == capacity, "the keep stops at the capacity " + capacity + ", got " + station.keep(0).amount());
        helper.succeed();
    }

    /** Tank 1 holds 5200 mB of water: an empty bucket takes 1000 mB of it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void emptyBucketTakesWater(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 5200);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBucket(helper, player, new ItemStack(Items.BUCKET), STATION.above());
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the player gets a water bucket");
        helper.assertTrue(station.tank(0).getFluidAmount() == 4200, "tank 1 holds 4200 mB, got " + station.tank(0).getFluidAmount());
        helper.succeed();
    }

    /** The potato names tank 1 with 5200 of 8000 mB of water keeping 8000 mB of water, and tanks 2 and 3 empty, keeping nothing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsKeeps(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 5200);
        station.stepKeep(0, 8000);
        BlockPos lower = helper.absolutePos(STATION);
        BlockState state = helper.getLevel().getBlockState(lower);
        List<Component> report = ((FluidStationBlock) state.getBlock()).potatoReport(helper.getLevel(), lower, state, Vec3.atCenterOf(lower));
        helper.assertTrue(report.size() == station.tankCount(), "one line per tank, got " + report.size());
        Component first = report.getFirst();
        helper.assertTrue(key(first).equals("message.servus_shuttles.potato.fluid_station_tank") && args(first)[2].equals(5200),
                "tank 1 holds 5200 mB: " + first.getString());
        helper.assertTrue(keyOfKeep(first).equals("message.servus_shuttles.potato.fluid_station_keeps") && argsOfKeep(first)[0].equals(8000),
                "tank 1 keeps 8000 mB: " + first.getString());
        for (Component line : report.subList(1, report.size())) {
            helper.assertTrue(key(line).equals("message.servus_shuttles.potato.fluid_station_empty")
                    && keyOfKeep(line).equals("message.servus_shuttles.potato.fluid_station_keeps_nothing"), "empty and keeping nothing: " + line.getString());
        }
        helper.assertTrue(station.tank(0).getFluidAmount() == 5200, "the potato changes nothing");
        helper.succeed();
    }

    private static String key(Component line) {
        return line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
    }

    private static Object[] args(Component line) {
        return line.getContents() instanceof TranslatableContents contents ? contents.getArgs() : new Object[0];
    }

    private static String keyOfKeep(Component line) {
        return line.getSiblings().isEmpty() ? "" : key(line.getSiblings().getFirst());
    }

    private static Object[] argsOfKeep(Component line) {
        return line.getSiblings().isEmpty() ? new Object[0] : args(line.getSiblings().getFirst());
    }

    /** Uses a fluid container on the front of a half, as a right click there does: the block takes the click first. */
    private static void useBucket(GameTestHelper helper, Player player, ItemStack bucket, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        BlockPos absolute = helper.absolutePos(pos);
        helper.getBlockState(pos).useItemOn(bucket, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(FRONT, 0.5), FRONT, absolute, false));
    }
}
