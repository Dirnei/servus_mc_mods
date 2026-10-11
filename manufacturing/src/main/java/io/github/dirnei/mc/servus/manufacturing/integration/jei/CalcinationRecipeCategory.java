package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for calcination: the input, an arrow, the principles it gives and the time. */
public final class CalcinationRecipeCategory implements IRecipeCategory<RecipeHolder<CalcinationRecipe>> {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 36;

    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public CalcinationRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ServusManufacturingItems.CALCINER.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(80);
    }

    @Override
    public RecipeType<RecipeHolder<CalcinationRecipe>> getRecipeType() {
        return ServusManufacturingJeiPlugin.CALCINATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.servus_manufacturing.jei.calcination");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CalcinationRecipe> holder, IFocusGroup focuses) {
        CalcinationRecipe recipe = holder.value();
        builder.addInputSlot(1, 5)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.asList(recipe.input().getItems()));
        int x = 60;
        for (Principle principle : Principle.values()) {
            int amount = recipe.output().get(principle);
            if (amount > 0) {
                builder.addOutputSlot(x, 5)
                        .setStandardSlotBackground()
                        .addFluidStack(PrincipleFluids.fluid(principle), amount);
                x += 20;
            }
        }
    }

    @Override
    public void draw(RecipeHolder<CalcinationRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 26, 5);
        Component time = Component.literal(String.format("%.1f s", holder.value().time() / 20.0));
        guiGraphics.text(Minecraft.getInstance().font, time, 26, 25, 0xFF808080, false);
    }
}
