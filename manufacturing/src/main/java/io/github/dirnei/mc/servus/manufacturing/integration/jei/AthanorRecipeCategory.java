package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorRecipe;
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

/** JEI category for Athanor crafts: the item, the principles, an arrow, the result, the time and the stage. */
public final class AthanorRecipeCategory implements IRecipeCategory<RecipeHolder<AthanorRecipe>> {

    private static final int WIDTH = 140;
    private static final int HEIGHT = 46;
    private static final String[] STAGES = {"nigredo", "albedo", "citrinitas", "rubedo"};

    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public AthanorRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ServusManufacturingItems.ATHANOR.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(100);
    }

    @Override
    public RecipeType<RecipeHolder<AthanorRecipe>> getRecipeType() {
        return ServusManufacturingJeiPlugin.ATHANOR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.servus_manufacturing.jei.athanor");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AthanorRecipe> holder, IFocusGroup focuses) {
        AthanorRecipe recipe = holder.value();
        builder.addInputSlot(1, 5)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.asList(recipe.input().getItems()));
        int x = 21;
        for (Principle principle : Principle.values()) {
            int amount = recipe.principles().get(principle);
            if (amount > 0) {
                builder.addInputSlot(x, 5)
                        .setStandardSlotBackground()
                        .addFluidStack(PrincipleFluids.fluid(principle), amount);
                x += 18;
            }
        }
        builder.addOutputSlot(120, 5)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<AthanorRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 90, 5);
        AthanorRecipe recipe = holder.value();
        Component time = Component.literal(String.format("%.1f s", recipe.time() / 20.0));
        int tier = Math.max(0, Math.min(STAGES.length - 1, recipe.tier()));
        Component stage = Component.translatable("gui.servus_manufacturing.jei.athanor.tier",
                Component.translatable("gui.servus_manufacturing.athanor.stage." + STAGES[tier]));
        guiGraphics.text(Minecraft.getInstance().font, time, 90, 25, 0xFF808080, false);
        guiGraphics.text(Minecraft.getInstance().font, stage, 1, 35, 0xFF808080, false);
    }
}
