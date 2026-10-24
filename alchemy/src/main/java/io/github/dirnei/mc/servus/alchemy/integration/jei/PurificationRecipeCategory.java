package io.github.dirnei.mc.servus.alchemy.integration.jei;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.purifier.PurificationRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;

/** JEI category for purification: the item and the water, an arrow, the Salt it makes if any, the residue if any and the time. */
public final class PurificationRecipeCategory extends AlchemyRecipeCategory<PurificationRecipe> {

    public PurificationRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusAlchemyJeiPlugin.PURIFICATION, "gui.servus_alchemy.jei.purification",
                ServusAlchemyItems.PURIFIER.get(), 80, 46, 120, 36);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PurificationRecipe> holder, IFocusGroup focuses) {
        PurificationRecipe recipe = holder.value();
        builder.addInputSlot(1, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(ServusRecipes.items(recipe.input()));
        builder.addInputSlot(21, SLOT_Y)
                .setStandardSlotBackground()
                .addFluidStack(Fluids.WATER, recipe.water());
        int outputX = 80;
        if (recipe.salt() > 0) {
            builder.addOutputSlot(outputX, SLOT_Y)
                    .setStandardSlotBackground()
                    .addFluidStack(PrincipleFluids.fluid(Principle.SALT), recipe.salt());
            outputX += 20;
        }
        ItemStack residue = recipe.residue();
        if (!residue.isEmpty()) {
            builder.addOutputSlot(outputX, SLOT_Y)
                    .setStandardSlotBackground()
                    .addItemStack(residue);
        }
    }
}
