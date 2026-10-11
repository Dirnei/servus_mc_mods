package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWater;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Farm rails, fields and the Field Head: water, hydration and ripeness. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FieldGameTests {

    private static final String EMPTY = "empty_12x6x12";
    /** The usual field: three farm rails along x at z 5, the head west of them. */
    private static final int RAIL_Z = 5;
    private static final BlockPos HEAD = new BlockPos(2, 1, RAIL_Z);

    private FieldGameTests() {
    }

    // Helpers

    /** Places a farm rail on stone the way a player would. */
    private static void farmRail(GameTestHelper helper, BlockPos pos) {
        rail(helper, pos, ServusStorageBlocks.FARM_RAIL.get().defaultBlockState());
    }

    private static void rail(GameTestHelper helper, BlockPos pos, BlockState rail) {
        helper.setBlock(pos.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(pos, Block.updateFromNeighbourShapes(rail, helper.getLevel(), helper.absolutePos(pos)));
    }

    /** Farm rails from x {@code from} to {@code to} at the rail row and the head west of them. */
    private static FieldHeadBlockEntity field(GameTestHelper helper, int from, int to) {
        for (int x = from; x <= to; x++) {
            farmRail(helper, new BlockPos(x, 1, RAIL_Z));
        }
        helper.setBlock(HEAD, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        return helper.getBlockEntity(HEAD);
    }

    private static IFluidHandler fluid(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
    }

    private static boolean wet(GameTestHelper helper, int x, int y, int z) {
        return FarmlandWaterManager.hasBlockWaterTicket(helper.getLevel(), helper.absolutePos(new BlockPos(x, y, z)));
    }

    /** A crop of that age on farmland. */
    private static void crop(GameTestHelper helper, BlockPos pos, Block block, int age) {
        helper.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(pos, ((CropBlock) block).getStateForAge(age));
    }

    private static Optional<WarehouseNetwork> network(GameTestHelper helper, BlockPos pos) {
        return ((StorageNodeBlockEntity) helper.getBlockEntity(pos)).network();
    }

    // Farm Rail

    /** Controller, a Warehouse Rail, farm rails and a crate: one network, the two rail kinds link, no water inside. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void farmRailIsARail(GameTestHelper helper) {
        BlockPos controller = new BlockPos(2, 1, 3);
        BlockPos warehouseRail = new BlockPos(3, 1, 3);
        BlockPos crate = new BlockPos(7, 1, 3);
        helper.setBlock(controller, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        rail(helper, warehouseRail, ServusStorageBlocks.RAIL.get().defaultBlockState());
        for (int x = 4; x <= 6; x++) {
            farmRail(helper, new BlockPos(x, 1, 3));
        }
        helper.setBlock(crate, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(10, () -> {
            BlockState first = helper.getBlockState(new BlockPos(4, 1, 3));
            helper.assertTrue(first.getValue(RailBlock.WEST), "the farm rail should connect to the Warehouse Rail");
            helper.assertTrue(helper.getBlockState(warehouseRail).getValue(RailBlock.EAST), "the Warehouse Rail should connect to the farm rail");
            helper.assertTrue(!first.hasProperty(BlockStateProperties.WATERLOGGED), "a farm rail must not be waterloggable");
            helper.assertTrue(helper.getBlockState(new BlockPos(6, 1, 3)).getValue(RailBlock.DOCK_EAST), "the farm rail should dock onto the crate");
            Optional<WarehouseNetwork> crateNetwork = network(helper, crate);
            helper.assertTrue(crateNetwork.isPresent() && crateNetwork.equals(network(helper, controller)),
                    "the crate should be in the controller's warehouse through the farm rails");
            helper.assertTrue(network(helper, new BlockPos(5, 1, 3)).equals(crateNetwork), "the farm rail is a node of the warehouse");
            helper.succeed();
        });
    }

    /** A run of five: removing the middle rail splits it, a Warehouse Rail there keeps it split, a farm rail joins it again. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fieldSplitsAndJoins(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 7);
        BlockPos middle = new BlockPos(5, 1, RAIL_Z);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.status() == FieldStatus.SERVING && head.rails().size() == 5, "the head should serve five rails, got " + head.rails().size());
            helper.setBlock(middle, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(head.rails().size() == 2, "the head's field should be the two rails on its side, got " + head.rails().size());
                rail(helper, middle, ServusStorageBlocks.RAIL.get().defaultBlockState());
                helper.runAfterDelay(3, () -> {
                    helper.assertTrue(head.rails().size() == 2, "a Warehouse Rail must not join the field, got " + head.rails().size());
                    helper.setBlock(middle, Blocks.AIR.defaultBlockState());
                    farmRail(helper, middle);
                    helper.runAfterDelay(3, () -> {
                        helper.assertTrue(head.rails().size() == 5, "the farm rail should join the runs again, got " + head.rails().size());
                        helper.succeed();
                    });
                });
            });
        });
    }

    // Field Head

    /** A head with no farm rail has no field; a second head on the same field stands aside until the first is broken. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void oneHeadPerField(GameTestHelper helper) {
        BlockPos lonely = new BlockPos(9, 1, 9);
        helper.setBlock(lonely, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        FieldHeadBlockEntity first = field(helper, 3, 5);
        BlockPos secondPos = new BlockPos(4, 1, RAIL_Z - 1);
        helper.setBlock(secondPos, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        FieldHeadBlockEntity second = helper.getBlockEntity(secondPos);
        helper.runAfterDelay(4, () -> {
            FieldHeadBlockEntity alone = helper.getBlockEntity(lonely);
            helper.assertTrue(alone.status() == FieldStatus.NO_FIELD, "a head without a farm rail has no field");
            boolean firstServes = first.status() == FieldStatus.SERVING;
            boolean secondServes = second.status() == FieldStatus.SERVING;
            helper.assertTrue(firstServes != secondServes, "exactly one head should serve, got " + first.status() + " and " + second.status());
            FieldHeadBlockEntity idle = firstServes ? second : first;
            BlockPos servingPos = firstServes ? HEAD : secondPos;
            helper.assertTrue(idle.status() == FieldStatus.OTHER_HEAD, "the other head should see another head, got " + idle.status());
            helper.setBlock(servingPos, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(idle.status() == FieldStatus.SERVING, "the remaining head should serve the field, got " + idle.status());
                helper.succeed();
            });
        });
    }

    /** The head takes water, refuses lava and gives nothing back. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void headTakesWaterOnly(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        helper.setBlock(pos, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        IFluidHandler handler = fluid(helper, pos);
        helper.assertTrue(handler != null, "the head should expose a fluid handler");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "the head should take 1000 mB of water");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "the head must refuse lava");
        helper.assertTrue(handler.drain(500, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "the head must not give water back");
        FieldHeadBlockEntity head = helper.getBlockEntity(pos);
        helper.assertTrue(head.water() == 1000, "the buffer should hold 1000 mB, got " + head.water());
        helper.succeed();
    }

    /** A water cauldron pulled in by a Utility Duct fluid line fills the head. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void headFillsThroughThePipe(GameTestHelper helper) {
        BlockPos ductA = new BlockPos(2, 1, 6);
        BlockPos ductB = new BlockPos(3, 1, 6);
        BlockPos ductC = new BlockPos(4, 1, 6);
        helper.setBlock(ductA.north(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(ductC.north(), ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        UtilityDuctBlockEntity a = duct(helper, ductA);
        duct(helper, ductB);
        duct(helper, ductC);
        a.setMode(Line.FLUID, Direction.NORTH, SideMode.INPUT);
        FieldHeadBlockEntity head = helper.getBlockEntity(ductC.north());
        helper.succeedWhen(() -> helper.assertTrue(head.water() == 1000, "the head should hold the cauldron's water, got " + head.water()));
    }

    private static UtilityDuctBlockEntity duct(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
        duct.addLine(Line.FLUID);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }

    // Hydration

    /** With water, farmland one below the rails is wet four blocks out and not five. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void hydrationReachesFourBlocks(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.setBlock(new BlockPos(4, 0, RAIL_Z + 4), Blocks.FARMLAND.defaultBlockState());
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!head.hydrated() && !wet(helper, 4, 0, RAIL_Z + 4), "no water, no hydration");
            fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(head.hydrated(), "the head should hydrate with water in the buffer");
                helper.assertTrue(wet(helper, 4, 0, RAIL_Z + 4), "farmland four blocks beside a rail should be wet");
                helper.assertTrue(!wet(helper, 4, 0, RAIL_Z + 5), "farmland five blocks beside a rail must stay dry");
                helper.assertTrue(wet(helper, 9, 0, RAIL_Z), "farmland four blocks past the end rail should be wet");
                helper.assertTrue(!wet(helper, 10, 0, RAIL_Z), "farmland five blocks past the end rail must stay dry");
                helper.assertTrue(!wet(helper, 4, 1, RAIL_Z + 2), "only the layer below the rails is kept wet");
                helper.assertTrue(head.water() == 1000 - FieldWater.use(3, StorageStatsManager.INSTANCE.fieldWaterPerRail()),
                        "the head should use one interval of water, has " + head.water());
                BlockState farmland = helper.getBlockState(new BlockPos(4, 0, RAIL_Z + 4));
                helper.assertTrue(farmland.is(Blocks.FARMLAND), "the farmland stays");
                helper.succeed();
            });
        });
    }

    /** One interval of water hydrates for one interval, then the field dries. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void hydrationEndsWhenDry(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        int need = FieldWater.use(3, StorageStatsManager.INSTANCE.fieldWaterPerRail());
        int interval = StorageStatsManager.INSTANCE.fieldWaterInterval();
        helper.runAfterDelay(2, () -> {
            fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, need), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(head.hydrated() && wet(helper, 4, 0, RAIL_Z + 2), "the field should be hydrated");
                helper.assertTrue(head.water() == 0, "the head should have used its water");
                helper.runAfterDelay(interval + 2, () -> {
                    helper.assertTrue(!head.hydrated(), "the head should stop once the water is gone");
                    helper.assertTrue(!wet(helper, 4, 0, RAIL_Z + 2), "the farmland should no longer count as near water");
                    helper.succeed();
                });
            });
        });
    }

    /** Breaking the end rail takes its part of the ground out of the field. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void hydrationEndsWithRail(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(2, () -> {
            fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(wet(helper, 9, 0, RAIL_Z), "past the end rail should be wet");
                helper.setBlock(new BlockPos(5, 1, RAIL_Z), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(head.rails().size() == 2, "two rails should remain");
                    helper.assertTrue(!wet(helper, 9, 0, RAIL_Z), "the removed rail's ground should dry");
                    helper.assertTrue(wet(helper, 8, 0, RAIL_Z), "the remaining rails keep their ground wet");
                    helper.setBlock(HEAD, Blocks.AIR.defaultBlockState());
                    helper.runAfterDelay(1, () -> {
                        helper.assertTrue(!wet(helper, 4, 0, RAIL_Z + 2), "a removed head waters nothing");
                        helper.succeed();
                    });
                });
            });
        });
    }

    // Ripeness

    /** A crop growing to its last stage counts as ripe at once. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void ripeAfterGrowth(GameTestHelper helper) {
        BlockPos plot = new BlockPos(4, 1, RAIL_Z + 2);
        crop(helper, plot, Blocks.WHEAT, 3);
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.plantedCount() == 1 && head.ripeCount() == 0, "one planted, none ripe after the load pass");
            BlockState ripe = ((CropBlock) Blocks.WHEAT).getStateForAge(7);
            BlockPos absolute = helper.absolutePos(plot);
            helper.getLevel().setBlock(absolute, ripe, Block.UPDATE_CLIENTS);
            CommonHooks.fireCropGrowPost(helper.getLevel(), absolute, ripe);
            helper.assertTrue(head.ripeCount() == 1, "the grown crop should count as ripe");
            helper.assertTrue(head.ripePlots().contains(absolute), "the ripe plot should be listed");
            helper.succeed();
        });
    }

    /** Bone meal up to the last stage: ripe a tick later. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void ripeAfterBonemeal(GameTestHelper helper) {
        BlockPos plot = new BlockPos(4, 1, RAIL_Z - 2);
        crop(helper, plot, Blocks.CARROTS, 0);
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.plantedCount() == 1 && head.ripeCount() == 0, "one young carrot");
            BlockPos absolute = helper.absolutePos(plot);
            for (int i = 0; i < 20 && !((CropBlock) Blocks.CARROTS).isMaxAge(helper.getLevel().getBlockState(absolute)); i++) {
                BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), helper.getLevel(), absolute, null);
            }
            helper.assertTrue(((CropBlock) Blocks.CARROTS).isMaxAge(helper.getLevel().getBlockState(absolute)), "bone meal should ripen the carrot");
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(head.ripeCount() == 1, "the head should count the carrot ripe, got " + head.ripeCount());
                helper.succeed();
            });
        });
    }

    /** A player breaking a ripe crop takes it out of the counts. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void breakForgetsRipe(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z + 1);
        crop(helper, plot, Blocks.WHEAT, 7);
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.ripeCount() == 1, "the ripe wheat should be counted");
            @SuppressWarnings("removal")
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.gameMode.destroyBlock(helper.absolutePos(plot));
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(head.ripeCount() == 0 && head.plantedCount() == 0, "the broken crop should leave the counts, got "
                        + head.plantedCount() + " planted " + head.ripeCount() + " ripe");
                helper.succeed();
            });
        });
    }

    /** Harvest due needs a crop or harvest only and half of the planted plots ripe. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void harvestDueNeedsCropAndShare(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(3, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1), new BlockPos(5, 1, RAIL_Z + 1), new BlockPos(6, 1, RAIL_Z + 1)};
        crop(helper, plots[0], Blocks.WHEAT, 7);
        for (int i = 1; i < plots.length; i++) {
            crop(helper, plots[i], Blocks.WHEAT, 2);
        }
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.assertTrue(head.plantedCount() == 4 && head.ripeCount() == 1, "four planted, one ripe");
            helper.assertTrue(!head.harvestDue(), "a quarter ripe is not due");
            BlockState ripe = ((CropBlock) Blocks.WHEAT).getStateForAge(7);
            BlockPos absolute = helper.absolutePos(plots[1]);
            helper.getLevel().setBlock(absolute, ripe, Block.UPDATE_CLIENTS);
            CommonHooks.fireCropGrowPost(helper.getLevel(), absolute, ripe);
            helper.assertTrue(head.harvestDue(), "half ripe with wheat set is due");
            head.setCrop(ItemStack.EMPTY);
            helper.assertTrue(!head.harvestDue(), "without a crop or harvest only nothing is due");
            head.setHarvestOnly(true);
            helper.assertTrue(head.harvestDue(), "harvest only is due too");
            helper.succeed();
        });
    }

    /** The GUI's crop slot takes a seed without using it, refuses cobblestone, and harvest only clears it. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void menuSetsCropAndHarvestOnly(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FieldHeadMenu menu = new FieldHeadMenu(1, player.getInventory(), head);
        menu.setCarried(new ItemStack(Items.WHEAT_SEEDS, 5));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(head.crop().is(Items.WHEAT_SEEDS), "wheat seeds should be the crop");
        helper.assertTrue(menu.getCarried().getCount() == 5, "the player keeps the seeds");
        menu.setCarried(new ItemStack(Items.COBBLESTONE));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(head.crop().is(Items.WHEAT_SEEDS), "cobblestone must not become the crop");
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_HARVEST_ONLY);
        helper.assertTrue(head.harvestOnly() && head.crop().isEmpty(), "harvest only should clear the crop");
        menu.setCarried(new ItemStack(Items.CARROT));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(head.crop().is(Items.CARROT) && !head.harvestOnly(), "setting a crop turns harvest only off");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(head.crop().isEmpty(), "an empty hand clears the crop");
        helper.succeed();
    }
}
