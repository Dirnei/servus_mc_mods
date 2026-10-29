package io.github.dirnei.mc.servus.delicacies.gametest;

import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumCycle;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumForming;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumLidBlock;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumMasterState;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumMenu;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumPartBlockEntity;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumSettingsPayload;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumStats;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumStatsManager;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumTankClicks;
import io.github.dirnei.mc.servus.delicacies.aquarium.FishSpecies;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenTags;
import io.github.dirnei.mc.servus.delicacies.registry.DelicaciesFluids;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.filter.Filters;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankStatsManager;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationComponents;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.EnergyView;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/** The aquarium: forming, the lid layer, buckets, the cycle, capabilities, power and the GUI settings. */
public final class AquariumGameTests {

    /** The tank's lowest north west corner; Floor blocks lie one below, the master under this corner. */
    private static final BlockPos TANK = new BlockPos(2, 2, 2);
    private static final BlockPos MASTER = TANK.below();

    private AquariumGameTests() {
    }

    private static int cap() {
        return TankStatsManager.INSTANCE.capacityPerBlock();
    }

    /** A tank of {@code width} by {@code depth} by {@code height} at {@link #TANK} with floors below and, when asked, lids above. */
    private static void build(GameTestHelper helper, int width, int depth, int height, boolean lids) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                helper.setBlock(MASTER.offset(x, 0, z), ServusDelicaciesBlocks.AQUARIUM_FLOOR.get());
                for (int y = 0; y < height; y++) {
                    helper.setBlock(TANK.offset(x, y, z), ServusLogisticsBlocks.FLUID_TANK.get());
                }
                if (lids) {
                    helper.setBlock(TANK.offset(x, height, z), ServusDelicaciesBlocks.AQUARIUM_LID.get());
                }
            }
        }
    }

    private static FluidView tankHandler(GameTestHelper helper) {
        FluidView view = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(TANK), Direction.UP));
        helper.assertTrue(view != null, "the tank has a fluid handler");
        return view;
    }

    private static void fillWater(GameTestHelper helper, int amount) {
        int filled = tankHandler(helper).fill(new FluidStack(Fluids.WATER, amount), false);
        helper.assertTrue(filled == amount, "the tank takes " + amount + " mB of water, took " + filled);
    }

    private static AquariumForming.Result form(GameTestHelper helper, BlockPos clicked) {
        return AquariumForming.tryForm(helper.getLevel(), helper.absolutePos(clicked));
    }

    private static AquariumPartBlockEntity master(GameTestHelper helper) {
        return helper.getBlockEntity(MASTER, AquariumPartBlockEntity.class);
    }

    /** A formed 3 by 2 by 2 aquarium with {@code water} mB; runs {@code then} once it formed. */
    private static void formed(GameTestHelper helper, int water, Consumer<AquariumPartBlockEntity> then) {
        build(helper, 3, 2, 2, true);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    if (water > 0) {
                        fillWater(helper, water);
                    }
                    AquariumForming.Result result = form(helper, TANK.above(2));
                    helper.assertTrue(result.formed(), "the aquarium forms, got " + result.outcome());
                    then.accept(master(helper));
                })
                .thenSucceed();
    }

    private static InteractionResult use(GameTestHelper helper, Player player, BlockPos pos, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return helper.getLevel().getBlockState(absolute).useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB area = new AABB(helper.absolutePos(MASTER)).inflate(3);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void statsLoadAndSeedsAreFeed(GameTestHelper helper) {
        AquariumStats stats = AquariumStatsManager.INSTANCE.stats();
        helper.assertTrue(stats.cycleTicks() == 1200 && stats.waterPerFish() == 4000 && stats.filterRate() == 40
                && stats.fePerTankBlock() == 2 && stats.fePerLid() == 1 && stats.keepDefault() == 8 && stats.drawnCap() == 16,
                "the shipped aquarium stats load, got " + stats);
        helper.assertTrue(new ItemStack(Items.WHEAT_SEEDS).is(KitchenTags.FISH_FEED), "wheat seeds are fish feed");
        helper.assertFalse(new ItemStack(Items.BREAD).is(KitchenTags.FISH_FEED), "bread is no fish feed");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void formsThreeByTwoByTwo(GameTestHelper helper) {
        formed(helper, 0, master -> {
            helper.assertTrue(master.isMaster() && master.box() != null && master.box().width() == 3
                    && master.box().depth() == 2 && master.box().height() == 2, "the master holds the 3 by 2 by 2 box");
            AquariumPartBlockEntity lid = helper.getBlockEntity(TANK.offset(2, 2, 1), AquariumPartBlockEntity.class);
            helper.assertTrue(lid.master() == master, "a far lid knows the master");
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void missingLidIsRefused(GameTestHelper helper) {
        build(helper, 3, 2, 2, true);
        helper.setBlock(TANK.offset(1, 2, 1), Blocks.AIR);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    AquariumForming.Result result = form(helper, MASTER);
                    helper.assertTrue(result.outcome() == AquariumForming.Outcome.LID_MISSING, "a missing lid refuses, got " + result.outcome());
                    helper.assertFalse(master(helper).formed(), "nothing formed");
                })
                .thenSucceed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void lavaIsRefused(GameTestHelper helper) {
        build(helper, 3, 2, 2, true);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    tankHandler(helper).fill(new FluidStack(Fluids.LAVA, 1000), false);
                    AquariumForming.Result result = form(helper, MASTER);
                    helper.assertTrue(result.outcome() == AquariumForming.Outcome.NOT_WATER, "lava refuses, got " + result.outcome());
                })
                .thenSucceed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void breakingAFloorDropsFishAndSlots(GameTestHelper helper) {
        formed(helper, 100_000, master -> {
            master.state().setStock(FishSpecies.COD, 12.4);
            master.state().slots().setStackInSlot(AquariumMasterState.FEED_SLOT, new ItemStack(Items.WHEAT_SEEDS, 20));
            helper.setBlock(MASTER.east(), Blocks.AIR);
            helper.assertFalse(master.formed(), "the aquarium unforms");
            helper.assertTrue(dropped(helper, Items.COD) == 12, "12 raw cod drop, got " + dropped(helper, Items.COD));
            helper.assertTrue(dropped(helper, Items.WHEAT_SEEDS) == 20, "the 20 seeds drop, got " + dropped(helper, Items.WHEAT_SEEDS));
            AquariumPartBlockEntity lid = helper.getBlockEntity(TANK.offset(0, 2, 0), AquariumPartBlockEntity.class);
            helper.assertFalse(lid.formed(), "the lid is loose again");
        });
    }

    @ServusGameTest(timeoutTicks = 80)
    public static void growingTheTankUnforms(GameTestHelper helper) {
        build(helper, 3, 2, 2, true);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(form(helper, MASTER).formed(), "the aquarium forms");
                    for (int x = 0; x < 3; x++) {
                        for (int y = 0; y < 2; y++) {
                            helper.setBlock(TANK.offset(x, y, 2), ServusLogisticsBlocks.FLUID_TANK.get());
                        }
                    }
                })
                .thenIdle(AquariumPartBlockEntity.CHECK_INTERVAL + 5)
                .thenExecute(() -> helper.assertFalse(master(helper).formed(), "a tank that grew to 3 by 3 unforms the aquarium"))
                .thenSucceed();
    }

    private static InteractionResult placeLid(GameTestHelper helper, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos top = helper.absolutePos(TANK.offset(1, 1, 0));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(top).relative(Direction.UP, 0.5), Direction.UP, top, false);
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        return ServusDelicaciesItems.AQUARIUM_LID.get().place(context);
    }

    private static int lids(GameTestHelper helper) {
        int count = 0;
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 2; z++) {
                if (helper.getBlockState(TANK.offset(x, 2, z)).getBlock() instanceof AquariumLidBlock) {
                    count++;
                }
            }
        }
        return count;
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void lidStackCoversTheTank(GameTestHelper helper) {
        build(helper, 3, 2, 2, false);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    ItemStack stack = new ItemStack(ServusDelicaciesItems.AQUARIUM_LID.get(), 16);
                    placeLid(helper, player, stack);
                    helper.assertTrue(lids(helper) == 6, "six lids lie on the tank, got " + lids(helper));
                    helper.assertTrue(stack.getCount() == 10, "10 lids are left, got " + stack.getCount());
                })
                .thenSucceed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void fewLidsPlaceOne(GameTestHelper helper) {
        build(helper, 3, 2, 2, false);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    ItemStack stack = new ItemStack(ServusDelicaciesItems.AQUARIUM_LID.get(), 4);
                    placeLid(helper, player, stack);
                    helper.assertTrue(lids(helper) == 1, "one lid is placed, got " + lids(helper));
                    helper.assertTrue(stack.getCount() == 3, "3 lids are left, got " + stack.getCount());
                })
                .thenSucceed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void sneakingPlacesOneLid(GameTestHelper helper) {
        build(helper, 3, 2, 2, false);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setShiftKeyDown(true);
                    placeLid(helper, player, new ItemStack(ServusDelicaciesItems.AQUARIUM_LID.get(), 16));
                    helper.assertTrue(lids(helper) == 1, "sneaking places one lid, got " + lids(helper));
                })
                .thenSucceed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void bucketsPourAndCatch(GameTestHelper helper) {
        formed(helper, 100_000, master -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            use(helper, player, TANK.above(2), new ItemStack(Items.COD_BUCKET));
            helper.assertTrue(master.state().wholeFish() == 1 && master.state().species() == FishSpecies.COD, "one cod is poured in");
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the player holds an empty bucket");
            master.state().setStock(FishSpecies.COD, 5);
            use(helper, player, MASTER, new ItemStack(Items.WATER_BUCKET));
            helper.assertTrue(master.state().wholeFish() == 4, "a water bucket catches one of 5, left " + master.state().wholeFish());
            helper.assertTrue(player.getMainHandItem().is(Items.COD_BUCKET), "the player holds a cod bucket");
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void otherSpeciesWaitsForAnEmptyAquarium(GameTestHelper helper) {
        formed(helper, 100_000, master -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            master.state().setStock(FishSpecies.COD, 1);
            use(helper, player, MASTER, new ItemStack(Items.SALMON_BUCKET));
            helper.assertTrue(player.getMainHandItem().is(Items.SALMON_BUCKET), "salmon is refused while cod swim");
            helper.assertTrue(master.state().species() == FishSpecies.COD && master.state().wholeFish() == 1, "the cod stays");
            use(helper, player, MASTER, new ItemStack(Items.WATER_BUCKET));
            use(helper, player, MASTER, new ItemStack(Items.SALMON_BUCKET));
            helper.assertTrue(master.state().species() == FishSpecies.SALMON && master.state().wholeFish() == 1,
                    "after the last cod the aquarium keeps one salmon");
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void waterBucketOnTheGlassFillsTheTank(GameTestHelper helper) {
        formed(helper, 100_000, master -> {
            master.state().setStock(FishSpecies.COD, 5);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            use(helper, player, TANK, new ItemStack(Items.WATER_BUCKET));
            helper.assertTrue(master.water() == 101_000, "the tank takes the bucket, holds " + master.water());
            helper.assertTrue(master.state().wholeFish() == 5, "no fish is caught");
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void fishBucketOnTheGlassPoursTheFish(GameTestHelper helper) {
        formed(helper, 100_000, master -> {
            master.state().setStock(FishSpecies.COD, 5);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD_BUCKET));
            boolean poured = AquariumTankClicks.pourThroughGlass(helper.getLevel(), helper.absolutePos(TANK), player,
                    InteractionHand.MAIN_HAND, player.getMainHandItem());
            helper.assertTrue(poured && master.state().wholeFish() == 6, "the cod is poured in, holds " + master.state().wholeFish());
            helper.assertTrue(master.water() == 100_000, "the tank's water is unchanged, holds " + master.water());
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the player holds an empty bucket");
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void cycleGrowsAndCatches(GameTestHelper helper) {
        formed(helper, 192_000, master -> {
            master.state().setStock(FishSpecies.COD, 20);
            master.state().slots().setStackInSlot(AquariumMasterState.FEED_SLOT, new ItemStack(Items.WHEAT_SEEDS, 64));
            master.state().slots().setStackInSlot(AquariumMasterState.FILTER_SLOT, new ItemStack(ServusFoundationItems.CARBON_FILTER.get()));
            master.state().setCatch(false, 8);
            master.runCycle(1);
            helper.assertTrue(master.state().stock() > 20.5, "the stock grows, got " + master.state().stock());
            master.state().setStock(FishSpecies.COD, 20);
            master.state().setCatch(true, 8);
            master.runCycle(0);
            helper.assertTrue(master.state().wholeFish() == 8, "8 cod stay, got " + master.state().wholeFish());
            int caught = 0;
            for (int slot = AquariumMasterState.FIRST_CATCH_SLOT; slot < AquariumMasterState.SLOT_COUNT; slot++) {
                ItemStack stack = master.state().slots().getStackInSlot(slot);
                caught += stack.is(Items.COD) ? stack.getCount() : 0;
            }
            helper.assertTrue(caught == 12, "12 raw cod are caught, got " + caught);
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void filterWearsAndMovesOutWhenSpent(GameTestHelper helper) {
        formed(helper, 192_000, master -> {
            master.state().setStock(FishSpecies.COD, 20);
            master.state().slots().setStackInSlot(AquariumMasterState.FEED_SLOT, new ItemStack(Items.WHEAT_SEEDS, 64));
            master.state().slots().setStackInSlot(AquariumMasterState.FILTER_SLOT, new ItemStack(ServusFoundationItems.CARBON_FILTER.get()));
            master.state().setCatch(false, 8);
            AquariumCycle.Output out = master.runCycle(1);
            int left = Filters.pointsLeft(master.state().slots().getStackInSlot(AquariumMasterState.FILTER_SLOT));
            helper.assertTrue(out.filterUsed() == 12 && left == 500, "the filter cleans 12 dirt and has 500 left, got " + left);
            ItemStack worn = new ItemStack(ServusFoundationItems.CARBON_FILTER.get());
            worn.set(ServusFoundationComponents.FILTER_USED.get(), 510);
            master.state().slots().setStackInSlot(AquariumMasterState.FILTER_SLOT, worn);
            master.runCycle(1);
            helper.assertTrue(master.state().slots().getStackInSlot(AquariumMasterState.FILTER_SLOT).isEmpty(), "the filter slot is empty");
            helper.assertTrue(master.state().slots().getStackInSlot(AquariumMasterState.FIRST_CATCH_SLOT).is(ServusFoundationItems.SPENT_FILTER.get()),
                    "a Spent Filter lies in the catch");
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void starvingAndDirtyWaterKill(GameTestHelper helper) {
        formed(helper, 192_000, master -> {
            master.state().setStock(FishSpecies.COD, 40);
            master.state().setUnfedCycles(3);
            master.state().setCatch(false, 8);
            master.runCycle(1);
            helper.assertTrue(Math.abs(master.state().stock() - 38) < 1e-6, "starving fish lose 5%, got " + master.state().stock());
            master.state().setStock(FishSpecies.COD, 40);
            master.state().setUnfedCycles(0);
            master.state().slots().setStackInSlot(AquariumMasterState.FEED_SLOT, new ItemStack(Items.WHEAT_SEEDS, 64));
            master.state().setDirt(1800);
            master.runCycle(1);
            helper.assertTrue(Math.abs(master.state().stock() - 36) < 1e-6, "dirty water kills 10%, got " + master.state().stock());
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void unpoweredCycleDoesNotBreed(GameTestHelper helper) {
        formed(helper, 192_000, master -> {
            master.state().setStock(FishSpecies.COD, 20);
            master.state().slots().setStackInSlot(AquariumMasterState.FEED_SLOT, new ItemStack(Items.WHEAT_SEEDS, 64));
            master.state().setCatch(false, 8);
            master.runCycle(0);
            helper.assertTrue(master.state().stock() == 20, "no power, no breeding, got " + master.state().stock());
            helper.assertTrue(master.state().slots().getStackInSlot(AquariumMasterState.FEED_SLOT).getCount() == 62, "but the fish eat");
        });
    }

    private static @Nullable ItemView items(GameTestHelper helper, BlockPos pos) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), Direction.NORTH));
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void pipesFeedAndTakeTheCatch(GameTestHelper helper) {
        formed(helper, 100_000, master -> {
            ItemView floor = items(helper, MASTER.east());
            helper.assertTrue(floor != null, "a formed floor offers items");
            ItemStack left = floor.insertItem(new ItemStack(Items.WHEAT_SEEDS, 16), false);
            helper.assertTrue(left.isEmpty() && master.state().slots().getStackInSlot(AquariumMasterState.FEED_SLOT).getCount() == 16,
                    "seeds land in the feed slot");
            helper.assertTrue(floor.insertItem(new ItemStack(Items.BREAD, 4), false).getCount() == 4, "bread is refused");
            master.state().slots().setStackInSlot(AquariumMasterState.FIRST_CATCH_SLOT, new ItemStack(Items.COD, 12));
            ItemView lid = items(helper, TANK.offset(1, 2, 1));
            helper.assertTrue(lid != null, "a formed lid offers items");
            helper.assertTrue(lid.extractItem(AquariumMasterState.FEED_SLOT, 64, false).isEmpty(), "the feed cannot be pulled");
            ItemStack fish = lid.extractItem(AquariumMasterState.FIRST_CATCH_SLOT, 64, false);
            helper.assertTrue(fish.is(Items.COD) && fish.getCount() == 12, "the pipe takes the 12 raw cod, got " + fish);
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void drainingThroughAPartTakesDirt(GameTestHelper helper) {
        formed(helper, 100_000, master -> {
            master.state().setDirt(200);
            FluidView part = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(MASTER), Direction.DOWN));
            helper.assertTrue(part != null, "a formed floor offers fluids");
            FluidStack out = part.drain(10_000, false);
            helper.assertTrue(out.getFluid().isSame(DelicaciesFluids.MURKY_WATER.get()) && out.getAmount() == 10_000,
                    "10 buckets of Murky Water come out, got " + out.getAmount());
            helper.assertTrue(Math.abs(master.state().dirt() - 180) < 1e-6, "180 dirt stay, got " + master.state().dirt());
            helper.assertTrue(master.water() == 90_000, "90 buckets of water stay, got " + master.water());
            helper.assertTrue(part.fill(new FluidStack(Fluids.LAVA, 1000), false) == 0, "lava is refused");
            helper.assertTrue(part.fill(new FluidStack(Fluids.WATER, 1000), false) == 1000, "water goes in");
        });
    }

    @ServusGameTest(timeoutTicks = 60)
    public static void powerCostAndLidsWithoutFe(GameTestHelper helper) {
        build(helper, 4, 2, 2, true);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(form(helper, MASTER).formed(), "the 4 by 2 by 2 aquarium forms");
                    AquariumPartBlockEntity master = master(helper);
                    helper.assertTrue(master.powerCost() == 40, "16 tank blocks and 8 lids cost 40 FE/t, got " + master.powerCost());
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(TANK.offset(1, 2, 1)),
                            Direction.UP) == null, "a lid takes no FE");
                    EnergyView floor = EnergyView.of(helper.getLevel().getCapability(Capabilities.Energy.BLOCK,
                            helper.absolutePos(MASTER.east(3)), Direction.DOWN));
                    helper.assertTrue(floor != null, "a floor takes FE");
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    EnergyView floor = EnergyView.of(helper.getLevel().getCapability(Capabilities.Energy.BLOCK,
                            helper.absolutePos(MASTER.east(3)), Direction.DOWN));
                    int taken = floor.receiveEnergy(1000, false);
                    helper.assertTrue(taken == 160, "the buffer holds four ticks, took " + taken);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    int stored = master(helper).state().energy().getEnergyStored();
                    helper.assertTrue(stored == 120, "one powered tick spends 40, left " + stored);
                    helper.assertTrue(helper.getBlockState(TANK.offset(0, 2, 0)).getValue(AquariumLidBlock.LIT), "the lid lights up");
                })
                .thenSucceed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void settingsPayloadSetsKeepAndAutoCatch(GameTestHelper helper) {
        formed(helper, 100_000, master -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(helper.absolutePos(MASTER.above(3))));
            player.containerMenu = new AquariumMenu(1, player.getInventory(), master);
            AquariumMenu.applySettings(player, new AquariumSettingsPayload(master.getBlockPos(), false, 18));
            helper.assertTrue(master.state().keep(AquariumStatsManager.INSTANCE.stats()) == 18, "keep is 18");
            helper.assertFalse(master.state().autoCatch(), "auto catch is off");
            AquariumMenu.applySettings(player, new AquariumSettingsPayload(master.getBlockPos().above(), true, 3));
            helper.assertTrue(master.state().keep(AquariumStatsManager.INSTANCE.stats()) == 18, "settings for another block change nothing");
        });
    }

}
