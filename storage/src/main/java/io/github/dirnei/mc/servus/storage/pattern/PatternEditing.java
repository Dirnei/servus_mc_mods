package io.github.dirnei.mc.servus.storage.pattern;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
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
     * Saves the grid as a pattern: a new one encodes a Blank Pattern from the
     * player's inventory into the first free slot, an existing one is
     * rewritten in place.
     *
     * @param editing the slot being edited, or null for a new pattern
     * @return an empty string on success, else {@code empty}, {@code no_match},
     *         {@code duplicate}, {@code no_blank} or {@code full}
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
        ItemStack result = match.get().value().getResultItem(level.registryAccess());
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
        Optional<PatternLibrary.Free> free = PatternLibrary.firstFree(level, network);
        if (free.isEmpty()) {
            return "full";
        }
        int blank = blankSlot(player);
        if (blank < 0) {
            return "no_blank";
        }
        Optional<PatternStorageBlockEntity> storage = storageAt(level, network, free.get());
        if (storage.isEmpty()) {
            return "full";
        }
        player.getInventory().removeItem(blank, 1);
        storage.get().set(free.get().slot(), pattern);
        return "";
    }

    /** Removes a pattern of the line and gives the player a Blank Pattern. */
    public static boolean delete(ServerLevel level, DataNetwork network, Player player, PatternLibrary.Free slot) {
        Optional<PatternStorageBlockEntity> storage = storageAt(level, network, slot);
        if (storage.isEmpty() || storage.get().pattern(slot.slot()).isEmpty()) {
            return false;
        }
        storage.get().clear(slot.slot());
        ItemStack blank = new ItemStack(ServusStorageItems.BLANK_PATTERN.get());
        if (!player.getInventory().add(blank)) {
            player.drop(blank, false);
        }
        return true;
    }

    private static Optional<PatternStorageBlockEntity> storageAt(ServerLevel level, DataNetwork network, PatternLibrary.Free slot) {
        for (PatternStorageBlockEntity storage : PatternLibrary.storages(level, network)) {
            if (storage.getBlockPos().equals(slot.storage()) && slot.slot() >= 0 && slot.slot() < storage.patterns().getSlots()) {
                return Optional.of(storage);
            }
        }
        return Optional.empty();
    }

    /** The first inventory slot holding a Blank Pattern, or -1. */
    private static int blankSlot(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ServusStorageItems.BLANK_PATTERN.get())) {
                return slot;
            }
        }
        return -1;
    }
}
