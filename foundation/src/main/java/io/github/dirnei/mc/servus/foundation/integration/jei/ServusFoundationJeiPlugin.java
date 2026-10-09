package io.github.dirnei.mc.servus.foundation.integration.jei;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankItem;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.block.Blocks;

/**
 * Shows Foundation's special crafting recipes in JEI as example recipes, since JEI cannot
 * read recipes without fixed ingredients. Only loaded when JEI is present.
 */
@JeiPlugin
public final class ServusFoundationJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Example floors; any solid full block works, which the floor slot cycles through.
        Ingredient floors = Ingredient.of(Items.STONE_BRICKS, Items.COBBLESTONE, Items.COPPER_BLOCK, Items.SMOOTH_STONE,
                Items.OAK_PLANKS, Items.BRICKS, Items.DEEPSLATE_TILES, Items.POLISHED_ANDESITE);
        ShapedRecipePattern tankPattern = ShapedRecipePattern.of(Map.of(
                        'C', Ingredient.of(Items.COPPER_INGOT),
                        'G', Ingredient.of(Items.GLASS),
                        'F', floors),
                "CGC", "G G", "CFC");
        ShapedRecipe tank = new ShapedRecipe("", CraftingBookCategory.MISC, tankPattern, FluidTankItem.withFloor(Blocks.STONE_BRICKS));

        ShapelessRecipe facade = new ShapelessRecipe("", CraftingBookCategory.MISC,
                FacadeItem.of(Blocks.STONE_BRICKS, FacadeRecipe.RESULT_COUNT),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.STONE_BRICKS), Ingredient.of(Items.IRON_INGOT)));

        List<RecipeHolder<CraftingRecipe>> recipes = List.of(
                new RecipeHolder<>(id("fluid_tank_example"), tank),
                new RecipeHolder<>(id("facade_example"), facade));
        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "jei/" + path);
    }
}
