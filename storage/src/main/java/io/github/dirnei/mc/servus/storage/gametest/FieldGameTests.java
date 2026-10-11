package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.field.FieldRadius;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
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

    /** Puts that many Range Upgrades into the head. */
    private static void widen(FieldHeadBlockEntity head, int upgrades) {
        head.rangeUpgrades().setStackInSlot(0, new ItemStack(ServusStorageItems.RANGE_UPGRADE.get(), upgrades));
    }

    /** Dirt under the plots from one to four blocks north of rail 4, so the rails see that whole line. */
    private static void dirtLine(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(4, 0, RAIL_Z + z), Blocks.DIRT.defaultBlockState());
        }
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

    /** A shuttle drives from its garage over farm rails to a rack and back to the terminal with the stack. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void shuttleDrivesOverFarmRails(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(1, 1, 3);
        BlockPos terminalPos = new BlockPos(1, 1, 4);
        BlockPos garagePos = new BlockPos(2, 1, 5);
        BlockPos rackPos = new BlockPos(9, 1, 5);
        helper.setBlock(controllerPos, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, terminalPos);
        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            Block rail = x >= 4 && x <= 7 ? ServusStorageBlocks.FARM_RAIL.get() : ServusStorageBlocks.RAIL.get();
            helper.setBlock(new BlockPos(x, 1, 4), rail.defaultBlockState());
        }
        helper.setBlock(new BlockPos(5, 1, 3), ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        helper.setBlock(garagePos, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(rackPos, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(garagePos);
        garage.shuttles().setStackInSlot(0, new ItemStack(ServusStorageItems.SHUTTLE_MK1.get()));
        HeavyRackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 36), false);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, terminalPos);
            new StorageTerminalMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), terminal)
                    .request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 36);
            WarehouseNetwork network = ((WarehouseControllerBlockEntity) helper.getBlockEntity(controllerPos)).network().orElseThrow();
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(network.garageOut(helper.absolutePos(garagePos)) == 1, "the shuttle should be out over the farm rails");
                TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 36, 500,
                        () -> "the 36 cobblestone should arrive, got " + terminal.outputCount(Items.COBBLESTONE), helper::succeed);
            });
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
        widen(head, 2);
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
                helper.assertTrue(wet(helper, 7, 0, RAIL_Z), "past the end rail should be wet");
                helper.setBlock(new BlockPos(5, 1, RAIL_Z), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(head.rails().size() == 2, "two rails should remain");
                    helper.assertTrue(!wet(helper, 7, 0, RAIL_Z), "the removed rail's ground should dry");
                    helper.assertTrue(wet(helper, 6, 0, RAIL_Z), "the remaining rails keep their ground wet");
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
        BlockPos plot = new BlockPos(4, 1, RAIL_Z + 1);
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
        BlockPos plot = new BlockPos(4, 1, RAIL_Z - 1);
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

    /** Each head keeps its own harvest threshold: a quarter ripe is due once the head waits for 20 percent. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void harvestThresholdPerHead(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(3, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1), new BlockPos(5, 1, RAIL_Z + 1), new BlockPos(6, 1, RAIL_Z + 1)};
        crop(helper, plots[0], Blocks.WHEAT, 7);
        for (int i = 1; i < plots.length; i++) {
            crop(helper, plots[i], Blocks.WHEAT, 2);
        }
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.assertTrue(head.harvestPercent() == 50, "a new head follows field.json, got " + head.harvestPercent());
            helper.assertTrue(!head.harvestDue(), "a quarter ripe is not due at 50 percent");
            head.stepHarvestPercent(-3);
            helper.assertTrue(head.harvestPercent() == 20 && head.harvestDue(), "a quarter ripe is due at 20 percent");
            head.stepHarvestPercent(-5);
            helper.assertTrue(head.harvestPercent() == 10, "the threshold stops at 10 percent");
            head.stepHarvestPercent(20);
            helper.assertTrue(head.harvestPercent() == 100 && !head.harvestDue(), "the threshold stops at 100 percent");
            helper.succeed();
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

    /**
     * Each head slot sits on its tab, and shift click routes honeycomb and a
     * Range Upgrade into their slots and back whatever tab is shown.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void menuTabsRouteShiftClick(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FieldHeadMenu menu = new FieldHeadMenu(1, player.getInventory(), head);
        helper.assertTrue(menu.page() == FieldHeadMenu.Page.FIELD, "the GUI opens on the Field tab");
        helper.assertTrue(FieldHeadMenu.pageOf(FieldHeadMenu.CROP_SLOT) == FieldHeadMenu.Page.FIELD
                && FieldHeadMenu.pageOf(FieldHeadMenu.ANTI_CORROSION_SLOT) == FieldHeadMenu.Page.WATER
                && FieldHeadMenu.pageOf(FieldHeadMenu.RANGE_SLOT) == FieldHeadMenu.Page.UPGRADES
                && FieldHeadMenu.pageOf(FieldHeadMenu.INVENTORY_START) == null, "each head slot belongs to its tab");
        helper.assertTrue(menu.slots.get(FieldHeadMenu.RANGE_SLOT).isActive()
                && menu.slots.get(FieldHeadMenu.ANTI_CORROSION_SLOT).isActive(), "head slots stay active on the server");
        player.getInventory().setItem(9, new ItemStack(Items.HONEYCOMB, 3));
        player.getInventory().setItem(10, new ItemStack(ServusStorageItems.RANGE_UPGRADE.get()));
        menu.quickMoveStack(player, FieldHeadMenu.INVENTORY_START);
        menu.quickMoveStack(player, FieldHeadMenu.INVENTORY_START + 1);
        helper.assertTrue(head.antiCorrosion().getStackInSlot(0).is(Items.HONEYCOMB)
                && head.antiCorrosion().getStackInSlot(0).getCount() == 3, "honeycomb goes into the anti-corrosion slot from the Field tab");
        helper.assertTrue(head.rangeUpgrades().getStackInSlot(0).is(ServusStorageItems.RANGE_UPGRADE.get()),
                "the Range Upgrade goes into its slot from the Field tab");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(10).isEmpty(), "both left the inventory");
        menu.setPage(FieldHeadMenu.Page.UPGRADES);
        menu.quickMoveStack(player, FieldHeadMenu.RANGE_SLOT);
        menu.quickMoveStack(player, FieldHeadMenu.ANTI_CORROSION_SLOT);
        helper.assertTrue(head.rangeUpgrades().getStackInSlot(0).isEmpty() && head.antiCorrosion().getStackInSlot(0).isEmpty(),
                "shift click from the slots empties them");
        helper.assertTrue(player.getInventory().countItem(Items.HONEYCOMB) == 3
                && player.getInventory().countItem(ServusStorageItems.RANGE_UPGRADE.get()) == 1, "both are back in the inventory");
        helper.succeed();
    }

    // Open plots

    /** Dirt under an empty plot is to till, farmland to plant; hoeing and planting by hand move the plots along. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void openPlotsCounted(GameTestHelper helper) {
        BlockPos dirtPlot = new BlockPos(4, 1, RAIL_Z + 1);
        BlockPos farmPlot = new BlockPos(5, 1, RAIL_Z - 1);
        helper.setBlock(dirtPlot.below(), Blocks.DIRT.defaultBlockState());
        helper.setBlock(farmPlot.below(), Blocks.FARMLAND.defaultBlockState());
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.tillableCount() == 1 && head.plantableCount() == 1,
                    "one plot to till and one to plant, got " + head.tillableCount() + " and " + head.plantableCount());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack hoe = new ItemStack(Items.IRON_HOE);
            player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
            BlockPos dirt = helper.absolutePos(dirtPlot.below());
            hoe.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, hoe,
                    new BlockHitResult(Vec3.atCenterOf(dirt).add(0, 0.5, 0), Direction.UP, dirt, false)));
            helper.assertBlockPresent(Blocks.FARMLAND, dirtPlot.below());
            ItemStack seeds = new ItemStack(Items.WHEAT_SEEDS);
            player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
            BlockPos farmland = helper.absolutePos(farmPlot.below());
            seeds.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, seeds,
                    new BlockHitResult(Vec3.atCenterOf(farmland).add(0, 0.5, 0), Direction.UP, farmland, false)));
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(head.tillableCount() == 0 && head.plantableCount() == 1,
                        "the hoed plot is to plant now, got " + head.tillableCount() + " to till and " + head.plantableCount() + " to plant");
                helper.assertTrue(head.plantedCount() == 1, "the seeded plot counts as planted, got " + head.plantedCount());
                helper.succeed();
            });
        });
    }

    /**
     * The rails see only soil joined to them: dirt behind a gap stays out of
     * the field until the gap is filled, grass with short grass on it counts.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void plotsBehindAGapAreOutOfSight(GameTestHelper helper) {
        BlockPos near = new BlockPos(4, 1, RAIL_Z + 1);
        BlockPos gap = new BlockPos(4, 1, RAIL_Z + 2);
        BlockPos far = new BlockPos(4, 1, RAIL_Z + 3);
        BlockPos overgrown = new BlockPos(5, 1, RAIL_Z - 1);
        helper.setBlock(near.below(), Blocks.DIRT.defaultBlockState());
        helper.setBlock(far.below(), Blocks.DIRT.defaultBlockState());
        helper.setBlock(overgrown.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        helper.setBlock(overgrown, Blocks.SHORT_GRASS.defaultBlockState());
        FieldHeadBlockEntity head = field(helper, 3, 5);
        widen(head, 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.tillableCount() == 2, "near and overgrown plots to till, the far one out of sight, got " + head.tillableCount());
            helper.assertTrue(!head.plots().contains(helper.absolutePos(far)), "the plot behind the gap is not part of the field");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack dirt = new ItemStack(Items.DIRT);
            player.setItemInHand(InteractionHand.MAIN_HAND, dirt);
            BlockPos floor = helper.absolutePos(gap.below(2));
            helper.setBlock(gap.below(2), Blocks.STONE.defaultBlockState());
            dirt.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, dirt,
                    new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
            helper.assertBlockPresent(Blocks.DIRT, gap.below());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(head.tillableCount() == 4, "filling the gap brings the far plot into sight, got " + head.tillableCount());
                helper.succeed();
            });
        });
    }

    // Field radius

    /** A head without upgrades reaches {@code radius} blocks: dirt two out is a plot, three out is not. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void radiusTwoByDefault(GameTestHelper helper) {
        dirtLine(helper);
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.radius() == StorageStatsManager.INSTANCE.fieldRadius(), "the radius is the base, got " + head.radius());
            helper.assertTrue(head.plots().contains(helper.absolutePos(new BlockPos(4, 1, RAIL_Z + 2))), "two out is a plot");
            helper.assertTrue(!head.plots().contains(helper.absolutePos(new BlockPos(4, 1, RAIL_Z + 3))), "three out is not");
            helper.succeed();
        });
    }

    /** Two Range Upgrades widen the field to four blocks at once, plots and water alike. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rangeUpgradesWiden(GameTestHelper helper) {
        dirtLine(helper);
        helper.setBlock(new BlockPos(5, 0, RAIL_Z - 4), Blocks.FARMLAND.defaultBlockState());
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(2, () -> {
            fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(head.hydrated() && !wet(helper, 5, 0, RAIL_Z - 4), "four out stays dry at radius two");
                widen(head, 2);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(head.radius() == 4, "two upgrades reach four, got " + head.radius());
                    helper.assertTrue(head.plots().contains(helper.absolutePos(new BlockPos(4, 1, RAIL_Z + 4))), "four out is a plot now");
                    helper.assertTrue(wet(helper, 5, 0, RAIL_Z - 4), "four out is kept wet now");
                    head.rangeUpgrades().extractItem(0, 1, false);
                    helper.runAfterDelay(2, () -> {
                        helper.assertTrue(head.radius() == 3, "one upgrade reaches three, got " + head.radius());
                        helper.assertTrue(!wet(helper, 5, 0, RAIL_Z - 4), "four out dries again");
                        helper.assertTrue(!head.plots().contains(helper.absolutePos(new BlockPos(4, 1, RAIL_Z + 4))), "four out leaves the field");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** The slot takes Range Upgrades only, and no more than the radius can still grow. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rangeUpgradeLimit(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        int room = FieldRadius.maxUpgrades(StorageStatsManager.INSTANCE.fieldRadius(), StorageStatsManager.INSTANCE.fieldMaxRadius());
        ItemStack left = head.rangeUpgrades().insertItem(0, new ItemStack(ServusStorageItems.RANGE_UPGRADE.get(), room + 1), false);
        helper.assertTrue(left.getCount() == 1, "one upgrade past the maximum is refused, left " + left.getCount());
        ItemStack seeds = head.rangeUpgrades().insertItem(0, new ItemStack(Items.WHEAT_SEEDS), true);
        helper.assertTrue(seeds.getCount() == 1, "the slot refuses other items");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(head.radius() == StorageStatsManager.INSTANCE.fieldMaxRadius(), "the radius stops at the maximum");
            helper.succeed();
        });
    }

    /** Breaking the head drops its Range Upgrades. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rangeUpgradesDropWithHead(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        widen(head, 2);
        helper.runAfterDelay(2, () -> {
            helper.destroyBlock(HEAD);
            helper.runAfterDelay(1, () -> {
                helper.assertItemEntityCountIs(ServusStorageItems.RANGE_UPGRADE.get(), HEAD, 2.0, 2);
                helper.succeed();
            });
        });
    }
}
