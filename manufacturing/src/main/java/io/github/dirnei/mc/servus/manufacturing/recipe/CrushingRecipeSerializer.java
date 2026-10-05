package io.github.dirnei.mc.servus.manufacturing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public final class CrushingRecipeSerializer implements RecipeSerializer<CrushingRecipe> {

    public static final int DEFAULT_TIME = 60;

    private static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.FLAT_CODEC.fieldOf("input").forGetter(CrushingRecipe::input),
            ItemStack.CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(CrushingRecipe::time)
    ).apply(instance, CrushingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, CrushingRecipe::input,
            ItemStack.STREAM_CODEC, CrushingRecipe::result,
            ByteBufCodecs.VAR_INT, CrushingRecipe::time,
            CrushingRecipe::new);

    @Override
    public MapCodec<CrushingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
