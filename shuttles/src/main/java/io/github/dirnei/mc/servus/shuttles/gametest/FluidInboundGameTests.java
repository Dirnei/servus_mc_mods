package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationTopBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
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
 * keeping tank, sides and wood, the one tank, the Fluids sides of both
 * halves, the lamps on the upper half, buckets, the potato and the hose arm's dock. Stations here face north, so a player in front sees east on the
 * left; the station's back is south.
 */
public final class FluidInboundGameTests {

    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos STATION = new BlockPos(4, 1, 4);
    private static final Identifier BIRCH = BuiltInRegistries.BLOCK.getKey(Blocks.BIRCH_PLANKS);

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
        helper.assertTrue(station.sideSlots().size() == 1 && station.tank().getCapacity() == capacity(), "the station has one tank of the configured capacity");
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

    /** A birch station dropped with 6000 mB of water and a Pull side; placed again, the station has water, side and birch back on both halves. */
    @ServusGameTest(timeoutTicks = 60)
    public static void dropKeepsTankSidesAndWood(GameTestHelper helper) {
        ItemStack birch = FluidStationFixtures.item();
        birch.set(MaterialContent.MATERIAL.get(), BIRCH);
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT, birch);
        helper.assertTrue(BIRCH.equals(station.materialSlot().raw()), "the placed station is birch");
        FluidStationFixtures.fill(station, Fluids.WATER, 6000);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.PULL);
        helper.destroyBlock(STATION.above());
        helper.runAfterDelay(1, () -> {
            List<ItemStack> drops = FluidStationFixtures.dropped(helper, STATION);
            helper.assertTrue(drops.size() == 1, "one item drops, got " + drops);
            helper.assertTrue(BIRCH.equals(drops.getFirst().get(MaterialContent.MATERIAL.get())), "the drop keeps birch");
            BlockPos again = new BlockPos(8, 1, 8);
            FluidInboundBlockEntity placed = FluidStationFixtures.place(helper, again, FRONT, drops.getFirst().copy());
            helper.assertTrue(placed.tank().getFluid().is(Fluids.WATER) && placed.tank().getFluidAmount() == 6000,
                    "the placed station holds 6000 mB of water, got " + placed.tank().getFluid());
            helper.assertTrue(placed.fluidSides().mode(RelativeSide.LEFT) == SideMode.PULL, "the left side is Pull again");
            helper.assertTrue(BIRCH.equals(placed.materialSlot().raw()), "the lower half is birch again");
            helper.assertTrue(helper.getBlockEntity(again.above(), FluidStationTopBlockEntity.class).materialSlot().raw() != null
                    && BIRCH.equals(helper.getBlockEntity(again.above(), FluidStationTopBlockEntity.class).materialSlot().raw()),
                    "the upper half shows birch too");
            helper.succeed();
        });
    }

    /** Sneaking with the carrot on the upper half dismantles the station into one item that keeps its water. */
    @ServusGameTest(timeoutTicks = 40)
    public static void carrotDismantles(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.fill(station, Fluids.WATER, 3000);
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

    /** Water piped into the back of the upper half fills the one tank; lava after it is refused, and nothing comes back out. */
    @ServusGameTest(timeoutTicks = 40)
    public static void oneTankRefusesSecondFluid(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        ResourceHandler<FluidResource> back = FluidStationFixtures.capability(helper, STATION.above(), FRONT.getOpposite());
        helper.assertTrue(back != null, "the back of the upper half takes pipes");
        helper.assertTrue(Transfer.fill(back, new FluidStack(Fluids.WATER, 2000), false) == 2000, "the water goes in");
        helper.assertTrue(Transfer.fill(back, new FluidStack(Fluids.LAVA, 1000), false) == 0, "the lava is refused");
        helper.assertTrue(station.tank().getFluid().is(Fluids.WATER) && station.tank().getFluidAmount() == 2000, "the tank holds the water");
        helper.assertTrue(Transfer.drain(back, 1000, false).isEmpty(), "the duct gets nothing back");
        helper.succeed();
    }

    /** The left side on Pull takes lava from a Fluid Tank beside the lower half. */
    @ServusGameTest(timeoutTicks = 60)
    public static void pullTakesFromFluidTank(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.PULL);
        BlockPos left = STATION.relative(FRONT.getClockWise());
        helper.setBlock(left, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        Transfer.fill(FluidStationFixtures.capability(helper, left, Direction.UP), new FluidStack(Fluids.LAVA, 4000), false);
        Waits.when(helper, () -> station.tank().getFluidAmount() == 4000, 40,
                () -> "the station pulls the lava, holds " + station.tank().getFluidAmount(), () -> {
            helper.assertTrue(station.tank().getFluid().is(Fluids.LAVA), "the tank holds lava");
            helper.succeed();
        });
    }

    /** The upper half takes over the lower half's lamp state, so the item station front shows it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void upperHalfShowsLamps(GameTestHelper helper) {
        FluidStationFixtures.place(helper, STATION, FRONT);
        BlockState lower = helper.getBlockState(STATION);
        helper.setBlock(STATION, lower.setValue(StorageNodeBlocks.ONLINE, true).setValue(StorageNodeBlocks.ACTIVITY,
                StorageNodeBlocks.Activity.BLOCKED));
        BlockState upper = helper.getBlockState(STATION.above());
        helper.assertTrue(upper.getValue(StorageNodeBlocks.ONLINE) && upper.getValue(StorageNodeBlocks.ACTIVITY) == StorageNodeBlocks.Activity.BLOCKED,
                "the upper half shows the lamps, got " + upper);
        helper.succeed();
    }

    /** A side on Off keeps its duct's arm but takes no fluid. */
    @ServusGameTest(timeoutTicks = 40)
    public static void offSideStaysConnected(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.setSide(station, RelativeSide.RIGHT, SideMode.DISABLED);
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
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.PULL);
        FluidStationFixtures.fill(station, Fluids.WATER, 5000);
        Direction left = FRONT.getClockWise();
        for (BlockPos half : List.of(STATION, STATION.above())) {
            ResourceHandler<FluidResource> side = FluidStationFixtures.capability(helper, half, left);
            helper.assertTrue(side != null, "the Pull side offers the capability");
            helper.assertTrue(Transfer.drain(side, 1000, false).isEmpty(), "nothing drains out of " + half);
            helper.assertTrue(Transfer.drain(side, new FluidStack(Fluids.WATER, 1000), false).isEmpty(), "no water drains out of " + half);
        }
        helper.assertTrue(station.tank().getFluidAmount() == 5000, "the water stays");
        helper.succeed();
    }

    /** A water bucket on an empty station fills the tank and leaves an empty bucket; the empty bucket takes it back. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bucketFillsEmptyStation(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBucket(helper, player, new ItemStack(Items.WATER_BUCKET), STATION.above());
        helper.assertTrue(station.tank().getFluid().is(Fluids.WATER) && station.tank().getFluidAmount() == 1000, "the tank holds 1000 mB of water");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the player holds an empty bucket");
        useBucket(helper, player, player.getMainHandItem(), STATION);
        helper.assertTrue(station.isEmpty(), "the bucket took the water back");
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the player holds a water bucket again");
        helper.succeed();
    }

    /** The potato names the tank with 6000 mB of water of its capacity in one line. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsTank(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        FluidStationFixtures.fill(station, Fluids.WATER, 6000);
        BlockPos upper = helper.absolutePos(STATION.above());
        BlockState state = helper.getLevel().getBlockState(upper);
        List<Component> report = ((FluidStationBlock) state.getBlock()).potatoReport(helper.getLevel(), upper, state, Vec3.atCenterOf(upper));
        helper.assertTrue(report.size() == 1, "one line, got " + report.size());
        helper.assertTrue(key(report.getFirst()).equals("message.servus_shuttles.potato.fluid_station_tank")
                && args(report.getFirst())[1].equals(6000) && args(report.getFirst())[2].equals(capacity()), "the tank has 6000 of its capacity");
        helper.assertTrue(station.tank().getFluidAmount() == 6000, "the potato changes nothing");
        helper.succeed();
    }

    /**
     * A 40 tick dock sends the arm out with its start and length in the
     * update tag; ended about 5 ticks in, the arm goes back over as many
     * ticks and then stands up again.
     */
    @ServusGameTest(timeoutTicks = 40)
    public static void armDocksAndEndsEarly(GameTestHelper helper) {
        FluidInboundBlockEntity station = FluidStationFixtures.place(helper, STATION, FRONT);
        helper.assertTrue(!station.armOut(), "a placed station's arm stands up");
        station.dockArm(40);
        long start = helper.getLevel().getGameTime();
        CompoundTag tag = station.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(tag.getIntOr("ArmTicks", 0) == 40 && tag.getLongOr("ArmStart", -1) == start, "clients learn the start and length, got " + tag);
        helper.assertTrue(station.armOut(), "the arm is out");
        helper.runAfterDelay(5, () -> {
            long elapsed = helper.getLevel().getGameTime() - start;
            station.undockArm();
            helper.assertTrue(station.armTicks() == 2 * elapsed,
                    "ended " + elapsed + " ticks in, the arm is back twice that after the dock, got " + station.armTicks());
            helper.assertTrue(station.armOut(), "the arm is still on its way back");
            helper.runAfterDelay((int) elapsed + 2, () -> {
                helper.assertTrue(!station.armOut(), "the arm stands up again");
                helper.succeed();
            });
        });
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
