package io.github.dirnei.mc.servus.delicacies.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A Baking Oven recipe ({@code servus_delicacies:baking}): counted items baked into a result.
 *
 * @param ingredients    the counted items
 * @param resultTemplate what it makes
 * @param time           ticks over a hot firebox
 */
public record BakingRecipe(List<SizedIngredient> ingredients, ItemStackTemplate resultTemplate, int time) implements VesselRecipe {

    public static final MapCodec<BakingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(BakingRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(BakingRecipe::resultTemplate),
            Codec.INT.fieldOf("time").forGetter(BakingRecipe::time)
    ).apply(instance, BakingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BakingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), BakingRecipe::ingredients,
            ItemStackTemplate.STREAM_CODEC, BakingRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, BakingRecipe::time,
            BakingRecipe::new);

    public static final RecipeSerializer<BakingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    @Override
    public Optional<SizedFluidIngredient> fluid() {
        return Optional.empty();
    }

    @Override
    public RecipeSerializer<BakingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<BakingRecipe> getType() {
        return ServusDelicaciesRecipes.BAKING.get();
    }
}
