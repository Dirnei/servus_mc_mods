package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.terminal.AmountSteps;
import io.github.dirnei.mc.servus.storage.terminal.CancelRequestPayload;
import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewRequestPayload;
import io.github.dirnei.mc.servus.storage.terminal.CraftStartPayload;
import io.github.dirnei.mc.servus.storage.terminal.GridClearPayload;
import io.github.dirnei.mc.servus.storage.terminal.GridPullPayload;
import io.github.dirnei.mc.servus.storage.terminal.PushCarriedPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestItemPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import io.github.dirnei.mc.servus.storage.terminal.SetStockRulePayload;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Counter;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import io.github.dirnei.mc.servus.storage.terminal.TerminalSettings;
import io.github.dirnei.mc.servus.storage.terminal.TerminalSort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Refined Storage style Counter Terminal: view buttons left of the panel,
 * search, the item grid with open requests first, a message line, the crafting
 * grid left of the output, the player inventory; an amount prompt for requests
 * and rules and a craft dialog with a server preview. Everything sits where
 * {@link Counter} says; extra grid rows grow the panel upwards.
 */
public class CounterTerminalScreen extends AbstractContainerScreen<CounterTerminalMenu> {

    private static final int COLUMNS = Counter.COLUMNS;
    private static final int CELL = TerminalLayout.CELL;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_DIM = 0x707070;
    private static final int COLOUR_ALERT = 0xB03030;
    private static final int COLOUR_GOOD = 0x2E7D32;
    private static final int PREVIEW_ROWS = 3;
    private static final float DIALOG_Z = 400;

    /** One grid cell: an open request, or a stocked or craftable item. */
    private record Cell(ItemStack stack, long count, boolean stocked, boolean craftable, RequestRow request) {
        boolean isRequest() {
            return request != null;
        }
    }

    private enum Dialog { NONE, REQUEST, RULE, CRAFT }

    private Counter.Layout layout = Counter.layout(Counter.BASE_ROWS);
    private EditBox search;
    private String searchText = "";
    private String lastSynced = null;
    private final List<Cell> cells = new ArrayList<>();
    private int cellsVersion = -1;
    private String cellsQuery = "";
    private TerminalSettings cellsSettings = null;
    private int scrollRow;
    private final List<IconButton> sideButtons = new ArrayList<>();

    private Dialog dialog = Dialog.NONE;
    private ItemStack subject = ItemStack.EMPTY;
    private EditBox amount;
    private EditBox maximum;
    private int askedAmount = -1;
    private final List<Button> stepButtons = new ArrayList<>();
    private Button confirmButton;
    private Button ruleButton;
    private Button removeRuleButton;
    private Button cancelButton;

    public CounterTerminalScreen(CounterTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = Counter.WIDTH;
        this.imageHeight = Counter.BASE_HEIGHT;
    }

    private TerminalSettings settings() {
        return ClientTerminalSettings.get();
    }

    private void update(TerminalSettings changed) {
        ClientTerminalSettings.set(changed);
        refreshSideButtons();
    }

    // Layout

    private Rect dialogRect() {
        return layout.dialog();
    }

    private int dx() {
        return leftPos + dialogRect().x();
    }

    private int dy() {
        return topPos + dialogRect().y();
    }

    /** The side column and the rows above the base top, for JEI to keep free. */
    public List<Rect2i> extraAreas() {
        List<Rect2i> areas = new ArrayList<>();
        int top = topPos + layout.top();
        areas.add(new Rect2i(leftPos + Counter.SIDE_X, top, -Counter.SIDE_X, layout.height()));
        if (layout.top() < 0) {
            areas.add(new Rect2i(leftPos, top, imageWidth, -layout.top()));
        }
        return areas;
    }

