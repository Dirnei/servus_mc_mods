package io.github.dirnei.mc.servus.shuttles.integration.jei;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import io.github.dirnei.mc.servus.shuttles.client.StorageTerminalScreen;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.terminal.GridFillPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI plus button on crafting recipes while a Storage Terminal is open: opens
 * the crafting popup and asks the server to fill the crafting grid from the
 * player inventory. JEI's crafting view lists its nine input slots in grid
 * order, so slot view i becomes grid slot i.
 */
public final class StorageTerminalRecipeTransferHandler implements IRecipeTransferHandler<StorageTerminalMenu, RecipeHolder<CraftingRecipe>> {

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
        if (doTransfer) {
            List<List<ItemStack>> grid = new ArrayList<>();
            for (IRecipeSlotView view : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
                if (grid.size() == 9) {
                    break;
                }
                grid.add(view.getItemStacks().map(ItemStack::copy).toList());
            }
            while (grid.size() < 9) {
                grid.add(List.of());
            }
            // The crafting popup shows the filled grid: open it now, or when the terminal returns.
            StorageTerminalScreen.requestCraftPopup();
            ClientPacketDistributor.sendToServer(new GridFillPayload(grid, maxTransfer));
        }
        return null;
    }
}
