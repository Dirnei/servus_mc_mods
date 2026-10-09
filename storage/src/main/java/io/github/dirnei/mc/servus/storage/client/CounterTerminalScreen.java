package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.terminal.CancelRequestPayload;
import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewRequestPayload;
import io.github.dirnei.mc.servus.storage.terminal.CraftStartPayload;
import io.github.dirnei.mc.servus.storage.terminal.PushCarriedPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestItemPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import io.github.dirnei.mc.servus.storage.terminal.SetStockRulePayload;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Counter;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Search and filter, stock grid with craftable items, open requests, output
 * slots and the player inventory; an amount prompt for requests and rules and
 * a craft dialog with a server preview. Everything sits where {@link Counter} says.
 */
public class CounterTerminalScreen extends AbstractContainerScreen<CounterTerminalMenu> {

    private static final int COLUMNS = Counter.COLUMNS;
    private static final int ROWS = Counter.ROWS;
    private static final int CELL = TerminalLayout.CELL;
    private static final int REQUEST_ROWS = Counter.REQUESTS.h() / Counter.REQUEST_ROW;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_DIM = 0x707070;
    private static final int COLOUR_ALERT = 0xB03030;
    private static final int COLOUR_GOOD = 0x2E7D32;
    private static final int PREVIEW_ROWS = 3;

    /** Which entries the grid shows. */
    private enum Filter {
        ALL, STOCK, CRAFTABLE;

        Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** One grid cell: a stocked item, a craftable one or both. */
    private record Cell(ItemStack stack, long count, boolean stocked, boolean craftable) {
    }

    private enum Dialog { NONE, REQUEST, RULE, CRAFT }

    private EditBox search;
    private Button filterButton;
    private Filter filter = Filter.ALL;
    private final List<Cell> cells = new ArrayList<>();
    private int cellsVersion = -1;
    private String cellsQuery = "";
    private Filter cellsFilter = Filter.ALL;
    private int scrollRow;
    private int requestScroll;

    private Dialog dialog = Dialog.NONE;
    private ItemStack subject = ItemStack.EMPTY;
    private EditBox amount;
    private EditBox maximum;
    private int askedAmount = -1;
    private final List<Button> amountButtons = new ArrayList<>();
    private Button confirmButton;
    private Button ruleButton;
    private Button removeRuleButton;
    private Button cancelButton;

    public CounterTerminalScreen(CounterTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = Counter.WIDTH;
        this.imageHeight = Counter.HEIGHT;
    }

    private int dx() {
        return leftPos + Counter.DIALOG.x();
    }

    private int dy() {
        return topPos + Counter.DIALOG.y();
    }

