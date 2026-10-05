package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.Arrays;
import java.util.List;
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
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** JEI category for assembling recipes: up to four inputs, an arrow, the result and the time. */
public final class AssemblingRecipeCategory implements IRecipeCategory<RecipeHolder<AssemblingRecipe>> {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 54;

    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public AssemblingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ServusManufacturingItems.ASSEMBLER.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(40);
    }

    @Override
    public RecipeType<RecipeHolder<AssemblingRecipe>> getRecipeType() {
        return ServusManufacturingJeiPlugin.ASSEMBLING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.servus_manufacturing.jei.assembling");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AssemblingRecipe> holder, IFocusGroup focuses) {
        AssemblingRecipe recipe = holder.value();
        List<SizedIngredient> ingredients = recipe.ingredients();
        for (int i = 0; i < Math.min(4, ingredients.size()); i++) {
            int x = 1 + (i % 2) * 18;
            int y = 1 + (i / 2) * 18;
            builder.addInputSlot(x, y)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.asList(ingredients.get(i).getItems()));
        }
        builder.addOutputSlot(95, 10)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<AssemblingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 52, 10);
        Component time = Component.literal(String.format("%.1f s", holder.value().time() / 20.0));
        guiGraphics.drawString(Minecraft.getInstance().font, time, 52, 30, 0x808080, false);
    }
}
