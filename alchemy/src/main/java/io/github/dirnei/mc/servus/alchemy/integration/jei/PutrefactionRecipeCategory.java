package io.github.dirnei.mc.servus.alchemy.integration.jei;

import io.github.dirnei.mc.servus.alchemy.AlchemyDataMaps;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.venter.PutrefactionRecipe;
import io.github.dirnei.mc.servus.alchemy.venter.PutrefactionWork;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category for putrefaction: the counted rot, an arrow, the result, the bedding that warms the bed, and the warm and cold times. */
public final class PutrefactionRecipeCategory extends AlchemyRecipeCategory<PutrefactionRecipe> {

    private static final int ARROW_X = 26;
    private static final int BEDDING_X = 100;
    private static final int TIMES_Y = 25;
    private static final int TICKS_PER_SECOND = 20;

    public PutrefactionRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusAlchemyJeiPlugin.PUTREFACTION, "gui.servus_alchemy.jei.putrefaction",
                ServusAlchemyItems.CUCURBIT.get(), 200, ARROW_X, 120, 36);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PutrefactionRecipe> holder, IFocusGroup focuses) {
        PutrefactionRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(recipe.input()));
        builder.addOutputSlot(60, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, BEDDING_X, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(beddingItems());
    }

    private static List<ItemStack> beddingItems() {
        return BuiltInRegistries.ITEM.getDataMap(AlchemyDataMaps.BEDDING).keySet().stream()
                .map(key -> new ItemStack(BuiltInRegistries.ITEM.getValueOrThrow(key)))
                .toList();
    }

    @Override
    public void draw(RecipeHolder<PutrefactionRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        PutrefactionRecipe recipe = holder.value();
        drawArrow(guiGraphics);
        int warm = PutrefactionWork.ticksAt(recipe.time(), recipe.coldTime(), 1000) / TICKS_PER_SECOND;
        int cold = PutrefactionWork.ticksAt(recipe.time(), recipe.coldTime(), 0) / TICKS_PER_SECOND;
        text(guiGraphics, Component.translatable("gui.servus_alchemy.jei.putrefaction.times", warm, cold), 1, TIMES_Y);
    }
}
