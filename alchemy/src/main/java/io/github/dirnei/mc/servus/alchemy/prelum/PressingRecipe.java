package io.github.dirnei.mc.servus.alchemy.prelum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.AlchemyRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A Prelum recipe: a counted item pressed into one result.
 *
 * @param input          what one pressing consumes, with count
 * @param resultTemplate what it makes
 * @param time           ticks per pressing
 */
public record PressingRecipe(SizedIngredient input, ItemStackTemplate resultTemplate, int time) implements AlchemyRecipe.CountedInput {

    /** The time Alchemy's own pressing takes. */
    public static final int DEFAULT_TIME = 600;

    public static final MapCodec<PressingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(PressingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(PressingRecipe::resultTemplate),
            Codec.INT.fieldOf("time").forGetter(PressingRecipe::time)
    ).apply(instance, PressingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, PressingRecipe::input,
            ItemStackTemplate.STREAM_CODEC, PressingRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, PressingRecipe::time,
            PressingRecipe::new);

    /** A new stack of what it makes. */
    public ItemStack result() {
        return resultTemplate.create();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return result();
    }

    @Override
    public RecipeSerializer<PressingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<PressingRecipe> getType() {
        return ServusAlchemyRecipes.PRESSING.get();
    }

    public static final RecipeSerializer<PressingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
