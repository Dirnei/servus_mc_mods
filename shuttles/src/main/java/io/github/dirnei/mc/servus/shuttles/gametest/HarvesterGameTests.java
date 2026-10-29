package io.github.dirnei.mc.servus.shuttles.gametest;

import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.CONTROLLER;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.GARAGE;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.HEAD;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.RACK;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.RAIL_Z;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.farm;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.network;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.noItemsLying;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.out;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldCropStockPayload;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldWork;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/** Harvester shuttles on the farm of {@link FarmFixtures}, and on a field behind an elevator shaft. */
public final class HarvesterGameTests {

    private static final BlockPos SECOND_RACK = new BlockPos(3, 1, RAIL_Z + 1);

    private HarvesterGameTests() {
    }

    /** How many of the item the farm head's To storage buffer holds. */
    static int toStorage(FarmFixtures.Farm farm, Item item) {
        return toStorageCount(farm.head(), item);
    }

    static int toStorageCount(FieldHeadBlockEntity head, Item item) {
        int count = 0;
        for (int slot = 0; slot < head.toStorage().getSlots(); slot++) {
            ItemStack stack = head.toStorage().getStackInSlot(slot);
            count += stack.is(item) ? stack.getCount() : 0;
        }
        return count;
    }

    private static ItemStack harvester() {
        return FarmFixtures.shuttleWith(ServusShuttlesItems.HARVESTER.get());
    }

