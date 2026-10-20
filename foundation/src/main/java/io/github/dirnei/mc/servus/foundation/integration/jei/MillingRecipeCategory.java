package io.github.dirnei.mc.servus.foundation.integration.jei;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for milling: the counted input, an arrow, the result and the work below the arrow. */
public final class MillingRecipeCategory implements IRecipeCategory<RecipeHolder<MillingRecipe>> {

    public static final RecipeType<RecipeHolder<MillingRecipe>> TYPE = RecipeType.createRecipeHolderType(MachineContent.MILLING_ID);

    private static final int SLOT_Y = 5;
    private static final int ARROW_X = 26;
    private static final int TEXT_Y = 25;
    private static final int TEXT_COLOR = 0xFF808080;
    private static final int ARROW_TICKS = 100;

    private final Component title = Component.translatable("gui.servus_foundation.jei.milling");
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public MillingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(MachineContent.MILL_ITEM.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(ARROW_TICKS);
    }

    @Override
    public RecipeType<RecipeHolder<MillingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 90;
    }

    @Override
    public int getHeight() {
        return 36;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MillingRecipe> holder, IFocusGroup focuses) {
        MillingRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(recipe.input()));
        builder.addOutputSlot(60, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<MillingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y);
        guiGraphics.text(Minecraft.getInstance().font, Component.translatable("gui.servus_foundation.jei.milling.work", holder.value().work()),
                ARROW_X, TEXT_Y, TEXT_COLOR, false);
    }
}