    @Override
    protected void init() {
        layout = Counter.layout(Counter.rowsFor(settings().height(), height));
        imageHeight = layout.height();
        super.init();
        // The panel is centred as a whole; the base top sits below the extra rows.
        topPos -= layout.top();
        // Every show, also after another screen was in front, registers the terminal again.
        ClientStock.open(menu.terminal().getBlockPos());
        titleLabelX = layout.title().x();
        titleLabelY = layout.title().y();
        inventoryLabelX = Counter.INVENTORY_LABEL.x();
        inventoryLabelY = Counter.INVENTORY_LABEL.y();

        Rect searchRect = layout.search();
        search = new EditBox(font, leftPos + searchRect.x(), topPos + searchRect.y(), searchRect.w(), searchRect.h(),
                Component.translatable("gui.servus_storage.search"));
        search.setMaxLength(50);
        search.setHint(Component.translatable("gui.servus_storage.search"));
        search.setValue(searchText);
        search.setResponder(this::searchChanged);
        addRenderableWidget(search);
        setInitialFocus(search);

        sideButtons.clear();
        List<Rect> side = layout.side();
        sideButtons.add(sideButton(side.get(0), this::sortIcon, b -> update(settings().withSort(TerminalSettings.next(settings().sort())))));
        sideButtons.add(sideButton(side.get(1), this::directionIcon,
                b -> update(settings().withDirection(TerminalSettings.next(settings().direction())))));
        sideButtons.add(sideButton(side.get(2), this::viewIcon, b -> {
            update(settings().withView(TerminalSettings.next(settings().view())));
            scrollRow = 0;
        }));
        sideButtons.add(sideButton(side.get(3), this::searchIcon, b -> update(settings().withSearch(nextSearchMode()))));
        sideButtons.add(sideButton(side.get(4), this::heightIcon, b -> {
            update(settings().withHeight(TerminalSettings.next(settings().height())));
            searchText = search.getValue();
            rebuildWidgets();
        }));
        refreshSideButtons();

        addRenderableWidget(Button.builder(Component.literal("x"), b -> PacketDistributor.sendToServer(GridClearPayload.INSTANCE))
                .bounds(leftPos + Counter.CLEAR.x(), topPos + Counter.CLEAR.y(), Counter.CLEAR.w(), Counter.CLEAR.h())
                .tooltip(Tooltip.create(Component.translatable("gui.servus_storage.terminal.clear_grid"))).build());

        amount = new EditBox(font, dx() + 40, dy() + 20, 50, 12, Component.translatable("gui.servus_storage.terminal.amount"));
        amount.setMaxLength(5);
        amount.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        amount.setResponder(text -> askPreview());
        addRenderableWidget(amount);
        maximum = new EditBox(font, dx() + 120, dy() + 20, 50, 12, Component.translatable("gui.servus_storage.terminal.rule_max"));
        maximum.setMaxLength(5);
        maximum.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        addRenderableWidget(maximum);
        stepButtons.clear();
        int[] kinds = {-2, -10, -1, 1, 10, 2};
        for (int i = 0; i < kinds.length; i++) {
            int kind = kinds[i];
            stepButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> step(kind))
                    .bounds(dx() + 4 + i * 28, dy() + 36, 26, 14).build()));
        }
        int bottom = dy() + dialogRect().h() - 17;
        removeRuleButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.rule_remove"),
                b -> removeRule()).bounds(dx() + 4, bottom, 46, 14).build());
        ruleButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggleRule())
                .bounds(dx() + 52, bottom, 38, 14).build());
        confirmButton = addRenderableWidget(Button.builder(Component.empty(), b -> confirm())
                .bounds(dx() + 92, bottom, 40, 14).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.terminal.cancel"),
                b -> closeDialog()).bounds(dx() + 134, bottom, 36, 14).build());
        applyDialog();
    }

    private IconButton sideButton(Rect rect, Supplier<String> icon, Button.OnPress press) {
        IconButton button = new IconButton(leftPos + rect.x(), topPos + rect.y(), rect.w(), rect.h(), icon, press);
        return addRenderableWidget(button);
    }

    private TerminalSettings.SearchMode nextSearchMode() {
        if (!JeiSearchBridge.available()) {
            return TerminalSettings.SearchMode.NORMAL;
        }
        return TerminalSettings.next(settings().search());
    }

    private TerminalSettings.SearchMode searchMode() {
        return JeiSearchBridge.available() ? settings().search() : TerminalSettings.SearchMode.NORMAL;
    }

    private String sortIcon() {
        return "sort_" + settings().sort().name().toLowerCase(Locale.ROOT);
    }

    private String directionIcon() {
        return "dir_" + settings().direction().name().toLowerCase(Locale.ROOT);
    }

    private String viewIcon() {
        return "view_" + settings().view().name().toLowerCase(Locale.ROOT);
    }

    private String searchIcon() {
        return "search_" + searchMode().name().toLowerCase(Locale.ROOT);
    }

    private String heightIcon() {
        return "height_" + settings().height().name().toLowerCase(Locale.ROOT);
    }

    private void refreshSideButtons() {
        String[] kinds = {"sort", "direction", "view", "search", "height"};
        String[] values = {settings().sort().name(), settings().direction().name(), settings().view().name(),
                searchMode().name(), settings().height().name()};
        for (int i = 0; i < sideButtons.size(); i++) {
            Component value = Component.translatable("gui.servus_storage.terminal.setting." + kinds[i] + "." + values[i].toLowerCase(Locale.ROOT));
            sideButtons.get(i).setTooltip(Tooltip.create(Component.translatable("gui.servus_storage.terminal.setting." + kinds[i], value)));
        }
    }

    // Search

    private void searchChanged(String text) {
        scrollRow = 0;
        searchText = text;
        if (searchMode() != TerminalSettings.SearchMode.NORMAL && !text.equals(lastSynced)) {
            lastSynced = text;
            JeiSearchBridge.write(text);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (searchMode() == TerminalSettings.SearchMode.JEI_TWO_WAY && search != null) {
            String jei = JeiSearchBridge.read();
            if (jei != null && !jei.equals(lastSynced) && !jei.equals(search.getValue())) {
                lastSynced = jei;
                search.setValue(jei);
            }
        }
    }

    // Dialogs

    private boolean dialogOpen() {
        return dialog != Dialog.NONE;
    }

    private boolean insideDialog(double mouseX, double mouseY) {
        return dialogOpen() && dialogRect().contains((int) mouseX - leftPos, (int) mouseY - topPos);
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
        sideButtons.forEach(button -> button.visible = !open);
        amount.visible = open;
        maximum.visible = dialog == Dialog.RULE;
        int stack = subject.isEmpty() ? 64 : subject.getMaxStackSize();
        String[] labels = {"-" + stack, "-10", "-1", "+1", "+10", "+" + stack};
        for (int i = 0; i < stepButtons.size(); i++) {
            Button button = stepButtons.get(i);
            button.visible = dialog == Dialog.REQUEST || dialog == Dialog.CRAFT;
            button.setMessage(Component.literal(labels[i]));
        }
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

    private List<AbstractWidget> dialogWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>(List.of(amount, maximum, removeRuleButton, ruleButton, confirmButton, cancelButton));
        widgets.addAll(stepButtons);
        return widgets;
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

    /** -2 and 2 are a stack down and up, other values add themselves. */
    private void step(int kind) {
        int stack = subject.getMaxStackSize();
        int now = enteredAmount();
        int next = switch (kind) {
            case 2 -> AmountSteps.plusStack(now, stack);
            case -2 -> AmountSteps.minusStack(now, stack);
            default -> AmountSteps.add(now, kind);
        };
        amount.setValue(Integer.toString(next));
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

    private static boolean matches(ItemStack stack, String query) {
        return query.isEmpty() || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private static TerminalSort.Key key(Cell cell) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(cell.stack().getItem());
        return new TerminalSort.Key(cell.stack().getHoverName().getString(), cell.stocked() ? cell.count() : 0, id.getNamespace());
    }

    private List<Cell> cells() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        TerminalSettings current = settings();
        if (cellsVersion != ClientStock.version() || !query.equals(cellsQuery) || !current.equals(cellsSettings)) {
            cells.clear();
            for (RequestRow row : ClientStock.requests()) {
                if (matches(row.stack(), query)) {
                    cells.add(new Cell(row.stack(), row.requested(), false, false, row));
                }
            }
            List<Cell> items = new ArrayList<>();
            for (StockEntry entry : ClientStock.entries()) {
                items.add(new Cell(entry.stack(), entry.count(), true, ClientStock.craftable(entry.stack()), null));
            }
            for (ItemStack craftable : ClientStock.craftables()) {
                boolean stocked = ClientStock.entries().stream().anyMatch(entry -> ItemStack.isSameItemSameComponents(entry.stack(), craftable));
                if (!stocked) {
                    items.add(new Cell(craftable, 0, false, true, null));
                }
            }
            Comparator<TerminalSort.Key> order = TerminalSort.comparator(current.sort(), current.direction());
            items.sort((a, b) -> order.compare(key(a), key(b)));
            for (Cell cell : items) {
                boolean shown = switch (current.view()) {
                    case ALL -> true;
                    case STOCK -> cell.stocked();
                    case CRAFTABLE -> cell.craftable();
                };
                if (shown && matches(cell.stack(), query)) {
                    cells.add(cell);
                }
            }
            cellsVersion = ClientStock.version();
            cellsQuery = query;
            cellsSettings = current;
            scrollRow = Math.min(scrollRow, maxScrollRow());
        }
        return cells;
    }

    private int maxScrollRow() {
        return Math.max(0, (cells.size() + COLUMNS - 1) / COLUMNS - layout.rows());
    }

    private int cellAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        Rect grid = layout.grid();
        if (!grid.contains(x, y)) {
            return -1;
        }
        return ((y - grid.y()) / CELL + scrollRow) * COLUMNS + (x - grid.x()) / CELL;
    }

    private Optional<Cell> cellUnder(double mouseX, double mouseY) {
        int index = cellAt(mouseX, mouseY);
        List<Cell> list = cells();
        return index >= 0 && index < list.size() ? Optional.of(list.get(index)) : Optional.empty();
    }

    // Drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        PanelPainter.panel(graphics, leftPos, topPos + layout.top(), imageWidth, layout.height());
        Rect grid = layout.grid();
        PanelPainter.slots(graphics, leftPos, topPos, grid, COLUMNS, COLUMNS * layout.rows());
        PanelPainter.inset(graphics, leftPos, topPos, layout.scrollbar());
        PanelPainter.slots(graphics, leftPos, topPos, Counter.OUTPUT, Counter.OUTPUT_COLUMNS, menu.inventoryStart());
        PanelPainter.slots(graphics, leftPos, topPos, Counter.INVENTORY, 9, 27);
        PanelPainter.slots(graphics, leftPos, topPos, Counter.HOTBAR, 9, 9);
        PanelPainter.slots(graphics, leftPos, topPos, Counter.CRAFT_GRID, 3, 9);
        PanelPainter.slots(graphics, leftPos, topPos, Counter.CRAFT_RESULT, 1, 1);
        arrow(graphics, leftPos + Counter.CRAFT_ARROW.x(), topPos + Counter.CRAFT_ARROW.y());
        int max = maxScrollRow();
        Rect bar = layout.scrollbar();
        int track = bar.h() - 15;
        int thumb = topPos + bar.y() + (max == 0 ? 0 : track * scrollRow / max);
        int thumbX = leftPos + bar.x();
        graphics.fill(thumbX, thumb, thumbX + bar.w(), thumb + 15, max == 0 ? 0xFF9A9A9A : 0xFFE0E0E0);
    }

    /** A grey arrow pointing right, 16 by 10, from the grid to the result. */
    private static void arrow(GuiGraphics graphics, int x, int y) {
        int colour = 0xFF8B8B8B;
        graphics.fill(x, y + 3, x + 10, y + 7, colour);
        for (int i = 0; i < 5; i++) {
            graphics.fill(x + 10 + i, y + i, x + 11 + i, y + 10 - i, colour);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!ClientStock.loaded() || !ClientStock.online()) {
            Component text = !ClientStock.loaded() ? Component.translatable("gui.servus_storage.terminal.loading")
                    : Component.translatable("gui.servus_storage.terminal." + ClientStock.reason());
            Rect grid = layout.grid();
            graphics.drawCenteredString(font, text, leftPos + grid.x() + grid.w() / 2, topPos + grid.y() + grid.h() / 2 - 4, 0xFFFFFF);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        renderMessage(graphics);
        if (dialogOpen()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, DIALOG_Z);
            renderDialog(graphics, mouseX, mouseY);
            graphics.pose().popPose();
            if (!insideDialog(mouseX, mouseY)) {
                renderTooltip(graphics, mouseX, mouseY);
            }
            return;
        }
        renderGrid(graphics, mouseX, mouseY);
        Optional<Cell> hovered = cellUnder(mouseX, mouseY);
        if (hovered.isPresent()) {
            graphics.renderTooltip(font, cellTooltip(hovered.get()), Optional.empty(), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private Component stateText(RequestRow row) {
        return row.state().equals("MISSING")
                ? Component.translatable("gui.servus_storage.terminal.state.missing", row.missing())
                : Component.translatable("gui.servus_storage.terminal.state." + row.state().toLowerCase(Locale.ROOT));
    }

    private List<Component> cellTooltip(Cell cell) {
        if (cell.isRequest()) {
            RequestRow row = cell.request();
            return List.of(cell.stack().getHoverName(),
                    Component.translatable("gui.servus_storage.terminal.request_line", row.delivered(), row.requested()),
                    stateText(row),
                    Component.translatable("gui.servus_storage.terminal.cancel_hint"));
        }
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

    /** The colour of a request's state mark. */
    private static int stateColour(String state) {
        return switch (state) {
            case "SENDING" -> 0xFF40DCC8;
            case "WAITING_ROOM" -> 0xFFFFD040;
            case "PRODUCING" -> 0xFF50D050;
            case "MISSING" -> 0xFFFF5050;
            default -> 0xFFF0F0F0;
        };
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Cell> list = cells();
        Rect grid = layout.grid();
        int first = scrollRow * COLUMNS;
        int visible = COLUMNS * layout.rows();
        for (int i = 0; i < visible && first + i < list.size(); i++) {
            Cell cell = list.get(first + i);
            int x = leftPos + grid.x() + (i % COLUMNS) * CELL;
            int y = topPos + grid.y() + (i / COLUMNS) * CELL;
            if (cell.isRequest()) {
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF5868A8);
            }
            graphics.renderItem(cell.stack(), x, y);
            if (cell.isRequest()) {
                RequestRow row = cell.request();
                smallText(graphics, Component.literal(row.delivered() + "/" + row.requested()), x + 16, y + 11, 0xFFFFFFFF);
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.fill(x + 12, y, x + 16, y + 4, 0xFF202020);
                graphics.fill(x + 13, y + 1, x + 15, y + 3, stateColour(row.state()));
                graphics.pose().popPose();
                continue;
            }
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
        if (cell >= first && cell < first + visible) {
            int x = leftPos + grid.x() + ((cell - first) % COLUMNS) * CELL;
            int y = topPos + grid.y() + ((cell - first) / COLUMNS) * CELL;
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

    private void renderMessage(GuiGraphics graphics) {
        if (!ClientStock.message().isEmpty()) {
            Component text = Component.translatable("gui.servus_storage.terminal.message." + ClientStock.message());
            graphics.drawString(font, font.plainSubstrByWidth(text.getString(), Counter.MESSAGE.w()),
                    leftPos + Counter.MESSAGE.x(), topPos + Counter.MESSAGE.y() + 1, COLOUR_ALERT, false);
        }
    }

    private void renderDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        Rect area = dialogRect();
        int x = dx();
        int y = dy();
        graphics.fill(x - 1, y - 1, x + area.w() + 1, y + area.h() + 1, 0xFF373737);
        graphics.fill(x, y, x + area.w(), y + area.h(), 0xFFC6C6C6);
        graphics.renderItem(subject, x + 4, y + 2);
        Component head = dialog == Dialog.CRAFT
                ? Component.translatable("gui.servus_storage.terminal.craft_title", subject.getHoverName())
                : subject.getHoverName();
        graphics.drawString(font, font.plainSubstrByWidth(head.getString(), area.w() - 28), x + 24, y + 6, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable(dialog == Dialog.RULE ? "gui.servus_storage.terminal.rule_min"
                : "gui.servus_storage.terminal.amount"), x + 4, y + 22, COLOUR_TEXT, false);
        if (dialog == Dialog.RULE) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.rule_max"), x + 96, y + 22, COLOUR_TEXT, false);
        }
        // Widgets were drawn by super.render below the dialog; draw them again on top of it.
        for (AbstractWidget widget : dialogWidgets()) {
            if (widget.visible) {
                widget.render(graphics, mouseX, mouseY, 0);
            }
        }
        if (dialog == Dialog.CRAFT) {
            renderPreview(graphics, x, y);
        }
    }

    private void renderPreview(GuiGraphics graphics, int x, int y) {
        CraftPreviewPayload preview = currentPreview();
        confirmButton.active = preview != null && preview.startable();
        int statusY = y + 96;
        if (preview == null) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal.craft_wait"), x + 4, statusY, COLOUR_DIM, false);
            return;
        }
        previewColumn(graphics, x + 4, y + 54, "gui.servus_storage.terminal.craft_from_stock", preview.fromStock(), COLOUR_TEXT);
        previewColumn(graphics, x + 60, y + 54, "gui.servus_storage.terminal.craft_to_craft", preview.toCraft(), COLOUR_GOOD);
        previewColumn(graphics, x + 116, y + 54, "gui.servus_storage.terminal.craft_missing", preview.missing(), COLOUR_ALERT);
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
        graphics.drawString(font, font.plainSubstrByWidth(status.getString(), dialogRect().w() - 8), x + 4, statusY, colour, false);
    }

    private void previewColumn(GuiGraphics graphics, int x, int y, String heading, List<StockEntry> rows, int colour) {
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(heading).getString(), 54), x, y, colour, false);
        for (int i = 0; i < Math.min(PREVIEW_ROWS, rows.size()); i++) {
            StockEntry row = rows.get(i);
            int rowY = y + 10 + i * 9;
            graphics.pose().pushPose();
            graphics.pose().translate(x, rowY, 0);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.renderItem(row.stack(), 0, 0);
            graphics.pose().popPose();
            String text = row.count() + " " + row.stack().getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(text, 44), x + 10, rowY, COLOUR_TEXT, false);
        }
        if (rows.size() > PREVIEW_ROWS) {
            smallText(graphics, Component.translatable("gui.servus_storage.terminal.craft_more", rows.size() - PREVIEW_ROWS),
                    x + 54, y + 10 + PREVIEW_ROWS * 9, COLOUR_DIM);
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

    /** The rows above the base top and the side column belong to the screen too. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        int x = (int) mouseX - left;
        int y = (int) mouseY - top;
        boolean panel = x >= 0 && x < imageWidth && y >= layout.top() && y < Counter.BASE_HEIGHT;
        boolean side = x >= Counter.SIDE_X && x < 0 && y >= layout.top() && y < Counter.BASE_HEIGHT;
        return !panel && !side;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && search != null && search.visible && search.isMouseOver(mouseX, mouseY)) {
            search.setValue("");
            scrollRow = 0;
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        if (dialogOpen()) {
            for (AbstractWidget widget : dialogWidgets()) {
                if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) {
                    setFocused(widget);
                    return true;
                }
            }
            if (insideDialog(mouseX, mouseY)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!ClientStock.online()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (cellAt(mouseX, mouseY) >= 0) {
            Optional<Cell> cell = cellUnder(mouseX, mouseY);
            if (cell.isPresent() && cell.get().isRequest()) {
                // A request entry cancels on click, as the request row did.
                PacketDistributor.sendToServer(new CancelRequestPayload(cell.get().request().id()));
                return true;
            }
            if (!menu.getCarried().isEmpty()) {
                PacketDistributor.sendToServer(PushCarriedPayload.INSTANCE);
                return true;
            }
            if (cell.isPresent() && hasAltDown()) {
                // Alt click: a stack from instant stock onto the cursor, for the crafting grid.
                if (cell.get().stocked()) {
                    PacketDistributor.sendToServer(new GridPullPayload(cell.get().stack()));
                }
                return true;
            }
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
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

    /** A vanilla button with a 16 by 16 icon from {@code textures/gui/terminal}. */
    private static final class IconButton extends Button {

        private final Supplier<String> icon;

        IconButton(int x, int y, int w, int h, Supplier<String> icon, OnPress press) {
            super(x, y, w, h, Component.empty(), press, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID,
                    "textures/gui/terminal/" + icon.get() + ".png");
            graphics.blit(texture, getX() + (width - 16) / 2, getY() + (height - 16) / 2, 0, 0, 16, 16, 16, 16);
        }
    }
}
