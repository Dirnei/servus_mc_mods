package io.github.dirnei.mc.servus.manufacturing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public final class AssemblingRecipeSerializer implements RecipeSerializer<AssemblingRecipe> {

    public static final int DEFAULT_TIME = 20;

    private static final MapCodec<AssemblingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(AssemblingRecipe::ingredients),
            ItemStack.CODEC.fieldOf("result").forGetter(AssemblingRecipe::result),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(AssemblingRecipe::time),
            ResourceLocation.CODEC.optionalFieldOf("tech").forGetter(AssemblingRecipe::tech)
    ).apply(instance, AssemblingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AssemblingRecipe::ingredients,
            ItemStack.STREAM_CODEC, AssemblingRecipe::result,
            ByteBufCodecs.VAR_INT, AssemblingRecipe::time,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), AssemblingRecipe::tech,
            AssemblingRecipe::new);

    @Override
    public MapCodec<AssemblingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
