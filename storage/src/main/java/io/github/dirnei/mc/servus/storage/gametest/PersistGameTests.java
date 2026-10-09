package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.access.AccessPointBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.ProductionLedger;
import io.github.dirnei.mc.servus.storage.order.StepRef;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.TerminalRequests;
import io.github.dirnei.mc.servus.storage.terminal.TerminalWarehouses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Requests, CPU orders, station jobs and shipments survive their block entities being saved and loaded again. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PersistGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos STATION_A = new BlockPos(4, 1, 2);
    private static final BlockPos STATION_B = new BlockPos(5, 1, 2);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    private PersistGameTests() {
    }

    /** Saves the block entity at {@code pos}, puts a fresh one loaded from that tag in its place and returns it. */
    @SuppressWarnings("unchecked")
    static <T extends BlockEntity> T reload(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(pos);
        BlockEntity old = level.getBlockEntity(absolute);
        CompoundTag tag = old.saveWithFullMetadata(level.registryAccess());
        BlockEntity fresh = BlockEntity.loadStatic(absolute, level.getBlockState(absolute), tag, level.registryAccess());
        level.setBlockEntity(fresh);
        level.invalidateCapabilities(absolute);
        return (T) level.getBlockEntity(absolute);
    }

    private static boolean ready(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2)))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 2
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    private static boolean planksIndexed(GameTestHelper helper) {
        ItemKey planks = ItemKey.of(new ItemStack(Items.OAK_PLANKS));
        return TerminalWarehouses.reachable(helper.getLevel(), helper.absolutePos(TerminalWiring.terminalPos(ACCESS))).stream()
                .anyMatch(warehouse -> warehouse.available(planks) >= 8);
    }

    private static CpuBlockEntity cpu(GameTestHelper helper) {
        return CpuBlock.master(helper.getLevel(), helper.absolutePos(CPU));
    }

    private static CraftingStationBlockEntity station(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    /** Open requests come back with their amounts, and a new request gets an id above the saved one. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void terminalRequestsSurviveReload(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 0);
        StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
        StorageTerminalBlockEntity.Request request = terminal.newRequest(helper.getLevel(), STICK, 12);
        request.delivered = 4;
        request.missing = 8;
        int savedId = request.id;
        StorageTerminalBlockEntity loaded = reload(helper, TerminalWiring.terminalPos(ACCESS));
        StorageTerminalBlockEntity.Request back = loaded.request(savedId);
        helper.assertTrue(back != null, "the request should be there after the reload");
        helper.assertTrue(back.key.equals(STICK) && back.requested == 12 && back.delivered == 4 && back.missing == 8,
                "item and amounts should be kept, got " + back.requested + "/" + back.delivered + "/" + back.missing);
        StorageTerminalBlockEntity.Request next = loaded.newRequest(helper.getLevel(), STICK, 1);
        helper.assertTrue(next.id > savedId, "a new id should be above " + savedId + ", got " + next.id);
        helper.succeed();
    }

    /**
     * A craft request runs on the CPU; while a station holds the job, the terminal, the CPU and both
     * stations are saved and loaded again. The order is still open, the job is still there, and the
     * sticks land in the terminal.
     */
    @GameTest(template = EMPTY, timeoutTicks = 1200)
    public static void orderFinishesAfterReload(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 8);
        // A second crate beside the controller takes the crafted sticks; a compartment holds one item type.
        helper.setBlock(new BlockPos(2, 1, 1), ServusStorageBlocks.CRATE.get().defaultBlockState()
                .setValue(CrateBlock.FACING, Direction.NORTH));
        TerminalWiring.when(helper, () -> ready(helper) && planksIndexed(helper), 100,
                () -> "stations, CPU and the planks should be on the line", () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            String refusal = TerminalRequests.craft(helper.getLevel(), terminal, STICK, 4);
            helper.assertTrue(refusal.isEmpty(), "the craft should start, refused " + refusal);
            int orderId = cpu(helper).book().orders().get(0).id();
            TerminalWiring.when(helper, () -> station(helper, STATION_A).working() || station(helper, STATION_B).working(), 200,
                    () -> "a station should get the job", () -> {
                BlockPos busy = station(helper, STATION_A).working() ? STATION_A : STATION_B;
                int left = station(helper, busy).jobLeft();
                reload(helper, TerminalWiring.terminalPos(ACCESS));
                reload(helper, CPU);
                reload(helper, STATION_A);
                reload(helper, STATION_B);
                WorkOrder order = cpu(helper).book().order(orderId);
                helper.assertTrue(order != null && order.open(), "the order should still be open after the reload");
                helper.assertTrue(station(helper, busy).working() && station(helper, busy).jobLeft() == left,
                        "the station should keep its job with " + left + " crafts left");
                TerminalWiring.when(helper, () -> TerminalWiring.at(helper, ACCESS).outputCount(Items.STICK) >= 4
                                && TerminalWiring.at(helper, ACCESS).requests().isEmpty(), 1000,
                        () -> "the sticks should land in the terminal and settle the request, order " + describe(helper, orderId)
                                + ", sticks " + TerminalWiring.at(helper, ACCESS).outputCount(Items.STICK), helper::succeed);
            });
        });
    }

    private static String describe(GameTestHelper helper, int orderId) {
        WorkOrder order = cpu(helper).book().order(orderId);
        return order == null ? "gone" : order.state() + " " + order.reason() + " " + order.steps().get(0).state();
    }

    /** A job whose CPU is loaded but has no such order ends by itself. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void orphanedJobEnds(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 0);
        CraftingStationBlockEntity station = station(helper, STATION_A);
        station.assign(StationJobs.sticks(), 2, new StepRef(helper.absolutePos(CPU), 999, 0));
        helper.assertTrue(station.working(), "the station should take the job");
        TerminalWiring.when(helper, () -> !station.working(), 250, () -> "the orphaned job should end", helper::succeed);
    }

    /**
     * A shipment saved while only half its items sit in the outgoing buffer waits only for those after
     * loading; the rest ends as a shortfall.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void shipmentCappedAfterReload(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 0);
        AccessPointBlockEntity point = helper.getBlockEntity(TerminalWiring.accessPointPos(ACCESS));
        AccessPointBlockEntity.Shipment shipment = point.addShipment(helper.absolutePos(STATION_A), STICK, 16);
        ItemHandlerHelper.insertItemStacked(point.outgoing(), new ItemStack(Items.STICK, 8), false);
        long id = shipment.id();
        AccessPointBlockEntity loaded = reload(helper, TerminalWiring.accessPointPos(ACCESS));
        helper.assertTrue(loaded.shipments().isEmpty() || loaded.shipments().get(0).id() == id, "no other shipment appears");
        ProductionLedger ledger = ProductionLedger.get(helper.getLevel());
        TerminalWiring.when(helper, () -> ledger.transfer(id) != null && ledger.transfer(id).asked() == 8, 150,
                () -> "the shipment should wait for 8, asked " + (ledger.transfer(id) == null ? "gone" : ledger.transfer(id).asked()),
                helper::succeed);
    }
}
