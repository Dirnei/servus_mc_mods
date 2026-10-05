package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipes;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shows assembling recipes in JEI. Only loaded when JEI is present. */
@JeiPlugin
public final class ServusManufacturingJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "jei");

    public static final RecipeType<RecipeHolder<AssemblingRecipe>> ASSEMBLING =
            RecipeType.createRecipeHolderType(ServusManufacturingRecipes.ASSEMBLING_ID);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AssemblingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        registration.addRecipes(ASSEMBLING, AssemblingRecipes.sorted(minecraft.level));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ServusManufacturingItems.ASSEMBLER.get()), ASSEMBLING, RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new AssemblerRecipeTransferHandler<>(ASSEMBLING), ASSEMBLING);
        registration.addRecipeTransferHandler(new AssemblerRecipeTransferHandler<>(RecipeTypes.CRAFTING), RecipeTypes.CRAFTING);
    }
}
