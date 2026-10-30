package io.github.dirnei.mc.servus.foundation.machine.mixer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.production.SlotMatcher;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A Mixer recipe ({@code servus_foundation:mixing}): one to four counted items, shapeless, maybe a counted fluid, mixed
 * into one result once a driver has given it enough work. Never in the recipe book; the Mixer and JEI read its fields.
 *
 * @param ingredients    the counted items, each served by an input slot of its own
 * @param fluid          the fluid one mixing drains, if any
 * @param resultTemplate what it makes
 * @param work           work units one mixing takes
 */
public record MixingRecipe(List<SizedIngredient> ingredients, Optional<SizedFluidIngredient> fluid, ItemStackTemplate resultTemplate,
                           int work) implements Recipe<MixingInput> {

    public static final MapCodec<MixingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf(1, MixerBlockEntity.INPUTS).fieldOf("ingredients").forGetter(MixingRecipe::ingredients),
            SizedFluidIngredient.CODEC.optionalFieldOf("fluid").forGetter(MixingRecipe::fluid),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(MixingRecipe::resultTemplate),
            Codec.INT.fieldOf("work").forGetter(MixingRecipe::work)
    ).apply(instance, MixingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MixingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MixerBlockEntity.INPUTS)), MixingRecipe::ingredients,
            ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC), MixingRecipe::fluid,
            ItemStackTemplate.STREAM_CODEC, MixingRecipe::resultTemplate,
            ByteBufCodecs.VAR_INT, MixingRecipe::work,
            MixingRecipe::new);

    public static final RecipeSerializer<MixingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** A new stack of what it makes. */
    public ItemStack result() {
        return resultTemplate.create();
    }

    /** Whether some ingredient takes {@code stack}, regardless of count, for slot acceptance. */
    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && ingredients.stream().anyMatch(ingredient -> ingredient.ingredient().test(stack));
    }

    /** Whether this recipe's fluid is {@code stack}, regardless of amount, for tank acceptance. */
    public boolean acceptsFluid(FluidStack stack) {
        return !stack.isEmpty() && fluid.map(wanted -> wanted.ingredient().test(stack)).orElse(false);
    }

    /** Which input slot serves each ingredient, in ingredient order, or empty when the items do not fit. */
    public Optional<int[]> slotsFor(MixingInput input) {
        List<Predicate<ItemStack>> wants = ingredients.stream().<Predicate<ItemStack>>map(ingredient -> ingredient::test).toList();
        return SlotMatcher.match(wants, input.items(), ItemStack::isEmpty);
    }

    @Override
    public boolean matches(MixingInput input, Level level) {
        return fluid.map(wanted -> wanted.test(input.fluid())).orElse(true) && slotsFor(input).isPresent();
    }

    @Override
    public ItemStack assemble(MixingInput input) {
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
    public RecipeSerializer<MixingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<MixingRecipe> getType() {
        return MachineContent.MIXING.get();
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
