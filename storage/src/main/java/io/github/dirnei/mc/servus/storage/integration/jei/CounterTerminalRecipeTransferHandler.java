package io.github.dirnei.mc.servus.storage.integration.jei;

import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.terminal.GridFillPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI plus button on crafting recipes while a Counter Terminal is open: asks
 * the server to fill the crafting grid, from the warehouse first and the
 * player inventory second. JEI's crafting view lists its nine input slots in
 * grid order, so slot view i becomes grid slot i.
 */
public final class CounterTerminalRecipeTransferHandler implements IRecipeTransferHandler<CounterTerminalMenu, RecipeHolder<CraftingRecipe>> {

    @Override
    public Class<? extends CounterTerminalMenu> getContainerClass() {
        return CounterTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<CounterTerminalMenu>> getMenuType() {
        return Optional.of(ServusStorageMenus.COUNTER_TERMINAL.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(CounterTerminalMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
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
            PacketDistributor.sendToServer(new GridFillPayload(grid, maxTransfer));
        }
        return null;
    }
}
