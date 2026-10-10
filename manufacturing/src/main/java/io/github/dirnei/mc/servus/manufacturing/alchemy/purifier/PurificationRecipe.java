package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

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
 * A Purifier recipe: a counted item leached with water into liquid Salt.
 *
 * @param input what is consumed, with count
 * @param water mB of water it takes
 * @param salt    mB of Salt it makes
 * @param residue what stays behind in the vat, empty for nothing
 * @param time    ticks per cycle
 */
public record PurificationRecipe(SizedIngredient input, int water, int salt, ItemStack residue, int time) implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 40;

    public static final MapCodec<PurificationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.FLAT_CODEC.fieldOf("input").forGetter(PurificationRecipe::input),
            Codec.INT.fieldOf("water").forGetter(PurificationRecipe::water),
            Codec.INT.fieldOf("salt").forGetter(PurificationRecipe::salt),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("residue", ItemStack.EMPTY).forGetter(PurificationRecipe::residue),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(PurificationRecipe::time)
    ).apply(instance, PurificationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PurificationRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, PurificationRecipe::input,
            ByteBufCodecs.VAR_INT, PurificationRecipe::water,
            ByteBufCodecs.VAR_INT, PurificationRecipe::salt,
            ItemStack.OPTIONAL_STREAM_CODEC, PurificationRecipe::residue,
            ByteBufCodecs.VAR_INT, PurificationRecipe::time,
            PurificationRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        ItemStack stack = recipeInput.item();
        return accepts(stack) && stack.getCount() >= input.count();
    }

    /** Whether the item matches regardless of count, for slot acceptance. */
    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && input.ingredient().test(stack);
    }

    /** The Salt one item of the input makes. */
    public int saltPerItem() {
        return salt / Math.max(1, input.count());
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
        return ServusManufacturingRecipes.PURIFICATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ServusManufacturingRecipes.PURIFICATION.get();
    }

    /** Serializer of purification recipes. */
    public static final class Serializer implements RecipeSerializer<PurificationRecipe> {

        @Override
        public MapCodec<PurificationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PurificationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
