package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.shuttles.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.order.ExportBook;
import io.github.dirnei.mc.servus.shuttles.order.RuleThresholds;
import io.github.dirnei.mc.servus.shuttles.order.StockRule;
import io.github.dirnei.mc.servus.shuttles.order.StockRules;
import io.github.dirnei.mc.servus.shuttles.outbound.ExportListPayload;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Stock rule exports: claiming them at Outbound Stations, releasing them, and shipping the surplus by shuttle. */
public final class StockRuleExportGameTests {

    private static final BlockPos ORIGIN = new BlockPos(2, 1, 4);
    /** An Outbound Station south of the second rail, front to the west. */
    private static final BlockPos STATION = ORIGIN.east().south();
    /** A second Outbound Station south of the first rail, front to the west. */
    private static final BlockPos SECOND = ORIGIN.south();
    private static final ItemKey WHEAT = ItemKey.of(new ItemStack(Items.WHEAT));
    private static final ItemKey KELP = ItemKey.of(new ItemStack(Items.KELP));
    private static final int WAIT = 60;

    private StockRuleExportGameTests() {
    }

    private static int check() {
        return ShuttlesStatsManager.INSTANCE.outboundCheckInterval();
    }

    /** A Mini Warehouse with a shuttle, an Outbound Station and the rack stocked; runs {@code then} once both are online. */
    private static void warehouse(GameTestHelper helper, boolean garage, int wheat, Runnable then) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, garage);
        OutboundStationBlockEntity station = WarehouseFixtures.outbound(helper, STATION, Direction.WEST);
        int left = wheat;
        while (left > 0) {
            int stack = Math.min(64, left);
            WarehouseFixtures.put(warehouse.rack(), new ItemStack(Items.WHEAT, stack));
            left -= stack;
        }
        Waits.when(helper, () -> station.network().map(network -> network.status() == WarehouseNetwork.Status.ONLINE
                        && network.index().count(WHEAT) == wheat).orElse(false), WAIT,
                () -> "the warehouse should be online with " + wheat + " wheat indexed", then);
    }

    private static OutboundStationBlockEntity station(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, OutboundStationBlockEntity.class);
    }

    private static WarehouseNetwork network(GameTestHelper helper) {
        return station(helper, STATION).network().orElseThrow();
    }

    private static Optional<BlockPos> holder(GameTestHelper helper, ItemKey key) {
        return network(helper).stockRules().orElseThrow().exportHolder(key).map(claim -> claim.station());
    }

    private static RuleRow row(GameTestHelper helper, ItemKey key) {
        return network(helper).ruleRows().stream().filter(row -> ItemKey.of(row.stack()).equals(key)).findFirst().orElseThrow();
    }

    private static void export(GameTestHelper helper, ItemKey key, int above) {
        String check = network(helper).setStockRule(key, null, 0, above);
        helper.assertTrue(check.equals(RuleThresholds.OK), "the export is saved, got " + check);
    }

    /** Export above 400 beside Fill up to 500 is refused; above 2000 without Make saves with no station. */
    @ServusGameTest(timeoutTicks = 200)
    public static void exportThresholdChecked(GameTestHelper helper) {
        warehouse(helper, false, 0, () -> {
            String refused = network(helper).setStockRule(WHEAT, new StockRule.Make(100, 500), 0, 400);
            helper.assertTrue(refused.equals(RuleThresholds.EXPORT_NOT_ABOVE), "export not above Fill up to is refused, got " + refused);
            helper.assertTrue(network(helper).stockRules().orElseThrow().get(WHEAT) == null, "no rule was saved");
            export(helper, WHEAT, 2000);
            StockRule rule = network(helper).stockRules().orElseThrow().get(WHEAT);
            helper.assertTrue(rule.hasExport() && rule.export() == 2000 && !rule.hasMake(), "the wheat rule exports above 2000, got " + rule);
            helper.assertTrue(row(helper, WHEAT).exportState().equals("no_station"), "a new export has no station");
            helper.succeed();
        });
    }

    /** An export threshold and the station and slot holding it survive saving the rules; a rule saved without Export has none. */
    @ServusGameTest(timeoutTicks = 200)
    public static void exportSurvivesSaving(GameTestHelper helper) {
        warehouse(helper, false, 0, () -> {
            export(helper, WHEAT, 2000);
            network(helper).setStockRule(KELP, new StockRule.Make(8, 16), 0);
            station(helper, STATION).claimExport(WHEAT, 3);
            StockRules rules = network(helper).stockRules().orElseThrow();
            CompoundTag saved = rules.save(helper.getLevel().registryAccess());
            StockRules loaded = new StockRules();
            loaded.load(saved, helper.getLevel().registryAccess());
            helper.assertTrue(loaded.get(WHEAT).export() == 2000, "the threshold survives");
            helper.assertTrue(loaded.exportHolder(WHEAT).equals(Optional.of(new ExportBook.Claim<>(station(helper, STATION).getBlockPos(), 3))),
                    "the station and slot survive, got " + loaded.exportHolder(WHEAT));
            helper.assertTrue(!loaded.get(KELP).hasExport(), "a rule without Export has no export");
            helper.succeed();
        });
    }

    /** The menu claims wheat into the third keep slot; kelp into the same slot and a claim into a keep slot are refused. */
    @ServusGameTest(timeoutTicks = 200)
    public static void claimThroughTheMenu(GameTestHelper helper) {
        warehouse(helper, false, 0, () -> {
            export(helper, WHEAT, 2000);
            export(helper, KELP, 100);
            OutboundStationBlockEntity station = station(helper, STATION);
            station.setKeep(0, new ItemStack(Items.STICK), 8);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(helper.absolutePos(STATION)).add(1, 0, 0));
            OutboundStationMenu menu = new OutboundStationMenu(1, player.getInventory(), station);
            menu.claim(2, new ItemStack(Items.WHEAT));
            helper.assertTrue(holder(helper, WHEAT).equals(Optional.of(station.getBlockPos())), "the wheat export is at the station");
            helper.assertTrue(station.isExportSlot(2), "the third keep slot holds the export");
            menu.claim(2, new ItemStack(Items.KELP));
            helper.assertTrue(holder(helper, KELP).isEmpty(), "an occupied slot refuses another export");
            menu.claim(0, new ItemStack(Items.KELP));
            helper.assertTrue(holder(helper, KELP).isEmpty(), "a keep slot refuses an export");
            ExportListPayload list = menu.exports();
            helper.assertTrue(list.slot(2) != null && list.slot(2).threshold() == 2000, "the list shows the export slot with 2000");
            helper.assertTrue(list.offers().size() == 1 && list.offers().get(0).item().is(Items.KELP) && !list.offers().get(0).held(),
                    "kelp is offered without a station, wheat is not offered here: " + list.offers());
            helper.succeed();
        });
    }

    /** Kelp held by a second station moves to the first when claimed there; the second station's slot is empty again. */
    @ServusGameTest(timeoutTicks = 200)
    public static void claimMovesFromAnotherStation(GameTestHelper helper) {
        warehouse(helper, false, 0, () -> {
            OutboundStationBlockEntity second = WarehouseFixtures.outbound(helper, SECOND, Direction.WEST);
            Waits.when(helper, () -> second.network().map(found -> found == network(helper)).orElse(false), WAIT,
                    () -> "the second station should join", () -> {
                        export(helper, KELP, 100);
                        helper.assertTrue(second.claimExport(KELP, 0), "the second station claims kelp");
                        OutboundStationBlockEntity first = station(helper, STATION);
                        helper.assertTrue(first.claimExport(KELP, 4), "the first station takes it");
                        helper.assertTrue(holder(helper, KELP).equals(Optional.of(first.getBlockPos())) && first.isExportSlot(4),
                                "kelp is at the first station");
                        helper.assertTrue(!second.isExportSlot(0), "the second station's slot is empty");
                        helper.succeed();
                    });
        });
    }

    /** At the station the export slot ignores a click with an item and scrolling; a right click with an empty hand releases it. */
    @ServusGameTest(timeoutTicks = 200)
    public static void exportSlotReadOnlyAndReleased(GameTestHelper helper) {
        warehouse(helper, false, 0, () -> {
            export(helper, WHEAT, 2000);
            OutboundStationBlockEntity station = station(helper, STATION);
            station.claimExport(WHEAT, 2);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(helper.absolutePos(STATION)).add(1, 0, 0));
            OutboundStationMenu menu = new OutboundStationMenu(1, player.getInventory(), station);
            menu.setCarried(new ItemStack(Items.IRON_INGOT, 32));
            menu.clicked(2, 0, ContainerInput.PICKUP, player);
            menu.setKeep(2, new ItemStack(Items.IRON_INGOT), 5);
            helper.assertTrue(station.isExportSlot(2) && station.keep().getStackInSlot(2).isEmpty(), "the export slot keeps nothing");
            helper.assertTrue(menu.getCarried().getCount() == 32, "the iron stays on the cursor");
            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(2, 1, ContainerInput.PICKUP, player);
            helper.assertTrue(!station.isExportSlot(2) && holder(helper, WHEAT).isEmpty(), "the right click released the export");
            helper.assertTrue(row(helper, WHEAT).exportState().equals("no_station"), "the rule shows no station");
            helper.succeed();
        });
    }

    /** Above 40 with 100 wheat in the rack, shuttles bring 60 into the station and 40 stay; no more is asked for those 60. */
    @ServusGameTest(timeoutTicks = 800)
    public static void surplusShipped(GameTestHelper helper) {
        warehouse(helper, true, 100, () -> {
            OutboundStationBlockEntity station = station(helper, STATION);
            station.sideConfig().set(RelativeSide.BACK, SideMode.DISABLED);
            export(helper, WHEAT, 40);
            station.claimExport(WHEAT, 0);
            Waits.when(helper, () -> station.bufferCount(WHEAT) == 60, check() + 3 * MiniWarehouse.TRIP_TICKS,
                    () -> "the surplus of 60 should arrive, has " + station.bufferCount(WHEAT), () ->
                            helper.runAfterDelay(2 * check(), () -> {
                                helper.assertTrue(station.bufferCount(WHEAT) == 60, "no more than the surplus arrives, has " + station.bufferCount(WHEAT));
                                helper.assertTrue(network(helper).index().count(WHEAT) == 40, "40 wheat stay in the racks");
                                helper.succeed();
                            }));
        });
    }

    /** With 30 wheat in the rack and the export above 40 nothing is shipped. */
    @ServusGameTest(timeoutTicks = 400)
    public static void nothingBelowTheThreshold(GameTestHelper helper) {
        warehouse(helper, true, 30, () -> {
            export(helper, WHEAT, 40);
            OutboundStationBlockEntity station = station(helper, STATION);
            station.claimExport(WHEAT, 0);
            helper.runAfterDelay(3 * check(), () -> {
                helper.assertTrue(station.bufferCount(WHEAT) == 0 && network(helper).queuedJobs() == 0 && network(helper).ridingJobs() == 0,
                        "no wheat is asked for");
                helper.assertTrue(row(helper, WHEAT).exportState().equals("ok"), "the export reads ok, got " + row(helper, WHEAT).exportState());
                helper.succeed();
            });
        });
    }

    /** A station whose buffer is full of wheat nobody takes turns red and the export reads station full. */
    @ServusGameTest(timeoutTicks = 400)
    public static void fullStationTurnsRed(GameTestHelper helper) {
        warehouse(helper, true, 200, () -> {
            OutboundStationBlockEntity station = station(helper, STATION);
            station.sideConfig().set(RelativeSide.BACK, SideMode.DISABLED);
            for (int slot = 0; slot < station.buffer().getSlots(); slot++) {
                station.buffer().setStackInSlot(slot, new ItemStack(Items.WHEAT, 64));
            }
            export(helper, WHEAT, 40);
            station.claimExport(WHEAT, 0);
            Waits.when(helper, () -> WarehouseFixtures.blocked(helper, STATION), 3 * check(), () -> "the lamp should turn red", () -> {
                helper.assertTrue(station.shortReason().equals("export_full") && station.shortStack().is(Items.WHEAT),
                        "the state line names wheat and a full station, got " + station.shortReason());
                helper.assertTrue(row(helper, WHEAT).exportState().equals("station_full"), "the export reads station full");
                helper.succeed();
            });
        });
    }

    /** Breaking the station releases its export. */
    @ServusGameTest(timeoutTicks = 300)
    public static void releasedWhenBroken(GameTestHelper helper) {
        warehouse(helper, false, 0, () -> {
            export(helper, WHEAT, 2000);
            station(helper, STATION).claimExport(WHEAT, 1);
            WarehouseNetwork network = network(helper);
            helper.setBlock(STATION, Blocks.AIR.defaultBlockState());
            Waits.when(helper, () -> network.stockRules().orElseThrow().exportHolder(WHEAT).isEmpty(), 3 * check(),
                    () -> "the export should lose its station", helper::succeed);
        });
    }

    /** Dismantling the station with the carrot releases its export. */
    @ServusGameTest(timeoutTicks = 300)
    public static void releasedWhenDismantled(GameTestHelper helper) {
        warehouse(helper, false, 0, () -> {
            export(helper, WHEAT, 2000);
            station(helper, STATION).claimExport(WHEAT, 1);
            WarehouseNetwork network = network(helper);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            GameTestSteps.useCarrot(helper, player, STATION, Direction.UP);
            helper.assertBlockPresent(Blocks.AIR, STATION);
            Waits.when(helper, () -> network.stockRules().orElseThrow().exportHolder(WHEAT).isEmpty(), 3 * check(),
                    () -> "the export should lose its station", helper::succeed);
        });
    }

    /** A station cut off by a broken rail loses its export, and the rail placed again does not give it back. */
    @ServusGameTest(timeoutTicks = 400)
    public static void releasedWhenTheRailIsRemoved(GameTestHelper helper) {
        BlockPos link = ORIGIN.east(2).south(3);
        BlockPos cut = link.south();
        warehouse(helper, false, 0, () -> {
            WarehouseFixtures.railOnStone(helper, ORIGIN.east(2).south());
            WarehouseFixtures.railOnStone(helper, ORIGIN.east(2).south(2));
            WarehouseFixtures.railOnStone(helper, link);
            OutboundStationBlockEntity far = WarehouseFixtures.outbound(helper, cut, Direction.SOUTH);
            WarehouseNetwork network = network(helper);
            Waits.when(helper, () -> far.network().map(found -> found == network).orElse(false), WAIT, () -> "the far station should join", () -> {
                export(helper, WHEAT, 2000);
                helper.assertTrue(far.claimExport(WHEAT, 0), "the far station claims wheat");
                helper.setBlock(link, Blocks.AIR.defaultBlockState());
                Waits.when(helper, () -> network.stockRules().orElseThrow().exportHolder(WHEAT).isEmpty(), 3 * check(),
                        () -> "the export should lose its station", () -> {
                            WarehouseFixtures.rail(helper, link);
                            helper.runAfterDelay(2 * check(), () -> {
                                helper.assertTrue(network.stockRules().orElseThrow().exportHolder(WHEAT).isEmpty(),
                                        "the rail placed again does not give the export back");
                                helper.succeed();
                            });
                        });
            });
        });
    }

    /**
     * A Mini Warehouse with a terminal on its Packing Station, the rack holding
     * {@code wheat} and an Outbound Station south of the third rail that
     * pushes nowhere and holds the wheat export above {@code above}; runs
     * {@code then} once the rack is indexed.
     */
    private static void terminalWarehouse(GameTestHelper helper, int wheat, int above, Consumer<MiniWarehouse> then) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal();
        OutboundStationBlockEntity station = WarehouseFixtures.outbound(helper, warehouse.inboundPos(), Direction.SOUTH);
        station.sideConfig().set(RelativeSide.BACK, SideMode.DISABLED);
        if (wheat > 0) {
            warehouse.stock(new ItemStack(Items.WHEAT, wheat));
        }
        Waits.when(helper, () -> station.network().map(network -> network.status() == WarehouseNetwork.Status.ONLINE
                        && network.index().count(WHEAT) == wheat).orElse(false), WAIT,
                () -> "the warehouse should be online with " + wheat + " wheat indexed", () -> {
                    String check = station.network().orElseThrow().setStockRule(WHEAT, null, 0, above);
                    helper.assertTrue(check.equals(RuleThresholds.OK), "the export is saved, got " + check);
                    helper.assertTrue(station.claimExport(WHEAT, 0), "the station claims the wheat export");
                    then.accept(warehouse);
                });
    }

    /**
     * With 30 wheat in the rack and the export above 40, 50 wheat put into the
     * terminal split: 10 go into the rack and 40 ride straight to the Outbound
     * Station; the rack never holds more than 40.
     */
    @ServusGameTest(timeoutTicks = 1000)
    public static void terminalInputSkipsTheRack(GameTestHelper helper) {
        int[] most = {0};
        helper.onEachTick(() -> {
            if (helper.getLevel().getBlockEntity(helper.absolutePos(ORIGIN.north())) instanceof HeavyRackBlockEntity rack) {
                most[0] = Math.max(most[0], WarehouseFixtures.count(rack, Items.WHEAT));
            }
        });
        terminalWarehouse(helper, 30, 40, warehouse -> {
            OutboundStationBlockEntity station = station(helper, warehouse.inboundPos());
            warehouse.terminal().input().setStackInSlot(0, new ItemStack(Items.WHEAT, 50));
            Waits.when(helper, () -> station.bufferCount(WHEAT) == 40 && warehouse.rackCount(Items.WHEAT) == 40,
                    check() + 6 * MiniWarehouse.TRIP_TICKS,
                    () -> "40 wheat should reach the station and 40 be in the rack, station " + station.bufferCount(WHEAT)
                            + ", rack " + warehouse.rackCount(Items.WHEAT), () ->
                            helper.runAfterDelay(2 * check(), () -> {
                                helper.assertTrue(most[0] <= 40, "the surplus never entered the rack, the rack held " + most[0]);
                                helper.assertTrue(station.bufferCount(WHEAT) == 40 && warehouse.rackCount(Items.WHEAT) == 40,
                                        "nothing ships twice, station " + station.bufferCount(WHEAT) + ", rack " + warehouse.rackCount(Items.WHEAT));
                                helper.succeed();
                            }));
        });
    }

    /**
     * When the Outbound Station has no free slot, 20 wheat from the terminal
     * above the threshold are stored in the rack; once the station is emptied
     * the export ships them from the rack.
     */
    @ServusGameTest(timeoutTicks = 1400)
    public static void fullStationStoresTheSurplus(GameTestHelper helper) {
        terminalWarehouse(helper, 40, 40, warehouse -> {
            OutboundStationBlockEntity station = station(helper, warehouse.inboundPos());
            for (int slot = 0; slot < station.buffer().getSlots(); slot++) {
                station.buffer().setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
            }
            warehouse.terminal().input().setStackInSlot(0, new ItemStack(Items.WHEAT, 20));
            Waits.when(helper, () -> warehouse.rackCount(Items.WHEAT) == 60, check() + 6 * MiniWarehouse.TRIP_TICKS,
                    () -> "the 20 wheat should be stored, rack " + warehouse.rackCount(Items.WHEAT), () -> {
                        helper.assertTrue(station.bufferCount(WHEAT) == 0, "no wheat reached the full station");
                        for (int slot = 0; slot < station.buffer().getSlots(); slot++) {
                            station.buffer().setStackInSlot(slot, ItemStack.EMPTY);
                        }
                        Waits.when(helper, () -> station.bufferCount(WHEAT) == 20 && warehouse.rackCount(Items.WHEAT) == 40,
                                check() + 4 * MiniWarehouse.TRIP_TICKS,
                                () -> "the export should ship the 20 from the rack, station " + station.bufferCount(WHEAT)
                                        + ", rack " + warehouse.rackCount(Items.WHEAT), helper::succeed);
                    });
        });
    }
}
