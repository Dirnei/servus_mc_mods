package io.github.dirnei.mc.servus.alchemy.integration.jei;

import io.github.dirnei.mc.servus.alchemy.prelum.PressingRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for pressing: the counted input, an arrow, the result and the time. */
public final class PressingRecipeCategory extends AlchemyRecipeCategory<PressingRecipe> {

    public PressingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusAlchemyJeiPlugin.PRESSING, "gui.servus_alchemy.jei.pressing",
                ServusAlchemyItems.PRELUM.get(), 200, 26, 90, 36);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PressingRecipe> holder, IFocusGroup focuses) {
        PressingRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(recipe.input()));
        builder.addOutputSlot(60, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }
}
