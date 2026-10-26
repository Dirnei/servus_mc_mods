package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import io.github.dirnei.mc.servus.foundation.client.Scrollbar;
import io.github.dirnei.mc.servus.shuttles.outbound.ClaimExportPayload;
import io.github.dirnei.mc.servus.shuttles.outbound.ExportListPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Claim an export picker over the Outbound Station GUI: a search box,
 * the exports no station holds, then those other stations hold with their
 * label, and Cancel. Picking one claims it for the keep slot the picker was
 * opened for.
 */
final class ExportPicker {

    static final int WIDTH = 168;
    static final int HEIGHT = 136;
    private static final int ROW = 18;
    private static final int ROWS = 4;
    private static final int LIST_Y = 40;
    /** Right edge of the rows, left of the scrollbar. */
    private static final int ROW_RIGHT = WIDTH - 13;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_HINT = 0xFF555555;
    private static final String KEY = "gui.servus_shuttles.outbound.claim.";

    /** One line of the list: a heading, or an export with its item and where it is held. */
    private record Line(Component heading, ExportListPayload.Offer offer) {
    }

    private final Supplier<ExportListPayload> exports;
    private EditBox search;
    private Button cancel;
    private int x;
    private int y;
    private int slot = -1;
    private final Scrollbar bar = new Scrollbar();

    ExportPicker(Supplier<ExportListPayload> exports) {
        this.exports = exports;
    }

    /** Builds the search box and Cancel for a picker centred on the GUI at {@code left}, {@code top}. */
    List<AbstractWidget> build(Font font, int left, int top, int guiWidth, int guiHeight) {
        x = left + (guiWidth - WIDTH) / 2;
        y = top + (guiHeight - HEIGHT) / 2;
        String kept = search == null ? "" : search.getValue();
        search = new EditBox(font, x + 6, y + 18, WIDTH - 12, 12, Component.translatable(KEY + "search"));
        search.setHint(Component.translatable(KEY + "search"));
        search.setMaxLength(32);
        search.setValue(kept);
        search.setResponder(text -> bar.setOffset(0));
        cancel = Button.builder(Component.translatable("gui.servus_shuttles.terminal.cancel"), b -> close())
                .bounds(x + WIDTH - 56, y + HEIGHT - 20, 50, 14).build();
        bar.setArea(x + WIDTH - 9, y + LIST_Y + 1, Scrollbar.NARROW, ROWS * ROW - 2);
        setVisible(isOpen());
        return List.of(search, cancel);
    }

    boolean isOpen() {
        return slot >= 0;
    }

    EditBox search() {
        return search;
    }

    /** Opens the picker for a keep slot. */
    void open(int keepSlot) {
        slot = keepSlot;
        bar.mouseReleased();
        bar.setOffset(0);
        search.setValue("");
        setVisible(true);
    }

    void close() {
        slot = -1;
        setVisible(false);
    }

    private void setVisible(boolean visible) {
        search.visible = visible;
        cancel.visible = visible;
        search.setFocused(visible);
    }

