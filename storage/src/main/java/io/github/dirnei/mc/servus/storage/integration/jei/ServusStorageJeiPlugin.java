package io.github.dirnei.mc.servus.storage.integration.jei;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.client.CounterTerminalScreen;
import io.github.dirnei.mc.servus.storage.client.JeiSearchBridge;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI integration, loaded only when JEI is present: the transfer button opens
 * the Production Terminal's pattern editor and fills the Counter Terminal's
 * crafting grid, the Counter Terminal's search can follow JEI's search, and
 * JEI keeps the terminal's side column and upper rows free.
 */
@JeiPlugin
public final class ServusStorageJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ProductionTerminalRecipeTransferHandler(), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new CounterTerminalRecipeTransferHandler(), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(CounterTerminalScreen.class, new IGuiContainerHandler<CounterTerminalScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(CounterTerminalScreen screen) {
                return screen.extraAreas();
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        IIngredientFilter filter = runtime.getIngredientFilter();
        JeiSearchBridge.connect(filter::setFilterText, filter::getFilterText);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiSearchBridge.disconnect();
    }
}
