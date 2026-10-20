package io.github.dirnei.mc.servus.brewery.brewhouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * What a Malt Kiln tray dries a stack into ({@code servus_brewery:kilning}), and how long it takes. The count stays.
 * In dark mode a recipe with a {@link Dark} part keeps the stack on the fire for its extra time and gives its result.
 *
 * @param input          what the tray dries
 * @param resultTemplate the dried result
 * @param time           ticks over a hot firebox
 * @param dark           the dark result and its extra time, if the recipe has one
 */
public record KilningRecipe(Ingredient input, ItemStackTemplate resultTemplate, int time, Optional<Dark> dark)
        implements Recipe<SingleRecipeInput> {

    /** The result of dark mode and the ticks it takes on top of {@link #time()}. */
    public record Dark(ItemStackTemplate resultTemplate, int time) {

        public static final Codec<Dark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(Dark::resultTemplate),
                Codec.INT.fieldOf("time").forGetter(Dark::time)
        ).apply(instance, Dark::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Dark> STREAM_CODEC = StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, Dark::resultTemplate,
                ByteBufCodecs.VAR_INT, Dark::time,
                Dark::new);
    }

    public static final MapCodec<KilningRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(KilningRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(KilningRecipe::resultTemplate),
            Codec.INT.fieldOf("time").forGetter(KilningRecipe::time),
            Dark.CODEC.optionalFieldOf("dark").forGetter(KilningRecipe::dark)
    ).apply(instance, KilningRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KilningRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, KilningRecipe::input,
            ItemStackTemplate.STREAM_CODEC, KilningRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, KilningRecipe::time,
            ByteBufCodecs.optional(Dark.STREAM_CODEC), KilningRecipe::dark,
            KilningRecipe::new);

    public static final RecipeSerializer<KilningRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** The extra time of dark mode, if the recipe has a dark result. */
    public OptionalInt darkTime() {
        return dark.map(found -> OptionalInt.of(found.time())).orElse(OptionalInt.empty());
    }

    /** The result for {@code count} items, dark or pale. */
    public ItemStack result(int count, boolean darkResult) {
        ItemStackTemplate template = darkResult && dark.isPresent() ? dark.get().resultTemplate() : resultTemplate;
        return template.create().copyWithCount(count);
    }

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        return !recipeInput.item().isEmpty() && input.test(recipeInput.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput) {
        return result(recipeInput.item().getCount(), false);
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
    public RecipeSerializer<KilningRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<KilningRecipe> getType() {
        return ServusBreweryRecipes.KILNING.get();
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
