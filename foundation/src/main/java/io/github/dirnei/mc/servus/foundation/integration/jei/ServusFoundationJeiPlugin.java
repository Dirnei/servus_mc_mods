package io.github.dirnei.mc.servus.foundation.integration.jei;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.client.UtilityDuctScreen;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.facade.HollowFacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankItem;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/**
 * Shows the Mill's milling recipes in JEI, and Foundation's special crafting recipes as example recipes, since JEI cannot
 * read recipes without fixed ingredients, and lets items dragged from JEI set duct
 * filter slots. Only loaded when JEI is present.
 */
@JeiPlugin
public final class ServusFoundationJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "jei");
    private static final Recipe.CommonInfo INFO = new Recipe.CommonInfo(false);
    private static final CraftingRecipe.CraftingBookInfo BOOK = new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    /** Items dragged from JEI onto a duct filter slot set that slot. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(UtilityDuctScreen.class, new DuctFilterGhostHandler());
    }

    /** Facades and Hollow Facades differ only by their block component; tell them apart by that block. */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> byBlock = new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return FacadeItem.blockOf(stack);
            }
        };
        registration.registerSubtypeInterpreter(ServusLogisticsItems.FACADE.get(), byBlock);
        registration.registerSubtypeInterpreter(ServusLogisticsItems.HOLLOW_FACADE.get(), byBlock);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MillingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(MachineContent.MILL_ITEM.get()), MillingRecipeCategory.TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            registration.addRecipes(MillingRecipeCategory.TYPE, ServusRecipes.all(minecraft.level, MachineContent.MILLING.get()));
        }
        // Any solid full block is a floor; the slot cycles through these examples.
        Ingredient floors = Ingredient.of(Items.STONE_BRICKS, Items.COBBLESTONE, Items.COPPER_BLOCK, Items.SMOOTH_STONE,
                Items.OAK_PLANKS, Items.BRICKS, Items.DEEPSLATE_TILES, Items.POLISHED_ANDESITE);
        ShapedRecipePattern tankPattern = ShapedRecipePattern.of(Map.of(
                        'C', Ingredient.of(Items.COPPER_INGOT),
                        'G', Ingredient.of(Items.GLASS),
                        'F', floors),
                "CGC", "G G", "CFC");
        ShapedRecipe tank = new ShapedRecipe(INFO, BOOK, tankPattern, template(FluidTankItem.withFloor(Blocks.STONE_BRICKS)));

        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        recipes.add(new RecipeHolder<>(id("fluid_tank_example"), tank));
        for (Block block : FacadeItem.EXAMPLES) {
            ShapelessRecipe facade = new ShapelessRecipe(INFO, BOOK,
                    template(FacadeItem.of(block, FacadeRecipe.RESULT_COUNT)),
                    List.of(Ingredient.of(block), Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(Tags.Items.NUGGETS))));
            recipes.add(new RecipeHolder<>(id("facade_" + BuiltInRegistries.BLOCK.getKey(block).getPath()), facade));
            ShapedRecipePattern ring = ShapedRecipePattern.of(Map.of('F', DataComponentIngredient.of(false, FacadeItem.of(block, 1))), "FFF", "F F", "FFF");
            ShapedRecipe hollow = new ShapedRecipe(INFO, BOOK, ring,
                    template(FacadeItem.of(block, HollowFacadeRecipe.RESULT_COUNT, true)));
            recipes.add(new RecipeHolder<>(id("hollow_facade_" + BuiltInRegistries.BLOCK.getKey(block).getPath()), hollow));
        }
        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
        List<ItemStack> facades = new ArrayList<>(FacadeItem.examples());
        facades.addAll(FacadeItem.hollowExamples());
        registration.addItemStackInfo(facades, Component.translatable("jei.servus_foundation.facade.info"));
    }

    private static ItemStackTemplate template(ItemStack stack) {
        return ItemStackTemplate.fromNonEmptyStack(stack);
    }

    private static ResourceKey<Recipe<?>> id(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "jei/" + path));
    }
}
