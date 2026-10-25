package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.shuttles.field.ChooseAntiCorrosionPayload;
import io.github.dirnei.mc.servus.shuttles.field.ChooseFertilizerPayload;
import io.github.dirnei.mc.servus.shuttles.field.ChooseFieldCropPayload;
import io.github.dirnei.mc.servus.shuttles.field.FertilizeMode;
import io.github.dirnei.mc.servus.shuttles.field.FertilizeStatus;
import io.github.dirnei.mc.servus.shuttles.field.FieldAmount;
import io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion;
import io.github.dirnei.mc.servus.shuttles.field.FieldDuration;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldStatus;
import io.github.dirnei.mc.servus.shuttles.field.FieldThreshold;
import io.github.dirnei.mc.servus.shuttles.field.FieldWork;
import io.github.dirnei.mc.servus.shuttles.field.SetFieldAmountPayload;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLocksPayload;
import io.github.dirnei.mc.servus.shuttles.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.shuttles.menu.FieldHeadMenu.Page;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The Field Head GUI in five tabs on the left edge, drawn like the Utility
 * Duct tabs: Field (crop picker, ripe share, threshold, work status), Water
 * (water bar, hydration, corrosion stage), Fertilizer (fertilizer picker,
 * mode, threshold, Spreader status), Stock (a table of the produce, seeds,
 * fertilizer and anti-corrosion item: warehouse stock, the head's buffer and
 * the typed reserves and levels) and Upgrades (Range Upgrade
 * slot, radius, field size). The player inventory is always below. A tab whose
 * page holds a problem gets a red marker.
 */
public class FieldHeadScreen extends AbstractContainerScreen<FieldHeadMenu> {

    private static final int MARGIN = FieldHeadMenu.PICKER_X;
    private static final int WIDTH = FieldHeadMenu.PICKER_WIDTH;
    private static final int RIGHT = MARGIN + WIDTH;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_HINT = 0xFF707070;
    private static final int COLOUR_DUE = 0xFF2E7D32;
    private static final int COLOUR_WARN = 0xFFA04000;
    private static final int COLOUR_WATER = 0xFF3F76E4;
    private static final int COLOUR_DRY = 0xFF9A8F80;
    private static final int COLOUR_RIPE = 0xFFD8B33A;
    private static final int COLOUR_MARK = 0xFF202020;
    private static final int COLOUR_PROBLEM = 0xFFD02020;
    private static final int COLOUR_TAB = 0xFF9A9A9A;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_TOP = 10;
    private static final int TAB_STRIDE = 28;
    /** The - and + buttons of a percent threshold, at the right end of its row. */
    private static final int STEP_W = 14;
    private static final int STEP_DOWN_X = RIGHT - 2 * STEP_W - 2;
    private static final int STEP_UP_X = RIGHT - STEP_W;
    private static final int RIPE_Y = 44;
    private static final int RIPE_X = 90;
    private static final int RIPE_W = RIGHT - RIPE_X;
    private static final int RIPE_H = 7;
    private static final int THRESHOLD_Y = 60;
    private static final int WORK_Y = 80;
    private static final int WATER_Y = 44;
    private static final int BAR_Y = 56;
    private static final int BAR_H = 8;
    private static final int WATER_RATE_Y = 70;
    private static final int CORROSION_Y = 84;
    /** Rows above and below the ripe bar that still take a slider click. */
    private static final int SLIDER_GRACE = 3;
    private static final int RADIUS_TEXT_X = 30;
    private static final int SIZE_Y = 44;
    private static final int MODE_Y = 44;
    private static final int MODE_H = 14;
    private static final int[] MODE_X = {MARGIN, 60, 162};
    private static final int[] MODE_W = {50, 100, RIGHT - 162};
    private static final int FERTILIZE_Y = 64;
    private static final int FERTILIZE_STATUS_Y = 82;
    /** The Stock tab: a table with one column per {@link StockColumn} right of the row labels. */
    private static final int TABLE_X = 84;
    private static final int COLUMN_W = (RIGHT - TABLE_X) / 4;
    private static final int HEADER_Y = 18;
    private static final int WAREHOUSE_Y = 40;
    private static final int HEAD_Y = 54;
    private static final int RULE_Y = 67;
    private static final int KEEP_Y = 72;
    private static final int HARVEST_LIMIT_Y = 89;
    private static final int FERTILIZE_BELOW_Y = 106;
    private static final int AMOUNT_BOX_W = 38;
    private static final int AMOUNT_BOX_H = 14;
    private static final int ROW_H = 12;
    private static final int COLOUR_RULE = 0xFF8B8B8B;
    private static final int ROWS = 3;

    private final ItemPicker cropPicker;
    private final ItemPicker fertilizerPicker;
    private final ItemPicker antiCorrosionPicker;
    /** The stock maps last handed to the pickers, to hand them over only when the menu got new ones. */
    private Map<Identifier, Long> cropPickerStock;
    private Map<Identifier, Long> supplyPickerStock;
    private final List<Button> modeButtons = new ArrayList<>();
    /** The typed amounts of the Stock tab, in {@link FieldAmount} order. */
    private final List<AmountField> amountFields = new ArrayList<>();
    private Button harvestDown;
    private Button harvestUp;
    private Button fertilizeDown;
    private Button fertilizeUp;
    /** The harvest percent the ripe bar slider shows while it is dragged; 0 while it is not. */
    private int sliderPercent;

