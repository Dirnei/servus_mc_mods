package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.storage.controller.SiteStatusPayload;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.ProductionLedger;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.site.SiteChunks;
import io.github.dirnei.mc.servus.storage.site.SiteRecord;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.SiteWakes;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.terminal.RemoteTransfers;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.SiteRow;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * The controller as the site's antenna: its FE buffer, its Wireless Upgrade
 * slot, and transfers between two sites in one test area through normal
 * Packing Stations. Site A (the base: controller, rails, a rack and a garage
 * with a shuttle north of the rails, a Packing Station with a terminal on the
 * data line) and site B (the outpost: controller, rails, a rack with iron and a
 * garage, a Packing Station without any duct), six blocks apart.
 */
public final class ControllerWirelessGameTests {

    private static final BlockPos A_CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos A_PACKING = new BlockPos(3, 1, 3);
    private static final BlockPos A_SECOND = new BlockPos(4, 1, 3);
    private static final BlockPos B_CONTROLLER = new BlockPos(2, 1, 8);
    private static final BlockPos B_PACKING = new BlockPos(4, 1, 9);
    /** Every test sets the same numbers, because the tests of a batch run at once: 2 FE per item plus 0.5 per block. */
    private static final double FE_PER_ITEM = 2.0;
    private static final double FE_PER_BLOCK = 0.5;
    /** The controllers are 6 blocks apart: 2 + 0.5 x 6 = 5 FE per item. */
    private static final int COST = 5;
    /** What the outpost's controller holds: powered, and it never spends it as the sender. */
    private static final int SENDER_CHARGE = 100;

    private ControllerWirelessGameTests() {
    }

    private record Sites(UUID team, WarehouseControllerBlockEntity a, WarehouseControllerBlockEntity b, HeavyRackBlockEntity rackA,
                         HeavyRackBlockEntity rackB) {
    }

    private static WarehouseControllerBlockEntity controller(GameTestHelper helper, BlockPos pos, UUID team) {
        helper.setBlock(pos, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, pos);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(pos, WarehouseControllerBlockEntity.class);
        controller.setTeam(team);
        return controller;
    }

    private static void upgrade(WarehouseControllerBlockEntity controller) {
        controller.wirelessUpgrade().insertItem(0, new ItemStack(ServusStorageItems.WIRELESS_UPGRADE.get()), false);
    }

    /** A rack north of the controller's first rail and, unless left out, a garage with one shuttle north of the second. */
    private static HeavyRackBlockEntity rackAndGarage(GameTestHelper helper, BlockPos controller, boolean garage) {
        BlockPos rack = new BlockPos(3, controller.getY(), controller.getZ() - 1);
        helper.setBlock(rack, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.SOUTH));
        if (garage) {
            BlockPos at = rack.east();
            helper.setBlock(at, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
            helper.assertTrue(helper.getBlockEntity(at, ShuttleGarageBlockEntity.class).addShuttle(), "the garage takes a shuttle");
        }
        return helper.getBlockEntity(rack, HeavyRackBlockEntity.class);
    }

    /** Rails east of a controller at x 3 and 4 on stone. */
    private static void rails(GameTestHelper helper, BlockPos controller) {
        for (int x = 3; x <= 4; x++) {
            BlockPos rail = new BlockPos(x, controller.getY(), controller.getZ());
            helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
            TerminalWiring.rail(helper, rail);
        }
    }

    /** Both sites, both controllers with a Wireless Upgrade; the base with 1000 FE, the outpost with a small charge. */
    private static Sites sites(GameTestHelper helper) {
        return sites(helper, true, true);
    }

