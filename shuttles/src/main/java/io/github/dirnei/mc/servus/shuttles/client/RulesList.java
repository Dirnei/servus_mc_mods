package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.terminal.RuleListing;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Storage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Storage Terminal's Rules tab in the grid's area: the add slot as a
 * fixed first row, then one row per stock rule with its item, its Make range
 * and its state, sorted by item name, filtered by the search and scrolled
 * like the grid.
 */
final class RulesList {

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_HINT = 0xFF555555;
    private static final String KEY = "gui.servus_shuttles.terminal.";

    private int scroll;

    /** The rules shown for the search, in name order. */
    List<RuleRow> shown(String query) {
        List<RuleRow> rules = ClientStock.rules();
        List<String> names = rules.stream().map(rule -> rule.stack().getHoverName().getString()).toList();
        List<RuleRow> shown = new ArrayList<>();
        for (int index : RuleListing.shown(names, query)) {
            shown.add(rules.get(index));
        }
        return shown;
    }

    int scroll() {
        return scroll;
    }

    void setScroll(int row, String query, int rows) {
        scroll = Math.max(0, Math.min(maxScroll(query, rows), row));
    }

    /** How far the rule rows scroll when {@code rows} grid rows show, one of them the add slot. */
    int maxScroll(String query, int rows) {
        return Math.max(0, shown(query).size() - (rows - 1));
    }

    /** The add slot: the first row's slot, at panel offsets. */
    static Rect addSlot(Rect grid) {
        return new Rect(grid.x(), grid.y(), 16, 16);
    }

    /** Whether a panel point is on the add slot's row. */
    static boolean overAdd(Rect grid, int x, int y) {
        return x >= grid.x() && x < grid.right() && y >= grid.y() && y < grid.y() + Storage.RULE_ROW;
    }

    /** The rule of the row at a panel point, empty on the add slot row and below the last rule. */
    Optional<RuleRow> ruleAt(Rect grid, String query, int x, int y) {
        if (!grid.contains(x, y) || overAdd(grid, x, y)) {
            return Optional.empty();
        }
        int index = (y - grid.y()) / Storage.RULE_ROW - 1 + scroll;
        List<RuleRow> shown = shown(query);
        return index >= 0 && index < shown.size() ? Optional.of(shown.get(index)) : Optional.empty();
    }

    /** The rows over the grid's sunken area, a light bar under the hovered row; absolute from {@code left}, {@code top}. */
    void render(GuiGraphicsExtractor graphics, Font font, int left, int top, Rect grid, int rows, String query, int mouseX, int mouseY) {
        Rect add = addSlot(grid);
        PanelPainter.slots(graphics, left, top, add, 1, 1);
        graphics.text(font, "+", left + add.x() + 5, top + add.y() + 4, 0xFFFFFFFF, true);
        Component hint = Component.translatable(KEY + "rules.add");
        graphics.text(font, font.plainSubstrByWidth(hint.getString(), grid.w() - 22), left + grid.x() + 20, top + grid.y() + 4, COLOUR_HINT, false);
        List<RuleRow> shown = shown(query);
        int x = mouseX - left;
        int y = mouseY - top;
        for (int row = 1; row < rows && scroll + row - 1 < shown.size(); row++) {
            RuleRow rule = shown.get(scroll + row - 1);
            int rowX = left + grid.x();
            int rowY = top + grid.y() + row * Storage.RULE_ROW;
            if (x >= grid.x() && x < grid.right() && y >= rowY - top && y < rowY - top + Storage.RULE_ROW) {
                graphics.fill(rowX, rowY, rowX + grid.w(), rowY + Storage.RULE_ROW, 0x40FFFFFF);
            }
            renderRow(graphics, font, rule, rowX, rowY);
        }
    }

    private void renderRow(GuiGraphicsExtractor graphics, Font font, RuleRow rule, int x, int y) {
        Rect icon = Storage.RULE_ROW_ICON;
        graphics.item(rule.stack(), x + icon.x(), y + icon.y());
        cell(graphics, font, Component.translatable(KEY + "rule_range", rule.min(), rule.max()), x, y, Storage.RULE_ROW_MAKE, COLOUR_TEXT);
        cell(graphics, font, Component.translatable(KEY + "rule_state." + rule.state()), x, y, Storage.RULE_ROW_STATE,
                ItemPopup.stateColour(rule.state()));
    }

    private static void cell(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, Rect at, int colour) {
        graphics.text(font, font.plainSubstrByWidth(text.getString(), at.w()), x + at.x(), y + at.y(), colour, false);
    }

    /** The tooltip of a rule row: the item and the click hint. */
    static List<Component> tooltip(RuleRow rule) {
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), rule.stack()));
        lines.add(Component.translatable(KEY + "rules.row_hint"));
        return lines;
    }
}
