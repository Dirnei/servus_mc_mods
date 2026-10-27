package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * The Fluid Inbound as a block: placing and breaking both halves, the drop
 * keeping tanks and sides, the Fluids sides of both halves, buckets and the
 * potato. Stations here face north, so a player in front sees east on the
 * left; the station's back is south.
 */
public final class FluidInboundGameTests {

    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos STATION = new BlockPos(4, 1, 4);

    private FluidInboundGameTests() {
    }

    private static int capacity() {
        return ShuttlesStatsManager.INSTANCE.fluidStationTankCapacity();
    }

    /** The item placed on stone stands two blocks high with its front toward the player. */
    @ServusGameTest(timeoutTicks = 40)
    public static void placedTwoHigh(GameTestHelper helper) {
        FluidStationFixtures.place(helper, STATION, FRONT);
        BlockState lower = helper.getBlockState(STATION);
        BlockState upper = helper.getBlockState(STATION.above());
        helper.assertTrue(lower.getBlock() instanceof FluidStationBlock && lower.getValue(FluidStationBlock.HALF) == DoubleBlockHalf.LOWER,
                "the lower half stands at the clicked block");
        helper.assertTrue(upper.getBlock() instanceof FluidStationBlock && upper.getValue(FluidStationBlock.HALF) == DoubleBlockHalf.UPPER,
                "the upper half stands above it");
        helper.assertTrue(lower.getValue(FluidStationBlock.FACING) == FRONT && upper.getValue(FluidStationBlock.FACING) == FRONT,
                "both halves face the player");
        FluidInboundBlockEntity station = FluidStationFixtures.station(helper, STATION);
        helper.assertTrue(station.tankCount() == ShuttlesStatsManager.INSTANCE.fluidStationTanks(), "the station has the configured tanks");
        helper.succeed();
    }

    /** A block where the upper half would go: nothing is placed and the item stays in the hand. */
    @ServusGameTest(timeoutTicks = 40)
    public static void refusedWithoutRoom(GameTestHelper helper) {
        WarehouseFixtures.stone(helper, STATION.below());
        WarehouseFixtures.stone(helper, STATION.above());
        ItemStack stack = FluidStationFixtures.item();
        InteractionResult result = FluidStationFixtures.use(helper, STATION, FRONT, stack);
        helper.assertTrue(result == InteractionResult.FAIL, "the placement fails, got " + result);
        helper.assertTrue(helper.getBlockState(STATION).isAir(), "nothing is placed");
        helper.assertTrue(stack.getCount() == 1, "the item is not used up");
        helper.succeed();
    }

    /** Breaking the upper half of one station and the lower half of another removes both halves of each and drops one item each. */
    @ServusGameTest(timeoutTicks = 40)
    public static void breakingEitherHalfRemovesBoth(GameTestHelper helper) {
        BlockPos other = STATION.east(4);
        FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.place(helper, other, FRONT);
        helper.destroyBlock(STATION.above());
        helper.destroyBlock(other);
        helper.runAfterDelay(1, () -> {
            for (BlockPos pos : List.of(STATION, other)) {
                helper.assertTrue(helper.getBlockState(pos).isAir() && helper.getBlockState(pos.above()).isAir(), "both halves are gone at " + pos);
                helper.assertTrue(FluidStationFixtures.dropped(helper, pos).size() == 1, "one item drops at " + pos + ", got "
                        + FluidStationFixtures.dropped(helper, pos));
            }
            helper.succeed();
        });
    }

