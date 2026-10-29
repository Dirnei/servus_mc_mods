package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Datagen of the vanilla milling recipes Foundation ships: one input item each, so no two recipes compete. */
final class MillingRecipes {

    private static final int QUICK = 100;
    private static final int SLOW = 200;

    /** A flower and the dye its vanilla crafting recipe makes; the Mill gives twice that. */
    private record Flower(Item flower, Item dye, int vanillaCount) {
    }

    private static final List<Flower> FLOWERS = List.of(
            new Flower(Items.WITHER_ROSE, Items.BLACK_DYE, 1),
            new Flower(Items.CORNFLOWER, Items.BLUE_DYE, 1),
            new Flower(Items.BLUE_ORCHID, Items.LIGHT_BLUE_DYE, 1),
            new Flower(Items.AZURE_BLUET, Items.LIGHT_GRAY_DYE, 1),
            new Flower(Items.OXEYE_DAISY, Items.LIGHT_GRAY_DYE, 1),
            new Flower(Items.WHITE_TULIP, Items.LIGHT_GRAY_DYE, 1),
            new Flower(Items.ALLIUM, Items.MAGENTA_DYE, 1),
            new Flower(Items.LILAC, Items.MAGENTA_DYE, 2),
            new Flower(Items.ORANGE_TULIP, Items.ORANGE_DYE, 1),
            new Flower(Items.TORCHFLOWER, Items.ORANGE_DYE, 1),
            new Flower(Items.OPEN_EYEBLOSSOM, Items.ORANGE_DYE, 1),
            new Flower(Items.CLOSED_EYEBLOSSOM, Items.GRAY_DYE, 1),
            new Flower(Items.PEONY, Items.PINK_DYE, 2),
            new Flower(Items.PINK_TULIP, Items.PINK_DYE, 1),
            new Flower(Items.CACTUS_FLOWER, Items.PINK_DYE, 1),
            new Flower(Items.PINK_PETALS, Items.PINK_DYE, 1),
            new Flower(Items.POPPY, Items.RED_DYE, 1),
            new Flower(Items.RED_TULIP, Items.RED_DYE, 1),
            new Flower(Items.ROSE_BUSH, Items.RED_DYE, 2),
            new Flower(Items.LILY_OF_THE_VALLEY, Items.WHITE_DYE, 1),
            new Flower(Items.DANDELION, Items.YELLOW_DYE, 1),
            new Flower(Items.WILDFLOWERS, Items.YELLOW_DYE, 1),
            new Flower(Items.SUNFLOWER, Items.YELLOW_DYE, 2),
            new Flower(Items.PITCHER_PLANT, Items.CYAN_DYE, 2));

    private MillingRecipes() {
    }

    static void recipes(RecipeOutput output, HolderGetter<Item> items) {
        mill(output, "sand", Ingredient.of(Items.GRAVEL), Items.SAND, 1, SLOW);
        mill(output, "cobblestone", Ingredient.of(Items.STONE), Items.COBBLESTONE, 1, QUICK);
        mill(output, "cobbled_deepslate", Ingredient.of(Items.DEEPSLATE), Items.COBBLED_DEEPSLATE, 1, QUICK);
        mill(output, "gravel", tag(items, Tags.Items.COBBLESTONES_NORMAL), Items.GRAVEL, 1, SLOW);
        mill(output, "gravel_from_deepslate", tag(items, Tags.Items.COBBLESTONES_DEEPSLATE), Items.GRAVEL, 1, SLOW);
        mill(output, "sand_from_sandstone", tag(items, Tags.Items.SANDSTONE_UNCOLORED_BLOCKS), Items.SAND, 4, SLOW);
        mill(output, "red_sand", tag(items, Tags.Items.SANDSTONE_RED_BLOCKS), Items.RED_SAND, 4, SLOW);
        mill(output, "sugar", tag(items, Tags.Items.CROPS_SUGAR_CANE), Items.SUGAR, 2, QUICK);
        mill(output, "glowstone_dust", Ingredient.of(Items.GLOWSTONE), Items.GLOWSTONE_DUST, 4, QUICK);
        mill(output, "bone_meal", tag(items, Tags.Items.BONES), Items.BONE_MEAL, 4, QUICK);
        mill(output, "blaze_powder", tag(items, Tags.Items.RODS_BLAZE), Items.BLAZE_POWDER, 3, SLOW);
        mill(output, "string", tag(items, ItemTags.WOOL), Items.STRING, 4, QUICK);
        mill(output, "clay_ball", Ingredient.of(Items.CLAY), Items.CLAY_BALL, 4, QUICK);
        mill(output, "quartz", Ingredient.of(Items.QUARTZ_BLOCK), Items.QUARTZ, 4, SLOW);
        mill(output, "amethyst_shard", Ingredient.of(Items.AMETHYST_BLOCK), Items.AMETHYST_SHARD, 4, SLOW);
        mill(output, "charcoal_powder", Ingredient.of(Items.CHARCOAL), ServusFoundationItems.CHARCOAL_POWDER.get(), 2, QUICK);
        for (Flower flower : FLOWERS) {
            mill(output, path(flower.dye()) + "_from_" + path(flower.flower()), Ingredient.of(flower.flower()),
                    flower.dye(), 2 * flower.vanillaCount(), QUICK);
        }
    }

    private static void mill(RecipeOutput output, String name, Ingredient input, ItemLike result, int count, int work) {
        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "milling/" + name)),
                new MillingRecipe(new SizedIngredient(input, 1), new ItemStackTemplate(result.asItem(), count), work), null);
    }

    private static Ingredient tag(HolderGetter<Item> items, TagKey<Item> tag) {
        return Ingredient.of(items.getOrThrow(tag));
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
