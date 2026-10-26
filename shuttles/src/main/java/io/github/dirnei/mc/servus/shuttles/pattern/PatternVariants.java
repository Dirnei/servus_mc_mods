package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
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
 * matches the recipe and makes the pattern's result item in the same count.
 * When such an item only changes the result's components (the rack's wood,
 * the Elevator's material) the pattern is a material pattern, and its result
 * follows the items. A cell whose item carries components itself (a Rail with
 * its wood) takes other stacks of that item only at planning, from what is
 * stocked or made. Any is possible when some cell has other items and none
 * makes another item or count. Works on both sides.
 */
public final class PatternVariants {

    /** Whether a pattern can be Any, and why not. */
    public enum Verdict {
        POSSIBLE,
        /** Other items change only the result's components: the result follows the items. */
        MATERIAL,
        /** An item the recipe accepts makes another item or count. */
        CHANGES_RESULT,
        /** Every cell accepts only its own item. */
        NO_ALTERNATIVES
    }

    /** No other items for any cell. */
    public static final PatternVariants NONE = new PatternVariants(List.of(), List.of(), Set.of(), Set.of(), Set.of(),
            Verdict.NO_ALTERNATIVES, List.of(), null, ItemStack.EMPTY);

    private final List<ItemKey> own;
    private final List<List<ItemKey>> candidates;
    private final Set<ItemKey> materialOwn;
    private final Set<ItemKey> componentOwn;
    private final Set<DataComponentType<?>> varying;
    private final VariantChoice<ItemKey> choice;
    private final Verdict verdict;
    private final List<AnyGroup> groups;
    private final AnyGroup follows;
    private final ItemStack generic;

