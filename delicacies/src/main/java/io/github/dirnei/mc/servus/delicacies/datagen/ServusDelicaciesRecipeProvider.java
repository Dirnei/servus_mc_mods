package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.board.CuttingRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.BakingRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.PanRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.PotRecipe;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.CropTags;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenTags;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** The kitchen's crafting recipes, its starter pot, pan and cutting recipes, and the bakery from flour to Pretzel. */
final class ServusDelicaciesRecipeProvider extends RecipeProvider {

    private ServusDelicaciesRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    /** The data provider: builds the recipes once the registries are known. */
    static final class Runner extends RecipeProvider.Runner {

        Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ServusDelicaciesRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Servus Delicacies recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        crafting();
        cutting();
        cooking();
        bakery();
    }

    private void crafting() {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, ServusDelicaciesItems.STOVE_TILE.get(), 4)
                .pattern("CCC")
                .pattern("CSC")
                .pattern("CCC")
                .define('C', Items.CLAY_BALL)
                .define('S', ItemTags.STONE_CRAFTING_MATERIALS)
                .unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusDelicaciesItems.POT.get())
                .pattern("I I")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusDelicaciesItems.PAN.get())
                .pattern("II")
                .pattern("S ")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusDelicaciesItems.KITCHEN_KNIFE.get())
                .pattern("I")
                .pattern("S")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, ServusDelicaciesItems.CUTTING_BOARD.get())
                .requires(ItemTags.WOODEN_SLABS)
                .requires(ItemTags.WOODEN_SLABS)
                .requires(Items.STICK)
                .unlockedBy("has_wooden_slab", has(ItemTags.WOODEN_SLABS))
                .save(output);
    }

    private void cutting() {
        Ingredient knife = tag(KitchenTags.KNIVES);
        output.accept(key("cutting/shredded_cabbage"), new CuttingRecipe(tag(CropTags.crop(CropKind.CABBAGE)), knife,
                new ItemStackTemplate(ServusDelicaciesItems.SHREDDED_CABBAGE.get(), 2)), null);
        output.accept(key("cutting/chopped_onion"), new CuttingRecipe(tag(CropTags.crop(CropKind.ONION)), knife,
                new ItemStackTemplate(ServusDelicaciesItems.CHOPPED_ONION.get(), 2)), null);
    }

    private void cooking() {
        output.accept(key("pot/boiled_potatoes"), new PotRecipe(List.of(sized(Items.POTATO, 2)),
                Optional.of(SizedFluidIngredient.of(Fluids.WATER, 250)),
                new ItemStackTemplate(ServusDelicaciesItems.BOILED_POTATOES.get(), 2), 200), null);
        output.accept(key("pan/fried_egg"), new PanRecipe(List.of(new SizedIngredient(tag(Tags.Items.EGGS), 1)),
                new ItemStackTemplate(ServusDelicaciesItems.FRIED_EGG.get()), 100), null);
        output.accept(key("pan/fried_onions"), new PanRecipe(List.of(sized(ServusDelicaciesItems.CHOPPED_ONION.get(), 2)),
                new ItemStackTemplate(ServusDelicaciesItems.FRIED_ONIONS.get()), 120), null);
        output.accept(key("pan/fried_potatoes"), new PanRecipe(List.of(sized(ServusDelicaciesItems.BOILED_POTATOES.get(), 2),
                sized(ServusDelicaciesItems.CHOPPED_ONION.get(), 1)),
                new ItemStackTemplate(ServusDelicaciesItems.FRIED_POTATOES.get(), 2), 160), null);
    }

    private void bakery() {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusDelicaciesItems.BAKING_OVEN.get())
                .pattern("III")
                .pattern("IGI")
                .define('I', Items.IRON_INGOT)
                .define('G', Tags.Items.GLASS_PANES)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);
        output.accept(key("milling/flour"), new MillingRecipe(new SizedIngredient(tag(Tags.Items.CROPS_WHEAT), 1),
                new ItemStackTemplate(ServusDelicaciesItems.FLOUR.get()), 100), null);
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusDelicaciesItems.DOUGH.get(), 3)
                .requires(KitchenTags.WHEAT_FLOUR)
                .requires(KitchenTags.WHEAT_FLOUR)
                .requires(KitchenTags.WHEAT_FLOUR)
                .requires(Items.WATER_BUCKET)
                .unlockedBy("has_flour", has(KitchenTags.WHEAT_FLOUR))
                .save(output);
        SimpleCookingRecipeBuilder.campfireCooking(tag(ItemTags.LOGS_THAT_BURN), RecipeCategory.MISC, ServusDelicaciesItems.WOOD_ASH.get(), 0.1F, 600)
                .unlockedBy("has_log", has(ItemTags.LOGS_THAT_BURN))
                .save(output, key("wood_ash_from_campfire"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.CHARCOAL), RecipeCategory.MISC, CookingBookCategory.MISC,
                        ServusDelicaciesItems.WOOD_ASH.get(), 0.1F, 200)
                .unlockedBy("has_charcoal", has(Items.CHARCOAL))
                .save(output, key("wood_ash_from_smelting"));
        output.accept(key("pot/salt"), new PotRecipe(List.of(), Optional.of(SizedFluidIngredient.of(Fluids.WATER, 1000)),
                new ItemStackTemplate(ServusDelicaciesItems.SALT.get()), 400), null);
        output.accept(key("pot/lye"), new PotRecipe(List.of(new SizedIngredient(tag(KitchenTags.ASH), 1)),
                Optional.of(SizedFluidIngredient.of(Fluids.WATER, 250)), new ItemStackTemplate(ServusDelicaciesItems.LYE.get()), 100), null);
        output.accept(key("baking/bread_roll"), new BakingRecipe(List.of(sized(ServusDelicaciesItems.DOUGH.get(), 1)),
                new ItemStackTemplate(ServusDelicaciesItems.BREAD_ROLL.get()), 200), null);
        output.accept(key("baking/pretzel"), new BakingRecipe(List.of(sized(ServusDelicaciesItems.DOUGH.get(), 2),
                sized(ServusDelicaciesItems.LYE.get(), 1), new SizedIngredient(tag(KitchenTags.SALT), 1)),
                new ItemStackTemplate(ServusDelicaciesItems.PRETZEL.get(), 2), 240), null);
        output.accept(key("cutting/bread_crumbs"), new CuttingRecipe(Ingredient.of(ServusDelicaciesItems.BREAD_ROLL.get()),
                tag(KitchenTags.KNIVES), new ItemStackTemplate(ServusDelicaciesItems.BREAD_CRUMBS.get(), 2)), null);
    }

    private static SizedIngredient sized(ItemLike item, int count) {
        return SizedIngredient.of(item, count);
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, path));
    }
}