    public FieldHeadScreen(FieldHeadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FieldHeadMenu.IMAGE_WIDTH, FieldHeadMenu.IMAGE_HEIGHT);
        this.cropPicker = picker("crop_picker", "crop_no_stock", CropPickerEntries.build(),
                entry -> ClientPacketDistributor.sendToServer(new ChooseFieldCropPayload(entry.itemId())));
        this.fertilizerPicker = picker("fertilizer_picker", "fertilizer_no_stock",
                TagPickerEntries.build(FieldHeadBlockEntity.FERTILIZER, Component.translatable("gui.servus_shuttles.field_head.fertilizer_none")),
                entry -> ClientPacketDistributor.sendToServer(new ChooseFertilizerPayload(entry.itemId())));
        this.antiCorrosionPicker = picker("anti_corrosion_picker", "anti_corrosion_no_stock",
                TagPickerEntries.build(FieldHeadBlockEntity.ANTI_CORROSION, Component.translatable("gui.servus_shuttles.field_head.anti_corrosion_none")),
                entry -> ClientPacketDistributor.sendToServer(new ChooseAntiCorrosionPayload(entry.itemId())));
    }

    private static ItemPicker picker(String title, String zeroHint, List<ItemPicker.Entry> entries,
                                     Consumer<ItemPicker.Entry> onPick) {
        ItemPicker picker = new ItemPicker(Component.translatable("gui.servus_shuttles.field_head." + title), onPick);
        picker.setEntries(entries);
        picker.setZeroHint(Component.translatable("gui.servus_shuttles.field_head." + zeroHint));
        return picker;
    }

    /** The picker of the shown page, null on pages without one. */
    private ItemPicker pagePicker() {
        return switch (menu.page()) {
            case FIELD -> cropPicker;
            case WATER -> antiCorrosionPicker;
            case FERTILIZER -> fertilizerPicker;
            default -> null;
        };
    }

    /** The picker whose popup is open, or null. */
    private ItemPicker openPicker() {
        if (cropPicker.isOpen()) {
            return cropPicker;
        }
        if (fertilizerPicker.isOpen()) {
            return fertilizerPicker;
        }
        return antiCorrosionPicker.isOpen() ? antiCorrosionPicker : null;
    }

    /** Hands the head's crop and fertilizer and the menu's stock to the pickers. */
    private void syncPickers() {
        cropPicker.setSelected(menu.harvestOnly() ? PickerList.NONE_ID : ItemPicker.Entry.idOf(menu.crop()));
        if (menu.stock() != cropPickerStock) {
            cropPickerStock = menu.stock();
            cropPicker.setStock(CropPickerEntries.keyed(cropPickerStock));
        }
        fertilizerPicker.setSelected(ItemPicker.Entry.idOf(menu.fertilizer()));
        antiCorrosionPicker.setSelected(ItemPicker.Entry.idOf(menu.antiCorrosion()));
        if (menu.supplyStock() != supplyPickerStock) {
            supplyPickerStock = menu.supplyStock();
            fertilizerPicker.setStock(CropPickerEntries.keyed(supplyPickerStock));
            antiCorrosionPicker.setStock(CropPickerEntries.keyed(supplyPickerStock));
        }
    }

    /** Keeps the tabs and the top of the panel on screen when the window is too small to centre it. */
    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(TAB_WIDTH, leftPos);
        topPos = Math.max(0, topPos);
        this.inventoryLabelX = FieldHeadMenu.INVENTORY_X;
        this.inventoryLabelY = FieldHeadMenu.INVENTORY_Y - 11;
        cropPicker.setButton(leftPos + FieldHeadMenu.PICKER_X, topPos + FieldHeadMenu.PICKER_Y, FieldHeadMenu.PICKER_WIDTH);
        cropPicker.setArea(leftPos, topPos, imageWidth, imageHeight);
        harvestDown = stepButton("-", THRESHOLD_Y - 2, STEP_DOWN_X, FieldHeadMenu.BUTTON_THRESHOLD_DOWN);
        harvestUp = stepButton("+", THRESHOLD_Y - 2, STEP_UP_X, FieldHeadMenu.BUTTON_THRESHOLD_UP);
        fertilizerPicker.setButton(leftPos + FieldHeadMenu.PICKER_X, topPos + FieldHeadMenu.PICKER_Y, FieldHeadMenu.PICKER_WIDTH);
        fertilizerPicker.setArea(leftPos, topPos, imageWidth, imageHeight);
        antiCorrosionPicker.setButton(leftPos + FieldHeadMenu.PICKER_X, topPos + FieldHeadMenu.PICKER_Y, FieldHeadMenu.PICKER_WIDTH);
        antiCorrosionPicker.setArea(leftPos, topPos, imageWidth, imageHeight);
        modeButtons.clear();
        for (FertilizeMode mode : FertilizeMode.values()) {
            String key = "gui.servus_shuttles.field_head.fertilize_mode." + mode.name().toLowerCase(Locale.ROOT);
            int index = mode.ordinal();
            modeButtons.add(addRenderableWidget(Button.builder(Component.translatable(key), button -> press(FieldHeadMenu.BUTTON_FERTILIZE_MODE + index))
                    .bounds(leftPos + MODE_X[index], topPos + MODE_Y, MODE_W[index], MODE_H)
                    .tooltip(Tooltip.create(Component.translatable(key + ".tooltip"))).build()));
        }
        fertilizeDown = stepButton("-", FERTILIZE_Y - 2, STEP_DOWN_X, FieldHeadMenu.BUTTON_FERTILIZE_DOWN);
        fertilizeUp = stepButton("+", FERTILIZE_Y - 2, STEP_UP_X, FieldHeadMenu.BUTTON_FERTILIZE_UP);
        amountFields.clear();
        for (FieldAmount amount : FieldAmount.values()) {
            AmountField field = new AmountField(font, leftPos + amountX(amount), topPos + amountY(amount), AMOUNT_BOX_W, AMOUNT_BOX_H, amount,
                    Component.translatable(amountKey(amount)), () -> headAmount(amount),
                    value -> ClientPacketDistributor.sendToServer(new SetFieldAmountPayload(menu.headPos(), amount.ordinal(), value)));
            amountFields.add(field);
            addRenderableWidget(field.box());
        }
    }

    /** The columns of the Stock tab's table. */
    private enum StockColumn {
        PRODUCE(null), SEEDS(SupplyBuffer.Part.SEEDS), FERTILIZER(SupplyBuffer.Part.FERTILIZER),
        ANTI_CORROSION(SupplyBuffer.Part.ANTI_CORROSION);

        /** The buffer part of the column, null for the produce. */
        private final SupplyBuffer.Part part;

        StockColumn(SupplyBuffer.Part part) {
            this.part = part;
        }

        private String key() {
            return "gui.servus_shuttles.field_head.stock.column." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** The rows of the Stock tab's table with a label on the left, top to bottom. */
    private enum StockRow {
        WAREHOUSE(WAREHOUSE_Y), HEAD(HEAD_Y), KEEP(KEEP_Y + 3), HARVEST_LIMIT(HARVEST_LIMIT_Y + 3), FERTILIZE_BELOW(FERTILIZE_BELOW_Y + 3);

        /** Where the row's label text sits. */
        private final int textY;

        StockRow(int textY) {
            this.textY = textY;
        }

        private String key() {
            return "gui.servus_shuttles.field_head.stock.row." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static int columnX(StockColumn column) {
        return TABLE_X + column.ordinal() * COLUMN_W;
    }

    private static StockColumn amountColumn(FieldAmount amount) {
        return switch (amount) {
            case HARVEST_LIMIT, FERTILIZE_BELOW -> StockColumn.PRODUCE;
            case RESERVE -> StockColumn.FERTILIZER;
            case ANTI_CORROSION_RESERVE -> StockColumn.ANTI_CORROSION;
        };
    }

    private static int amountX(FieldAmount amount) {
        return columnX(amountColumn(amount)) + (COLUMN_W - AMOUNT_BOX_W) / 2;
    }

    private static int amountY(FieldAmount amount) {
        return switch (amount) {
            case HARVEST_LIMIT -> HARVEST_LIMIT_Y;
            case FERTILIZE_BELOW -> FERTILIZE_BELOW_Y;
            case RESERVE, ANTI_CORROSION_RESERVE -> KEEP_Y;
        };
    }

    private static String amountKey(FieldAmount amount) {
        return "gui.servus_shuttles.field_head.amount." + amount.name().toLowerCase(Locale.ROOT);
    }

    /** The value the head keeps for the amount, as the container data last brought it. */
    private int headAmount(FieldAmount amount) {
        return switch (amount) {
            case HARVEST_LIMIT -> menu.harvestLimit();
            case FERTILIZE_BELOW -> menu.fertilizeBelow();
            case RESERVE -> menu.reserve();
            case ANTI_CORROSION_RESERVE -> menu.antiCorrosionReserve();
        };
    }

    /** The typed field that has the keyboard, or null. */
    private AmountField focusedField() {
        for (AmountField field : amountFields) {
            if (field.isFocused()) {
                return field;
            }
        }
        return null;
    }

    /** Takes the keyboard from the typed fields; a field that had it sends its number at the next tick. */
    private void clearFieldFocus() {
        setFocused(null);
        for (AmountField field : amountFields) {
            field.box().setFocused(false);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (AmountField field : amountFields) {
            field.tick();
        }
    }

    @Override
    public void removed() {
        AmountField focused = focusedField();
        if (focused != null) {
            focused.commit();
        }
        super.removed();
    }

    private Button stepButton(String label, int y, int x, int id) {
        return addRenderableWidget(Button.builder(Component.literal(label), button -> press(id))
                .bounds(leftPos + x, topPos + y, STEP_W, 11).build());
    }

    private void press(int id) {
        MenuButtons.press(minecraft, menu, id);
    }

    private boolean serving() {
        return menu.status() == FieldStatus.SERVING;
    }

    /** The problem the page shows, or null when it has none. */
    private Component problem(Page page) {
        switch (page) {
            case FIELD -> {
                if (!serving()) {
                    return statusLine();
                }
                FieldWork work = menu.work();
                return work != null && work.problem() ? workLine(work) : null;
            }
            case WATER -> {
                if (menu.corrosion() == FieldCorrosion.CORRODED) {
                    return Component.translatable("gui.servus_shuttles.field_head.corroded");
                }
                return serving() && !menu.hydrated() ? Component.translatable("gui.servus_shuttles.field_head.dry") : null;
            }
            case FERTILIZER -> {
                FertilizeStatus status = menu.fertilizeStatus();
                return status != null && status.problem() ? fertilizeLine(status) : null;
            }
            default -> {
                return null;
            }
        }
    }

    private Component statusLine() {
        return Component.translatable("gui.servus_shuttles.field_head.status." + menu.status().name().toLowerCase(Locale.ROOT));
    }

    private Component workLine(FieldWork work) {
        String worker = ClientFieldWorker.name();
        if (work == FieldWork.WORKING && !worker.isEmpty()) {
            return Component.translatable("gui.servus_shuttles.field_head.work.working_named", worker);
        }
        if (work == FieldWork.HARVEST_PAUSED) {
            return Component.translatable("gui.servus_shuttles.field_head.work.harvest_paused",
                    grouped(menu.produceStock()), grouped(menu.harvestLimit()), menu.produce().getHoverName());
        }
        return Component.translatable("gui.servus_shuttles.field_head.work." + work.name().toLowerCase(Locale.ROOT));
    }

    private Component fertilizeLine(FertilizeStatus status) {
        String spreader = ClientFieldWorker.spreader();
        if (status == FertilizeStatus.WORKING && !spreader.isEmpty()) {
            return Component.translatable("gui.servus_shuttles.field_head.fertilize.working_named", spreader);
        }
        if (status == FertilizeStatus.NOT_NEEDED) {
            return menu.harvestPaused()
                    ? Component.translatable("gui.servus_shuttles.field_head.fertilize.not_needed_paused")
                    : Component.translatable("gui.servus_shuttles.field_head.fertilize.not_needed",
                            grouped(menu.produceStock()), menu.produce().getHoverName(), grouped(menu.fertilizeBelow()));
        }
        return Component.translatable("gui.servus_shuttles.field_head.fertilize." + status.name().toLowerCase(Locale.ROOT));
    }

    /** A count with thousands separators, "1,024". */
    private static String grouped(long count) {
        return String.format(Locale.ROOT, "%,d", count);
    }

    private int tabX() {
        return leftPos - TAB_WIDTH;
    }

    private int tabY(int index) {
        return topPos + TAB_TOP + index * TAB_STRIDE;
    }

    private Page tabAt(double mouseX, double mouseY) {
        for (Page page : Page.VALUES) {
            int x = tabX();
            int y = tabY(page.ordinal());
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
                return page;
            }
        }
        return null;
    }

    private static ItemStack icon(Page page) {
        return switch (page) {
            case FIELD -> new ItemStack(Items.WHEAT);
            case WATER -> new ItemStack(Items.WATER_BUCKET);
            case FERTILIZER -> new ItemStack(Items.BONE_MEAL);
            case STOCK -> new ItemStack(Items.CHEST);
            case UPGRADES -> new ItemStack(ServusShuttlesItems.RANGE_UPGRADE.get());
        };
    }

    private void renderTabs(GuiGraphicsExtractor graphics) {
        for (Page page : Page.VALUES) {
            boolean selected = page == menu.page();
            boolean problem = problem(page) != null;
            int x = tabX();
            int y = tabY(page.ordinal());
            int width = selected ? TAB_WIDTH + 2 : TAB_WIDTH;
            graphics.fill(x, y, x + width, y + TAB_HEIGHT, problem ? COLOUR_PROBLEM : PanelPainter.DARK);
            graphics.fill(x + 1, y + 1, x + width, y + TAB_HEIGHT - 1, selected ? PanelPainter.FACE : COLOUR_TAB);
            graphics.fill(x + 1, y + 1, x + width, y + 2, 0xFFFFFFFF);
            if (problem) {
                graphics.fill(x + 1, y + 1, x + 3, y + TAB_HEIGHT - 1, COLOUR_PROBLEM);
            }
            graphics.fakeItem(icon(page), x + 6, y + 5);
        }
    }

    /** Shows the widgets of the current page. */
    private void updateWidgets() {
        Page page = menu.page();
        boolean serving = serving();
        harvestDown.visible = page == Page.FIELD && serving;
        harvestUp.visible = page == Page.FIELD && serving;
        for (AmountField field : amountFields) {
            field.box().visible = page == Page.STOCK;
        }
        boolean fertilizerPage = page == Page.FERTILIZER;
        FertilizeMode mode = menu.fertilizeMode();
        for (int i = 0; i < modeButtons.size(); i++) {
            modeButtons.get(i).visible = fertilizerPage;
            modeButtons.get(i).active = mode.ordinal() != i;
        }
        fertilizeDown.visible = fertilizerPage && mode == FertilizeMode.UNTIL_RIPE;
        fertilizeUp.visible = fertilizerPage && mode == FertilizeMode.UNTIL_RIPE;
    }

    /** While a picker is open nothing below it reacts to the mouse. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateWidgets();
        syncPickers();
        ItemPicker open = openPicker();
        if (open != null) {
            super.extractRenderState(graphics, -1, -1, partialTick);
            open.renderPopup(graphics, font, mouseX, mouseY, partialTick);
            open.renderTooltip(graphics, font, mouseX, mouseY);
            return;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        ItemPicker shown = pagePicker();
        if (shown == null || !shown.renderTooltip(graphics, font, mouseX, mouseY)) {
            renderPageTooltip(graphics, mouseX, mouseY);
        }
    }

    private void renderPageTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Page hovered = tabAt(mouseX, mouseY);
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.servus_shuttles.field_head.tab." + hovered.name().toLowerCase(Locale.ROOT)));
            Component problem = problem(hovered);
            if (problem != null) {
                lines.add(problem.copy().withColor(COLOUR_PROBLEM & 0xFFFFFF));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        List<Component> lines = switch (menu.page()) {
            case FIELD -> serving() && onRipeBar(mouseX, mouseY)
                    ? List.of(Component.translatable("gui.servus_shuttles.field_head.ripe_tooltip",
                            menu.ripe(), menu.planted(), percent(menu.ripe(), menu.planted()), shownThreshold()),
                            Component.translatable("gui.servus_shuttles.field_head.ripe_slider"))
                    : List.of();
            case WATER -> inside(mouseX, mouseY, MARGIN, CORROSION_Y - 1, WIDTH, 10) ? corrosionTooltip()
                    : inside(mouseX, mouseY, MARGIN, BAR_Y, WIDTH, BAR_H)
                    ? List.of(Component.translatable("gui.servus_shuttles.field_head.water", menu.water(), menu.capacity()),
                            menu.hydrated() ? Component.translatable("gui.servus_shuttles.field_head.water_rate", menu.waterPerMinute())
                                    : Component.translatable("gui.servus_shuttles.field_head.dry"))
                    : List.of();
            case STOCK -> stockTooltip(mouseX, mouseY);
            case UPGRADES -> emptySlotHovered(FieldHeadMenu.RANGE_SLOT, mouseX, mouseY)
                    ? List.of(Component.translatable("gui.servus_shuttles.field_head.range_slot", menu.radius()))
                    : List.of();
            default -> List.of();
        };
        if (!lines.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    /** What the cell, typed field or row label of the Stock tab under the mouse means; empty elsewhere. */
    private List<Component> stockTooltip(int mouseX, int mouseY) {
        for (FieldAmount amount : FieldAmount.values()) {
            if (inside(mouseX, mouseY, amountX(amount), amountY(amount), AMOUNT_BOX_W, AMOUNT_BOX_H)) {
                return amountTooltip(amount);
            }
        }
        for (StockColumn column : StockColumn.values()) {
            int x = columnX(column);
            if (inside(mouseX, mouseY, x, HEADER_Y, COLUMN_W, 16)) {
                return columnTooltip(column);
            }
            if (inside(mouseX, mouseY, x, WAREHOUSE_Y - 2, COLUMN_W, ROW_H)) {
                return List.of(warehouseTooltip(column));
            }
            if (column.part != null && inside(mouseX, mouseY, x, HEAD_Y - 2, COLUMN_W, ROW_H)) {
                return bufferTooltip(column.part);
            }
        }
        for (StockRow row : StockRow.values()) {
            if (inside(mouseX, mouseY, MARGIN, row.textY - 2, TABLE_X - MARGIN, ROW_H)) {
                return rowTooltip(row);
            }
        }
        if (onToStorageRow(mouseX, mouseY)) {
            return List.of(Component.translatable("gui.servus_shuttles.field_head.to_storage.tooltip"));
        }
        return List.of();
    }

    /** Whether the mouse is on the To storage label or one of its empty slots; a slot with items shows the item instead. */
    private boolean onToStorageRow(int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, MARGIN, FieldHeadMenu.TO_STORAGE_Y, TABLE_X - MARGIN, 16)) {
            return true;
        }
        for (int slot = 0; slot < menu.toStorageShown(); slot++) {
            if (emptySlotHovered(FieldHeadMenu.TO_STORAGE_START + slot, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /** The column's role and its item, or why it has none. */
    private List<Component> columnTooltip(StockColumn column) {
        ItemStack item = columnItem(column);
        Component role = Component.translatable(column.key());
        if (!item.isEmpty()) {
            return List.of(item.getHoverName(), role);
        }
        return List.of(role, Component.translatable(column == StockColumn.PRODUCE
                ? "gui.servus_shuttles.field_head.produce_unknown" : column.key() + ".none"));
    }

    /** How many of the column's item the warehouse holds, or why there is no count. */
    private Component warehouseTooltip(StockColumn column) {
        ItemStack item = columnItem(column);
        if (item.isEmpty()) {
            return Component.translatable(column == StockColumn.PRODUCE
                    ? "gui.servus_shuttles.field_head.produce_unknown" : column.key() + ".none");
        }
        Long count = warehouseCount(column);
        return count == null ? Component.translatable("gui.servus_shuttles.field_head.stock.no_warehouse")
                : Component.translatable("gui.servus_shuttles.field_head.stock.in_warehouse", item.getHoverName(), grouped(count));
    }

    private List<Component> rowTooltip(StockRow row) {
        return switch (row) {
            case HARVEST_LIMIT -> amountTooltip(FieldAmount.HARVEST_LIMIT);
            case FERTILIZE_BELOW -> amountTooltip(FieldAmount.FERTILIZE_BELOW);
            default -> List.of(Component.translatable(row.key()), Component.translatable(row.key() + ".tooltip"));
        };
    }

    /** The item a column counts: the produce, the crop, the fertilizer or the anti-corrosion item; empty for none. */
    private ItemStack columnItem(StockColumn column) {
        return switch (column) {
            case PRODUCE -> menu.produce();
            case SEEDS -> menu.crop();
            case FERTILIZER -> menu.fertilizer();
            case ANTI_CORROSION -> menu.antiCorrosion();
        };
    }

    /** The warehouse stock of the column's item; null while there is no item or no warehouse count. */
    private Long warehouseCount(StockColumn column) {
        ItemStack item = columnItem(column);
        if (item.isEmpty()) {
            return null;
        }
        if (column == StockColumn.PRODUCE) {
            return (long) menu.produceStock();
        }
        Map<Identifier, Long> stock = column == StockColumn.SEEDS ? menu.stock() : menu.supplyStock();
        return stock == null ? null : stock.getOrDefault(BuiltInRegistries.ITEM.getKey(item.getItem()), 0L);
    }

    /** What the amount does, the produce a level counts, and how to type it. */
    private List<Component> amountTooltip(FieldAmount amount) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(amountKey(amount)));
        lines.add(Component.translatable(amountKey(amount) + ".tooltip"));
        lines.add(Component.translatable(amountKey(amount) + ".hint"));
        if (amount.level() != null) {
            ItemStack produce = menu.produce();
            lines.add(produce.isEmpty() ? Component.translatable("gui.servus_shuttles.field_head.produce_unknown")
                    : Component.translatable("gui.servus_shuttles.field_head.produce_stock", produce.getHoverName(), grouped(menu.produceStock())));
        }
        lines.add(Component.translatable("gui.servus_shuttles.field_head.amount.typing"));
        return lines;
    }

    /** What a buffer part holds and how it is filled. */
    private List<Component> bufferTooltip(SupplyBuffer.Part part) {
        List<Component> lines = new ArrayList<>();
        String key = "gui.servus_shuttles.field_head.buffer." + part.name().toLowerCase(Locale.ROOT);
        if (part == SupplyBuffer.Part.ANTI_CORROSION) {
            lines.add(Component.translatable(key, antiCorrosionName(), menu.supplyCount(part), menu.supplyLevel(part)));
            lines.add(Component.translatable("gui.servus_shuttles.field_head.buffer.anti_corrosion_tooltip"));
        } else {
            lines.add(Component.translatable(key, menu.supplyCount(part), menu.supplyLevel(part)));
        }
        lines.add(Component.translatable("gui.servus_shuttles.field_head.buffer.filled_by"));
        for (SlotLocksPayload.Entry lock : menu.locksOf(part)) {
            lines.add(LockOverlay.line(lock));
        }
        return lines;
    }

    private Component antiCorrosionName() {
        ItemStack antiCorrosion = menu.antiCorrosion();
        return antiCorrosion.isEmpty() ? Component.translatable("tag.item.servus_shuttles.anti_corrosion") : antiCorrosion.getHoverName();
    }


    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private boolean emptySlotHovered(int index, int mouseX, int mouseY) {
        Slot slot = menu.slots.get(index);
        return menu.getCarried().isEmpty() && !slot.hasItem() && inside(mouseX, mouseY, slot.x, slot.y, 16, 16);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        renderTabs(graphics);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        PanelPainter.playerInventory(graphics, leftPos, topPos, FieldHeadMenu.INVENTORY_X, FieldHeadMenu.INVENTORY_Y,
                FieldHeadMenu.INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET);
        switch (menu.page()) {
            case FIELD -> {
                cropPicker.renderButton(graphics, font, mouseX, mouseY);
                if (serving()) {
                    renderRipeBar(graphics);
                }
            }
            case WATER -> {
                antiCorrosionPicker.renderButton(graphics, font, mouseX, mouseY);
                renderWaterBar(graphics);
            }
            case FERTILIZER -> fertilizerPicker.renderButton(graphics, font, mouseX, mouseY);
            case STOCK -> renderStockIcons(graphics);
            case UPGRADES -> slotFrame(graphics, FieldHeadMenu.RANGE_X, FieldHeadMenu.RANGE_Y);
        }
    }

    /** The item of each column above the table, and the rule between the counts and the settings. */
    private void renderStockIcons(GuiGraphicsExtractor graphics) {
        for (StockColumn column : StockColumn.values()) {
            ItemStack item = columnItem(column);
            if (!item.isEmpty()) {
                graphics.fakeItem(item, leftPos + columnX(column) + (COLUMN_W - 16) / 2, topPos + HEADER_Y);
            }
        }
        graphics.fill(leftPos + MARGIN, topPos + RULE_Y, leftPos + RIGHT, topPos + RULE_Y + 1, COLOUR_RULE);
        for (int slot = 0; slot < menu.toStorageShown(); slot++) {
            slotFrame(graphics, FieldHeadMenu.TO_STORAGE_X + slot * 18, FieldHeadMenu.TO_STORAGE_Y);
        }
    }

    private void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
        PanelPainter.sunken(graphics, leftPos + slotX - 1, topPos + slotY - 1, 18, 18);
    }

    /** The ripe share of the planted plots, dark green once a harvest is due, with a mark at the threshold. */
    private void renderRipeBar(GuiGraphicsExtractor graphics) {
        int left = leftPos + RIPE_X;
        int top = topPos + RIPE_Y;
        PanelPainter.sunken(graphics, left - 1, top - 1, RIPE_W + 2, RIPE_H + 2);
        int fill = menu.planted() <= 0 ? 0 : RIPE_W * Math.min(menu.ripe(), menu.planted()) / menu.planted();
        if (fill > 0) {
            graphics.fill(left, top, left + fill, top + RIPE_H, menu.harvestDue() ? COLOUR_DUE : COLOUR_RIPE);
        }
        int mark = left + Math.min(RIPE_W - 1, RIPE_W * shownThreshold() / 100);
        graphics.fill(mark, top - 2, mark + 1, top + RIPE_H + 2, COLOUR_MARK);
    }

    private void renderWaterBar(GuiGraphicsExtractor graphics) {
        int left = leftPos + MARGIN;
        int top = topPos + BAR_Y;
        PanelPainter.sunken(graphics, left - 1, top - 1, WIDTH + 2, BAR_H + 2);
        int capacity = Math.max(1, menu.capacity());
        int filled = (int) ((long) WIDTH * Math.min(menu.water(), capacity) / capacity);
        if (filled > 0) {
            graphics.fill(left, top, left + filled, top + BAR_H, menu.hydrated() ? COLOUR_WATER : COLOUR_DRY);
        }
    }

    /** The harvest threshold the Field tab shows: the dragged slider's while it moves, else the head's. */
    private int shownThreshold() {
        return sliderPercent > 0 ? sliderPercent : menu.threshold();
    }

    private boolean onRipeBar(double mouseX, double mouseY) {
        return inside((int) mouseX, (int) mouseY, RIPE_X, RIPE_Y - SLIDER_GRACE, RIPE_W, RIPE_H + 2 * SLIDER_GRACE);
    }

    /** Moves the slider to the mouse and sends the percent when it changed. */
    private void slideTo(double mouseX) {
        int percent = FieldThreshold.fromFraction((mouseX - leftPos - RIPE_X) / RIPE_W);
        if (percent != shownThreshold()) {
            press(FieldHeadMenu.BUTTON_HARVEST_PERCENT + percent / FieldThreshold.STEP);
        }
        sliderPercent = percent;
    }

    private static int percent(int ripe, int planted) {
        return planted <= 0 ? 0 : 100 * ripe / planted;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        switch (menu.page()) {
            case FIELD -> renderFieldLabels(graphics);
            case WATER -> renderWaterLabels(graphics);
            case FERTILIZER -> renderFertilizerLabels(graphics);
            case STOCK -> renderStockLabels(graphics);
            case UPGRADES -> renderUpgradeLabels(graphics);
        }
    }

    private void renderFertilizerLabels(GuiGraphicsExtractor graphics) {
        if (menu.fertilizeMode() == FertilizeMode.UNTIL_RIPE) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.field_head.fertilize_at", menu.fertilizePercent()),
                    MARGIN, FERTILIZE_Y, COLOUR_TEXT, false);
        }
        if (!serving()) {
            wrapped(graphics, statusLine(), FERTILIZE_STATUS_Y, COLOUR_WARN);
            return;
        }
        FertilizeStatus status = menu.fertilizeStatus();
        if (status != null) {
            int colour = status.problem() ? COLOUR_WARN : status == FertilizeStatus.WORKING ? COLOUR_DUE : COLOUR_TEXT;
            wrapped(graphics, fertilizeLine(status), FERTILIZE_STATUS_Y, colour);
        }
    }

    private void renderFieldLabels(GuiGraphicsExtractor graphics) {
        if (!serving()) {
            wrapped(graphics, statusLine(), RIPE_Y, COLOUR_WARN);
            return;
        }
        graphics.text(font, Component.translatable("gui.servus_shuttles.field_head.ripe", menu.ripe(), menu.planted()), MARGIN, RIPE_Y, COLOUR_TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_shuttles.field_head.harvest_at", shownThreshold()), MARGIN, THRESHOLD_Y, COLOUR_TEXT, false);
        FieldWork work = menu.work();
        if (work != null) {
            int colour = switch (work) {
                case WORKING, HARVEST_DUE, READY -> COLOUR_DUE;
                default -> work.problem() ? COLOUR_WARN : COLOUR_TEXT;
            };
            wrapped(graphics, workLine(work), WORK_Y, colour);
        }
    }

    /** The row labels, a "?" or "off" for a column without an item, the warehouse counts and the buffer counts. */
    private void renderStockLabels(GuiGraphicsExtractor graphics) {
        for (StockRow row : StockRow.values()) {
            clipped(graphics, Component.translatable(row.key()), MARGIN, row.textY, TABLE_X - MARGIN - 2, COLOUR_TEXT);
        }
        clipped(graphics, Component.translatable("gui.servus_shuttles.field_head.to_storage"), MARGIN, FieldHeadMenu.TO_STORAGE_Y + 4,
                TABLE_X - MARGIN - 2, COLOUR_TEXT);
        for (StockColumn column : StockColumn.values()) {
            if (columnItem(column).isEmpty()) {
                cell(graphics, column == StockColumn.PRODUCE ? "?"
                        : Component.translatable("gui.servus_shuttles.field_head.buffer.off").getString(), column, HEADER_Y + 4, COLOUR_HINT);
            }
            Long count = warehouseCount(column);
            cell(graphics, count == null ? "-" : CompactCount.of(count), column, WAREHOUSE_Y, count == null ? COLOUR_HINT : COLOUR_TEXT);
            if (column.part == null) {
                cell(graphics, "-", column, HEAD_Y, COLOUR_HINT);
                continue;
            }
            List<SlotLocksPayload.Entry> locks = menu.locksOf(column.part);
            if (!locks.isEmpty()) {
                int x = columnX(column);
                graphics.fill(x, HEAD_Y - 2, x + COLUMN_W, HEAD_Y - 2 + ROW_H, LockOverlay.wash(locks.get(0).in()));
            }
            int level = menu.supplyLevel(column.part);
            int held = menu.supplyCount(column.part);
            cell(graphics, level > 0 ? held + "/" + level : "-", column, HEAD_Y,
                    level <= 0 ? COLOUR_HINT : held > 0 ? COLOUR_TEXT : COLOUR_WARN);
        }
    }

    /** Text centred in a column of the Stock tab's table. */
    private void cell(GuiGraphicsExtractor graphics, String text, StockColumn column, int y, int colour) {
        graphics.text(font, text, columnX(column) + (COLUMN_W - font.width(text)) / 2, y, colour, false);
    }

    private void renderWaterLabels(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.translatable("gui.servus_shuttles.field_head.water", menu.water(), menu.capacity()), MARGIN, WATER_Y, COLOUR_TEXT, false);
        if (serving()) {
            Component water = menu.hydrated() || menu.waterPerMinute() <= 0
                    ? Component.translatable("gui.servus_shuttles.field_head.water_rate", menu.waterPerMinute())
                    : Component.translatable("gui.servus_shuttles.field_head.dry");
            graphics.text(font, water, MARGIN, WATER_RATE_Y, menu.hydrated() ? COLOUR_TEXT : COLOUR_WARN, false);
        }
        int stage = menu.corrosion();
        if (stage >= FieldCorrosion.COPPER) {
            Component corrosion = stage == FieldCorrosion.CORRODED
                    ? Component.translatable("gui.servus_shuttles.field_head.corroded")
                    : Component.translatable("gui.servus_shuttles.field_head.corrosion",
                            Component.translatable("gui.servus_shuttles.field_head.stage." + FieldCorrosion.name(stage)), menu.corrosionProgress());
            graphics.text(font, corrosion, MARGIN, CORROSION_Y,
                    stage == FieldCorrosion.CORRODED ? COLOUR_WARN : COLOUR_TEXT, false);
        }
    }

    /** The copper stage and when its next aging step is due; empty while the head serves no field. */
    private List<Component> corrosionTooltip() {
        int stage = menu.corrosion();
        if (stage < FieldCorrosion.COPPER) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.servus_shuttles.field_head.stage." + FieldCorrosion.name(stage)));
        Component aging = agingLine(stage);
        if (aging != null) {
            lines.add(aging);
        }
        return lines;
    }

    /** When the next aging step is due while the field is watered, and whether an anti-corrosion item will hold it; null when unknown. */
    private Component agingLine(int stage) {
        long seconds = menu.secondsToAging();
        if (stage == FieldCorrosion.CORRODED || seconds < 0) {
            return null;
        }
        String in = FieldDuration.of(seconds);
        return menu.supplyCount(SupplyBuffer.Part.ANTI_CORROSION) > 0
                ? Component.translatable("gui.servus_shuttles.field_head.aging_held", in, menu.antiCorrosion().getHoverName())
                : Component.translatable("gui.servus_shuttles.field_head.aging_in", in);
    }

    private void renderUpgradeLabels(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.translatable("gui.servus_shuttles.field_head.radius", menu.radius()),
                RADIUS_TEXT_X, FieldHeadMenu.RANGE_Y + 4, COLOUR_TEXT, false);
        if (!serving()) {
            wrapped(graphics, statusLine(), SIZE_Y, COLOUR_WARN);
            return;
        }
        graphics.text(font, Component.translatable("gui.servus_shuttles.field_head.size", menu.rails(), menu.plots()), MARGIN, SIZE_Y, COLOUR_TEXT, false);
    }

    /** Draws a line wrapped to the page width, at most {@link #ROWS} rows. */
    private void wrapped(GuiGraphicsExtractor graphics, Component text, int y, int colour) {
        List<FormattedCharSequence> rows = font.split(text, WIDTH);
        for (int i = 0; i < Math.min(ROWS, rows.size()); i++) {
            graphics.text(font, rows.get(i), MARGIN, y + i * 9, colour, false);
        }
    }

    /** Draws one line cut to the width. */
    private void clipped(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int colour) {
        List<FormattedCharSequence> rows = font.split(text, width);
        if (!rows.isEmpty()) {
            graphics.text(font, rows.get(0), x, y, colour, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ItemPicker open = openPicker();
        if (open != null) {
            return open.mouseClicked(event, doubleClick);
        }
        Page tab = tabAt(event.x(), event.y());
        if (tab != null) {
            clearFieldFocus();
            menu.setPage(tab);
            return true;
        }
        AmountField focused = focusedField();
        if (focused != null && !focused.box().isMouseOver(event.x(), event.y())) {
            clearFieldFocus();
        }
        ItemPicker shown = pagePicker();
        if (shown != null && shown.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (menu.page() == Page.FIELD && serving() && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && onRipeBar(event.x(), event.y())) {
            slideTo(event.x());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        ItemPicker open = openPicker();
        if (open == null && sliderPercent > 0) {
            slideTo(event.x());
            return true;
        }
        return open != null && open.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        sliderPercent = 0;
        ItemPicker open = openPicker();
        return open != null && open.mouseReleased() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ItemPicker open = openPicker();
        return open != null && open.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** A typed field keeps every key but Escape, so E does not close the GUI; Enter sends its number. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        ItemPicker open = openPicker();
        if (open != null) {
            return open.keyPressed(event) || super.keyPressed(event);
        }
        AmountField focused = focusedField();
        if (focused != null && event.key() != GLFW.GLFW_KEY_ESCAPE) {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                focused.commit();
                clearFieldFocus();
                return true;
            }
            if (focused.box().keyPressed(event) || focused.box().canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        ItemPicker open = openPicker();
        return open != null && open.charTyped(event) || super.charTyped(event);
    }
}
