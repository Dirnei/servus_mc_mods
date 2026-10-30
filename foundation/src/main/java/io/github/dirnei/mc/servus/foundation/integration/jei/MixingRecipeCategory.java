package io.github.dirnei.mc.servus.foundation.integration.jei;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.mixer.MixerBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.mixer.MixingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** JEI category for mixing: up to four counted items in a row, the fluid if any, an arrow, the result and the work below. */
public final class MixingRecipeCategory implements IRecipeCategory<RecipeHolder<MixingRecipe>> {

    public static final RecipeType<RecipeHolder<MixingRecipe>> TYPE = RecipeType.createRecipeHolderType(MachineContent.MIXING_ID);

    private static final int SLOT_Y = 5;
    private static final int SLOT_STEP = 18;
    private static final int FLUID_X = 1 + MixerBlockEntity.INPUTS * SLOT_STEP;
    private static final int ARROW_X = FLUID_X + SLOT_STEP + 4;
    private static final int OUTPUT_X = ARROW_X + 30;
    private static final int TEXT_Y = 27;
    private static final int TEXT_COLOR = 0xFF808080;
    private static final int ARROW_TICKS = 100;

    private final Component title = Component.translatable("gui.servus_foundation.jei.mixing");
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public MixingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(MachineContent.MIXER_ITEM.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(ARROW_TICKS);
    }

    @Override
    public RecipeType<RecipeHolder<MixingRecipe>> getRecipeType() {
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
        return OUTPUT_X + SLOT_STEP + 1;
    }

    @Override
    public int getHeight() {
        return 38;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MixingRecipe> holder, IFocusGroup focuses) {
        MixingRecipe recipe = holder.value();
        List<SizedIngredient> ingredients = recipe.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            builder.addInputSlot(1 + i * SLOT_STEP, SLOT_Y)
                    .setStandardSlotBackground()
                    .addItemStacks(ServusRecipes.items(ingredients.get(i)));
        }
        recipe.fluid().ifPresent(fluid -> {
            var slot = builder.addInputSlot(FLUID_X, SLOT_Y).setStandardSlotBackground();
            for (Holder<Fluid> option : fluid.ingredient().fluids()) {
                slot.addFluidStack(option.value(), fluid.amount());
            }
        });
        builder.addOutputSlot(OUTPUT_X, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<MixingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y);
        guiGraphics.text(Minecraft.getInstance().font, Component.translatable("gui.servus_foundation.jei.mixing.work", holder.value().work()),
                1, TEXT_Y, TEXT_COLOR, false);
    }
}
