package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleCodecs;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * An Athanor craft: a counted item and fixed principles give one result, once the structure reaches the tier.
 *
 * @param input      the item lying on the Athanor, with count
 * @param principles what the craft burns from the tanks
 * @param tier       the lowest tier of the structure that allows it
 * @param result     what it makes
 * @param time       ticks per craft
 */
public record AthanorRecipe(SizedIngredient input, Principles principles, int tier, ItemStack result, int time)
        implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 100;

    public static final MapCodec<AthanorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.FLAT_CODEC.fieldOf("input").forGetter(AthanorRecipe::input),
            PrincipleCodecs.CODEC.optionalFieldOf("principles", Principles.NONE).forGetter(AthanorRecipe::principles),
            Codec.intRange(0, 3).optionalFieldOf("tier", 0).forGetter(AthanorRecipe::tier),
            ItemStack.CODEC.fieldOf("result").forGetter(AthanorRecipe::result),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(AthanorRecipe::time)
    ).apply(instance, AthanorRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AthanorRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, AthanorRecipe::input,
            PrincipleCodecs.STREAM_CODEC.cast(), AthanorRecipe::principles,
            ByteBufCodecs.VAR_INT, AthanorRecipe::tier,
            ItemStack.STREAM_CODEC, AthanorRecipe::result,
            ByteBufCodecs.VAR_INT, AthanorRecipe::time,
            AthanorRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        ItemStack stack = recipeInput.item();
        return accepts(stack) && stack.getCount() >= input.count();
    }

    /** Whether the item matches regardless of count. */
    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && input.ingredient().test(stack);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input.ingredient());
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ServusManufacturingRecipes.ATHANOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ServusManufacturingRecipes.ATHANOR.get();
    }

    /** Serializer of Athanor crafts. */
    public static final class Serializer implements RecipeSerializer<AthanorRecipe> {

        @Override
        public MapCodec<AthanorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AthanorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
