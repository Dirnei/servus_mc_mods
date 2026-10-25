package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldRadius;
import io.github.dirnei.mc.servus.shuttles.field.FieldStatus;
import io.github.dirnei.mc.servus.shuttles.field.FieldWater;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.fluids.FluidStack;

/** Farm rails, fields and the Field Head: water, hydration and ripeness. */
public final class FieldGameTests {

    /** The usual field: three farm rails along x at z 5, the head west of them. */
    private static final int RAIL_Z = 5;
    private static final BlockPos HEAD = new BlockPos(2, 1, RAIL_Z);

    private FieldGameTests() {
    }

    /** Farm rails from x {@code from} to {@code to} at the rail row and the head west of them. */
    private static FieldHeadBlockEntity field(GameTestHelper helper, int from, int to) {
        for (int x = from; x <= to; x++) {
            FieldFixtures.farmRail(helper, new BlockPos(x, 1, RAIL_Z));
        }
        return FieldFixtures.head(helper, HEAD);
    }

    /** Puts that many Range Upgrades into the head. */
    private static void widen(FieldHeadBlockEntity head, int upgrades) {
        head.rangeUpgrades().setStackInSlot(0, new ItemStack(ServusShuttlesItems.RANGE_UPGRADE.get(), upgrades));
    }

