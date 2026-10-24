package io.github.dirnei.mc.servus.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinationDimension;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A Calciner recipe: one counted input burned into fixed amounts of the three principles.
 *
 * @param input     what is consumed, with count
 * @param output    the principles it gives
 * @param time      ticks per cycle
 * @param dimension where its matter comes from; only the journal reads it
 */
public record CalcinationRecipe(SizedIngredient input, Principles output, int time, CalcinationDimension dimension)
        implements AlchemyRecipe.CountedInput {

    public static final int DEFAULT_TIME = 80;

    private static final Codec<CalcinationDimension> DIMENSION_CODEC = Codec.STRING.comapFlatMap(
            id -> CalcinationDimension.byId(id).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown calcination dimension: " + id)),
            CalcinationDimension::id);

    public static final MapCodec<CalcinationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(CalcinationRecipe::input),
            PrincipleCodecs.CODEC.fieldOf("output").forGetter(CalcinationRecipe::output),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(CalcinationRecipe::time),
            DIMENSION_CODEC.optionalFieldOf("dimension", CalcinationDimension.OVERWORLD).forGetter(CalcinationRecipe::dimension)
    ).apply(instance, CalcinationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalcinationRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, CalcinationRecipe::input,
            PrincipleCodecs.STREAM_CODEC.cast(), CalcinationRecipe::output,
            ByteBufCodecs.VAR_INT, CalcinationRecipe::time,
            ByteBufCodecs.VAR_INT.map(ordinal -> CalcinationDimension.values()[ordinal], CalcinationDimension::ordinal),
            CalcinationRecipe::dimension,
            CalcinationRecipe::new);

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<CalcinationRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<CalcinationRecipe> getType() {
        return ServusAlchemyRecipes.CALCINATION.get();
    }

    public static final RecipeSerializer<CalcinationRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
