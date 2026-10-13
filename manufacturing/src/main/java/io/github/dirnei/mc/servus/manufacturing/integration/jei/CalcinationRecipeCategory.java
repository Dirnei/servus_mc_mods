package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for calcination: the input, an arrow, the principles it gives and the time. */
public final class CalcinationRecipeCategory extends AlchemyRecipeCategory<CalcinationRecipe> {

    public CalcinationRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusManufacturingJeiPlugin.CALCINATION, "gui.servus_manufacturing.jei.calcination",
                ServusManufacturingItems.CALCINER.get(), 80, 26, 120, 36);
    }

    @Override
    int time(CalcinationRecipe recipe) {
        return recipe.time();
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
