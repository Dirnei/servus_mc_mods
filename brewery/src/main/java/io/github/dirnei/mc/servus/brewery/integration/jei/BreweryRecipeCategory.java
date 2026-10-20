package io.github.dirnei.mc.servus.brewery.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

/** What the Brewhouse JEI categories share: icon, title, size, the slot row, an animated arrow and grey text. */
abstract class BreweryRecipeCategory<R extends Recipe<?>> implements IRecipeCategory<RecipeHolder<R>> {

    static final int SLOT_Y = 5;
    static final int SLOT_STEP = 18;
    static final int TEXT_Y = 25;
    static final int TEXT_STEP = 10;
    private static final int TEXT_COLOR = 0xFF808080;
    private static final int ARROW_WIDTH = 30;

    private final RecipeType<RecipeHolder<R>> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;
    private final int arrowX;
    private final int width;
    private final int height;

    BreweryRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<R>> type, String titleKey, ItemLike icon, int arrowX,
                          int outputs, int textLines) {
        this.type = type;
        this.title = Component.translatable(titleKey);
        this.icon = guiHelper.createDrawableItemLike(icon);
        this.arrow = guiHelper.createAnimatedRecipeArrow(40);
        this.arrowX = arrowX;
        this.width = Math.max(outputX() + outputs * SLOT_STEP + 2, 170);
        this.height = TEXT_Y + textLines * TEXT_STEP + 2;
    }

    /** The x of the first output slot, right of the arrow. */
    final int outputX() {
        return arrowX + ARROW_WIDTH;
    }

    @Override
    public final RecipeType<RecipeHolder<R>> getRecipeType() {
        return type;
    }

    @Override
    public final Component getTitle() {
        return title;
    }

    @Override
    public final IDrawable getIcon() {
        return icon;
    }

    @Override
    public final int getWidth() {
        return width;
    }

    @Override
    public final int getHeight() {
        return height;
    }

    @Override
    public void draw(RecipeHolder<R> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, arrowX, SLOT_Y);
    }

    static Component seconds(int ticks) {
        return Component.literal(String.format("%.1f s", ticks / 20.0));
    }

    static void text(GuiGraphicsExtractor guiGraphics, Component text, int x, int line) {
        guiGraphics.text(Minecraft.getInstance().font, text, x, TEXT_Y + line * TEXT_STEP, TEXT_COLOR, false);
    }
}
