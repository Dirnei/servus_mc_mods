package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.menu.AssemblerMenu;
import io.github.dirnei.mc.servus.manufacturing.network.ConfigureRecipePayload;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI plus button on crafting and assembling recipes while an assembler GUI is
 * open: asks the server to add the recipe to the machine.
 */
public final class AssemblerRecipeTransferHandler<R extends Recipe<?>> implements IRecipeTransferHandler<AssemblerMenu, RecipeHolder<R>> {

    private final RecipeType<RecipeHolder<R>> recipeType;

    public AssemblerRecipeTransferHandler(RecipeType<RecipeHolder<R>> recipeType) {
        this.recipeType = recipeType;
    }

    @Override
    public Class<? extends AssemblerMenu> getContainerClass() {
        return AssemblerMenu.class;
    }

    @Override
    public Optional<MenuType<AssemblerMenu>> getMenuType() {
        return Optional.of(ServusManufacturingMenus.ASSEMBLER.get());
    }

    @Override
    public RecipeType<RecipeHolder<R>> getRecipeType() {
        return recipeType;
    }

    @Override
    public IRecipeTransferError transferRecipe(AssemblerMenu menu, RecipeHolder<R> recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            PacketDistributor.sendToServer(new ConfigureRecipePayload(recipe.id()));
        }
        return null;
    }
}
