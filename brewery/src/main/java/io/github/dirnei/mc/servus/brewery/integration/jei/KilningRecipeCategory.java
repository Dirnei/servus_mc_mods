package io.github.dirnei.mc.servus.brewery.integration.jei;

import io.github.dirnei.mc.servus.brewery.brewhouse.KilningRecipe;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for the Malt Kiln: the green malt, an arrow, the pale result and, for dark mode, the dark result. */
final class KilningRecipeCategory extends BreweryRecipeCategory<KilningRecipe> {

    private static final int DARK_GAP = 8;

    KilningRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusBreweryJeiPlugin.KILNING, "gui.servus_brewery.jei.kilning", ServusBreweryItems.BREWHOUSE_BRICKS.get(),
                1 + SLOT_STEP + 4, 2, 2);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<KilningRecipe> holder, IFocusGroup focuses) {
        KilningRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y).setStandardSlotBackground().addItemStacks(ServusRecipes.items(recipe.input()));
        builder.addOutputSlot(outputX(), SLOT_Y).setStandardSlotBackground().addItemStack(recipe.result(1, false));
        recipe.dark().ifPresent(dark -> builder.addOutputSlot(outputX() + SLOT_STEP + DARK_GAP, SLOT_Y).setStandardSlotBackground()
                .addItemStack(recipe.result(1, true)));
    }

    @Override
    public void draw(RecipeHolder<KilningRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        super.draw(holder, recipeSlotsView, guiGraphics, mouseX, mouseY);
        KilningRecipe recipe = holder.value();
        text(guiGraphics, seconds(recipe.time()), 1, 0);
        text(guiGraphics, Component.translatable("gui.servus_brewery.jei.hot_brewhouse"), 36, 0);
        recipe.dark().ifPresent(dark -> text(guiGraphics, Component.translatable("gui.servus_brewery.jei.dark",
                recipe.result(1, true).getHoverName(), String.format("%.1f", dark.time() / 20.0)), 1, 1));
    }
}