    private PatternVariants(List<ItemKey> own, List<List<ItemKey>> candidates, Set<ItemKey> materialOwn, Set<ItemKey> componentOwn,
                            Set<DataComponentType<?>> varying, Verdict verdict, List<AnyGroup> groups, AnyGroup follows, ItemStack generic) {
        this.own = own;
        this.candidates = candidates;
        this.materialOwn = materialOwn;
        this.componentOwn = componentOwn;
        this.varying = varying;
        this.choice = own.isEmpty() ? VariantChoice.exact(List.of()) : VariantChoice.of(own, candidates);
        this.verdict = verdict;
        this.groups = groups;
        this.follows = follows;
        this.generic = generic;
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
        Set<ItemKey> materialOwn = new LinkedHashSet<>();
        Set<ItemKey> componentOwn = new LinkedHashSet<>();
        Set<DataComponentType<?>> varying = new LinkedHashSet<>();
        for (ItemKey own : new LinkedHashSet<>(keys)) {
            if (own == null) {
                continue;
            }
            for (Holder<Item> item : accepted) {
                if (own.item() == item.value()) {
                    continue;
                }
                ItemStack made = made(level, recipe.get(), substituted(pattern, keys, own, new ItemStack(item))).orElse(ItemStack.EMPTY);
                if (made.isEmpty()) {
                    continue;
                }
                if (!sameItemAndCount(pattern.result(), made)) {
                    changes = true;
                    continue;
                }
                if (!pattern.sameResult(made)) {
                    materialOwn.add(own);
                    varying.addAll(differing(pattern.result(), made));
                }
                ItemKey candidate = ItemKey.of(new ItemStack(item));
                for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
                    if (own.equals(keys.get(cell))) {
                        candidates.get(cell).add(candidate);
                    }
                }
            }
            if (!own.stack(1).getComponentsPatch().isEmpty()) {
                ItemStack made = made(level, recipe.get(), substituted(pattern, keys, own, new ItemStack(own.item())))
                        .orElse(ItemStack.EMPTY);
                if (!made.isEmpty() && sameItemAndCount(pattern.result(), made) && !pattern.sameResult(made)) {
                    materialOwn.add(own);
                    componentOwn.add(own);
                    varying.addAll(differing(pattern.result(), made));
                }
            }
        }
        VariantChoice<ItemKey> choice = VariantChoice.of(keys, candidates);
        Verdict verdict = changes ? Verdict.CHANGES_RESULT : !materialOwn.isEmpty() ? Verdict.MATERIAL
                : choice.varies() ? Verdict.POSSIBLE : Verdict.NO_ALTERNATIVES;
        List<AnyGroup> groups = new ArrayList<>();
        AnyGroup follows = null;
        for (List<ItemKey> group : choice.groups()) {
            AnyGroup named = group(placement, group);
            groups.add(named);
            if (follows == null && materialOwn.contains(group.get(0))) {
                follows = named;
            }
        }
        if (follows == null && !componentOwn.isEmpty()) {
            follows = new AnyGroup("", List.of(new ItemStack(componentOwn.iterator().next().item())), 1);
        }
        ItemStack generic = verdict == Verdict.MATERIAL ? withoutTypes(pattern.result(), varying) : pattern.result();
        return new PatternVariants(keys, candidates, Set.copyOf(materialOwn), Set.copyOf(componentOwn), Set.copyOf(varying),
                verdict, List.copyOf(groups), follows, generic);
    }

    /** The pattern's template with the stack in every cell holding {@code own}. */
    private static List<ItemStack> substituted(EncodedPattern pattern, List<ItemKey> keys, ItemKey own, ItemStack stack) {
        List<ItemStack> cells = new ArrayList<>(pattern.template());
        for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
            if (own.equals(keys.get(cell))) {
                cells.set(cell, stack);
            }
        }
        return cells;
    }

    /** What the recipe makes from these cells, empty when they do not match it. */
    private static Optional<ItemStack> made(Level level, CraftingRecipe recipe, List<ItemStack> cells) {
        CraftingInput input = CraftingInput.of(3, 3, cells);
        if (!recipe.matches(input, level)) {
            return Optional.empty();
        }
        return Optional.of(recipe.assemble(input));
    }

    private static boolean sameItemAndCount(ItemStack a, ItemStack b) {
        return ItemStack.isSameItem(a, b) && a.getCount() == b.getCount();
    }

    /** The component types whose values differ between the two stacks. */
    private static Set<DataComponentType<?>> differing(ItemStack a, ItemStack b) {
        Set<DataComponentType<?>> types = new HashSet<>();
        a.getComponentsPatch().entrySet().forEach(entry -> types.add(entry.getKey()));
        b.getComponentsPatch().entrySet().forEach(entry -> types.add(entry.getKey()));
        types.removeIf(type -> Objects.equals(a.get(type), b.get(type)));
        return types;
    }

    /** The stack without the given components, as the item comes without them. */
    private static ItemStack withoutTypes(ItemStack stack, Collection<DataComponentType<?>> types) {
        ItemStack plain = new ItemStack(stack.getItem(), stack.getCount());
        plain.applyComponents(stack.getComponentsPatch().forget(types::contains));
        return plain;
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
        return verdict == Verdict.POSSIBLE || verdict == Verdict.MATERIAL;
    }

    /** Whether the result's components follow the items: a material pattern. */
    public boolean material() {
        return verdict == Verdict.MATERIAL;
    }

    /** What the cells of each ingredient that has other items accept. */
    public List<AnyGroup> groups() {
        return groups;
    }

    /** The ingredient the material follows; null when this is no material pattern. */
    public AnyGroup follows() {
        return follows;
    }

    /** The pattern's result without its material, as a generic order asks for it; the result itself otherwise. */
    public ItemStack genericResult() {
        return generic;
    }

    /** The stack without the components a material pattern's variants change. */
    public ItemStack generic(ItemStack stack) {
        return varying.isEmpty() ? stack : withoutTypes(stack, varying);
    }

    /**
     * The pattern crafting from the accepted items the stock holds most of; a
     * material pattern makes the result of those items. The pattern itself
     * when it is Exact, when nothing changes, or when the recipe does not take
     * the chosen items together.
     */
    public EncodedPattern variant(Level level, EncodedPattern pattern, Map<ItemKey, ? extends Number> stock) {
        if (!pattern.any() || !possible()) {
            return pattern;
        }
        if (material()) {
            return variants(level, pattern, stock, Set.of()).get(0);
        }
        List<ItemKey> chosen = choice.pick(stock);
        return chosen.equals(pattern.keys()) ? pattern : checked(level, pattern, chosen).orElse(pattern);
    }

    /**
     * Every variant the planner may use: for a material Any pattern one per
     * material item, see {@link VariantExpansion}; for any other pattern only
     * {@link #variant}. A cell whose item carries components also takes the
     * stacks of that item in {@code stock} and in {@code made}.
     *
     * @param made what other recipes of the line make
     */
    public List<EncodedPattern> variants(Level level, EncodedPattern pattern, Map<ItemKey, ? extends Number> stock, Collection<ItemKey> made) {
        if (!pattern.any() || !material()) {
            return List.of(variant(level, pattern, stock));
        }
        VariantChoice<ItemKey> planned = choiceWith(stock.keySet(), made);
        List<Boolean> materialGroups = new ArrayList<>();
        for (List<ItemKey> group : planned.groups()) {
            materialGroups.add(materialOwn.contains(group.get(0)));
        }
        List<EncodedPattern> list = new ArrayList<>();
        for (List<ItemKey> perGroup : VariantExpansion.expand(planned.groups(), materialGroups, stock, VariantExpansion.MAX_VARIANTS)) {
            List<ItemKey> cells = planned.cells(perGroup);
            Optional<EncodedPattern> checked = cells.equals(pattern.keys()) ? Optional.of(pattern) : checked(level, pattern, cells);
            checked.ifPresent(list::add);
        }
        return list.isEmpty() ? List.of(pattern) : list;
    }

    /** The static choice with the component cells widened by the stacks of their item in {@code stock} and {@code made}. */
    private VariantChoice<ItemKey> choiceWith(Collection<ItemKey> stocked, Collection<ItemKey> made) {
        if (componentOwn.isEmpty()) {
            return choice;
        }
        List<List<ItemKey>> widened = new ArrayList<>(candidates.size());
        for (int cell = 0; cell < candidates.size(); cell++) {
            ItemKey cellOwn = own.get(cell);
            List<ItemKey> list = new ArrayList<>(candidates.get(cell));
            if (cellOwn != null && componentOwn.contains(cellOwn)) {
                for (Collection<ItemKey> source : List.of(stocked, made)) {
                    for (ItemKey key : source) {
                        if (key.item() == cellOwn.item() && !list.contains(key)) {
                            list.add(key);
                        }
                    }
                }
            }
            widened.add(list);
        }
        return VariantChoice.of(own, widened);
    }

    /** The pattern with other items and the result the recipe makes from them, when that is the same item and count. */
    private static Optional<EncodedPattern> checked(Level level, EncodedPattern pattern, List<ItemKey> cells) {
        EncodedPattern variant = pattern.withKeys(cells);
        Optional<CraftingRecipe> recipe = ServusRecipes.crafting(level, pattern.recipe()).map(holder -> holder.value());
        Optional<ItemStack> made = recipe.flatMap(crafting -> made(level, crafting, variant.template()));
        if (made.isEmpty() || !sameItemAndCount(pattern.result(), made.get())) {
            return Optional.empty();
        }
        return Optional.of(pattern.sameResult(made.get()) ? variant : variant.withResult(made.get()));
    }
}