    private List<Line> lines() {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Line> free = new ArrayList<>();
        List<Line> held = new ArrayList<>();
        for (ExportListPayload.Offer offer : exports.get().offers()) {
            String text = (offer.item().getHoverName().getString() + " " + offer.holder().getString()).toLowerCase(Locale.ROOT);
            if (query.isEmpty() || text.contains(query)) {
                (offer.held() ? held : free).add(new Line(null, offer));
            }
        }
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.translatable(KEY + "free"), null));
        if (free.isEmpty()) {
            lines.add(new Line(Component.translatable(exports.get().offers().isEmpty() ? KEY + "nothing" : KEY + "none_match"), null));
        }
        lines.addAll(free);
        if (!held.isEmpty()) {
            lines.add(new Line(Component.translatable(KEY + "held"), null));
            lines.addAll(held);
        }
        return lines;
    }

    /** The panel over the GUI, then its widgets again on top. */
    void render(GuiGraphicsExtractor graphics, Font font, int dimX, int dimY, int dimW, int dimH, int mouseX, int mouseY) {
        PopupPanel.draw(graphics, dimX, dimY, dimW, dimH, x, y, WIDTH, HEIGHT);
        graphics.text(font, Component.translatable(KEY + "title"), x + 6, y + 6, COLOUR_TEXT, false);
        List<Line> lines = lines();
        bar.setContent(lines.size(), ROWS);
        int scroll = bar.offset();
        ExportListPayload.Offer hovered = null;
        for (int i = 0; i < ROWS && scroll + i < lines.size(); i++) {
            Line line = lines.get(scroll + i);
            int rowY = y + LIST_Y + i * ROW;
            if (line.offer() == null) {
                graphics.text(font, font.plainSubstrByWidth(line.heading().getString(), ROW_RIGHT - 6), x + 6, rowY + 5, COLOUR_HINT, false);
                continue;
            }
            boolean over = overRow(mouseX, mouseY, rowY);
            graphics.fill(x + 4, rowY, x + ROW_RIGHT, rowY + ROW - 1, over ? 0xFFD8D8D8 : 0xFFB5B5B5);
            graphics.item(line.offer().item(), x + 6, rowY + 1);
            Component name = line.offer().held()
                    ? Component.translatable(KEY + "held_at", line.offer().item().getHoverName(), line.offer().holder())
                    : line.offer().item().getHoverName();
            Component above = Component.translatable(KEY + "above", StorageTerminalScreen.abbreviate(line.offer().threshold()));
            int aboveWidth = font.width(above);
            graphics.text(font, font.plainSubstrByWidth(name.getString(), WIDTH - 43 - aboveWidth), x + 26, rowY + 5, COLOUR_TEXT, false);
            graphics.text(font, above, x + WIDTH - 15 - aboveWidth, rowY + 5, COLOUR_TEXT, false);
            if (over) {
                hovered = line.offer();
            }
        }
        bar.render(graphics, mouseX, mouseY);
        search.extractRenderState(graphics, mouseX, mouseY, 0);
        cancel.extractRenderState(graphics, mouseX, mouseY, 0);
        if (hovered != null) {
            graphics.setTooltipForNextFrame(font, Component.translatable(KEY + "pick_hint"), mouseX, mouseY);
        }
    }

    /** A click while the picker is open: Cancel, the search box or a row; the rest of the GUI takes no clicks. */
    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (cancel.mouseClicked(event, doubleClick) || search.mouseClicked(event, doubleClick)) {
            return true;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (bar.mouseClicked(mouseX, mouseY, event.button())) {
            return true;
        }
        List<Line> lines = lines();
        int scroll = bar.offset();
        for (int i = 0; i < ROWS && scroll + i < lines.size(); i++) {
            Line line = lines.get(scroll + i);
            int rowY = y + LIST_Y + i * ROW;
            if (line.offer() != null && overRow(mouseX, mouseY, rowY)) {
                ClientPacketDistributor.sendToServer(new ClaimExportPayload(slot, line.offer().item()));
                close();
                return true;
            }
        }
        return true;
    }

    private boolean overRow(double mouseX, double mouseY, int rowY) {
        return mouseX >= x + 4 && mouseX < x + ROW_RIGHT && mouseY >= rowY && mouseY < rowY + ROW;
    }

    /** True while the scrollbar holds a drag. */
    boolean mouseDragged(double mouseY) {
        return bar.mouseDragged(mouseY);
    }

    /** True when the release ended a scrollbar drag. */
    boolean mouseReleased() {
        return bar.mouseReleased();
    }

    void mouseScrolled(double scrollY) {
        bar.setContent(lines().size(), ROWS);
        bar.scrollBy(-(int) Math.signum(scrollY));
    }
}
