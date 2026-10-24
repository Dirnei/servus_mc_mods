package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ui.ListScroll;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A heading over a scrollable list of items: each row a half size icon, then
 * the count and the item name cut to the width. A thin scrollbar shows while
 * there are more rows than fit; the mouse wheel over the list scrolls it one
 * row at a time, and a hovered row shows the item's tooltip.
 *
 * <p>Use: {@link #setArea} with absolute coordinates (the height includes the
 * heading line), give it rows whenever they change, call {@link #render} from
 * the screen and forward the mouse wheel to {@link #mouseScrolled}.
 */
public final class ItemListView {

    public static final int LINE = 9;
    private static final int ICON = 8;
    private static final int TEXT_X = ICON + 2;
    private static final int BAR = 2;
    private static final int TRACK = 0x40000000;
    private static final int THUMB = 0xFF707070;

    /** One row: the item and how many. */
    public record Row(ItemStack stack, long count) {
    }

    private final Component heading;
    private List<Row> rows = List.of();
    private int offset;
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
        offset = ListScroll.clamp(offset, rows.size(), visible());
    }

    /** The new rows; the scroll position stays where it still fits. */
    public void setRows(List<Row> rows) {
        this.rows = List.copyOf(rows);
        offset = ListScroll.clamp(offset, this.rows.size(), visible());
    }

    /** Back to the first row. */
    public void scrollToTop() {
        offset = 0;
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
        boolean scrolls = ListScroll.scrolls(rows.size(), visible());
        int textWidth = width - TEXT_X - (scrolls ? BAR + 1 : 0);
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
        if (scrolls) {
            int track = visible() * LINE;
            int barX = x + width - BAR;
            int thumbY = listTop() + ListScroll.thumbStart(offset, rows.size(), visible(), track);
            graphics.fill(barX, listTop(), barX + BAR, listTop() + track, TRACK);
            graphics.fill(barX, thumbY, barX + BAR, thumbY + ListScroll.thumbLength(rows.size(), visible(), track), THUMB);
        }
        rowAt(mouseX, mouseY).ifPresent(row -> graphics.setTooltipForNextFrame(font, row.stack(), mouseX, mouseY));
    }

    /** Scrolls one row for a wheel turn over the list; true when it was over the list. */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!over(mouseX, mouseY) || scrollY == 0) {
            return false;
        }
        offset = ListScroll.scroll(offset, scrollY > 0 ? -1 : 1, rows.size(), visible());
        return true;
    }

    /** The row under the mouse, if any. */
    public Optional<Row> rowAt(double mouseX, double mouseY) {
        if (!over(mouseX, mouseY) || mouseY < listTop()) {
            return Optional.empty();
        }
        int index = offset + (int) (mouseY - listTop()) / LINE;
        return index < rows.size() && index - offset < visible() ? Optional.of(rows.get(index)) : Optional.empty();
    }

    private boolean over(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
