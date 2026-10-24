package io.github.dirnei.mc.servus.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A Coagulator recipe: a fixed amount of each principle, at most one counted item, gives one result. The seed in the
 * Coagulator's seed slot picks the recipe, and one seed is used up per cycle. A transmutation seed, one that is not the
 * result, turns into the result; a seed that is the result itself comes back with it, so the cycle gives one more.
 *
 * @param input      the principles it takes
 * @param ingredient the item it takes as well, if any
 * @param resultTemplate what it makes
 * @param time       ticks per cycle
 * @param seed       the items in the seed slot that grow this recipe, if not its own result
 * @param chapter    the journal chapter a team must have finished to grow this recipe, if any
 */
public record CoagulationRecipe(Principles input, Optional<SizedIngredient> ingredient, ItemStackTemplate resultTemplate, int time,
                                Optional<Ingredient> seed, Optional<Identifier> chapter)
        implements AlchemyRecipe {

    public static final int DEFAULT_TIME = 40;

    public static final MapCodec<CoagulationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PrincipleCodecs.CODEC.fieldOf("input").forGetter(CoagulationRecipe::input),
            SizedIngredient.NESTED_CODEC.optionalFieldOf("ingredient").forGetter(CoagulationRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CoagulationRecipe::resultTemplate),
            Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(CoagulationRecipe::time),
            Ingredient.CODEC.optionalFieldOf("seed").forGetter(CoagulationRecipe::seed),
            Identifier.CODEC.optionalFieldOf("chapter").forGetter(CoagulationRecipe::chapter)
    ).apply(instance, CoagulationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoagulationRecipe> STREAM_CODEC = StreamCodec.composite(
            PrincipleCodecs.STREAM_CODEC.cast(), CoagulationRecipe::input,
            ByteBufCodecs.optional(SizedIngredient.STREAM_CODEC), CoagulationRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, CoagulationRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, CoagulationRecipe::time,
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, CoagulationRecipe::seed,
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

    /** What grows this recipe from the seed slot: its {@code seed}, or else its own result item. */
    public Ingredient seedIngredient() {
        return seed.orElseGet(() -> Ingredient.of(resultTemplate.item().value()));
    }

    /** Whether the seed is a crystal of the result itself rather than a transmutation seed. */
    public boolean selfSeed(ItemStack seed) {
        return seed.is(resultTemplate.item());
    }

    /** What one cycle grown from {@code seed} gives: the result, and one more of it when the seed is a crystal of the result. */
    public ItemStack resultFrom(ItemStack seed) {
        ItemStack result = result();
        if (selfSeed(seed)) {
            result.grow(1);
        }
        return result;
    }

    /** A new stack of what it makes. */
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
    public RecipeSerializer<CoagulationRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<CoagulationRecipe> getType() {
        return ServusAlchemyRecipes.COAGULATION.get();
    }

    public static final RecipeSerializer<CoagulationRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
