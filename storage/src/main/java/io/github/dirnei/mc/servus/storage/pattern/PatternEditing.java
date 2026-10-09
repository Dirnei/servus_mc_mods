package io.github.dirnei.mc.servus.storage.pattern;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

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
        List<ItemStack> cells = new ArrayList<>();
        boolean any = false;
        for (int i = 0; i < EncodedPattern.GRID; i++) {
            ItemStack cell = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
            cells.add(cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1));
            any |= !cell.isEmpty();
        }
        if (!any) {
            return "empty";
        }
        Optional<RecipeHolder<CraftingRecipe>> match = CraftingGrid.match(level, cells);
        if (match.isEmpty()) {
            return "no_match";
        }
        // The result as the recipe really crafts it from these cells, components included.
        ItemStack result = EncodedPattern.assembledResult(level, match.get().id(), cells,
                match.get().value().getResultItem(level.registryAccess()));
        if (result.isEmpty()) {
            return "no_match";
        }
        EncodedPattern pattern = new EncodedPattern(match.get().id(), cells, result);
        if (PatternLibrary.contains(level, network, pattern, editing)) {
            return "duplicate";
        }
        if (editing != null) {
            Optional<PatternStorageBlockEntity> storage = storageAt(level, network, editing);
            if (storage.isEmpty()) {
                return "full";
            }
            storage.get().set(editing.slot(), pattern);
            return "";
        }
        if (PatternLibrary.storages(level, network).isEmpty()) {
            return "no_storage";
        }
        Optional<PatternLibrary.Free> free = PatternLibrary.firstFree(level, network);
        if (free.isEmpty()) {
            return "full";
        }
        Optional<PatternStorageBlockEntity> storage = storageAt(level, network, free.get());
        if (storage.isEmpty()) {
            return "full";
        }
        storage.get().set(free.get().slot(), pattern);
        return "";
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
