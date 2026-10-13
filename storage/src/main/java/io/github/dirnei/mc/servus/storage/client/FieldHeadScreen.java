package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.field.ChooseFertilizerPayload;
import io.github.dirnei.mc.servus.storage.field.ChooseFieldCropPayload;
import io.github.dirnei.mc.servus.storage.field.FertilizeMode;
import io.github.dirnei.mc.servus.storage.field.FertilizeStatus;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu.Page;
import io.github.dirnei.mc.servus.storage.picker.PickerList;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Field Head GUI in four tabs on the left edge, drawn like the Utility
 * Duct tabs: Field (crop picker, ripe share, threshold, work status),
 * Water (water bar, anti-corrosion slot, corrosion stage), Fertilizer
 * (fertilizer picker, mode, reserve, threshold, Spreader status) and Upgrades
 * (Range Upgrade slot, radius, field size). The player inventory is always
 * below. A tab whose page holds a problem gets a red marker.
 */
public class FieldHeadScreen extends AbstractContainerScreen<FieldHeadMenu> {

    /** The plain panel of the garage GUI; slot frames and bars are drawn on it. */
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/shuttle_garage.png");
    private static final int MARGIN = 8;
    private static final int WIDTH = 160;
    private static final int COLOUR_TEXT = 0xFF404040;
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
    private static final int LABEL_HIDDEN = -10000;
    private static final int RIPE_Y = 40;
    private static final int RIPE_X = 60;
    private static final int RIPE_W = MARGIN + WIDTH - RIPE_X;
    private static final int RIPE_H = 7;
    private static final int THRESHOLD_Y = 53;
    private static final int WORK_Y = 66;
    private static final int WATER_Y = 20;
    private static final int BAR_Y = 31;
    private static final int BAR_H = 8;
    private static final int HYDRATED_Y = 43;
    private static final int CORROSION_TEXT_X = 30;
    private static final int RADIUS_TEXT_X = 30;
    private static final int SIZE_Y = 44;
    private static final int MODE_Y = 37;
    private static final int MODE_H = 13;
    private static final int[] MODE_X = {8, 34, 110};
    private static final int[] MODE_W = {24, 74, 58};
    private static final int RESERVE_Y = 53;
    private static final int[] RESERVE_X = {70, 90, 135, 150};
    private static final int[] RESERVE_W = {18, 13, 13, 18};
    private static final int RESERVE_TEXT_X = 104;
    private static final int RESERVE_TEXT_W = 30;
    private static final int FERTILIZE_Y = 65;
    private static final int FERTILIZE_STATUS_Y = 79;

    private final ItemPicker cropPicker;
    private final ItemPicker fertilizerPicker;
    /** The stock maps last handed to the pickers, to hand them over only when the menu got new ones. */
    private Map<Identifier, Long> cropPickerStock;
    private Map<Identifier, Long> fertilizerPickerStock;
    private final List<Button> modeButtons = new ArrayList<>();
    private final List<Button> reserveButtons = new ArrayList<>();
    private Button harvestDown;
    private Button harvestUp;
    private Button fertilizeDown;
    private Button fertilizeUp;

