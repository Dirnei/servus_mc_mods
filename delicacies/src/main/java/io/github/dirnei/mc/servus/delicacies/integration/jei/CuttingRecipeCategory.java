package io.github.dirnei.mc.servus.delicacies.integration.jei;

import io.github.dirnei.mc.servus.delicacies.board.CuttingRecipe;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for cutting: the item, the tool that cuts it, an arrow and the pieces. */
final class CuttingRecipeCategory extends KitchenRecipeCategory<CuttingRecipe> {

    CuttingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusDelicaciesJeiPlugin.CUTTING, "gui.servus_delicacies.jei.cutting",
                ServusDelicaciesItems.CUTTING_BOARD.get(), 1 + 2 * SLOT_STEP + 4);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CuttingRecipe> holder, IFocusGroup focuses) {
        CuttingRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y).setStandardSlotBackground().addItemStacks(ServusRecipes.items(recipe.ingredient()));
        builder.addInputSlot(1 + SLOT_STEP, SLOT_Y).setStandardSlotBackground().addItemStacks(ServusRecipes.items(recipe.tool()));
        builder.addOutputSlot(outputX(), SLOT_Y).setStandardSlotBackground().addItemStack(recipe.result());
    }
}
