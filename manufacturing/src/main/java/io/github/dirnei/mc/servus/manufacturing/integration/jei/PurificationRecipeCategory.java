package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurificationRecipe;
import net.minecraft.world.level.material.Fluids;
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

/** JEI category for purification: the item and the water, an arrow, the Salt it makes and the time. */
public final class PurificationRecipeCategory implements IRecipeCategory<RecipeHolder<PurificationRecipe>> {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 36;

    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public PurificationRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ServusManufacturingItems.PURIFIER.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(80);
    }

    @Override
    public RecipeType<RecipeHolder<PurificationRecipe>> getRecipeType() {
        return ServusManufacturingJeiPlugin.PURIFICATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.servus_manufacturing.jei.purification");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PurificationRecipe> holder, IFocusGroup focuses) {
        PurificationRecipe recipe = holder.value();
        builder.addInputSlot(1, 5)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.asList(recipe.input().getItems()));
        builder.addInputSlot(21, 5)
                .setStandardSlotBackground()
                .addFluidStack(Fluids.WATER, recipe.water());
        builder.addOutputSlot(80, 5)
                .setStandardSlotBackground()
                .addFluidStack(PrincipleFluids.fluid(Principle.SALT), recipe.salt());
    }

    @Override
    public void draw(RecipeHolder<PurificationRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 46, 5);
        Component time = Component.literal(String.format("%.1f s", holder.value().time() / 20.0));
        guiGraphics.text(Minecraft.getInstance().font, time, 46, 25, 0xFF808080, false);
    }
}
