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
 * A Pot recipe ({@code servus_delicacies:pot}): counted items and an optional counted fluid cooked into a result.
 *
 * @param ingredients    the counted items
 * @param fluid          the fluid one cooking drains, if any
 * @param resultTemplate what it makes
 * @param time           ticks over a hot firebox
 */
public record PotRecipe(List<SizedIngredient> ingredients, Optional<SizedFluidIngredient> fluid, ItemStackTemplate resultTemplate,
                        int time) implements VesselRecipe {

    public static final MapCodec<PotRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(PotRecipe::ingredients),
            SizedFluidIngredient.CODEC.optionalFieldOf("fluid").forGetter(PotRecipe::fluid),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(PotRecipe::resultTemplate),
            Codec.INT.fieldOf("time").forGetter(PotRecipe::time)
    ).apply(instance, PotRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PotRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), PotRecipe::ingredients,
            ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC), PotRecipe::fluid,
            ItemStackTemplate.STREAM_CODEC, PotRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, PotRecipe::time,
            PotRecipe::new);

    public static final RecipeSerializer<PotRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    @Override
    public RecipeSerializer<PotRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<PotRecipe> getType() {
        return ServusDelicaciesRecipes.POT.get();
    }
}
