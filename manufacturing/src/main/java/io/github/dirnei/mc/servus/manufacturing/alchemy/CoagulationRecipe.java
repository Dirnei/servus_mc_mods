package io.github.dirnei.mc.servus.manufacturing.alchemy;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.PlacementInfo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
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
 * @param resultTemplate what it makes
 * @param time       ticks per cycle
 * @param chapter    the journal chapter a team must have finished to select this recipe, if any
 */
public record CoagulationRecipe(Principles input, Optional<SizedIngredient> ingredient, ItemStackTemplate resultTemplate, int time,
                                Optional<Identifier> chapter)
        implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 40;

    public static final MapCodec<CoagulationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PrincipleCodecs.CODEC.fieldOf("input").forGetter(CoagulationRecipe::input),
            SizedIngredient.NESTED_CODEC.optionalFieldOf("ingredient").forGetter(CoagulationRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CoagulationRecipe::resultTemplate),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(CoagulationRecipe::time),
            Identifier.CODEC.optionalFieldOf("chapter").forGetter(CoagulationRecipe::chapter)
    ).apply(instance, CoagulationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoagulationRecipe> STREAM_CODEC = StreamCodec.composite(
            PrincipleCodecs.STREAM_CODEC.cast(), CoagulationRecipe::input,
            ByteBufCodecs.optional(SizedIngredient.STREAM_CODEC), CoagulationRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, CoagulationRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, CoagulationRecipe::time,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), CoagulationRecipe::chapter,
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

    /** A new stack of what it makes. Recipes hold templates since 1.21.5; stacks need bound item components. */
    public ItemStack result() {
        return resultTemplate.create();
    }

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        return itemReady(recipeInput.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return result();
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
    public RecipeSerializer<CoagulationRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<CoagulationRecipe> getType() {
        return ServusManufacturingRecipes.COAGULATION.get();
    }

    public static final RecipeSerializer<CoagulationRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
