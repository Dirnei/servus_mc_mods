package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.client.TerminalCells.Cell;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.site.RemoteSite;
import io.github.dirnei.mc.servus.shuttles.terminal.AmountSteps;
import io.github.dirnei.mc.servus.shuttles.terminal.CancelRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftStartPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.OrderStartPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.PushCarriedPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestItemPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestRow;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestSplit;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import io.github.dirnei.mc.servus.shuttles.terminal.SelectSitePayload;
import io.github.dirnei.mc.servus.shuttles.terminal.SetStockRulePayload;
import io.github.dirnei.mc.servus.shuttles.terminal.StockEntry;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Storage;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.shuttles.pattern.CraftingGrid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Refined Storage style Storage Terminal: view buttons left of the panel,
 * a Craft button and search, the item grid with open requests first, a
 * message line, the Input and Output rows, the player inventory; an amount
 * prompt for requests and rules, a craft dialog and the order grid, each
 * with a server preview. Everything sits where {@link Storage} says; extra
 * grid rows grow the panel upwards.
 */
public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {

    private static final int COLUMNS = Storage.COLUMNS;
    private static final int CELL = TerminalLayout.CELL;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_DIM = 0xFF707070;
    private static final int COLOUR_ALERT = 0xFFB03030;
    private static final int COLOUR_GOOD = 0xFF2E7D32;
    private static final int PREVIEW_ROWS = 3;
    /** Preview rows under the request split line and in the order grid. */
    private static final int SHORT_PREVIEW_ROWS = 2;
    private static final String[] SETTING_KINDS = {"sort", "direction", "view", "search", "height"};
    /** Step button kinds: -2 and 2 are a stack down and up, the others add themselves. */
    private static final int[] STEP_KINDS = {-2, -10, -1, 1, 10, 2};

    private enum Dialog { NONE, REQUEST, RULE, CRAFT, ORDER }

    /** Set by the recipe viewer's transfer: open the order grid on the terminal screen now or when it shows next. */
    private static boolean orderRequested;

    private Storage.Layout layout = Storage.layout(Storage.BASE_ROWS);
    private EditBox search;
    private String searchText = "";
    private String lastSynced = null;
    private final TerminalCells cells = new TerminalCells();
    private int scrollRow;
    private final List<IconButton> sideButtons = new ArrayList<>();

    private Dialog dialog = Dialog.NONE;
    private Button craftButton;
    private ItemStack subject = ItemStack.EMPTY;
    /** The requestable count of the request prompt's item. */
    private long subjectCount;
    /** Whether a producer of the line makes the request prompt's item. */
    private boolean subjectCraftable;
    private EditBox amount;
    private EditBox maximum;
    private int askedAmount = -1;
    /** The order grid cells the last order preview was asked for. */
    private List<ItemStack> askedGrid = List.of();
    /** The order grid's result the amount was last set for. */
    private ItemStack lastResult = ItemStack.EMPTY;
    /** Whether the order grid asks whether to save its pattern. */
    private boolean asking;
    private Button saveButton;
    private Button onceButton;
    private final List<Button> stepButtons = new ArrayList<>();
    private Button confirmButton;
    private Button ruleButton;
    private Button removeRuleButton;
    private Button cancelButton;

    public StorageTerminalScreen(StorageTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Storage.WIDTH, Storage.BASE_HEIGHT);
    }

    private TerminalSettings settings() {
        return ClientTerminalSettings.get();
    }

    private void update(TerminalSettings changed) {
        ClientTerminalSettings.set(changed);
        refreshSideButtons();
    }

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
        areas.add(new Rect2i(leftPos + Storage.SIDE_X, top, -Storage.SIDE_X, layout.height()));
        if (layout.top() < 0) {
            areas.add(new Rect2i(leftPos, top, imageWidth, -layout.top()));
        }
        return areas;
    }

    /**
     * Centres the panel as a whole, the base top below the extra rows, and
     * registers the terminal on every show, also after another screen was in front.
     */
    @Override
    protected void init() {
        layout = Storage.layout(Storage.rowsFor(settings().height(), height));
        super.init();
        topPos = (height - layout.height()) / 2 - layout.top();
        ClientStock.open(menu.terminal().getBlockPos());
        titleLabelX = layout.title().x();
        titleLabelY = layout.title().y();
        inventoryLabelX = Storage.INVENTORY_LABEL.x();
        inventoryLabelY = Storage.INVENTORY_LABEL.y();
        initSearch();
        initSideButtons();
        initCraftButton();
        initDialog();
        applyDialog();
        if (orderRequested) {
            orderRequested = false;
            openOrder();
        }
    }

    private void initSearch() {
        Rect searchRect = layout.search();
        search = new EditBox(font, leftPos + searchRect.x(), topPos + searchRect.y(), searchRect.w(), searchRect.h(),
                Component.translatable("gui.servus_shuttles.search"));
        search.setMaxLength(50);
        search.setHint(Component.translatable("gui.servus_shuttles.search"));
        search.setValue(searchText);
        search.setResponder(this::searchChanged);
        addRenderableWidget(search);
        setInitialFocus(search);
    }

    private void initSideButtons() {
        sideButtons.clear();
        List<Rect> side = layout.side();
        sideButtons.add(sideButton(side.get(0), () -> icon("sort_", settings().sort()),
                b -> update(settings().withSort(TerminalSettings.next(settings().sort())))));
        sideButtons.add(sideButton(side.get(1), () -> icon("dir_", settings().direction()),
                b -> update(settings().withDirection(TerminalSettings.next(settings().direction())))));
        sideButtons.add(sideButton(side.get(2), () -> icon("view_", settings().view()), b -> {
            update(settings().withView(TerminalSettings.next(settings().view())));
            scrollRow = 0;
        }));
        sideButtons.add(sideButton(side.get(3), () -> icon("search_", searchMode()), b -> update(settings().withSearch(nextSearchMode()))));
        sideButtons.add(sideButton(side.get(4), () -> icon("height_", settings().height()), b -> {
            update(settings().withHeight(TerminalSettings.next(settings().height())));
            searchText = search.getValue();
            rebuildWidgets();
        }));
        refreshSideButtons();
    }

    private void initCraftButton() {
        Rect craftRect = layout.craft();
        craftButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.craft_button"), b -> openOrder())
                .bounds(leftPos + craftRect.x(), topPos + craftRect.y(), craftRect.w(), craftRect.h())
                .tooltip(Tooltip.create(Component.translatable("gui.servus_shuttles.terminal.craft_button_hint"))).build());
    }

    private void initDialog() {
        amount = numberField(40, "gui.servus_shuttles.terminal.amount");
        amount.setResponder(text -> amountChanged());
        maximum = numberField(120, "gui.servus_shuttles.terminal.rule_max");
        stepButtons.clear();
        for (int i = 0; i < STEP_KINDS.length; i++) {
            int kind = STEP_KINDS[i];
            stepButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> step(kind))
                    .bounds(dx() + 4 + i * 28, dy() + 36, 26, 14).build()));
        }
        int bottom = dy() + dialogRect().h() - 17;
        removeRuleButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.rule_remove"),
                b -> removeRule()).bounds(dx() + 4, bottom, 46, 14).build());
        ruleButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggleRule())
                .bounds(dx() + 52, bottom, 38, 14).build());
        confirmButton = addRenderableWidget(Button.builder(Component.empty(), b -> confirm())
                .bounds(dx() + 92, bottom, 40, 14).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.cancel"),
                b -> closeDialog()).bounds(dx() + 134, bottom, 36, 14).build());
        Rect row = Storage.ORDER_BUTTONS;
        int half = (row.w() - 2) / 2;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.order_save"),
                b -> startOrder(true)).bounds(leftPos + row.x(), topPos + row.y(), half, row.h()).build());
        onceButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.order_once"),
                b -> startOrder(false)).bounds(leftPos + row.right() - half, topPos + row.y(), half, row.h()).build());
    }

    /** A five digit field in the dialog's amount row. */
    private EditBox numberField(int x, String key) {
        EditBox field = new EditBox(font, dx() + x, dy() + 20, 50, 12, Component.translatable(key));
        field.setMaxLength(5);
        field.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        return addRenderableWidget(field);
    }

    /** The recipe viewer's transfer: show the order grid now, or when the terminal screen shows next. */
    public static void requestOrderGrid() {
        if (Minecraft.getInstance().screen instanceof StorageTerminalScreen screen) {
            screen.openOrder();
        } else {
            orderRequested = true;
        }
    }

    /** Opens the order grid over the item grid while a CPU and a Crafting Station are on the line. */
    public void openOrder() {
        if (!ClientStock.orderGrid()) {
            return;
        }
        subject = ItemStack.EMPTY;
        dialog = Dialog.ORDER;
        asking = false;
        lastResult = ItemStack.EMPTY;
        ClientStock.clearPreview();
        askedAmount = -1;
        askedGrid = List.of();
        amount.setValue("1");
        applyDialog();
        focusAmount();
        askPreview();
    }

    /** The result the order grid's ghost cells make, on the client, empty when they match nothing. */
    private ItemStack orderResult() {
        Level level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            return ItemStack.EMPTY;
        }
        return CraftingGrid.match(level, menu.gridCells()).map(holder -> ServusRecipes.displayResult(level, holder.value()))
                .orElse(ItemStack.EMPTY);
    }

    private IconButton sideButton(Rect rect, Supplier<String> icon, Button.OnPress press) {
        return addRenderableWidget(new IconButton(leftPos + rect.x(), topPos + rect.y(), rect.w(), rect.h(), icon, press));
    }

    private static String icon(String prefix, Enum<?> value) {
        return prefix + value.name().toLowerCase(Locale.ROOT);
    }

    /** The search modes past Normal need JEI. */
    private TerminalSettings.SearchMode nextSearchMode() {
        if (!JeiSearchBridge.available()) {
            return TerminalSettings.SearchMode.NORMAL;
        }
        return TerminalSettings.next(settings().search());
    }

    private TerminalSettings.SearchMode searchMode() {
        return JeiSearchBridge.available() ? settings().search() : TerminalSettings.SearchMode.NORMAL;
    }

    private void refreshSideButtons() {
        Enum<?>[] values = {settings().sort(), settings().direction(), settings().view(), searchMode(), settings().height()};
        for (int i = 0; i < sideButtons.size(); i++) {
            String key = "gui.servus_shuttles.terminal.setting." + SETTING_KINDS[i];
            Component value = Component.translatable(key + "." + values[i].name().toLowerCase(Locale.ROOT));
            sideButtons.get(i).setTooltip(Tooltip.create(Component.translatable(key, value)));
        }
    }

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
        if (dialog == Dialog.ORDER) {
            if (!ClientStock.orderGrid()) {
                closeDialog();
            } else {
                followResult();
                askPreview();
            }
        }
        if (craftButton != null) {
            craftButton.visible = !dialogOpen() && ClientStock.orderGrid();
        }
    }

    /** A new result sets the amount to what one craft makes. */
    private void followResult() {
        ItemStack result = orderResult();
        if (!ItemStack.isSameItemSameComponents(result, lastResult)) {
            lastResult = result.copy();
            asking = false;
            if (!result.isEmpty()) {
                amount.setValue(Integer.toString(result.getCount()));
            }
            applyDialog();
        }
    }

    private boolean dialogOpen() {
        return dialog != Dialog.NONE;
    }

    private boolean insideDialog(double mouseX, double mouseY) {
        return dialogOpen() && dialogRect().contains((int) mouseX - leftPos, (int) mouseY - topPos);
    }

    private void openRequest(Cell cell) {
        subject = cell.stack().copyWithCount(1);
        subjectCount = cell.count();
        subjectCraftable = cell.craftable();
        dialog = Dialog.REQUEST;
        ClientStock.clearPreview();
        askedAmount = -1;
        amount.setValue(Integer.toString(RequestSplit.prefill(subject.getMaxStackSize(), subjectCount, subjectCraftable)));
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
            amount.setValue(Integer.toString(RequestSplit.prefill(subject.getMaxStackSize(), subjectCount, subjectCraftable)));
        } else {
            dialog = Dialog.RULE;
            RuleRow rule = ClientStock.ruleFor(subject);
            amount.setValue(rule == null ? "0" : Integer.toString(rule.min()));
            maximum.setValue(rule == null ? Integer.toString(subject.getMaxStackSize()) : Integer.toString(rule.max()));
        }
        applyDialog();
    }

    /** Shows the widgets of the open dialog, or of the grid when none is open. */
    private void applyDialog() {
        boolean open = dialogOpen();
        boolean order = dialog == Dialog.ORDER;
        menu.setOrderOpen(order);
        craftButton.visible = !open && ClientStock.orderGrid();
        search.visible = !open;
        sideButtons.forEach(button -> button.visible = !open);
        amount.visible = open;
        maximum.visible = dialog == Dialog.RULE;
        placeDialogWidgets(order);
        int stack = stackSize();
        String[] labels = {"-" + stack, "-10", "-1", "+1", "+10", "+" + stack};
        for (int i = 0; i < stepButtons.size(); i++) {
            Button button = stepButtons.get(i);
            button.visible = dialog == Dialog.REQUEST || dialog == Dialog.CRAFT || order;
            button.setMessage(Component.literal(labels[i]));
        }
        confirmButton.visible = open && !(order && asking);
        confirmButton.active = true;
        saveButton.visible = order && asking;
        onceButton.visible = order && asking;
        confirmButton.setMessage(Component.translatable(switch (dialog) {
            case RULE -> "gui.servus_shuttles.terminal.rule_save";
            case CRAFT -> "gui.servus_shuttles.terminal.craft_start";
            default -> "gui.servus_shuttles.terminal.request";
        }));
        ruleButton.visible = dialog == Dialog.REQUEST || dialog == Dialog.RULE;
        ruleButton.setMessage(Component.translatable(dialog == Dialog.RULE ? "gui.servus_shuttles.terminal.request" : "gui.servus_shuttles.terminal.rule"));
        removeRuleButton.visible = dialog == Dialog.RULE;
        cancelButton.visible = open && !(order && asking);
    }

    /** The step size: the item's stack, or the order grid result's stack. */
    private int stackSize() {
        ItemStack item = dialog == Dialog.ORDER ? lastResult : subject;
        return item.isEmpty() ? 64 : item.getMaxStackSize();
    }

    /** The amount, step and button widgets sit in the order grid's rows, or in the dialog's own rows. */
    private void placeDialogWidgets(boolean order) {
        if (order) {
            Rect field = Storage.ORDER_AMOUNT;
            amount.setRectangle(field.w(), field.h(), leftPos + field.x(), topPos + field.y());
            Rect steps = Storage.ORDER_STEPS;
            for (int i = 0; i < stepButtons.size(); i++) {
                stepButtons.get(i).setRectangle(26, steps.h(), leftPos + steps.x() + i * 28, topPos + steps.y());
            }
            Rect row = Storage.ORDER_BUTTONS;
            confirmButton.setRectangle(60, row.h(), leftPos + row.x(), topPos + row.y());
            cancelButton.setRectangle(40, row.h(), leftPos + row.right() - 40, topPos + row.y());
            return;
        }
        amount.setRectangle(50, 12, dx() + 40, dy() + 20);
        for (int i = 0; i < stepButtons.size(); i++) {
            stepButtons.get(i).setRectangle(26, 14, dx() + 4 + i * 28, dy() + 36);
        }
        int bottom = dy() + dialogRect().h() - 17;
        confirmButton.setRectangle(40, 14, dx() + 92, bottom);
        cancelButton.setRectangle(36, 14, dx() + 134, bottom);
    }

    private List<AbstractWidget> dialogWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>(List.of(amount, maximum, removeRuleButton, ruleButton, confirmButton, cancelButton,
                saveButton, onceButton));
        widgets.addAll(stepButtons);
        return widgets;
    }

    private void closeDialog() {
        dialog = Dialog.NONE;
        asking = false;
        subject = ItemStack.EMPTY;
        amount.setFocused(false);
        maximum.setFocused(false);
        ClientStock.clearPreview();
        applyDialog();
    }

    private static int parsed(EditBox field, int fallback) {
        try {
            return Integer.parseInt(field.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int enteredAmount() {
        return Math.max(0, parsed(amount, 0));
    }

    private void step(int kind) {
        int stack = stackSize();
        int now = enteredAmount();
        int next = switch (kind) {
            case 2 -> AmountSteps.plusStack(now, stack);
            case -2 -> AmountSteps.minusStack(now, stack);
            default -> AmountSteps.add(now, kind);
        };
        amount.setValue(Integer.toString(next));
    }

    /** The amount field changed: a request prompt stays within what it may ask, then a preview is asked. */
    private void amountChanged() {
        if (dialog == Dialog.REQUEST) {
            int cap = RequestSplit.cap(subjectCount, subjectCraftable);
            if (enteredAmount() > cap) {
                amount.setValue(Integer.toString(cap));
                return;
            }
        }
        askPreview();
    }

    /**
     * Asks the server for a preview of the amount in the field: a craft, the
     * crafted rest of a request above the requestable count, or the order
     * grid. Answers arrive as {@link CraftPreviewPayload}.
     */
    private void askPreview() {
        int wanted = enteredAmount();
        if (wanted <= 0) {
            return;
        }
        switch (dialog) {
            case CRAFT, REQUEST -> {
                boolean needed = dialog == Dialog.CRAFT || wanted > subjectCount;
                if (!subject.isEmpty() && needed && wanted != askedAmount) {
                    askedAmount = wanted;
                    CraftPreviewRequestPayload.Mode mode = dialog == Dialog.CRAFT
                            ? CraftPreviewRequestPayload.Mode.CRAFT : CraftPreviewRequestPayload.Mode.REQUEST;
                    ClientPacketDistributor.sendToServer(new CraftPreviewRequestPayload(subject, wanted, mode));
                }
            }
            case ORDER -> {
                List<ItemStack> cells = menu.gridCells();
                if (wanted != askedAmount || !sameCells(cells, askedGrid)) {
                    askedAmount = wanted;
                    askedGrid = cells.stream().map(ItemStack::copy).toList();
                    ClientPacketDistributor.sendToServer(new CraftPreviewRequestPayload(ItemStack.EMPTY, wanted,
                            CraftPreviewRequestPayload.Mode.ORDER));
                }
            }
            default -> {
            }
        }
    }

    private static boolean sameCells(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.isSameItemSameComponents(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** The preview for exactly what the dialog shows, or null while it is on its way. */
    private CraftPreviewPayload currentPreview() {
        CraftPreviewPayload preview = ClientStock.preview();
        ItemStack item = dialog == Dialog.ORDER ? lastResult : subject;
        if (preview == null || preview.amount() != enteredAmount() || !ItemStack.isSameItemSameComponents(preview.item(), item)
                || dialog == Dialog.ORDER && !sameCells(menu.gridCells(), askedGrid)) {
            return null;
        }
        return preview;
    }

    private void confirm() {
        int wanted = enteredAmount();
        switch (dialog) {
            case REQUEST -> {
                if (wanted > subjectCount) {
                    CraftPreviewPayload preview = currentPreview();
                    if (preview == null || !preview.requestable()) {
                        return;
                    }
                }
                if (wanted > 0) {
                    ClientPacketDistributor.sendToServer(new RequestItemPayload(subject, wanted));
                }
            }
            case ORDER -> {
                CraftPreviewPayload preview = currentPreview();
                if (preview == null || !preview.startable()) {
                    return;
                }
                if (!preview.patternSaved()) {
                    asking = true;
                    applyDialog();
                    return;
                }
                ClientPacketDistributor.sendToServer(new OrderStartPayload(wanted, false));
            }
            case RULE -> ClientPacketDistributor.sendToServer(new SetStockRulePayload(subject, wanted, Math.max(wanted, parsed(maximum, wanted))));
            case CRAFT -> {
                CraftPreviewPayload preview = currentPreview();
                if (preview == null || !preview.startable()) {
                    return;
                }
                ClientPacketDistributor.sendToServer(new CraftStartPayload(subject, wanted));
            }
            default -> {
            }
        }
        closeDialog();
    }

    /** Starts the order grid's order, saving its pattern too when asked to. */
    private void startOrder(boolean save) {
        int wanted = enteredAmount();
        if (wanted > 0) {
            ClientPacketDistributor.sendToServer(new OrderStartPayload(wanted, save));
        }
        closeDialog();
    }

    private void removeRule() {
        if (!subject.isEmpty()) {
            ClientPacketDistributor.sendToServer(new SetStockRulePayload(subject, -1, -1));
        }
        closeDialog();
    }

    /** The grid's cells; clamps the scroll when they were rebuilt. */
    private List<Cell> cells() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        if (cells.refresh(query, settings())) {
            scrollRow = Math.min(scrollRow, maxScrollRow());
        }
        return cells.list();
    }

    private int maxScrollRow() {
        return Math.max(0, (cells.list().size() + COLUMNS - 1) / COLUMNS - layout.rows());
    }

    private int cellAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        Rect grid = layout.grid();
        if (!grid.contains(x, y) || dialogOpen()) {
            return -1;
        }
        return ((y - grid.y()) / CELL + scrollRow) * COLUMNS + (x - grid.x()) / CELL;
    }

    private Optional<Cell> cellUnder(double mouseX, double mouseY) {
        int index = cellAt(mouseX, mouseY);
        List<Cell> list = cells();
        return index >= 0 && index < list.size() ? Optional.of(list.get(index)) : Optional.empty();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos + layout.top(), imageWidth, layout.height());
        PanelPainter.slots(graphics, leftPos, topPos, layout.grid(), COLUMNS, COLUMNS * layout.rows());
        PanelPainter.inset(graphics, leftPos, topPos, layout.scrollbar());
        PanelPainter.slots(graphics, leftPos, topPos, Storage.INPUT, Storage.ROW_SLOTS, Storage.ROW_SLOTS);
        PanelPainter.slots(graphics, leftPos, topPos, Storage.OUTPUT, Storage.ROW_SLOTS, menu.outputSlots());
        tag(graphics, Storage.INPUT_TAG, true, menu.inputOpen() ? 0xFF4C9A5A : PanelPainter.SLOT);
        if (!menu.inputOpen()) {
            Rect in = Storage.INPUT;
            graphics.fill(leftPos + in.x(), topPos + in.y(), leftPos + in.x() + in.w(), topPos + in.y() + in.h(), 0x808B8B8B);
        }
        tag(graphics, Storage.OUTPUT_TAG, false, 0xFF4C7FB0);
        PanelPainter.slots(graphics, leftPos, topPos, Storage.INVENTORY, 9, 27);
        PanelPainter.slots(graphics, leftPos, topPos, Storage.HOTBAR, 9, 9);
        renderScrollThumb(graphics);
    }

    private void renderScrollThumb(GuiGraphicsExtractor graphics) {
        int max = maxScrollRow();
        Rect bar = layout.scrollbar();
        int track = bar.h() - 15;
        int thumb = topPos + bar.y() + (max == 0 ? 0 : track * scrollRow / max);
        int thumbX = leftPos + bar.x();
        graphics.fill(thumbX, thumb, thumbX + bar.w(), thumb + 15, max == 0 ? 0xFF9A9A9A : 0xFFE0E0E0);
    }

    /** A small arrow beside a row: up into the warehouse for the input, down to the player for the output. */
    private void tag(GuiGraphicsExtractor graphics, Rect rect, boolean up, int colour) {
        int cx = leftPos + rect.x() + rect.w() / 2;
        int top = topPos + rect.y() + 4;
        graphics.fill(cx - 1, top, cx + 1, top + 10, colour);
        for (int i = 0; i < 4; i++) {
            int y = up ? top + i : top + 9 - i;
            graphics.fill(cx - 1 - i, y, cx + 1 + i, y + 1, colour);
        }
    }

    /** The Input row is locked while another site is shown, since what is handed in would be stored here. */
    private Component tagTooltip(int x, int y) {
        if (!menu.inputOpen() && (Storage.INPUT_TAG.contains(x, y) || Storage.INPUT.contains(x, y) && menu.getCarried().isEmpty()
                && (hoveredSlot == null || !hoveredSlot.hasItem()))) {
            return Component.translatable("gui.servus_shuttles.terminal.input_locked");
        }
        if (Storage.INPUT_TAG.contains(x, y)) {
            return Component.translatable("gui.servus_shuttles.terminal.input_tag");
        }
        if (Storage.OUTPUT_TAG.contains(x, y)) {
            return Component.translatable("gui.servus_shuttles.terminal.output_tag");
        }
        return null;
    }

    /** A grey arrow pointing right, 16 by 10, from the grid to the result. */
    private static void arrow(GuiGraphicsExtractor graphics, int x, int y) {
        int colour = PanelPainter.SLOT;
        graphics.fill(x, y + 3, x + 10, y + 7, colour);
        for (int i = 0; i < 5; i++) {
            graphics.fill(x + 10 + i, y + i, x + 11 + i, y + 10 - i, colour);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!ClientStock.loaded() || !ClientStock.online()) {
            Component text = !ClientStock.loaded() ? Component.translatable("gui.servus_shuttles.terminal.loading")
                    : Component.translatable("gui.servus_shuttles.terminal." + ClientStock.reason());
            Rect grid = layout.grid();
            graphics.centeredText(font, text, leftPos + grid.x() + grid.w() / 2, topPos + grid.y() + grid.h() / 2 - 4, 0xFFFFFFFF);
            return;
        }
        renderMessage(graphics);
        if (dialogOpen()) {
            renderDialog(graphics, mouseX, mouseY);
            return;
        }
        renderGrid(graphics, mouseX, mouseY);
        Optional<Cell> hovered = cellUnder(mouseX, mouseY);
        Component tag = tagTooltip(mouseX - leftPos, mouseY - topPos);
        if (siteSelector() && overTitle(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, font.split(Component.translatable("gui.servus_shuttles.terminal.site_tooltip"), 200), mouseX, mouseY);
        } else if (hovered.isPresent()) {
            graphics.setTooltipForNextFrame(font, cellTooltip(hovered.get()), Optional.empty(), mouseX, mouseY);
        } else if (tag != null) {
            graphics.setTooltipForNextFrame(font, tag, mouseX, mouseY);
        }
    }

    private Component stateText(RequestRow row) {
        return row.state().equals("MISSING")
                ? Component.translatable("gui.servus_shuttles.terminal.state.missing", row.missing())
                : Component.translatable("gui.servus_shuttles.terminal.state." + row.state().toLowerCase(Locale.ROOT));
    }

    private List<Component> cellTooltip(Cell cell) {
        if (cell.isRequest()) {
            RequestRow row = cell.request();
            return List.of(cell.stack().getHoverName(),
                    Component.translatable("gui.servus_shuttles.terminal.request_line", row.delivered(), row.requested()),
                    stateText(row),
                    Component.translatable("gui.servus_shuttles.terminal.cancel_hint"));
        }
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), cell.stack()));
        if (cell.stocked()) {
            lines.add(Component.translatable("gui.servus_shuttles.stored", cell.count()));
            RuleRow rule = ClientStock.ruleFor(cell.stack());
            if (rule != null) {
                lines.add(Component.translatable("gui.servus_shuttles.terminal.rule_line", rule.min(), rule.max(),
                        Component.translatable("gui.servus_shuttles.terminal.rule_state." + rule.state())));
            }
            lines.add(Component.translatable("gui.servus_shuttles.terminal.click_hint"));
            if (cell.craftable()) {
                lines.add(Component.translatable("gui.servus_shuttles.terminal.craftable_hint"));
            }
        } else {
            lines.add(Component.translatable("gui.servus_shuttles.terminal.craft_hint"));
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

    private void renderGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Cell> list = cells();
        Rect grid = layout.grid();
        int first = scrollRow * COLUMNS;
        int visible = COLUMNS * layout.rows();
        boolean greyed = greyed();
        for (int i = 0; i < visible && first + i < list.size(); i++) {
            int x = leftPos + grid.x() + (i % COLUMNS) * CELL;
            int y = topPos + grid.y() + (i / COLUMNS) * CELL;
            renderCell(graphics, list.get(first + i), x, y, greyed);
        }
        int hovered = cellAt(mouseX, mouseY);
        if (hovered >= first && hovered < first + visible) {
            int x = leftPos + grid.x() + ((hovered - first) % COLUMNS) * CELL;
            int y = topPos + grid.y() + ((hovered - first) / COLUMNS) * CELL;
            graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
    }

    /**
     * One cell: a request with its delivered count and state mark on a blue
     * ground, or an item with its count or a craft label, a blue corner for a
     * stock rule and an orange corner when a stocked item is craftable too.
     */
    private void renderCell(GuiGraphicsExtractor graphics, Cell cell, int x, int y, boolean greyed) {
        if (cell.isRequest()) {
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF5868A8);
            graphics.item(cell.stack(), x, y);
            RequestRow row = cell.request();
            smallText(graphics, Component.literal(row.delivered() + "/" + row.requested()), x + 16, y + 11, 0xFFFFFFFF);
            graphics.fill(x + 12, y, x + 16, y + 4, 0xFF202020);
            graphics.fill(x + 13, y + 1, x + 15, y + 3, stateColour(row.state()));
            return;
        }
        graphics.item(cell.stack(), x, y);
        if (greyed) {
            graphics.fill(x, y, x + 16, y + 16, 0xA0707070);
        }
        if (cell.stocked()) {
            graphics.itemDecorations(font, cell.stack(), x, y, abbreviate(cell.count()));
        } else {
            smallText(graphics, Component.translatable("gui.servus_shuttles.terminal.craft_cell"), x + 16, y + 11, 0xFFFFE080);
        }
        if (ClientStock.ruleFor(cell.stack()) != null) {
            graphics.fill(x, y, x + 3, y + 3, 0xFF40A0FF);
        }
        if (cell.stocked() && cell.craftable()) {
            graphics.fill(x, y + 13, x + 3, y + 16, 0xFFFFB040);
        }
    }

    /** Half size text right aligned at {@code right}, drawn above items. */
    private void smallText(GuiGraphicsExtractor graphics, Component text, int right, int y, int colour) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(right - font.width(text) * 0.5F, y);
        graphics.pose().scale(0.5F, 0.5F);
        graphics.text(font, text, 0, 0, colour, true);
        graphics.pose().popMatrix();
    }

    /** Whether the grid shows the last known stock of a sleeping or unreachable site. */
    private static boolean greyed() {
        RemoteSite.State state = ClientStock.viewState();
        return state != null && state != RemoteSite.State.ONLINE;
    }

    /**
     * The message line: the shown site's state when there is no message, an
     * unpowered site first; else the server's message, else a no power note
     * while the stock stays shown and requests wait for FE.
     */
    private void renderMessage(GuiGraphicsExtractor graphics) {
        RemoteSite.State state = ClientStock.viewState();
        if (ClientStock.message().isEmpty() && state != null) {
            boolean unpowered = ClientStock.sites().stream().anyMatch(row -> row.key().equals(ClientStock.view()) && !row.trade());
            String line = unpowered ? "no_power" : state.name().toLowerCase(Locale.ROOT);
            Component text = Component.translatable("gui.servus_shuttles.terminal.view." + line, ClientStock.viewName());
            messageLine(graphics, text, unpowered || state == RemoteSite.State.UNREACHABLE ? COLOUR_ALERT : COLOUR_TEXT);
            return;
        }
        if (!ClientStock.message().isEmpty()) {
            messageLine(graphics, Component.translatable("gui.servus_shuttles.terminal.message." + ClientStock.message()), COLOUR_ALERT);
        } else if (ClientStock.noPower() && ClientStock.online()) {
            messageLine(graphics, Component.translatable("gui.servus_shuttles.terminal.no_power"), COLOUR_ALERT);
        }
    }

    private void messageLine(GuiGraphicsExtractor graphics, Component text, int colour) {
        graphics.text(font, font.plainSubstrByWidth(text.getString(), Storage.MESSAGE.w()),
                leftPos + Storage.MESSAGE.x(), topPos + Storage.MESSAGE.y() + 1, colour, false);
    }

    /** The dialog panel over the grid; its widgets were drawn below it by super and are drawn again on top. */
    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Rect area = dialogRect();
        int x = dx();
        int y = dy();
        PanelPainter.bordered(graphics, x, y, area.w(), area.h(), PanelPainter.DARK, PanelPainter.FACE);
        if (dialog == Dialog.ORDER) {
            renderOrder(graphics, mouseX, mouseY);
            return;
        }
        graphics.item(subject, x + 4, y + 2);
        Component head = dialog == Dialog.CRAFT
                ? Component.translatable("gui.servus_shuttles.terminal.craft_title", subject.getHoverName())
                : subject.getHoverName();
        graphics.text(font, font.plainSubstrByWidth(head.getString(), area.w() - 28), x + 24, y + 6, COLOUR_TEXT, false);
        graphics.text(font, Component.translatable(dialog == Dialog.RULE ? "gui.servus_shuttles.terminal.rule_min"
                : "gui.servus_shuttles.terminal.amount"), x + 4, y + 22, COLOUR_TEXT, false);
        if (dialog == Dialog.RULE) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.terminal.rule_max"), x + 96, y + 22, COLOUR_TEXT, false);
        }
        for (AbstractWidget widget : dialogWidgets()) {
            if (widget.visible) {
                widget.extractRenderState(graphics, mouseX, mouseY, 0);
            }
        }
        if (dialog == Dialog.CRAFT) {
            renderPreview(graphics, x, y);
        } else if (dialog == Dialog.REQUEST) {
            renderRequestSplit(graphics, x, y);
        }
    }

    /**
     * A request above the requestable count: how many come from stock and
     * how many are crafted, the craft preview and whether it can go.
     */
    private void renderRequestSplit(GuiGraphicsExtractor graphics, int x, int y) {
        int wanted = enteredAmount();
        if (wanted <= subjectCount) {
            confirmButton.active = wanted > 0;
            return;
        }
        CraftPreviewPayload preview = currentPreview();
        confirmButton.active = preview != null && preview.requestable();
        if (preview == null) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.terminal.craft_wait"), x + 4, y + 96, COLOUR_DIM, false);
            return;
        }
        Component split = Component.translatable("gui.servus_shuttles.terminal.request_split", preview.inStock(), wanted - preview.inStock());
        graphics.text(font, font.plainSubstrByWidth(split.getString(), dialogRect().w() - 8), x + 4, y + 54, COLOUR_TEXT, false);
        previewColumns(graphics, x + 4, y + 66, preview, SHORT_PREVIEW_ROWS);
        statusLine(graphics, preview, x + 4, y + 96, dialogRect().w() - 8, 1);
    }

    /**
     * The order grid: ghost cells drawn again over the panel, the arrow and
     * result, the amount, the status, the preview and the buttons.
     */
    private void renderOrder(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PanelPainter.slots(graphics, leftPos, topPos, Storage.ORDER_GRID, 3, 9);
        List<ItemStack> cells = menu.gridCells();
        for (int cell = 0; cell < cells.size(); cell++) {
            int cx = leftPos + Storage.orderX(cell);
            int cy = topPos + Storage.orderY(cell);
            graphics.item(cells.get(cell), cx, cy);
            if (mouseX >= cx && mouseX < cx + 16 && mouseY >= cy && mouseY < cy + 16) {
                graphics.fill(cx, cy, cx + 16, cy + 16, 0x80FFFFFF);
            }
        }
        arrow(graphics, leftPos + Storage.ORDER_ARROW.x(), topPos + Storage.ORDER_ARROW.y());
        Rect result = Storage.ORDER_RESULT;
        PanelPainter.slots(graphics, leftPos, topPos, result, 1, 1);
        if (!lastResult.isEmpty()) {
            graphics.item(lastResult, leftPos + result.x(), topPos + result.y());
            graphics.itemDecorations(font, lastResult, leftPos + result.x(), topPos + result.y());
        }
        Rect label = Storage.ORDER_AMOUNT_LABEL;
        graphics.text(font, Component.translatable("gui.servus_shuttles.terminal.amount"), leftPos + label.x(), topPos + label.y(), COLOUR_TEXT, false);
        for (AbstractWidget widget : dialogWidgets()) {
            if (widget.visible) {
                widget.extractRenderState(graphics, mouseX, mouseY, 0);
            }
        }
        CraftPreviewPayload preview = currentPreview();
        confirmButton.active = preview != null && preview.startable();
        Rect status = Storage.ORDER_STATUS;
        int statusX = leftPos + status.x();
        int statusY = topPos + status.y();
        if (asking) {
            wrapped(graphics, Component.translatable("gui.servus_shuttles.terminal.order_ask"), statusX, statusY, status.w(), COLOUR_TEXT);
        } else if (lastResult.isEmpty()) {
            wrapped(graphics, Component.translatable("gui.servus_shuttles.terminal.message.no_match"), statusX, statusY, status.w(), COLOUR_DIM);
        } else if (preview == null) {
            wrapped(graphics, Component.translatable("gui.servus_shuttles.terminal.craft_wait"), statusX, statusY, status.w(), COLOUR_DIM);
        } else {
            statusLine(graphics, preview, statusX, statusY, status.w(), 3);
        }
        if (preview != null && !lastResult.isEmpty()) {
            Rect rows = Storage.ORDER_PREVIEW;
            previewColumns(graphics, leftPos + rows.x(), topPos + rows.y(), preview, SHORT_PREVIEW_ROWS);
        }
        if (mouseX >= leftPos + result.x() && mouseX < leftPos + result.x() + 16 && mouseY >= topPos + result.y()
                && mouseY < topPos + result.y() + 16 && !lastResult.isEmpty()) {
            graphics.setTooltipForNextFrame(font, lastResult, mouseX, mouseY);
        }
    }

    /** Text wrapped to a width, at most as many lines as fit 30 pixels. */
    private void wrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int colour) {
        var lines = font.split(text, width);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            graphics.text(font, lines.get(i), x, y + i * 10, colour, false);
        }
    }

    /** Why a preview cannot start, or that it is ready, in up to {@code lines} lines. */
    private void statusLine(GuiGraphicsExtractor graphics, CraftPreviewPayload preview, int x, int y, int width, int lines) {
        Component status;
        int colour;
        if (!preview.refusal().isEmpty()) {
            status = Component.translatable("gui.servus_shuttles.terminal.message." + preview.refusal());
            colour = COLOUR_ALERT;
        } else if (!preview.missing().isEmpty()) {
            status = Component.translatable("gui.servus_shuttles.terminal.message.missing");
            colour = COLOUR_ALERT;
        } else {
            status = Component.translatable("gui.servus_shuttles.terminal.craft_ready");
            colour = COLOUR_GOOD;
        }
        if (lines > 1) {
            wrapped(graphics, status, x, y, width, colour);
        } else {
            graphics.text(font, font.plainSubstrByWidth(status.getString(), width), x, y, colour, false);
        }
    }

    private void previewColumns(GuiGraphicsExtractor graphics, int x, int y, CraftPreviewPayload preview, int rows) {
        previewColumn(graphics, x, y, "gui.servus_shuttles.terminal.craft_from_stock", preview.fromStock(), COLOUR_TEXT, rows);
        previewColumn(graphics, x + 56, y, "gui.servus_shuttles.terminal.craft_to_craft", preview.toCraft(), COLOUR_GOOD, rows);
        previewColumn(graphics, x + 112, y, "gui.servus_shuttles.terminal.craft_missing", preview.missing(), COLOUR_ALERT, rows);
    }

    /** What the craft would take from stock, craft and miss, and whether it can start; Start waits for it. */
    private void renderPreview(GuiGraphicsExtractor graphics, int x, int y) {
        CraftPreviewPayload preview = currentPreview();
        confirmButton.active = preview != null && preview.startable();
        int statusY = y + 96;
        if (preview == null) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.terminal.craft_wait"), x + 4, statusY, COLOUR_DIM, false);
            return;
        }
        previewColumn(graphics, x + 4, y + 54, "gui.servus_shuttles.terminal.craft_from_stock", preview.fromStock(), COLOUR_TEXT, PREVIEW_ROWS);
        previewColumn(graphics, x + 60, y + 54, "gui.servus_shuttles.terminal.craft_to_craft", preview.toCraft(), COLOUR_GOOD, PREVIEW_ROWS);
        previewColumn(graphics, x + 116, y + 54, "gui.servus_shuttles.terminal.craft_missing", preview.missing(), COLOUR_ALERT, PREVIEW_ROWS);
        statusLine(graphics, preview, x + 4, statusY, dialogRect().w() - 8, 1);
    }

    private void previewColumn(GuiGraphicsExtractor graphics, int x, int y, String heading, List<StockEntry> rows, int colour, int shown) {
        graphics.text(font, font.plainSubstrByWidth(Component.translatable(heading).getString(), 54), x, y, colour, false);
        for (int i = 0; i < Math.min(shown, rows.size()); i++) {
            StockEntry row = rows.get(i);
            int rowY = y + 10 + i * 9;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, rowY);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.item(row.stack(), 0, 0);
            graphics.pose().popMatrix();
            String text = row.count() + " " + row.stack().getHoverName().getString();
            graphics.text(font, font.plainSubstrByWidth(text, 44), x + 10, rowY, COLOUR_TEXT, false);
        }
        if (rows.size() > shown) {
            smallText(graphics, Component.translatable("gui.servus_shuttles.terminal.craft_more", rows.size() - shown),
                    x + 54, y + 10 + shown * 9, COLOUR_DIM);
        }
    }

    /** A count in at most four characters: 999, 1.2k, 45k, 1.5M. */
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

    /** The rows above the base top and the side column belong to the screen too. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        int x = (int) mouseX - left;
        int y = (int) mouseY - top;
        boolean panel = x >= 0 && x < imageWidth && y >= layout.top() && y < Storage.BASE_HEIGHT;
        boolean side = x >= Storage.SIDE_X && x < 0 && y >= layout.top() && y < Storage.BASE_HEIGHT;
        return !panel && !side;
    }

    /** Whether the title is the site selector: this site is wireless capable and has other sites to show. */
    private boolean siteSelector() {
        return ClientStock.loaded() && !ClientStock.sites().isEmpty();
    }

    private boolean overTitle(double mouseX, double mouseY) {
        Rect title = layout.title();
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        return x >= title.x() && x < title.right() && y >= title.y() - 2 && y < title.bottom() + 2;
    }

    /** The key of the next (or previous) site after the one shown; this site is the empty key, first in the cycle. */
    private String nextSite(int step) {
        List<String> keys = new ArrayList<>();
        keys.add("");
        ClientStock.sites().forEach(row -> keys.add(row.key()));
        int at = Math.max(0, keys.indexOf(ClientStock.view()));
        return keys.get(Math.floorMod(at + step, keys.size()));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!siteSelector()) {
            super.extractLabels(graphics, mouseX, mouseY);
            return;
        }
        String name = ClientStock.view().isEmpty() ? Component.translatable("gui.servus_shuttles.terminal.this_site").getString()
                : ClientStock.viewName();
        String text = font.plainSubstrByWidth("< " + name, layout.title().w() - font.width(" >")) + " >";
        int colour = overTitle(mouseX, mouseY) ? 0xFF2050A0 : COLOUR_TEXT;
        graphics.text(font, text, titleLabelX, titleLabelY, colour, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOUR_TEXT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (!dialogOpen() && siteSelector() && overTitle(mouseX, mouseY)
                && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            ClientPacketDistributor.sendToServer(new SelectSitePayload(nextSite(button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1 : -1)));
            scrollRow = 0;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && search != null && search.visible && search.isMouseOver(mouseX, mouseY)) {
            search.setValue("");
            scrollRow = 0;
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        if (dialogOpen()) {
            for (AbstractWidget widget : dialogWidgets()) {
                if (widget.visible && widget.mouseClicked(event, doubleClick)) {
                    setFocused(widget);
                    return true;
                }
            }
            boolean ghost = dialog == Dialog.ORDER && Storage.ORDER_GRID.contains((int) mouseX - leftPos, (int) mouseY - topPos);
            return !ghost && insideDialog(mouseX, mouseY) || super.mouseClicked(event, doubleClick);
        }
        if (ClientStock.online() && cellAt(mouseX, mouseY) >= 0 && clickGrid(cellUnder(mouseX, mouseY), button)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * A click on the grid: a request cancels; a carried stack is stored; an
     * item opens the request dialog, or the craft dialog when it is not stocked
     * or is middle or control clicked while craftable; shift requests a stack
     * at once, or all there is when the stock is smaller.
     */
    private boolean clickGrid(Optional<Cell> cell, int button) {
        if (cell.isPresent() && cell.get().isRequest()) {
            ClientPacketDistributor.sendToServer(new CancelRequestPayload(cell.get().request().id()));
            return true;
        }
        if (!menu.getCarried().isEmpty()) {
            ClientPacketDistributor.sendToServer(PushCarriedPayload.INSTANCE);
            return true;
        }
        if (cell.isEmpty()) {
            return false;
        }
        Cell found = cell.get();
        boolean craftClick = button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || Minecraft.getInstance().hasControlDown();
        if (!found.stocked() || (found.craftable() && craftClick)) {
            openCraft(found.stack());
        } else if (Minecraft.getInstance().hasShiftDown()) {
            int count = (int) Math.min(found.stack().getMaxStackSize(), found.count());
            ClientPacketDistributor.sendToServer(new RequestItemPayload(found.stack(), count));
        } else {
            openRequest(found);
        }
        return true;
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
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (asking) {
                asking = false;
                applyDialog();
                return true;
            }
            if (dialogOpen()) {
                closeDialog();
                return true;
            }
            return super.keyPressed(event);
        }
        if (dialogOpen()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirm();
                return true;
            }
            if (maximum.isFocused() && (maximum.keyPressed(event) || maximum.canConsumeInput())) {
                return true;
            }
            amount.keyPressed(event);
            return true;
        }
        if (search.isFocused() && (search.keyPressed(event) || search.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(event);
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
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            extractDefaultSprite(graphics);
            Identifier texture = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID,
                    "textures/gui/terminal/" + icon.get() + ".png");
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + (width - 16) / 2, getY() + (height - 16) / 2, 0, 0, 16, 16, 16, 16);
        }
    }
}
