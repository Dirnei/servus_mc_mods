package io.github.dirnei.mc.servus.brewery.malting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * What soaked grain on a Malting Floor sprouts into, and how long it takes. The count stays: n grain become n
 * results. Never in the recipe book.
 *
 * @param input          the soaked grain
 * @param resultTemplate what it sprouts into
 * @param time           game ticks until it is done
 * @param turnBonus      ticks one turn takes off the rest of the time
 * @param turnCooldown   ticks between two turns
 */
public record GerminatingRecipe(Ingredient input, ItemStackTemplate resultTemplate, int time, int turnBonus, int turnCooldown)
        implements Recipe<SingleRecipeInput> {

    public static final MapCodec<GerminatingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(GerminatingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(GerminatingRecipe::resultTemplate),
            Codec.INT.fieldOf("time").forGetter(GerminatingRecipe::time),
            Codec.INT.optionalFieldOf("turn_bonus", 0).forGetter(GerminatingRecipe::turnBonus),
            Codec.INT.optionalFieldOf("turn_cooldown", 0).forGetter(GerminatingRecipe::turnCooldown)
    ).apply(instance, GerminatingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GerminatingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, GerminatingRecipe::input,
            ItemStackTemplate.STREAM_CODEC, GerminatingRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, GerminatingRecipe::time,
            ByteBufCodecs.VAR_INT, GerminatingRecipe::turnBonus,
            ByteBufCodecs.VAR_INT, GerminatingRecipe::turnCooldown,
            GerminatingRecipe::new);

    public static final RecipeSerializer<GerminatingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** The result for {@code count} grain. */
    public ItemStack result(int count) {
        return resultTemplate.create().copyWithCount(count);
    }

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        return !recipeInput.item().isEmpty() && input.test(recipeInput.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return result(recipeInput.item().getCount());
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
    public RecipeSerializer<GerminatingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<GerminatingRecipe> getType() {
        return ServusBreweryRecipes.GERMINATING.get();
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
