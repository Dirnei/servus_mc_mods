package io.github.dirnei.mc.servus.brewery.integration.jei;

import io.github.dirnei.mc.servus.brewery.beer.FermentingRecipe;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;

/** JEI category for the Fermenting Vat: the yeast, the wort, an arrow, the beer and the time in game days. */
final class FermentingRecipeCategory extends BreweryRecipeCategory<FermentingRecipe> {

    /** The amount shown; the whole tank ferments one to one. */
    private static final int SHOWN_AMOUNT = 1000;
    private static final double TICKS_PER_DAY = 24000.0;
    private static final int WORT_X = 1 + SLOT_STEP;

    FermentingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusBreweryJeiPlugin.FERMENTING, "gui.servus_brewery.jei.fermenting", ServusBreweryItems.FERMENTING_VAT.get(),
                WORT_X + SLOT_STEP + 4, 1, 1);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FermentingRecipe> holder, IFocusGroup focuses) {
        FermentingRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y).setStandardSlotBackground().addItemStacks(ServusRecipes.items(recipe.yeast()));
        IRecipeSlotBuilder wort = builder.addInputSlot(WORT_X, SLOT_Y).setStandardSlotBackground();
        for (Holder<Fluid> option : recipe.wort().fluids()) {
            wort.addFluidStack(option.value(), SHOWN_AMOUNT);
        }
        builder.addOutputSlot(outputX(), SLOT_Y).setStandardSlotBackground().addFluidStack(recipe.result(), SHOWN_AMOUNT);
    }

    @Override
    public void draw(RecipeHolder<FermentingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        super.draw(holder, recipeSlotsView, guiGraphics, mouseX, mouseY);
        String days = String.format("%.1f", holder.value().time() / TICKS_PER_DAY);
        text(guiGraphics, Component.translatable("gui.servus_brewery.jei.fermenting_time", days), 1, 0);
        text(guiGraphics, Component.translatable("gui.servus_brewery.jei.yeast_stays"), 90, 0);
    }
}
