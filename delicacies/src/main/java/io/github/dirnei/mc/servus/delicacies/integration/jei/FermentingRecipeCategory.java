package io.github.dirnei.mc.servus.delicacies.integration.jei;

import io.github.dirnei.mc.servus.delicacies.crock.Fermentation;
import io.github.dirnei.mc.servus.delicacies.crock.FermentingRecipe;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.network.chat.Component;

/** JEI category for the Fermenting Crock: the items, the fluid, the result and the time in in-game days. */
final class FermentingRecipeCategory extends VesselRecipeCategory<FermentingRecipe> {

    FermentingRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, ServusDelicaciesJeiPlugin.FERMENTING, "gui.servus_delicacies.jei.fermenting",
                ServusDelicaciesItems.FERMENTING_CROCK.get(), true);
    }

    @Override
    protected Component timeText(FermentingRecipe recipe) {
        double days = Fermentation.days(recipe.time());
        String shown = days == Math.rint(days) ? String.valueOf((long) days) : String.format("%.1f", days);
        return Component.translatable("gui.servus_delicacies.jei.days", shown);
    }

    @Override
    protected Component note() {
        return Component.translatable("gui.servus_delicacies.jei.sealed");
    }
}
