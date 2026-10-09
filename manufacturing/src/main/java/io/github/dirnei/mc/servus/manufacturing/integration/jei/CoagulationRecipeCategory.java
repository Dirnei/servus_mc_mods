package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for coagulation: the principles and item it takes, an arrow, the result and the time. */
public final class CoagulationRecipeCategory implements IRecipeCategory<RecipeHolder<CoagulationRecipe>> {

    private static final int WIDTH = 130;
    private static final int HEIGHT = 36;

    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public CoagulationRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ServusManufacturingItems.COAGULATOR.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(40);
    }

    @Override
    public RecipeType<RecipeHolder<CoagulationRecipe>> getRecipeType() {
        return ServusManufacturingJeiPlugin.COAGULATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.servus_manufacturing.jei.coagulation");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CoagulationRecipe> holder, IFocusGroup focuses) {
        CoagulationRecipe recipe = holder.value();
        int x = 1;
        for (Principle principle : Principle.values()) {
            int amount = recipe.input().get(principle);
            if (amount > 0) {
                builder.addInputSlot(x, 5)
                        .setStandardSlotBackground()
                        .addFluidStack(PrincipleFluids.fluid(principle), amount);
                x += 18;
            }
        }
        int itemX = x;
        recipe.ingredient().ifPresent(sized -> builder.addInputSlot(itemX, 5)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.asList(sized.getItems())));
        builder.addOutputSlot(110, 5)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<CoagulationRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 80, 5);
        Component time = Component.literal(String.format("%.1f s", holder.value().time() / 20.0));
        guiGraphics.drawString(Minecraft.getInstance().font, time, 80, 25, 0x808080, false);
    }
}
