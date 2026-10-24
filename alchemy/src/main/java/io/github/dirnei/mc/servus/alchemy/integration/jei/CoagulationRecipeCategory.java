package io.github.dirnei.mc.servus.alchemy.integration.jei;

import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.client.CoagulatorScreen;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorSeeds;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/** JEI category for coagulation: the principles and the item it takes, an arrow, the result and the time, and the seeds below. */
public final class CoagulationRecipeCategory extends AlchemyRecipeCategory<CoagulationRecipe> {

    /** The row of the seed slot, under the principles. */
    private static final int SEED_Y = 30;

    public CoagulationRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusAlchemyJeiPlugin.COAGULATION, "gui.servus_alchemy.jei.coagulation",
                ServusAlchemyItems.COAGULATOR.get(), 40, 80, 130, 50);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CoagulationRecipe> holder, IFocusGroup focuses) {
        CoagulationRecipe recipe = holder.value();
        int itemX = principleSlots(builder, false, recipe.input(), 1, 18);
        recipe.ingredient().ifPresent(sized -> builder.addInputSlot(itemX, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(sized)));
        List<ItemStack> seeds = seeds(holder);
        builder.addOutputSlot(110, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStack(seeds.stream().allMatch(recipe::selfSeed) ? recipe.resultFrom(recipe.result()) : recipe.result())
                .addRichTooltipCallback((view, tooltip) -> seeds.stream().filter(recipe::selfSeed).findFirst()
                        .ifPresent(self -> tooltip.add(Component.translatable("gui.servus_alchemy.jei.coagulation.self_seed",
                                recipe.resultFrom(self).getCount()))));
        builder.addInputSlot(1, SEED_Y)
                .setStandardSlotBackground()
                .addItemStacks(seeds)
                .addRichTooltipCallback((view, tooltip) -> view.getDisplayedItemStack()
                        .ifPresent(seed -> tooltip.add(CoagulatorScreen.seedUse(recipe, seed))));
    }

    /** The seeds that really grow the recipe, after duplicates were settled; the recipe's own seed ingredient without a level. */
    private static List<ItemStack> seeds(RecipeHolder<CoagulationRecipe> holder) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return ServusRecipes.items(holder.value().seedIngredient());
        }
        return CoagulatorSeeds.seedsOf(level, holder.id().identifier()).stream().map(ItemStack::new).toList();
    }

    @Override
    public void draw(RecipeHolder<CoagulationRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        super.draw(holder, recipeSlotsView, guiGraphics, mouseX, mouseY);
        text(guiGraphics, Component.translatable("gui.servus_alchemy.jei.coagulation.seed"), 21, SEED_Y + 5);
    }
}
