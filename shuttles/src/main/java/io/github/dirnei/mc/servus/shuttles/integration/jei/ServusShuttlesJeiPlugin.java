package io.github.dirnei.mc.servus.shuttles.integration.jei;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.client.StorageTerminalScreen;
import io.github.dirnei.mc.servus.shuttles.client.JeiSearchBridge;
import io.github.dirnei.mc.servus.shuttles.client.WarehouseControllerScreen;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

/**
 * JEI integration, loaded only when JEI is present: the transfer button opens
 * the Production Terminal's pattern editor and fills the Storage Terminal's
 * crafting grid, the Storage Terminal's search can follow JEI's search, an
 * ingredient dropped on the Rules tab's add slot opens its Rule tab, and JEI
 * keeps the terminal's tab strip, side column and upper rows free.
 */
@JeiPlugin
public final class ServusShuttlesJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ProductionTerminalRecipeTransferHandler(), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new StorageTerminalRecipeTransferHandler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(StorageTerminalScreen.class, new IGuiContainerHandler<StorageTerminalScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(StorageTerminalScreen screen) {
                return screen.extraAreas();
            }

            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory,
                                                                                              StorageTerminalScreen screen, double mouseX,
                                                                                              double mouseY) {
                return screen.gridItemAt(mouseX, mouseY)
                        .flatMap(item -> factory.createBuilder(item.stack()).buildWithArea(item.x(), item.y(), 16, 16));
            }
        });
        registration.addGhostIngredientHandler(StorageTerminalScreen.class, new RuleAddSlotHandler());
        registration.addGuiContainerHandler(WarehouseControllerScreen.class,
                new IGuiContainerHandler<WarehouseControllerScreen>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(WarehouseControllerScreen screen) {
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
