package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidKeep;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidOutboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.SetFluidKeepPayload;
import io.github.dirnei.mc.servus.shuttles.fluidstation.SetFluidLevelPayload;
import io.github.dirnei.mc.servus.shuttles.menu.FluidOutboundMenu;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * The Fluid Outbound as a block: placing, the drop keeping the keep,
 * the tank guarded by its keep fluid, sides that hand fluid out and take
 * none in, not being stock, the keep picker and level payloads, buckets and the potato.
 * Stations here face north, so a player in front sees east on the left; the
 * station's back is south.
 */
public final class FluidOutboundGameTests {

    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos STATION = new BlockPos(4, 1, 4);

    private FluidOutboundGameTests() {
    }

    private static FluidOutboundBlockEntity place(GameTestHelper helper) {
        return FluidStationFixtures.outbound(helper, STATION, FRONT);
    }

    /** A station keeping that fluid and holding that much of it. */
    private static FluidOutboundBlockEntity keeping(GameTestHelper helper, Fluid fluid, int amount) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(fluid);
        FluidStationFixtures.fill(station, fluid, amount);
        return station;
    }

    /** Placed on stone it stands two blocks high with one tank, pushes through its back and keeps every other side Off. */
    @ServusGameTest(timeoutTicks = 40)
    public static void placedWithBackOnPush(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        BlockState upper = helper.getBlockState(STATION.above());
        helper.assertTrue(upper.getBlock() instanceof FluidStationBlock && upper.getValue(FluidStationBlock.HALF) == DoubleBlockHalf.UPPER,
                "the upper half stands above the lower one");
        helper.assertTrue(station.sideSlots().size() == 1
                && station.tank().getCapacity() == ShuttlesStatsManager.INSTANCE.fluidStationTankCapacity(), "the station has one tank");
        helper.assertTrue(station.fluidSides().mode(RelativeSide.BACK) == SideMode.PUSH, "the back pushes");
        for (RelativeSide side : List.of(RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.TOP, RelativeSide.BOTTOM)) {
            helper.assertTrue(station.fluidSides().mode(side) == SideMode.DISABLED, side + " is Off");
        }
        helper.assertTrue(station.fluidSides().isLocked(RelativeSide.FRONT), "the front is fixed");
        helper.assertTrue(!station.keep().isSet(), "a new station keeps nothing");
        helper.succeed();
    }

    /** The dropped item keeps 8000 mB of water with its 5000 mB; placed again, the station keeps and holds them. */
    @ServusGameTest(timeoutTicks = 60)
    public static void breakKeepsKeep(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 5000);
        station.setKeepAmount(8000);
        helper.destroyBlock(STATION.above());
        helper.runAfterDelay(1, () -> {
            List<ItemStack> drops = FluidStationFixtures.dropped(helper, STATION);
            helper.assertTrue(drops.size() == 1, "one item drops, got " + drops);
            FluidOutboundBlockEntity placed = FluidStationFixtures.outbound(helper, new BlockPos(8, 1, 8), FRONT, drops.getFirst().copy());
            helper.assertTrue(placed.keep().equals(new FluidKeep<>(Fluids.WATER, 8000, 1000)),
                    "it still keeps 8000 mB of water, got " + placed.keep());
            helper.assertTrue(placed.tank().getFluidAmount() == 5000, "it still holds 5000 mB");
            helper.assertTrue(placed.fluidSides().mode(RelativeSide.BACK) == SideMode.PUSH, "the back still pushes");
            helper.succeed();
        });
    }

    /** The dropped item keeps a restock level of 3000 mB under a keep of 8000 mB and the restocking flag. */
    @ServusGameTest(timeoutTicks = 60)
    public static void restockLevelSurvivesBreak(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 2000);
        station.setKeepAmount(8000);
        station.setRestockAt(3000);
        station.setRestocking(true);
        CompoundTag tag = station.saveWithFullMetadata(helper.getLevel().registryAccess());
        FluidOutboundBlockEntity reloaded = new FluidOutboundBlockEntity(station.getBlockPos(), station.getBlockState());
        reloaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(reloaded.restocking(), "a reload in the middle of a refill goes on refilling");
        helper.destroyBlock(STATION.above());
        helper.runAfterDelay(1, () -> {
            List<ItemStack> drops = FluidStationFixtures.dropped(helper, STATION);
            helper.assertTrue(drops.size() == 1, "one item drops, got " + drops);
            FluidOutboundBlockEntity placed = FluidStationFixtures.outbound(helper, new BlockPos(8, 1, 8), FRONT, drops.getFirst().copy());
            helper.assertTrue(placed.keep().equals(new FluidKeep<>(Fluids.WATER, 8000, 3000)),
                    "it still keeps 8000 mB of water and restocks at 3000 mB, got " + placed.keep());
            helper.succeed();
        });
    }

    /** A station saved before restock levels existed restocks at one bucket, and at its keep when that is less. */
    @ServusGameTest(timeoutTicks = 20)
    public static void oldSaveRestocksAtKeep(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(Fluids.WATER);
        station.setKeepAmount(6000);
        station.setRestockAt(4000);
        FluidOutboundBlockEntity reloaded = reloadedWithoutRestockLevel(helper, station);
        helper.assertTrue(reloaded.keep().equals(new FluidKeep<>(Fluids.WATER, 6000, 1000)),
                "the old save restocks at 1000 mB, got " + reloaded.keep());
        helper.assertTrue(!reloaded.restocking(), "the old save is not restocking");
        station.setKeepAmount(500);
        helper.assertTrue(reloadedWithoutRestockLevel(helper, station).keep().equals(new FluidKeep<>(Fluids.WATER, 500, 500)),
                "an old keep of 500 mB restocks at 500 mB");
        helper.succeed();
    }

    private static FluidOutboundBlockEntity reloadedWithoutRestockLevel(GameTestHelper helper, FluidOutboundBlockEntity station) {
        CompoundTag tag = station.saveWithFullMetadata(helper.getLevel().registryAccess());
        tag.remove("RestockAt");
        tag.remove("Restocking");
        FluidOutboundBlockEntity reloaded = new FluidOutboundBlockEntity(station.getBlockPos(), station.getBlockState());
        reloaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        return reloaded;
    }

    /** A station keeping 8 B and restocking at 6 B: a restock level of 10 B pushes the keep up to 10 B. */
    @ServusGameTest(timeoutTicks = 20)
    public static void levelPayloadPushesKeepUp(GameTestHelper helper) {
        FluidOutboundMenu menu = keepingLevels(helper, 8000, 6000);
        menu.setLevel(true, 10000);
        helper.assertTrue(menu.keep().equals(new FluidKeep<>(Fluids.WATER, 10000, 10000)),
                "the keep goes up with the restock level, got " + menu.keep());
        helper.succeed();
    }

    /** A station keeping 8 B and restocking at 6 B: a keep of 4 B drags the restock level down to 4 B. */
    @ServusGameTest(timeoutTicks = 20)
    public static void levelPayloadDragsRestockDown(GameTestHelper helper) {
        FluidOutboundMenu menu = keepingLevels(helper, 8000, 6000);
        menu.setLevel(false, 4000);
        helper.assertTrue(menu.keep().equals(new FluidKeep<>(Fluids.WATER, 4000, 4000)),
                "the restock level comes down with the keep, got " + menu.keep());
        menu.setLevel(true, 2000);
        helper.assertTrue(menu.keep().equals(new FluidKeep<>(Fluids.WATER, 4000, 2000)), "a lower restock level leaves the keep alone");
        helper.succeed();
    }

    /** The menu of a station keeping water at those levels, set by the level payload's handler. */
    private static FluidOutboundMenu keepingLevels(GameTestHelper helper, int keep, int restockAt) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(Fluids.WATER);
        FluidOutboundMenu menu = new FluidOutboundMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), station);
        menu.setLevel(false, keep);
        menu.setLevel(true, restockAt);
        helper.assertTrue(station.keep().equals(new FluidKeep<>(Fluids.WATER, keep, restockAt)), "the station keeps the levels set");
        return menu;
    }

    /** A station that kept nothing keeps the full tank of water from a pick, restocked from 1 B; lava then keeps the levels. */
    @ServusGameTest(timeoutTicks = 20)
    public static void newKeepStartsFull(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        int capacity = station.tank().getCapacity();
        FluidOutboundMenu menu = new FluidOutboundMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), station);
        menu.pickKeep(Optional.of(Identifier.withDefaultNamespace("water")));
        helper.assertTrue(station.keep().equals(new FluidKeep<>(Fluids.WATER, capacity, 1000)),
                "a new keep is the full " + capacity + " mB restocked at 1000 mB, got " + station.keep());
        menu.setLevel(false, 8000);
        menu.setLevel(true, 2000);
        menu.pickKeep(Optional.of(Identifier.withDefaultNamespace("lava")));
        helper.assertTrue(station.keep().equals(new FluidKeep<>(Fluids.LAVA, 8000, 2000)), "lava keeps the levels, got " + station.keep());
        helper.succeed();
    }

    /** Levels above the tank's capacity stop at it, and without a keep fluid the payload does nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void levelPayloadLimitedToCapacity(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        int capacity = station.tank().getCapacity();
        FluidOutboundMenu menu = new FluidOutboundMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), station);
        menu.setLevel(false, 4000);
        menu.setLevel(true, 2000);
        helper.assertTrue(station.keep().equals(FluidKeep.none()), "no keep fluid, no levels, got " + station.keep());
        station.setKeepFluid(Fluids.WATER);
        menu.setLevel(false, 40000);
        helper.assertTrue(station.keep().amount() == capacity, "the keep stops at the capacity " + capacity + ", got " + station.keep());
        menu.setLevel(true, 40000);
        helper.assertTrue(station.keep().equals(new FluidKeep<>(Fluids.WATER, capacity, capacity)),
                "the restock level stops at the capacity too, got " + station.keep());
        menu.setLevel(true, -1000);
        helper.assertTrue(station.keep().restockAt() == 0, "and at 0 below, got " + station.keep());
        helper.succeed();
    }

    /** The tank keeps water: a lava bucket puts nothing in, a water bucket fills it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void keepFluidGuardsTheTank(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(Fluids.WATER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBucket(helper, player, new ItemStack(Items.LAVA_BUCKET), STATION);
        helper.assertTrue(station.isEmpty(), "the tank takes no lava");
        helper.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET), "the player keeps the lava bucket");
        useBucket(helper, player, new ItemStack(Items.WATER_BUCKET), STATION.above());
        helper.assertTrue(station.tank().getFluid().is(Fluids.WATER) && station.tank().getFluidAmount() == 1000, "the tank takes the water");
        helper.succeed();
    }

    /** The tank holds 3000 mB of lava when its keep is cleared: the lava stays and drains, and nothing new goes in. */
    @ServusGameTest(timeoutTicks = 40)
    public static void clearedKeepKeepsContent(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(Fluids.LAVA);
        FluidStationFixtures.fill(station, Fluids.LAVA, 3000);
        station.setKeepFluid(null);
        helper.assertTrue(station.tank().getFluidAmount() == 3000, "the lava stays in the tank");
        helper.assertTrue(station.tank().fill(new FluidStack(Fluids.LAVA, 1000), false) == 0, "no more lava goes in");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBucket(helper, player, new ItemStack(Items.BUCKET), STATION);
        helper.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET) && station.tank().getFluidAmount() == 2000,
                "an empty bucket still drains the lava");
        helper.succeed();
    }

    /** Water flows into a Fluid Tank behind the lower half through the back on Push. */
    @ServusGameTest(timeoutTicks = 60)
    public static void pushIntoFluidTank(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 4000);
        BlockPos behind = STATION.relative(FRONT.getOpposite());
        helper.setBlock(behind, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        Waits.when(helper, () -> station.tank().isEmpty(), 50, () -> "the water is pushed out, " + station.tank().getFluidAmount() + " mB left", () -> {
            FluidStack pushed = Transfer.fluidInTank(FluidStationFixtures.capability(helper, behind, Direction.UP), 0);
            helper.assertTrue(pushed.is(Fluids.WATER) && pushed.getAmount() == 4000, "the Fluid Tank holds the water, got " + pushed);
            helper.succeed();
        });
    }

    /** The left side on Output lets a pipe drain lava from both halves and takes nothing in. */
    @ServusGameTest(timeoutTicks = 40)
    public static void outputLetsPipesDrain(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(Fluids.LAVA);
        FluidStationFixtures.fill(station, Fluids.LAVA, 4000);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.OUTPUT);
        Direction left = FRONT.getClockWise();
        ResourceHandler<FluidResource> lower = FluidStationFixtures.capability(helper, STATION, left);
        ResourceHandler<FluidResource> upper = FluidStationFixtures.capability(helper, STATION.above(), left);
        helper.assertTrue(Transfer.drain(lower, 1000, false).is(Fluids.LAVA), "a pipe drains lava from the lower half");
        helper.assertTrue(Transfer.drain(upper, new FluidStack(Fluids.LAVA, 1000), false).getAmount() == 1000, "and from the upper half");
        helper.assertTrue(Transfer.fill(lower, new FluidStack(Fluids.LAVA, 1000), false) == 0, "nothing flows in through the Output side");
        helper.assertTrue(station.tank().getFluidAmount() == 2000, "the tank holds the rest");
        helper.succeed();
    }

    /** With every side on Push or Output and the tank keeping water, no side takes water in. */
    @ServusGameTest(timeoutTicks = 40)
    public static void noFillThroughAnySide(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(Fluids.WATER);
        FluidStationFixtures.setSide(station, RelativeSide.LEFT, SideMode.OUTPUT);
        FluidStationFixtures.setSide(station, RelativeSide.RIGHT, SideMode.PUSH);
        FluidStationFixtures.setSide(station, RelativeSide.TOP, SideMode.OUTPUT);
        FluidStationFixtures.setSide(station, RelativeSide.BOTTOM, SideMode.OUTPUT);
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
        TankerFixtures.fill(station, Fluids.WATER, 6000);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(TankerFixtures.network(helper).fluidStations().contains(helper.absolutePos(TankerFixtures.OUTBOUND)),
                    "the station is a member of the warehouse");
            helper.assertTrue(TankerFixtures.network(helper).fluidIndex().count(FluidKey.of(new FluidStack(Fluids.WATER, 1))) == 0,
                    "the water is no stock");
            helper.assertTrue(TankerFixtures.network(helper).fluidStores().isEmpty(), "the station is no fluid store");
            helper.succeed();
        });
    }

    /** Picking water sets the keep; a keep of 8 B makes 8000 mB; picking nothing clears it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void keepSetByPicker(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidOutboundMenu menu = new FluidOutboundMenu(1, player.getInventory(), station);
        helper.assertTrue(menu.pickKeep(Optional.of(Identifier.withDefaultNamespace("water"))), "water is a keepable fluid");
        menu.setLevel(false, 8000);
        helper.assertTrue(station.keep().equals(new FluidKeep<>(Fluids.WATER, 8000, 1000)),
                "the station keeps 8000 mB of water, got " + station.keep());
        helper.assertTrue(menu.keep().equals(station.keep()), "the menu shows the keep");
        helper.assertTrue(menu.pickKeep(Optional.empty()), "nothing is a valid pick");
        helper.assertTrue(!station.keep().isSet(), "picking nothing clears the keep, got " + station.keep());
        helper.succeed();
    }

    /** An id that names no fluid, or a flowing fluid, is refused and leaves the keep as it was. */
    @ServusGameTest(timeoutTicks = 40)
    public static void unknownKeepFluidRefused(GameTestHelper helper) {
        FluidOutboundBlockEntity station = place(helper);
        station.setKeepFluid(Fluids.WATER);
        station.setKeepAmount(4000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidOutboundMenu menu = new FluidOutboundMenu(1, player.getInventory(), station);
        FluidKeep<Fluid> before = station.keep();
        helper.assertTrue(!menu.pickKeep(Optional.of(Identifier.fromNamespaceAndPath("servus_test", "no_such_fluid"))), "an unknown id is refused");
        helper.assertTrue(!menu.pickKeep(Optional.of(Identifier.withDefaultNamespace("flowing_lava"))), "a flowing fluid is refused");
        helper.assertTrue(!menu.pickKeep(Optional.of(Identifier.withDefaultNamespace("empty"))), "the empty fluid is refused");
        helper.assertTrue(station.keep().equals(before), "the keep stays " + before + ", got " + station.keep());
        helper.succeed();
    }

    /** The payload carries the fluid id through its codec unchanged, and nothing as nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void keepPayloadRoundTrip(GameTestHelper helper) {
        for (SetFluidKeepPayload payload : List.of(new SetFluidKeepPayload(Optional.of(Identifier.withDefaultNamespace("water"))),
                new SetFluidKeepPayload(Optional.empty()))) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            SetFluidKeepPayload.STREAM_CODEC.encode(buf, payload);
            SetFluidKeepPayload decoded = SetFluidKeepPayload.STREAM_CODEC.decode(buf);
            helper.assertTrue(decoded.equals(payload), "the payload " + payload + " comes back as " + decoded);
        }
        helper.succeed();
    }

    /** The level payload carries which level and the mB through its codec unchanged. */
    @ServusGameTest(timeoutTicks = 20)
    public static void levelPayloadRoundTrip(GameTestHelper helper) {
        for (SetFluidLevelPayload payload : List.of(new SetFluidLevelPayload(false, 8000), new SetFluidLevelPayload(true, 0),
                new SetFluidLevelPayload(true, 16000))) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            SetFluidLevelPayload.STREAM_CODEC.encode(buf, payload);
            SetFluidLevelPayload decoded = SetFluidLevelPayload.STREAM_CODEC.decode(buf);
            helper.assertTrue(decoded.equals(payload), "the payload " + payload + " comes back as " + decoded);
        }
        helper.succeed();
    }

    /** The tank holds 5200 mB of water: an empty bucket takes 1000 mB of it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void emptyBucketTakesWater(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 5200);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBucket(helper, player, new ItemStack(Items.BUCKET), STATION.above());
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the player gets a water bucket");
        helper.assertTrue(station.tank().getFluidAmount() == 4200, "the tank holds 4200 mB, got " + station.tank().getFluidAmount());
        helper.succeed();
    }

    /** The potato names the tank with 5200 mB of water keeping 8000 mB of water and restocking at 4000 mB, in one line. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsKeeps(GameTestHelper helper) {
        FluidOutboundBlockEntity station = keeping(helper, Fluids.WATER, 5200);
        station.setKeepAmount(8000);
        station.setRestockAt(4000);
        BlockPos lower = helper.absolutePos(STATION);
        BlockState state = helper.getLevel().getBlockState(lower);
        List<Component> report = ((FluidStationBlock) state.getBlock()).potatoReport(helper.getLevel(), lower, state, Vec3.atCenterOf(lower));
        helper.assertTrue(report.size() == 1, "one line, got " + report.size());
        Component first = report.getFirst();
        helper.assertTrue(key(first).equals("message.servus_shuttles.potato.fluid_station_tank") && args(first)[1].equals(5200),
                "the tank holds 5200 mB: " + first.getString());
        helper.assertTrue(keyOfKeep(first).equals("message.servus_shuttles.potato.fluid_station_keeps_restock")
                        && argsOfKeep(first)[0].equals(8000) && argsOfKeep(first)[2].equals(4000),
                "the tank keeps 8000 mB and restocks at 4000 mB: " + first.getString());
        helper.assertTrue(station.tank().getFluidAmount() == 5200, "the potato changes nothing");
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
