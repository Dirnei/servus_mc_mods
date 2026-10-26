package io.github.dirnei.mc.servus.foundation.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A heading over a scrollable list of items: each row a half size icon, then
 * the count and the item name cut to the width, with the shared
 * {@link Scrollbar} right of the rows. The mouse wheel over the list or the
 * bar scrolls it one row at a time, the bar can be dragged, and a hovered row
 * shows the item's tooltip.
 *
 * <p>Use: {@link #setArea} with absolute coordinates (the height includes the
 * heading line), give it rows whenever they change, call {@link #render} from
 * the screen and forward the mouse to {@link #mouseClicked},
 * {@link #mouseDragged}, {@link #mouseReleased} and {@link #mouseScrolled}.
 */
public final class ItemListView {

    public static final int LINE = 9;
    private static final int ICON = 8;
    private static final int TEXT_X = ICON + 2;
    /** The bar with its frame and a gap before it. */
    private static final int BAR_SPACE = Scrollbar.NARROW + 3;

    /** One row: the item and how many. */
    public record Row(ItemStack stack, long count) {
    }

    private final Component heading;
    private final Scrollbar bar = new Scrollbar();
    private List<Row> rows = List.of();
    private int x;
    private int y;
    private int width;
    private int height;

    public ItemListView(Component heading) {
        this.heading = heading;
    }

    public void setArea(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        bar.setArea(x + width - Scrollbar.NARROW - 1, listTop() + 1, Scrollbar.NARROW, visible() * LINE - 2);
        bar.setContent(rows.size(), visible());
    }

    /** The new rows; the scroll position stays where it still fits. */
    public void setRows(List<Row> rows) {
        this.rows = List.copyOf(rows);
        bar.setContent(this.rows.size(), visible());
    }

    /** Back to the first row. */
    public void scrollToTop() {
        bar.setOffset(0);
    }

    private int visible() {
        return Math.max(0, (height - LINE) / LINE);
    }

    private int listTop() {
        return y + LINE;
    }

    /** Draws the heading, the visible rows, the scrollbar and the tooltip of the hovered row. */
    public void render(GuiGraphicsExtractor graphics, Font font, int colour, int mouseX, int mouseY) {
        graphics.text(font, font.plainSubstrByWidth(heading.getString(), width), x, y, colour, false);
        int textWidth = width - TEXT_X - BAR_SPACE;
        int offset = bar.offset();
        for (int i = 0; i < visible() && offset + i < rows.size(); i++) {
            Row row = rows.get(offset + i);
            int rowY = listTop() + i * LINE;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, rowY);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.item(row.stack(), 0, 0);
            graphics.pose().popMatrix();
            String text = row.count() + " " + row.stack().getHoverName().getString();
            graphics.text(font, font.plainSubstrByWidth(text, textWidth), x + TEXT_X, rowY, colour, false);
        }
        bar.render(graphics, mouseX, mouseY);
        rowAt(mouseX, mouseY).ifPresent(row -> graphics.setTooltipForNextFrame(font, row.stack(), mouseX, mouseY));
    }

    /** A press on the scrollbar; true when it took the press. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return bar.mouseClicked(mouseX, mouseY, button);
    }

    /** True while the scrollbar holds a drag. */
    public boolean mouseDragged(double mouseY) {
        return bar.mouseDragged(mouseY);
    }

    /** True when the release ended a scrollbar drag. */
    public boolean mouseReleased() {
        return bar.mouseReleased();
    }

    /** Scrolls one row for a wheel turn over the list or its bar; true when it was over them. */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!over(mouseX, mouseY) && !bar.contains(mouseX, mouseY) || scrollY == 0) {
            return false;
        }
        bar.scrollBy(scrollY > 0 ? -1 : 1);
        return true;
    }

    /** The row under the mouse, if any. */
    public Optional<Row> rowAt(double mouseX, double mouseY) {
        if (!over(mouseX, mouseY) || mouseY < listTop() || mouseX >= x + width - BAR_SPACE) {
            return Optional.empty();
        }
        int offset = bar.offset();
        int index = offset + (int) (mouseY - listTop()) / LINE;
        return index < rows.size() && index - offset < visible() ? Optional.of(rows.get(index)) : Optional.empty();
    }

    private boolean over(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
