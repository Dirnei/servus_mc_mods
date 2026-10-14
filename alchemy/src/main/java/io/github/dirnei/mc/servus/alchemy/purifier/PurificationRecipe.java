package io.github.dirnei.mc.servus.alchemy.purifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.AlchemyRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import java.util.Optional;
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
 * A Purifier recipe: a counted item leached with water into liquid Salt.
 *
 * @param input what is consumed, with count
 * @param water mB of water it takes
 * @param salt    mB of Salt it makes
 * @param residueTemplate what stays behind in the vat, empty for nothing
 * @param time    ticks per cycle
 */
public record PurificationRecipe(SizedIngredient input, int water, int salt, Optional<ItemStackTemplate> residueTemplate, int time) implements AlchemyRecipe.CountedInput {

    public static final int DEFAULT_TIME = 40;

    public static final MapCodec<PurificationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(PurificationRecipe::input),
            Codec.INT.fieldOf("water").forGetter(PurificationRecipe::water),
            Codec.INT.fieldOf("salt").forGetter(PurificationRecipe::salt),
            ItemStackTemplate.CODEC.optionalFieldOf("residue").forGetter(PurificationRecipe::residueTemplate),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(PurificationRecipe::time)
    ).apply(instance, PurificationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PurificationRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, PurificationRecipe::input,
            ByteBufCodecs.VAR_INT, PurificationRecipe::water,
            ByteBufCodecs.VAR_INT, PurificationRecipe::salt,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), PurificationRecipe::residueTemplate,
            ByteBufCodecs.VAR_INT, PurificationRecipe::time,
            PurificationRecipe::new);

    /** A new stack of what stays behind, empty for nothing. */
    public ItemStack residue() {
        return residueTemplate.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);
    }

    /** The Salt one item of the input makes. */
    public int saltPerItem() {
        return salt / Math.max(1, input.count());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<PurificationRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<PurificationRecipe> getType() {
        return ServusAlchemyRecipes.PURIFICATION.get();
    }

    public static final RecipeSerializer<PurificationRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
