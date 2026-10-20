package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.board.CuttingRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.BakingRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.PanRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.PotRecipe;
import io.github.dirnei.mc.servus.delicacies.crock.FermentingRecipe;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.CropTags;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenTags;
import io.github.dirnei.mc.servus.delicacies.meat.LoafSlices;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
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
        meatDishes();
        sides();
    }

    /** The Fermenting Crock, sauerkraut and cheese, red cabbage, the dumplings and the bowl servings. */
    private void sides() {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusDelicaciesItems.FERMENTING_CROCK.get())
                .pattern("B B")
                .pattern("B B")
                .pattern("BBB")
                .define('B', Items.BRICK)
                .unlockedBy("has_brick", has(Items.BRICK))
                .save(output);
        Ingredient knife = tag(KitchenTags.KNIVES);
        output.accept(key("cutting/shredded_red_cabbage"), new CuttingRecipe(tag(CropTags.crop(CropKind.RED_CABBAGE)), knife,
                new ItemStackTemplate(ServusDelicaciesItems.SHREDDED_RED_CABBAGE.get(), 2)), null);
        output.accept(key("cutting/grated_potato"), new CuttingRecipe(tag(Tags.Items.CROPS_POTATO), knife,
                new ItemStackTemplate(ServusDelicaciesItems.GRATED_POTATO.get())), null);
        SizedIngredient salt = new SizedIngredient(tag(KitchenTags.SALT), 1);
        output.accept(key("fermenting/sauerkraut"), new FermentingRecipe(List.of(sized(ServusDelicaciesItems.SHREDDED_CABBAGE.get(), 4), salt),
                Optional.empty(), new ItemStackTemplate(ServusDelicaciesItems.SAUERKRAUT.get(), 4), 72000), null);
        output.accept(key("fermenting/cheese"), new FermentingRecipe(List.of(salt), Optional.of(milk(1000)),
                new ItemStackTemplate(ServusDelicaciesItems.CHEESE.get(), 4), 48000), null);
        output.accept(key("pot/braised_red_cabbage"), new PotRecipe(List.of(sized(ServusDelicaciesItems.SHREDDED_RED_CABBAGE.get(), 2),
                sized(Items.APPLE, 1), sized(Items.SUGAR, 1)), Optional.of(SizedFluidIngredient.of(Fluids.WATER, 250)),
                new ItemStackTemplate(ServusDelicaciesItems.BRAISED_RED_CABBAGE.get(), 2), 300), null);
        output.accept(key("pot/bread_dumplings"), new PotRecipe(List.of(sized(ServusDelicaciesItems.BREAD_CRUMBS.get(), 2),
                new SizedIngredient(tag(Tags.Items.EGGS), 1), sized(ServusDelicaciesItems.CHOPPED_ONION.get(), 1),
                new SizedIngredient(tag(CropTags.crop(CropKind.PARSLEY)), 1)), Optional.of(milk(250)),
                new ItemStackTemplate(ServusDelicaciesItems.BREAD_DUMPLING.get(), 2), 300), null);
        output.accept(key("pot/potato_dumplings"), new PotRecipe(List.of(sized(ServusDelicaciesItems.GRATED_POTATO.get(), 2),
                sized(ServusDelicaciesItems.BOILED_POTATOES.get(), 1), salt), Optional.of(SizedFluidIngredient.of(Fluids.WATER, 250)),
                new ItemStackTemplate(ServusDelicaciesItems.POTATO_DUMPLING.get(), 2), 300), null);
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusDelicaciesItems.BOWL_OF_SAUERKRAUT.get())
                .requires(ServusDelicaciesItems.SAUERKRAUT.get())
                .requires(Items.BOWL)
                .unlockedBy("has_sauerkraut", has(ServusDelicaciesItems.SAUERKRAUT.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusDelicaciesItems.BOWL_OF_RED_CABBAGE.get())
                .requires(ServusDelicaciesItems.BRAISED_RED_CABBAGE.get())
                .requires(Items.BOWL)
                .unlockedBy("has_braised_red_cabbage", has(ServusDelicaciesItems.BRAISED_RED_CABBAGE.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusDelicaciesItems.POTATO_SALAD.get())
                .requires(ServusDelicaciesItems.BOILED_POTATOES.get())
                .requires(ServusDelicaciesItems.BOILED_POTATOES.get())
                .requires(ServusDelicaciesItems.CHOPPED_ONION.get())
                .requires(Items.BOWL)
                .unlockedBy("has_boiled_potatoes", has(ServusDelicaciesItems.BOILED_POTATOES.get()))
                .save(output);
    }

    /** {@code amount} of any fluid in {@code c:milk}. */
    private SizedFluidIngredient milk(int amount) {
        return new SizedFluidIngredient(FluidIngredient.of(registries.lookupOrThrow(Registries.FLUID).getOrThrow(Tags.Fluids.MILK)), amount);
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

    /** Cuts, breading, sausage meat, ham, Weisswurst, mustard, the Leberkaese loaf and roll, Roast Pork and Gravy. */
    private void meatDishes() {
        Ingredient knife = tag(KitchenTags.KNIVES);
        output.accept(key("cutting/pork_cutlet"), new CuttingRecipe(Ingredient.of(Items.PORKCHOP), knife,
                new ItemStackTemplate(ServusDelicaciesItems.PORK_CUTLET.get())), null);
        output.accept(key("cutting/minced_pork"), new CuttingRecipe(Ingredient.of(ServusDelicaciesItems.PORK_CUTLET.get()), knife,
                new ItemStackTemplate(ServusDelicaciesItems.MINCED_PORK.get(), 2)), null);
        output.accept(key("cutting/minced_beef"), new CuttingRecipe(Ingredient.of(Items.BEEF), knife,
                new ItemStackTemplate(ServusDelicaciesItems.MINCED_BEEF.get(), 2)), null);
        output.accept(key("cutting/leberkaese_slices"), new CuttingRecipe(Ingredient.of(ServusDelicaciesItems.LEBERKAESE.get()), knife,
                new ItemStackTemplate(ServusDelicaciesItems.LEBERKAESE_SLICE.get(), LoafSlices.SLICES)), null);

        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusDelicaciesItems.BREADED_CUTLET.get())
                .requires(ServusDelicaciesItems.PORK_CUTLET.get())
                .requires(KitchenTags.WHEAT_FLOUR)
                .requires(Tags.Items.EGGS)
                .requires(ServusDelicaciesItems.BREAD_CRUMBS.get())
                .unlockedBy("has_pork_cutlet", has(ServusDelicaciesItems.PORK_CUTLET.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusDelicaciesItems.RAW_CORDON_BLEU.get())
                .requires(ServusDelicaciesItems.PORK_CUTLET.get())
                .requires(ServusDelicaciesItems.HAM.get())
                .requires(KitchenTags.CHEESE)
                .requires(KitchenTags.WHEAT_FLOUR)
                .requires(Tags.Items.EGGS)
                .requires(ServusDelicaciesItems.BREAD_CRUMBS.get())
                .unlockedBy("has_ham", has(ServusDelicaciesItems.HAM.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusDelicaciesItems.SAUSAGE_MEAT.get(), 2)
                .requires(ServusDelicaciesItems.MINCED_PORK.get())
                .requires(ServusDelicaciesItems.MINCED_BEEF.get())
                .requires(CropTags.crop(CropKind.PARSLEY))
                .requires(KitchenTags.SALT)
                .unlockedBy("has_minced_pork", has(ServusDelicaciesItems.MINCED_PORK.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusDelicaciesItems.LEBERKAESE_ROLL.get())
                .requires(ServusDelicaciesItems.LEBERKAESE_SLICE.get())
                .requires(ServusDelicaciesItems.BREAD_ROLL.get())
                .unlockedBy("has_leberkaese_slice", has(ServusDelicaciesItems.LEBERKAESE_SLICE.get()))
                .save(output);

        output.accept(key("pan/schnitzel"), new PanRecipe(List.of(sized(ServusDelicaciesItems.BREADED_CUTLET.get(), 1)),
                new ItemStackTemplate(ServusDelicaciesItems.SCHNITZEL.get()), 160), null);
        output.accept(key("pan/cordon_bleu"), new PanRecipe(List.of(sized(ServusDelicaciesItems.RAW_CORDON_BLEU.get(), 1)),
                new ItemStackTemplate(ServusDelicaciesItems.CORDON_BLEU.get()), 200), null);

        Optional<SizedFluidIngredient> water = Optional.of(SizedFluidIngredient.of(Fluids.WATER, 250));
        output.accept(key("pot/ham"), new PotRecipe(List.of(sized(Items.PORKCHOP, 1), new SizedIngredient(tag(KitchenTags.SALT), 1)),
                water, new ItemStackTemplate(ServusDelicaciesItems.HAM.get(), 2), 300), null);
        output.accept(key("pot/weisswurst"), new PotRecipe(List.of(sized(ServusDelicaciesItems.SAUSAGE_MEAT.get(), 2)),
                water, new ItemStackTemplate(ServusDelicaciesItems.WEISSWURST.get(), 2), 200), null);
        output.accept(key("pot/sweet_mustard"), new PotRecipe(List.of(sized(ServusDelicaciesItems.MUSTARD_FLOUR.get(), 2),
                sized(Items.SUGAR, 1)), water, new ItemStackTemplate(ServusDelicaciesItems.SWEET_MUSTARD.get(), 2), 200), null);
        output.accept(key("pot/gravy"), new PotRecipe(List.of(sized(ServusDelicaciesItems.CHOPPED_ONION.get(), 1),
                new SizedIngredient(tag(KitchenTags.WHEAT_FLOUR), 1), sized(Items.BOWL, 1)),
                water, new ItemStackTemplate(ServusDelicaciesItems.GRAVY.get()), 160), null);

        output.accept(key("baking/leberkaese"), new BakingRecipe(List.of(sized(ServusDelicaciesItems.MINCED_PORK.get(), 2),
                sized(ServusDelicaciesItems.MINCED_BEEF.get(), 2), new SizedIngredient(tag(KitchenTags.SALT), 1)),
                new ItemStackTemplate(ServusDelicaciesItems.LEBERKAESE.get()), 400), null);
        output.accept(key("baking/roast_pork"), new BakingRecipe(List.of(sized(Items.PORKCHOP, 2),
                new SizedIngredient(tag(CropTags.crop(CropKind.ONION)), 1), new SizedIngredient(tag(KitchenTags.SALT), 1)),
                new ItemStackTemplate(ServusDelicaciesItems.ROAST_PORK.get(), 2), 400), null);

        output.accept(key("milling/mustard_flour"), new MillingRecipe(new SizedIngredient(tag(CropTags.crop(CropKind.MUSTARD)), 1),
                new ItemStackTemplate(ServusDelicaciesItems.MUSTARD_FLOUR.get()), 100), null);
    }

    private static SizedIngredient sized(ItemLike item, int count) {
        return SizedIngredient.of(item, count);
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, path));
    }
}
