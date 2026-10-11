package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.field.FieldCropStockPayload;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.storage.zone.ZoneFilter;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Harvester shuttles on a small farm: a controller, Warehouse Rails x 1 to 3
 * and farm rails x 4 to 7 along z 6, a garage and a quad crate on the
 * Warehouse Rails and the Field Head east of the farm rails. The field's
 * plots are x 0 to 11, z 2 to 10 at y 1, the farmland under them at y 0.
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HarvesterGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final int RAIL_Z = 6;
    private static final BlockPos CONTROLLER = new BlockPos(0, 1, RAIL_Z);
    private static final BlockPos GARAGE = new BlockPos(1, 1, RAIL_Z + 1);
    private static final BlockPos CRATE = new BlockPos(2, 1, RAIL_Z - 1);
    private static final BlockPos SECOND_CRATE = new BlockPos(3, 1, RAIL_Z + 1);
    private static final BlockPos HEAD = new BlockPos(8, 1, RAIL_Z);

    private HarvesterGameTests() {
    }

    // Helpers

    private record Farm(FieldHeadBlockEntity head, ShuttleGarageBlockEntity garage, CrateBlockEntity crate) {
    }

    /** A shuttle with a Harvester in its tool slot. */
    private static ItemStack harvester() {
        ItemStack shuttle = new ItemStack(ServusStorageItems.SHUTTLE_MK1.get());
        ShuttleModules.set(shuttle, ShuttleModules.EMPTY.withTool(Optional.of(ServusStorageItems.HARVESTER.get())));
        return shuttle;
    }

    private static void rail(GameTestHelper helper, BlockPos pos, Block rail) {
        helper.setBlock(pos.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(pos, Block.updateFromNeighbourShapes(rail.defaultBlockState(), helper.getLevel(), helper.absolutePos(pos)));
    }

    /** Builds the farm with that shuttle in the garage, quick work and checks, and the given storage block. */
    private static Farm farm(GameTestHelper helper, ItemStack shuttle, Block storage) {
        StorageStatsManager.INSTANCE.setFieldWorkTicks(2);
        StorageStatsManager.INSTANCE.setFieldCheckInterval(5);
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 1; x <= 7; x++) {
            rail(helper, new BlockPos(x, 1, RAIL_Z), x <= 3 ? ServusStorageBlocks.RAIL.get() : ServusStorageBlocks.FARM_RAIL.get());
        }
        helper.setBlock(GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(CRATE, storage.defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(HEAD, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(GARAGE);
        garage.shuttles().setStackInSlot(0, shuttle);
        return new Farm(helper.getBlockEntity(HEAD), garage, helper.getBlockEntity(CRATE));
    }

    private static Farm farm(GameTestHelper helper, ItemStack shuttle) {
        return farm(helper, shuttle, ServusStorageBlocks.QUAD_CRATE.get());
    }

    private static void water(Farm farm) {
        farm.head().fluidHandler().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
    }

    /** A crop of that age on farmland. */
    private static void crop(GameTestHelper helper, BlockPos pos, Block block, int age) {
        helper.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(pos, ((CropBlock) block).getStateForAge(age));
    }

    private static boolean ripe(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private static boolean young(GameTestHelper helper, BlockPos pos, Block block) {
        BlockState state = helper.getBlockState(pos);
        return state.is(block) && ((CropBlock) block).getAge(state) == 0;
    }

    /** How many of the item the crate holds over all its compartments. */
    private static int held(CrateBlockEntity crate, Item item) {
        int count = 0;
        for (int i = 0; i < crate.compartments(); i++) {
            if (crate.type(i).is(item)) {
                count += crate.count(i);
            }
        }
        return count;
    }

    private static WarehouseNetwork network(GameTestHelper helper) {
        return ((WarehouseControllerBlockEntity) helper.getBlockEntity(CONTROLLER)).network().orElseThrow();
    }

    private static int out(GameTestHelper helper) {
        return network(helper).garageOut(helper.absolutePos(GARAGE));
    }

    private static boolean noItemsLying(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).isEmpty();
    }

    // Module

    /** The Harvester goes into a shuttle in the workshop through the install slot. */
    @GameTest(template = EMPTY)
    public static void workshopFitsHarvester(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = new BlockPos(5, 1, 5);
        helper.setBlock(at, ServusStorageBlocks.SHUTTLE_WORKSHOP.get().defaultBlockState());
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, at);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE_MK1.get()));
        player.getInventory().setItem(1, new ItemStack(ServusStorageItems.HARVESTER.get()));
        int hotbar = ShuttleWorkshopMenu.INVENTORY_START + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).tool().equals(Optional.of(ServusStorageItems.HARVESTER.get())),
                "the Harvester is the shuttle's tool");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.INSTALL_SLOT).getItem().isEmpty(), "the install slot stays empty");
        helper.assertTrue(player.getInventory().getItem(1).isEmpty(), "the Harvester item left the inventory");
        helper.succeed();
    }

    // Field runs

    /** Ripe wheat is harvested into the crate and every plot is planted again with seeds from the harvest. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void harvesterHarvestsIntoStorage(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1), new BlockPos(7, 1, RAIL_Z - 1)};
        for (BlockPos plot : plots) {
            crop(helper, plot, Blocks.WHEAT, 7);
        }
        Farm farm = farm(helper, harvester());
        helper.runAfterDelay(2, () -> {
            water(farm);
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            TerminalWiring.when(helper, () -> {
                for (BlockPos plot : plots) {
                    if (!young(helper, plot, Blocks.WHEAT)) {
                        return false;
                    }
                }
                return held(farm.crate(), Items.WHEAT) == plots.length && out(helper) == 0;
            }, 550, () -> "the four wheat should be in the crate and replanted, crate holds " + held(farm.crate(), Items.WHEAT)
                    + " wheat, harvester out " + out(helper), () -> {
                helper.assertTrue(noItemsLying(helper), "nothing should drop on the ground");
                helper.assertTrue(farm.head().ripeCount() == 0 && farm.head().plantedCount() == plots.length,
                        "the head should see four young crops, got " + farm.head().plantedCount() + " planted " + farm.head().ripeCount() + " ripe");
                helper.succeed();
            });
        });
    }

    /** Dirt and grass under empty plots in the strip become farmland with wheat; dirt five blocks out and stone stay. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void harvesterTillsInsideStripOnly(GameTestHelper helper) {
        BlockPos dirt = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos grass = new BlockPos(6, 1, RAIL_Z + 1);
        BlockPos stone = new BlockPos(7, 1, RAIL_Z + 3);
        BlockPos outside = new BlockPos(5, 1, RAIL_Z + 5);
        helper.setBlock(dirt.below(), Blocks.DIRT.defaultBlockState());
        helper.setBlock(grass.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        helper.setBlock(stone.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(outside.below(), Blocks.DIRT.defaultBlockState());
        Farm farm = farm(helper, harvester());
        farm.crate().insert(new ItemStack(Items.WHEAT_SEEDS, 16), false);
        helper.runAfterDelay(2, () -> {
            water(farm);
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            TerminalWiring.when(helper, () -> young(helper, dirt, Blocks.WHEAT) && young(helper, grass, Blocks.WHEAT) && out(helper) == 0,
                    550, () -> "the dirt and grass plots should be tilled and planted", () -> {
                helper.assertBlockPresent(Blocks.FARMLAND, dirt.below());
                helper.assertBlockPresent(Blocks.FARMLAND, grass.below());
                helper.assertBlockPresent(Blocks.STONE, stone.below());
                helper.assertBlockPresent(Blocks.DIRT, outside.below());
                helper.assertTrue(helper.getBlockState(outside).isAir(), "nothing is planted five blocks out");
                helper.assertTrue(held(farm.crate(), Items.WHEAT_SEEDS) == 14, "two seeds should have come from the crate, it holds "
                        + held(farm.crate(), Items.WHEAT_SEEDS));
                helper.succeed();
            });
        });
    }

    /**
     * The crop picker's stock: the menu counts the crop items the warehouse
     * holds, only non zero ones, and follows the crate as seeds come in.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void menuCropStockCounts(GameTestHelper helper) {
        Farm farm = farm(helper, harvester());
        farm.crate().insert(new ItemStack(Items.WHEAT_SEEDS, 40), false);
        farm.crate().insert(new ItemStack(Items.COBBLESTONE, 5), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FieldHeadMenu menu = new FieldHeadMenu(1, player.getInventory(), farm.head());
        ResourceLocation wheat = BuiltInRegistries.ITEM.getKey(Items.WHEAT_SEEDS);
        ResourceLocation beetroot = BuiltInRegistries.ITEM.getKey(Items.BEETROOT_SEEDS);
        TerminalWiring.when(helper, () -> menu.stockPayload().counts().getOrDefault(wheat, 0L) == 40, 150,
                () -> "the menu should count 40 wheat seeds, got " + menu.stockPayload(), () -> {
            FieldCropStockPayload payload = menu.stockPayload();
            helper.assertTrue(payload.networked(), "the head's field is on a warehouse");
            helper.assertTrue(!payload.counts().containsKey(beetroot), "beetroot seeds hold none and are not sent");
            helper.assertTrue(!payload.counts().containsKey(BuiltInRegistries.ITEM.getKey(Items.COBBLESTONE)), "cobblestone is no crop");
            helper.assertTrue(FieldHeadBlockEntity.cropItems().contains(Items.BEETROOT_SEEDS)
                    && FieldHeadBlockEntity.cropItems().contains(Items.CARROT)
                    && !FieldHeadBlockEntity.cropItems().contains(Items.COBBLESTONE), "the crop items come from the registry");
            farm.crate().insert(new ItemStack(Items.BEETROOT_SEEDS, 3), false);
            TerminalWiring.when(helper, () -> menu.stockPayload().counts().getOrDefault(beetroot, 0L) == 3, 40,
                    () -> "the menu should follow the crate to 3 beetroot seeds, got " + menu.stockPayload(), helper::succeed);
        });
    }

    /** Harvest only: ripe wheat is harvested and the farmland stays empty, an empty farmland plot is left alone. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void harvestOnlyDoesNotPlant(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1)};
        for (BlockPos plot : plots) {
            crop(helper, plot, Blocks.WHEAT, 7);
        }
        BlockPos empty = new BlockPos(4, 1, RAIL_Z + 1);
        helper.setBlock(empty.below(), Blocks.FARMLAND.defaultBlockState());
        Farm farm = farm(helper, harvester());
        farm.crate().insert(new ItemStack(Items.WHEAT_SEEDS, 16), false);
        helper.runAfterDelay(2, () -> {
            water(farm);
            farm.head().setHarvestOnly(true);
            TerminalWiring.when(helper, () -> held(farm.crate(), Items.WHEAT) == 2 && out(helper) == 0, 550,
                    () -> "the two wheat should be in the crate, it holds " + held(farm.crate(), Items.WHEAT), () -> {
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
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void plainShuttleTakesNoFieldJob(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        crop(helper, plot, Blocks.WHEAT, 7);
        Farm farm = farm(helper, new ItemStack(ServusStorageItems.SHUTTLE_MK1.get()));
        helper.runAfterDelay(2, () -> {
            water(farm);
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(farm.head().harvestDue(), "the harvest should be due");
                helper.assertTrue(ripe(helper, plot), "the wheat should still stand");
                helper.assertTrue(out(helper) == 0 && network(helper).queuedJobs() == 0, "no shuttle should be out and no job queued");
                helper.succeed();
            });
        });
    }

    /** With only a harvester, a request from a crate beside a rail is served instantly and the harvester stays home. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void harvesterTakesNoWarehouseJob(GameTestHelper helper) {
        BlockPos packing = new BlockPos(1, 1, RAIL_Z - 1);
        Farm farm = farm(helper, harvester());
        TerminalWiring.place(helper, packing);
        farm.crate().insert(new ItemStack(Items.COBBLESTONE, 20), false);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = network(helper);
            helper.assertTrue(!network.canReach(helper.absolutePos(CRATE)), "a harvester must not count as a shuttle for warehouse jobs");
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, packing);
            new StorageTerminalMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), terminal)
                    .request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 20);
            helper.runAfterDelay(2, () -> helper.assertTrue(out(helper) == 0, "the harvester stays in its garage"));
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 20, 250,
                    () -> "the cobblestone should arrive, got " + terminal.outputCount(Items.COBBLESTONE), () -> {
                helper.assertTrue(out(helper) == 0, "the harvester never left");
                helper.succeed();
            });
        });
    }

    /** Nothing ripe and empty farmland: the harvester picks seeds from the crate and plants. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void seedsFromStorage(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1), new BlockPos(7, 1, RAIL_Z + 1)};
        for (BlockPos plot : plots) {
            helper.setBlock(plot.below(), Blocks.FARMLAND.defaultBlockState());
        }
        Farm farm = farm(helper, harvester());
        farm.crate().insert(new ItemStack(Items.WHEAT_SEEDS, 10), false);
        helper.runAfterDelay(2, () -> {
            water(farm);
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            TerminalWiring.when(helper, () -> {
                for (BlockPos plot : plots) {
                    if (!young(helper, plot, Blocks.WHEAT)) {
                        return false;
                    }
                }
                return out(helper) == 0;
            }, 550, () -> "the three plots should be planted", () -> {
                helper.assertTrue(held(farm.crate(), Items.WHEAT_SEEDS) == 7, "three seeds should have left the crate, it holds "
                        + held(farm.crate(), Items.WHEAT_SEEDS));
                helper.succeed();
            });
        });
    }

    /** Wheat, carrots and beetroot do not fit two stacks at once: the harvester puts away and comes back for the rest. */
    @GameTest(template = EMPTY, timeoutTicks = 800)
    public static void fullCargoComesBack(GameTestHelper helper) {
        BlockPos wheat = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos carrots = new BlockPos(5, 1, RAIL_Z + 1);
        BlockPos beetroot = new BlockPos(6, 1, RAIL_Z - 1);
        crop(helper, wheat, Blocks.WHEAT, 7);
        crop(helper, carrots, Blocks.CARROTS, 7);
        crop(helper, beetroot, Blocks.BEETROOTS, 3);
        Farm farm = farm(helper, harvester());
        helper.setBlock(SECOND_CRATE, ServusStorageBlocks.QUAD_CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.SOUTH));
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            CrateBlockEntity second = helper.getBlockEntity(SECOND_CRATE);
            TerminalWiring.when(helper, () -> helper.getBlockState(wheat).isAir() && helper.getBlockState(carrots).isAir()
                    && helper.getBlockState(beetroot).isAir() && out(helper) == 0, 750, () -> "all three crops should be harvested", () -> {
                int beetroots = held(farm.crate(), Items.BEETROOT) + held(second, Items.BEETROOT);
                int wheats = held(farm.crate(), Items.WHEAT) + held(second, Items.WHEAT);
                int carrotCount = held(farm.crate(), Items.CARROT) + held(second, Items.CARROT);
                helper.assertTrue(beetroots == 1 && wheats == 1 && carrotCount >= 1,
                        "the harvest should be in storage, got " + wheats + " wheat " + carrotCount + " carrots " + beetroots + " beetroot");
                helper.assertTrue(noItemsLying(helper), "nothing should drop on the ground");
                helper.succeed();
            });
        });
    }

    /** Wheat goes into the crate zoned for wheat, the seeds into general storage. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void harvestFollowsZones(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        crop(helper, plot, Blocks.WHEAT, 7);
        Farm farm = farm(helper, harvester());
        helper.setBlock(SECOND_CRATE, ServusStorageBlocks.QUAD_CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.SOUTH));
        CrateBlockEntity zoned = helper.getBlockEntity(SECOND_CRATE);
        zoned.setZone(new ZoneDefinition("Grain", new ZoneFilter(List.of(new ItemStack(Items.WHEAT)), List.of())));
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            TerminalWiring.when(helper, () -> helper.getBlockState(plot).isAir() && out(helper) == 0, 550,
                    () -> "the wheat should be harvested", () -> {
                helper.assertTrue(held(zoned, Items.WHEAT) == 1 && held(farm.crate(), Items.WHEAT) == 0, "the wheat should be in the zoned crate");
                helper.assertTrue(held(farm.crate(), Items.WHEAT_SEEDS) >= 1 && held(zoned, Items.WHEAT_SEEDS) == 0,
                        "the seeds should be in the unzoned crate");
                helper.succeed();
            });
        });
    }

    /** Breaking the head mid run: the harvester stops, puts its wheat away and parks; the rest stays ripe. */
    @GameTest(template = EMPTY, timeoutTicks = 800)
    public static void headBrokenMidRun(GameTestHelper helper) {
        BlockPos first = new BlockPos(4, 1, RAIL_Z - 1);
        BlockPos[] rest = {new BlockPos(7, 1, RAIL_Z + 1), new BlockPos(7, 1, RAIL_Z + 2), new BlockPos(7, 1, RAIL_Z + 3)};
        crop(helper, first, Blocks.WHEAT, 7);
        for (BlockPos plot : rest) {
            crop(helper, plot, Blocks.WHEAT, 7);
        }
        Farm farm = farm(helper, harvester());
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            TerminalWiring.when(helper, () -> helper.getBlockState(first).isAir(), 400, () -> "the first wheat should be harvested", () -> {
                helper.setBlock(HEAD, Blocks.AIR.defaultBlockState());
                TerminalWiring.when(helper, () -> out(helper) == 0 && held(farm.crate(), Items.WHEAT) >= 1, 350,
                        () -> "the harvester should put its wheat away and park, crate holds " + held(farm.crate(), Items.WHEAT), () -> {
                    int stillRipe = 0;
                    for (BlockPos plot : rest) {
                        stillRipe += ripe(helper, plot) ? 1 : 0;
                    }
                    helper.assertTrue(stillRipe >= 1, "the harvester should have stopped before the end of the field");
                    helper.succeed();
                });
            });
        });
    }

    /** A full warehouse: the harvester waits at its garage with the carrots and stores them once there is room. */
    @GameTest(template = EMPTY, timeoutTicks = 800)
    public static void warehouseFullWaits(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        crop(helper, plot, Blocks.CARROTS, 7);
        Farm farm = farm(helper, harvester(), ServusStorageBlocks.CRATE.get());
        farm.crate().insert(new ItemStack(Items.COBBLESTONE, farm.crate().capacity()), false);
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            TerminalWiring.when(helper, () -> helper.getBlockState(plot).isAir(), 300, () -> "the carrots should be harvested", () ->
                    helper.runAfterDelay(80, () -> {
                        helper.assertTrue(out(helper) == 1, "the harvester should wait out with its carrots");
                        helper.assertTrue(noItemsLying(helper), "the carrots must not drop");
                        farm.crate().clear();
                        TerminalWiring.when(helper, () -> held(farm.crate(), Items.CARROT) >= 1 && out(helper) == 0, 300,
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
                crop(helper, plots[i++], Blocks.WHEAT, 7);
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
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void railBrokenUnderHarvester(GameTestHelper helper) {
        BlockPos[] plots = lastRailColumn(helper);
        Farm farm = farm(helper, harvester());
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            TerminalWiring.when(helper, () -> anyHarvested(helper, plots), 500, () -> "the harvester should start on the last rail", () -> {
                helper.setBlock(new BlockPos(7, 1, RAIL_Z), Blocks.AIR.defaultBlockState());
                TerminalWiring.when(helper, () -> out(helper) == 0 && !noItemsLying(helper), 40,
                        () -> "the harvester should be back with its cargo dropped at the garage", () -> {
                    helper.assertTrue(held(farm.crate(), Items.WHEAT) == 0, "nothing reached storage");
                    helper.succeed();
                });
            });
        });
    }

    /** A recall brings a working harvester in at once and drops its cargo at the garage. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void recallMidRun(GameTestHelper helper) {
        BlockPos[] plots = lastRailColumn(helper);
        Farm farm = farm(helper, harvester());
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            TerminalWiring.when(helper, () -> anyHarvested(helper, plots), 500, () -> "the harvester should start on the last rail", () -> {
                network(helper).recallGarage(helper.absolutePos(GARAGE));
                helper.assertTrue(out(helper) == 0, "the harvester should be in its garage at once");
                helper.assertTrue(!noItemsLying(helper), "its cargo should drop at the garage");
                helper.succeed();
            });
        });
    }

    // Lifts: a shaft at x 4, z 3 from y 1 to 3. Ground rails x 1 to 3 with the controller north of x 1,
    // the garage north of x 3 and a crate south of x 2; the field lies east of the shaft.

    private static final BlockPos SHAFT_BOTTOM = new BlockPos(4, 1, 3);
    private static final BlockPos SHAFT_CONTROLLER = new BlockPos(1, 1, 2);
    private static final BlockPos SHAFT_GARAGE = new BlockPos(3, 1, 2);
    private static final BlockPos SHAFT_CRATE = new BlockPos(2, 1, 4);

    /** The ground floor with a Harvester in the garage, a crate and the shaft; the field is built by the test. */
    private static void shaftBase(GameTestHelper helper) {
        StorageStatsManager.INSTANCE.setFieldWorkTicks(2);
        StorageStatsManager.INSTANCE.setFieldCheckInterval(5);
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(4, y, 3), ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        }
        helper.setBlock(SHAFT_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(SHAFT_CRATE, ServusStorageBlocks.QUAD_CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.SOUTH));
        helper.setBlock(SHAFT_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(SHAFT_GARAGE);
        garage.shuttles().setStackInSlot(0, harvester());
    }

    /** A rail east of the shaft at that height, farm rails x 6 to 8 and the head at x 9, ripe wheat south of x 7. */
    private static FieldHeadBlockEntity shaftField(GameTestHelper helper, int y, BlockPos[] wheat) {
        helper.setBlock(new BlockPos(5, y - 1, 3), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(5, y, 3), ServusStorageBlocks.RAIL.get().defaultBlockState());
        for (int x = 6; x <= 8; x++) {
            rail(helper, new BlockPos(x, y, 3), ServusStorageBlocks.FARM_RAIL.get());
        }
        helper.setBlock(new BlockPos(9, y - 1, 3), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(9, y, 3), ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        wheat[0] = new BlockPos(7, y, 4);
        crop(helper, wheat[0], Blocks.WHEAT, 7);
        return helper.getBlockEntity(new BlockPos(9, y, 3));
    }

    private static WarehouseNetwork shaftNetwork(GameTestHelper helper) {
        return ((WarehouseControllerBlockEntity) helper.getBlockEntity(SHAFT_CONTROLLER)).network().orElseThrow();
    }

    /** Garage and field share the ground floor with the shaft between them: the harvester drives through, the car never moves. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void harvesterCrossesShaft(GameTestHelper helper) {
        shaftBase(helper);
        BlockPos[] wheat = new BlockPos[1];
        FieldHeadBlockEntity head = shaftField(helper, 1, wheat);
        BlockPos bottom = helper.absolutePos(SHAFT_BOTTOM);
        boolean[] carMoved = {false};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(SHAFT_CONTROLLER) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().map(network -> network.lift(bottom)).ifPresent(car -> carMoved[0] |= car.moving());
            }
        });
        helper.runAfterDelay(2, () -> {
            head.setHarvestOnly(true);
            CrateBlockEntity crate = helper.getBlockEntity(SHAFT_CRATE);
            TerminalWiring.when(helper, () -> helper.getBlockState(wheat[0]).isAir() && held(crate, Items.WHEAT) == 1
                    && shaftNetwork(helper).garageOut(helper.absolutePos(SHAFT_GARAGE)) == 0, 550,
                    () -> "the wheat should be harvested into the crate, crate holds " + held(crate, Items.WHEAT), () -> {
                helper.assertTrue(!carMoved[0], "the harvester crossed without calling the car");
                helper.succeed();
            });
        });
    }

    /** The field lies one floor up and the shaft is the only way: the head says so and the harvester stays home. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void harvesterRefusesLift(GameTestHelper helper) {
        shaftBase(helper);
        BlockPos[] wheat = new BlockPos[1];
        FieldHeadBlockEntity head = shaftField(helper, 3, wheat);
        int[] wentOut = {0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(SHAFT_CONTROLLER) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().ifPresent(network -> wentOut[0] = Math.max(wentOut[0], network.garageOut(helper.absolutePos(SHAFT_GARAGE))));
            }
        });
        helper.runAfterDelay(2, () -> {
            head.setHarvestOnly(true);
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(head.work() == FieldWork.NO_WAY, "the head says no harvester gets there without a lift, got " + head.work());
                helper.assertTrue(wentOut[0] == 0, "the harvester never left its garage");
                helper.assertTrue(ripe(helper, wheat[0]), "the wheat stays ripe on the field");
                helper.succeed();
            });
        });
    }

    /** The crate zoned for wheat stands upstairs behind the lift: the harvester skips it and fills the crate on its floor. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void harvestSkipsStorageUpstairs(GameTestHelper helper) {
        shaftBase(helper);
        helper.setBlock(new BlockPos(3, 3, 3), ServusStorageBlocks.RAIL.get().defaultBlockState());
        BlockPos upstairs = new BlockPos(3, 3, 4);
        helper.setBlock(upstairs, ServusStorageBlocks.QUAD_CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.SOUTH));
        CrateBlockEntity zoned = helper.getBlockEntity(upstairs);
        zoned.setZone(new ZoneDefinition("Grain", new ZoneFilter(List.of(new ItemStack(Items.WHEAT)), List.of())));
        BlockPos[] wheat = new BlockPos[1];
        FieldHeadBlockEntity head = shaftField(helper, 1, wheat);
        helper.runAfterDelay(2, () -> {
            head.setHarvestOnly(true);
            CrateBlockEntity crate = helper.getBlockEntity(SHAFT_CRATE);
            TerminalWiring.when(helper, () -> helper.getBlockState(wheat[0]).isAir()
                    && shaftNetwork(helper).garageOut(helper.absolutePos(SHAFT_GARAGE)) == 0, 550,
                    () -> "the wheat should be harvested", () -> {
                helper.assertTrue(held(crate, Items.WHEAT) == 1, "the wheat went into the crate on the harvester's floor, it holds " + held(crate, Items.WHEAT));
                helper.assertTrue(held(zoned, Items.WHEAT) == 0, "the zoned crate upstairs got nothing");
                helper.succeed();
            });
        });
    }
}
