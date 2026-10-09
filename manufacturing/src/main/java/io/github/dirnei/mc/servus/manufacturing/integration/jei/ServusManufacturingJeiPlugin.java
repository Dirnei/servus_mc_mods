package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shows calcination and coagulation recipes in JEI. Only loaded when JEI is present. */
@JeiPlugin
public final class ServusManufacturingJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "jei");

    public static final RecipeType<RecipeHolder<CalcinationRecipe>> CALCINATION =
            RecipeType.createRecipeHolderType(ServusManufacturingRecipes.CALCINATION_ID);
    public static final RecipeType<RecipeHolder<CoagulationRecipe>> COAGULATION =
            RecipeType.createRecipeHolderType(ServusManufacturingRecipes.COAGULATION_ID);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CalcinationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CoagulationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        registration.addRecipes(CALCINATION, minecraft.level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.CALCINATION.get()));
        registration.addRecipes(COAGULATION, CoagulatorBlockEntity.sortedRecipes(minecraft.level.getRecipeManager()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ServusManufacturingItems.CALCINER.get()), CALCINATION);
        registration.addRecipeCatalyst(new ItemStack(ServusManufacturingItems.COAGULATOR.get()), COAGULATION);
    }
}
