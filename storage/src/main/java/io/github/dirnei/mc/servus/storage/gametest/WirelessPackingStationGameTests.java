package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.ProductionLedger;
import io.github.dirnei.mc.servus.storage.packing.WirelessPackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.site.RemoteSite;
import io.github.dirnei.mc.servus.storage.site.SiteChunks;
import io.github.dirnei.mc.servus.storage.site.SiteRecord;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.SiteWakes;
import io.github.dirnei.mc.servus.storage.terminal.RemoteTransfers;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.SiteRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Two sites in one test area, linked by Wireless Packing Stations of one team:
 * site A (controller, crate, rails, a Packing Station with a terminal on the
 * data line, a Wireless Packing Station) and site B (controller, crate with
 * iron, rails, a Wireless Packing Station), six blocks apart.
 */
public final class WirelessPackingStationGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos A_CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos A_CRATE = new BlockPos(1, 1, 2);
    private static final BlockPos A_PACKING = new BlockPos(3, 1, 3);
    private static final BlockPos A_WIRELESS = new BlockPos(4, 1, 3);
    private static final BlockPos B_CONTROLLER = new BlockPos(2, 1, 8);
    private static final BlockPos B_CRATE = new BlockPos(1, 1, 8);
    private static final BlockPos B_WIRELESS = new BlockPos(4, 1, 9);
    /** Every test sets the same numbers, because the tests of a batch run at once: 2 FE per item plus 0.5 per block. */
    private static final double FE_PER_ITEM = 2.0;
    private static final double FE_PER_BLOCK = 0.5;
    /** The stations are 6 blocks apart: 2 + 0.5 x 6 = 5 FE per item. */
    private static final int COST = 5;
    /** What the outpost's station holds: powered, and it never spends it as the sender. */
    private static final int SENDER_CHARGE = 100;

    private WirelessPackingStationGameTests() {
    }

    private record Sites(UUID team, WarehouseControllerBlockEntity a, WarehouseControllerBlockEntity b, CrateBlockEntity crateA,
                         CrateBlockEntity crateB, WirelessPackingStationBlockEntity wirelessA, WirelessPackingStationBlockEntity wirelessB) {
    }

    private static WarehouseControllerBlockEntity controller(GameTestHelper helper, BlockPos pos, UUID team) {
        helper.setBlock(pos, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(pos, WarehouseControllerBlockEntity.class);
        controller.setTeam(team);
        return controller;
    }

    private static CrateBlockEntity crate(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos, CrateBlockEntity.class);
    }

    private static WirelessPackingStationBlockEntity wireless(GameTestHelper helper, BlockPos pos, UUID team) {
        helper.setBlock(pos, ServusStorageBlocks.WIRELESS_PACKING_STATION.get().defaultBlockState());
        WirelessPackingStationBlockEntity station = helper.getBlockEntity(pos, WirelessPackingStationBlockEntity.class);
        station.setTeam(team);
        return station;
    }

    /** Rails east of a controller at x 3 and 4 on stone. */
    private static void rails(GameTestHelper helper, BlockPos controller) {
        for (int x = 3; x <= 4; x++) {
            BlockPos rail = new BlockPos(x, controller.getY(), controller.getZ());
            helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
            TerminalWiring.rail(helper, rail);
        }
    }

    /** Both sites; site B's station may belong to another team. */
    private static Sites sites(GameTestHelper helper, UUID teamB) {
        StorageStatsManager.INSTANCE.setWirelessCost(FE_PER_ITEM, FE_PER_BLOCK);
        UUID team = UUID.randomUUID();
        WarehouseControllerBlockEntity a = controller(helper, A_CONTROLLER, team);
        CrateBlockEntity crateA = crate(helper, A_CRATE);
        rails(helper, A_CONTROLLER);
        TerminalWiring.place(helper, A_PACKING);
        WirelessPackingStationBlockEntity wirelessA = wireless(helper, A_WIRELESS, team);
        WarehouseControllerBlockEntity b = controller(helper, B_CONTROLLER, team);
        CrateBlockEntity crateB = crate(helper, B_CRATE);
        crateB.insert(new ItemStack(Items.IRON_INGOT, 32), false);
        rails(helper, B_CONTROLLER);
        WirelessPackingStationBlockEntity wirelessB = wireless(helper, B_WIRELESS, teamB == null ? team : teamB);
        // The sender pays nothing but must be powered to be on the wireless line.
        wirelessB.setEnergy(SENDER_CHARGE);
        return new Sites(team, a, b, crateA, crateB, wirelessA, wirelessB);
    }

    private static int count(CrateBlockEntity crate, Item item) {
        return crate.isEmpty() || !crate.type(0).is(item) ? 0 : crate.count();
    }

    private static String key(GameTestHelper helper, BlockPos controller) {
        return SiteRecord.key(helper.getLevel().dimension(), helper.absolutePos(controller));
    }

    private static StorageTerminalBlockEntity terminal(GameTestHelper helper) {
        return TerminalWiring.at(helper, A_PACKING);
    }

    /** Whether the outpost's warehouse has indexed its crate. */
    private static boolean indexed(Sites sites) {
        return sites.b().network().map(network -> network.index().count(ItemKey.of(new ItemStack(Items.IRON_INGOT))) == 32).orElse(false);
    }

    /** The terminal's row of a site, if it lists it. */
    private static Optional<SiteRow> row(GameTestHelper helper, String site) {
        return RemoteTransfers.sites(helper.getLevel(), terminal(helper).getBlockPos()).stream().filter(row -> row.key().equals(site)).findFirst();
    }

    private static boolean lit(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ONLINE);
    }

    private static long stock(StockSyncPayload payload, Item item) {
        return payload.entries().stream().filter(entry -> entry.stack().is(item)).mapToLong(StockEntry::count).sum();
    }

    /** A data duct beside the station shows no arm and the station is no member. */
    @ServusGameTest(timeoutTicks = 60)
    public static void wirelessStationIsNoDataMember(GameTestHelper helper) {
        WirelessPackingStationBlockEntity station = wireless(helper, A_WIRELESS, UUID.randomUUID());
        TerminalWiring.dataDuct(helper, A_WIRELESS.above());
        helper.runAfterDelay(5, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(DataNetworks.networkAt(level, station.getBlockPos()).isEmpty(), "the station has no data network");
            boolean member = DataNetworks.networkAt(level, helper.absolutePos(A_WIRELESS.above()))
                    .map(network -> network.dataMembers().stream().anyMatch(m -> m.pos().equals(station.getBlockPos()))).orElse(false);
            helper.assertTrue(!member, "the duct's network does not list the station");
            helper.succeed();
        });
    }

    /** Iron leaves the outpost's crate, arrives in the base's crate, and the base pays 5 FE per item. */
    @ServusGameTest(timeoutTicks = 300)
    public static void transferMovesItemsAndCostsEnergy(GameTestHelper helper) {
        Sites sites = sites(helper, null);
        sites.wirelessA().setEnergy(1000);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork outpost = sites.b().network().orElseThrow();
            WarehouseNetwork.Shipping shipping = outpost.shipWireless(sites.wirelessA().getBlockPos(), ItemKey.of(new ItemStack(Items.IRON_INGOT)),
                    16, 0, sites.team());
            helper.assertTrue(shipping.shipped(), "the outpost ships, refusal " + shipping.refusal());
            long id = shipping.shipment().orElseThrow().id();
            TerminalWiring.when(helper, () -> count(sites.crateA(), Items.IRON_INGOT) == 16, 200,
                    () -> "16 iron arrive in the base crate, had " + count(sites.crateA(), Items.IRON_INGOT), () -> {
                helper.assertTrue(count(sites.crateB(), Items.IRON_INGOT) == 16, "16 iron are left at the outpost");
                helper.assertTrue(sites.wirelessA().energyStored() == 1000 - 16 * COST,
                        "the base paid 80 FE, has " + sites.wirelessA().energyStored());
                ProductionLedger.Transfer transfer = ProductionLedger.get(helper.getLevel()).transfer(id);
                helper.assertTrue(transfer != null && transfer.ended() && transfer.delivered() == 16, "the transfer ended with 16 delivered");
                helper.succeed();
            });
        });
    }

    /** Without FE at the receiver nothing moves and the stations say why; FE lets it through. */
    @ServusGameTest(timeoutTicks = 300)
    public static void noPowerNoTransfer(GameTestHelper helper) {
        Sites sites = sites(helper, null);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork outpost = sites.b().network().orElseThrow();
            helper.assertTrue(outpost.shipWireless(sites.wirelessA().getBlockPos(), ItemKey.of(new ItemStack(Items.IRON_INGOT)),
                    8, 0, sites.team()).shipped(), "the outpost ships");
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(count(sites.crateA(), Items.IRON_INGOT) == 0, "nothing arrives without power");
                helper.assertTrue(sites.wirelessB().status().alert() && sites.wirelessB().status().line().getString().contains("power"),
                        "the sender says the base needs power, was " + sites.wirelessB().status().line().getString());
                Transfer.receiveEnergy(sites.wirelessA().energy(), 500, false);
                TerminalWiring.when(helper, () -> count(sites.crateA(), Items.IRON_INGOT) == 8, 200,
                        () -> "with power the iron arrives, had " + count(sites.crateA(), Items.IRON_INGOT), helper::succeed);
            });
        });
    }

    /** A station of another team is no wireless line for this team. */
    @ServusGameTest(timeoutTicks = 100)
    public static void otherTeamRefused(GameTestHelper helper) {
        Sites sites = sites(helper, UUID.randomUUID());
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork outpost = sites.b().network().orElseThrow();
            WarehouseNetwork.Shipping shipping = outpost.shipWireless(sites.wirelessA().getBlockPos(), ItemKey.of(new ItemStack(Items.IRON_INGOT)),
                    8, 0, sites.team());
            helper.assertTrue(!shipping.shipped() && shipping.refusal().equals("no_wireless_station"), "refused, was " + shipping.refusal());
            helper.assertTrue(RemoteTransfers.sites(helper.getLevel(), terminal(helper).getBlockPos()).isEmpty(),
                    "the terminal lists no site without a station of its team");
            helper.succeed();
        });
    }

    /** The terminal lists the outpost, shows its live stock, a sleeping site's last known stock, and refuses an unreachable one. */
    @ServusGameTest(timeoutTicks = 200)
    public static void terminalListsSitesAndRemoteStock(GameTestHelper helper) {
        Sites sites = sites(helper, null);
        sites.wirelessA().setEnergy(1000);
        ServerLevel level = helper.getLevel();
        SiteRegistry registry = SiteRegistry.of(level);
        BlockPos far = helper.absolutePos(new BlockPos(3, 1, 3)).offset(160000, 0, 0);
        BlockPos farther = far.offset(0, 0, 64);
        registry.remember(level.dimension(), far, "Gold Camp", sites.team(), Set.of(SiteChunks.ofBlock(far.getX(), far.getZ())), 1,
                Map.of(ItemKey.of(new ItemStack(Items.GOLD_INGOT)), 40L), Set.of(sites.team()));
        registry.remember(level.dimension(), farther, "Lost Camp", sites.team(), Set.of(SiteChunks.ofBlock(farther.getX(), farther.getZ())), 0,
                Map.of(), Set.of(sites.team()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String outpost = key(helper, B_CONTROLLER);
        TerminalWiring.when(helper, () -> indexed(sites) && RemoteTransfers.sites(level, terminal(helper).getBlockPos()).stream()
                .anyMatch(row -> row.key().equals(outpost)),
                60, () -> "the terminal lists the outpost, had " + RemoteTransfers.sites(level, terminal(helper).getBlockPos()), () -> {
            List<SiteRow> rows = RemoteTransfers.sites(level, terminal(helper).getBlockPos());
            helper.assertTrue(rows.size() == 3, "outpost, gold camp and lost camp are listed, had " + rows);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(outpost);
            StockSyncPayload live = menu.stock();
            helper.assertTrue(live.view().equals(outpost) && live.viewState() == RemoteSite.State.ONLINE.ordinal(), "the outpost is online");
            helper.assertTrue(stock(live, Items.IRON_INGOT) == 32, "its live stock shows 32 iron");
            String gold = SiteRecord.key(level.dimension(), far);
            menu.selectSite(gold);
            StockSyncPayload sleeping = menu.stock();
            helper.assertTrue(sleeping.viewState() == RemoteSite.State.SLEEPING.ordinal() && stock(sleeping, Items.GOLD_INGOT) == 40,
                    "the sleeping camp shows its last known 40 gold");
            menu.selectSite(SiteRecord.key(level.dimension(), farther));
            helper.assertTrue(menu.stock().viewState() == RemoteSite.State.UNREACHABLE.ordinal(), "the camp without upgrades is unreachable");
            menu.request(ItemKey.of(new ItemStack(Items.GOLD_INGOT)), 4);
            helper.assertTrue(menu.stock().message().equals("site_asleep"), "a request there is refused, was " + menu.stock().message());
            menu.selectSite("");
            helper.assertTrue(menu.stock().view().isEmpty(), "back to this site");
            registry.remove(level.dimension(), far);
            registry.remove(level.dimension(), farther);
            helper.succeed();
        });
    }

    /** A request on the outpost becomes a transfer: listed, put away at the base, then gone. */
    @ServusGameTest(timeoutTicks = 400)
    public static void remoteRequestArrivesInStorage(GameTestHelper helper) {
        Sites sites = sites(helper, null);
        sites.wirelessA().setEnergy(10000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String outpost = key(helper, B_CONTROLLER);
        TerminalWiring.when(helper, () -> indexed(sites) && RemoteTransfers.sites(helper.getLevel(), terminal(helper).getBlockPos()).stream()
                .anyMatch(row -> row.key().equals(outpost)), 60, () -> "the outpost is listed", () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(outpost);
            menu.request(ItemKey.of(new ItemStack(Items.IRON_INGOT)), 20);
            StockSyncPayload after = menu.stock();
            helper.assertTrue(after.message().isEmpty(), "the transfer is made, message " + after.message());
            helper.assertTrue(after.requests().size() == 1 && after.requests().get(0).requested() == 20, "the transfer is listed");
            TerminalWiring.when(helper, () -> count(sites.crateA(), Items.IRON_INGOT) == 20 && terminal(helper).requests().isEmpty(), 300,
                    () -> "20 iron arrive at the base and the transfer leaves the list, had " + count(sites.crateA(), Items.IRON_INGOT)
                            + " and " + terminal(helper).requests().size() + " requests", () -> {
                helper.assertTrue(terminal(helper).outputCount(Items.IRON_INGOT) == 0, "nothing lands in the output slots");
                helper.assertTrue(count(sites.crateB(), Items.IRON_INGOT) == 12, "12 iron stay at the outpost");
                helper.succeed();
            });
        });
    }

    /** A sleeping site that can wake is held awake while its transfer waits; cancelling lets it sleep. */
    @ServusGameTest(timeoutTicks = 300)
    public static void wakeableSiteHeldForRequest(GameTestHelper helper) {
        StorageStatsManager.INSTANCE.setWakeGraceTicks(20);
        Sites sites = sites(helper, null);
        sites.wirelessA().setEnergy(1000);
        ServerLevel level = helper.getLevel();
        // A recorded site whose controller is not there: it sleeps, and its one chunk is the test area's.
        BlockPos camp = helper.absolutePos(new BlockPos(10, 1, 10));
        SiteRegistry.of(level).remember(level.dimension(), camp, "Quiet Camp", sites.team(), Set.of(SiteChunks.ofBlock(camp.getX(), camp.getZ())),
                1, Map.of(ItemKey.of(new ItemStack(Items.COAL)), 12L), Set.of(sites.team()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String key = SiteRecord.key(level.dimension(), camp);
        TerminalWiring.when(helper, () -> RemoteTransfers.sites(level, terminal(helper).getBlockPos()).stream().anyMatch(row -> row.key().equals(key)),
                60, () -> "the camp is listed", () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(key);
            menu.request(ItemKey.of(new ItemStack(Items.COAL)), 6);
            StockSyncPayload after = menu.stock();
            List<RequestRow> rows = after.requests();
            helper.assertTrue(after.message().isEmpty() && rows.size() == 1 && rows.get(0).state().equals("WAKING"),
                    "the transfer waits for the camp to wake, was " + after.message() + " " + rows);
            helper.assertTrue(SiteWakes.isAwake(level.dimension(), camp), "the camp is woken for it");
            menu.cancel(rows.get(0).id());
            helper.assertTrue(terminal(helper).requests().isEmpty(), "the transfer is cancelled");
            TerminalWiring.when(helper, () -> SiteWakes.status(level.dimension(), camp).isEmpty(), 80,
                    () -> "the camp sleeps again after the grace, was " + SiteWakes.status(level.dimension(), camp), () -> {
                SiteRegistry.of(level).remove(level.dimension(), camp);
                helper.succeed();
            });
        });
    }

    /** A request for this site's own stock still goes through the Packing Station on the data line. */
    @ServusGameTest(timeoutTicks = 200)
    public static void dataShipmentIgnoresWirelessStation(GameTestHelper helper) {
        Sites sites = sites(helper, null);
        sites.crateA().insert(new ItemStack(Items.COBBLESTONE, 24), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 8);
            TerminalWiring.when(helper, () -> terminal(helper).outputCount(Items.COBBLESTONE) == 8, 150,
                    () -> "8 cobblestone arrive in the output, had " + terminal(helper).outputCount(Items.COBBLESTONE), () -> {
                helper.assertTrue(sites.wirelessA().shipments().isEmpty(), "the wireless station took no part");
                helper.succeed();
            });
        });
    }

    /** An unpowered outpost station takes the outpost off the wireless line and refuses requests; FE brings it back. */
    @ServusGameTest(timeoutTicks = 400)
    public static void unpoweredStationRefusesRequest(GameTestHelper helper) {
        Sites sites = sites(helper, null);
        sites.wirelessA().setEnergy(10000);
        sites.wirelessB().setEnergy(0);
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String outpost = key(helper, B_CONTROLLER);
        ItemKey iron = ItemKey.of(new ItemStack(Items.IRON_INGOT));
        TerminalWiring.when(helper, () -> indexed(sites) && row(helper, outpost).filter(SiteRow::unpowered).isPresent(), 60,
                () -> "the outpost is listed off the wireless line, had " + row(helper, outpost), () -> {
            SiteRecord record = SiteRegistry.of(level).site(outpost).orElseThrow();
            helper.assertTrue(record.hasStation(sites.team()) && !record.hasWireless(sites.team()), "the record keeps the station as unpowered");
            helper.assertTrue(!lit(helper, B_WIRELESS), "the unpowered station's lamp is dark");
            helper.assertTrue(sites.wirelessB().status().alert() && sites.wirelessB().status().line().getString().contains("power"),
                    "its status says no power, was " + sites.wirelessB().status().line().getString());
            WarehouseNetwork.Shipping direct = sites.b().network().orElseThrow().shipWireless(sites.wirelessA().getBlockPos(), iron, 8, 0, sites.team());
            helper.assertTrue(!direct.shipped() && direct.refusal().equals("no_power_there"), "the outpost refuses to ship, was " + direct.refusal());
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(outpost);
            menu.request(iron, 8);
            helper.assertTrue(menu.stock().message().equals("no_power_there"), "the request is refused, was " + menu.stock().message());
            helper.assertTrue(terminal(helper).requests().isEmpty(), "nothing is listed");
            Transfer.receiveEnergy(sites.wirelessB().energy(), 500, false);
            TerminalWiring.when(helper, () -> row(helper, outpost).filter(SiteRow::wireless).isPresent() && lit(helper, B_WIRELESS), 40,
                    () -> "with FE the outpost is back on the wireless line, had " + row(helper, outpost), () -> {
                menu.request(iron, 8);
                helper.assertTrue(menu.stock().message().isEmpty(), "the request is made, was " + menu.stock().message());
                TerminalWiring.when(helper, () -> count(sites.crateA(), Items.IRON_INGOT) == 8, 200,
                        () -> "8 iron arrive at the base, had " + count(sites.crateA(), Items.IRON_INGOT), helper::succeed);
            });
        });
    }

    /** A base whose only station is unpowered cannot receive: the request is refused at the base. */
    @ServusGameTest(timeoutTicks = 200)
    public static void unpoweredReceiverRefusesRequest(GameTestHelper helper) {
        Sites sites = sites(helper, null);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String outpost = key(helper, B_CONTROLLER);
        TerminalWiring.when(helper, () -> indexed(sites) && row(helper, outpost).filter(SiteRow::wireless).isPresent(), 60,
                () -> "the outpost is listed", () -> {
            helper.assertTrue(!lit(helper, A_WIRELESS), "the base station's lamp is dark");
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(outpost);
            menu.request(ItemKey.of(new ItemStack(Items.IRON_INGOT)), 8);
            helper.assertTrue(menu.stock().message().equals("no_power_here"), "the request is refused, was " + menu.stock().message());
            helper.assertTrue(terminal(helper).requests().isEmpty(), "nothing is listed");
            helper.succeed();
        });
    }

    /** A running transfer whose receiver loses power stops, shows it, and resumes with FE. */
    @ServusGameTest(timeoutTicks = 500)
    public static void transferWaitsForPower(GameTestHelper helper) {
        Sites sites = sites(helper, null);
        sites.wirelessA().setEnergy(10000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String outpost = key(helper, B_CONTROLLER);
        TerminalWiring.when(helper, () -> indexed(sites) && row(helper, outpost).filter(SiteRow::wireless).isPresent(), 60,
                () -> "the outpost is listed", () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(outpost);
            menu.request(ItemKey.of(new ItemStack(Items.IRON_INGOT)), 24);
            helper.assertTrue(menu.stock().message().isEmpty(), "the transfer is made, was " + menu.stock().message());
            TerminalWiring.when(helper, () -> delivered(helper, terminal(helper)) > 0, 200, () -> "the first iron arrives", () -> {
                sites.wirelessA().setEnergy(0);
                int stopped = delivered(helper, terminal(helper));
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(delivered(helper, terminal(helper)) == stopped, "nothing moves without power");
                    List<RequestRow> rows = menu.stock().requests();
                    helper.assertTrue(rows.size() == 1 && rows.get(0).state().equals("NO_POWER"), "the transfer waits for power, was " + rows);
                    sites.wirelessA().setEnergy(10000);
                    TerminalWiring.when(helper, () -> count(sites.crateA(), Items.IRON_INGOT) == 24 && terminal(helper).requests().isEmpty(), 300,
                            () -> "with FE the rest arrives, had " + count(sites.crateA(), Items.IRON_INGOT), helper::succeed);
                });
            });
        });
    }

    /** Items of the terminal's open transfers that arrived so far. */
    private static int delivered(GameTestHelper helper, StorageTerminalBlockEntity terminal) {
        int sum = 0;
        for (StorageTerminalBlockEntity.Request request : terminal.requests()) {
            sum += request.delivered;
            for (StorageTerminalBlockEntity.Tracked tracked : request.shipments) {
                ProductionLedger.Transfer transfer = ProductionLedger.get(helper.getLevel()).transfer(tracked.shipmentId());
                sum += transfer == null ? 0 : transfer.delivered();
            }
        }
        return sum;
    }
}
