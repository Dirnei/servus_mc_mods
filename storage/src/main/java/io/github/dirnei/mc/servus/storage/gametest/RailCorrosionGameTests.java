package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldWater;
import io.github.dirnei.mc.servus.storage.rail.RailBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Copper corrosion of farm rail fields: aging, upkeep, the corroded field, axe scraping and joins (m10-rail-corrosion). */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailCorrosionGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final int RAIL_Z = 5;
    private static final int FIRST = 3;
    private static final int LAST = 7;
    private static final int RAILS = LAST - FIRST + 1;
    private static final BlockPos HEAD = new BlockPos(2, 1, RAIL_Z);

    private RailCorrosionGameTests() {
    }

    // Helpers

    private static void rail(GameTestHelper helper, int x) {
        BlockPos rail = new BlockPos(x, 1, RAIL_Z);
        helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(rail, Block.updateFromNeighbourShapes(ServusStorageBlocks.FARM_RAIL.get().defaultBlockState(),
                helper.getLevel(), helper.absolutePos(rail)));
    }

    /** Farm rails from x 3 to 7, farmland north of them one below, the head west of the rails. */
    private static FieldHeadBlockEntity field(GameTestHelper helper) {
        for (int x = FIRST; x <= LAST; x++) {
            helper.setBlock(new BlockPos(x, 0, RAIL_Z + 1), Blocks.FARMLAND.defaultBlockState());
            rail(helper, x);
        }
        helper.setBlock(HEAD, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        return helper.getBlockEntity(HEAD);
    }

    private static int stage(GameTestHelper helper, int x) {
        return ((RailBlockEntity) helper.getBlockEntity(new BlockPos(x, 1, RAIL_Z))).corrosion();
    }

    private static void assertRails(GameTestHelper helper, int from, int to, int expected, String why) {
        for (int x = from; x <= to; x++) {
            int stage = stage(helper, x);
            helper.assertTrue(stage == expected, why + ": rail at x " + x + " is " + stage + ", expected " + expected);
        }
    }

    private static int perStep() {
        return StorageStatsManager.INSTANCE.fieldCorrosionWater();
    }

    private static IFluidHandler fluid(GameTestHelper helper) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(HEAD), null);
    }

    private static boolean wet(GameTestHelper helper, int x) {
        return FarmlandWaterManager.hasBlockWaterTicket(helper.getLevel(), helper.absolutePos(new BlockPos(x, 0, RAIL_Z + 1)));
    }

    private static Player playerHolding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /** Uses the player's main hand item on the top of the rail at that x, as a right click does. */
    private static void useOnRail(GameTestHelper helper, Player player, int x) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, RAIL_Z));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        helper.getLevel().getBlockState(pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    // Aging

    /** A served field starts at copper on every rail and goes to exposed after one step of water without upkeep. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void agingWithoutUpkeep(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.corrosion() == FieldCorrosion.COPPER, "a new field is copper, got " + head.corrosion());
            assertRails(helper, FIRST, LAST, FieldCorrosion.COPPER, "served rails take the field's stage");
            head.age(perStep() - 1);
            helper.assertTrue(head.corrosion() == FieldCorrosion.COPPER, "not a full step yet");
            head.age(1);
            helper.assertTrue(head.corrosion() == FieldCorrosion.EXPOSED, "one step makes exposed, got " + head.corrosion());
            assertRails(helper, FIRST, LAST, FieldCorrosion.EXPOSED, "the whole field ages");
            helper.succeed();
        });
    }

    /** Honeycomb in the slot holds the stage for one step each. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void honeycombHoldsStage(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.WEATHERED);
            ItemStack rest = head.itemHandler().insertItem(0, new ItemStack(Items.HONEYCOMB, 3), false);
            helper.assertTrue(rest.isEmpty(), "the head takes honeycomb through its item handler");
            head.age(perStep());
            helper.assertTrue(head.corrosion() == FieldCorrosion.WEATHERED, "honeycomb holds weathered, got " + head.corrosion());
            helper.assertTrue(head.antiCorrosion().getStackInSlot(0).getCount() == 2, "one honeycomb used");
            assertRails(helper, FIRST, LAST, FieldCorrosion.WEATHERED, "rails stay weathered");
            helper.succeed();
        });
    }

    /** Hydration water counts toward aging. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void waterUseCountsForAging(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            fluid(helper).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.succeedWhen(() -> {
                helper.assertTrue(head.hydrated(), "the head should hydrate");
                long need = FieldWater.use(RAILS, StorageStatsManager.INSTANCE.fieldWaterPerRail());
                helper.assertTrue(head.agedWater() == need, "the used water counts, got " + head.agedWater() + " expected " + need);
            });
        });
    }

    // Corroded

    /** A corroded field is no longer watered, uses no water and shows the rust rim. */
    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void corrodedStopsHydration(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            fluid(helper).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(head.hydrated() && wet(helper, 4), "the field should be watered first");
                head.setStage(FieldCorrosion.OXIDIZED);
                head.age(perStep());
                helper.assertTrue(head.corrosion() == FieldCorrosion.CORRODED, "oxidized plus a step is corroded");
                helper.runAfterDelay(2, () -> {
                    int water = head.water();
                    helper.assertTrue(!head.hydrated() && !wet(helper, 4), "a corroded field is not watered");
                    helper.assertTrue(helper.getBlockState(HEAD).getValue(FieldHeadBlock.CORRODED), "the head shows the rust rim");
                    helper.runAfterDelay(5, () -> {
                        helper.assertTrue(head.water() == water, "a corroded field uses no water");
                        helper.succeed();
                    });
                });
            });
        });
    }

    // Axe

    /** An axe scrapes every rail back one stage and loses one durability per rail; the field is watered again. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void axeScrapesField(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.CORRODED);
            fluid(helper).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            Player player = playerHolding(helper, axe);
            useOnRail(helper, player, 5);
            helper.assertTrue(head.corrosion() == FieldCorrosion.OXIDIZED, "scraped to oxidized, got " + head.corrosion());
            assertRails(helper, FIRST, LAST, FieldCorrosion.OXIDIZED, "every rail is scraped");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == RAILS,
                    "one durability per rail, got " + player.getMainHandItem().getDamageValue());
            helper.succeedWhen(() -> {
                helper.assertTrue(head.hydrated(), "the scraped field is watered again");
                helper.assertTrue(!helper.getBlockState(HEAD).getValue(FieldHeadBlock.CORRODED), "the rust rim is gone");
            });
        });
    }

    /** An axe with less durability left than the field has rails does nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void wornAxeRefused(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.WEATHERED);
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            axe.setDamageValue(axe.getMaxDamage() - (RAILS - 2));
            int damage = axe.getDamageValue();
            Player player = playerHolding(helper, axe);
            useOnRail(helper, player, 4);
            helper.assertTrue(head.corrosion() == FieldCorrosion.WEATHERED, "a worn axe changes nothing");
            assertRails(helper, FIRST, LAST, FieldCorrosion.WEATHERED, "rails stay weathered");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == damage, "the worn axe keeps its durability");
            helper.succeed();
        });
    }

    /** Fresh copper is not scraped. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void freshCopperNotScraped(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            Player player = playerHolding(helper, new ItemStack(Items.IRON_AXE));
            useOnRail(helper, player, 4);
            helper.assertTrue(head.corrosion() == FieldCorrosion.COPPER, "copper stays copper");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == 0, "the axe keeps its durability");
            helper.succeed();
        });
    }

    // Joins

    /** A rail placed at the end of a headed field takes the field's stage. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void placedRailAdoptsStage(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.WEATHERED);
            rail(helper, LAST + 1);
            helper.succeedWhen(() -> helper.assertTrue(stage(helper, LAST + 1) == FieldCorrosion.WEATHERED,
                    "the new rail takes weathered, got " + stage(helper, LAST + 1)));
        });
    }

    /** A broken rail's drop places a loose, fresh rail. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void droppedRailIsFresh(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.OXIDIZED);
            BlockPos pos = helper.absolutePos(new BlockPos(LAST, 1, RAIL_Z));
            List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                    helper.getLevel().getBlockEntity(pos), null, new ItemStack(Items.IRON_PICKAXE));
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(ServusStorageItems.FARM_RAIL.get()), "one farm rail drops, got " + drops);
            ItemStack stack = drops.get(0);
            BlockPos at = new BlockPos(5, 1, 10);
            helper.setBlock(at.below(), Blocks.STONE.defaultBlockState());
            Player player = playerHolding(helper, stack);
            BlockPos floor = helper.absolutePos(at.below());
            stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
            helper.assertBlockPresent(ServusStorageBlocks.FARM_RAIL.get(), at);
            helper.runAfterDelay(2, () -> {
                int placed = ((RailBlockEntity) helper.getBlockEntity(at)).corrosion();
                helper.assertTrue(placed == FieldCorrosion.LOOSE, "the placed drop is fresh and loose, got " + placed);
                helper.succeed();
            });
        });
    }

    /** Without a head, a rail joining a weathered and an exposed run makes all of them exposed. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void headlessJoinTakesYoungest(GameTestHelper helper) {
        for (int x : new int[] {3, 4, 6, 7}) {
            rail(helper, x);
        }
        helper.runAfterDelay(2, () -> {
            for (int x : new int[] {3, 4}) {
                ((RailBlockEntity) helper.getBlockEntity(new BlockPos(x, 1, RAIL_Z))).setCorrosion(FieldCorrosion.WEATHERED);
            }
            for (int x : new int[] {6, 7}) {
                ((RailBlockEntity) helper.getBlockEntity(new BlockPos(x, 1, RAIL_Z))).setCorrosion(FieldCorrosion.EXPOSED);
            }
            rail(helper, 5);
            helper.succeedWhen(() -> assertRails(helper, 3, 7, FieldCorrosion.EXPOSED, "the joined field takes the younger stage"));
        });
    }

    // Upkeep

    /** A hopper fills the anti-corrosion slot with honeycomb and keeps other items; the head is an upkeep holder. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void hopperFillsUpkeep(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        BlockPos hopperPos = HEAD.above();
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
        hopper.setItem(0, new ItemStack(Items.HONEYCOMB, 2));
        hopper.setItem(1, new ItemStack(Items.WHEAT_SEEDS, 1));
        UpkeepHolder upkeep = helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, helper.absolutePos(HEAD), Direction.UP);
        helper.assertTrue(upkeep != null && upkeep.upkeepItem().is(Items.HONEYCOMB)
                        && upkeep.upkeepLevel() == StorageStatsManager.INSTANCE.fieldAntiCorrosionLevel(),
                "the head keeps honeycomb as upkeep");
        helper.succeedWhen(() -> {
            helper.assertTrue(head.antiCorrosion().getStackInSlot(0).is(Items.HONEYCOMB)
                    && head.antiCorrosion().getStackInSlot(0).getCount() == 2, "both honeycomb went in");
            helper.assertTrue(upkeep.upkeepCount() == 2, "the upkeep count follows the slot");
            helper.assertTrue(hopper.getItem(1).is(Items.WHEAT_SEEDS), "seeds are refused");
            helper.assertTrue(head.itemHandler().extractItem(0, 1, true).isEmpty(), "automation takes nothing out");
        });
    }
}
