package io.github.dirnei.mc.servus.storage.client;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
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
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import io.github.dirnei.mc.servus.storage.picker.PickerList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_DUE = 0x2E7D32;
    private static final int COLOUR_WARN = 0xA04000;
    private static final int COLOUR_WATER = 0xFF3F76E4;
    private static final int COLOUR_DRY = 0xFF9A8F80;
    private static final int COLOUR_RIPE = 0xFFD8B33A;
    private static final int COLOUR_MARK = 0xFF202020;
    private static final int COLOUR_PROBLEM = 0xFFD02020;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF373737;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_TOP = 10;
    private static final int TAB_STRIDE = 28;
    // Field page
    private static final int RIPE_Y = 40;
    private static final int RIPE_X = 60;
    private static final int RIPE_W = MARGIN + WIDTH - RIPE_X;
    private static final int RIPE_H = 7;
    private static final int THRESHOLD_Y = 53;
    private static final int WORK_Y = 66;
    // Water page
    private static final int WATER_Y = 20;
    private static final int BAR_Y = 31;
    private static final int BAR_H = 8;
    private static final int HYDRATED_Y = 43;
    private static final int CORROSION_TEXT_X = 30;
    // Upgrades page
    private static final int RADIUS_TEXT_X = 30;
    private static final int SIZE_Y = 44;
    // Fertilizer page
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

    private Button down;
    private final ItemPicker picker;
    private final ItemPicker fertilizerPicker;
    /** The fertilizer stock map last handed to its picker. */
    private Map<Identifier, Long> fertilizerPickerStock;
    private final List<Button> modeButtons = new ArrayList<>();
    private final List<Button> reserveButtons = new ArrayList<>();
    private Button fertilizeDown;
    private Button fertilizeUp;
    /** The stock map last handed to the picker, to hand it over only when the menu got a new one. */
    private Map<Identifier, Long> pickerStock;
    private Button up;

    public FieldHeadScreen(FieldHeadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 178);
        this.picker = new ItemPicker(Component.translatable("gui.servus_storage.field_head.crop_picker"), this::choose);
        picker.setEntries(CropPickerEntries.build());
        picker.setZeroHint(Component.translatable("gui.servus_storage.field_head.crop_no_stock"));
        this.fertilizerPicker = new ItemPicker(Component.translatable("gui.servus_storage.field_head.fertilizer_picker"), this::chooseFertilizer);
        fertilizerPicker.setEntries(FertilizerPickerEntries.build());
        fertilizerPicker.setZeroHint(Component.translatable("gui.servus_storage.field_head.fertilizer_no_stock"));
    }

    /** Sends the picked fertilizer, or Off for the none entry, to the server. */
    private void chooseFertilizer(ItemPicker.Entry entry) {
        Optional<Identifier> fertilizer = entry.isNone() ? Optional.empty() : Optional.ofNullable(Identifier.tryParse(entry.id()));
        ClientPacketDistributor.sendToServer(new ChooseFertilizerPayload(fertilizer));
    }

    /** The picker of the shown page, null on pages without one. */
    private ItemPicker pagePicker() {
        return switch (menu.page()) {
            case FIELD -> picker;
            case FERTILIZER -> fertilizerPicker;
            default -> null;
        };
    }

    /** The picker whose popup is open, or null. */
    private ItemPicker openPicker() {
        if (picker.isOpen()) {
            return picker;
        }
        return fertilizerPicker.isOpen() ? fertilizerPicker : null;
    }

    /** Sends the picked crop, or harvest only for the none entry, to the server. */
    private void choose(ItemPicker.Entry entry) {
        Optional<Identifier> crop = entry.isNone() ? Optional.empty() : Optional.ofNullable(Identifier.tryParse(entry.id()));
        ClientPacketDistributor.sendToServer(new ChooseFieldCropPayload(crop));
    }

    /** Hands the head's crop and the menu's stock to the picker. */
    private void syncPicker() {
        ItemStack crop = menu.crop();
        picker.setSelected(menu.harvestOnly() || crop.isEmpty() ? PickerList.NONE_ID : BuiltInRegistries.ITEM.getKey(crop.getItem()).toString());
        if (menu.stock() != pickerStock) {
            pickerStock = menu.stock();
            picker.setStock(CropPickerEntries.keyed(pickerStock));
        }
        ItemStack fertilizer = menu.fertilizer();
        fertilizerPicker.setSelected(fertilizer.isEmpty() ? PickerList.NONE_ID : BuiltInRegistries.ITEM.getKey(fertilizer.getItem()).toString());
        if (menu.fertilizerStock() != fertilizerPickerStock) {
            fertilizerPickerStock = menu.fertilizerStock();
            fertilizerPicker.setStock(CropPickerEntries.keyed(fertilizerPickerStock));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = FieldHeadMenu.INVENTORY_Y - 11;
        picker.setButton(leftPos + FieldHeadMenu.PICKER_X, topPos + FieldHeadMenu.PICKER_Y, FieldHeadMenu.PICKER_WIDTH);
        picker.setArea(leftPos, topPos, imageWidth, imageHeight);
        down = addRenderableWidget(Button.builder(Component.literal("-"), button -> press(FieldHeadMenu.BUTTON_THRESHOLD_DOWN))
                .bounds(leftPos + 136, topPos + THRESHOLD_Y - 2, 14, 11).build());
        up = addRenderableWidget(Button.builder(Component.literal("+"), button -> press(FieldHeadMenu.BUTTON_THRESHOLD_UP))
                .bounds(leftPos + 152, topPos + THRESHOLD_Y - 2, 14, 11).build());
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
        fertilizeDown = addRenderableWidget(Button.builder(Component.literal("-"), button -> press(FieldHeadMenu.BUTTON_FERTILIZE_DOWN))
                .bounds(leftPos + 136, topPos + FERTILIZE_Y, 14, 11).build());
        fertilizeUp = addRenderableWidget(Button.builder(Component.literal("+"), button -> press(FieldHeadMenu.BUTTON_FERTILIZE_UP))
                .bounds(leftPos + 152, topPos + FERTILIZE_Y, 14, 11).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private boolean serving() {
        return menu.status() == FieldStatus.SERVING;
    }

    // Problems

    /** The work status is a warning, drawn in the warning colour. */
    private static boolean warning(FieldWork work) {
        return switch (work) {
            case WORKING, HARVEST_DUE, READY, GROWING, WAITING_RIPE -> false;
            default -> true;
        };
    }

    /** The problem the page shows, or null when it has none. */
    private Component problem(Page page) {
        switch (page) {
            case FIELD -> {
                if (!serving()) {
                    return statusLine();
                }
                FieldWork work = menu.work();
                return work != null && warning(work) ? workLine(work) : null;
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

    // Tabs

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
            graphics.fill(x, y, x + width, y + TAB_HEIGHT, problem ? COLOUR_PROBLEM : PANEL_DARK);
            graphics.fill(x + 1, y + 1, x + width, y + TAB_HEIGHT - 1, selected ? PANEL : 0xFF9A9A9A);
            graphics.fill(x + 1, y + 1, x + width, y + 2, PANEL_LIGHT);
            if (problem) {
                graphics.fill(x + 1, y + 1, x + 3, y + TAB_HEIGHT - 1, COLOUR_PROBLEM);
            }
            graphics.fakeItem(icon(page), x + 6, y + 5);
        }
    }

    // Rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Page page = menu.page();
        boolean serving = serving();
        down.visible = page == Page.FIELD && serving;
        up.visible = page == Page.FIELD && serving;
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
        // The Fertilizer page needs the room of the inventory label for its status.
        this.inventoryLabelY = fertilizerPage ? -10000 : FieldHeadMenu.INVENTORY_Y - 11;
        syncPicker();
        ItemPicker open = openPicker();
        if (open != null) {
            // While a picker is open nothing below it reacts to the mouse.
            super.render(graphics, -1, -1, partialTick);
            open.renderPopup(graphics, font, mouseX, mouseY, partialTick);
            open.renderTooltip(graphics, font, mouseX, mouseY);
            return;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        ItemPicker shown = pagePicker();
        if (shown != null && shown.renderTooltip(graphics, font, mouseX, mouseY)) {
            return;
        }
        if (fertilizerPage && inside(mouseX, mouseY, MARGIN, RESERVE_Y, RESERVE_X[0] - MARGIN, 11)) {
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
        if (page == Page.FIELD && serving && inside(mouseX, mouseY, RIPE_X, RIPE_Y, RIPE_W, RIPE_H)) {
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
        var slot = menu.slots.get(index);
        return menu.getCarried().isEmpty() && !slot.hasItem() && inside(mouseX, mouseY, slot.x, slot.y, 16, 16);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        renderTabs(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        switch (menu.page()) {
            case FIELD -> {
                picker.renderButton(graphics, font, mouseX, mouseY);
                if (serving()) {
                    int ripeLeft = leftPos + RIPE_X;
                    int ripeTop = topPos + RIPE_Y;
                    sunken(graphics, ripeLeft - 1, ripeTop - 1, RIPE_W + 2, RIPE_H + 2);
                    int ripeFill = menu.planted() <= 0 ? 0 : RIPE_W * Math.min(menu.ripe(), menu.planted()) / menu.planted();
                    if (ripeFill > 0) {
                        graphics.fill(ripeLeft, ripeTop, ripeLeft + ripeFill, ripeTop + RIPE_H, menu.harvestDue() ? 0xFF000000 | COLOUR_DUE : COLOUR_RIPE);
                    }
                    int mark = ripeLeft + Math.min(RIPE_W - 1, RIPE_W * menu.threshold() / 100);
                    graphics.fill(mark, ripeTop - 2, mark + 1, ripeTop + RIPE_H + 2, COLOUR_MARK);
                }
            }
            case WATER -> {
                int barLeft = leftPos + MARGIN;
                int barTop = topPos + BAR_Y;
                sunken(graphics, barLeft - 1, barTop - 1, WIDTH + 2, BAR_H + 2);
                int capacity = Math.max(1, menu.capacity());
                int filled = (int) ((long) WIDTH * Math.min(menu.water(), capacity) / capacity);
                if (filled > 0) {
                    graphics.fill(barLeft, barTop, barLeft + filled, barTop + BAR_H, menu.hydrated() ? COLOUR_WATER : COLOUR_DRY);
                }
                sunken(graphics, leftPos + FieldHeadMenu.ANTI_CORROSION_X - 1, topPos + FieldHeadMenu.ANTI_CORROSION_Y - 1, 18, 18);
            }
            case FERTILIZER -> {
                fertilizerPicker.renderButton(graphics, font, mouseX, mouseY);
                sunken(graphics, leftPos + RESERVE_TEXT_X, topPos + RESERVE_Y, RESERVE_TEXT_W, 11);
            }
            case UPGRADES -> sunken(graphics, leftPos + FieldHeadMenu.RANGE_X - 1, topPos + FieldHeadMenu.RANGE_Y - 1, 18, 18);
        }
    }

    private static int percent(int ripe, int planted) {
        return planted <= 0 ? 0 : 100 * ripe / planted;
    }

    private static void sunken(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x, y, x + width, y + 1, PANEL_DARK);
        graphics.fill(x, y, x + 1, y + height, PANEL_DARK);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_LIGHT);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_LIGHT);
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
                default -> warning(work) ? COLOUR_WARN : COLOUR_TEXT;
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
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        ItemPicker open = openPicker();
        if (open != null) {
            return open.mouseClicked(mouseX, mouseY, button);
        }
        Page tab = tabAt(mouseX, mouseY);
        if (tab != null) {
            menu.setPage(tab);
            return true;
        }
        ItemPicker shown = pagePicker();
        if (shown != null && shown.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        ItemPicker open = openPicker();
        return open != null && open.mouseDragged(mouseX, mouseY) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
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
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        ItemPicker open = openPicker();
        return open != null && open.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char codePoint = (char) event.codepoint();
        int modifiers = 0;
        ItemPicker open = openPicker();
        return open != null && open.charTyped(codePoint, modifiers) || super.charTyped(event);
    }
}
