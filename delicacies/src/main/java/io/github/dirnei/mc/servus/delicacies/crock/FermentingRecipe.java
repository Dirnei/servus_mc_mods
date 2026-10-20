package io.github.dirnei.mc.servus.delicacies.crock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselRecipe;
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
 * A Fermenting Crock recipe ({@code servus_delicacies:fermenting}): counted items and an optional counted fluid that
 * ferment into a result over game time.
 *
 * @param ingredients    the counted items of one batch
 * @param fluid          the fluid one batch takes, if any
 * @param resultTemplate what one batch makes
 * @param time           game ticks the crock stays sealed
 */
public record FermentingRecipe(List<SizedIngredient> ingredients, Optional<SizedFluidIngredient> fluid, ItemStackTemplate resultTemplate,
                               int time) implements VesselRecipe {

    public static final MapCodec<FermentingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(FermentingRecipe::ingredients),
            SizedFluidIngredient.CODEC.optionalFieldOf("fluid").forGetter(FermentingRecipe::fluid),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(FermentingRecipe::resultTemplate),
            Codec.INT.fieldOf("time").forGetter(FermentingRecipe::time)
    ).apply(instance, FermentingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FermentingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), FermentingRecipe::ingredients,
            ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC), FermentingRecipe::fluid,
            ItemStackTemplate.STREAM_CODEC, FermentingRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, FermentingRecipe::time,
            FermentingRecipe::new);

    public static final RecipeSerializer<FermentingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    @Override
    public RecipeSerializer<FermentingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<FermentingRecipe> getType() {
        return ServusDelicaciesRecipes.FERMENTING.get();
    }
}
