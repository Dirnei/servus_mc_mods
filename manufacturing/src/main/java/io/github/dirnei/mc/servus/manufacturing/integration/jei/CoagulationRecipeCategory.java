package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for coagulation: the principles and the item it takes, an arrow, the result and the time. */
public final class CoagulationRecipeCategory extends AlchemyRecipeCategory<CoagulationRecipe> {

    public CoagulationRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusManufacturingJeiPlugin.COAGULATION, "gui.servus_manufacturing.jei.coagulation",
                ServusManufacturingItems.COAGULATOR.get(), 40, 80, 130, 36);
    }

    @Override
    int time(CoagulationRecipe recipe) {
        return recipe.time();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CoagulationRecipe> holder, IFocusGroup focuses) {
        CoagulationRecipe recipe = holder.value();
        int itemX = principleSlots(builder, false, recipe.input(), 1, 18);
        recipe.ingredient().ifPresent(sized -> builder.addInputSlot(itemX, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(sized)));
        builder.addOutputSlot(110, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }
}