    @Override
    protected void init() {
        super.init();
        // Every show, also after another screen was in front, registers the terminal again.
        ClientStock.open(menu.terminal().getBlockPos());
        titleLabelX = Counter.TITLE.x();
        titleLabelY = Counter.TITLE.y();
        inventoryLabelX = Counter.INVENTORY_LABEL.x();
        inventoryLabelY = Counter.INVENTORY_LABEL.y();
        search = new EditBox(font, leftPos + Counter.SEARCH.x(), topPos + Counter.SEARCH.y(), Counter.SEARCH.w(), Counter.SEARCH.h(),
                Component.translatable("gui.servus_storage.search"));
        search.setMaxLength(50);
        search.setHint(Component.translatable("gui.servus_storage.search"));
        search.setResponder(text -> scrollRow = 0);
        addRenderableWidget(search);
        setInitialFocus(search);
        filterButton = addRenderableWidget(Button.builder(filterLabel(), b -> {
                    filter = filter.next();
                    scrollRow = 0;
                    b.setMessage(filterLabel());
                })
                .bounds(leftPos + Counter.FILTER.x(), topPos + Counter.FILTER.y(), Counter.FILTER.w(), Counter.FILTER.h()).build());

        amount = new EditBox(font, dx() + 50, dy() + 22, 50, 12, Component.translatable("gui.servus_storage.terminal.amount"));
        amount.setMaxLength(6);
        amount.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        amount.setResponder(text -> askPreview());
        addRenderableWidget(amount);
        maximum = new EditBox(font, dx() + 160, dy() + 22, 50, 12, Component.translatable("gui.servus_storage.terminal.rule_max"));
        maximum.setMaxLength(6);
        maximum.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        addRenderableWidget(maximum);
        amountButtons.clear();
        amountButtons.add(addRenderableWidget(Button.builder(Component.literal("+1"), b -> bump(1))
                .bounds(dx() + 104, dy() + 21, 28, 14).build()));
        amountButtons.add(addRenderableWidget(Button.builder(Component.literal("+10"), b -> bump(10))
                .bounds(dx() + 134, dy() + 21, 28, 14).build()));
        amountButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.plus_stack"),
                b -> bump(subject.getMaxStackSize())).bounds(dx() + 164, dy() + 21, 44, 14).build()));
        confirmButton = addRenderableWidget(Button.builder(Component.empty(), b -> confirm())
                .bounds(dx() + 118, dy() + 77, 50, 14).build());
        ruleButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggleRule())
                .bounds(dx() + 170, dy() + 77, 50, 14).build());
        removeRuleButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.rule_remove"),
                b -> removeRule()).bounds(dx() + 4, dy() + 77, 80, 14).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.cancel"),
                b -> closeDialog()).bounds(dx() + 222, dy() + 77, 48, 14).build());
        applyDialog();
    }

    private Component filterLabel() {
        return Component.translatable("gui.servus_storage.terminal.filter." + filter.name().toLowerCase(Locale.ROOT));
    }

    // Dialogs

    private boolean dialogOpen() {
        return dialog != Dialog.NONE;
    }

    private void openRequest(ItemStack stack) {
        subject = stack.copyWithCount(1);
        dialog = Dialog.REQUEST;
        amount.setValue(Integer.toString(stack.getMaxStackSize()));
        applyDialog();
        focusAmount();
    }

    private void openCraft(ItemStack stack) {
        subject = stack.copyWithCount(1);
        dialog = Dialog.CRAFT;
        ClientStock.clearPreview();
        askedAmount = -1;
        amount.setValue("1");
        applyDialog();
        focusAmount();
        askPreview();
    }

    private void focusAmount() {
        setFocused(amount);
        amount.setFocused(true);
        search.setFocused(false);
    }

    private void toggleRule() {
        if (dialog == Dialog.RULE) {
            dialog = Dialog.REQUEST;
            amount.setValue(Integer.toString(subject.getMaxStackSize()));
        } else {
            dialog = Dialog.RULE;
            RuleRow rule = ClientStock.ruleFor(subject);
            amount.setValue(rule == null ? "0" : Integer.toString(rule.min()));
            maximum.setValue(rule == null ? Integer.toString(subject.getMaxStackSize()) : Integer.toString(rule.max()));
        }
        applyDialog();
    }

    private void applyDialog() {
        boolean open = dialogOpen();
        search.visible = !open;
        filterButton.visible = !open;
        amount.visible = open;
        maximum.visible = dialog == Dialog.RULE;
        amountButtons.forEach(button -> button.visible = dialog == Dialog.REQUEST || dialog == Dialog.CRAFT);
        confirmButton.visible = open;
        confirmButton.setMessage(Component.translatable(switch (dialog) {
            case RULE -> "gui.servus_storage.terminal.rule_save";
            case CRAFT -> "gui.servus_storage.terminal.craft_start";
            default -> "gui.servus_storage.terminal.request";
        }));
        ruleButton.visible = dialog == Dialog.REQUEST || dialog == Dialog.RULE;
        ruleButton.setMessage(Component.translatable(dialog == Dialog.RULE ? "gui.servus_storage.terminal.request" : "gui.servus_storage.terminal.rule"));
        removeRuleButton.visible = dialog == Dialog.RULE;
        cancelButton.visible = open;
    }

    private void closeDialog() {
        dialog = Dialog.NONE;
        subject = ItemStack.EMPTY;
        amount.setFocused(false);
        maximum.setFocused(false);
        ClientStock.clearPreview();
        applyDialog();
    }

    private int enteredAmount() {
        try {
            return Math.max(0, Integer.parseInt(amount.getValue().trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void bump(int by) {
        amount.setValue(Integer.toString(Math.min(99999, enteredAmount() + by)));
    }

    /** Asks the server for a preview of the amount in the field; answers arrive as {@link CraftPreviewPayload}. */
    private void askPreview() {
        if (dialog != Dialog.CRAFT || subject.isEmpty()) {
            return;
        }
        int wanted = enteredAmount();
        if (wanted > 0 && wanted != askedAmount) {
            askedAmount = wanted;
            PacketDistributor.sendToServer(new CraftPreviewRequestPayload(subject, wanted));
        }
    }

    /** The preview for exactly what the dialog shows, or null while it is on its way. */
    private CraftPreviewPayload currentPreview() {
        CraftPreviewPayload preview = ClientStock.preview();
        if (preview == null || preview.amount() != enteredAmount() || !ItemStack.isSameItemSameComponents(preview.item(), subject)) {
            return null;
        }
        return preview;
    }

    private void confirm() {
        int wanted = enteredAmount();
        switch (dialog) {
            case REQUEST -> {
                if (wanted > 0) {
                    PacketDistributor.sendToServer(new RequestItemPayload(subject, wanted));
                }
            }
            case RULE -> {
                int max;
                try {
                    max = Math.max(wanted, Integer.parseInt(maximum.getValue().trim()));
                } catch (NumberFormatException e) {
                    max = wanted;
                }
                PacketDistributor.sendToServer(new SetStockRulePayload(subject, wanted, max));
            }
            case CRAFT -> {
                CraftPreviewPayload preview = currentPreview();
                if (preview == null || !preview.startable()) {
                    return;
                }
                PacketDistributor.sendToServer(new CraftStartPayload(subject, wanted));
            }
            default -> {
            }
        }
        closeDialog();
    }

    private void removeRule() {
        if (!subject.isEmpty()) {
            PacketDistributor.sendToServer(new SetStockRulePayload(subject, -1, -1));
        }
        closeDialog();
    }

    // Grid

    private List<Cell> cells() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        if (cellsVersion != ClientStock.version() || !query.equals(cellsQuery) || cellsFilter != filter) {
            cells.clear();
            List<Cell> all = new ArrayList<>();
            for (StockEntry entry : ClientStock.entries()) {
                all.add(new Cell(entry.stack(), entry.count(), true, ClientStock.craftable(entry.stack())));
            }
            for (ItemStack craftable : ClientStock.craftables()) {
                boolean stocked = ClientStock.entries().stream().anyMatch(entry -> ItemStack.isSameItemSameComponents(entry.stack(), craftable));
                if (!stocked) {
                    all.add(new Cell(craftable, 0, false, true));
                }
            }
            for (Cell cell : all) {
                boolean shown = switch (filter) {
                    case ALL -> true;
                    case STOCK -> cell.stocked();
                    case CRAFTABLE -> cell.craftable();
                };
                if (shown && (query.isEmpty() || cell.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))) {
                    cells.add(cell);
                }
            }
            cellsVersion = ClientStock.version();
            cellsQuery = query;
            cellsFilter = filter;
            scrollRow = Math.min(scrollRow, maxScrollRow());
        }
        return cells;
    }

    private int maxScrollRow() {
        return Math.max(0, (cells.size() + COLUMNS - 1) / COLUMNS - ROWS);
    }

    private int cellAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (!Counter.GRID.contains(x, y)) {
            return -1;
        }
        return ((y - Counter.GRID.y()) / CELL + scrollRow) * COLUMNS + (x - Counter.GRID.x()) / CELL;
    }

    private Optional<Cell> cellUnder(double mouseX, double mouseY) {
        int index = cellAt(mouseX, mouseY);
        List<Cell> list = cells();
        return index >= 0 && index < list.size() ? Optional.of(list.get(index)) : Optional.empty();
    }

    private int requestRowAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (!Counter.REQUESTS.contains(x, y)) {
            return -1;
        }
        int row = (y - Counter.REQUESTS.y()) / Counter.REQUEST_ROW + requestScroll;
        return row < ClientStock.requests().size() ? row : -1;
    }

    // Drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        PanelPainter.slots(graphics, leftPos, topPos, Counter.GRID, COLUMNS, COLUMNS * ROWS);
        PanelPainter.inset(graphics, leftPos, topPos, Counter.SCROLLBAR);
        PanelPainter.inset(graphics, leftPos, topPos, Counter.REQUESTS);
        PanelPainter.slots(graphics, leftPos, topPos, Counter.OUTPUT, Counter.OUTPUT_COLUMNS, menu.inventoryStart());
        PanelPainter.slots(graphics, leftPos, topPos, Counter.INVENTORY, 9, 27);
        PanelPainter.slots(graphics, leftPos, topPos, Counter.HOTBAR, 9, 9);
        int max = maxScrollRow();
        int track = Counter.SCROLLBAR.h() - 15;
        int thumb = topPos + Counter.SCROLLBAR.y() + (max == 0 ? 0 : track * scrollRow / max);
        int thumbX = leftPos + Counter.SCROLLBAR.x();
        graphics.fill(thumbX, thumb, thumbX + Counter.SCROLLBAR.w(), thumb + 15, max == 0 ? 0xFF9A9A9A : 0xFFE0E0E0);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.requests"),
                Counter.REQUESTS_LABEL.x(), Counter.REQUESTS_LABEL.y(), COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.output"),
                Counter.OUTPUT_LABEL.x(), Counter.OUTPUT_LABEL.y(), COLOUR_TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!ClientStock.loaded() || !ClientStock.online()) {
            Component text = !ClientStock.loaded() ? Component.translatable("gui.servus_storage.terminal.loading")
                    : Component.translatable("gui.servus_storage.terminal." + ClientStock.reason());
            Rect grid = Counter.GRID;
            graphics.drawCenteredString(font, text, leftPos + grid.x() + grid.w() / 2, topPos + grid.y() + grid.h() / 2 - 4, 0xFFFFFF);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        if (dialogOpen()) {
            renderDialog(graphics, mouseX, mouseY);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        renderGrid(graphics, mouseX, mouseY);
        renderRequests(graphics, mouseX, mouseY);
        Optional<Cell> hovered = cellUnder(mouseX, mouseY);
        if (hovered.isPresent()) {
            graphics.renderTooltip(font, cellTooltip(hovered.get()), Optional.empty(), mouseX, mouseY);
        } else if (requestRowAt(mouseX, mouseY) >= 0) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.terminal.cancel_hint"), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private List<Component> cellTooltip(Cell cell) {
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), cell.stack()));
        if (cell.stocked()) {
            lines.add(Component.translatable("gui.servus_storage.stored", cell.count()));
            RuleRow rule = ClientStock.ruleFor(cell.stack());
            if (rule != null) {
                lines.add(Component.translatable("gui.servus_storage.terminal.rule_line", rule.min(), rule.max(),
                        Component.translatable("gui.servus_storage.terminal.rule_state." + rule.state())));
            }
            lines.add(Component.translatable("gui.servus_storage.terminal.click_hint"));
            if (cell.craftable()) {
                lines.add(Component.translatable("gui.servus_storage.terminal.craftable_hint"));
            }
        } else {
            lines.add(Component.translatable("gui.servus_storage.terminal.craft_hint"));
        }
        return lines;
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Cell> list = cells();
        int first = scrollRow * COLUMNS;
        for (int i = 0; i < COLUMNS * ROWS && first + i < list.size(); i++) {
            Cell cell = list.get(first + i);
            int x = leftPos + Counter.GRID.x() + (i % COLUMNS) * CELL;
            int y = topPos + Counter.GRID.y() + (i / COLUMNS) * CELL;
            graphics.renderItem(cell.stack(), x, y);
            if (cell.stocked()) {
                graphics.renderItemDecorations(font, cell.stack(), x, y, abbreviate(cell.count()));
            } else {
                smallText(graphics, Component.translatable("gui.servus_storage.terminal.craft_cell"), x + 16, y + 11, 0xFFFFE080);
            }
            if (ClientStock.ruleFor(cell.stack()) != null) {
                graphics.fill(x, y, x + 3, y + 3, 0xFF40A0FF);
            }
            if (cell.stocked() && cell.craftable()) {
                graphics.fill(x, y + 13, x + 3, y + 16, 0xFFFFB040);
            }
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= first && cell < first + COLUMNS * ROWS) {
            int x = leftPos + Counter.GRID.x() + ((cell - first) % COLUMNS) * CELL;
            int y = topPos + Counter.GRID.y() + ((cell - first) / COLUMNS) * CELL;
            graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
    }

    /** Half size text right aligned at {@code right}, drawn above items. */
    private void smallText(GuiGraphics graphics, Component text, int right, int y, int colour) {
        graphics.pose().pushPose();
        graphics.pose().translate(right - font.width(text) * 0.5F, y, 200);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, text, 0, 0, colour, true);
        graphics.pose().popPose();
    }

    private void renderRequests(GuiGraphics graphics, int mouseX, int mouseY) {
        Rect area = Counter.REQUESTS;
        List<RequestRow> rows = ClientStock.requests();
        int hovered = requestRowAt(mouseX, mouseY);
        if (rows.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.no_requests"),
                    leftPos + area.x() + 2, topPos + area.y() + 2, 0xE0E0E0, false);
        }
        for (int i = 0; i < REQUEST_ROWS && requestScroll + i < rows.size(); i++) {
            int index = requestScroll + i;
            RequestRow row = rows.get(index);
            int y = topPos + area.y() + i * Counter.REQUEST_ROW;
            int left = leftPos + area.x();
            if (index == hovered) {
                graphics.fill(left, y, left + area.w(), y + Counter.REQUEST_ROW, 0x30FFFFFF);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(left + 1, y + 1, 0);
            graphics.pose().scale(0.625F, 0.625F, 1.0F);
            graphics.renderItem(row.stack(), 0, 0);
            graphics.pose().popPose();
            graphics.drawString(font, row.delivered() + "/" + row.requested(), left + 13, y + 2, 0xFFFFFF, false);
            Component state = row.state().equals("MISSING")
                    ? Component.translatable("gui.servus_storage.terminal.state.missing", row.missing())
                    : Component.translatable("gui.servus_storage.terminal.state." + row.state().toLowerCase(Locale.ROOT));
            int colour = row.state().equals("MISSING") ? 0xFFFF8080 : 0xFFE0E0E0;
            smallText(graphics, state, left + area.w() - 2, y + 4, colour);
        }
        if (!ClientStock.message().isEmpty()) {
            Component text = Component.translatable("gui.servus_storage.terminal.message." + ClientStock.message());
            graphics.drawString(font, font.plainSubstrByWidth(text.getString(), Counter.MESSAGE.w()),
                    leftPos + Counter.MESSAGE.x(), topPos + Counter.MESSAGE.y() + 1, COLOUR_ALERT, false);
        }
    }

    private void renderDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        Rect area = Counter.DIALOG;
        int x = dx();
        int y = dy();
        graphics.fill(x - 1, y - 1, x + area.w() + 1, y + area.h() + 1, 0xFF373737);
        graphics.fill(x, y, x + area.w(), y + area.h(), 0xFFC6C6C6);
        graphics.renderItem(subject, x + 4, y + 3);
        Component head = dialog == Dialog.CRAFT
                ? Component.translatable("gui.servus_storage.terminal.craft_title", subject.getHoverName())
                : subject.getHoverName();
        graphics.drawString(font, head, x + 24, y + 7, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable(dialog == Dialog.RULE ? "gui.servus_storage.terminal.rule_min"
                : "gui.servus_storage.terminal.amount"), x + 4, y + 24, COLOUR_TEXT, false);
        if (dialog == Dialog.RULE) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.rule_max"), x + 130, y + 24, COLOUR_TEXT, false);
        }
        // Widgets were drawn by super.render; draw them again over the dialog background.
        for (var widget : List.of(amount, maximum, cancelButton, confirmButton, ruleButton, removeRuleButton)) {
            if (widget.visible) {
                widget.render(graphics, mouseX, mouseY, 0);
            }
        }
        amountButtons.forEach(button -> {
            if (button.visible) {
                button.render(graphics, mouseX, mouseY, 0);
            }
        });
        if (dialog == Dialog.CRAFT) {
            renderPreview(graphics, x, y);
        }
    }

    private void renderPreview(GuiGraphics graphics, int x, int y) {
        CraftPreviewPayload preview = currentPreview();
        confirmButton.active = preview != null && preview.startable();
        if (preview == null) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.craft_wait"), x + 4, y + 81, COLOUR_DIM, false);
            return;
        }
        previewColumn(graphics, x + 4, y + 38, "gui.servus_storage.terminal.craft_from_stock", preview.fromStock(), COLOUR_TEXT);
        previewColumn(graphics, x + 94, y + 38, "gui.servus_storage.terminal.craft_to_craft", preview.toCraft(), COLOUR_GOOD);
        previewColumn(graphics, x + 184, y + 38, "gui.servus_storage.terminal.craft_missing", preview.missing(), COLOUR_ALERT);
        Component status;
        int colour;
        if (!preview.refusal().isEmpty()) {
            status = Component.translatable("gui.servus_storage.terminal.message." + preview.refusal());
            colour = COLOUR_ALERT;
        } else if (!preview.missing().isEmpty()) {
            status = Component.translatable("gui.servus_storage.terminal.message.missing");
            colour = COLOUR_ALERT;
        } else {
            status = Component.translatable("gui.servus_storage.terminal.craft_ready");
            colour = COLOUR_GOOD;
        }
        graphics.drawString(font, font.plainSubstrByWidth(status.getString(), 110), x + 4, y + 81, colour, false);
    }

    private void previewColumn(GuiGraphics graphics, int x, int y, String heading, List<StockEntry> rows, int colour) {
        graphics.drawString(font, Component.translatable(heading), x, y, colour, false);
        for (int i = 0; i < Math.min(PREVIEW_ROWS, rows.size()); i++) {
            StockEntry row = rows.get(i);
            int rowY = y + 10 + i * 9;
            graphics.pose().pushPose();
            graphics.pose().translate(x, rowY, 0);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.renderItem(row.stack(), 0, 0);
            graphics.pose().popPose();
            String text = row.count() + " " + row.stack().getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(text, 76), x + 10, rowY, COLOUR_TEXT, false);
        }
        if (rows.size() > PREVIEW_ROWS) {
            smallText(graphics, Component.translatable("gui.servus_storage.terminal.craft_more", rows.size() - PREVIEW_ROWS),
                    x + 86, y, COLOUR_DIM);
        }
    }

    static String abbreviate(long count) {
        if (count < 1000) {
            return Long.toString(count);
        }
        if (count < 10000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        }
        if (count < 1000000) {
            return (count / 1000) + "k";
        }
        return String.format(Locale.ROOT, "%.1fM", count / 1000000.0);
    }

    // Input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dialogOpen() || !ClientStock.online()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (cellAt(mouseX, mouseY) >= 0) {
            if (!menu.getCarried().isEmpty()) {
                PacketDistributor.sendToServer(PushCarriedPayload.INSTANCE);
                return true;
            }
            Optional<Cell> cell = cellUnder(mouseX, mouseY);
            if (cell.isPresent()) {
                Cell found = cell.get();
                boolean craftClick = button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || hasControlDown();
                if (!found.stocked() || (found.craftable() && craftClick)) {
                    openCraft(found.stack());
                } else if (hasShiftDown()) {
                    PacketDistributor.sendToServer(new RequestItemPayload(found.stack(), found.stack().getMaxStackSize()));
                } else {
                    openRequest(found.stack());
                }
                return true;
            }
        }
        int row = requestRowAt(mouseX, mouseY);
        if (row >= 0) {
            PacketDistributor.sendToServer(new CancelRequestPayload(ClientStock.requests().get(row).id()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (Counter.REQUESTS.contains(x, y)) {
            int maxRow = Math.max(0, ClientStock.requests().size() - REQUEST_ROWS);
            requestScroll = Math.max(0, Math.min(maxRow, requestScroll - (int) Math.signum(scrollY)));
            return true;
        }
        if (!dialogOpen()) {
            cells();
            scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(scrollY)));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (dialogOpen()) {
                closeDialog();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (dialogOpen()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirm();
                return true;
            }
            if (maximum.isFocused() && (maximum.keyPressed(keyCode, scanCode, modifiers) || maximum.canConsumeInput())) {
                return true;
            }
            amount.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (search.isFocused() && (search.keyPressed(keyCode, scanCode, modifiers) || search.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        ClientStock.close();
    }
}
