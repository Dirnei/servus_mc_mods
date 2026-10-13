package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorData;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.site.SiteChunks;
import io.github.dirnei.mc.servus.storage.site.SiteRecord;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.SiteWakes;
import io.github.dirnei.mc.servus.storage.site.WakeAllowance;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Site records with last known stock, and waking a site inside its capacity and its team's cap. */
public final class SiteWakeGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    /** For the stock test: a rail east of the controller and a rack east of the rail, facing it. */
    private static final BlockPos RAIL = new BlockPos(3, 1, 2);
    private static final BlockPos RACK = new BlockPos(4, 1, 2);
    /** Every wake test runs with the same short grace time, set by each test because tests of a batch run at once. */
    private static final int GRACE = 20;

    private SiteWakeGameTests() {
    }

    private static WarehouseControllerBlockEntity site(GameTestHelper helper, UUID team, int upgrades) {
        StorageStatsManager.INSTANCE.setWakeGraceTicks(GRACE);
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, CONTROLLER);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        if (team != null) {
            controller.setTeam(team);
        }
        for (int i = 0; i < upgrades; i++) {
            controller.upgrades().insertItem(i, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
        }
        return controller;
    }

    private static Optional<SiteRecord> record(GameTestHelper helper) {
        return SiteRegistry.of(helper.getLevel()).site(helper.getLevel().dimension(), helper.absolutePos(CONTROLLER));
    }

    private static Optional<SiteWakes.Status> status(GameTestHelper helper) {
        return SiteWakes.status(helper.getLevel().dimension(), helper.absolutePos(CONTROLLER));
    }

    private static int loaders(GameTestHelper helper, UUID team) {
        return EnderAnchorData.get(helper.getLevel().getServer()).load(team);
    }

    /** Registers a stand-in loader holding the one far chunk number {@code i} east of {@code base}. */
    private static void farChunk(EnderAnchorData data, ServerLevel level, UUID team, BlockPos base, int i) {
        BlockPos pos = base.offset(i * 16, 0, 0);
        data.register(team, EnderAnchorData.key(level, pos),
                EnderAnchorData.chunks(level.dimension(), java.util.List.of(net.minecraft.world.level.ChunkPos.pack(pos))));
    }

    private static StorageNodeBlocks.Activity lamp(GameTestHelper helper) {
        return helper.getBlockState(CONTROLLER).getValue(StorageNodeBlocks.ACTIVITY);
    }

    /** The record names the site, keeps its chunks and the stock it reported. */
    @ServusGameTest(timeoutTicks = 200)
    public static void recordKeepsNameSpanAndStock(GameTestHelper helper) {
        StorageStatsManager.INSTANCE.setSiteSnapshotInterval(5);
        WarehouseControllerBlockEntity controller = site(helper, UUID.randomUUID(), 0);
        helper.setBlock(RAIL.below(), Blocks.STONE.defaultBlockState());
        TerminalWiring.rail(helper, RAIL);
        helper.setBlock(RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.WEST));
        MiniWarehouse.stock(helper, RACK, new ItemStack(Items.IRON_INGOT, 40));
        controller.setSiteName("Iron Outpost");
        // The rack may stand in the next chunk, so the span is compared, not counted.
        TerminalWiring.when(helper, () -> record(helper).map(site -> site.stock().getOrDefault(ItemKey.of(new ItemStack(Items.IRON_INGOT)), 0L) == 40L
                        && site.span().equals(controller.span())).orElse(false), 100,
                () -> "the record lists 40 iron ingots and the span, had " + record(helper).map(SiteRecord::stock), () -> {
            SiteRecord site = record(helper).orElseThrow();
            helper.assertTrue(site.name().equals("Iron Outpost"), "the record carries the name");
            helper.assertTrue(!site.span().isEmpty() && site.span().contains(controller.homeChunk()), "the record carries the span");
            helper.assertTrue(site.team().equals(controller.team()), "the record carries the team");
            StorageStatsManager.INSTANCE.setSiteSnapshotInterval(StorageStatsManager.DEFAULT_SITE_SNAPSHOT_INTERVAL);
            helper.succeed();
        });
    }

    /** A wake forces the span and counts it in the team's load, the lamp turns orange; after the grace all is released. */
    @ServusGameTest(timeoutTicks = 200)
    public static void wakeLoadsAndReleasesAfterGrace(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        site(helper, team, 1);
        helper.runAfterDelay(3, () -> {
            SiteWakes.Status woken = SiteWakes.request(helper.getLevel(), helper.absolutePos(CONTROLLER)).orElseThrow();
            helper.assertTrue(woken.awake(), "the site wakes, was " + woken);
            helper.assertTrue(woken.forced() == 1, "its one chunk is forced, had " + woken.forced());
            helper.assertTrue(loaders(helper, team) == 1, "the wake counts its one chunk in the team load");
            helper.assertTrue(lamp(helper) == StorageNodeBlocks.Activity.WORKING, "the lamp is orange while awake");
            TerminalWiring.when(helper, () -> status(helper).isEmpty(), GRACE + 40,
                    () -> "the site sleeps after the grace time, was " + status(helper), () -> {
                helper.assertTrue(loaders(helper, team) == 0, "the team load is free again");
                helper.assertTrue(lamp(helper) == StorageNodeBlocks.Activity.IDLE, "the lamp is green again");
                helper.succeed();
            });
        });
    }

    /** A site larger than its upgrades cover never wakes. */
    @ServusGameTest(timeoutTicks = 60)
    public static void overCapacityNeverWakes(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        WarehouseControllerBlockEntity controller = site(helper, team, 1);
        Set<Long> big = new HashSet<>(controller.span());
        for (int i = 0; i < 40; i++) {
            big.add(SiteChunks.key(200000 + i, 200000));
        }
        controller.setSpan(big);
        helper.runAfterDelay(3, () -> {
            SiteWakes.Status refused = SiteWakes.request(helper.getLevel(), helper.absolutePos(CONTROLLER)).orElseThrow();
            helper.assertTrue(!refused.awake() && refused.reason() == WakeAllowance.Reason.OVER_CAPACITY, "over capacity, was " + refused);
            helper.assertTrue(refused.forced() == 0 && loaders(helper, team) == 0, "nothing loads, no load counted");
            helper.succeed();
        });
    }

    /** Without an upgrade a site does not wake. */
    @ServusGameTest(timeoutTicks = 60)
    public static void noUpgradeWaits(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        site(helper, team, 0);
        helper.runAfterDelay(3, () -> {
            SiteWakes.Status refused = SiteWakes.request(helper.getLevel(), helper.absolutePos(CONTROLLER)).orElseThrow();
            helper.assertTrue(!refused.awake() && refused.reason() == WakeAllowance.Reason.NO_UPGRADE, "no upgrade, was " + refused);
            helper.assertTrue(loaders(helper, team) == 0, "no load counted");
            helper.succeed();
        });
    }

    /** A team at its chunk cap waits with the reason and wakes once enough chunks free up while the wake is still held. */
    @ServusGameTest(timeoutTicks = 200)
    public static void teamCapWaitsThenWakes(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        site(helper, team, 1);
        ServerLevel level = helper.getLevel();
        EnderAnchorData data = EnderAnchorData.get(level.getServer());
        int cap = ServusFoundationConfig.maxLoadedChunksPerTeam();
        BlockPos base = helper.absolutePos(CONTROLLER).offset(5000, 0, 0);
        for (int i = 0; i < cap; i++) {
            farChunk(data, level, team, base, i);
        }
        Object token = new Object();
        helper.runAfterDelay(3, () -> {
            SiteWakes.Status waiting = SiteWakes.hold(level, helper.absolutePos(CONTROLLER), token).orElseThrow();
            helper.assertTrue(!waiting.awake() && waiting.reason() == WakeAllowance.Reason.TEAM_LIMIT, "team limit, was " + waiting);
            WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
            helper.assertTrue(controller.wakeAlert() && controller.wakeLine().getString().contains("team limit"),
                    "the GUI says why, was " + controller.wakeLine().getString());
            data.unregister(team, level, base);
            int span = record(helper).orElseThrow().span().size();
            for (int i = 1; i < span; i++) {
                data.unregister(team, level, base.offset(i * 16, 0, 0));
            }
            TerminalWiring.when(helper, () -> status(helper).map(SiteWakes.Status::awake).orElse(false), 20,
                    () -> "free chunks let the held wake through, was " + status(helper), () -> {
                helper.assertTrue(loaders(helper, team) == cap, "the site took the free chunks, load " + loaders(helper, team));
                SiteWakes.release(token);
                for (int i = span; i < cap; i++) {
                    data.unregister(team, level, base.offset(i * 16, 0, 0));
                }
                TerminalWiring.when(helper, () -> status(helper).isEmpty(), GRACE + 40,
                        () -> "released after the grace time, was " + status(helper), () -> {
                    helper.assertTrue(loaders(helper, team) == 0, "no load left");
                    helper.succeed();
                });
            });
        });
    }

    /**
     * A site whose span shares chunks with an anchor of the same team adds only
     * its other chunks to the team load, and takes them away again when it sleeps.
     */
    @ServusGameTest(timeoutTicks = 200)
    public static void wakeOverlapWithAnchorCountedOnce(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        WarehouseControllerBlockEntity controller = site(helper, team, 1);
        BlockPos anchorPos = CONTROLLER.above(2);
        helper.setBlock(anchorPos, MachineContent.ENDER_ANCHOR.get().defaultBlockState());
        EnderAnchorBlockEntity anchor = helper.getBlockEntity(anchorPos, EnderAnchorBlockEntity.class);
        anchor.setOwner(team);
        anchor.energyStorage().receiveEnergy(20000, false);
        long home = controller.homeChunk();
        // The anchor holds the home chunk; a far chunk that nothing loads stays in the span until the wake loads it.
        controller.setSpan(Set.of(home, SiteChunks.key(SiteChunks.x(home) + 2000, SiteChunks.z(home))));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(anchor.status() == EnderAnchorBlockEntity.STATUS_ACTIVE, "the anchor is active, status " + anchor.status());
            helper.assertTrue(loaders(helper, team) == 1, "the anchor holds its chunk, load " + loaders(helper, team));
            helper.assertTrue(record(helper).orElseThrow().span().size() == 2, "the site spans two chunks");
            SiteWakes.Status woken = SiteWakes.request(helper.getLevel(), helper.absolutePos(CONTROLLER)).orElseThrow();
            helper.assertTrue(woken.awake(), "the site wakes, was " + woken);
            helper.assertTrue(loaders(helper, team) == 2, "the load grows by the one chunk the anchor does not hold, was " + loaders(helper, team));
            TerminalWiring.when(helper, () -> status(helper).isEmpty(), GRACE + 60,
                    () -> "the site sleeps after the grace time, was " + status(helper), () -> {
                helper.assertTrue(loaders(helper, team) == 1, "only the anchor is left, load " + loaders(helper, team));
                helper.succeed();
            });
        });
    }

    private static final BlockPos SH_CONTROLLER = new BlockPos(2, 1, 6);
    private static final BlockPos SH_TERMINAL = new BlockPos(3, 1, 7);
    private static final BlockPos SH_FAR_RACK = new BlockPos(9, 1, 7);
    private static final BlockPos SH_GARAGE = new BlockPos(3, 1, 5);

    /** A shuttle on its way keeps a woken site awake past the grace time; it sleeps once the work is done. */
    @ServusGameTest(timeoutTicks = 600)
    public static void shuttleJobKeepsSiteAwake(GameTestHelper helper) {
        StorageStatsManager.INSTANCE.setWakeGraceTicks(GRACE);
        UUID team = UUID.randomUUID();
        helper.setBlock(SH_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, SH_CONTROLLER);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(SH_CONTROLLER, WarehouseControllerBlockEntity.class);
        controller.setTeam(team);
        controller.upgrades().insertItem(0, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
        TerminalWiring.place(helper, SH_TERMINAL);
        for (int x = 3; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 0, 6), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 6), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(SH_FAR_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        MiniWarehouse.stock(helper, SH_FAR_RACK, new ItemStack(Items.COBBLESTONE, 40));
        helper.setBlock(SH_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(SH_GARAGE, ShuttleGarageBlockEntity.class);
        helper.assertTrue(garage.addShuttle(), "one shuttle fits");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(SH_CONTROLLER);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SH_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(SiteWakes.request(helper.getLevel(), at).orElseThrow().awake(), "the site wakes");
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 40);
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.runAfterDelay(GRACE + 10, () -> {
                helper.assertTrue(network.siteBusy(), "the shuttle is still on its way");
                helper.assertTrue(SiteWakes.isAwake(helper.getLevel().dimension(), at), "the busy site stays awake past the grace time");
            });
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 40, 500,
                    () -> "the cobblestone arrives, had " + terminal.outputCount(Items.COBBLESTONE), () ->
                    TerminalWiring.when(helper, () -> SiteWakes.status(helper.getLevel().dimension(), at).isEmpty(), GRACE + 200,
                            () -> "the site sleeps once the shuttle is home and the grace is over", () -> {
                        helper.assertTrue(loaders(helper, team) == 0, "the slot is free");
                        helper.succeed();
                    }));
        });
    }

    /** A broken controller takes its record and its wake along. */
    @ServusGameTest(timeoutTicks = 60)
    public static void brokenControllerDropsRecordAndWake(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        site(helper, team, 1);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(record(helper).isPresent(), "the site is recorded");
            helper.assertTrue(SiteWakes.request(helper.getLevel(), helper.absolutePos(CONTROLLER)).orElseThrow().awake(), "the site wakes");
            helper.setBlock(CONTROLLER, Blocks.AIR.defaultBlockState());
            helper.assertTrue(record(helper).isEmpty(), "the record is gone");
            helper.assertTrue(status(helper).isEmpty() && loaders(helper, team) == 0, "the wake and its slot are gone");
            helper.succeed();
        });
    }

    /** The wake command wakes a recorded site and names the reason when it cannot. */
    @ServusGameTest(timeoutTicks = 60)
    public static void wakeCommandReports(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        site(helper, team, 1);
        helper.runAfterDelay(3, () -> {
            BlockPos at = helper.absolutePos(CONTROLLER);
            var source = helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()).withSuppressedOutput();
            helper.getLevel().getServer().getCommands().performPrefixedCommand(source,
                    "servus_warehouse wake " + at.getX() + " " + at.getY() + " " + at.getZ());
            helper.assertTrue(SiteWakes.isAwake(helper.getLevel().dimension(), at), "the command woke the site");
            helper.getLevel().getServer().getCommands().performPrefixedCommand(source, "servus_warehouse sites");
            helper.succeed();
        });
    }
}
