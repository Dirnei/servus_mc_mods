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
 * A Pan recipe ({@code servus_delicacies:pan}): counted items fried into a result.
 *
 * @param ingredients    the counted items
 * @param resultTemplate what it makes
 * @param time           ticks over a hot firebox
 */
public record PanRecipe(List<SizedIngredient> ingredients, ItemStackTemplate resultTemplate, int time) implements VesselRecipe {

    public static final MapCodec<PanRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(PanRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(PanRecipe::resultTemplate),
            Codec.INT.fieldOf("time").forGetter(PanRecipe::time)
    ).apply(instance, PanRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PanRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), PanRecipe::ingredients,
            ItemStackTemplate.STREAM_CODEC, PanRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, PanRecipe::time,
            PanRecipe::new);

    public static final RecipeSerializer<PanRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    @Override
    public Optional<SizedFluidIngredient> fluid() {
        return Optional.empty();
    }

    @Override
    public RecipeSerializer<PanRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<PanRecipe> getType() {
        return ServusDelicaciesRecipes.PAN.get();
    }
}
