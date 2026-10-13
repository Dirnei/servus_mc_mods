package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import net.minecraft.world.item.ItemStackTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleCodecs;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.alchemy.AlchemyRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * An Athanor craft: a counted item and fixed principles give one result, once the structure reaches the tier.
 *
 * @param input      the item lying on the Athanor, with count
 * @param principles what the craft burns from the tanks
 * @param tier       the lowest tier of the structure that allows it
 * @param resultTemplate what it makes
 * @param time       ticks per craft
 */
public record AthanorRecipe(SizedIngredient input, Principles principles, int tier, ItemStackTemplate resultTemplate, int time)
        implements AlchemyRecipe.CountedInput {

    public static final int DEFAULT_TIME = 100;

    public static final MapCodec<AthanorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(AthanorRecipe::input),
            PrincipleCodecs.CODEC.optionalFieldOf("principles", Principles.NONE).forGetter(AthanorRecipe::principles),
            Codec.intRange(0, 3).optionalFieldOf("tier", 0).forGetter(AthanorRecipe::tier),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AthanorRecipe::resultTemplate),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(AthanorRecipe::time)
    ).apply(instance, AthanorRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AthanorRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, AthanorRecipe::input,
            PrincipleCodecs.STREAM_CODEC.cast(), AthanorRecipe::principles,
            ByteBufCodecs.VAR_INT, AthanorRecipe::tier,
            ItemStackTemplate.STREAM_CODEC, AthanorRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, AthanorRecipe::time,
            AthanorRecipe::new);

    /** A new stack of what it makes. */
    public ItemStack result() {
        return resultTemplate.create();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return result();
    }

    @Override
    public RecipeSerializer<AthanorRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<AthanorRecipe> getType() {
        return ServusManufacturingRecipes.ATHANOR.get();
    }

    public static final RecipeSerializer<AthanorRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
