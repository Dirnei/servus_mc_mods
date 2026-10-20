package io.github.dirnei.mc.servus.brewery.integration.jei;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewingRecipe;
import io.github.dirnei.mc.servus.brewery.brewhouse.KilningRecipe;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shows the Brew Kettle and Malt Kiln recipes in JEI. Only loaded when JEI is present. */
@JeiPlugin
public final class ServusBreweryJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "jei");

    public static final RecipeType<RecipeHolder<BrewingRecipe>> BREWING = RecipeType.createRecipeHolderType(ServusBreweryRecipes.BREWING_ID);
    public static final RecipeType<RecipeHolder<KilningRecipe>> KILNING = RecipeType.createRecipeHolderType(ServusBreweryRecipes.KILNING_ID);

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new BrewingRecipeCategory(gui), new KilningRecipeCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        registration.addRecipes(BREWING, ServusRecipes.all(minecraft.level, ServusBreweryRecipes.BREWING.get()));
        registration.addRecipes(KILNING, ServusRecipes.all(minecraft.level, ServusBreweryRecipes.KILNING.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ServusBreweryItems.BREW_KETTLE.get()), BREWING);
        registration.addRecipeCatalyst(new ItemStack(ServusBreweryItems.BREWHOUSE_BRICKS.get()), BREWING, KILNING);
    }
}
