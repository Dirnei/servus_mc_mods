package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.recipe.CrushingRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.Arrays;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for crushing recipes: input, arrow, result, time. */
public final class CrushingRecipeCategory implements IRecipeCategory<RecipeHolder<CrushingRecipe>> {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 36;

    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public CrushingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ServusManufacturingItems.CRUSHER.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(40);
    }

    @Override
    public RecipeType<RecipeHolder<CrushingRecipe>> getRecipeType() {
        return ServusManufacturingJeiPlugin.CRUSHING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.servus_manufacturing.jei.crushing");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrushingRecipe> holder, IFocusGroup focuses) {
        CrushingRecipe recipe = holder.value();
        builder.addInputSlot(1, 5)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.asList(recipe.input().getItems()));
        builder.addOutputSlot(75, 5)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<CrushingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 32, 5);
        Component time = Component.literal(String.format("%.1f s", holder.value().time() / 20.0));
        guiGraphics.drawString(Minecraft.getInstance().font, time, 32, 25, 0x808080, false);
    }
}
