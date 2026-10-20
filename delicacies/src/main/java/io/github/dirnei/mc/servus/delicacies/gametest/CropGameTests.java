package io.github.dirnei.mc.servus.delicacies.gametest;

import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.CropLootTables;
import io.github.dirnei.mc.servus.delicacies.crop.CropStages;
import io.github.dirnei.mc.servus.delicacies.crop.DelicaciesCropBlock;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The Delicacies crops plant, grow with bone meal, drop like vanilla crops, and their first seeds can be found. */
public final class CropGameTests {

    private static final BlockPos PLOT = new BlockPos(2, 2, 2);
    private static final int ROLLS = 400;

    private CropGameTests() {
    }

    @ServusGameTest
    public static void everyCropPlantsOnFarmland(GameTestHelper helper) {
        int x = 1;
        for (CropKind crop : CropKind.values()) {
            BlockPos plot = new BlockPos(x, 2, 2);
            helper.setBlock(plot.below(), wetFarmland());
            ItemStack stack = new ItemStack(ServusDelicaciesItems.planting(crop), 2);
            useOnTop(helper, stack, plot.below());
            helper.assertBlockPresent(ServusDelicaciesBlocks.crop(crop), plot);
            helper.assertTrue(stack.getCount() == 1, crop + ": planting should use one item, left " + stack.getCount());
            x += 2;
        }
        helper.succeed();
    }

    @ServusGameTest
    public static void cropsDoNotPlantOnDirt(GameTestHelper helper) {
        helper.setBlock(PLOT.below(), Blocks.DIRT);
        ItemStack stack = new ItemStack(ServusDelicaciesItems.planting(CropKind.PARSLEY));
        useOnTop(helper, stack, PLOT.below());
        helper.assertBlockNotPresent(ServusDelicaciesBlocks.crop(CropKind.PARSLEY), PLOT);
        helper.assertTrue(stack.getCount() == 1, "no seed should be used on dirt");
        helper.succeed();
    }