    private static boolean ripe(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    /** A crop of that block that is not ripe; random ticks may already have grown it a little. */
    private static boolean planted(GameTestHelper helper, BlockPos pos, Block block) {
        BlockState state = helper.getBlockState(pos);
        return state.is(block) && !((CropBlock) block).isMaxAge(state);
    }

    private static String ages(GameTestHelper helper, BlockPos... plots) {
        StringBuilder ages = new StringBuilder();
        for (BlockPos plot : plots) {
            BlockState state = helper.getBlockState(plot);
            ages.append(ages.isEmpty() ? "" : ", ").append(state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1);
        }
        return ages.toString();
    }

    /** The Harvester goes into a shuttle in the workshop through the install slot. */
    @ServusGameTest
    public static void workshopFitsHarvester(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = new BlockPos(5, 1, 5);
        helper.setBlock(at, ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get().defaultBlockState());
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, at);
        player.getInventory().setItem(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        player.getInventory().setItem(1, new ItemStack(ServusShuttlesItems.HARVESTER.get()));
        int hotbar = ShuttleWorkshopMenu.INVENTORY_START + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).tool().equals(Optional.of(ServusShuttlesItems.HARVESTER.get())),
                "the Harvester is the shuttle's tool");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.INSTALL_SLOT).getItem().isEmpty(), "the install slot stays empty");
        helper.assertTrue(player.getInventory().getItem(1).isEmpty(), "the Harvester item left the inventory");
        helper.succeed();
    }

    /**
     * Ripe wheat is harvested into the rack and every plot is planted again
     * with seeds from the harvest; the spare seeds fill the head's buffer.
     */
    @ServusGameTest(timeoutTicks = 600)
    public static void harvesterHarvestsIntoStorage(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1), new BlockPos(7, 1, RAIL_Z - 1)};
        for (BlockPos plot : plots) {
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        }
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            Waits.when(helper, () -> {
                for (BlockPos plot : plots) {
                    if (!planted(helper, plot, Blocks.WHEAT)) {
                        return false;
                    }
                }
                return WarehouseFixtures.count(farm.rack(), Items.WHEAT) == plots.length && out(helper) == 0;
            }, 550, () -> "the four wheat should be in the rack and replanted, rack holds " + WarehouseFixtures.count(farm.rack(), Items.WHEAT)
                    + " wheat, harvester out " + out(helper) + ", ages " + ages(helper, plots), () -> {
                helper.assertTrue(noItemsLying(helper), "nothing should drop on the ground");
                helper.assertTrue(farm.head().ripeCount() == 0 && farm.head().plantedCount() == plots.length,
                        "the head should see four young crops, got " + farm.head().plantedCount() + " planted " + farm.head().ripeCount() + " ripe");
                helper.assertTrue(farm.head().supplyCount(SupplyBuffer.Part.SEEDS) > 0 && WarehouseFixtures.count(farm.rack(), Items.WHEAT_SEEDS) == 0,
                        "spare seeds should fill the buffer, not the rack, buffer " + farm.head().supplyCount(SupplyBuffer.Part.SEEDS));
                helper.succeed();
            });
        });
    }

    /** Without power no harvest run starts; once FE comes the field is harvested. */
    @ServusGameTest(timeoutTicks = 700)
    public static void fieldRunWaitsForPower(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1)};
        for (BlockPos plot : plots) {
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        }
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        controller.setEnergy(0);
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(out(helper) == 0, "no harvester leaves without power");
                for (BlockPos plot : plots) {
                    helper.assertTrue(ripe(helper, plot), "the wheat is still standing");
                }
                PowerFixtures.fill(controller);
                Waits.when(helper, () -> WarehouseFixtures.count(farm.rack(), Items.WHEAT) == plots.length && out(helper) == 0, 550,
                        () -> "with FE the field is harvested, rack holds " + WarehouseFixtures.count(farm.rack(), Items.WHEAT), helper::succeed);
            });
        });
    }

    /**
     * The bar on the head is red while the head has a problem: clear once a
     * harvester, water and a crop are there, red again when the crop is cleared.
     */
    @ServusGameTest(timeoutTicks = 300)
    public static void problemTurnsBarRed(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1)};
        for (BlockPos plot : plots) {
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 0);
        }
        FarmFixtures.Farm farm = farm(helper, harvester());
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(1, () -> helper.assertTrue(helper.getBlockState(HEAD).getValue(FieldHeadBlock.PROBLEM),
                "without water and crop the bar is red, work " + head.work()));
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            Waits.when(helper, () -> !helper.getBlockState(HEAD).getValue(FieldHeadBlock.PROBLEM), 100,
                    () -> "a watered field with crop and harvester has no red bar, work " + head.work() + " hydrated " + head.hydrated()
                            + " fertilize " + head.fertilizeStatus(), () -> {
                helper.assertTrue(!head.problem(), "the head has no problem");
                head.setCrop(ItemStack.EMPTY);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(head.work() == FieldWork.NO_CROP, "no crop set, got " + head.work());
                    helper.assertTrue(helper.getBlockState(HEAD).getValue(FieldHeadBlock.PROBLEM), "no crop: the bar is red again");
                    helper.succeed();
                });
            });
        });
    }

    /** Dirt and grass under empty plots in the strip become farmland with wheat; dirt five blocks out and stone stay. */
    @ServusGameTest(timeoutTicks = 600)
    public static void harvesterTillsInsideStripOnly(GameTestHelper helper) {
        BlockPos dirt = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos grass = new BlockPos(6, 1, RAIL_Z + 1);
        BlockPos stone = new BlockPos(7, 1, RAIL_Z + 3);
        BlockPos outside = new BlockPos(5, 1, RAIL_Z + 5);
        helper.setBlock(dirt.below(), Blocks.DIRT.defaultBlockState());
        helper.setBlock(grass.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        helper.setBlock(stone.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(outside.below(), Blocks.DIRT.defaultBlockState());
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT_SEEDS, 16));
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            Waits.when(helper, () -> planted(helper, dirt, Blocks.WHEAT) && planted(helper, grass, Blocks.WHEAT) && out(helper) == 0,
                    550, () -> "the dirt and grass plots should be tilled and planted, ages " + ages(helper, dirt, grass), () -> {
                helper.assertBlockPresent(Blocks.FARMLAND, dirt.below());
                helper.assertBlockPresent(Blocks.FARMLAND, grass.below());
                helper.assertBlockPresent(Blocks.STONE, stone.below());
                helper.assertBlockPresent(Blocks.DIRT, outside.below());
                helper.assertTrue(helper.getBlockState(outside).isAir(), "nothing is planted five blocks out");
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT_SEEDS) == 0
                        && farm.head().supplyCount(SupplyBuffer.Part.SEEDS) == 14, "a supply run brought the 16 seeds and two were planted, buffer holds "
                        + farm.head().supplyCount(SupplyBuffer.Part.SEEDS));
                helper.succeed();
            });
        });
    }

    /**
     * The crop picker's stock: the menu counts the crop items the warehouse
     * holds, only non zero ones, and follows the rack as seeds come in.
     */
    @ServusGameTest(timeoutTicks = 200)
    public static void menuCropStockCounts(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, harvester());
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT_SEEDS, 40));
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.COBBLESTONE, 5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FieldHeadMenu menu = new FieldHeadMenu(1, player.getInventory(), farm.head());
        Identifier wheat = BuiltInRegistries.ITEM.getKey(Items.WHEAT_SEEDS);
        Identifier beetroot = BuiltInRegistries.ITEM.getKey(Items.BEETROOT_SEEDS);
        Waits.when(helper, () -> menu.stockPayload().counts().getOrDefault(wheat, 0L) == 40, 150,
                () -> "the menu should count 40 wheat seeds, got " + menu.stockPayload(), () -> {
            FieldCropStockPayload payload = menu.stockPayload();
            helper.assertTrue(payload.networked(), "the head's field is on a warehouse");
            helper.assertTrue(!payload.counts().containsKey(beetroot), "beetroot seeds hold none and are not sent");
            helper.assertTrue(!payload.counts().containsKey(BuiltInRegistries.ITEM.getKey(Items.COBBLESTONE)), "cobblestone is no crop");
            helper.assertTrue(FieldHeadBlockEntity.cropItems().contains(Items.BEETROOT_SEEDS)
                    && FieldHeadBlockEntity.cropItems().contains(Items.CARROT)
                    && !FieldHeadBlockEntity.cropItems().contains(Items.COBBLESTONE), "the crop items come from the registry");
            WarehouseFixtures.put(farm.rack(), new ItemStack(Items.BEETROOT_SEEDS, 3));
            Waits.when(helper, () -> menu.stockPayload().counts().getOrDefault(beetroot, 0L) == 3, 40,
                    () -> "the menu should follow the rack to 3 beetroot seeds, got " + menu.stockPayload(), helper::succeed);
        });
    }

    /** Harvest only: ripe wheat is harvested and the farmland stays empty, an empty farmland plot is left alone. */
    @ServusGameTest(timeoutTicks = 600)
    public static void harvestOnlyDoesNotPlant(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1)};
        for (BlockPos plot : plots) {
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        }
        BlockPos empty = new BlockPos(4, 1, RAIL_Z + 1);
        helper.setBlock(empty.below(), Blocks.FARMLAND.defaultBlockState());
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT_SEEDS, 16));
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 2 && out(helper) == 0, 550,
                    () -> "the two wheat should be in the rack, it holds " + WarehouseFixtures.count(farm.rack(), Items.WHEAT), () -> {
                for (BlockPos plot : plots) {
                    helper.assertTrue(helper.getBlockState(plot).isAir(), "a harvested plot stays empty in harvest only mode");
                    helper.assertBlockPresent(Blocks.FARMLAND, plot.below());
                }
                helper.assertTrue(helper.getBlockState(empty).isAir(), "the empty farmland is not planted");
                helper.succeed();
            });
        });
    }

    /** A plain shuttle never drives onto the field, however ripe it is. */
    @ServusGameTest(timeoutTicks = 200)
    public static void plainShuttleTakesNoFieldJob(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        FarmFixtures.Farm farm = farm(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(farm.head().harvestDue(), "the harvest should be due");
                helper.assertTrue(ripe(helper, plot), "the wheat should still stand");
                helper.assertTrue(out(helper) == 0 && network(helper).queuedJobs() == 0, "no shuttle should be out and no job queued");
                helper.succeed();
            });
        });
    }

    /** With only a harvester, a request from a rack is refused for lack of a shuttle and the harvester stays home. */
    @ServusGameTest(timeoutTicks = 300)
    public static void harvesterTakesNoWarehouseJob(GameTestHelper helper) {
        BlockPos packing = new BlockPos(1, 1, RAIL_Z - 1);
        FarmFixtures.Farm farm = farm(helper, harvester());
        TerminalWiring.place(helper, packing);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.COBBLESTONE, 20));
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = network(helper);
            helper.assertTrue(!network.canReach(helper.absolutePos(RACK)), "a harvester must not count as a shuttle for warehouse jobs");
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, packing);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 20);
            helper.assertTrue(!menu.stock().message().isEmpty() && menu.stock().requests().isEmpty(),
                    "the request is refused without a warehouse shuttle, got '" + menu.stock().message() + "'");
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(out(helper) == 0, "the harvester never left");
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0 && WarehouseFixtures.count(farm.rack(), Items.COBBLESTONE) == 20, "the rack keeps its cobblestone");
                helper.succeed();
            });
        });
    }

    /** Nothing ripe and empty farmland: a supply run brings the rack's seeds into the buffer, and the harvester takes them from there and plants. */
    @ServusGameTest(timeoutTicks = 900)
    public static void seedsFromTheBuffer(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1), new BlockPos(7, 1, RAIL_Z + 1)};
        for (BlockPos plot : plots) {
            helper.setBlock(plot.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        }
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT_SEEDS, 10));
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            Waits.when(helper, () -> {
                for (BlockPos plot : plots) {
                    if (!planted(helper, plot, Blocks.WHEAT)) {
                        return false;
                    }
                }
                return out(helper) == 0;
            }, 850, () -> "the three plots should be planted", () -> {
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT_SEEDS) == 0
                        && farm.head().supplyCount(SupplyBuffer.Part.SEEDS) == 7, "three seeds should have left the buffer, it holds "
                        + farm.head().supplyCount(SupplyBuffer.Part.SEEDS));
                helper.succeed();
            });
        });
    }

    /** Wheat, carrots and beetroot do not fit two stacks at once: the harvester puts away and comes back for the rest. */
    @ServusGameTest(timeoutTicks = 800)
    public static void fullCargoComesBack(GameTestHelper helper) {
        BlockPos wheat = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos carrots = new BlockPos(5, 1, RAIL_Z + 1);
        BlockPos beetroot = new BlockPos(6, 1, RAIL_Z - 1);
        FieldFixtures.crop(helper, wheat, Blocks.WHEAT, 7);
        FieldFixtures.crop(helper, carrots, Blocks.CARROTS, 7);
        FieldFixtures.crop(helper, beetroot, Blocks.BEETROOTS, 3);
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        HeavyRackBlockEntity second = WarehouseFixtures.rack(helper, SECOND_RACK, Direction.NORTH);
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> helper.getBlockState(wheat).isAir() && helper.getBlockState(carrots).isAir()
                    && helper.getBlockState(beetroot).isAir() && out(helper) == 0, 750, () -> "all three crops should be harvested", () -> {
                int beetroots = WarehouseFixtures.count(farm.rack(), Items.BEETROOT) + WarehouseFixtures.count(second, Items.BEETROOT);
                int wheats = WarehouseFixtures.count(farm.rack(), Items.WHEAT) + WarehouseFixtures.count(second, Items.WHEAT);
                int carrotCount = WarehouseFixtures.count(farm.rack(), Items.CARROT) + WarehouseFixtures.count(second, Items.CARROT);
                helper.assertTrue(beetroots == 1 && wheats == 1 && carrotCount >= 1,
                        "the harvest should be in storage, got " + wheats + " wheat " + carrotCount + " carrots " + beetroots + " beetroot");
                helper.assertTrue(noItemsLying(helper), "nothing should drop on the ground");
                helper.succeed();
            });
        });
    }

    /** Wheat goes into the rack zoned for wheat, the seeds into general storage. */
    @ServusGameTest(timeoutTicks = 600)
    public static void harvestFollowsZones(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        HeavyRackBlockEntity zoned = WarehouseFixtures.rack(helper, SECOND_RACK, Direction.NORTH);
        zoned.setZone(ZoneFixtures.zone(helper, "Grain", new ZoneFilter(List.of(new ItemStack(Items.WHEAT)), List.of())));
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> helper.getBlockState(plot).isAir() && out(helper) == 0, 550,
                    () -> "the wheat should be harvested", () -> {
                helper.assertTrue(WarehouseFixtures.count(zoned, Items.WHEAT) == 1 && WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 0, "the wheat should be in the zoned rack");
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT_SEEDS) >= 1 && WarehouseFixtures.count(zoned, Items.WHEAT_SEEDS) == 0,
                        "the seeds should be in the unzoned rack");
                helper.succeed();
            });
        });
    }

    /** The head is broken mid run: the harvester stops, goes home and its wheat drops at the garage. */
    @ServusGameTest(timeoutTicks = 800)
    public static void headBrokenMidRun(GameTestHelper helper) {
        BlockPos first = new BlockPos(4, 1, RAIL_Z - 1);
        BlockPos[] rest = {new BlockPos(7, 1, RAIL_Z + 1), new BlockPos(7, 1, RAIL_Z + 2), new BlockPos(7, 1, RAIL_Z + 3)};
        FieldFixtures.crop(helper, first, Blocks.WHEAT, 7);
        for (BlockPos plot : rest) {
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        }
        FarmFixtures.Farm farm = farm(helper, harvester());
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> helper.getBlockState(first).isAir(), 400, () -> "the first wheat should be harvested", () -> {
                helper.setBlock(HEAD, Blocks.AIR.defaultBlockState());
                Waits.when(helper, () -> out(helper) == 0 && !noItemsLying(helper), 350,
                        () -> "the harvester should go home and drop its wheat at the garage, out " + out(helper), () -> {
                    int stillRipe = 0;
                    for (BlockPos plot : rest) {
                        stillRipe += ripe(helper, plot) ? 1 : 0;
                    }
                    helper.assertTrue(stillRipe >= 1, "the harvester should have stopped before the end of the field");
                    helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 0, "no wheat reaches the rack without a head");
                    helper.succeed();
                });
            });
        });
    }


    /**
     * No rack has room: the carrots wait in the head's To storage buffer and
     * the head says so; once the rack has room a plain shuttle stores them.
     */
    @ServusGameTest(timeoutTicks = 800)
    public static void warehouseFullWaits(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        FieldFixtures.crop(helper, plot, Blocks.CARROTS, 7);
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        WarehouseFixtures.fill(farm.rack(), Items.COBBLESTONE);
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> helper.getBlockState(plot).isAir() && out(helper) == 0, 400, () -> "the carrots should be harvested and the harvester home", () ->
                    Waits.when(helper, () -> farm.head().work() == FieldWork.COLLECT_NO_ROOM, 100,
                            () -> "the head should say no rack has room, it says " + farm.head().work(), () -> {
                        helper.assertTrue(toStorage(farm, Items.CARROT) >= 1, "the carrots wait in the head's To storage buffer");
                        helper.assertTrue(noItemsLying(helper), "the carrots must not drop");
                        farm.rack().clear();
                        Waits.when(helper, () -> WarehouseFixtures.count(farm.rack(), Items.CARROT) >= 1 && out(helper) == 0, 300,
                                () -> "the carrots should be stored once there is room", helper::succeed);
                    }));
        });
    }


    /** Ripe wheat across the last farm rail, x 7, so the harvester works there for a while. */
    private static BlockPos[] lastRailColumn(GameTestHelper helper) {
        BlockPos[] plots = new BlockPos[8];
        int i = 0;
        for (int z = RAIL_Z - 4; z <= RAIL_Z + 4; z++) {
            if (z != RAIL_Z) {
                plots[i] = new BlockPos(7, 1, z);
                FieldFixtures.crop(helper, plots[i++], Blocks.WHEAT, 7);
            }
        }
        return plots;
    }

    private static boolean anyHarvested(GameTestHelper helper, BlockPos[] plots) {
        for (BlockPos plot : plots) {
            if (helper.getBlockState(plot).isAir()) {
                return true;
            }
        }
        return false;
    }

    /** Breaking the farm rail under a working harvester puts it back into its garage and drops its cargo there. */
    @ServusGameTest(timeoutTicks = 600)
    public static void railBrokenUnderHarvester(GameTestHelper helper) {
        BlockPos[] plots = lastRailColumn(helper);
        FarmFixtures.Farm farm = farm(helper, harvester());
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> anyHarvested(helper, plots), 500, () -> "the harvester should start on the last rail", () -> {
                helper.setBlock(new BlockPos(7, 1, RAIL_Z), Blocks.AIR.defaultBlockState());
                Waits.when(helper, () -> out(helper) == 0 && !noItemsLying(helper), 40,
                        () -> "the harvester should be back with its cargo dropped at the garage", () -> {
                    helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 0, "nothing reached storage");
                    helper.succeed();
                });
            });
        });
    }

    /** A recall brings a working harvester in at once and drops its cargo at the garage. */
    @ServusGameTest(timeoutTicks = 600)
    public static void recallMidRun(GameTestHelper helper) {
        BlockPos[] plots = lastRailColumn(helper);
        FarmFixtures.Farm farm = farm(helper, harvester());
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> anyHarvested(helper, plots), 500, () -> "the harvester should start on the last rail", () -> {
                network(helper).recallGarage(helper.absolutePos(GARAGE));
                helper.assertTrue(out(helper) == 0, "the harvester should be in its garage at once");
                helper.assertTrue(!noItemsLying(helper), "its cargo should drop at the garage");
                helper.succeed();
            });
        });
    }

    static final BlockPos SHAFT_BOTTOM = new BlockPos(4, 1, 3);
    static final BlockPos SHAFT_CONTROLLER = new BlockPos(1, 1, 2);
    static final BlockPos SHAFT_GARAGE = new BlockPos(3, 1, 2);
    static final BlockPos SHAFT_RACK = new BlockPos(2, 1, 4);

    private static void shaftBase(GameTestHelper helper) {
        shaftBase(helper, harvester());
    }

    /**
     * The ground floor: a shaft at x 4, z 3 from y 1 to 3, rails x 1 to 3 with the controller north of x 1,
     * the garage with those shuttles north of x 3 and a rack south of x 2. The field east of the shaft is
     * built by the test.
     */
    static void shaftBase(GameTestHelper helper, ItemStack... shuttles) {
        ShuttlesStatsManager.INSTANCE.setFieldWorkTicks(2);
        ShuttlesStatsManager.INSTANCE.setFieldCheckInterval(5);
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        }
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(4, y, 3), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        }
        WarehouseFixtures.controller(helper, SHAFT_CONTROLLER);
        WarehouseFixtures.rack(helper, SHAFT_RACK, Direction.NORTH);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, SHAFT_GARAGE, Direction.NORTH, 0);
        for (int slot = 0; slot < shuttles.length; slot++) {
            garage.shuttles().setStackInSlot(slot, shuttles[slot]);
        }
    }

    /** A rail east of the shaft at that height, farm rails x 6 to 8 and the head at x 9, ripe wheat south of x 7. */
    private static FieldHeadBlockEntity shaftField(GameTestHelper helper, int y, BlockPos[] wheat) {
        helper.setBlock(new BlockPos(5, y - 1, 3), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(5, y, 3), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        for (int x = 6; x <= 8; x++) {
            FieldFixtures.rail(helper, new BlockPos(x, y, 3), ServusShuttlesBlocks.FARM_RAIL.get());
        }
        WarehouseFixtures.stone(helper, new BlockPos(9, y - 1, 3));
        FieldHeadBlockEntity head = FieldFixtures.head(helper, new BlockPos(9, y, 3));
        wheat[0] = new BlockPos(7, y, 4);
        FieldFixtures.crop(helper, wheat[0], Blocks.WHEAT, 7);
        return head;
    }

    static WarehouseNetwork shaftNetwork(GameTestHelper helper) {
        return helper.getBlockEntity(SHAFT_CONTROLLER, WarehouseControllerBlockEntity.class).network().orElseThrow();
    }

    /** Garage and field share the ground floor with the shaft between them: the harvester drives through, the car never moves. */
    @ServusGameTest(timeoutTicks = 600)
    public static void harvesterCrossesShaft(GameTestHelper helper) {
        shaftBase(helper, harvester(), new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        BlockPos[] wheat = new BlockPos[1];
        FieldHeadBlockEntity head = shaftField(helper, 1, wheat);
        BlockPos bottom = helper.absolutePos(SHAFT_BOTTOM);
        boolean[] carMoved = {false};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(SHAFT_CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().map(network -> network.lift(bottom)).ifPresent(car -> carMoved[0] |= car.moving());
            }
        });
        helper.runAfterDelay(2, () -> {
            head.setHarvestOnly(true);
            HeavyRackBlockEntity floor = helper.getBlockEntity(SHAFT_RACK, HeavyRackBlockEntity.class);
            Waits.when(helper, () -> helper.getBlockState(wheat[0]).isAir() && WarehouseFixtures.count(floor, Items.WHEAT) == 1
                    && shaftNetwork(helper).garageOut(helper.absolutePos(SHAFT_GARAGE)) == 0, 550,
                    () -> "the wheat should be harvested into the rack, rack holds " + WarehouseFixtures.count(floor, Items.WHEAT), () -> {
                helper.assertTrue(!carMoved[0], "the harvester crossed without calling the car");
                helper.succeed();
            });
        });
    }

    /** The field lies one floor up and the shaft is the only way: the head says so and the harvester stays home. */
    @ServusGameTest(timeoutTicks = 300)
    public static void harvesterRefusesLift(GameTestHelper helper) {
        shaftBase(helper);
        BlockPos[] wheat = new BlockPos[1];
        FieldHeadBlockEntity head = shaftField(helper, 3, wheat);
        int[] wentOut = {0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(SHAFT_CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().ifPresent(network -> wentOut[0] = Math.max(wentOut[0], network.garageOut(helper.absolutePos(SHAFT_GARAGE))));
            }
        });
        helper.runAfterDelay(2, () -> {
            head.setHarvestOnly(true);
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(head.work() == FieldWork.NO_WAY, "the head says no harvester gets there without a lift, got " + head.work());
                helper.assertTrue(head.getBlockState().getValue(FieldHeadBlock.PROBLEM), "the bar on the head is red");
                helper.assertTrue(wentOut[0] == 0, "the harvester never left its garage");
                helper.assertTrue(ripe(helper, wheat[0]), "the wheat stays ripe on the field");
                helper.succeed();
            });
        });
    }

    /**
     * Field and garage lie on the ground floor, the rack zoned for wheat
     * upstairs behind the lift: the harvester unloads at the head and a plain
     * shuttle rides the lift with the wheat to the zoned rack.
     */
    @ServusGameTest(timeoutTicks = 900)
    public static void harvestReachesStorageUpstairs(GameTestHelper helper) {
        shaftBase(helper, harvester(), new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        helper.setBlock(new BlockPos(3, 3, 3), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        BlockPos upstairs = new BlockPos(3, 3, 4);
        HeavyRackBlockEntity zoned = WarehouseFixtures.rack(helper, upstairs, Direction.NORTH);
        zoned.setZone(ZoneFixtures.zone(helper, "Grain", new ZoneFilter(List.of(new ItemStack(Items.WHEAT)), List.of())));
        BlockPos[] wheat = new BlockPos[1];
        FieldHeadBlockEntity head = shaftField(helper, 1, wheat);
        helper.runAfterDelay(2, () -> {
            head.setHarvestOnly(true);
            HeavyRackBlockEntity floor = helper.getBlockEntity(SHAFT_RACK, HeavyRackBlockEntity.class);
            Waits.when(helper, () -> helper.getBlockState(wheat[0]).isAir() && WarehouseFixtures.count(zoned, Items.WHEAT) == 1
                    && shaftNetwork(helper).garageOut(helper.absolutePos(SHAFT_GARAGE)) == 0, 850,
                    () -> "the wheat should reach the zoned rack upstairs, it holds " + WarehouseFixtures.count(zoned, Items.WHEAT)
                            + ", the floor rack " + WarehouseFixtures.count(floor, Items.WHEAT), () -> {
                helper.assertTrue(WarehouseFixtures.count(floor, Items.WHEAT) == 0, "the rack on the harvester's floor got no wheat");
                helper.assertTrue(!head.toStorageHoldsItems() || toStorageCount(head, Items.WHEAT) == 0, "the head's To storage buffer holds no wheat");
                helper.succeed();
            });
        });
    }

    /** With only Harvester shuttles the harvest stays in the head's To storage buffer and the head says nobody collects it. */
    @ServusGameTest(timeoutTicks = 500)
    public static void onlyHarvestersLeaveTheHarvestAtTheHead(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        FarmFixtures.Farm farm = farm(helper, harvester());
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> helper.getBlockState(plot).isAir() && out(helper) == 0 && toStorage(farm, Items.WHEAT) == 1, 450,
                    () -> "the wheat should wait in the head's To storage buffer, it holds " + toStorage(farm, Items.WHEAT), () -> {
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 0, "no wheat reaches the rack");
                helper.assertTrue(farm.head().work() == FieldWork.COLLECT_NO_SHUTTLE, "the head says nobody collects: " + farm.head().work());
                helper.succeed();
            });
        });
    }

    /** A To storage buffer without a free slot sends no harvester out. */
    @ServusGameTest(timeoutTicks = 200)
    public static void fullToStorageQueuesNoFieldJob(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        FarmFixtures.Farm farm = farm(helper, harvester());
        for (int slot = 0; slot < farm.head().toStorage().getSlots(); slot++) {
            farm.head().toStorage().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(ripe(helper, plot) && out(helper) == 0, "the wheat stays ripe and the harvester home");
                helper.assertTrue(!network(helper).hasFieldJob(farm.head().getBlockPos()), "no field job is queued");
                helper.succeed();
            });
        });
    }

    /** The To storage buffer is kept when the head is saved and loaded, and drops when the head is broken. */
    @ServusGameTest(timeoutTicks = 60)
    public static void toStorageSurvivesReloadAndDrops(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, harvester());
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            List<ItemStack> aboard = head.unloadToStorage(List.of(new ItemStack(Items.WHEAT, 64), new ItemStack(Items.WHEAT_SEEDS, 12)));
            helper.assertTrue(aboard.isEmpty(), "the buffer takes the wheat and seeds");
            CompoundTag tag = head.saveWithFullMetadata(helper.getLevel().registryAccess());
            FieldHeadBlockEntity copy = new FieldHeadBlockEntity(head.getBlockPos(), head.getBlockState());
            copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
            helper.assertTrue(toStorageCount(copy, Items.WHEAT) == 64 && toStorageCount(copy, Items.WHEAT_SEEDS) == 12,
                    "the To storage buffer should survive saving, got " + toStorageCount(copy, Items.WHEAT) + " wheat");
            helper.setBlock(HEAD, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(dropped(helper, Items.WHEAT) == 64 && dropped(helper, Items.WHEAT_SEEDS) == 12,
                        "the buffer drops with the head, got " + dropped(helper, Items.WHEAT) + " wheat");
                helper.succeed();
            });
        });
    }

    private static int dropped(GameTestHelper helper, Item item) {
        int count = 0;
        for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }
}
