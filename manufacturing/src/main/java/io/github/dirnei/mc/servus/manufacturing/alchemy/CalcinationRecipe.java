package io.github.dirnei.mc.servus.manufacturing.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * A Calciner recipe: one counted input burned into fixed amounts of the three principles.
 *
 * @param input  what is consumed, with count
 * @param output the principles it gives
 * @param time   ticks per cycle
 */
public record CalcinationRecipe(SizedIngredient input, Principles output, int time) implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 80;

    public static final MapCodec<CalcinationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.FLAT_CODEC.fieldOf("input").forGetter(CalcinationRecipe::input),
            PrincipleCodecs.CODEC.fieldOf("output").forGetter(CalcinationRecipe::output),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(CalcinationRecipe::time)
    ).apply(instance, CalcinationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalcinationRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, CalcinationRecipe::input,
            PrincipleCodecs.STREAM_CODEC.cast(), CalcinationRecipe::output,
            ByteBufCodecs.VAR_INT, CalcinationRecipe::time,
            CalcinationRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        ItemStack stack = recipeInput.item();
        return accepts(stack) && stack.getCount() >= input.count();
    }

    /** Whether the item matches regardless of count, for slot acceptance. */
    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && input.ingredient().test(stack);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
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
        return ServusManufacturingRecipes.CALCINATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ServusManufacturingRecipes.CALCINATION.get();
    }

    /** Serializer of calcination recipes. */
    public static final class Serializer implements RecipeSerializer<CalcinationRecipe> {

        @Override
        public MapCodec<CalcinationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CalcinationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
