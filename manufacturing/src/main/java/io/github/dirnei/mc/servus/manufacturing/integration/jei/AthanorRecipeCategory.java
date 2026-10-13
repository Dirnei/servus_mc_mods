package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for Athanor crafts: the item, the principles, an arrow, the result, the time and the stage. */
public final class AthanorRecipeCategory extends AlchemyRecipeCategory<AthanorRecipe> {

    private static final String[] STAGES = {"nigredo", "albedo", "citrinitas", "rubedo"};

    public AthanorRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusManufacturingJeiPlugin.ATHANOR, "gui.servus_manufacturing.jei.athanor",
                ServusManufacturingItems.ATHANOR.get(), 100, 90, 140, 46);
    }

    @Override
    int time(AthanorRecipe recipe) {
        return recipe.time();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AthanorRecipe> holder, IFocusGroup focuses) {
        AthanorRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(recipe.input()));
        principleSlots(builder, false, recipe.principles(), 21, 18);
        builder.addOutputSlot(120, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<AthanorRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        super.draw(holder, recipeSlotsView, guiGraphics, mouseX, mouseY);
        int tier = Math.max(0, Math.min(STAGES.length - 1, holder.value().tier()));
        Component stage = Component.translatable("gui.servus_manufacturing.jei.athanor.tier",
                Component.translatable("gui.servus_manufacturing.athanor.stage." + STAGES[tier]));
        text(guiGraphics, stage, 1, 35);
    }
}
