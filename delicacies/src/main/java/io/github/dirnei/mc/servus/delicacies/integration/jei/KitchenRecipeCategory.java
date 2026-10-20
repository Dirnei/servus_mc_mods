package io.github.dirnei.mc.servus.delicacies.integration.jei;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** What the kitchen JEI categories share: icon, title, size, the slot row and an animated arrow. */
abstract class KitchenRecipeCategory<R extends Recipe<?>> implements IRecipeCategory<RecipeHolder<R>> {

    static final int SLOT_Y = 5;
    static final int SLOT_STEP = 18;
    static final int TEXT_Y = 25;
    private static final int TEXT_COLOR = 0xFF808080;
    private static final int ARROW_WIDTH = 30;

    private final RecipeType<RecipeHolder<R>> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;
    private final int arrowX;

    KitchenRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<R>> type, String titleKey, ItemLike icon, int arrowX) {
        this.type = type;
        this.title = Component.translatable(titleKey);
        this.icon = guiHelper.createDrawableItemLike(icon);
        this.arrow = guiHelper.createAnimatedRecipeArrow(40);
        this.arrowX = arrowX;
    }

    /** The x of the output slot, right of the arrow. */
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
        return outputX() + SLOT_STEP + 2;
    }

    @Override
    public final int getHeight() {
        return 36;
    }

    @Override
    public void draw(RecipeHolder<R> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, arrowX, SLOT_Y);
    }

    /** One input slot per counted ingredient, from x 1 on; returns the x after the last. */
    static int ingredientSlots(IRecipeLayoutBuilder builder, List<SizedIngredient> ingredients) {
        int x = 1;
        for (SizedIngredient ingredient : ingredients) {
            builder.addInputSlot(x, SLOT_Y).setStandardSlotBackground()
                    .addItemStacks(ServusRecipes.items(ingredient));
            x += SLOT_STEP;
        }
        return x;
    }

    static void text(GuiGraphicsExtractor guiGraphics, Component text, int x, int y) {
        guiGraphics.text(Minecraft.getInstance().font, text, x, y, TEXT_COLOR, false);
    }
}
