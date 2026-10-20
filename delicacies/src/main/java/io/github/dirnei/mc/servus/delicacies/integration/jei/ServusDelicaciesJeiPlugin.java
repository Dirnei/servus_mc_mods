package io.github.dirnei.mc.servus.delicacies.integration.jei;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.board.CuttingRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.BakingRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.PanRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.PotRecipe;
import io.github.dirnei.mc.servus.delicacies.crock.FermentingRecipe;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shows the pot, pan, baking and cutting recipes and how to slice the placed loaf in JEI. Only loaded when JEI is present. */
@JeiPlugin
public final class ServusDelicaciesJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "jei");

    public static final RecipeType<RecipeHolder<PotRecipe>> POT = RecipeType.createRecipeHolderType(ServusDelicaciesRecipes.POT_ID);
    public static final RecipeType<RecipeHolder<PanRecipe>> PAN = RecipeType.createRecipeHolderType(ServusDelicaciesRecipes.PAN_ID);
    public static final RecipeType<RecipeHolder<BakingRecipe>> BAKING = RecipeType.createRecipeHolderType(ServusDelicaciesRecipes.BAKING_ID);
    public static final RecipeType<RecipeHolder<CuttingRecipe>> CUTTING =
            RecipeType.createRecipeHolderType(ServusDelicaciesRecipes.CUTTING_ID);

    public static final RecipeType<RecipeHolder<FermentingRecipe>> FERMENTING =
            RecipeType.createRecipeHolderType(ServusDelicaciesRecipes.FERMENTING_ID);

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new VesselRecipeCategory<>(gui, POT, "gui.servus_delicacies.jei.pot", ServusDelicaciesItems.POT.get(), true),
                new VesselRecipeCategory<>(gui, PAN, "gui.servus_delicacies.jei.pan", ServusDelicaciesItems.PAN.get(), false),
                new VesselRecipeCategory<>(gui, BAKING, "gui.servus_delicacies.jei.baking", ServusDelicaciesItems.BAKING_OVEN.get(), false),
                new CuttingRecipeCategory(gui));
        registration.addRecipeCategories(new FermentingRecipeCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        registration.addRecipes(POT, ServusRecipes.all(minecraft.level, ServusDelicaciesRecipes.POT.get()));
        registration.addRecipes(PAN, ServusRecipes.all(minecraft.level, ServusDelicaciesRecipes.PAN.get()));
        registration.addRecipes(BAKING, ServusRecipes.all(minecraft.level, ServusDelicaciesRecipes.BAKING.get()));
        registration.addRecipes(CUTTING, ServusRecipes.all(minecraft.level, ServusDelicaciesRecipes.CUTTING.get()));
        registration.addRecipes(FERMENTING, ServusRecipes.all(minecraft.level, ServusDelicaciesRecipes.FERMENTING.get()));
        registration.addIngredientInfo(ServusDelicaciesItems.LEBERKAESE.get(),
                Component.translatable("gui." + ServusDelicaciesIds.MOD_ID + ".jei.leberkaese_info"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ServusDelicaciesItems.POT.get()), POT);
        registration.addRecipeCatalyst(new ItemStack(ServusDelicaciesItems.PAN.get()), PAN);
        registration.addRecipeCatalyst(new ItemStack(ServusDelicaciesItems.BAKING_OVEN.get()), BAKING);
        registration.addRecipeCatalyst(new ItemStack(ServusDelicaciesItems.STOVE_TILE.get()), POT, PAN, BAKING);
        registration.addRecipeCatalyst(new ItemStack(ServusDelicaciesItems.CUTTING_BOARD.get()), CUTTING);
        registration.addRecipeCatalyst(new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get()), CUTTING);
        registration.addRecipeCatalyst(new ItemStack(ServusDelicaciesItems.FERMENTING_CROCK.get()), FERMENTING);
    }
}
