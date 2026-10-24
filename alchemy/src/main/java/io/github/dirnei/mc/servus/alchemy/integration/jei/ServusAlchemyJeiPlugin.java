package io.github.dirnei.mc.servus.alchemy.integration.jei;

import io.github.dirnei.mc.servus.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinationRecipes;
import io.github.dirnei.mc.servus.alchemy.prelum.PressingRecipe;
import io.github.dirnei.mc.servus.alchemy.purifier.PurificationRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyComponents;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Shows the calcination, purification, coagulation, Athanor and pressing recipes in JEI and lists every vein's Seed.
 * Only loaded when JEI is present.
 */
@JeiPlugin
public final class ServusAlchemyJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "jei");

    public static final RecipeType<RecipeHolder<CalcinationRecipe>> CALCINATION =
            RecipeType.createRecipeHolderType(ServusAlchemyRecipes.CALCINATION_ID);
    public static final RecipeType<RecipeHolder<PurificationRecipe>> PURIFICATION =
            RecipeType.createRecipeHolderType(ServusAlchemyRecipes.PURIFICATION_ID);
    public static final RecipeType<RecipeHolder<CoagulationRecipe>> COAGULATION =
            RecipeType.createRecipeHolderType(ServusAlchemyRecipes.COAGULATION_ID);
    public static final RecipeType<RecipeHolder<AthanorRecipe>> ATHANOR =
            RecipeType.createRecipeHolderType(ServusAlchemyRecipes.ATHANOR_ID);
    public static final RecipeType<RecipeHolder<PressingRecipe>> PRESSING =
            RecipeType.createRecipeHolderType(ServusAlchemyRecipes.PRESSING_ID);

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    /** Each vein's Seed is its own entry. */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerFromDataComponentTypes(ServusAlchemyItems.SEED.get(), ServusAlchemyComponents.VEIN.get());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CalcinationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PurificationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CoagulationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AthanorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PressingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        registration.addRecipes(CALCINATION, ServusRecipes.all(minecraft.level, ServusAlchemyRecipes.CALCINATION.get()).stream()
                .filter(holder -> !CalcinationRecipes.burns(minecraft.level, holder).isEmpty())
                .toList());
        registration.addRecipes(PURIFICATION, ServusRecipes.all(minecraft.level, ServusAlchemyRecipes.PURIFICATION.get()));
        registration.addRecipes(COAGULATION, CoagulatorBlockEntity.sortedRecipes(minecraft.level));
        registration.addRecipes(ATHANOR, ServusRecipes.all(minecraft.level, ServusAlchemyRecipes.ATHANOR.get()));
        registration.addRecipes(PRESSING, ServusRecipes.all(minecraft.level, ServusAlchemyRecipes.PRESSING.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ServusAlchemyItems.CALCINER.get()), CALCINATION);
        registration.addRecipeCatalyst(new ItemStack(ServusAlchemyItems.PURIFIER.get()), PURIFICATION);
        registration.addRecipeCatalyst(new ItemStack(ServusAlchemyItems.COAGULATOR.get()), COAGULATION);
        registration.addRecipeCatalyst(new ItemStack(ServusAlchemyItems.ATHANOR.get()), ATHANOR);
        registration.addRecipeCatalyst(new ItemStack(ServusAlchemyItems.PRELUM.get()), PRESSING);
        registration.addCraftingStation(RecipeTypes.SMELTING, ServusAlchemyItems.SALAMANDER_FORGE.get());
    }
}
