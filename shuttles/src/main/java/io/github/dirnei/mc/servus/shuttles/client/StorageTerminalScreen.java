package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.GreyOut;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.client.TerminalCells.Cell;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.pattern.AnyGroup;
import io.github.dirnei.mc.servus.shuttles.site.RemoteSite;
import io.github.dirnei.mc.servus.shuttles.terminal.CancelRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.ItemPopupTabs.Tab;
import io.github.dirnei.mc.servus.shuttles.terminal.LineWarehouse;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftStartPayload;

import io.github.dirnei.mc.servus.shuttles.terminal.OrderSavePayload;
import io.github.dirnei.mc.servus.shuttles.terminal.OrderStartPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.PushCarriedPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestItemPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestRow;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import io.github.dirnei.mc.servus.shuttles.terminal.SelectSitePayload;
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
import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.client.ItemListView;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.NumberStepper;
import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import io.github.dirnei.mc.servus.foundation.client.RecipeGridView;
import io.github.dirnei.mc.servus.foundation.client.Scrollbar;
import io.github.dirnei.mc.servus.foundation.ui.AmountSteps;
import io.github.dirnei.mc.servus.foundation.ui.RecipeGridLayout.Box;
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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Refined Storage style Storage Terminal: the Stock and Rules tabs above the
 * panel, view buttons left of it, a Craft button and search, the item grid or
 * the rule list, a message line, the Input and Output rows with the requests
 * on the Output row, the player inventory; the item popup with its Request,
 * Craft and Rule tabs, and the order grid followed by the amount panel it
 * shares with the Craft tab, each with a server preview. Everything sits where
 * {@link Storage} says; extra grid rows grow the panel upwards.
 */
