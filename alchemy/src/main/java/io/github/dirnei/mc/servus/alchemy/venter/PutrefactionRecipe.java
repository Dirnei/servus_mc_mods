package io.github.dirnei.mc.servus.alchemy.venter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.AlchemyRecipe;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A Venter Equinus recipe: a counted batch of rot putrefies into a result, in {@code time} ticks in a
 * full bed of fresh straw and in {@code coldTime} ticks in an empty bed.
 *
 * @param input          the rot one batch takes, with count
 * @param resultTemplate what one batch makes
 * @param time           ticks per batch in a full bed
 * @param coldTime       ticks per batch in an empty bed, at least {@code time}
 */
public record PutrefactionRecipe(SizedIngredient input, ItemStackTemplate resultTemplate, int time, int coldTime)
        implements AlchemyRecipe.CountedInput {

    /** The rot the shipped recipe takes. */
    public static final TagKey<Item> PUTREFIABLE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "putrefiable"));

    /** How much slower an empty bed is when a recipe names no cold time. */
    public static final int DEFAULT_COLD_FACTOR = 4;

    public static final MapCodec<PutrefactionRecipe> CODEC = RecordCodecBuilder.<PutrefactionRecipe>mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(PutrefactionRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(PutrefactionRecipe::resultTemplate),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("time").forGetter(PutrefactionRecipe::time),
            Codec.INT.optionalFieldOf("cold_time").forGetter(recipe -> Optional.of(recipe.coldTime()))
    ).apply(instance, (input, result, time, coldTime) ->
            new PutrefactionRecipe(input, result, time, coldTime.orElse(time * DEFAULT_COLD_FACTOR)))).validate(recipe ->
            recipe.coldTime() >= recipe.time() ? DataResult.success(recipe)
                    : DataResult.error(() -> "cold_time " + recipe.coldTime() + " is below time " + recipe.time()));

    public static final StreamCodec<RegistryFriendlyByteBuf, PutrefactionRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, PutrefactionRecipe::input,
            ItemStackTemplate.STREAM_CODEC, PutrefactionRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, PutrefactionRecipe::time,
            ByteBufCodecs.VAR_INT, PutrefactionRecipe::coldTime,
            PutrefactionRecipe::new);

    /** A new stack of what one batch makes. */
    public ItemStack result() {
        return resultTemplate.create();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return result();
    }

    @Override
    public RecipeSerializer<PutrefactionRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<PutrefactionRecipe> getType() {
        return ServusAlchemyRecipes.PUTREFACTION.get();
    }

    public static final RecipeSerializer<PutrefactionRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
