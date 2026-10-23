package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * Saves and deletes patterns of a data line from a 3 by 3 grid, for the
 * Production Terminal's editor. Server thread only.
 */
public final class PatternEditing {

    private PatternEditing() {
    }

    /**
     * Saves the grid as a pattern: a new one goes into the first free place of
     * the line's Pattern Storages, an existing one is rewritten in place.
     *
     * @param editing the slot being edited, or null for a new pattern
     * @return an empty string on success, else {@code empty}, {@code no_match},
     *         {@code duplicate}, {@code no_storage} or {@code full}
     */
    public static String save(ServerLevel level, DataNetwork network, Player player, List<ItemStack> grid,
                              PatternLibrary.Free editing) {
        if (grid.stream().allMatch(ItemStack::isEmpty)) {
            return "empty";
        }
        Optional<EncodedPattern> encoded = encode(level, grid);
        if (encoded.isEmpty()) {
            return "no_match";
        }
        EncodedPattern pattern = encoded.get();
        if (PatternLibrary.contains(level, network, pattern, editing)) {
            return "duplicate";
        }
        PatternLibrary.Free target = editing;
        if (target == null) {
            if (PatternLibrary.storages(level, network).isEmpty()) {
                return "no_storage";
            }
            Optional<PatternLibrary.Free> free = PatternLibrary.firstFree(level, network);
            if (free.isEmpty()) {
                return "full";
            }
            target = free.get();
        }
        Optional<PatternStorageBlockEntity> storage = storageAt(level, network, target);
        if (storage.isEmpty()) {
            return "full";
        }
        storage.get().set(target.slot(), pattern);
        return "";
    }

    /**
     * The pattern a 3 by 3 grid makes: the matching vanilla crafting recipe,
     * one item per used cell and the result the recipe assembles from them.
     * Empty when the grid is empty or matches nothing.
     */
    public static Optional<EncodedPattern> encode(Level level, List<ItemStack> grid) {
        List<ItemStack> cells = new ArrayList<>();
        for (int i = 0; i < EncodedPattern.GRID; i++) {
            ItemStack cell = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
            cells.add(cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1));
        }
        Optional<RecipeHolder<CraftingRecipe>> match = CraftingGrid.match(level, cells);
        if (match.isEmpty()) {
            return Optional.empty();
        }
        ItemStack result = EncodedPattern.assembledResult(level, match.get().id().identifier(), cells,
                ServusRecipes.displayResult(level, match.get().value()));
        return result.isEmpty() ? Optional.empty() : Optional.of(new EncodedPattern(match.get().id().identifier(), cells, result));
    }

    /** Removes a pattern of the line. */
    public static boolean delete(ServerLevel level, DataNetwork network, Player player, PatternLibrary.Free slot) {
        Optional<PatternStorageBlockEntity> storage = storageAt(level, network, slot);
        if (storage.isEmpty() || storage.get().pattern(slot.slot()).isEmpty()) {
            return false;
        }
        storage.get().clear(slot.slot());
        return true;
    }

    private static Optional<PatternStorageBlockEntity> storageAt(ServerLevel level, DataNetwork network, PatternLibrary.Free slot) {
        for (PatternStorageBlockEntity storage : PatternLibrary.storages(level, network)) {
            if (storage.getBlockPos().equals(slot.storage()) && slot.slot() >= 0 && slot.slot() < storage.size()) {
                return Optional.of(storage);
            }
        }
        return Optional.empty();
    }
}