    @ServusGameTest
    public static void boneMealRipensEveryCrop(GameTestHelper helper) {
        int x = 1;
        for (CropKind crop : CropKind.values()) {
            BlockPos plot = new BlockPos(x, 2, 2);
            DelicaciesCropBlock block = ServusDelicaciesBlocks.crop(crop);
            plant(helper, plot, block, 0);
            BlockPos absolute = helper.absolutePos(plot);
            for (int i = 0; i < 20 && !block.isMaxAge(helper.getLevel().getBlockState(absolute)); i++) {
                BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), helper.getLevel(), absolute, null);
            }
            helper.assertTrue(block.isMaxAge(helper.getLevel().getBlockState(absolute)), crop + " should ripen with bone meal");
            ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
            helper.assertFalse(BoneMealItem.applyBonemeal(boneMeal, helper.getLevel(), absolute, null),
                    crop + ": a ripe crop should not take bone meal");
            x += 2;
        }
        helper.succeed();
    }

    @ServusGameTest
    public static void ripeCropsDropTheirHarvest(GameTestHelper helper) {
        for (CropKind crop : CropKind.values()) {
            List<ItemStack> drops = drops(helper, crop, CropStages.MAX_AGE, ItemStack.EMPTY);
            Item harvest = ServusDelicaciesItems.harvest(crop);
            helper.assertTrue(count(drops, harvest) >= 1, crop + " should drop its harvest when ripe, got " + drops);
            Set<Item> kinds = drops.stream().map(ItemStack::getItem).collect(Collectors.toSet());
            Set<Item> allowed = new HashSet<>(List.of(harvest, ServusDelicaciesItems.planting(crop)));
            helper.assertTrue(allowed.containsAll(kinds), crop + " should drop only its own items, got " + drops);
        }
        helper.succeed();
    }

    @ServusGameTest
    public static void youngCropsDropOnePlantingItem(GameTestHelper helper) {
        for (CropKind crop : CropKind.values()) {
            for (int age = 0; age < CropStages.MAX_AGE; age++) {
                List<ItemStack> drops = drops(helper, crop, age, ItemStack.EMPTY);
                Item planting = ServusDelicaciesItems.planting(crop);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(planting) && drops.getFirst().getCount() == 1,
                        crop + " at age " + age + " should drop exactly one " + planting + ", got " + drops);
            }
        }
        helper.succeed();
    }

    @ServusGameTest
    public static void fortuneRaisesRipeDrops(GameTestHelper helper) {
        ItemStack fortunePickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        fortunePickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
        for (CropKind crop : List.of(CropKind.CABBAGE, CropKind.ONION)) {
            int plain = 0;
            int lucky = 0;
            for (int i = 0; i < 100; i++) {
                plain += total(drops(helper, crop, CropStages.MAX_AGE, ItemStack.EMPTY));
                lucky += total(drops(helper, crop, CropStages.MAX_AGE, fortunePickaxe));
            }
            helper.assertTrue(lucky > plain, crop + ": Fortune should raise the drops, " + lucky + " against " + plain);
        }
        helper.succeed();
    }

    @ServusGameTest
    public static void pickBlockGivesThePlantingItem(GameTestHelper helper) {
        for (CropKind crop : CropKind.values()) {
            plant(helper, PLOT, ServusDelicaciesBlocks.crop(crop), 3);
            ItemStack picked = helper.getBlockState(PLOT).getCloneItemStack(helper.getLevel(), helper.absolutePos(PLOT), false);
            helper.assertTrue(picked.is(ServusDelicaciesItems.planting(crop)), crop + " should pick its planting item, got " + picked);
        }
        helper.succeed();
    }

    @ServusGameTest
    public static void grassDropsPlantingItems(GameTestHelper helper) {
        Set<Item> found = rollGrass(helper, ItemStack.EMPTY);
        helper.assertTrue(!found.isEmpty(), "short grass broken by hand should sometimes drop a Delicacies planting item");
        helper.succeed();
    }

    @ServusGameTest
    public static void shearedGrassDropsNoPlantingItems(GameTestHelper helper) {
        Set<Item> found = rollGrass(helper, new ItemStack(Items.SHEARS));
        helper.assertTrue(found.isEmpty(), "sheared grass should drop no Delicacies planting item, got " + found);
        helper.succeed();
    }

    @ServusGameTest
    public static void villageHouseChestsHoldPlantingItems(GameTestHelper helper) {
        for (ResourceKey<LootTable> house : CropLootTables.VILLAGE_HOUSES) {
            Set<Item> found = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                found.addAll(plantingItemsIn(rollChest(helper, house)));
            }
            helper.assertTrue(!found.isEmpty(), house.identifier() + " should sometimes hold Delicacies planting items");
        }
        helper.succeed();
    }

    private static Set<Item> rollGrass(GameTestHelper helper, ItemStack tool) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(PLOT);
        Set<Item> found = new HashSet<>();
        for (int i = 0; i < ROLLS; i++) {
            found.addAll(plantingItemsIn(Block.getDrops(Blocks.SHORT_GRASS.defaultBlockState(), level, pos, null, null, tool)));
        }
        return found;
    }

    private static List<ItemStack> rollChest(GameTestHelper helper, ResourceKey<LootTable> table) {
        ServerLevel level = helper.getLevel();
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(PLOT)))
                .create(LootContextParamSets.CHEST);
        return level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params);
    }

    private static Set<Item> plantingItemsIn(List<ItemStack> stacks) {
        Set<Item> planting = new HashSet<>();
        for (CropKind crop : CropKind.values()) {
            planting.add(ServusDelicaciesItems.planting(crop));
        }
        return stacks.stream().map(ItemStack::getItem).filter(planting::contains).collect(Collectors.toSet());
    }

    /** The drops of the crop at {@code age}, broken with {@code tool}. */
    private static List<ItemStack> drops(GameTestHelper helper, CropKind crop, int age, ItemStack tool) {
        BlockState state = ServusDelicaciesBlocks.crop(crop).getStateForAge(age);
        return Block.getDrops(state, helper.getLevel(), helper.absolutePos(PLOT), null, null, tool);
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static int total(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(ItemStack::getCount).sum();
    }

    private static BlockState wetFarmland() {
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
    }

    private static void plant(GameTestHelper helper, BlockPos plot, DelicaciesCropBlock block, int age) {
        helper.setBlock(plot.below(), wetFarmland());
        helper.setBlock(plot, block.getStateForAge(age));
    }

    /** Uses {@code stack} on the top face of the block at {@code pos}, as a survival player's right click does. */
    private static void useOnTop(GameTestHelper helper, ItemStack stack, BlockPos pos) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(pos);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false)));
    }
}
