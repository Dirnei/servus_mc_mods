package io.github.dirnei.mc.servus.storage.integration.jei;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

/** JEI integration, loaded only when JEI is present: the transfer button opens the Crafting Station's editor. */
@JeiPlugin
public final class ServusStorageJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new CraftingStationRecipeTransferHandler(), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new ProductionTerminalRecipeTransferHandler(), RecipeTypes.CRAFTING);
    }
}
