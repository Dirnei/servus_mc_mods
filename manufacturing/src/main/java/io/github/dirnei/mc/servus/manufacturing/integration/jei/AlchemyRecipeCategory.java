package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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

/** What the alchemy JEI categories share: icon, title and size, an animated arrow with the recipe time below it, principle slots. */
abstract class AlchemyRecipeCategory<R extends Recipe<?>> implements IRecipeCategory<RecipeHolder<R>> {

    /** The row of the slots and the arrow. */
    static final int SLOT_Y = 5;
    private static final int TIME_Y = 25;
    private static final int TEXT_COLOR = 0xFF808080;

    private final RecipeType<RecipeHolder<R>> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;
    private final int arrowX;
    private final int width;
    private final int height;

    AlchemyRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<R>> type, String titleKey, ItemLike machine, int arrowTicks,
                          int arrowX, int width, int height) {
        this.type = type;
        this.title = Component.translatable(titleKey);
        this.icon = guiHelper.createDrawableItemLike(machine);
        this.arrow = guiHelper.createAnimatedRecipeArrow(arrowTicks);
        this.arrowX = arrowX;
        this.width = width;
        this.height = height;
    }

    /** Ticks per cycle of the recipe, shown in seconds under the arrow. */
    abstract int time(R recipe);

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
        text(guiGraphics, Component.literal(String.format("%.1f s", time(holder.value()) / 20.0)), arrowX, TIME_Y);
    }

    static void text(GuiGraphicsExtractor guiGraphics, Component text, int x, int y) {
        guiGraphics.text(Minecraft.getInstance().font, text, x, y, TEXT_COLOR, false);
    }

    /** One slot per principle that is not zero, from {@code x} on, {@code step} apart; returns the x after the last. */
    static int principleSlots(IRecipeLayoutBuilder builder, boolean output, Principles principles, int x, int step) {
        for (Principle principle : Principle.values()) {
            int amount = principles.get(principle);
            if (amount > 0) {
                IRecipeSlotBuilder slot = output ? builder.addOutputSlot(x, SLOT_Y) : builder.addInputSlot(x, SLOT_Y);
                slot.setStandardSlotBackground().addFluidStack(PrincipleFluids.fluid(principle), amount);
                x += step;
            }
        }
        return x;
    }
}
