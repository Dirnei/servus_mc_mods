package io.github.dirnei.mc.servus.alchemy.integration.jei;

import io.github.dirnei.mc.servus.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for calcination: the input, an arrow, the principles it gives and the time. */
public final class CalcinationRecipeCategory extends AlchemyRecipeCategory<CalcinationRecipe> {

    public CalcinationRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusAlchemyJeiPlugin.CALCINATION, "gui.servus_manufacturing.jei.calcination",
                ServusAlchemyItems.CALCINER.get(), 80, 26, 120, 36);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CalcinationRecipe> holder, IFocusGroup focuses) {
        CalcinationRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(recipe.input()));
        principleSlots(builder, true, recipe.output(), 60, 20);
    }
}
