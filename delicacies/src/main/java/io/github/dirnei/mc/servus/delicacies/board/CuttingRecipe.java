package io.github.dirnei.mc.servus.delicacies.board;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A Cutting Board recipe ({@code servus_delicacies:cutting}): one item cut by a tool into a counted result. Never in
 * the recipe book.
 *
 * @param ingredient     what one cut takes, one item
 * @param tool           what cuts it, usually {@code #c:tools/knife}
 * @param resultTemplate the pieces one cut makes
 */
public record CuttingRecipe(Ingredient ingredient, Ingredient tool, ItemStackTemplate resultTemplate) implements Recipe<CuttingInput> {

    public static final MapCodec<CuttingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(CuttingRecipe::ingredient),
            Ingredient.CODEC.fieldOf("tool").forGetter(CuttingRecipe::tool),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CuttingRecipe::resultTemplate)
    ).apply(instance, CuttingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CuttingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CuttingRecipe::ingredient,
            Ingredient.CONTENTS_STREAM_CODEC, CuttingRecipe::tool,
            ItemStackTemplate.STREAM_CODEC, CuttingRecipe::resultTemplate,
            CuttingRecipe::new);

    public static final RecipeSerializer<CuttingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** A new stack of the pieces. */
    public ItemStack result() {
        return resultTemplate.create();
    }

    @Override
    public boolean matches(CuttingInput input, Level level) {
        return !input.item().isEmpty() && ingredient.test(input.item()) && tool.test(input.tool());
    }

    @Override
    public ItemStack assemble(CuttingInput input) {
        return result();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<CuttingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<CuttingRecipe> getType() {
        return ServusDelicaciesRecipes.CUTTING.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
