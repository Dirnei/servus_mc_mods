package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.TreeTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The entries of the Field Head's sapling picker in a Trees field: "None,
 * fell only" and every sapling that places a block, mods' saplings included;
 * a sapling that grows big trees gets a second entry for 2x2 squares, one
 * that grows only from squares has only that entry.
 */
final class SaplingPickerEntries {

    /** Appended to a sapling's item id for its 2x2 entry. */
    private static final String SQUARE = "|2x2";

    private SaplingPickerEntries() {
    }

    static List<ItemPicker.Entry> build() {
        List<ItemPicker.Entry> entries = new ArrayList<>();
        entries.add(ItemPicker.Entry.none(Component.translatable("gui.servus_shuttles.field_head.sapling_none")));
        for (Item item : FieldHeadBlockEntity.saplingItems()) {
            ItemStack stack = new ItemStack(item);
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (!stack.is(TreeTags.SAPLINGS_2X2_ONLY)) {
                entries.add(new ItemPicker.Entry(id, stack, stack.getHoverName()));
            }
            if (stack.is(TreeTags.SAPLINGS_2X2)) {
                entries.add(new ItemPicker.Entry(id + SQUARE, stack,
                        Component.translatable("gui.servus_shuttles.field_head.sapling_2x2", stack.getHoverName())));
            }
        }
        return entries;
    }

    /** The picker id of the sapling in that layout. */
    static String idOf(ItemStack sapling, boolean twoByTwo) {
        String id = ItemPicker.Entry.idOf(sapling);
        return twoByTwo && !sapling.isEmpty() ? id + SQUARE : id;
    }

    /** Whether the entry is a 2x2 entry. */
    static boolean square(ItemPicker.Entry entry) {
        return entry.id().endsWith(SQUARE);
    }

    /** The item id an entry names, empty for the none entry. */
    static Optional<Identifier> itemId(ItemPicker.Entry entry) {
        if (entry.isNone()) {
            return Optional.empty();
        }
        String id = square(entry) ? entry.id().substring(0, entry.id().length() - SQUARE.length()) : entry.id();
        return Optional.ofNullable(Identifier.tryParse(id));
    }

    /** The 2x2 entries, each with the hint that it needs a Range Upgrade. */
    static Map<String, Component> squaresLocked(List<ItemPicker.Entry> entries) {
        Map<String, Component> locked = new HashMap<>();
        for (ItemPicker.Entry entry : entries) {
            if (square(entry)) {
                locked.put(entry.id(), Component.translatable("gui.servus_shuttles.field_head.sapling_needs_range"));
            }
        }
        return locked;
    }

    /** The menu's stock keyed by picker id, the 2x2 entries counting their sapling too; null stays null. */
    static Map<String, Long> keyed(Map<Identifier, Long> stock) {
        if (stock == null) {
            return null;
        }
        Map<String, Long> keyed = new HashMap<>();
        stock.forEach((id, count) -> {
            keyed.put(id.toString(), count);
            keyed.put(id + SQUARE, count);
        });
        return keyed;
    }
}