    /** Dirt under the plots from one to four blocks north of rail 4, so the rails see that whole line. */
    private static void dirtLine(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(4, 0, RAIL_Z + z), Blocks.DIRT.defaultBlockState());
        }
    }

    private static boolean wet(GameTestHelper helper, int x, int y, int z) {
        return FieldFixtures.wet(helper, new BlockPos(x, y, z));
    }

    private static Optional<WarehouseNetwork> network(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, StorageNodeBlockEntity.class).network();
    }

    /** Controller, a Warehouse Rail, farm rails and a rack: one network, the two rail kinds link, no water inside. */
    @ServusGameTest(timeoutTicks = 100)
    public static void farmRailIsARail(GameTestHelper helper) {
        BlockPos controller = new BlockPos(2, 1, 3);
        BlockPos warehouseRail = new BlockPos(3, 1, 3);
        BlockPos rack = new BlockPos(7, 1, 3);
        WarehouseFixtures.controller(helper, controller);
        WarehouseFixtures.railOnStone(helper, warehouseRail);
        for (int x = 4; x <= 6; x++) {
            FieldFixtures.farmRail(helper, new BlockPos(x, 1, 3));
        }
        WarehouseFixtures.rack(helper, rack, Direction.WEST);
        helper.runAfterDelay(10, () -> {
            BlockState first = helper.getBlockState(new BlockPos(4, 1, 3));
            helper.assertTrue(first.getValue(RailBlock.WEST), "the farm rail should connect to the Warehouse Rail");
            helper.assertTrue(helper.getBlockState(warehouseRail).getValue(RailBlock.EAST), "the Warehouse Rail should connect to the farm rail");
            helper.assertTrue(!first.hasProperty(BlockStateProperties.WATERLOGGED), "a farm rail must not be waterloggable");
            helper.assertTrue(helper.getBlockState(new BlockPos(6, 1, 3)).getValue(RailBlock.DOCK_EAST), "the farm rail should dock onto the rack");
            Optional<WarehouseNetwork> rackNetwork = network(helper, rack);
            helper.assertTrue(rackNetwork.isPresent() && rackNetwork.equals(network(helper, controller)),
                    "the rack should be in the controller's warehouse through the farm rails");
            helper.assertTrue(network(helper, new BlockPos(5, 1, 3)).equals(rackNetwork), "the farm rail is a node of the warehouse");
            helper.succeed();
        });
    }

    /** A shuttle drives from its garage over farm rails to a rack and back to the terminal with the stack. */
    @ServusGameTest(timeoutTicks = 600)
    public static void shuttleDrivesOverFarmRails(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(1, 1, 3);
        BlockPos terminalPos = new BlockPos(1, 1, 4);
        BlockPos garagePos = new BlockPos(2, 1, 5);
        BlockPos rackPos = new BlockPos(9, 1, 5);
        WarehouseFixtures.controller(helper, controllerPos);
        TerminalWiring.place(helper, terminalPos);
        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            Block rail = x >= 4 && x <= 7 ? ServusShuttlesBlocks.FARM_RAIL.get() : ServusShuttlesBlocks.RAIL.get();
            helper.setBlock(new BlockPos(x, 1, 4), rail.defaultBlockState());
        }
        helper.setBlock(new BlockPos(5, 1, 3), ServusShuttlesBlocks.FIELD_HEAD.get().defaultBlockState());
        WarehouseFixtures.garage(helper, garagePos, Direction.SOUTH, 0);
        WarehouseFixtures.rack(helper, rackPos, Direction.NORTH);
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(garagePos, ShuttleGarageBlockEntity.class);
        garage.shuttles().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        HeavyRackBlockEntity rack = helper.getBlockEntity(rackPos, HeavyRackBlockEntity.class);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 36), false);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, terminalPos);
            new StorageTerminalMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), terminal)
                    .request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 36);
            WarehouseNetwork network = helper.getBlockEntity(controllerPos, WarehouseControllerBlockEntity.class).network().orElseThrow();
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(network.garageOut(helper.absolutePos(garagePos)) == 1, "the shuttle should be out over the farm rails");
                Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 36, 500,
                        () -> "the 36 cobblestone should arrive, got " + terminal.outputCount(Items.COBBLESTONE), helper::succeed);
            });
        });
    }

    /** A run of five: removing the middle rail splits it, a Warehouse Rail there keeps it split, a farm rail joins it again. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fieldSplitsAndJoins(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 7);
        BlockPos middle = new BlockPos(5, 1, RAIL_Z);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.status() == FieldStatus.SERVING && head.rails().size() == 5, "the head should serve five rails, got " + head.rails().size());
            helper.setBlock(middle, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(head.rails().size() == 2, "the head's field should be the two rails on its side, got " + head.rails().size());
                WarehouseFixtures.railOnStone(helper, middle);
                helper.runAfterDelay(3, () -> {
                    helper.assertTrue(head.rails().size() == 2, "a Warehouse Rail must not join the field, got " + head.rails().size());
                    helper.setBlock(middle, Blocks.AIR.defaultBlockState());
                    FieldFixtures.farmRail(helper, middle);
                    helper.runAfterDelay(3, () -> {
                        helper.assertTrue(head.rails().size() == 5, "the farm rail should join the runs again, got " + head.rails().size());
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** A head with no farm rail has no field; a second head on the same field stands aside until the first is broken. */
    @ServusGameTest(timeoutTicks = 100)
    public static void oneHeadPerField(GameTestHelper helper) {
        BlockPos lonely = new BlockPos(9, 1, 9);
        helper.setBlock(lonely, ServusShuttlesBlocks.FIELD_HEAD.get().defaultBlockState());
        FieldHeadBlockEntity first = field(helper, 3, 5);
        BlockPos secondPos = new BlockPos(4, 1, RAIL_Z - 1);
        FieldHeadBlockEntity second = FieldFixtures.head(helper, secondPos);
        helper.runAfterDelay(4, () -> {
            FieldHeadBlockEntity alone = helper.getBlockEntity(lonely, FieldHeadBlockEntity.class);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void headTakesWaterOnly(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        FieldFixtures.head(helper, pos);
        FluidView handler = FieldFixtures.fluid(helper, pos);
        helper.assertTrue(handler != null, "the head should expose a fluid handler");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), false) == 1000, "the head should take 1000 mB of water");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 1000), false) == 0, "the head must refuse lava");
        helper.assertTrue(handler.drain(500, false).isEmpty(), "the head must not give water back");
        FieldHeadBlockEntity head = helper.getBlockEntity(pos, FieldHeadBlockEntity.class);
        helper.assertTrue(head.water() == 1000, "the buffer should hold 1000 mB, got " + head.water());
        helper.succeed();
    }

    /** Uses the stack on the head at {@code pos} and returns what the player holds afterwards. */
    private static ItemStack useOnHead(GameTestHelper helper, BlockPos pos, Player player, ItemStack stack) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.getLevel().getBlockState(absolute).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    /** A water bucket empties into the head's buffer. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fieldHeadTakesWaterBucket(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        FieldFixtures.head(helper, pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = useOnHead(helper, pos, player, new ItemStack(Items.WATER_BUCKET));
        FieldHeadBlockEntity head = helper.getBlockEntity(pos, FieldHeadBlockEntity.class);
        helper.assertTrue(head.water() == 1000, "the bucket should pour 1000 mB in, got " + head.water());
        helper.assertTrue(held.is(Items.BUCKET), "the player should hold an empty bucket");
        helper.succeed();
    }

    /** A lava bucket stays full and the head stays empty. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fieldHeadRefusesLavaBucket(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        FieldFixtures.head(helper, pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = useOnHead(helper, pos, player, new ItemStack(Items.LAVA_BUCKET));
        FieldHeadBlockEntity head = helper.getBlockEntity(pos, FieldHeadBlockEntity.class);
        helper.assertTrue(head.water() == 0, "the head must refuse lava, holds " + head.water());
        helper.assertTrue(held.is(Items.LAVA_BUCKET), "the player should keep the lava bucket");
        helper.succeed();
    }

    /** An empty bucket takes no water from the head. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fieldHeadGivesNoWaterToEmptyBucket(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        FieldFixtures.head(helper, pos);
        FieldFixtures.fluid(helper, pos).fill(new FluidStack(Fluids.WATER, 2000), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = useOnHead(helper, pos, player, new ItemStack(Items.BUCKET));
        FieldHeadBlockEntity head = helper.getBlockEntity(pos, FieldHeadBlockEntity.class);
        helper.assertTrue(head.water() == 2000, "the head must keep its water, holds " + head.water());
        helper.assertTrue(held.is(Items.BUCKET), "the bucket should stay empty");
        helper.succeed();
    }

    /** A full head leaves the water bucket full. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fullFieldHeadKeepsBucket(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        FieldFixtures.head(helper, pos);
        int capacity = ShuttlesStatsManager.INSTANCE.fieldCapacity();
        FieldFixtures.fluid(helper, pos).fill(new FluidStack(Fluids.WATER, capacity), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = useOnHead(helper, pos, player, new ItemStack(Items.WATER_BUCKET));
        FieldHeadBlockEntity head = helper.getBlockEntity(pos, FieldHeadBlockEntity.class);
        helper.assertTrue(head.water() == capacity, "the full head should stay full, holds " + head.water());
        helper.assertTrue(held.is(Items.WATER_BUCKET), "the player should keep the water bucket");
        helper.succeed();
    }

    /** A water cauldron pulled in by a Utility Duct fluid line fills the head. */
    @ServusGameTest(timeoutTicks = 100)
    public static void headFillsThroughThePipe(GameTestHelper helper) {
        BlockPos ductA = new BlockPos(2, 1, 6);
        BlockPos ductB = new BlockPos(3, 1, 6);
        BlockPos ductC = new BlockPos(4, 1, 6);
        helper.setBlock(ductA.north(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(ductC.north(), ServusShuttlesBlocks.FIELD_HEAD.get().defaultBlockState());
        GameTestSteps.duct(helper, ductA, Line.FLUID);
        GameTestSteps.duct(helper, ductB, Line.FLUID);
        GameTestSteps.duct(helper, ductC, Line.FLUID);
        UtilityDuctBlockEntity a = helper.getBlockEntity(ductA, UtilityDuctBlockEntity.class);
        a.setMode(Line.FLUID, Direction.NORTH, SideMode.INPUT);
        FieldHeadBlockEntity head = helper.getBlockEntity(ductC.north(), FieldHeadBlockEntity.class);
        helper.succeedWhen(() -> helper.assertTrue(head.water() == 1000, "the head should hold the cauldron's water, got " + head.water()));
    }

    /** With water, farmland one below the rails is wet four blocks out and not five. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hydrationReachesFourBlocks(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        widen(head, 2);
        helper.setBlock(new BlockPos(4, 0, RAIL_Z + 4), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!head.hydrated() && !wet(helper, 4, 0, RAIL_Z + 4), "no water, no hydration");
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(head.hydrated(), "the head should hydrate with water in the buffer");
                helper.assertTrue(wet(helper, 4, 0, RAIL_Z + 4), "farmland four blocks beside a rail should be wet");
                helper.assertTrue(!wet(helper, 4, 0, RAIL_Z + 5), "farmland five blocks beside a rail must stay dry");
                helper.assertTrue(wet(helper, 9, 0, RAIL_Z), "farmland four blocks past the end rail should be wet");
                helper.assertTrue(!wet(helper, 10, 0, RAIL_Z), "farmland five blocks past the end rail must stay dry");
                helper.assertTrue(!wet(helper, 4, 1, RAIL_Z + 2), "only the layer below the rails is kept wet");
                helper.assertTrue(head.water() == 1000 - FieldWater.use(3, ShuttlesStatsManager.INSTANCE.fieldWaterPerRail()),
                        "the head should use one interval of water, has " + head.water());
                BlockState farmland = helper.getBlockState(new BlockPos(4, 0, RAIL_Z + 4));
                helper.assertTrue(farmland.is(Blocks.FARMLAND), "the farmland stays");
                helper.succeed();
            });
        });
    }

    /** One interval of water hydrates for one interval, then the field dries. */
    @ServusGameTest(timeoutTicks = 400)
    public static void hydrationEndsWhenDry(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        int need = FieldWater.use(3, ShuttlesStatsManager.INSTANCE.fieldWaterPerRail());
        int interval = ShuttlesStatsManager.INSTANCE.fieldWaterInterval();
        helper.runAfterDelay(2, () -> {
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, need), false);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void hydrationEndsWithRail(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(2, () -> {
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
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

    /** A crop growing to its last stage counts as ripe at once. */
    @ServusGameTest(timeoutTicks = 100)
    public static void ripeAfterGrowth(GameTestHelper helper) {
        BlockPos plot = new BlockPos(4, 1, RAIL_Z + 1);
        FieldFixtures.crop(helper, plot, Blocks.WHEAT, 3);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void ripeAfterBonemeal(GameTestHelper helper) {
        BlockPos plot = new BlockPos(4, 1, RAIL_Z - 1);
        FieldFixtures.crop(helper, plot, Blocks.CARROTS, 0);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void breakForgetsRipe(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z + 1);
        FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.ripeCount() == 1, "the ripe wheat should be counted");
            ServerPlayer player = TestPlayers.quiet(helper);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void harvestThresholdPerHead(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(3, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1), new BlockPos(5, 1, RAIL_Z + 1), new BlockPos(6, 1, RAIL_Z + 1)};
        FieldFixtures.crop(helper, plots[0], Blocks.WHEAT, 7);
        for (int i = 1; i < plots.length; i++) {
            FieldFixtures.crop(helper, plots[i], Blocks.WHEAT, 2);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void harvestDueNeedsCropAndShare(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(3, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1), new BlockPos(5, 1, RAIL_Z + 1), new BlockPos(6, 1, RAIL_Z + 1)};
        FieldFixtures.crop(helper, plots[0], Blocks.WHEAT, 7);
        for (int i = 1; i < plots.length; i++) {
            FieldFixtures.crop(helper, plots[i], Blocks.WHEAT, 2);
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

    /** A mock player standing at the head with its menu open, as the picker's payload finds it. */
    private static FieldHeadMenu openMenu(GameTestHelper helper, Player player, FieldHeadBlockEntity head) {
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(HEAD).east()));
        FieldHeadMenu menu = new FieldHeadMenu(1, player.getInventory(), head);
        player.containerMenu = menu;
        return menu;
    }

    private static Optional<Identifier> id(Item item) {
        return Optional.of(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * The crop picker sets a crop by item id without the item in the inventory
     * and turns harvest only off, its none entry sets harvest only, and ids of
     * items that plant no crop or of no item at all are refused.
     */
    @ServusGameTest(timeoutTicks = 100)
    public static void menuChoosesCropById(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        openMenu(helper, player, head);
        helper.assertTrue(player.getInventory().countItem(Items.WHEAT_SEEDS) == 0, "the player holds no seeds");
        head.setHarvestOnly(true);
        helper.assertTrue(FieldHeadMenu.choose(player, id(Items.WHEAT_SEEDS), false), "wheat seeds should be taken");
        helper.assertTrue(head.crop().is(Items.WHEAT_SEEDS) && !head.harvestOnly(), "wheat is the crop and harvest only is off");
        helper.assertTrue(!FieldHeadMenu.choose(player, id(Items.COBBLESTONE), false), "cobblestone should be refused");
        helper.assertTrue(!FieldHeadMenu.choose(player, Optional.of(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "no_such_item")), false),
                "an unknown id should be refused");
        helper.assertTrue(head.crop().is(Items.WHEAT_SEEDS) && !head.harvestOnly(), "refused ids change nothing");
        helper.assertTrue(FieldHeadMenu.choose(player, Optional.empty(), false), "the none entry should be taken");
        helper.assertTrue(head.harvestOnly() && head.crop().isEmpty(), "harvest only clears the crop");
        helper.assertTrue(FieldHeadMenu.choose(player, id(Items.CARROT), false), "a carrot plants a crop");
        helper.assertTrue(head.crop().is(Items.CARROT) && !head.harvestOnly(), "carrots turn harvest only off");
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!FieldHeadMenu.choose(player, id(Items.WHEAT_SEEDS), false) && head.crop().is(Items.CARROT),
                "without the head's menu open nothing changes");
        helper.succeed();
    }

    /** A head whose field is on no warehouse sends no counts to the crop picker. */
    @ServusGameTest(timeoutTicks = 100)
    public static void menuCropStockWithoutWarehouse(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.status() == FieldStatus.SERVING, "the head should serve its rails");
            FieldHeadMenu menu = new FieldHeadMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), head);
            helper.assertTrue(!menu.stockPayload().networked() && menu.stockPayload().counts().isEmpty(), "no warehouse, no counts");
            helper.succeed();
        });
    }

    /**
     * The Range Upgrade slot sits on its tab and shift click routes a Range
     * Upgrade into it and back whatever tab is shown; honeycomb stays with the
     * player, the buffer takes nothing by hand.
     */
    @ServusGameTest(timeoutTicks = 100)
    public static void menuTabsRouteShiftClick(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FieldHeadMenu menu = new FieldHeadMenu(1, player.getInventory(), head);
        helper.assertTrue(menu.page() == FieldHeadMenu.Page.FIELD, "the GUI opens on the Field tab");
        helper.assertTrue(FieldHeadMenu.pageOf(FieldHeadMenu.RANGE_SLOT) == FieldHeadMenu.Page.UPGRADES
                && FieldHeadMenu.pageOf(FieldHeadMenu.INVENTORY_START) == null, "the head slot belongs to its tab");
        helper.assertTrue(menu.slots.get(FieldHeadMenu.RANGE_SLOT).isActive(), "the head slot stays active on the server");
        player.getInventory().setItem(9, new ItemStack(Items.HONEYCOMB, 3));
        player.getInventory().setItem(10, new ItemStack(ServusShuttlesItems.RANGE_UPGRADE.get()));
        menu.quickMoveStack(player, FieldHeadMenu.INVENTORY_START);
        menu.quickMoveStack(player, FieldHeadMenu.INVENTORY_START + 1);
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 3
                && head.buffer().count(SupplyBuffer.Part.ANTI_CORROSION) == 0, "honeycomb stays with the player");
        helper.assertTrue(head.rangeUpgrades().getStackInSlot(0).is(ServusShuttlesItems.RANGE_UPGRADE.get())
                && player.getInventory().getItem(10).isEmpty(), "the Range Upgrade goes into its slot from the Field tab");
        menu.setPage(FieldHeadMenu.Page.UPGRADES);
        menu.quickMoveStack(player, FieldHeadMenu.RANGE_SLOT);
        helper.assertTrue(head.rangeUpgrades().getStackInSlot(0).isEmpty()
                && player.getInventory().countItem(ServusShuttlesItems.RANGE_UPGRADE.get()) == 1, "the Range Upgrade is back in the inventory");
        helper.succeed();
    }

    /** Dirt under an empty plot is to till, farmland to plant; hoeing and planting by hand move the plots along. */
    @ServusGameTest(timeoutTicks = 100)
    public static void openPlotsCounted(GameTestHelper helper) {
        BlockPos dirtPlot = new BlockPos(4, 1, RAIL_Z + 1);
        BlockPos farmPlot = new BlockPos(5, 1, RAIL_Z - 1);
        helper.setBlock(dirtPlot.below(), Blocks.DIRT.defaultBlockState());
        helper.setBlock(farmPlot.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
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
            helper.setBlock(dirtPlot.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
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
    @ServusGameTest(timeoutTicks = 100)
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

    /** A head without upgrades reaches {@code radius} blocks: dirt two out is a plot, three out is not. */
    @ServusGameTest(timeoutTicks = 100)
    public static void radiusTwoByDefault(GameTestHelper helper) {
        dirtLine(helper);
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.radius() == ShuttlesStatsManager.INSTANCE.fieldRadius(), "the radius is the base, got " + head.radius());
            helper.assertTrue(head.plots().contains(helper.absolutePos(new BlockPos(4, 1, RAIL_Z + 2))), "two out is a plot");
            helper.assertTrue(!head.plots().contains(helper.absolutePos(new BlockPos(4, 1, RAIL_Z + 3))), "three out is not");
            helper.succeed();
        });
    }

    /** Two Range Upgrades widen the field to four blocks at once, plots and water alike. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rangeUpgradesWiden(GameTestHelper helper) {
        dirtLine(helper);
        helper.setBlock(new BlockPos(5, 0, RAIL_Z - 4), Blocks.FARMLAND.defaultBlockState());
        FieldHeadBlockEntity head = field(helper, 3, 5);
        helper.runAfterDelay(2, () -> {
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void rangeUpgradeLimit(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        int room = FieldRadius.maxUpgrades(ShuttlesStatsManager.INSTANCE.fieldRadius(), ShuttlesStatsManager.INSTANCE.fieldMaxRadius());
        ItemStack left = head.rangeUpgrades().insertItem(0, new ItemStack(ServusShuttlesItems.RANGE_UPGRADE.get(), room + 1), false);
        helper.assertTrue(left.getCount() == 1, "one upgrade past the maximum is refused, left " + left.getCount());
        ItemStack seeds = head.rangeUpgrades().insertItem(0, new ItemStack(Items.WHEAT_SEEDS), true);
        helper.assertTrue(seeds.getCount() == 1, "the slot refuses other items");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(head.radius() == ShuttlesStatsManager.INSTANCE.fieldMaxRadius(), "the radius stops at the maximum");
            helper.succeed();
        });
    }

    /** Breaking the head drops its Range Upgrades. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rangeUpgradesDropWithHead(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper, 3, 5);
        widen(head, 2);
        helper.runAfterDelay(2, () -> {
            helper.destroyBlock(HEAD);
            helper.runAfterDelay(1, () -> {
                helper.assertItemEntityCountIs(ServusShuttlesItems.RANGE_UPGRADE.get(), HEAD, 2.0, 2);
                helper.succeed();
            });
        });
    }
}