    private static Sites sites(GameTestHelper helper, boolean outpostGarage, boolean outpostPacking) {
        StorageStatsManager.INSTANCE.setWirelessCost(FE_PER_ITEM, FE_PER_BLOCK);
        UUID team = UUID.randomUUID();
        WarehouseControllerBlockEntity a = controller(helper, A_CONTROLLER, team);
        rails(helper, A_CONTROLLER);
        HeavyRackBlockEntity rackA = rackAndGarage(helper, A_CONTROLLER, true);
        TerminalWiring.place(helper, A_PACKING);
        WarehouseControllerBlockEntity b = controller(helper, B_CONTROLLER, team);
        rails(helper, B_CONTROLLER);
        HeavyRackBlockEntity rackB = rackAndGarage(helper, B_CONTROLLER, outpostGarage);
        MiniWarehouse.put(rackB, new ItemStack(Items.IRON_INGOT, 32));
        if (outpostPacking) {
            helper.setBlock(B_PACKING, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        }
        upgrade(a);
        upgrade(b);
        // These tests count the FE of wireless transfers; the shuttles of both sites drive for free.
        PowerFixtures.freeDraws(a);
        PowerFixtures.freeDraws(b);
        a.setEnergy(1000);
        // The sender pays nothing but must be powered to be on the wireless line.
        b.setEnergy(SENDER_CHARGE);
        return new Sites(team, a, b, rackA, rackB);
    }

    private static int count(HeavyRackBlockEntity rack, Item item) {
        return MiniWarehouse.count(rack, item);
    }

    private static String key(GameTestHelper helper, BlockPos controller) {
        return SiteRecord.key(helper.getLevel().dimension(), helper.absolutePos(controller));
    }

    private static StorageTerminalBlockEntity terminal(GameTestHelper helper) {
        return TerminalWiring.at(helper, A_PACKING);
    }

    private static ItemKey iron() {
        return ItemKey.of(new ItemStack(Items.IRON_INGOT));
    }

    /** Whether the outpost's warehouse has indexed its rack. */
    private static boolean indexed(Sites sites) {
        return sites.b().network().map(network -> network.index().count(iron()) == 32).orElse(false);
    }

    /** The terminal's row of a site, if it lists it. */
    private static Optional<SiteRow> row(GameTestHelper helper, String site) {
        return RemoteTransfers.sites(helper.getLevel(), terminal(helper).getBlockPos()).stream().filter(row -> row.key().equals(site)).findFirst();
    }

    /** Waits until the outpost is indexed and listed as able to trade, then runs {@code then}. */
    private static void listed(GameTestHelper helper, Sites sites, Runnable then) {
        String outpost = key(helper, B_CONTROLLER);
        TerminalWiring.when(helper, () -> indexed(sites) && row(helper, outpost).filter(SiteRow::trade).isPresent(), 60,
                () -> "the outpost is listed, had " + row(helper, outpost), then);
    }

    /** Items of the terminal's open transfers that crossed the wireless line so far. */
    private static int delivered(GameTestHelper helper, StorageTerminalBlockEntity terminal) {
        int sum = 0;
        for (StorageTerminalBlockEntity.Request request : terminal.requests()) {
            for (StorageTerminalBlockEntity.Tracked tracked : request.shipments) {
                ProductionLedger.Transfer transfer = ProductionLedger.get(helper.getLevel()).transfer(tracked.shipmentId());
                sum += transfer == null ? 0 : transfer.delivered();
            }
        }
        return sum;
    }

    private static EnergyHandler energyAt(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), side);
    }

    // The FE buffer and the slot

    /** A Coal Generator feeds a power duct; the duct shows an arm toward the controller and its FE rises. */
    @ServusGameTest(timeoutTicks = 200)
    public static void controllerTakesFeFromPowerLine(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(2, 1, 2);
        BlockPos ductPos = generatorPos.east();
        BlockPos controllerPos = ductPos.east();
        WarehouseControllerBlockEntity controller = controller(helper, controllerPos, UUID.randomUUID());
        helper.setBlock(generatorPos, MachineContent.COAL_GENERATOR.get().defaultBlockState());
        helper.getBlockEntity(generatorPos, CoalGeneratorBlockEntity.class).inventory()
                .setStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
        helper.setBlock(ductPos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(ductPos, UtilityDuctBlockEntity.class);
        duct.addLine(Line.ENERGY);
        BlockPos absolute = helper.absolutePos(ductPos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        helper.assertTrue(energyAt(helper, controllerPos, Direction.WEST) != null, "the controller offers an energy handler");
        TerminalWiring.when(helper, () -> controller.energyStored() > 0, 180,
                () -> "the controller's FE rises, has " + controller.energyStored(), () -> {
            helper.assertTrue(duct.hasArm(Line.ENERGY, Direction.EAST), "the duct shows a power arm toward the controller");
            helper.assertTrue(controller.energy().capacity() == StorageStatsManager.INSTANCE.controllerEnergyCapacity(),
                    "the buffer holds controller_energy_capacity");
            helper.succeed();
        });
    }

    /** Nothing can pull FE out of the controller. */
    @ServusGameTest(timeoutTicks = 20)
    public static void controllerGivesNoFe(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = controller(helper, A_CONTROLLER, UUID.randomUUID());
        controller.setEnergy(5000);
        for (Direction side : Direction.values()) {
            EnergyHandler handler = energyAt(helper, A_CONTROLLER, side);
            helper.assertTrue(handler != null, "an energy handler on " + side);
            helper.assertTrue(Transfer.extractEnergy(handler, 1000, false) == 0, "no FE leaves on " + side);
        }
        helper.assertTrue(controller.energyStored() == 5000, "the buffer keeps its 5000 FE, has " + controller.energyStored());
        helper.succeed();
    }

    /** The wireless slot takes one Wireless Upgrade and nothing else. */
    @ServusGameTest(timeoutTicks = 20)
    public static void wirelessSlotTakesOnlyTheUpgrade(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = controller(helper, A_CONTROLLER, UUID.randomUUID());
        var slot = controller.wirelessUpgrade();
        helper.assertTrue(!slot.isItemValid(0, new ItemStack(ServusStorageItems.CHUNK_LOADER_UPGRADE.get())), "no Chunk Loader Upgrade");
        helper.assertTrue(!slot.isItemValid(0, new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get())), "no Speed Upgrade");
        ItemStack left = slot.insertItem(0, new ItemStack(ServusStorageItems.WIRELESS_UPGRADE.get(), 2), false);
        helper.assertTrue(left.getCount() == 1, "one Wireless Upgrade fits, the second does not, left " + left.getCount());
        helper.assertTrue(controller.hasWirelessUpgrade(), "the controller holds a Wireless Upgrade");
        helper.succeed();
    }

    /** Dismantling drops the Wireless Upgrade; the controller item carries no FE and no upgrade. */
    @ServusGameTest(timeoutTicks = 40)
    public static void upgradeDropsWhenDismantled(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        BlockPos again = new BlockPos(8, 1, 8);
        WarehouseControllerBlockEntity controller = controller(helper, at, UUID.randomUUID());
        upgrade(controller);
        controller.setEnergy(12000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        BlockPos pos = helper.absolutePos(at);
        ((ConfigurationCarrotItem) carrot.getItem()).useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, carrot,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)));
        helper.assertBlockNotPresent(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), at);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        long upgrades = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(ServusStorageItems.WIRELESS_UPGRADE.get()))
                .mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(upgrades == 1, "the Wireless Upgrade drops on its own, got " + upgrades);
        ItemStack item = drops.stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(ServusStorageItems.WAREHOUSE_CONTROLLER.get())).findFirst().orElseThrow();
        WarehouseControllerBlockEntity placed = controller(helper, again, UUID.randomUUID());
        BlockItem.updateCustomBlockEntityTag(helper.getLevel(), player, helper.absolutePos(again), item);
        helper.getBlockState(again).getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(again), helper.getBlockState(again), player, item);
        helper.assertTrue(placed.energyStored() == 0, "the item carries no FE, has " + placed.energyStored());
        helper.assertTrue(!placed.hasWirelessUpgrade(), "the item carries no Wireless Upgrade");
        helper.succeed();
    }

    // Transfers

    /** Ten iron go from the outpost's rack to the base's rack; the base pays 5 FE per item, the outpost nothing. */
    @ServusGameTest(timeoutTicks = 600)
    public static void transferMovesItemsAndCostsReceiverFe(GameTestHelper helper) {
        Sites sites = sites(helper);
        TerminalWiring.when(helper, () -> indexed(sites), 40, () -> "the outpost indexes its rack", () -> {
            WarehouseNetwork outpost = sites.b().network().orElseThrow();
            WarehouseNetwork.Shipping shipping = outpost.shipWireless(helper.absolutePos(A_PACKING), key(helper, A_CONTROLLER), iron(), 10, 0);
            helper.assertTrue(shipping.shipped(), "the outpost ships, refusal " + shipping.refusal());
            helper.assertTrue(shipping.packingStation().equals(helper.absolutePos(B_PACKING)), "it ships through the outpost's Packing Station");
            long id = shipping.shipment().orElseThrow().id();
            TerminalWiring.when(helper, () -> count(sites.rackA(), Items.IRON_INGOT) == 10, 440,
                    () -> "10 iron arrive in the base rack, had " + count(sites.rackA(), Items.IRON_INGOT), () -> {
                helper.assertTrue(count(sites.rackB(), Items.IRON_INGOT) == 22, "22 iron are left at the outpost");
                helper.assertTrue(sites.a().energyStored() == 1000 - 10 * COST, "the base paid 50 FE, has " + sites.a().energyStored());
                helper.assertTrue(sites.b().energyStored() == SENDER_CHARGE, "the outpost paid nothing, has " + sites.b().energyStored());
                ProductionLedger.Transfer transfer = ProductionLedger.get(helper.getLevel()).transfer(id);
                helper.assertTrue(transfer != null && transfer.ended() && transfer.delivered() == 10, "the transfer ended with 10 delivered");
                helper.succeed();
            });
        });
    }

    /** A request at the base terminal goes through the outpost's Packing Station, which has no data line; its status is no alert. */
    @ServusGameTest(timeoutTicks = 700)
    public static void transferThroughStationWithoutDataLine(GameTestHelper helper) {
        Sites sites = sites(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        listed(helper, sites, () -> {
            PackingStationBlockEntity sender = helper.getBlockEntity(B_PACKING, PackingStationBlockEntity.class);
            helper.assertTrue(DataNetworks.networkAt(helper.getLevel(), sender.getBlockPos()).isEmpty(), "the outpost's station has no data line");
            helper.assertTrue(!sender.status().alert(), "no data line is no alert while the controller is capable, was "
                    + sender.status().line().getString());
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(key(helper, B_CONTROLLER));
            menu.request(iron(), 20);
            StockSyncPayload after = menu.stock();
            helper.assertTrue(after.message().isEmpty(), "the transfer is made, message " + after.message());
            helper.assertTrue(after.requests().size() == 1 && after.requests().get(0).requested() == 20, "the transfer is listed");
            TerminalWiring.when(helper, () -> terminal(helper).outputCount(Items.IRON_INGOT) == 20 && terminal(helper).requests().isEmpty(), 540,
                    () -> "20 iron arrive in the output slots and the transfer leaves the list, had "
                            + terminal(helper).outputCount(Items.IRON_INGOT) + " and " + terminal(helper).requests().size() + " requests", () -> {
                helper.assertTrue(count(sites.rackA(), Items.IRON_INGOT) == 0, "nothing is put away at the base");
                helper.assertTrue(count(sites.rackB(), Items.IRON_INGOT) == 12, "12 iron stay at the outpost");
                helper.succeed();
            });
        });
    }

    /** An outpost without a shuttle refuses and nothing leaves its rack. */
    @ServusGameTest(timeoutTicks = 100)
    public static void transferWithoutShuttleRefused(GameTestHelper helper) {
        Sites sites = sites(helper, false, true);
        TerminalWiring.when(helper, () -> indexed(sites), 40, () -> "the outpost indexes its rack", () -> {
            WarehouseNetwork.Shipping shipping = sites.b().network().orElseThrow()
                    .shipWireless(helper.absolutePos(A_PACKING), key(helper, A_CONTROLLER), iron(), 8, 0);
            helper.assertTrue(!shipping.shipped() && shipping.refusal().equals("needs_shuttle"), "refused for a shuttle, was " + shipping.refusal());
            helper.assertTrue(count(sites.rackB(), Items.IRON_INGOT) == 32, "the iron stays");
            helper.succeed();
        });
    }

    /** Without a Wireless Upgrade the terminal lists no site and the controller panel shows the hint; without FE the power hint. */
    @ServusGameTest(timeoutTicks = 100)
    public static void noUpgradeNoSites(GameTestHelper helper) {
        Sites sites = sites(helper);
        sites.a().wirelessUpgrade().extractItem(0, 1, false);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(RemoteTransfers.sites(helper.getLevel(), terminal(helper).getBlockPos()).isEmpty(), "the terminal lists no site");
            SiteStatusPayload status = WarehouseControllerMenu.status(sites.a());
            helper.assertTrue(!status.capable() && status.hint() == SiteStatusPayload.HINT_NO_UPGRADE && status.sites().isEmpty(),
                    "the panel lists nothing and asks for the upgrade");
            upgrade(sites.a());
            sites.a().setEnergy(0);
            SiteStatusPayload unpowered = WarehouseControllerMenu.status(sites.a());
            helper.assertTrue(unpowered.hint() == SiteStatusPayload.HINT_NO_POWER && unpowered.needs() == 2 && unpowered.sites().isEmpty(),
                    "with the upgrade but no FE the panel says it needs 2 FE");
            sites.a().setEnergy(1000);
            listed(helper, sites, () -> {
                SiteStatusPayload capable = WarehouseControllerMenu.status(sites.a());
                helper.assertTrue(capable.capable() && capable.sites().stream().anyMatch(row -> row.key().equals(key(helper, B_CONTROLLER))),
                        "a capable controller lists the outpost");
                helper.assertTrue(capable.energy() == 1000 && capable.energyCapacity() == sites.a().energy().capacity(), "the GUI shows the FE");
                helper.succeed();
            });
        });
    }

    /** An outpost without power is listed in orange and refused; a base without power refuses too. */
    @ServusGameTest(timeoutTicks = 200)
    public static void unpoweredSiteRefused(GameTestHelper helper) {
        Sites sites = sites(helper);
        sites.b().setEnergy(0);
        String outpost = key(helper, B_CONTROLLER);
        TerminalWiring.when(helper, () -> indexed(sites) && row(helper, outpost).filter(row -> !row.trade()).isPresent(), 60,
                () -> "the outpost is listed as unable to trade, had " + row(helper, outpost), () -> {
            ServerLevel level = helper.getLevel();
            String there = RemoteTransfers.request(level, terminal(helper), outpost, iron(), 8);
            helper.assertTrue(there.equals("no_power_there"), "refused for the outpost's power, was " + there);
            WarehouseNetwork.Shipping direct = sites.b().network().orElseThrow()
                    .shipWireless(helper.absolutePos(A_PACKING), key(helper, A_CONTROLLER), iron(), 8, 0);
            helper.assertTrue(direct.refusal().equals("no_power_there"), "the outpost refuses to ship, was " + direct.refusal());
            sites.b().setEnergy(SENDER_CHARGE);
            sites.a().setEnergy(0);
            String here = RemoteTransfers.request(level, terminal(helper), outpost, iron(), 8);
            helper.assertTrue(here.equals("no_power_here"), "refused for the base's power, was " + here);
            helper.assertTrue(terminal(helper).requests().isEmpty(), "nothing is listed");
            helper.succeed();
        });
    }

    /** An outpost without a Packing Station is refused, by the terminal and by its own warehouse. */
    @ServusGameTest(timeoutTicks = 200)
    public static void noPackingStationRefused(GameTestHelper helper) {
        Sites sites = sites(helper, true, false);
        String outpost = key(helper, B_CONTROLLER);
        listed(helper, sites, () -> {
            ServerLevel level = helper.getLevel();
            SiteRecord record = SiteRegistry.of(level).site(outpost).orElseThrow();
            helper.assertTrue(!record.hasPacking(), "the outpost's record has no Packing Station");
            String refusal = RemoteTransfers.request(level, terminal(helper), outpost, iron(), 8);
            helper.assertTrue(refusal.equals("no_packing_there"), "refused for the outpost's Packing Station, was " + refusal);
            WarehouseNetwork.Shipping direct = sites.b().network().orElseThrow()
                    .shipWireless(helper.absolutePos(A_PACKING), key(helper, A_CONTROLLER), iron(), 8, 0);
            helper.assertTrue(direct.refusal().equals("no_packing_there"), "the outpost refuses to ship, was " + direct.refusal());
            helper.assertTrue(count(sites.rackB(), Items.IRON_INGOT) == 32, "nothing leaves the outpost");
            helper.assertTrue(SiteRegistry.of(level).site(key(helper, A_CONTROLLER)).orElseThrow().hasPacking(), "the base has its Packing Station");
            helper.succeed();
        });
    }

    /** A running transfer stops when the base loses its FE, shows waiting for power, and goes on with FE. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void transferWaitsForPowerAndResumes(GameTestHelper helper) {
        Sites sites = sites(helper);
        sites.a().setEnergy(10000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        listed(helper, sites, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(key(helper, B_CONTROLLER));
            menu.request(iron(), 24);
            helper.assertTrue(menu.stock().message().isEmpty(), "the transfer is made, was " + menu.stock().message());
            TerminalWiring.when(helper, () -> delivered(helper, terminal(helper)) > 0, 440, () -> "the first iron arrives", () -> {
                sites.a().setEnergy(0);
                int stopped = delivered(helper, terminal(helper));
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(delivered(helper, terminal(helper)) == stopped, "nothing moves without power");
                    List<RequestRow> rows = menu.stock().requests();
                    helper.assertTrue(rows.size() == 1 && rows.get(0).state().equals("NO_POWER"), "the transfer waits for power, was " + rows);
                    sites.a().setEnergy(10000);
                    TerminalWiring.when(helper, () -> terminal(helper).outputCount(Items.IRON_INGOT) == 24 && terminal(helper).requests().isEmpty(), 540,
                            () -> "with FE the rest arrives, had " + terminal(helper).outputCount(Items.IRON_INGOT), helper::succeed);
                });
            });
        });
    }

    /** Taking the outpost's Wireless Upgrade out pauses its transfer; putting it back lets it finish. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void upgradeRemovedPausesTransfer(GameTestHelper helper) {
        Sites sites = sites(helper);
        sites.a().setEnergy(10000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        listed(helper, sites, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(key(helper, B_CONTROLLER));
            menu.request(iron(), 24);
            helper.assertTrue(menu.stock().message().isEmpty(), "the transfer is made, was " + menu.stock().message());
            TerminalWiring.when(helper, () -> delivered(helper, terminal(helper)) > 0, 440, () -> "the first iron arrives", () -> {
                ItemStack taken = sites.b().wirelessUpgrade().extractItem(0, 1, false);
                helper.assertTrue(!taken.isEmpty(), "the upgrade comes out");
                int stopped = delivered(helper, terminal(helper));
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(delivered(helper, terminal(helper)) == stopped, "nothing moves without the outpost's upgrade");
                    List<RequestRow> rows = menu.stock().requests();
                    helper.assertTrue(rows.size() == 1 && rows.get(0).state().equals("NO_POWER"), "the transfer waits, was " + rows);
                    upgrade(sites.b());
                    TerminalWiring.when(helper, () -> terminal(helper).outputCount(Items.IRON_INGOT) == 24 && terminal(helper).requests().isEmpty(), 540,
                            () -> "with the upgrade back the rest arrives, had " + terminal(helper).outputCount(Items.IRON_INGOT), helper::succeed);
                });
            });
        });
    }

    /** A terminal showing the outpost falls back to this site when the base runs out of FE, and offers no site until FE arrives. */
    @ServusGameTest(timeoutTicks = 200)
    public static void viewFallsBackWhenPowerLost(GameTestHelper helper) {
        Sites sites = sites(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String outpost = key(helper, B_CONTROLLER);
        listed(helper, sites, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(outpost);
            helper.assertTrue(menu.stock().view().equals(outpost), "the outpost is shown");
            sites.a().setEnergy(0);
            StockSyncPayload after = menu.stock();
            helper.assertTrue(after.view().isEmpty() && after.sites().isEmpty(), "back on this site with no site offered");
            helper.assertTrue(menu.inputOpen(), "the Input row is open again");
            menu.selectSite(outpost);
            helper.assertTrue(menu.stock().view().isEmpty(), "the outpost cannot be picked without FE");
            sites.a().setEnergy(1000);
            menu.selectSite(outpost);
            helper.assertTrue(menu.stock().view().equals(outpost), "with FE the outpost can be picked again");
            helper.succeed();
        });
    }

    /** While the terminal shows another site its Input row takes nothing: no click, no shift click, no carried stack. */
    @ServusGameTest(timeoutTicks = 200)
    public static void inputLockedWhileViewingOtherSite(GameTestHelper helper) {
        Sites sites = sites(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        listed(helper, sites, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(key(helper, B_CONTROLLER));
            helper.assertTrue(!menu.inputOpen(), "the Input row is closed");
            helper.assertTrue(!menu.getSlot(menu.inputStart()).mayPlace(new ItemStack(Items.OAK_LOG)), "a click places nothing");
            int pocket = menu.inputStart() + terminal(helper).input().getSlots();
            menu.getSlot(pocket).set(new ItemStack(Items.OAK_LOG, 16));
            menu.quickMoveStack(player, pocket);
            helper.assertTrue(menu.getSlot(pocket).getItem().getCount() == 16, "shift click keeps the logs in the inventory");
            menu.setCarried(new ItemStack(Items.OAK_LOG, 8));
            menu.pushCarried();
            helper.assertTrue(menu.getCarried().getCount() == 8, "the carried stack stays");
            helper.assertTrue(terminal(helper).input().getStackInSlot(0).isEmpty(), "nothing entered the Input row");
            menu.selectSite("");
            menu.quickMoveStack(player, pocket);
            helper.assertTrue(menu.getSlot(pocket).getItem().isEmpty(), "back on this site the Input row takes them");
            helper.succeed();
        });
    }

    /** A sleeping site whose controller has no Wireless Upgrade is not listed and a request to it is refused. */
    @ServusGameTest(timeoutTicks = 200)
    public static void sleepingSiteWithoutUpgradeRefused(GameTestHelper helper) {
        Sites sites = sites(helper);
        ServerLevel level = helper.getLevel();
        BlockPos camp = helper.absolutePos(new BlockPos(3, 1, 3)).offset(160000, 0, 0);
        SiteRegistry.of(level).remember(level.dimension(), camp, "Quiet Camp", sites.team(), Set.of(SiteChunks.ofBlock(camp.getX(), camp.getZ())),
                1, Map.of(ItemKey.of(new ItemStack(Items.COAL)), 12L), false, true, true);
        String key = SiteRecord.key(level.dimension(), camp);
        listed(helper, sites, () -> {
            helper.assertTrue(row(helper, key).isEmpty(), "the camp without the upgrade is not listed");
            String refusal = RemoteTransfers.request(level, terminal(helper), key, ItemKey.of(new ItemStack(Items.COAL)), 4);
            helper.assertTrue(refusal.equals("no_wireless_there"), "refused, was " + refusal);
            SiteRegistry.of(level).remove(level.dimension(), camp);
            helper.succeed();
        });
    }

    /** A sleeping site that can wake and trade is held awake while its transfer waits; cancelling lets it sleep. */
    @ServusGameTest(timeoutTicks = 600)
    public static void wakeableSiteHeldForRequest(GameTestHelper helper) {
        StorageStatsManager.INSTANCE.setWakeGraceTicks(20);
        Sites sites = sites(helper);
        ServerLevel level = helper.getLevel();
        // A recorded site whose controller is not there: it sleeps, and its one chunk is the test area's.
        BlockPos camp = helper.absolutePos(new BlockPos(10, 1, 10));
        SiteRegistry.of(level).remember(level.dimension(), camp, "Quiet Camp", sites.team(), Set.of(SiteChunks.ofBlock(camp.getX(), camp.getZ())),
                1, Map.of(ItemKey.of(new ItemStack(Items.COAL)), 12L), true, true, true, 100000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String key = SiteRecord.key(level.dimension(), camp);
        TerminalWiring.when(helper, () -> row(helper, key).isPresent(), 60, () -> "the camp is listed", () -> {
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

    /** Two transfers at once go to the base's two Packing Stations, one each. */
    @ServusGameTest(timeoutTicks = 900)
    public static void receiversSpreadOverStations(GameTestHelper helper) {
        Sites sites = sites(helper);
        sites.a().setEnergy(10000);
        helper.setBlock(A_SECOND, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        // The second station reaches the terminal by data line too.
        TerminalWiring.dataDuct(helper, A_SECOND.above());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TerminalWiring.when(helper, () -> indexed(sites) && row(helper, key(helper, B_CONTROLLER)).filter(SiteRow::trade).isPresent()
                && sites.a().network().map(network -> network.packingStations().size() == 2).orElse(false), 60,
                () -> "the outpost is listed and the base has two Packing Stations", () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(key(helper, B_CONTROLLER));
            menu.request(iron(), 8);
            menu.request(iron(), 8);
            PackingStationBlockEntity sender = helper.getBlockEntity(B_PACKING, PackingStationBlockEntity.class);
            List<BlockPos> targets = sender.shipments().stream().map(PackingStationBlockEntity.Shipment::target).distinct().toList();
            helper.assertTrue(sender.shipments().size() == 2 && targets.size() == 2, "each base station receives one transfer, targets " + targets);
            TerminalWiring.when(helper, () -> terminal(helper).outputCount(Items.IRON_INGOT) == 16 && terminal(helper).requests().isEmpty(), 780,
                    () -> "16 iron arrive in the output slots, had " + terminal(helper).outputCount(Items.IRON_INGOT), () -> {
                helper.assertTrue(count(sites.rackA(), Items.IRON_INGOT) == 0, "nothing is put away at the base");
                helper.succeed();
            });
        });
    }

    /** A transfer that arrives while the output slots are full waits in the receiving Packing Station and moves on once they are emptied. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void transferWaitsWhileOutputFull(GameTestHelper helper) {
        Sites sites = sites(helper);
        sites.a().setEnergy(10000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalBlockEntity terminal = terminal(helper);
        for (int slot = 0; slot < terminal.output().getSlots(); slot++) {
            terminal.output().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        listed(helper, sites, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal(helper));
            menu.selectSite(key(helper, B_CONTROLLER));
            menu.request(iron(), 16);
            helper.assertTrue(menu.stock().message().isEmpty(), "the transfer is made, was " + menu.stock().message());
            // The outpost sent everything: only the relay of the base's Packing Station is left.
            TerminalWiring.when(helper, () -> terminal(helper).requests().size() == 1 && terminal(helper).requests().get(0).shipments.isEmpty()
                            && terminal(helper).requests().get(0).relay != null, 540,
                    () -> "the outpost sent all 16 iron to the base's Packing Station", () -> {
                helper.runAfterDelay(20, () -> {
                    helper.assertTrue(terminal(helper).outputCount(Items.IRON_INGOT) == 0, "no iron fits into the full output slots");
                    helper.assertTrue(count(sites.rackA(), Items.IRON_INGOT) == 0, "nothing is put away at the base");
                    List<RequestRow> rows = menu.stock().requests();
                    helper.assertTrue(rows.size() == 1 && rows.get(0).state().equals("WAITING_ROOM"), "the transfer waits for room, was " + rows);
                    for (int slot = 0; slot < terminal(helper).output().getSlots(); slot++) {
                        terminal(helper).output().setStackInSlot(slot, ItemStack.EMPTY);
                    }
                    TerminalWiring.when(helper, () -> terminal(helper).outputCount(Items.IRON_INGOT) == 16 && terminal(helper).requests().isEmpty(), 300,
                            () -> "with room the iron moves on, had " + terminal(helper).outputCount(Items.IRON_INGOT), () -> {
                        helper.assertTrue(count(sites.rackA(), Items.IRON_INGOT) == 0, "still nothing is put away at the base");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** The record follows the upgrade, the power and the Packing Station of an online site and keeps them. */
    @ServusGameTest(timeoutTicks = 200)
    public static void recordKeepsTradeStateWhileAsleep(GameTestHelper helper) {
        Sites sites = sites(helper);
        ServerLevel level = helper.getLevel();
        String outpost = key(helper, B_CONTROLLER);
        TerminalWiring.when(helper, () -> SiteRegistry.of(level).site(outpost).filter(site -> site.canTrade() && site.hasPacking()).isPresent(), 40,
                () -> "the record says the outpost trades and has a Packing Station", () -> {
            sites.b().setEnergy(0);
            TerminalWiring.when(helper, () -> SiteRegistry.of(level).site(outpost).filter(site -> site.hasUpgrade() && !site.powered()).isPresent(), 20,
                    () -> "the record learns the lost power", () -> {
                helper.setBlock(B_PACKING, Blocks.AIR.defaultBlockState());
                sites.b().wirelessUpgrade().extractItem(0, 1, false);
                TerminalWiring.when(helper, () -> SiteRegistry.of(level).site(outpost)
                                .filter(site -> !site.hasPacking() && !site.hasUpgrade() && !site.powered()).isPresent(), 20,
                        () -> "the record learns the lost Packing Station and upgrade", () -> {
                    SiteRecord record = SiteRegistry.of(level).site(outpost).orElseThrow();
                    helper.assertTrue(!record.canTrade(), "the outpost cannot trade");
                    helper.succeed();
                });
            });
        });
    }
}