    /** The dropped item keeps 6000 mB of water and a Pull side on tank 2; placed again, the station has both back. */
    @ServusGameTest(timeoutTicks = 60)
    public static void dropKeepsTanksAndSides(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.fill(station, 0, Fluids.WATER, 6000);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.PULL, 1);
        helper.destroyBlock(STATION.above());
        helper.runAfterDelay(1, () -> {
            List<ItemStack> drops = FluidStationFixtures.dropped(helper, STATION);
            helper.assertTrue(drops.size() == 1, "one item drops, got " + drops);
            BlockPos again = new BlockPos(8, 1, 8);
            FluidInboundBlockEntity placed = FluidStationFixtures.place(helper, again, FRONT, drops.getFirst().copy());
            helper.assertTrue(placed.tank(0).getFluid().is(Fluids.WATER) && placed.tank(0).getFluidAmount() == 6000,
                    "the placed station holds 6000 mB of water in tank 1, got " + placed.tank(0).getFluid());
            helper.assertTrue(placed.fluidSides().mode(RelativeSide.LEFT) == SideMode.PULL && placed.fluidSides().tank(RelativeSide.LEFT) == 1,
                    "the left side is Pull on tank 2 again");
            helper.succeed();
        });
    }

    /** Sneaking with the carrot on the upper half dismantles the station into one item that keeps its water. */
    @ServusGameTest(timeoutTicks = 40)
    public static void carrotDismantles(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.fill(station, 1, Fluids.WATER, 3000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        GameTestSteps.useCarrot(helper, player, STATION.above(), FRONT);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(helper.getBlockState(STATION).isAir() && helper.getBlockState(STATION.above()).isAir(), "both halves are gone");
            List<ItemStack> drops = FluidStationFixtures.dropped(helper, STATION);
            helper.assertTrue(drops.size() == 1, "one item drops, got " + drops);
            helper.succeed();
        });
    }

    /** Water and then lava piped into the back of the upper half on All land in tanks 1 and 2, and nothing comes back out. */
    @ServusGameTest(timeoutTicks = 40)
    public static void inputFillsTwoFluidsIntoTwoTanks(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        ResourceHandler<FluidResource> back = FluidStationFixtures.capability(helper, STATION.above(), FRONT.getOpposite());
        helper.assertTrue(back != null, "the back of the upper half takes pipes");
        helper.assertTrue(Transfer.fill(back, new FluidStack(Fluids.WATER, 2000), false) == 2000, "the water goes in");
        helper.assertTrue(Transfer.fill(back, new FluidStack(Fluids.LAVA, 1000), false) == 1000, "the lava goes in");
        helper.assertTrue(station.tank(0).getFluid().is(Fluids.WATER) && station.tank(0).getFluidAmount() == 2000, "tank 1 holds the water");
        helper.assertTrue(station.tank(1).getFluid().is(Fluids.LAVA) && station.tank(1).getFluidAmount() == 1000, "tank 2 holds the lava");
        helper.assertTrue(Transfer.drain(back, 1000, false).isEmpty(), "the duct gets nothing back");
        helper.succeed();
    }

    /** The left side on Pull for tank 2 takes lava from a Fluid Tank beside the lower half into tank 2 only. */
    @ServusGameTest(timeoutTicks = 60)
    public static void pullFillsOnlyChosenTank(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.PULL, 1);
        BlockPos left = STATION.relative(FRONT.getClockWise());
        helper.setBlock(left, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        Transfer.fill(FluidStationFixtures.capability(helper, left, Direction.UP), new FluidStack(Fluids.LAVA, 4000), false);
        Waits.when(helper, () -> station.tank(1).getFluidAmount() == 4000, 40,
                () -> "tank 2 pulls the lava, holds " + station.tank(1).getFluidAmount(), () -> {
            helper.assertTrue(station.tank(1).getFluid().is(Fluids.LAVA), "tank 2 holds lava");
            helper.assertTrue(station.tank(0).isEmpty() && station.tank(2).isEmpty(), "the other tanks stay empty");
            helper.succeed();
        });
    }

    /** A side on Off keeps its duct's arm but takes no fluid. */
    @ServusGameTest(timeoutTicks = 40)
    public static void offSideStaysConnected(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.setSide(station, RelativeSide.RIGHT, SideMode.DISABLED, FluidSides.ALL);
        Direction right = FRONT.getCounterClockWise();
        BlockPos duct = STATION.relative(right);
        GameTestSteps.duct(helper, duct, Line.FLUID);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockProperty(duct, ConnectableBlock.property(right.getOpposite()), true);
            ResourceHandler<FluidResource> side = FluidStationFixtures.capability(helper, STATION, right);
            helper.assertTrue(side != null, "the Off side still offers the capability");
            helper.assertTrue(Transfer.fill(side, new FluidStack(Fluids.WATER, 1000), false) == 0, "the Off side takes nothing");
            helper.assertTrue(station.isEmpty(), "the station stays empty");
            helper.succeed();
        });
    }

    /** Neither half offers fluid on its front, and a duct there draws no arm. */
    @ServusGameTest(timeoutTicks = 40)
    public static void frontHasNoEndpoint(GameTestHelper helper) {
        FluidStationFixtures.place(helper, STATION, FRONT);
        BlockPos duct = STATION.relative(FRONT);
        GameTestSteps.duct(helper, duct, Line.FLUID);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(FluidStationFixtures.capability(helper, STATION, FRONT) == null, "the lower front has no endpoint");
            helper.assertTrue(FluidStationFixtures.capability(helper, STATION.above(), FRONT) == null, "the upper front has no endpoint");
            helper.assertTrue(FluidStationFixtures.capability(helper, STATION, Direction.UP) == null, "the faces between the halves have none");
            helper.assertBlockProperty(duct, ConnectableBlock.property(FRONT.getOpposite()), false);
            helper.succeed();
        });
    }

    /** A duct on a Pull side drains nothing out of a filled station. */
    @ServusGameTest(timeoutTicks = 40)
    public static void ductOnPullDrainsNothing(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.PULL, FluidSides.ALL);
        FluidStationFixtures.fill(station, 0, Fluids.WATER, 5000);
        Direction left = FRONT.getClockWise();
        for (BlockPos half : List.of(STATION, STATION.above())) {
            ResourceHandler<FluidResource> side = FluidStationFixtures.capability(helper, half, left);
            helper.assertTrue(side != null, "the Pull side offers the capability");
            helper.assertTrue(Transfer.drain(side, 1000, false).isEmpty(), "nothing drains out of " + half);
            helper.assertTrue(Transfer.drain(side, new FluidStack(Fluids.WATER, 1000), false).isEmpty(), "no water drains out of " + half);
        }
        helper.assertTrue(station.tank(0).getFluidAmount() == 5000, "the water stays");
        helper.succeed();
    }

    /** A water bucket on an empty station fills tank 1 and leaves an empty bucket; the empty bucket takes it back. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bucketFillsEmptyStation(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBucket(helper, player, new ItemStack(Items.WATER_BUCKET), STATION.above());
        helper.assertTrue(station.tank(0).getFluid().is(Fluids.WATER) && station.tank(0).getFluidAmount() == 1000, "tank 1 holds 1000 mB of water");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the player holds an empty bucket");
        useBucket(helper, player, player.getMainHandItem(), STATION);
        helper.assertTrue(station.isEmpty(), "the bucket took the water back");
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the player holds a water bucket again");
        helper.succeed();
    }

    /** The potato names tank 1 with 6000 of 8000 mB of water and tanks 2 and 3 empty. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsTanks(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.fill(station, 0, Fluids.WATER, 6000);
        BlockPos upper = helper.absolutePos(STATION.above());
        BlockState state = helper.getLevel().getBlockState(upper);
        List<Component> report = ((FluidStationBlock) state.getBlock()).potatoReport(helper.getLevel(), upper, state, Vec3.atCenterOf(upper));
        helper.assertTrue(report.size() == station.tankCount(), "one line per tank, got " + report.size());
        helper.assertTrue(key(report.getFirst()).equals("message.servus_shuttles.potato.fluid_station_tank")
                && args(report.getFirst())[2].equals(6000) && args(report.getFirst())[3].equals(capacity()), "tank 1 has 6000 of its capacity");
        for (Component line : report.subList(1, report.size())) {
            helper.assertTrue(key(line).equals("message.servus_shuttles.potato.fluid_station_empty") && args(line)[1].equals(capacity()),
                    "the other tanks are empty: " + line.getString());
        }
        helper.assertTrue(station.tank(0).getFluidAmount() == 6000, "the potato changes nothing");
        helper.succeed();
    }

    private static String key(Component line) {
        return line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
    }

    private static Object[] args(Component line) {
        return line.getContents() instanceof TranslatableContents contents ? contents.getArgs() : new Object[0];
    }

    /** Uses a fluid container on the front of a half, as a right click there does: the block takes the click first. */
    private static void useBucket(GameTestHelper helper, Player player, ItemStack bucket, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        BlockPos absolute = helper.absolutePos(pos);
        helper.getBlockState(pos).useItemOn(bucket, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(FRONT, 0.5), FRONT, absolute, false));
    }
}
