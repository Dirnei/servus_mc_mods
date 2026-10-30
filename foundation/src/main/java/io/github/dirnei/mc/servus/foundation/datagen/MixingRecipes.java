package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.mixer.MixingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Datagen of the vanilla mixing recipes Foundation ships, with the items vanilla's own recipes use. No two of them match
 * the same contents: their items differ in kinds or in the number of slots they fill.
 */
final class MixingRecipes {

    private static final int QUICK = 100;
    private static final int SLOW = 200;
    /** A bottle's worth: what a player pours on Dirt to make Mud in vanilla. */
    private static final int MUD_WATER = 250;

    private MixingRecipes() {
    }

    static void recipes(RecipeOutput output) {
        mix(output, "mud", List.of(sized(Items.DIRT, 1)), Optional.of(SizedFluidIngredient.of(Fluids.WATER, MUD_WATER)),
                Items.MUD, 1, QUICK);
        mix(output, "coarse_dirt", List.of(sized(Items.DIRT, 2), sized(Items.GRAVEL, 2)), Optional.empty(), Items.COARSE_DIRT, 4, QUICK);
        for (DyeColor color : DyeColor.values()) {
            String name = color.getSerializedName();
            mix(output, name + "_concrete_powder", List.of(sized(Items.SAND, 4), sized(Items.GRAVEL, 4), sized(vanilla(name + "_dye"), 1)),
                    Optional.empty(), vanilla(name + "_concrete_powder"), 8, SLOW);
        }
    }

    private static void mix(RecipeOutput output, String name, List<SizedIngredient> ingredients, Optional<SizedFluidIngredient> fluid,
                            ItemLike result, int count, int work) {
        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "mixing/" + name)),
                new MixingRecipe(ingredients, fluid, new ItemStackTemplate(result.asItem(), count), work), null);
    }

    private static SizedIngredient sized(ItemLike item, int count) {
        return new SizedIngredient(Ingredient.of(item), count);
    }

    private static Item vanilla(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }
}
