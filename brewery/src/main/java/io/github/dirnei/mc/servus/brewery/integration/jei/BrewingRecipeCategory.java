package io.github.dirnei.mc.servus.brewery.integration.jei;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewKettle;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewingRecipe;
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
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** JEI category for the Brew Kettle: the counted grist and hops, the water, an arrow, the wort and the time. */
final class BrewingRecipeCategory extends BreweryRecipeCategory<BrewingRecipe> {

    private static final int WATER_X = 1 + BrewKettle.INPUTS * SLOT_STEP;

    BrewingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusBreweryJeiPlugin.BREWING, "gui.servus_brewery.jei.brewing", ServusBreweryItems.BREW_KETTLE.get(),
                WATER_X + SLOT_STEP + 4, 1, 1);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BrewingRecipe> holder, IFocusGroup focuses) {
        BrewingRecipe recipe = holder.value();
        int x = 1;
        for (SizedIngredient ingredient : recipe.ingredients()) {
            builder.addInputSlot(x, SLOT_Y).setStandardSlotBackground().addItemStacks(ServusRecipes.items(ingredient));
            x += SLOT_STEP;
        }
        IRecipeSlotBuilder water = builder.addInputSlot(WATER_X, SLOT_Y).setStandardSlotBackground();
        for (Holder<Fluid> option : recipe.fluid().ingredient().fluids()) {
            water.addFluidStack(option.value(), recipe.fluid().amount());
        }
        builder.addOutputSlot(outputX(), SLOT_Y).setStandardSlotBackground()
                .addFluidStack(recipe.result().fluid().value(), recipe.result().amount());
    }

    @Override
    public void draw(RecipeHolder<BrewingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        super.draw(holder, recipeSlotsView, guiGraphics, mouseX, mouseY);
        text(guiGraphics, seconds(holder.value().time()), 1, 0);
        text(guiGraphics, Component.translatable("gui.servus_brewery.jei.hot_brewhouse"), 36, 0);
    }
}