    public FieldHeadScreen(FieldHeadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 178);
        this.cropPicker = picker("crop_picker", "crop_no_stock", CropPickerEntries.build(),
                entry -> ClientPacketDistributor.sendToServer(new ChooseFieldCropPayload(idOf(entry))));
        this.fertilizerPicker = picker("fertilizer_picker", "fertilizer_no_stock", FertilizerPickerEntries.build(),
                entry -> ClientPacketDistributor.sendToServer(new ChooseFertilizerPayload(idOf(entry))));
    }

    private static ItemPicker picker(String title, String zeroHint, List<ItemPicker.Entry> entries,
                                     Consumer<ItemPicker.Entry> onPick) {
        ItemPicker picker = new ItemPicker(Component.translatable("gui.servus_storage.field_head." + title), onPick);
        picker.setEntries(entries);
        picker.setZeroHint(Component.translatable("gui.servus_storage.field_head." + zeroHint));
        return picker;
    }

    /** The picked item id, or empty for the none entry. */
    private static Optional<Identifier> idOf(ItemPicker.Entry entry) {
        return entry.isNone() ? Optional.empty() : Optional.ofNullable(Identifier.tryParse(entry.id()));
    }

    /** The picker of the shown page, null on pages without one. */
    private ItemPicker pagePicker() {
        return switch (menu.page()) {
            case FIELD -> cropPicker;
            case FERTILIZER -> fertilizerPicker;
            default -> null;
        };
    }

    /** The picker whose popup is open, or null. */
    private ItemPicker openPicker() {
        if (cropPicker.isOpen()) {
            return cropPicker;
        }
        return fertilizerPicker.isOpen() ? fertilizerPicker : null;
    }

    private static String pickerId(ItemStack stack) {
        return stack.isEmpty() ? PickerList.NONE_ID : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** Hands the head's crop and fertilizer and the menu's stock to the pickers. */
    private void syncPickers() {
        cropPicker.setSelected(menu.harvestOnly() ? PickerList.NONE_ID : pickerId(menu.crop()));
        if (menu.stock() != cropPickerStock) {
            cropPickerStock = menu.stock();
            cropPicker.setStock(CropPickerEntries.keyed(cropPickerStock));
        }
        fertilizerPicker.setSelected(pickerId(menu.fertilizer()));
        if (menu.fertilizerStock() != fertilizerPickerStock) {
            fertilizerPickerStock = menu.fertilizerStock();
            fertilizerPicker.setStock(CropPickerEntries.keyed(fertilizerPickerStock));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = FieldHeadMenu.INVENTORY_Y - 11;
        cropPicker.setButton(leftPos + FieldHeadMenu.PICKER_X, topPos + FieldHeadMenu.PICKER_Y, FieldHeadMenu.PICKER_WIDTH);
        cropPicker.setArea(leftPos, topPos, imageWidth, imageHeight);
        harvestDown = stepButton("-", THRESHOLD_Y - 2, 136, FieldHeadMenu.BUTTON_THRESHOLD_DOWN);
        harvestUp = stepButton("+", THRESHOLD_Y - 2, 152, FieldHeadMenu.BUTTON_THRESHOLD_UP);
        fertilizerPicker.setButton(leftPos + FieldHeadMenu.PICKER_X, topPos + FieldHeadMenu.PICKER_Y, FieldHeadMenu.PICKER_WIDTH);
        fertilizerPicker.setArea(leftPos, topPos, imageWidth, imageHeight);
        modeButtons.clear();
        for (FertilizeMode mode : FertilizeMode.values()) {
            String key = "gui.servus_storage.field_head.fertilize_mode." + mode.name().toLowerCase(Locale.ROOT);
            int index = mode.ordinal();
            modeButtons.add(addRenderableWidget(Button.builder(Component.translatable(key), button -> press(FieldHeadMenu.BUTTON_FERTILIZE_MODE + index))
                    .bounds(leftPos + MODE_X[index], topPos + MODE_Y, MODE_W[index], MODE_H)
                    .tooltip(Tooltip.create(Component.translatable(key + ".tooltip"))).build()));
        }
        reserveButtons.clear();
        for (int i = 0; i < FieldHeadMenu.RESERVE_STEPS.length; i++) {
            int step = FieldHeadMenu.RESERVE_STEPS[i];
            int id = FieldHeadMenu.BUTTON_RESERVE + i;
            reserveButtons.add(addRenderableWidget(Button.builder(Component.literal(step > 0 ? "+" + step : String.valueOf(step)), button -> press(id))
                    .bounds(leftPos + RESERVE_X[i], topPos + RESERVE_Y, RESERVE_W[i], 11).build()));
        }
        fertilizeDown = stepButton("-", FERTILIZE_Y, 136, FieldHeadMenu.BUTTON_FERTILIZE_DOWN);
        fertilizeUp = stepButton("+", FERTILIZE_Y, 152, FieldHeadMenu.BUTTON_FERTILIZE_UP);
    }

    private Button stepButton(String label, int y, int x, int id) {
        return addRenderableWidget(Button.builder(Component.literal(label), button -> press(id))
                .bounds(leftPos + x, topPos + y, 14, 11).build());
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
                    return Component.translatable("gui.servus_storage.field_head.corroded");
                }
                return serving() && !menu.hydrated() ? Component.translatable("gui.servus_storage.field_head.dry") : null;
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
        return Component.translatable("gui.servus_storage.field_head.status." + menu.status().name().toLowerCase(Locale.ROOT));
    }

    private Component workLine(FieldWork work) {
        String worker = ClientFieldWorker.name();
        return work == FieldWork.WORKING && !worker.isEmpty()
                ? Component.translatable("gui.servus_storage.field_head.work.working_named", worker)
                : Component.translatable("gui.servus_storage.field_head.work." + work.name().toLowerCase(Locale.ROOT));
    }

    private Component fertilizeLine(FertilizeStatus status) {
        String spreader = ClientFieldWorker.spreader();
        return status == FertilizeStatus.WORKING && !spreader.isEmpty()
                ? Component.translatable("gui.servus_storage.field_head.fertilize.working_named", spreader)
                : Component.translatable("gui.servus_storage.field_head.fertilize." + status.name().toLowerCase(Locale.ROOT));
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
            case UPGRADES -> new ItemStack(ServusStorageItems.RANGE_UPGRADE.get());
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

    /** Shows the widgets of the current page; the Fertilizer page needs the inventory label's room for its status. */
    private void updateWidgets() {
        Page page = menu.page();
        boolean serving = serving();
        harvestDown.visible = page == Page.FIELD && serving;
        harvestUp.visible = page == Page.FIELD && serving;
        boolean fertilizerPage = page == Page.FERTILIZER;
        FertilizeMode mode = menu.fertilizeMode();
        for (int i = 0; i < modeButtons.size(); i++) {
            modeButtons.get(i).visible = fertilizerPage;
            modeButtons.get(i).active = mode.ordinal() != i;
        }
        for (Button button : reserveButtons) {
            button.visible = fertilizerPage;
        }
        fertilizeDown.visible = fertilizerPage && mode == FertilizeMode.UNTIL_RIPE;
        fertilizeUp.visible = fertilizerPage && mode == FertilizeMode.UNTIL_RIPE;
        this.inventoryLabelY = fertilizerPage ? LABEL_HIDDEN : FieldHeadMenu.INVENTORY_Y - 11;
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
        Page page = menu.page();
        if (page == Page.FERTILIZER && inside(mouseX, mouseY, MARGIN, RESERVE_Y, RESERVE_X[0] - MARGIN, 11)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_storage.field_head.reserve_tooltip"), mouseX, mouseY);
            return;
        }
        Page hovered = tabAt(mouseX, mouseY);
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.servus_storage.field_head.tab." + hovered.name().toLowerCase(Locale.ROOT)));
            Component problem = problem(hovered);
            if (problem != null) {
                lines.add(problem.copy().withColor(COLOUR_PROBLEM & 0xFFFFFF));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        if (page == Page.FIELD && serving() && inside(mouseX, mouseY, RIPE_X, RIPE_Y, RIPE_W, RIPE_H)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_storage.field_head.ripe_tooltip",
                    menu.ripe(), menu.planted(), percent(menu.ripe(), menu.planted()), menu.threshold()), mouseX, mouseY);
        }
        if (page == Page.WATER && inside(mouseX, mouseY, MARGIN, BAR_Y, WIDTH, BAR_H)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.servus_storage.field_head.water", menu.water(), menu.capacity()),
                    Component.translatable(menu.hydrated() ? "gui.servus_storage.field_head.hydrated" : "gui.servus_storage.field_head.dry")),
                    mouseX, mouseY);
        }
        if (page == Page.UPGRADES && emptySlotHovered(FieldHeadMenu.RANGE_SLOT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_storage.field_head.range_slot", menu.radius()), mouseX, mouseY);
        }
        if (page == Page.WATER && emptySlotHovered(FieldHeadMenu.ANTI_CORROSION_SLOT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_storage.field_head.anti_corrosion_slot"), mouseX, mouseY);
        }
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        switch (menu.page()) {
            case FIELD -> {
                cropPicker.renderButton(graphics, font, mouseX, mouseY);
                if (serving()) {
                    renderRipeBar(graphics);
                }
            }
            case WATER -> {
                renderWaterBar(graphics);
                slotFrame(graphics, FieldHeadMenu.ANTI_CORROSION_X, FieldHeadMenu.ANTI_CORROSION_Y);
            }
            case FERTILIZER -> {
                fertilizerPicker.renderButton(graphics, font, mouseX, mouseY);
                PanelPainter.sunken(graphics, leftPos + RESERVE_TEXT_X, topPos + RESERVE_Y, RESERVE_TEXT_W, 11);
            }
            case UPGRADES -> slotFrame(graphics, FieldHeadMenu.RANGE_X, FieldHeadMenu.RANGE_Y);
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
        int mark = left + Math.min(RIPE_W - 1, RIPE_W * menu.threshold() / 100);
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
            case UPGRADES -> renderUpgradeLabels(graphics);
        }
    }

    private void renderFertilizerLabels(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.reserve"), MARGIN, RESERVE_Y + 2, COLOUR_TEXT, false);
        String reserve = String.valueOf(menu.reserve());
        graphics.text(font, reserve, RESERVE_TEXT_X + (RESERVE_TEXT_W - font.width(reserve)) / 2, RESERVE_Y + 2, COLOUR_TEXT, false);
        if (menu.fertilizeMode() == FertilizeMode.UNTIL_RIPE) {
            graphics.text(font, Component.translatable("gui.servus_storage.field_head.fertilize_at", menu.fertilizePercent()),
                    MARGIN, FERTILIZE_Y + 2, COLOUR_TEXT, false);
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
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.ripe", menu.ripe(), menu.planted()), MARGIN, RIPE_Y, COLOUR_TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.harvest_at", menu.threshold()), MARGIN, THRESHOLD_Y, COLOUR_TEXT, false);
        FieldWork work = menu.work();
        if (work != null) {
            int colour = switch (work) {
                case WORKING, HARVEST_DUE, READY -> COLOUR_DUE;
                default -> work.problem() ? COLOUR_WARN : COLOUR_TEXT;
            };
            wrapped(graphics, workLine(work), WORK_Y, colour);
        }
    }

    private void renderWaterLabels(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.water", menu.water(), menu.capacity()), MARGIN, WATER_Y, COLOUR_TEXT, false);
        if (serving()) {
            boolean hydrated = menu.hydrated();
            graphics.text(font, Component.translatable(hydrated ? "gui.servus_storage.field_head.hydrated" : "gui.servus_storage.field_head.dry"),
                    MARGIN, HYDRATED_Y, hydrated ? COLOUR_DUE : COLOUR_WARN, false);
        }
        int stage = menu.corrosion();
        if (stage >= FieldCorrosion.COPPER) {
            Component corrosion = stage == FieldCorrosion.CORRODED
                    ? Component.translatable("gui.servus_storage.field_head.corroded")
                    : Component.translatable("gui.servus_storage.field_head.corrosion",
                            Component.translatable("gui.servus_storage.field_head.stage." + FieldCorrosion.name(stage)), menu.corrosionProgress());
            graphics.text(font, corrosion, CORROSION_TEXT_X, FieldHeadMenu.ANTI_CORROSION_Y + 4,
                    stage == FieldCorrosion.CORRODED ? COLOUR_WARN : COLOUR_TEXT, false);
        }
    }

    private void renderUpgradeLabels(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.radius", menu.radius()),
                RADIUS_TEXT_X, FieldHeadMenu.RANGE_Y + 4, COLOUR_TEXT, false);
        if (!serving()) {
            wrapped(graphics, statusLine(), SIZE_Y, COLOUR_WARN);
            return;
        }
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.size", menu.rails(), menu.plots()), MARGIN, SIZE_Y, COLOUR_TEXT, false);
    }

    /** Draws a line wrapped to the panel width, at most two rows. */
    private void wrapped(GuiGraphicsExtractor graphics, Component text, int y, int colour) {
        List<FormattedCharSequence> rows = font.split(text, WIDTH);
        for (int i = 0; i < Math.min(2, rows.size()); i++) {
            graphics.text(font, rows.get(i), MARGIN, y + i * 9, colour, false);
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
            menu.setPage(tab);
            return true;
        }
        ItemPicker shown = pagePicker();
        if (shown != null && shown.mouseClicked(event, doubleClick)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        ItemPicker open = openPicker();
        return open != null && open.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        ItemPicker open = openPicker();
        return open != null && open.mouseReleased() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ItemPicker open = openPicker();
        return open != null && open.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        ItemPicker open = openPicker();
        return open != null && open.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        ItemPicker open = openPicker();
        return open != null && open.charTyped(event) || super.charTyped(event);
    }
}
