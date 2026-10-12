package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.PlacementInfo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
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
            SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(PurificationRecipe::input),
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
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    /** Never placed by the recipe book; the machines and JEI read the recipe's own fields. */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<PurificationRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<PurificationRecipe> getType() {
        return ServusManufacturingRecipes.PURIFICATION.get();
    }

    public static final RecipeSerializer<PurificationRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
