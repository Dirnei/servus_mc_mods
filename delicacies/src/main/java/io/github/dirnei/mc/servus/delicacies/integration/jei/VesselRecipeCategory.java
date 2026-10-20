package io.github.dirnei.mc.servus.delicacies.integration.jei;

import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

/**
 * JEI category for pot or pan recipes: the counted items, for the Pot the fluid, an arrow, the result, the time and
 * the reminder that it needs a burning stove.
 */
final class VesselRecipeCategory<R extends VesselRecipe> extends KitchenRecipeCategory<R> {

    private static final int ITEMS_END = 1 + VesselBlockEntity.INPUTS * SLOT_STEP;

    private final boolean withFluid;

    VesselRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<R>> type, String titleKey, ItemLike vessel, boolean withFluid) {
        super(guiHelper, type, titleKey, vessel, ITEMS_END + (withFluid ? SLOT_STEP : 0) + 4);
        this.withFluid = withFluid;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<R> holder, IFocusGroup focuses) {
        R recipe = holder.value();
        ingredientSlots(builder, recipe.ingredients());
        if (withFluid) {
            recipe.fluid().ifPresent(fluid -> {
                var slot = builder.addInputSlot(ITEMS_END, SLOT_Y).setStandardSlotBackground();
                for (Holder<Fluid> option : fluid.ingredient().fluids()) {
                    slot.addFluidStack(option.value(), fluid.amount());
                }
            });
        }
        builder.addOutputSlot(outputX(), SLOT_Y).setStandardSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<R> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        super.draw(holder, recipeSlotsView, guiGraphics, mouseX, mouseY);
        text(guiGraphics, Component.literal(String.format("%.1f s", holder.value().time() / 20.0)), 1, TEXT_Y);
        text(guiGraphics, Component.translatable("gui.servus_delicacies.jei.hot_stove"), 36, TEXT_Y);
    }
}
