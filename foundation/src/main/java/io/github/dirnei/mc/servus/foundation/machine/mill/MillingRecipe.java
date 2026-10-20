package io.github.dirnei.mc.servus.foundation.machine.mill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A Mill recipe: a counted item ground into one result once a driver has given it enough work. Never in the recipe
 * book; the Mill and JEI read its fields.
 *
 * @param input          what one milling consumes, with count
 * @param resultTemplate what it makes
 * @param work           work units one milling takes
 */
public record MillingRecipe(SizedIngredient input, ItemStackTemplate resultTemplate, int work) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<MillingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.fieldOf("input").forGetter(MillingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(MillingRecipe::resultTemplate),
            Codec.INT.fieldOf("work").forGetter(MillingRecipe::work)
    ).apply(instance, MillingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MillingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, MillingRecipe::input,
            ItemStackTemplate.STREAM_CODEC, MillingRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, MillingRecipe::work,
            MillingRecipe::new);

    public static final RecipeSerializer<MillingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** A new stack of what it makes. */
    public ItemStack result() {
        return resultTemplate.create();
    }

    /** Whether the item matches regardless of count, for slot acceptance. */
    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && input.ingredient().test(stack);
    }

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        ItemStack stack = recipeInput.item();
        return accepts(stack) && stack.getCount() >= input.count();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return result();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<MillingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<MillingRecipe> getType() {
        return MachineContent.MILLING.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
