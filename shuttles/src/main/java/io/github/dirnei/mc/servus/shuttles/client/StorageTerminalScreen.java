package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.client.TerminalCells.Cell;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.site.RemoteSite;
import io.github.dirnei.mc.servus.shuttles.terminal.CancelRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.LineWarehouse;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftStartPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.GridFillPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.OrderSavePayload;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import io.github.dirnei.mc.servus.foundation.client.ItemListView;
import io.github.dirnei.mc.servus.foundation.client.NumberStepper;
import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import io.github.dirnei.mc.servus.foundation.ui.AmountSteps;
import io.github.dirnei.mc.servus.foundation.ui.ListScroll;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternEditing;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Refined Storage style Storage Terminal: view buttons left of the panel,
 * a Craft button and search, the item grid, a message line, the Input and
 * Output rows with the requests on the Output row, the player inventory; an
 * amount prompt for requests and rules, and the order grid followed by the
 * amount panel it shares with the craft dialog, each with a server preview. Everything sits where {@link Storage} says; extra
 * grid rows grow the panel upwards.
 */
public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {

    private static final int COLUMNS = Storage.COLUMNS;
    private static final int CELL = TerminalLayout.CELL;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_DIM = 0xFF707070;
    private static final int COLOUR_ALERT = 0xFFB03030;
    /** Length of the grid's scrollbar thumb. */
    private static final int THUMB = 15;
    /** The alert colour on the dark item grid. */
    private static final int COLOUR_ALERT_ON_GRID = 0xFFFF6060;
    private static final int COLOUR_GOOD = 0xFF2E7D32;
    /** Preview rows under the request split line. */
    private static final int SHORT_PREVIEW_ROWS = 2;
    private static final String[] CRAFT_LISTS = {"craft_from_stock", "craft_to_craft", "craft_missing"};
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
    private IconButton craftButton;
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
    /** Whether the amount panel came from the order grid: Back returns there and Start orders the grid's recipe. */
    private boolean fromOrder;
    private NumberStepper craftAmount;
    private final List<ItemListView> craftLists = new ArrayList<>();
    private IconButton saveButton;
    private Button backButton;
    private Button clearButton;
    private Button nextButton;
    private Button startButton;
    /** The ghost cells one drag of the cursor's item has set, so each is clicked once. */
    private final Set<Integer> draggedCells = new HashSet<>();
    /** Set when a dialog took a press, so its release puts no carried stack into a slot behind it. */
    private boolean swallowRelease;
    /** Whether the left button went down on the grid's scrollbar and still holds the thumb. */
    private boolean draggingThumb;
    /** The reason Next or Start is greyed out, null while it can be pressed. */
    private String nextReason;
    private String startReason;
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
        Rect craft = layout.craft();
        areas.add(new Rect2i(leftPos + craft.x(), topPos + craft.y(), craft.w(), craft.h()));
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
        craftButton = sideButton(craftRect, () -> "craft", b -> openOrder());
        craftButton.setTooltip(Tooltip.create(Component.translatable("gui.servus_shuttles.terminal.craft_button_hint")));
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
        initCraftPanel();
    }

    /** The order grid's Save, Back or Cancel and Next, and the amount panel's stepper, lists and Start. */
    private void initCraftPanel() {
        Rect save = Storage.ORDER_SAVE;
        saveButton = addRenderableWidget(new IconButton(leftPos + save.x(), topPos + save.y(), save.w(), save.h(), () -> "save_pattern",
                b -> savePattern()));
        saveButton.setTooltip(Tooltip.create(Component.translatable("gui.servus_shuttles.terminal.order_save")));
        Rect order = Storage.ORDER_BUTTONS;
        Rect row = Storage.CRAFT_BUTTONS;
        backButton = addRenderableWidget(Button.builder(Component.empty(), b -> back())
                .bounds(leftPos + row.x(), topPos + row.y(), Storage.BUTTON, row.h()).build());
        clearButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.order_clear"), b -> clearGrid())
                .bounds(leftPos + order.x() + (order.w() - Storage.BUTTON) / 2, topPos + order.y(), Storage.BUTTON, order.h()).build());
        nextButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.order_next"),
                b -> openOrderAmount()).bounds(leftPos + order.right() - Storage.WIDE_BUTTON, topPos + order.y(), Storage.WIDE_BUTTON, order.h())
                .build());
        startButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.craft_start"),
                b -> start()).bounds(leftPos + row.right() - Storage.WIDE_BUTTON, topPos + row.y(), Storage.WIDE_BUTTON, row.h()).build());
        nextReason = null;
        startReason = null;
        int kept = craftAmount == null ? AmountSteps.MIN : craftAmount.value();
        craftAmount = new NumberStepper(font, Component.translatable("gui.servus_shuttles.terminal.amount"), AmountSteps.MIN,
                AmountSteps.MAX, () -> craftItem().isEmpty() ? 64 : craftItem().getMaxStackSize(), value -> askPreview(),
                1, 10, AmountSteps.STACK);
        craftAmount.widgets().forEach(this::addRenderableWidget);
        craftAmount.place(leftPos + Storage.CRAFT_STEPPER.x(), topPos + Storage.CRAFT_STEPPER.y());
        craftAmount.setValue(kept);
        craftLists.clear();
        Rect lists = Storage.CRAFT_LISTS;
        for (int i = 0; i < CRAFT_LISTS.length; i++) {
            ItemListView list = new ItemListView(Component.translatable("gui.servus_shuttles.terminal." + CRAFT_LISTS[i]));
            list.setArea(leftPos + Storage.craftListX(i), topPos + lists.y(), Storage.CRAFT_LIST_WIDTH, lists.h());
            craftLists.add(list);
        }
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
        fromOrder = false;
        lastResult = ItemStack.EMPTY;
        showOrderGrid();
    }

    private void showOrderGrid() {
        ClientStock.clearPreview();
        askedAmount = -1;
        askedGrid = List.of();
        applyDialog();
        askPreview();
    }

    /** Next: the amount panel for the order grid's result. */
    private void openOrderAmount() {
        if (dialog != Dialog.ORDER || lastResult.isEmpty()) {
            return;
        }
        dialog = Dialog.CRAFT;
        fromOrder = true;
        showAmountPanel();
    }

    /** Back to the order grid from its amount panel, or Cancel. */
    private void back() {
        if (dialog == Dialog.CRAFT && fromOrder) {
            dialog = Dialog.ORDER;
            fromOrder = false;
            showOrderGrid();
        } else {
            cancel();
        }
    }

    /** Cancel or Escape: closes the open dialog; the order grid's cells are cleared. */
    private void cancel() {
        if (orderFlow()) {
            clearGrid();
        }
        closeDialog();
    }

    /** Whether the order grid or its amount panel is open. */
    private boolean orderFlow() {
        return dialog == Dialog.ORDER || dialog == Dialog.CRAFT && fromOrder;
    }

    /** The order grid and the amount panel are popups; the other dialogs lie over the grid. */
    private boolean popupOpen() {
        return dialog == Dialog.ORDER || dialog == Dialog.CRAFT;
    }

    private Rect popupRect() {
        return dialog == Dialog.ORDER ? Storage.ORDER_POPUP : Storage.CRAFT_POPUP;
    }

    /** Empties every ghost cell of the order grid. */
    private void clearGrid() {
        ClientPacketDistributor.sendToServer(GridFillPayload.CLEAR);
    }

    private void showAmountPanel() {
        ClientStock.clearPreview();
        askedAmount = -1;
        askedGrid = List.of();
        craftAmount.setValue(1);
        craftLists.forEach(ItemListView::scrollToTop);
        applyDialog();
        focus(craftAmount.field());
        askPreview();
    }

    /** The item the amount panel makes: the order grid's result or the clicked craftable. */
    private ItemStack craftItem() {
        return fromOrder ? lastResult : subject;
    }

    /** Saves the order grid as a pattern without ordering, then asks again whether it is saved. */
    private void savePattern() {
        ClientPacketDistributor.sendToServer(OrderSavePayload.INSTANCE);
        askedGrid = List.of();
        askPreview();
    }

    /** The result the order grid's ghost cells make, on the client, empty when they match nothing. */
    private ItemStack orderResult() {
        Level level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            return ItemStack.EMPTY;
        }
        return PatternEditing.encode(level, menu.gridCells()).map(EncodedPattern::result).orElse(ItemStack.EMPTY);
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
        if (dialog == Dialog.ORDER || dialog == Dialog.CRAFT && fromOrder) {
            if (!ClientStock.orderGrid()) {
                closeDialog();
            } else {
                followResult();
                askPreview();
            }
        }
        if (craftAmount != null) {
            craftAmount.tick();
        }
        if (craftButton != null) {
            craftButton.visible = !dialogOpen() && ClientStock.orderGrid();
        }
    }

    /** Follows the result of the order grid's cells. */
    private void followResult() {
        ItemStack result = orderResult();
        if (dialog == Dialog.ORDER && !ItemStack.matches(result, lastResult)) {
            lastResult = result.copy();
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
        fromOrder = false;
        showAmountPanel();
    }

    private void focusAmount() {
        focus(amount);
    }

    private void focus(EditBox field) {
        setFocused(field);
        field.setFocused(true);
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
        boolean panel = dialog == Dialog.CRAFT;
        boolean prompt = dialog == Dialog.REQUEST || dialog == Dialog.RULE;
        menu.setOrderOpen(order);
        craftButton.visible = !open && ClientStock.orderGrid();
        search.visible = !open;
        sideButtons.forEach(button -> button.visible = !open);
        amount.visible = prompt;
        maximum.visible = dialog == Dialog.RULE;
        int stack = subject.isEmpty() ? 64 : subject.getMaxStackSize();
        String[] labels = {"-" + stack, "-10", "-1", "+1", "+10", "+" + stack};
        for (int i = 0; i < stepButtons.size(); i++) {
            Button button = stepButtons.get(i);
            button.visible = dialog == Dialog.REQUEST;
            button.setMessage(Component.literal(labels[i]));
        }
        confirmButton.visible = prompt;
        confirmButton.active = true;
        confirmButton.setMessage(Component.translatable(dialog == Dialog.RULE ? "gui.servus_shuttles.terminal.rule_save"
                : "gui.servus_shuttles.terminal.request"));
        ruleButton.visible = prompt;
        ruleButton.setMessage(Component.translatable(dialog == Dialog.RULE ? "gui.servus_shuttles.terminal.request" : "gui.servus_shuttles.terminal.rule"));
        removeRuleButton.visible = dialog == Dialog.RULE;
        cancelButton.visible = prompt;
        craftAmount.setVisible(panel);
        saveButton.visible = false;
        backButton.visible = order || panel;
        Rect backRow = order ? Storage.ORDER_BUTTONS : Storage.CRAFT_BUTTONS;
        backButton.setPosition(leftPos + backRow.x(), topPos + backRow.y());
        clearButton.visible = order;
        backButton.setMessage(Component.translatable(panel && fromOrder ? "gui.servus_shuttles.terminal.back" : "gui.servus_shuttles.terminal.cancel"));
        nextButton.visible = order;
        startButton.visible = panel;
    }

    private List<AbstractWidget> dialogWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>(List.of(amount, maximum, removeRuleButton, ruleButton, confirmButton, cancelButton,
                saveButton, backButton, clearButton, nextButton, startButton));
        widgets.addAll(stepButtons);
        widgets.addAll(craftAmount.widgets());
        return widgets;
    }

    private void closeDialog() {
        dialog = Dialog.NONE;
        fromOrder = false;
        subject = ItemStack.EMPTY;
        amount.setFocused(false);
        maximum.setFocused(false);
        craftAmount.field().setFocused(false);
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
        int stack = subject.isEmpty() ? 64 : subject.getMaxStackSize();
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
        switch (dialog) {
            case REQUEST -> {
                int wanted = enteredAmount();
                if (!subject.isEmpty() && wanted > subjectCount && wanted != askedAmount) {
                    askedAmount = wanted;
                    ClientPacketDistributor.sendToServer(new CraftPreviewRequestPayload(subject, wanted, CraftPreviewRequestPayload.Mode.REQUEST));
                }
            }
            case CRAFT -> {
                int wanted = craftAmount.value();
                if (fromOrder) {
                    askOrderPreview(wanted);
                } else if (!subject.isEmpty() && wanted != askedAmount) {
                    askedAmount = wanted;
                    ClientPacketDistributor.sendToServer(new CraftPreviewRequestPayload(subject, wanted, CraftPreviewRequestPayload.Mode.CRAFT));
                }
            }
            case ORDER -> askOrderPreview(previewAmount());
            default -> {
            }
        }
    }

    /** Asks the order grid's preview for the amount when the amount or the cells changed. */
    private void askOrderPreview(int wanted) {
        List<ItemStack> cells = menu.gridCells();
        if (wanted != askedAmount || !sameCells(cells, askedGrid)) {
            askedAmount = wanted;
            askedGrid = cells.stream().map(ItemStack::copy).toList();
            ClientPacketDistributor.sendToServer(new CraftPreviewRequestPayload(ItemStack.EMPTY, wanted, CraftPreviewRequestPayload.Mode.ORDER));
        }
    }

    /** The amount the open dialog's preview is for: the order grid's one craft, the amount panel's or the prompt's amount. */
    private int previewAmount() {
        return switch (dialog) {
            case ORDER -> lastResult.isEmpty() ? 1 : lastResult.getCount();
            case CRAFT -> craftAmount.value();
            default -> enteredAmount();
        };
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
        boolean order = dialog == Dialog.ORDER || dialog == Dialog.CRAFT && fromOrder;
        ItemStack item = order ? lastResult : subject;
        if (preview == null || preview.amount() != previewAmount() || !ItemStack.isSameItemSameComponents(preview.item(), item)
                || order && !sameCells(menu.gridCells(), askedGrid)) {
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
                openOrderAmount();
                return;
            }
            case CRAFT -> {
                start();
                return;
            }
            case RULE -> ClientPacketDistributor.sendToServer(new SetStockRulePayload(subject, wanted, Math.max(wanted, parsed(maximum, wanted))));
            default -> {
            }
        }
        closeDialog();
    }

    /** Start in the amount panel: orders the order grid's recipe once, or crafts the clicked item. */
    private void start() {
        CraftPreviewPayload preview = currentPreview();
        if (preview == null || !preview.startable()) {
            return;
        }
        int wanted = craftAmount.value();
        if (fromOrder) {
            ClientPacketDistributor.sendToServer(new OrderStartPayload(wanted));
        } else {
            ClientPacketDistributor.sendToServer(new CraftStartPayload(subject, wanted));
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

    /** An item drawn in the grid and the corner it is drawn at, on the screen. */
    public record GridItem(ItemStack stack, int x, int y) {
    }

    /** The stock item under the mouse in the grid, for JEI's recipe and usage keys. */
    public Optional<GridItem> gridItemAt(double mouseX, double mouseY) {
        int index = cellAt(mouseX, mouseY);
        List<Cell> list = cells();
        if (index < 0 || index >= list.size()) {
            return Optional.empty();
        }
        int shown = index - scrollRow * COLUMNS;
        Rect grid = layout.grid();
        return Optional.of(new GridItem(list.get(index).stack(), leftPos + grid.x() + shown % COLUMNS * CELL,
                topPos + grid.y() + shown / COLUMNS * CELL));
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

    private boolean overScrollbar(double mouseX, double mouseY) {
        return layout.scrollbar().contains((int) mouseX - leftPos, (int) mouseY - topPos);
    }

    /** Scrolls the grid so the thumb centres on the mouse, as far as the grid scrolls. */
    private void dragThumb(double mouseY) {
        Rect bar = layout.scrollbar();
        scrollRow = ListScroll.offsetAt(mouseY - topPos - bar.y(), THUMB, bar.h(), maxScrollRow());
    }

    private void renderScrollThumb(GuiGraphicsExtractor graphics) {
        int max = maxScrollRow();
        Rect bar = layout.scrollbar();
        int track = bar.h() - THUMB;
        int thumb = topPos + bar.y() + (max == 0 ? 0 : track * scrollRow / max);
        int thumbX = leftPos + bar.x();
        graphics.fill(thumbX, thumb, thumbX + bar.w(), thumb + THUMB, max == 0 ? 0xFF9A9A9A : 0xFFE0E0E0);
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

    /** The grid or the open dialog, drawn before the carried item so a dragged stack stays on top. */
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        if (!ClientStock.loaded() || !ClientStock.online()) {
            Component text = !ClientStock.loaded() ? Component.translatable("gui.servus_shuttles.terminal.loading")
                    : Component.translatable("gui.servus_shuttles.terminal." + ClientStock.reason());
            Rect grid = layout.grid();
            int colour = LineWarehouse.CONFLICT_KEY.equals(ClientStock.reason()) ? COLOUR_ALERT_ON_GRID : 0xFFFFFFFF;
            graphics.centeredText(font, text, leftPos + grid.x() + grid.w() / 2, topPos + grid.y() + grid.h() / 2 - 4, colour);
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

    private List<Component> requestTooltip(RequestRow row) {
        return List.of(row.stack().getHoverName(),
                Component.translatable("gui.servus_shuttles.terminal.request_line", row.delivered(), row.requested()),
                stateText(row),
                Component.translatable("gui.servus_shuttles.terminal.cancel_hint"));
    }

    private List<Component> cellTooltip(Cell cell) {
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
     * One cell: an item with its count or a craft label, a blue corner for a
     * stock rule and an orange corner when a stocked item is craftable too.
     */
    private void renderCell(GuiGraphicsExtractor graphics, Cell cell, int x, int y, boolean greyed) {
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

    /** The open request owning an output slot of the menu, or null. */
    private RequestRow requestAt(Slot slot) {
        if (slot == null || slot.index >= menu.outputSlots()) {
            return null;
        }
        for (RequestRow row : ClientStock.requests()) {
            if (row.owns(slot.index)) {
                return row;
            }
        }
        return null;
    }

    /**
     * Each request on its output slot: the item faded while nothing has
     * arrived, delivered of requested at the top, a state mark in the corner
     * and a progress bar along the bottom edge.
     */
    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        for (RequestRow row : ClientStock.requests()) {
            for (int index : row.slots()) {
                if (index >= 0 && index < menu.outputSlots()) {
                    extractRequest(graphics, row, menu.slots.get(index));
                }
            }
        }
    }

    /** One request on one of its output slots. */
    private void extractRequest(GuiGraphicsExtractor graphics, RequestRow row, Slot slot) {
        int x = slot.x;
        int y = slot.y;
        if (!slot.hasItem()) {
            graphics.fakeItem(row.stack(), x, y);
            graphics.fill(x, y, x + 16, y + 16, 0x908B8B8B);
        }
        smallText(graphics, Component.literal(row.delivered() + "/" + row.requested()), x + 16, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 4, y + 4, 0xFF202020);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, stateColour(row.state()));
        int filled = row.requested() <= 0 ? 0 : 16 * Math.min(row.delivered(), row.requested()) / row.requested();
        graphics.fill(x, y + 16, x + 16, y + 17, 0xFF202020);
        graphics.fill(x, y + 16, x + filled, y + 17, stateColour(row.state()));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (popupOpen() && hoveredSlot != null && hoveredSlot.y < Storage.INVENTORY_LABEL.y() && !menu.isGridSlot(hoveredSlot.index)) {
            return;
        }
        RequestRow row = menu.getCarried().isEmpty() ? requestAt(hoveredSlot) : null;
        if (row != null) {
            graphics.setComponentTooltipForNextFrame(font, requestTooltip(row), mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
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
        if (popupOpen()) {
            Rect popup = popupRect();
            int dimTop = topPos + layout.top();
            PopupPanel.draw(graphics, leftPos + Storage.SIDE_X, dimTop, imageWidth - Storage.SIDE_X, topPos + Storage.INVENTORY_LABEL.y() - 1 - dimTop,
                    leftPos + popup.x(), topPos + popup.y(), popup.w(), popup.h());
            if (dialog == Dialog.ORDER) {
                renderOrder(graphics, mouseX, mouseY);
            } else {
                renderCraftPanel(graphics, mouseX, mouseY);
            }
            return;
        }
        Rect area = dialogRect();
        int x = dx();
        int y = dy();
        PanelPainter.bordered(graphics, x, y, area.w(), area.h(), PanelPainter.DARK, PanelPainter.FACE);
        graphics.item(subject, x + 4, y + 2);
        graphics.text(font, font.plainSubstrByWidth(subject.getHoverName().getString(), area.w() - 28), x + 24, y + 6, COLOUR_TEXT, false);
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
        if (dialog == Dialog.REQUEST) {
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
        statusLine(graphics, preview, x + 4, y + 96, dialogRect().w() - 8);
    }

    /**
     * The order grid: ghost cells drawn again over the panel, the arrow, the
     * result with one craft's count, Save while no pattern has the recipe,
     * Cancel and Next.
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
        CraftPreviewPayload preview = currentPreview();
        saveButton.visible = !lastResult.isEmpty() && preview != null && preview.refusal().isEmpty() && !preview.patternSaved();
        nextReason = greyOut(nextButton, nextReason, lastResult.isEmpty() ? "gui.servus_shuttles.terminal.message.no_match" : null);
        renderDialogWidgets(graphics, mouseX, mouseY);
        if (!lastResult.isEmpty()) {
            renderItem(graphics, lastResult, result.x(), result.y(), mouseX, mouseY);
        }
    }

    /**
     * The amount panel: title, the stepper with the item and the count that
     * gets crafted beside its field, the From stock, To craft and Missing
     * lists, Back or Cancel and Start.
     */
    private void renderCraftPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack item = craftItem();
        CraftPreviewPayload preview = currentPreview();
        Rect title = Storage.CRAFT_TITLE;
        Component head = Component.translatable("gui.servus_shuttles.terminal.craft_title", item.getHoverName());
        graphics.text(font, font.plainSubstrByWidth(head.getString(), title.w()), leftPos + title.x(), topPos + title.y(), COLOUR_TEXT, false);
        startReason = greyOut(startButton, startReason, startRefusal(preview));
        renderDialogWidgets(graphics, mouseX, mouseY);
        List<List<StockEntry>> rows = preview == null ? List.of(List.of(), List.of(), List.of())
                : List.of(preview.fromStock(), preview.toCraft(), preview.missing());
        for (int i = 0; i < craftLists.size(); i++) {
            ItemListView list = craftLists.get(i);
            list.setRows(rows.get(i).stream().map(entry -> new ItemListView.Row(entry.stack(), entry.count())).toList());
            list.render(graphics, font, COLOUR_TEXT, mouseX, mouseY);
        }
        if (!item.isEmpty()) {
            Rect at = Storage.CRAFT_ITEM;
            renderItem(graphics, item.copyWithCount(craftedCount(preview, item)), at.x(), at.y(), mouseX, mouseY);
        }
    }

    private void renderDialogWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (AbstractWidget widget : dialogWidgets()) {
            if (widget.visible) {
                widget.extractRenderState(graphics, mouseX, mouseY, 0);
            }
        }
    }

    /** An item with its count at a panel offset, without a slot frame, and its tooltip while hovered. */
    private void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        int itemX = leftPos + x;
        int itemY = topPos + y;
        graphics.item(stack, itemX, itemY);
        graphics.itemDecorations(font, stack, itemX, itemY);
        if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
            graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        }
    }

    /** How many of the item the amount gets crafted: the preview's To craft entry of it, else the amount. */
    private int craftedCount(CraftPreviewPayload preview, ItemStack item) {
        if (preview != null) {
            for (StockEntry entry : preview.toCraft()) {
                if (ItemStack.isSameItemSameComponents(entry.stack(), item)) {
                    return (int) Math.min(Integer.MAX_VALUE, entry.count());
                }
            }
        }
        return craftAmount.value();
    }

    /** Why Start cannot be pressed, as a lang key, or null when it can. */
    private static String startRefusal(CraftPreviewPayload preview) {
        if (preview == null) {
            return "gui.servus_shuttles.terminal.craft_wait";
        }
        if (!preview.refusal().isEmpty()) {
            return "gui.servus_shuttles.terminal.message." + preview.refusal();
        }
        if (!preview.missing().isEmpty()) {
            return "gui.servus_shuttles.terminal.message.missing";
        }
        return preview.startable() ? null : "gui.servus_shuttles.terminal.craft_wait";
    }

    /**
     * Greys the button out with the reason as its tooltip, or enables it
     * without one; the tooltip is only replaced when the reason changes.
     *
     * @return the reason now shown
     */
    private static String greyOut(Button button, String shown, String reason) {
        button.active = reason == null;
        if (!Objects.equals(shown, reason)) {
            button.setTooltip(reason == null ? null : Tooltip.create(Component.translatable(reason)));
        }
        return reason;
    }

    private void statusLine(GuiGraphicsExtractor graphics, CraftPreviewPayload preview, int x, int y, int width) {
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
        graphics.text(font, font.plainSubstrByWidth(status.getString(), width), x, y, colour, false);
    }

    private void previewColumns(GuiGraphicsExtractor graphics, int x, int y, CraftPreviewPayload preview, int rows) {
        previewColumn(graphics, x, y, "gui.servus_shuttles.terminal.craft_from_stock", preview.fromStock(), COLOUR_TEXT, rows);
        previewColumn(graphics, x + 56, y, "gui.servus_shuttles.terminal.craft_to_craft", preview.toCraft(), COLOUR_GOOD, rows);
        previewColumn(graphics, x + 112, y, "gui.servus_shuttles.terminal.craft_missing", preview.missing(), COLOUR_ALERT, rows);
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

    /** The rows above the base top, the side column and an open popup belong to the screen too. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        int x = (int) mouseX - left;
        int y = (int) mouseY - top;
        boolean panel = x >= 0 && x < imageWidth && y >= layout.top() && y < Storage.BASE_HEIGHT;
        boolean side = x >= Storage.SIDE_X && x < 0 && y >= layout.top() && y < Storage.BASE_HEIGHT
                || layout.craft().contains(x, y);
        boolean popup = popupOpen() && popupRect().contains(x, y);
        return !panel && !side && !popup;
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
        blurSearchUnlessOver(mouseX, mouseY);
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
                    swallowRelease = true;
                    return true;
                }
            }
            int cell = ghostCellAt(mouseX, mouseY);
            draggedCells.clear();
            if (cell >= 0) {
                draggedCells.add(cell);
                if (menu.getCarried().isEmpty()) {
                    return super.mouseClicked(event, doubleClick);
                }
                clickGhostCell(cell, button);
                swallowRelease = true;
                return true;
            }
            if (popupOpen() ? overDimmed(mouseX, mouseY) : insideDialog(mouseX, mouseY)) {
                swallowRelease = true;
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && overScrollbar(mouseX, mouseY)) {
            draggingThumb = true;
            dragThumb(mouseY);
            return true;
        }
        if (ClientStock.online() && cellAt(mouseX, mouseY) >= 0 && clickGrid(cellUnder(mouseX, mouseY), button)) {
            return true;
        }
        RequestRow request = requestAt(hoveredSlot);
        boolean cancelClick = button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                || button == GLFW.GLFW_MOUSE_BUTTON_LEFT && Minecraft.getInstance().hasControlDown();
        if (request != null && cancelClick) {
            ClientPacketDistributor.sendToServer(new CancelRequestPayload(request.id()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Takes the focus from the search box, keeping its text, when a press lands anywhere else. */
    private void blurSearchUnlessOver(double mouseX, double mouseY) {
        if (search == null || !search.isFocused() || search.isMouseOver(mouseX, mouseY)) {
            return;
        }
        search.setFocused(false);
        if (getFocused() == search) {
            setFocused(null);
        }
    }

    /**
     * A click on the grid: a carried stack is stored; an item is requested
     * once, a stack at once with shift (or all there is when the stock is
     * smaller), the amount prompt opens on a right click, and the craft dialog
     * when it is not stocked or is middle or control clicked while craftable.
     */
    private boolean clickGrid(Optional<Cell> cell, int button) {
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
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            openRequest(found);
        } else {
            ClientPacketDistributor.sendToServer(new RequestItemPayload(found.stack(), 1));
        }
        return true;
    }

    /** The order grid's ghost cell under the mouse while it is open, else -1. */
    private int ghostCellAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        Rect grid = Storage.ORDER_GRID;
        if (dialog != Dialog.ORDER || !grid.contains(x, y)) {
            return -1;
        }
        return (y - grid.y()) / CELL * 3 + (x - grid.x()) / CELL;
    }

    /** Whether the mouse is over the popup or the dimmed part of the terminal above the inventory. */
    private boolean overDimmed(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        return popupRect().contains(x, y)
                || x >= Storage.SIDE_X && x < imageWidth && y >= layout.top() && y < Storage.INVENTORY_LABEL.y();
    }

    /** Dragging the cursor's item over the order grid sets a ghost copy in each cell it passes. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingThumb) {
            dragThumb(event.y());
            return true;
        }
        int cell = ghostCellAt(event.x(), event.y());
        if (cell >= 0 && !menu.getCarried().isEmpty()) {
            if (draggedCells.add(cell)) {
                clickGhostCell(cell, event.button());
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    private void clickGhostCell(int cell, int button) {
        Slot slot = menu.slots.get(menu.gridStart() + cell);
        slotClicked(slot, slot.index, button, ContainerInput.PICKUP);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingThumb) {
            draggingThumb = false;
            return true;
        }
        if (!swallowRelease) {
            return super.mouseReleased(event);
        }
        swallowRelease = false;
        setDragging(false);
        if (getFocused() != null) {
            getFocused().mouseReleased(event);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dialog == Dialog.CRAFT) {
            if (!craftAmount.mouseScrolled(mouseX, mouseY, scrollY)) {
                craftLists.forEach(list -> list.mouseScrolled(mouseX, mouseY, scrollY));
            }
        } else if (!dialogOpen()) {
            cells();
            scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(scrollY)));
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (dialogOpen()) {
                cancel();
                return true;
            }
            return super.keyPressed(event);
        }
        if (dialogOpen()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                craftAmount.commit();
                confirm();
                return true;
            }
            if (maximum.isFocused() && (maximum.keyPressed(event) || maximum.canConsumeInput())) {
                return true;
            }
            (dialog == Dialog.CRAFT ? craftAmount.field() : amount).keyPressed(event);
            return true;
        }
        if (search.isFocused() && (search.keyPressed(event) || search.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(event);
    }

    /** Forgets the shown content only when the menu closed, not while another screen such as JEI's recipe view is in front. */
    @Override
    public void removed() {
        super.removed();
        if (minecraft == null || minecraft.player == null || minecraft.player.containerMenu != menu) {
            ClientStock.close();
        }
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
