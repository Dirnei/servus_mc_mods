package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.storage.field.ChooseFertilizerPayload;
import io.github.dirnei.mc.servus.storage.field.ChooseFieldCropPayload;
import io.github.dirnei.mc.servus.storage.field.FertilizeMode;
import io.github.dirnei.mc.servus.storage.field.FertilizeStatus;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.field.SetFieldLevelPayload;
import io.github.dirnei.mc.servus.storage.field.StockLevels;
import io.github.dirnei.mc.servus.storage.field.SupplyBuffer;
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
import net.minecraft.client.Minecraft;
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

/**
 * The Field Head GUI in four tabs on the left edge, drawn like the Utility
 * Duct tabs: Field (crop picker, ripe share, threshold, harvest limit,
 * produce, work status), Water (water bar, anti-corrosion items in the buffer,
 * corrosion stage), Fertilizer (fertilizer picker, mode, reserve, threshold,
 * fertilize level, Spreader status) and Upgrades (Range Upgrade slot, radius,
 * field size). The player inventory is always below. A tab whose page holds a
 * problem gets a red marker.
 */
public class FieldHeadScreen extends AbstractContainerScreen<FieldHeadMenu> {

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
    private static final int RIPE_Y = 40;
    private static final int RIPE_X = 60;
    private static final int RIPE_W = MARGIN + WIDTH - RIPE_X;
    private static final int RIPE_H = 7;
    private static final int THRESHOLD_Y = 53;
    /** The harvest limit row: label, produce icon, step buttons around the value. */
    private static final int LIMIT_Y = 66;
    private static final int PRODUCE_Y = 80;
    private static final int WORK_Y = 100;
    private static final int WATER_Y = 20;
    private static final int BAR_Y = 31;
    private static final int BAR_H = 8;
    private static final int HYDRATED_Y = 43;
    /** The anti-corrosion icon on the Water tab; its count stands right of it. */
    private static final int ANTI_CORROSION_Y = 54;
    private static final int ANTI_CORROSION_TEXT_X = MARGIN + 20;
    private static final int CORROSION_Y = 74;
    /** The buffer icon right of the crop and fertilizer pickers; its count stands right of it. */
    private static final int BUFFER_X = FieldHeadMenu.PICKER_X + FieldHeadMenu.PICKER_WIDTH + 3;
    private static final int BUFFER_Y = FieldHeadMenu.PICKER_Y + 1;
    private static final int BUFFER_TEXT_X = BUFFER_X + 18;
    /** The right end of text beside the pickers, inside the panel's shaded rim. */
    private static final int TEXT_RIGHT = 170;
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
    private static final int BELOW_Y = 79;
    private static final int FERTILIZE_STATUS_Y = 96;
    /** A stock level row: icon after the label, buttons as in {@link FieldHeadMenu#LEVEL_STEPS}, the value between the inner two. */
    private static final int LEVEL_ICON_X = 46;
    private static final int[] LEVEL_X = {64, 83, 136, 150};
    private static final int[] LEVEL_W = {18, 13, 13, 18};
    private static final int LEVEL_TEXT_X = 97;
    private static final int LEVEL_TEXT_W = 38;

    private final ItemPicker cropPicker;
    private final ItemPicker fertilizerPicker;
    /** The stock maps last handed to the pickers, to hand them over only when the menu got new ones. */
    private Map<Identifier, Long> cropPickerStock;
    private Map<Identifier, Long> fertilizerPickerStock;
    private final List<Button> modeButtons = new ArrayList<>();
    private final List<Button> reserveButtons = new ArrayList<>();
    private final List<Button> limitButtons = new ArrayList<>();
    private final List<Button> belowButtons = new ArrayList<>();
    private Button harvestDown;
    private Button harvestUp;
    private Button fertilizeDown;
    private Button fertilizeUp;

