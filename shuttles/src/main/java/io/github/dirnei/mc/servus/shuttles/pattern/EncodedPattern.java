package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;

/**
 * A vanilla crafting recipe as a pattern holds it: the recipe id, a 3 by 3
 * template with one concrete item per used cell, the result and the mode.
 *
 * @param recipe   the crafting recipe the template matched
 * @param template nine cells, empty stacks for unused cells, used cells with count 1
 * @param result   what one craft makes
 * @param mode     whether the CPU may craft with other items the cells accept
 */
public record EncodedPattern(Identifier recipe, List<ItemStack> template, ItemStack result, Mode mode) {

    public static final int GRID = 9;

    /** Exact crafts with the template's own items; Any lets the CPU use any item a cell accepts that makes the same result. */
    public enum Mode implements StringRepresentable {
        EXACT("exact"),
        ANY("any");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        public static final StreamCodec<ByteBuf, Mode> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Mode::ordinal);

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final Codec<EncodedPattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("recipe").forGetter(EncodedPattern::recipe),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("template").forGetter(EncodedPattern::template),
            ItemStack.CODEC.fieldOf("result").forGetter(EncodedPattern::result),
            Mode.CODEC.optionalFieldOf("mode", Mode.EXACT).forGetter(EncodedPattern::mode)
    ).apply(instance, EncodedPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedPattern> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, EncodedPattern::recipe,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), EncodedPattern::template,
            ItemStack.STREAM_CODEC, EncodedPattern::result,
            Mode.STREAM_CODEC, EncodedPattern::mode,
            EncodedPattern::new);

    public EncodedPattern {
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            ItemStack cell = i < template.size() ? template.get(i) : ItemStack.EMPTY;
            cells.add(cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1));
        }
        template = List.copyOf(cells);
        result = result.copy();
        mode = mode == null ? Mode.EXACT : mode;
    }

    /** An Exact pattern. */
    public EncodedPattern(Identifier recipe, List<ItemStack> template, ItemStack result) {
        this(recipe, template, result, Mode.EXACT);
    }

    /**
     * What the recipe really makes from the template: the assembled stack,
     * with the components the recipe adds (such as the wood of the planks).
     * Falls back to the plain recipe result, or to {@code fallback} when the
     * recipe is gone.
     */
    public static ItemStack assembledResult(Level level, Identifier recipe, List<ItemStack> template, ItemStack fallback) {
        var holder = ServusRecipes.byId(level, recipe);
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe crafting)) {
            return fallback.copy();
        }
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            cells.add(i < template.size() ? template.get(i) : ItemStack.EMPTY);
        }
        CraftingInput input = CraftingInput.of(3, 3, cells);
        if (crafting.matches(input, level)) {
            ItemStack assembled = crafting.assemble(input);
            if (!assembled.isEmpty()) {
                return assembled;
            }
        }
        ItemStack plain = ServusRecipes.displayResult(level, crafting);
        return plain.isEmpty() ? fallback.copy() : plain.copy();
    }

    /** This pattern with the result its recipe really makes; the same instance when that is already the result. */
    public EncodedPattern assembled(Level level) {
        ItemStack real = assembledResult(level, recipe, template, result);
        if (sameResult(real)) {
            return this;
        }
        return new EncodedPattern(recipe, template, real, mode);
    }

    /** Whether the CPU may craft with other items the cells accept. */
    public boolean any() {
        return mode == Mode.ANY;
    }

    /** This pattern with another mode. */
    public EncodedPattern withMode(Mode other) {
        return other == mode ? this : new EncodedPattern(recipe, template, result, other);
    }

    /** This pattern crafting from other items, one key per cell and {@code null} for empty cells; recipe, result and mode stay. */
    public EncodedPattern withKeys(List<ItemKey> keys) {
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            ItemKey key = i < keys.size() ? keys.get(i) : null;
            cells.add(key == null ? ItemStack.EMPTY : key.stack(1));
        }
        return new EncodedPattern(recipe, cells, result, mode);
    }

    /** This pattern with another result, as a material variant makes it. */
    public EncodedPattern withResult(ItemStack other) {
        return new EncodedPattern(recipe, template, other, mode);
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
    public boolean sameWork(Identifier otherRecipe, List<ItemKey> otherKeys) {
        return recipe.equals(otherRecipe) && TemplateCounts.same(keys(), otherKeys);
    }

    /**
     * Whether a line holding both would hold the same work twice: the same
     * recipe with the same items, or two Any patterns of one recipe whose
     * results are the same item in the same count, whatever their material.
     */
    public boolean duplicates(EncodedPattern other) {
        if (sameWork(other.recipe, other.keys())) {
            return true;
        }
        return any() && other.any() && recipe.equals(other.recipe) && ItemStack.isSameItem(result, other.result)
                && result.getCount() == other.result.getCount();
    }

    /** Whether the stack is this pattern's result: same item, components and count. */
    public boolean sameResult(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(result, stack) && result.getCount() == stack.getCount();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EncodedPattern that) || !recipe.equals(that.recipe) || mode != that.mode || !sameResult(that.result)) {
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
        int hash = (recipe.hashCode() * 31 + result.getItem().hashCode()) * 31 + mode.hashCode();
        for (ItemStack cell : template) {
            hash = hash * 31 + (cell.isEmpty() ? 0 : cell.getItem().hashCode());
        }
        return hash;
    }
}
