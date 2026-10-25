package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * Which other items each cell of a pattern may take: every item the recipe's
 * ingredients accept that, put into all cells holding the same item, still
 * matches the recipe and makes the pattern's result. Any is possible when some
 * cell has such items and no accepted item changes the result. Works on both
 * sides.
 */
public final class PatternVariants {

    /** Whether a pattern can be Any, and why not. */
    public enum Verdict {
        POSSIBLE,
        /** An item the recipe accepts makes another result, such as a rack of another wood. */
        CHANGES_RESULT,
        /** Every cell accepts only its own item. */
        NO_ALTERNATIVES
    }

    /** No other items for any cell. */
    public static final PatternVariants NONE = new PatternVariants(VariantChoice.exact(List.of()), Verdict.NO_ALTERNATIVES, List.of());

    private final VariantChoice<ItemKey> choice;
    private final Verdict verdict;
    private final List<AnyGroup> groups;

    private PatternVariants(VariantChoice<ItemKey> choice, Verdict verdict, List<AnyGroup> groups) {
        this.choice = choice;
        this.verdict = verdict;
        this.groups = groups;
    }

    /** The other items each cell of the pattern may take; {@link #NONE} when its recipe is gone. */
    public static PatternVariants of(Level level, EncodedPattern pattern) {
        Optional<CraftingRecipe> recipe = ServusRecipes.crafting(level, pattern.recipe()).map(holder -> holder.value());
        if (recipe.isEmpty() || recipe.get().placementInfo().isImpossibleToPlace()) {
            return NONE;
        }
        PlacementInfo placement = recipe.get().placementInfo();
        Set<Holder<Item>> accepted = new LinkedHashSet<>();
        for (Ingredient ingredient : placement.ingredients()) {
            ingredient.items().forEach(accepted::add);
        }
        List<ItemKey> keys = pattern.keys();
        List<List<ItemKey>> candidates = new ArrayList<>(EncodedPattern.GRID);
        for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
            candidates.add(keys.get(cell) == null ? List.of() : new ArrayList<>(List.of(keys.get(cell))));
        }
        boolean changes = false;
        for (ItemKey own : new LinkedHashSet<>(keys)) {
            if (own == null) {
                continue;
            }
            for (Holder<Item> item : accepted) {
                if (own.item() == item.value()) {
                    continue;
                }
                List<ItemStack> cells = new ArrayList<>(pattern.template());
                for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
                    if (own.equals(keys.get(cell))) {
                        cells.set(cell, new ItemStack(item));
                    }
                }
                Optional<ItemStack> made = made(level, recipe.get(), cells);
                if (made.isEmpty()) {
                    continue;
                }
                if (!pattern.sameResult(made.get())) {
                    changes = true;
                    continue;
                }
                ItemKey candidate = ItemKey.of(new ItemStack(item));
                for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
                    if (own.equals(keys.get(cell))) {
                        candidates.get(cell).add(candidate);
                    }
                }
            }
        }
        VariantChoice<ItemKey> choice = VariantChoice.of(keys, candidates);
        Verdict verdict = changes ? Verdict.CHANGES_RESULT : choice.varies() ? Verdict.POSSIBLE : Verdict.NO_ALTERNATIVES;
        List<AnyGroup> groups = new ArrayList<>();
        for (List<ItemKey> group : choice.groups()) {
            groups.add(group(placement, group));
        }
        return new PatternVariants(choice, verdict, List.copyOf(groups));
    }

    /** What the recipe makes from these cells, empty when they do not match it. */
    private static Optional<ItemStack> made(Level level, CraftingRecipe recipe, List<ItemStack> cells) {
        CraftingInput input = CraftingInput.of(3, 3, cells);
        if (!recipe.matches(input, level)) {
            return Optional.empty();
        }
        return Optional.of(recipe.assemble(input));
    }

    /** The group named by the item tag of an ingredient that accepts all its items, if there is one. */
    private static AnyGroup group(PlacementInfo placement, List<ItemKey> items) {
        String tagKey = "";
        for (Ingredient ingredient : placement.ingredients()) {
            if (ingredient.isCustom() || !items.stream().allMatch(key -> ingredient.test(key.stack(1)))) {
                continue;
            }
            var tag = ingredient.getValues().unwrapKey();
            if (tag.isPresent()) {
                tagKey = Tags.getTagTranslationKey(tag.get());
                break;
            }
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < items.size() && i < AnyGroup.SHOWN; i++) {
            stacks.add(items.get(i).stack(1));
        }
        return new AnyGroup(tagKey, stacks, items.size());
    }

    public Verdict verdict() {
        return verdict;
    }

    /** Whether the pattern may be Any. */
    public boolean possible() {
        return verdict == Verdict.POSSIBLE;
    }

    /** What the cells of each ingredient that has other items accept. */
    public List<AnyGroup> groups() {
        return groups;
    }

    /**
     * The pattern crafting from the accepted items the stock holds most of.
     * The pattern itself when it is Exact, when nothing changes, or when the
     * recipe does not make the same result from the chosen items together.
     */
    public EncodedPattern variant(Level level, EncodedPattern pattern, Map<ItemKey, ? extends Number> stock) {
        if (!pattern.any() || !possible()) {
            return pattern;
        }
        List<ItemKey> chosen = choice.pick(stock);
        if (chosen.equals(pattern.keys())) {
            return pattern;
        }
        EncodedPattern variant = pattern.withKeys(chosen);
        Optional<CraftingRecipe> recipe = ServusRecipes.crafting(level, pattern.recipe()).map(holder -> holder.value());
        boolean same = recipe.flatMap(crafting -> made(level, crafting, variant.template())).map(pattern::sameResult).orElse(false);
        return same ? variant : pattern;
    }
}
