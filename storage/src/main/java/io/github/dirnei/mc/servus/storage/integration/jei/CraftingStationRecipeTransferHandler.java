package io.github.dirnei.mc.servus.storage.integration.jei;

import io.github.dirnei.mc.servus.storage.crafting.OpenStationEditorPayload;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
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
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI plus button on crafting recipes while a Crafting Station GUI is open:
 * asks the server to open the station's recipe editor prefilled with the recipe.
 */
public final class CraftingStationRecipeTransferHandler implements IRecipeTransferHandler<CraftingStationMenu, RecipeHolder<CraftingRecipe>> {

    @Override
    public Class<? extends CraftingStationMenu> getContainerClass() {
        return CraftingStationMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingStationMenu>> getMenuType() {
        return Optional.of(ServusStorageMenus.CRAFTING_STATION.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(CraftingStationMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            PacketDistributor.sendToServer(new OpenStationEditorPayload(recipe.id()));
        }
        return null;
    }
}
