package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.ItemSearchFacts;
import io.github.dirnei.mc.servus.foundation.search.SearchQuery;
import io.github.dirnei.mc.servus.shuttles.terminal.StockEntry;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSettings;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The Storage Terminal grid's cells from {@link ClientStock}: stocked and
 * craftable items in the settings' order and view, filtered by the search.
 * Rebuilt only when the stock, the search or the settings changed.
 */
final class TerminalCells {

    /** One grid cell: a stocked or craftable item. */
    record Cell(ItemStack stack, long count, boolean stocked, boolean craftable) {
    }

    private final List<Cell> cells = new ArrayList<>();
    private final ItemSearchFacts.Cache facts;
    private int version = -1;
    private String query = "";
    private TerminalSettings settings;

    /** Cells whose items are searched through {@code facts}. */
    TerminalCells(ItemSearchFacts.Cache facts) {
        this.facts = facts;
    }

    /** The cells for the search text as typed; returns whether they were rebuilt. */
    boolean refresh(String query, TerminalSettings settings) {
        if (version == ClientStock.version() && query.equals(this.query) && settings.equals(this.settings)) {
            return false;
        }
        cells.clear();
        SearchQuery search = SearchQuery.parse(query);
        List<Cell> items = items();
        Comparator<TerminalSort.Key> order = TerminalSort.comparator(settings.sort(), settings.direction());
        items.sort((a, b) -> order.compare(key(a), key(b)));
        for (Cell cell : items) {
            boolean shown = switch (settings.view()) {
                case ALL -> true;
                case STOCK -> cell.stocked();
                case CRAFTABLE -> cell.craftable();
            };
            if (shown && (search.isEmpty() || search.matches(facts.get(cell.stack())))) {
                cells.add(cell);
            }
        }
        version = ClientStock.version();
        this.query = query;
        this.settings = settings;
        return true;
    }

    List<Cell> list() {
        return cells;
    }

    /** Every stocked item, then every craftable one not in stock. */
    private static List<Cell> items() {
        List<Cell> items = new ArrayList<>();
        for (StockEntry entry : ClientStock.entries()) {
            items.add(new Cell(entry.stack(), entry.count(), true, ClientStock.craftable(entry.stack())));
        }
        for (ItemStack craftable : ClientStock.craftables()) {
            boolean stocked = ClientStock.entries().stream().anyMatch(entry -> ItemStack.isSameItemSameComponents(entry.stack(), craftable));
            if (!stocked) {
                items.add(new Cell(craftable, 0, false, true));
            }
        }
        return items;
    }

    private static TerminalSort.Key key(Cell cell) {
        Identifier id = BuiltInRegistries.ITEM.getKey(cell.stack().getItem());
        return new TerminalSort.Key(cell.stack().getHoverName().getString(), cell.stocked() ? cell.count() : 0, id.getNamespace());
    }
}
