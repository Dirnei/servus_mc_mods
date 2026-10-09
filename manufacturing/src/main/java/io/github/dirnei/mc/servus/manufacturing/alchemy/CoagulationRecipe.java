package io.github.dirnei.mc.servus.manufacturing.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A Coagulator recipe: a fixed amount of each principle, at most one counted item, gives one result.
 *
 * @param input      the principles it takes
 * @param ingredient the item it takes as well, if any
 * @param result     what it makes
 * @param time       ticks per cycle
 * @param chapter    the journal chapter a team must have finished to select this recipe, if any
 */
public record CoagulationRecipe(Principles input, Optional<SizedIngredient> ingredient, ItemStack result, int time,
                                Optional<ResourceLocation> chapter)
        implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 40;

    public static final MapCodec<CoagulationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PrincipleCodecs.CODEC.fieldOf("input").forGetter(CoagulationRecipe::input),
            SizedIngredient.FLAT_CODEC.optionalFieldOf("ingredient").forGetter(CoagulationRecipe::ingredient),
            ItemStack.CODEC.fieldOf("result").forGetter(CoagulationRecipe::result),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(CoagulationRecipe::time),
            ResourceLocation.CODEC.optionalFieldOf("chapter").forGetter(CoagulationRecipe::chapter)
    ).apply(instance, CoagulationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoagulationRecipe> STREAM_CODEC = StreamCodec.composite(
            PrincipleCodecs.STREAM_CODEC.cast(), CoagulationRecipe::input,
            ByteBufCodecs.optional(SizedIngredient.STREAM_CODEC), CoagulationRecipe::ingredient,
            ItemStack.STREAM_CODEC, CoagulationRecipe::result,
            ByteBufCodecs.VAR_INT, CoagulationRecipe::time,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), CoagulationRecipe::chapter,
            CoagulationRecipe::new);

    /** Whether the item slot satisfies this recipe: empty for a recipe without item, else enough of the item. */
    public boolean itemReady(ItemStack stack) {
        return ingredient.map(sized -> !stack.isEmpty() && sized.ingredient().test(stack) && stack.getCount() >= sized.count())
                .orElse(true);
    }

    /** Whether the stack may go into the ingredient slot for this recipe. */
    public boolean acceptsItem(ItemStack stack) {
        return ingredient.map(sized -> sized.ingredient().test(stack)).orElse(false);
    }

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        return itemReady(recipeInput.item());
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
        ingredient.ifPresent(sized -> list.add(sized.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ServusManufacturingRecipes.COAGULATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ServusManufacturingRecipes.COAGULATION.get();
    }

    /** Serializer of coagulation recipes. */
    public static final class Serializer implements RecipeSerializer<CoagulationRecipe> {

        @Override
        public MapCodec<CoagulationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CoagulationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