public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> implements ItemPopup.Host {

    private static final int COLUMNS = Storage.COLUMNS;
    private static final int CELL = TerminalLayout.CELL;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_ALERT = 0xFFB03030;
    /** The alert colour on the dark item grid. */
    private static final int COLOUR_ALERT_ON_GRID = 0xFFFF6060;
    private static final String[] CRAFT_LISTS = {"craft_from_stock", "craft_to_craft", "craft_missing"};
    private static final String[] SETTING_KINDS = {"sort", "direction", "view", "search", "height"};
    private static final String[] TERMINAL_TABS = {"stock", "rules"};
    /** The view buttons that only the Stock tab shows: sort, direction and view. */
    private static final int STOCK_ONLY_BUTTONS = 3;

    /** The item popup, the order grid and the order grid's amount panel. */
    private enum Dialog { NONE, ITEM, ORDER, ORDER_AMOUNT }

    /** Set by the recipe viewer's transfer: open the order grid on the terminal screen now or when it shows next. */
    private static boolean orderRequested;
    /** Whether the last shown terminal screen has its order grid open, also while JEI's recipe view is in front of it. */
    private static boolean orderShown;

    private Storage.Layout layout = Storage.layout(Storage.BASE_ROWS);
    private EditBox search;
    private String searchText = "";
    private String lastSynced = null;
    private final TerminalCells cells = new TerminalCells();
    private int scrollRow;
    private final List<IconButton> sideButtons = new ArrayList<>();
    /** Whether the Rules tab shows in place of the item grid; every opened screen starts on Stock. */
    private boolean rulesTab;
    private final RulesList rulesList = new RulesList();

    private Dialog dialog = Dialog.NONE;
    private final ItemPopup popup = new ItemPopup(this);
    private IconButton craftButton;
    private int askedAmount = -1;
    /** The order grid cells the last order preview was asked for. */
    private List<ItemStack> askedGrid = List.of();
    /** The order grid's result the amount was last set for. */
    private ItemStack lastResult = ItemStack.EMPTY;
    private NumberStepper craftAmount;
    private final List<ItemListView> craftLists = new ArrayList<>();
    private IconButton saveButton;
    private Button backButton;
    private Button startButton;
    private final PatternGridClient grid;
    private final RecipeGridView orderView;
    /** Set when a dialog took a press, so its release puts no carried stack into a slot behind it. */
    private boolean swallowRelease;
    /** The scrollbar of the shown list, the Stock grid or the Rules tab's rule rows; its offset follows {@link #scrolled()}. */
    private final Scrollbar scrollbar = new Scrollbar();
    /** The reason Start is greyed out, null while it can be pressed. */
    private String startReason;

    public StorageTerminalScreen(StorageTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Storage.WIDTH, Storage.BASE_HEIGHT);
        grid = new PatternGridClient(menu::gridCells, menu::mode, () -> press(RecipeEditorButtons.MODE));
        orderView = new RecipeGridView(Storage.ORDER_POPUP.x(), Storage.ORDER_POPUP.y(), null, grid, List.of(
                RecipeGridView.Action.cancel(this::cancel),
                RecipeGridView.Action.clear(() -> press(RecipeEditorButtons.CLEAR)),
                RecipeGridView.Action.needsResult(Component.translatable("gui.servus_shuttles.terminal.order_next"), grid, () -> true,
                        this::openOrderAmount)));
    }

    private void press(int button) {
        MenuButtons.press(minecraft, menu, button);
    }

    private TerminalSettings settings() {
        return ClientTerminalSettings.get();
    }

    private void update(TerminalSettings changed) {
        ClientTerminalSettings.set(changed);
        refreshSideButtons();
    }

    /** The tab strip, the side column and the rows above the base top, for JEI to keep free. */
    public List<Rect2i> extraAreas() {
        List<Rect2i> areas = new ArrayList<>();
        int top = topPos + layout.top();
        areas.add(new Rect2i(leftPos + Storage.SIDE_X, top, -Storage.SIDE_X, layout.height()));
        Rect craft = layout.craft();
        areas.add(new Rect2i(leftPos + craft.x(), topPos + craft.y(), craft.w(), craft.h()));
        Rect tabs = layout.tabs();
        areas.add(new Rect2i(leftPos + tabs.x(), topPos + tabs.y(), tabs.w(), tabs.h()));
        if (layout.top() < 0) {
            areas.add(new Rect2i(leftPos, top, imageWidth, -layout.top()));
        }
        return areas;
    }

    /**
     * Centres the panel with its tab strip as a whole, the base top below the
     * extra rows, and registers the terminal on every show, also after another
     * screen was in front.
     */
    @Override
    protected void init() {
        layout = Storage.layout(Storage.rowsFor(settings().height(), height));
        super.init();
        topPos = (height - layout.heightWithTabs()) / 2 + Storage.TAB_STRIP - layout.top();
        ClientStock.open(menu.terminal().getBlockPos());
        titleLabelX = layout.title().x();
        titleLabelY = layout.title().y();
        inventoryLabelX = Storage.INVENTORY_LABEL.x();
        inventoryLabelY = Storage.INVENTORY_LABEL.y();
        initSearch();
        initSideButtons();
        initCraftButton();
        popup.build(font, leftPos, topPos).forEach(this::addRenderableWidget);
        orderView.build(leftPos, topPos).forEach(this::addRenderableWidget);
        initCraftPanel();
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

    /** The order grid's Save beside its result, and the amount panel's Back or Cancel, stepper, lists and Start. */
    private void initCraftPanel() {
        Box save = orderView.extra();
        saveButton = addRenderableWidget(new IconButton(save.x(), save.y(), save.w(), save.h(), () -> "save_pattern", b -> savePattern()));
        saveButton.setTooltip(Tooltip.create(Component.translatable("gui.servus_shuttles.terminal.order_save")));
        Rect row = Storage.CRAFT_BUTTONS;
        backButton = addRenderableWidget(Button.builder(Component.empty(), b -> back())
                .bounds(leftPos + row.x(), topPos + row.y(), Storage.BUTTON, row.h()).build());
        startButton = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.terminal.craft_start"),
                b -> start()).bounds(leftPos + row.right() - Storage.WIDE_BUTTON, topPos + row.y(), Storage.WIDE_BUTTON, row.h()).build());
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

    /**
     * The recipe viewer's transfer: show the order grid now, or when the
     * terminal screen shows next.
     *
     * @return whether the order grid was open already, so the transfer keeps its mode
     */
    public static boolean requestOrderGrid() {
        boolean wasOpen = orderShown;
        if (Minecraft.getInstance().screen instanceof StorageTerminalScreen screen) {
            if (!wasOpen) {
                screen.openOrder();
            }
        } else {
            orderRequested = true;
        }
        return wasOpen;
    }

    /** Opens the order grid over the item grid while a CPU and a Crafting Station are on the line. */
    public void openOrder() {
        if (!ClientStock.orderGrid()) {
            return;
        }
        dialog = Dialog.ORDER;
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
        dialog = Dialog.ORDER_AMOUNT;
        showAmountPanel();
    }

    /** Back to the order grid from its amount panel, or Cancel. */
    private void back() {
        if (dialog == Dialog.ORDER_AMOUNT) {
            dialog = Dialog.ORDER;
            showOrderGrid();
        } else {
            cancel();
        }
    }

    /** Cancel or Escape: closes the open dialog; the order grid's cells are cleared and its mode asks for Any again. */
    private void cancel() {
        if (orderFlow()) {
            press(RecipeEditorButtons.CANCEL);
        }
        closeDialog();
    }

    /** Whether the order grid or its amount panel is open. */
    private boolean orderFlow() {
        return dialog == Dialog.ORDER || dialog == Dialog.ORDER_AMOUNT;
    }

    /** Whether the amount panel shows: the order grid's second step or the item popup's Craft tab. */
    private boolean amountPanel() {
        return dialog == Dialog.ORDER_AMOUNT || dialog == Dialog.ITEM && popup.tab() == Tab.CRAFT;
    }

    private Rect popupRect() {
        return dialog == Dialog.ORDER ? Storage.ORDER_POPUP : Storage.CRAFT_POPUP;
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

    /** The item the amount panel makes: the order grid's result or the item popup's item. */
    private ItemStack craftItem() {
        return dialog == Dialog.ORDER_AMOUNT ? lastResult : popup.item();
    }

    /** Saves the order grid as a pattern without ordering, then asks again whether it is saved. */
    private void savePattern() {
        ClientPacketDistributor.sendToServer(OrderSavePayload.INSTANCE);
        askedGrid = List.of();
        askPreview();
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
        rulesList.setScroll(0, text, layout.rows());
        searchText = text;
        if (searchMode() != TerminalSettings.SearchMode.NORMAL && !text.equals(lastSynced)) {
            lastSynced = text;
            JeiSearchBridge.write(text);
        }
    }

    private String query() {
        return search == null ? "" : search.getValue();
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
        if (orderFlow()) {
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
        popup.tick();
        if (craftButton != null) {
            craftButton.visible = !dialogOpen() && ClientStock.orderGrid();
        }
    }

    /** Follows the result of the order grid's cells. */
    private void followResult() {
        ItemStack result = grid.ownResult();
        if (dialog == Dialog.ORDER && !ItemStack.matches(result, lastResult)) {
            lastResult = result.copy();
            applyDialog();
        }
    }

    private boolean dialogOpen() {
        return dialog != Dialog.NONE;
    }

    /** How many of the item the warehouse can ship, as the stock sync says. */
    private static long stockCount(ItemStack stack) {
        for (StockEntry entry : ClientStock.entries()) {
            if (ItemStack.isSameItemSameComponents(entry.stack(), stack)) {
                return entry.count();
            }
        }
        return 0;
    }

    /** Opens the item popup for one of the stack on the tab. */
    private void openItem(ItemStack stack, Tab tab) {
        dialog = Dialog.ITEM;
        popup.open(stack, stockCount(stack), ClientStock.craftable(stack), ClientStock.online(), tab);
        applyDialog();
    }

    /** Opens the Rule tab for the item, as the Rules tab's add slot does; for JEI's ghost drop too. */
    public void openRuleFor(ItemStack stack) {
        if (!stack.isEmpty()) {
            openItem(stack, Tab.RULE);
        }
    }

    /** The add slot on the screen while the Rules tab shows and nothing covers it, for JEI's ghost drop. */
    public Optional<Rect2i> ruleAddTarget() {
        if (!rulesTab || dialogOpen() || !ClientStock.online()) {
            return Optional.empty();
        }
        Rect add = RulesList.addSlot(layout.grid());
        return Optional.of(new Rect2i(leftPos + add.x(), topPos + add.y(), add.w(), add.h()));
    }

    @Override
    public void tabShown(Tab tab) {
        if (tab == Tab.CRAFT) {
            showAmountPanel();
        } else {
            applyDialog();
        }
    }

    @Override
    public void closePopup() {
        closeDialog();
    }

    @Override
    public void focus(EditBox field) {
        setFocused(field);
        field.setFocused(true);
        search.setFocused(false);
    }

    /** Shows the widgets of the open dialog, or of the grid when none is open. */
    private void applyDialog() {
        boolean open = dialogOpen();
        boolean order = dialog == Dialog.ORDER;
        boolean panel = amountPanel();
        menu.setOrderOpen(order);
        orderView.setVisible(order);
        orderShown = order;
        craftButton.visible = !open && ClientStock.orderGrid();
        search.visible = !open;
        for (int i = 0; i < sideButtons.size(); i++) {
            sideButtons.get(i).visible = !open && (!rulesTab || i >= STOCK_ONLY_BUTTONS);
        }
        popup.setVisible(dialog == Dialog.ITEM);
        craftAmount.setVisible(panel);
        saveButton.visible = false;
        backButton.visible = panel;
        backButton.setMessage(Component.translatable(dialog == Dialog.ORDER_AMOUNT ? "gui.servus_shuttles.terminal.back"
                : "gui.servus_shuttles.terminal.cancel"));
        startButton.visible = panel;
    }

    /** The widgets of the open dialog that take clicks and are drawn again over its panel. */
    private List<AbstractWidget> dialogWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>(List.of(saveButton, backButton, startButton));
        widgets.addAll(orderView.widgets());
        widgets.addAll(craftAmount.widgets());
        if (dialog == Dialog.ITEM) {
            widgets.addAll(popup.pageWidgets());
        }
        return widgets;
    }

    private void closeDialog() {
        dialog = Dialog.NONE;
        craftAmount.field().setFocused(false);
        ClientStock.clearPreview();
        applyDialog();
    }

    /**
     * Asks the server for a preview of the amount panel's amount or of the
     * order grid. Answers arrive as {@link CraftPreviewPayload}; the item
     * popup asks for its Request tab itself.
     */
    private void askPreview() {
        if (dialog == Dialog.ORDER) {
            askOrderPreview(previewAmount());
        } else if (dialog == Dialog.ORDER_AMOUNT) {
            askOrderPreview(craftAmount.value());
        } else if (amountPanel()) {
            int wanted = craftAmount.value();
            ItemStack item = popup.item();
            if (!item.isEmpty() && wanted != askedAmount) {
                askedAmount = wanted;
                ClientPacketDistributor.sendToServer(new CraftPreviewRequestPayload(item, wanted, CraftPreviewRequestPayload.Mode.CRAFT));
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

    /** The amount the open dialog's preview is for: the order grid's one craft or the amount panel's amount. */
    private int previewAmount() {
        return dialog == Dialog.ORDER ? (lastResult.isEmpty() ? 1 : lastResult.getCount()) : craftAmount.value();
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

    /** The preview for exactly what the order grid or the amount panel shows, or null while it is on its way. */
    private CraftPreviewPayload currentPreview() {
        CraftPreviewPayload preview = ClientStock.preview();
        boolean order = orderFlow();
        ItemStack item = order ? lastResult : popup.item();
        if (preview == null || preview.amount() != previewAmount() || !ItemStack.isSameItemSameComponents(preview.item(), item)
                || order && !sameCells(menu.gridCells(), askedGrid)) {
            return null;
        }
        return preview;
    }

    /** Enter: Next on the order grid, Start on the amount panel, else the popup's Request or Save. */
    private void confirm() {
        if (dialog == Dialog.ORDER) {
            openOrderAmount();
        } else if (amountPanel()) {
            craftAmount.commit();
            start();
        } else if (dialog == Dialog.ITEM) {
            popup.confirm();
        }
    }

    /** Start in the amount panel: orders the order grid's recipe once, or crafts the popup's item. */
    private void start() {
        CraftPreviewPayload preview = currentPreview();
        if (preview == null || !preview.startable()) {
            return;
        }
        int wanted = craftAmount.value();
        if (dialog == Dialog.ORDER_AMOUNT) {
            ClientPacketDistributor.sendToServer(new OrderStartPayload(wanted));
        } else {
            ClientPacketDistributor.sendToServer(new CraftStartPayload(popup.item(), wanted));
        }
        closeDialog();
    }

    /** The grid's cells; clamps the scroll when they were rebuilt. */
    private List<Cell> cells() {
        String query = query().toLowerCase(Locale.ROOT).trim();
        if (cells.refresh(query, settings())) {
            scrollRow = Math.min(scrollRow, maxScrollRow());
        }
        return cells.list();
    }

    private int maxScrollRow() {
        return Math.max(0, (cells.list().size() + COLUMNS - 1) / COLUMNS - layout.rows());
    }

    /** How far the shown list scrolls: the grid's rows or the Rules tab's rule rows. */
    private int maxScroll() {
        return rulesTab ? rulesList.maxScroll(query(), layout.rows()) : maxScrollRow();
    }

    private int scrolled() {
        return rulesTab ? rulesList.scroll() : scrollRow;
    }

    private void scrollTo(int row) {
        if (rulesTab) {
            rulesList.setScroll(row, query(), layout.rows());
        } else {
            cells();
            scrollRow = Math.max(0, Math.min(maxScrollRow(), row));
        }
    }

    private int cellAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        Rect grid = layout.grid();
        if (!grid.contains(x, y) || dialogOpen() || rulesTab) {
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

    private Optional<RuleRow> ruleUnder(double mouseX, double mouseY) {
        if (!rulesTab || dialogOpen()) {
            return Optional.empty();
        }
        return rulesList.ruleAt(layout.grid(), query(), (int) mouseX - leftPos, (int) mouseY - topPos);
    }

    private boolean overAddSlot(double mouseX, double mouseY) {
        return rulesTab && !dialogOpen() && RulesList.overAdd(layout.grid(), (int) mouseX - leftPos, (int) mouseY - topPos);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos + layout.top(), imageWidth, layout.height());
        renderTerminalTabs(graphics);
        if (rulesTab) {
            PanelPainter.inset(graphics, leftPos, topPos, layout.grid());
        } else {
            PanelPainter.slots(graphics, leftPos, topPos, layout.grid(), COLUMNS, COLUMNS * layout.rows());
        }
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
        syncScrollbar().render(graphics, mouseX, mouseY);
    }

    private List<Rect> terminalTabRects() {
        int[] widths = new int[TERMINAL_TABS.length];
        for (int i = 0; i < TERMINAL_TABS.length; i++) {
            widths[i] = font.width(terminalTabLabel(i));
        }
        return TerminalLayout.compactTabs(layout.tabs(), widths, Storage.TAB_PADDING);
    }

    private static Component terminalTabLabel(int tab) {
        return Component.translatable("gui.servus_shuttles.terminal.tab." + TERMINAL_TABS[tab]);
    }

    /** The Stock and Rules tabs on the panel's top edge, the shown one joined to the panel. */
    private void renderTerminalTabs(GuiGraphicsExtractor graphics) {
        List<Rect> rects = terminalTabRects();
        for (int i = 0; i < rects.size(); i++) {
            Rect rect = rects.get(i);
            boolean active = (i == 1) == rulesTab;
            PanelPainter.tab(graphics, leftPos + rect.x(), topPos + rect.y(), rect.w(), rect.h(), active, 3);
            Component label = terminalTabLabel(i);
            graphics.text(font, label, leftPos + rect.x() + (rect.w() - font.width(label)) / 2, topPos + rect.y() + (active ? 4 : 5),
                    COLOUR_TEXT, false);
        }
    }

    /** The terminal tab under the mouse, -1 elsewhere. */
    private int terminalTabAt(double mouseX, double mouseY) {
        List<Rect> rects = terminalTabRects();
        for (int i = 0; i < rects.size(); i++) {
            if (rects.get(i).contains((int) mouseX - leftPos, (int) mouseY - topPos)) {
                return i;
            }
        }
        return -1;
    }

    private void showRulesTab(boolean rules) {
        if (rules == rulesTab) {
            return;
        }
        rulesTab = rules;
        scrollRow = 0;
        rulesList.setScroll(0, query(), layout.rows());
        applyDialog();
    }

    /** The scrollbar set to the shown list's rows and scroll. */
    private Scrollbar syncScrollbar() {
        Rect bar = layout.scrollbar();
        scrollbar.setArea(leftPos + bar.x(), topPos + bar.y(), bar.w(), bar.h());
        scrollbar.setContent(maxScroll() + layout.rows(), layout.rows());
        scrollbar.setOffset(scrolled());
        return scrollbar;
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


    /** The grid, the rule list or the open dialog, drawn before the carried item so a dragged stack stays on top. */
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
        Component tag = tagTooltip(mouseX - leftPos, mouseY - topPos);
        if (rulesTab) {
            rulesList.render(graphics, font, leftPos, topPos, layout.grid(), layout.rows(), query(), mouseX, mouseY);
            Optional<RuleRow> rule = ruleUnder(mouseX, mouseY);
            if (rule.isPresent()) {
                graphics.setTooltipForNextFrame(font, RulesList.tooltip(rule.get()), Optional.empty(), mouseX, mouseY);
                return;
            }
            Rect add = RulesList.addSlot(layout.grid());
            if (add.contains(mouseX - leftPos, mouseY - topPos)) {
                graphics.setTooltipForNextFrame(font, font.split(RulesList.addTooltip(), 200), mouseX, mouseY);
                return;
            }
        } else {
            renderGrid(graphics, mouseX, mouseY);
        }
        Optional<Cell> hovered = cellUnder(mouseX, mouseY);
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
                lines.addAll(RulesList.ruleLines(rule));
            }
            lines.add(Component.translatable("gui.servus_shuttles.terminal.click_hint"));
            if (cell.craftable()) {
                lines.add(Component.translatable("gui.servus_shuttles.terminal.craftable_hint"));
            }
        } else {
            lines.add(Component.translatable("gui.servus_shuttles.terminal.craft_hint"));
        }
        AnyGroup follows = ClientStock.follows(cell.stack());
        if (follows != null) {
            lines.add(PatternGridClient.followsLine(follows));
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
            PanelPainter.smallText(graphics, font, Component.translatable("gui.servus_shuttles.terminal.craft_cell"), x + 16, y + 11, 0xFFFFE080);
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
        PanelPainter.smallText(graphics, font, Component.literal(row.delivered() + "/" + row.requested()), x + 16, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 4, y + 4, 0xFF202020);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, stateColour(row.state()));
        int filled = row.requested() <= 0 ? 0 : 16 * Math.min(row.delivered(), row.requested()) / row.requested();
        graphics.fill(x, y + 16, x + 16, y + 17, 0xFF202020);
        graphics.fill(x, y + 16, x + filled, y + 17, stateColour(row.state()));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (dialogOpen() && hoveredSlot != null && hoveredSlot.y < Storage.INVENTORY_LABEL.y() && !menu.isGridSlot(hoveredSlot.index)) {
            return;
        }
        RequestRow row = menu.getCarried().isEmpty() ? requestAt(hoveredSlot) : null;
        if (row != null) {
            graphics.setComponentTooltipForNextFrame(font, requestTooltip(row), mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
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

    /** The popup over the dimmed terminal; its widgets were drawn below it by super and are drawn again on top. */
    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int dimX = leftPos + Storage.SIDE_X;
        int dimTop = topPos + layout.top();
        int dimWidth = imageWidth - Storage.SIDE_X;
        int dimHeight = topPos + Storage.INVENTORY_LABEL.y() - 1 - dimTop;
        if (dialog == Dialog.ORDER) {
            orderView.render(graphics, font, dimX, dimTop, dimWidth, dimHeight);
            renderOrder(graphics, mouseX, mouseY);
            return;
        }
        Rect panel = popupRect();
        PopupPanel.draw(graphics, dimX, dimTop, dimWidth, dimHeight, leftPos + panel.x(), topPos + panel.y(), panel.w(), panel.h());
        if (dialog == Dialog.ITEM) {
            popup.renderTabs(graphics, font, mouseX, mouseY);
        }
        if (amountPanel()) {
            renderCraftPanel(graphics, mouseX, mouseY);
        } else {
            popup.renderPage(graphics, font, mouseX, mouseY);
        }
    }

    /**
     * Over the order grid's view: the ghost cells drawn again, Save while no
     * pattern has the recipe with this mode, the view's buttons and tooltips.
     */
    private void renderOrder(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        orderView.renderCells(graphics, menu.gridCells(), mouseX, mouseY);
        CraftPreviewPayload preview = currentPreview();
        saveButton.visible = !lastResult.isEmpty() && preview != null && preview.refusal().isEmpty() && !preview.patternSaved();
        renderDialogWidgets(graphics, mouseX, mouseY);
        orderView.renderTooltips(graphics, font, mouseX, mouseY);
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
        startReason = GreyOut.apply(startButton, startReason, startRefusal(preview));
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
            ItemStack made = madeVariant(preview, item);
            renderItem(graphics, made.copyWithCount(craftedCount(preview, made)), at.x(), at.y(), mouseX, mouseY);
        }
    }

    /** The stack the order makes: the preview's To craft entry of the item, which names a material variant, else the item. */
    private static ItemStack madeVariant(CraftPreviewPayload preview, ItemStack item) {
        if (preview != null) {
            for (StockEntry entry : preview.toCraft()) {
                if (ItemStack.isSameItem(entry.stack(), item)) {
                    return entry.stack();
                }
            }
        }
        return item;
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

    /** The tab strip, the rows above the base top, the side column and an open popup belong to the screen too. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        int x = (int) mouseX - left;
        int y = (int) mouseY - top;
        boolean panel = x >= 0 && x < imageWidth && y >= layout.top() && y < Storage.BASE_HEIGHT;
        boolean side = x >= Storage.SIDE_X && x < 0 && y >= layout.top() && y < Storage.BASE_HEIGHT
                || layout.craft().contains(x, y);
        boolean tabs = layout.tabs().contains(x, y);
        boolean open = dialogOpen() && (popupRect().contains(x, y) || dialog == Dialog.ITEM && Storage.POPUP_TABS.contains(x, y));
        return !panel && !side && !tabs && !open;
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
        if (dialogOpen()) {
            return clickDialog(event, doubleClick);
        }
        int terminalTab = terminalTabAt(mouseX, mouseY);
        if (terminalTab >= 0) {
            showRulesTab(terminalTab == 1);
            return true;
        }
        if (siteSelector() && overTitle(mouseX, mouseY) && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
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
        if (syncScrollbar().mouseClicked(mouseX, mouseY, button)) {
            scrollTo(scrollbar.offset());
            return true;
        }
        if (rulesTab && ClientStock.online() && layout.grid().contains((int) mouseX - leftPos, (int) mouseY - topPos)) {
            clickRules(mouseX, mouseY);
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

    /** A press while a dialog is open: its tabs and widgets, the order grid's ghost cells; the dimmed rest takes the press. */
    private boolean clickDialog(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (dialog == Dialog.ITEM && popup.clickTab(font, mouseX, mouseY)) {
            swallowRelease = true;
            return true;
        }
        if (amountPanel() && craftLists.stream().anyMatch(list -> list.mouseClicked(mouseX, mouseY, event.button()))) {
            swallowRelease = true;
            return true;
        }
        for (AbstractWidget widget : dialogWidgets()) {
            if (widget.visible && widget.mouseClicked(event, doubleClick)) {
                setFocused(widget);
                swallowRelease = true;
                return true;
            }
        }
        if (orderView.press(mouseX, mouseY, !menu.getCarried().isEmpty(), cell -> clickGhostCell(cell, event.button()))) {
            swallowRelease = true;
            return true;
        }
        if (orderView.cellAt(mouseX, mouseY) >= 0) {
            return super.mouseClicked(event, doubleClick);
        }
        if (overDimmed(mouseX, mouseY)) {
            swallowRelease = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** A click on the Rules tab: the add slot with a carried stack, or a rule row; both open the Rule tab. */
    private void clickRules(double mouseX, double mouseY) {
        swallowRelease = true;
        if (overAddSlot(mouseX, mouseY)) {
            openRuleFor(menu.getCarried());
            return;
        }
        ruleUnder(mouseX, mouseY).ifPresent(rule -> openItem(rule.stack(), Tab.RULE));
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
     * smaller), the item popup opens on Request on a right click, and on Craft
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
            openItem(found.stack(), Tab.CRAFT);
        } else if (Minecraft.getInstance().hasShiftDown()) {
            int count = (int) Math.min(found.stack().getMaxStackSize(), found.count());
            ClientPacketDistributor.sendToServer(new RequestItemPayload(found.stack(), count));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            openItem(found.stack(), Tab.REQUEST);
        } else {
            ClientPacketDistributor.sendToServer(new RequestItemPayload(found.stack(), 1));
        }
        return true;
    }


    /** Whether the mouse is over the popup, its tabs or the dimmed part of the terminal above the inventory. */
    private boolean overDimmed(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        return popupRect().contains(x, y) || Storage.POPUP_TABS.contains(x, y) || layout.tabs().contains(x, y)
                || x >= Storage.SIDE_X && x < imageWidth && y >= layout.top() && y < Storage.INVENTORY_LABEL.y();
    }

    /** Dragging the cursor's item over the order grid sets a ghost copy in each cell it passes. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (scrollbar.mouseDragged(event.y())) {
            scrollTo(scrollbar.offset());
            return true;
        }
        if (amountPanel() && craftLists.stream().anyMatch(list -> list.mouseDragged(event.y()))) {
            return true;
        }
        if (orderView.drag(event.x(), event.y(), !menu.getCarried().isEmpty(), cell -> clickGhostCell(cell, event.button()))) {
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
        boolean barDragEnded = scrollbar.mouseReleased() | releaseCraftLists();
        if (barDragEnded) {
            swallowRelease = false;
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

    /** Ends a scrollbar drag in the amount panel's lists; true when one was held. */
    private boolean releaseCraftLists() {
        boolean ended = false;
        for (ItemListView list : craftLists) {
            ended |= list.mouseReleased();
        }
        return ended;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (amountPanel()) {
            if (!craftAmount.mouseScrolled(mouseX, mouseY, scrollY)) {
                craftLists.forEach(list -> list.mouseScrolled(mouseX, mouseY, scrollY));
            }
        } else if (dialog == Dialog.ITEM) {
            popup.mouseScrolled(mouseX, mouseY, scrollY);
        } else if (!dialogOpen()) {
            scrollTo(scrolled() - (int) Math.signum(scrollY));
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
            if (dialog == Dialog.ITEM && !amountPanel()) {
                return popup.keyPressed(event);
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirm();
                return true;
            }
            craftAmount.field().keyPressed(event);
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
            orderShown = false;
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
