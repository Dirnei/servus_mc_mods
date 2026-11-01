package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.terminal.MostStocked;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** The 3 by 3 crafting grid of the pattern editor: matching it to a recipe and laying a recipe out in it. */
public final class CraftingGrid {

    public static final int GRID = 9;
    /** At most this many stacks a recipe viewer sends for one cell. */
    public static final int MAX_SHOWN = 64;
    /** The stacks a recipe viewer shows per cell, row by row, for serverbound payloads. */
    public static final StreamCodec<RegistryFriendlyByteBuf, List<List<ItemStack>>> SHOWN_CELLS_CODEC =
            ItemStack.OPTIONAL_UNTRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SHOWN)).apply(ByteBufCodecs.list(GRID));

    private CraftingGrid() {
    }

    /** The vanilla crafting recipe a 3 by 3 grid matches, for saving and for the editor's preview. */
    public static Optional<RecipeHolder<CraftingRecipe>> match(Level level, List<ItemStack> cells) {
        if (cells.stream().allMatch(ItemStack::isEmpty)) {
            return Optional.empty();
        }
        return ServusRecipes.craftingFor(level, CraftingInput.of(3, 3, cells));
    }

    /** "2 Oak Planks, 1 Coal" for a list of stacks with counts. */
    public static MutableComponent stacks(List<ItemStack> stacks) {
        MutableComponent text = Component.empty();
        for (int i = 0; i < stacks.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(stacks.get(i).getCount() + " ").append(stacks.get(i).getHoverName());
        }
        return text;
    }

    /**
     * The template laid out by a vanilla crafting recipe for the recipe viewer:
     * per cell the stack {@link #stockedChoice} picks from what its ingredient accepts.
     *
     * @param stock the warehouse's requestable stock, largest first; empty without a warehouse
     */
    public static Optional<List<ItemStack>> templateOf(Level level, Identifier id, List<Map.Entry<ItemKey, Long>> stock) {
        Optional<RecipeHolder<?>> holder = ServusRecipes.byId(level, id);
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe recipe)) {
            return Optional.empty();
        }
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            cells.add(ItemStack.EMPTY);
        }
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        List<Optional<Ingredient>> ingredients = ServusRecipes.gridIngredients(recipe);
        for (int i = 0; i < ingredients.size() && i < GRID; i++) {
            ItemStack item = ingredients.get(i).map(ingredient -> stockedChoice(ServusRecipes.items(ingredient), stock))
                    .orElse(ItemStack.EMPTY);
            if (!item.isEmpty()) {
                int cell = width == 3 ? i : (i / width) * 3 + i % width;
                if (cell < GRID) {
                    cells.set(cell, item);
                }
            }
        }
        return Optional.of(cells);
    }

    /**
     * The template for a recipe viewer transfer: {@link #templateOf} when the
     * server knows the recipe, else {@link #templateOfShown} from what the viewer showed.
     */
    public static Optional<List<ItemStack>> templateFor(Level level, Identifier id, List<List<ItemStack>> shown,
                                                        List<Map.Entry<ItemKey, Long>> stock) {
        Optional<List<ItemStack>> known = templateOf(level, id, stock);
        return known.isPresent() ? known : templateOfShown(shown, stock);
    }

    /**
     * The template laid out from the stacks a recipe viewer shows per cell, for
     * recipes only the viewer knows: per cell the stocked stack matching a shown
     * stack the warehouse has the most of, else the first shown stack. A shown
     * stack with components of its own matches only stock with those components.
     *
     * @param shown the shown stacks per cell, row by row; empty when the viewer sent none
     * @return empty when no cell shows anything
     */
    public static Optional<List<ItemStack>> templateOfShown(List<List<ItemStack>> shown, List<Map.Entry<ItemKey, Long>> stock) {
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            cells.add(i < shown.size() ? shownChoice(shown.get(i), stock) : ItemStack.EMPTY);
        }
        return cells.stream().allMatch(ItemStack::isEmpty) ? Optional.empty() : Optional.of(cells);
    }

    private static ItemStack shownChoice(List<ItemStack> shown, List<Map.Entry<ItemKey, Long>> stock) {
        List<ItemStack> options = shown.stream().filter(stack -> !stack.isEmpty()).toList();
        if (options.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return MostStocked.pick(stock, key -> options.stream().anyMatch(option -> fits(option, key)))
                .map(key -> key.stack(1)).orElse(options.getFirst().copyWithCount(1));
    }

    /** Whether stock of this key fits a shown stack: the same item, and the same components when the shown stack has its own. */
    private static boolean fits(ItemStack shown, ItemKey key) {
        return shown.getItem() == key.item()
                && (shown.getComponentsPatch().isEmpty() || shown.getComponentsPatch().equals(key.components()));
    }

    /**
     * A single stack for a cell that accepts these items: the stocked stack of
     * an accepted item the warehouse has the most of, with its components,
     * else the first accepted item. Empty when nothing is accepted.
     */
    private static ItemStack stockedChoice(List<ItemStack> accepted, List<Map.Entry<ItemKey, Long>> stock) {
        Set<Item> items = new HashSet<>();
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack option : accepted) {
            if (!option.isEmpty()) {
                items.add(option.getItem());
                if (first.isEmpty()) {
                    first = option;
                }
            }
        }
        if (first.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack fallback = first.copyWithCount(1);
        return MostStocked.pick(stock, key -> items.contains(key.item())).map(key -> key.stack(1)).orElse(fallback);
    }
}
