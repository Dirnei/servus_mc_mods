package io.github.dirnei.mc.servus.storage.integration.jei;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import io.github.dirnei.mc.servus.storage.pattern.OpenRecipeEditorPayload;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI plus button on crafting recipes while a Production Terminal is open:
 * asks the server to open the pattern editor prefilled with the recipe.
 */
public final class ProductionTerminalRecipeTransferHandler implements IRecipeTransferHandler<ProductionTerminalMenu, RecipeHolder<CraftingRecipe>> {

    @Override
    public Class<? extends ProductionTerminalMenu> getContainerClass() {
        return ProductionTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<ProductionTerminalMenu>> getMenuType() {
        return Optional.of(ServusStorageMenus.PRODUCTION_TERMINAL.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(ProductionTerminalMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            ClientPacketDistributor.sendToServer(new OpenRecipeEditorPayload(recipe.id()));
        }
        return null;
    }
}
