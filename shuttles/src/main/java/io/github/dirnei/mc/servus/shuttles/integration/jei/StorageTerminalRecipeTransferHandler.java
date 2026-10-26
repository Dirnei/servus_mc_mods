package io.github.dirnei.mc.servus.shuttles.integration.jei;

import io.github.dirnei.mc.servus.shuttles.client.ClientStock;
import io.github.dirnei.mc.servus.shuttles.client.StorageTerminalScreen;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.terminal.OrderGridRecipePayload;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * JEI plus button on crafting recipes while a Storage Terminal is open: opens
 * the order grid and asks the server to lay the recipe out in its ghost
 * cells, the same way the Production Terminal's editor is filled. Refused
 * while the terminal's data line has no Crafting CPU and Crafting Station to
 * order from.
 */
public final class StorageTerminalRecipeTransferHandler implements IRecipeTransferHandler<StorageTerminalMenu, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;

    public StorageTerminalRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends StorageTerminalMenu> getContainerClass() {
        return StorageTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<StorageTerminalMenu>> getMenuType() {
        return Optional.of(ServusShuttlesMenus.STORAGE_TERMINAL.get());
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(StorageTerminalMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (!ClientStock.orderGrid()) {
            return helper.createUserErrorWithTooltip(Component.translatable("gui.servus_shuttles.terminal.order_needs"));
        }
        if (doTransfer) {
            boolean keepMode = StorageTerminalScreen.requestOrderGrid();
            ClientPacketDistributor.sendToServer(new OrderGridRecipePayload(recipe.id().identifier(), keepMode));
        }
        return null;
    }
}
