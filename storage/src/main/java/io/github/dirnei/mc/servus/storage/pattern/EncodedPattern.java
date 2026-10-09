package io.github.dirnei.mc.servus.storage.pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;

/**
 * A vanilla crafting recipe as a pattern holds it: the recipe id, a 3 by 3
 * template with one concrete item per used cell, and the result.
 *
 * @param recipe   the crafting recipe the template matched
 * @param template nine cells, empty stacks for unused cells, used cells with count 1
 * @param result   what one craft makes
 */
public record EncodedPattern(ResourceLocation recipe, List<ItemStack> template, ItemStack result) {

    public static final int GRID = 9;

    public static final Codec<EncodedPattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("recipe").forGetter(EncodedPattern::recipe),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("template").forGetter(EncodedPattern::template),
            ItemStack.CODEC.fieldOf("result").forGetter(EncodedPattern::result)
    ).apply(instance, EncodedPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedPattern> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, EncodedPattern::recipe,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), EncodedPattern::template,
            ItemStack.STREAM_CODEC, EncodedPattern::result,
            EncodedPattern::new);

    public EncodedPattern {
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            ItemStack cell = i < template.size() ? template.get(i) : ItemStack.EMPTY;
            cells.add(cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1));
        }
        template = List.copyOf(cells);
        result = result.copy();
    }

    /**
     * What the recipe really makes from the template: the assembled stack,
     * with the components the recipe adds (such as the wood of the planks).
     * Falls back to the plain recipe result, or to {@code fallback} when the
     * recipe is gone.
     */
    public static ItemStack assembledResult(Level level, ResourceLocation recipe, List<ItemStack> template, ItemStack fallback) {
        var holder = level.getRecipeManager().byKey(recipe);
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe crafting)) {
            return fallback.copy();
        }
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            cells.add(i < template.size() ? template.get(i) : ItemStack.EMPTY);
        }
        CraftingInput input = CraftingInput.of(3, 3, cells);
        if (crafting.matches(input, level)) {
            ItemStack assembled = crafting.assemble(input, level.registryAccess());
            if (!assembled.isEmpty()) {
                return assembled;
            }
        }
        ItemStack plain = crafting.getResultItem(level.registryAccess());
        return plain.isEmpty() ? fallback.copy() : plain.copy();
    }

    /** This pattern with the result its recipe really makes; the same instance when that is already the result. */
    public EncodedPattern assembled(Level level) {
        ItemStack real = assembledResult(level, recipe, template, result);
        if (ItemStack.isSameItemSameComponents(real, result) && real.getCount() == result.getCount()) {
            return this;
        }
        return new EncodedPattern(recipe, template, real);
    }

    /** The template's cells as keys, {@code null} for empty cells. */
    public List<ItemKey> keys() {
        List<ItemKey> keys = new ArrayList<>(GRID);
        for (ItemStack cell : template) {
            keys.add(cell.isEmpty() ? null : ItemKey.of(cell));
        }
        return keys;
    }

    /** What one craft consumes, per item. */
    public Map<ItemKey, Integer> required() {
        return TemplateCounts.counts(keys());
    }

    /** What one craft consumes, as stacks with counts. */
    public List<ItemStack> inputs() {
        List<ItemStack> inputs = new ArrayList<>();
        required().forEach((key, count) -> inputs.add(key.stack(count)));
        return inputs;
    }

    /** Whether the other pattern is the same recipe with the same items. */
    public boolean sameWork(ResourceLocation otherRecipe, List<ItemKey> otherKeys) {
        return recipe.equals(otherRecipe) && TemplateCounts.same(keys(), otherKeys);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EncodedPattern that) || !recipe.equals(that.recipe)
                || !ItemStack.isSameItemSameComponents(result, that.result) || result.getCount() != that.result.getCount()) {
            return false;
        }
        for (int i = 0; i < GRID; i++) {
            if (!ItemStack.isSameItemSameComponents(template.get(i), that.template.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = recipe.hashCode() * 31 + result.getItem().hashCode();
        for (ItemStack cell : template) {
            hash = hash * 31 + (cell.isEmpty() ? 0 : cell.getItem().hashCode());
        }
        return hash;
    }
}
