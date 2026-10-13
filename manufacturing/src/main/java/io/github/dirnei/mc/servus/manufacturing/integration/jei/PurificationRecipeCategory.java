package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurificationRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;

/** JEI category for purification: the item and the water, an arrow, the Salt it makes and the time. */
public final class PurificationRecipeCategory extends AlchemyRecipeCategory<PurificationRecipe> {

    public PurificationRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusManufacturingJeiPlugin.PURIFICATION, "gui.servus_manufacturing.jei.purification",
                ServusManufacturingItems.PURIFIER.get(), 80, 46, 120, 36);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PurificationRecipe> holder, IFocusGroup focuses) {
        PurificationRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(recipe.input()));
        builder.addInputSlot(21, SLOT_Y)
                .setStandardSlotBackground()
                .addFluidStack(Fluids.WATER, recipe.water());
        builder.addOutputSlot(80, SLOT_Y)
                .setStandardSlotBackground()
                .addFluidStack(PrincipleFluids.fluid(Principle.SALT), recipe.salt());
    }
}