    public FieldHeadScreen(FieldHeadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, FieldHeadMenu.IMAGE_HEIGHT);
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
            reserveButtons.add(addRenderableWidget(Button.builder(Component.literal(signed(step)), button -> press(id))
                    .bounds(leftPos + RESERVE_X[i], topPos + RESERVE_Y, RESERVE_W[i], 11).build()));
        }
        fertilizeDown = stepButton("-", FERTILIZE_Y, 136, FieldHeadMenu.BUTTON_FERTILIZE_DOWN);
        fertilizeUp = stepButton("+", FERTILIZE_Y, 152, FieldHeadMenu.BUTTON_FERTILIZE_UP);
        levelButtons(limitButtons, StockLevels.Level.HARVEST, LIMIT_Y);
        levelButtons(belowButtons, StockLevels.Level.FERTILIZE, BELOW_Y);
    }

    private void levelButtons(List<Button> buttons, StockLevels.Level which, int y) {
        buttons.clear();
        for (int i = 0; i < FieldHeadMenu.LEVEL_STEPS.length; i++) {
            int step = FieldHeadMenu.LEVEL_STEPS[i];
            buttons.add(addRenderableWidget(Button.builder(Component.literal(signed(step)), button -> ClientPacketDistributor.sendToServer(
                            new SetFieldLevelPayload(menu.headPos(), which.ordinal(), step, Minecraft.getInstance().hasShiftDown())))
                    .bounds(leftPos + LEVEL_X[i], topPos + y, LEVEL_W[i], 11).build()));
        }
    }

    private static String signed(int step) {
        return step > 0 ? "+" + step : String.valueOf(step);
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
        if (work == FieldWork.WORKING && !worker.isEmpty()) {
            return Component.translatable("gui.servus_storage.field_head.work.working_named", worker);
        }
        if (work == FieldWork.HARVEST_PAUSED) {
            return Component.translatable("gui.servus_storage.field_head.work.harvest_paused",
                    grouped(menu.produceStock()), grouped(menu.harvestLimit()), menu.produce().getHoverName());
        }
        return Component.translatable("gui.servus_storage.field_head.work." + work.name().toLowerCase(Locale.ROOT));
    }

    private Component fertilizeLine(FertilizeStatus status) {
        String spreader = ClientFieldWorker.spreader();
        if (status == FertilizeStatus.WORKING && !spreader.isEmpty()) {
            return Component.translatable("gui.servus_storage.field_head.fertilize.working_named", spreader);
        }
        if (status == FertilizeStatus.NOT_NEEDED) {
            return menu.harvestPaused()
                    ? Component.translatable("gui.servus_storage.field_head.fertilize.not_needed_paused")
                    : Component.translatable("gui.servus_storage.field_head.fertilize.not_needed",
                            grouped(menu.produceStock()), menu.produce().getHoverName(), grouped(menu.fertilizeBelow()));
        }
        return Component.translatable("gui.servus_storage.field_head.fertilize." + status.name().toLowerCase(Locale.ROOT));
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

    /** Shows the widgets of the current page. */
    private void updateWidgets() {
        Page page = menu.page();
        boolean serving = serving();
        harvestDown.visible = page == Page.FIELD && serving;
        harvestUp.visible = page == Page.FIELD && serving;
        for (Button button : limitButtons) {
            button.visible = page == Page.FIELD && serving;
        }
        boolean fertilizerPage = page == Page.FERTILIZER;
        FertilizeMode mode = menu.fertilizeMode();
        for (int i = 0; i < modeButtons.size(); i++) {
            modeButtons.get(i).visible = fertilizerPage;
            modeButtons.get(i).active = mode.ordinal() != i;
        }
        for (Button button : reserveButtons) {
            button.visible = fertilizerPage;
        }
        for (Button button : belowButtons) {
            button.visible = fertilizerPage;
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
        Page page = menu.page();
        if (page == Page.FERTILIZER && inside(mouseX, mouseY, MARGIN, RESERVE_Y, RESERVE_X[0] - MARGIN, 11)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_storage.field_head.reserve_tooltip"), mouseX, mouseY);
            return;
        }
        if (page == Page.FIELD && serving() && overLevel(mouseX, mouseY, LIMIT_Y)) {
            graphics.setComponentTooltipForNextFrame(font, levelTooltip("limit"), mouseX, mouseY);
            return;
        }
        if (page == Page.FERTILIZER && overLevel(mouseX, mouseY, BELOW_Y)) {
            graphics.setComponentTooltipForNextFrame(font, levelTooltip("below"), mouseX, mouseY);
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
        if (page == Page.WATER && inside(mouseX, mouseY, MARGIN, ANTI_CORROSION_Y, WIDTH, 16)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.servus_storage.field_head.buffer.anti_corrosion_tooltip"),
                    Component.translatable("gui.servus_storage.field_head.buffer.filled_by")), mouseX, mouseY);
        }
        SupplyBuffer.Part part = pickerPart(page);
        if (part != null && menu.supplyLevel(part) > 0 && inside(mouseX, mouseY, BUFFER_X, BUFFER_Y, MARGIN + WIDTH - BUFFER_X, 16)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.servus_storage.field_head.buffer." + part.name().toLowerCase(Locale.ROOT),
                            menu.supplyCount(part), menu.supplyLevel(part)),
                    Component.translatable("gui.servus_storage.field_head.buffer.filled_by")), mouseX, mouseY);
        }
    }

    /** Whether the mouse is over the label, icon or value of the level row at y. */
    private boolean overLevel(int mouseX, int mouseY, int y) {
        return inside(mouseX, mouseY, MARGIN, y - 2, LEVEL_X[0] - MARGIN, 16)
                || inside(mouseX, mouseY, LEVEL_TEXT_X, y, LEVEL_TEXT_W, 11);
    }

    /** What a level does, the produce it counts and the shift hint. */
    private List<Component> levelTooltip(String level) {
        ItemStack produce = menu.produce();
        return List.of(
                Component.translatable("gui.servus_storage.field_head.level." + level + ".title"),
                Component.translatable("gui.servus_storage.field_head.level." + level + ".tooltip"),
                produce.isEmpty() ? Component.translatable("gui.servus_storage.field_head.produce_unknown")
                        : Component.translatable("gui.servus_storage.field_head.produce_stock", produce.getHoverName(), grouped(menu.produceStock())),
                Component.translatable("gui.servus_storage.field_head.level.shift"));
    }

    /** The buffer part shown beside the picker of the page, or null on pages without one. */
    private static SupplyBuffer.Part pickerPart(Page page) {
        return switch (page) {
            case FIELD -> SupplyBuffer.Part.SEEDS;
            case FERTILIZER -> SupplyBuffer.Part.FERTILIZER;
            default -> null;
        };
    }

    /** The item of the buffer part the screen draws. */
    private ItemStack supplyIcon(SupplyBuffer.Part part) {
        return switch (part) {
            case SEEDS -> menu.crop();
            case FERTILIZER -> menu.fertilizer();
            case ANTI_CORROSION -> menu.antiCorrosionShown();
        };
    }

    private String supplyText(SupplyBuffer.Part part) {
        return menu.supplyCount(part) + "/" + menu.supplyLevel(part);
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
        PanelPainter.playerInventory(graphics, leftPos, topPos, FieldHeadMenu.INVENTORY_Y,
                FieldHeadMenu.INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET);
        switch (menu.page()) {
            case FIELD -> {
                cropPicker.renderButton(graphics, font, mouseX, mouseY);
                if (serving()) {
                    renderRipeBar(graphics);
                    renderLevelBackground(graphics, LIMIT_Y);
                }
            }
            case WATER -> {
                renderWaterBar(graphics);
                graphics.fakeItem(supplyIcon(SupplyBuffer.Part.ANTI_CORROSION), leftPos + MARGIN, topPos + ANTI_CORROSION_Y);
            }
            case FERTILIZER -> {
                fertilizerPicker.renderButton(graphics, font, mouseX, mouseY);
                PanelPainter.sunken(graphics, leftPos + RESERVE_TEXT_X, topPos + RESERVE_Y, RESERVE_TEXT_W, 11);
                renderLevelBackground(graphics, BELOW_Y);
            }
            case UPGRADES -> slotFrame(graphics, FieldHeadMenu.RANGE_X, FieldHeadMenu.RANGE_Y);
        }
        SupplyBuffer.Part part = pickerPart(menu.page());
        if (part != null && menu.supplyLevel(part) > 0) {
            graphics.fakeItem(supplyIcon(part), leftPos + BUFFER_X, topPos + BUFFER_Y);
        }
    }

    /** The value frame of a level row and the produce icon after its label. */
    private void renderLevelBackground(GuiGraphicsExtractor graphics, int y) {
        PanelPainter.sunken(graphics, leftPos + LEVEL_TEXT_X, topPos + y, LEVEL_TEXT_W, 11);
        ItemStack produce = menu.produce();
        if (!produce.isEmpty()) {
            graphics.fakeItem(produce, leftPos + LEVEL_ICON_X, topPos + y - 2);
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
        SupplyBuffer.Part part = pickerPart(menu.page());
        if (part != null && menu.supplyLevel(part) > 0) {
            String text = supplyText(part);
            if (BUFFER_TEXT_X + font.width(text) > TEXT_RIGHT) {
                text = String.valueOf(menu.supplyCount(part));
            }
            graphics.text(font, text, BUFFER_TEXT_X, BUFFER_Y + 4, menu.supplyCount(part) > 0 ? COLOUR_TEXT : COLOUR_WARN, false);
        }
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
        renderLevelLabels(graphics, "below", BELOW_Y, menu.fertilizeBelow());
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
        renderLevelLabels(graphics, "limit", LIMIT_Y, menu.harvestLimit());
        ItemStack produce = menu.produce();
        wrapped(graphics, produce.isEmpty() ? Component.translatable("gui.servus_storage.field_head.produce_unknown")
                : Component.translatable("gui.servus_storage.field_head.produce_stock", produce.getHoverName(), grouped(menu.produceStock())),
                PRODUCE_Y, COLOUR_TEXT);
        FieldWork work = menu.work();
        if (work != null) {
            int colour = switch (work) {
                case WORKING, HARVEST_DUE, READY -> COLOUR_DUE;
                default -> work.problem() ? COLOUR_WARN : COLOUR_TEXT;
            };
            wrapped(graphics, workLine(work), WORK_Y, colour);
        }
    }

    /** The label of a level row and its value, or the word for 0, centred in the value frame. */
    private void renderLevelLabels(GuiGraphicsExtractor graphics, String level, int y, int value) {
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.level." + level), MARGIN, y + 2, COLOUR_TEXT, false);
        String shown = value > 0 ? grouped(value)
                : Component.translatable("gui.servus_storage.field_head.level." + level + ".off").getString();
        shown = font.plainSubstrByWidth(shown, LEVEL_TEXT_W - 2);
        graphics.text(font, shown, LEVEL_TEXT_X + (LEVEL_TEXT_W - font.width(shown)) / 2, y + 2, COLOUR_TEXT, false);
    }

    private void renderWaterLabels(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.water", menu.water(), menu.capacity()), MARGIN, WATER_Y, COLOUR_TEXT, false);
        if (serving()) {
            boolean hydrated = menu.hydrated();
            graphics.text(font, Component.translatable(hydrated ? "gui.servus_storage.field_head.hydrated" : "gui.servus_storage.field_head.dry"),
                    MARGIN, HYDRATED_Y, hydrated ? COLOUR_DUE : COLOUR_WARN, false);
        }
        ItemStack antiCorrosion = supplyIcon(SupplyBuffer.Part.ANTI_CORROSION);
        Component antiCorrosionName = antiCorrosion.isEmpty()
                ? Component.translatable("tag.item.servus_storage.anti_corrosion") : antiCorrosion.getHoverName();
        SupplyBuffer.Part antiCorrosionPart = SupplyBuffer.Part.ANTI_CORROSION;
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.buffer.anti_corrosion", antiCorrosionName,
                        menu.supplyCount(antiCorrosionPart), menu.supplyLevel(antiCorrosionPart)),
                ANTI_CORROSION_TEXT_X, ANTI_CORROSION_Y + 4, menu.supplyCount(antiCorrosionPart) > 0 ? COLOUR_TEXT : COLOUR_WARN, false);
        int stage = menu.corrosion();
        if (stage >= FieldCorrosion.COPPER) {
            Component corrosion = stage == FieldCorrosion.CORRODED
                    ? Component.translatable("gui.servus_storage.field_head.corroded")
                    : Component.translatable("gui.servus_storage.field_head.corrosion",
                            Component.translatable("gui.servus_storage.field_head.stage." + FieldCorrosion.name(stage)), menu.corrosionProgress());
            graphics.text(font, corrosion, MARGIN, CORROSION_Y,
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
