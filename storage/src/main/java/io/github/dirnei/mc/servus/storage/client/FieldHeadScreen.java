package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.storage.field.ChooseAntiCorrosionPayload;
import io.github.dirnei.mc.servus.storage.field.ChooseFertilizerPayload;
import io.github.dirnei.mc.servus.storage.field.ChooseFieldCropPayload;
import io.github.dirnei.mc.servus.storage.field.FertilizeMode;
import io.github.dirnei.mc.servus.storage.field.FertilizeStatus;
import io.github.dirnei.mc.servus.storage.field.FieldAmount;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.field.SetFieldAmountPayload;
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
 * mode, threshold, Spreader status), Stock (produce, typed harvest limit,
 * fertilize level and reserve, the head's buffer) and Upgrades (Range Upgrade
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
    private static final int HYDRATED_Y = 70;
    private static final int CORROSION_Y = 84;
    private static final int RADIUS_TEXT_X = 30;
    private static final int SIZE_Y = 44;
    private static final int MODE_Y = 44;
    private static final int MODE_H = 14;
    private static final int[] MODE_X = {MARGIN, 60, 162};
    private static final int[] MODE_W = {50, 100, RIGHT - 162};
    private static final int FERTILIZE_Y = 64;
    private static final int FERTILIZE_STATUS_Y = 82;
    /** The Stock tab: the produce row, one row per typed amount, then the buffer. */
    private static final int PRODUCE_Y = 18;
    private static final int AMOUNT_Y = 40;
    private static final int AMOUNT_STRIDE = 17;
    private static final int AMOUNT_BOX_X = 158;
    private static final int AMOUNT_BOX_W = 40;
    private static final int AMOUNT_BOX_H = 14;
    private static final int AMOUNT_HINT_X = AMOUNT_BOX_X + AMOUNT_BOX_W + 4;
    private static final int BUFFER_TITLE_Y = 109;
    private static final int BUFFER_Y = 119;
    private static final int BUFFER_STRIDE = 84;
    private static final int ICON_TEXT_GAP = 20;
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

    public FieldHeadScreen(FieldHeadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FieldHeadMenu.IMAGE_WIDTH, FieldHeadMenu.IMAGE_HEIGHT);
        this.cropPicker = picker("crop_picker", "crop_no_stock", CropPickerEntries.build(),
                entry -> ClientPacketDistributor.sendToServer(new ChooseFieldCropPayload(idOf(entry))));
        this.fertilizerPicker = picker("fertilizer_picker", "fertilizer_no_stock",
                TagPickerEntries.build(FieldHeadBlockEntity.FERTILIZER, Component.translatable("gui.servus_storage.field_head.fertilizer_none")),
                entry -> ClientPacketDistributor.sendToServer(new ChooseFertilizerPayload(idOf(entry))));
        this.antiCorrosionPicker = picker("anti_corrosion_picker", "anti_corrosion_no_stock",
                TagPickerEntries.build(FieldHeadBlockEntity.ANTI_CORROSION, Component.translatable("gui.servus_storage.field_head.anti_corrosion_none")),
                entry -> ClientPacketDistributor.sendToServer(new ChooseAntiCorrosionPayload(idOf(entry))));
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
        antiCorrosionPicker.setSelected(pickerId(menu.antiCorrosion()));
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
            String key = "gui.servus_storage.field_head.fertilize_mode." + mode.name().toLowerCase(Locale.ROOT);
            int index = mode.ordinal();
            modeButtons.add(addRenderableWidget(Button.builder(Component.translatable(key), button -> press(FieldHeadMenu.BUTTON_FERTILIZE_MODE + index))
                    .bounds(leftPos + MODE_X[index], topPos + MODE_Y, MODE_W[index], MODE_H)
                    .tooltip(Tooltip.create(Component.translatable(key + ".tooltip"))).build()));
        }
        fertilizeDown = stepButton("-", FERTILIZE_Y - 2, STEP_DOWN_X, FieldHeadMenu.BUTTON_FERTILIZE_DOWN);
        fertilizeUp = stepButton("+", FERTILIZE_Y - 2, STEP_UP_X, FieldHeadMenu.BUTTON_FERTILIZE_UP);
        amountFields.clear();
        for (FieldAmount amount : FieldAmount.values()) {
            AmountField field = new AmountField(font, leftPos + AMOUNT_BOX_X, topPos + amountY(amount), AMOUNT_BOX_W, AMOUNT_BOX_H, amount,
                    Component.translatable(amountKey(amount)), () -> headAmount(amount),
                    value -> ClientPacketDistributor.sendToServer(new SetFieldAmountPayload(menu.headPos(), amount.ordinal(), value)));
            amountFields.add(field);
            addRenderableWidget(field.box());
        }
    }

    private static int amountY(FieldAmount amount) {
        return AMOUNT_Y + amount.ordinal() * AMOUNT_STRIDE;
    }

    private static String amountKey(FieldAmount amount) {
        return "gui.servus_storage.field_head.amount." + amount.name().toLowerCase(Locale.ROOT);
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
            case STOCK -> new ItemStack(Items.CHEST);
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
            lines.add(Component.translatable("gui.servus_storage.field_head.tab." + hovered.name().toLowerCase(Locale.ROOT)));
            Component problem = problem(hovered);
            if (problem != null) {
                lines.add(problem.copy().withColor(COLOUR_PROBLEM & 0xFFFFFF));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        List<Component> lines = switch (menu.page()) {
            case FIELD -> serving() && inside(mouseX, mouseY, RIPE_X, RIPE_Y, RIPE_W, RIPE_H)
                    ? List.of(Component.translatable("gui.servus_storage.field_head.ripe_tooltip",
                            menu.ripe(), menu.planted(), percent(menu.ripe(), menu.planted()), menu.threshold()))
                    : List.of();
            case WATER -> inside(mouseX, mouseY, MARGIN, BAR_Y, WIDTH, BAR_H)
                    ? List.of(Component.translatable("gui.servus_storage.field_head.water", menu.water(), menu.capacity()),
                            Component.translatable(menu.hydrated() ? "gui.servus_storage.field_head.hydrated" : "gui.servus_storage.field_head.dry"))
                    : List.of();
            case STOCK -> stockTooltip(mouseX, mouseY);
            case UPGRADES -> emptySlotHovered(FieldHeadMenu.RANGE_SLOT, mouseX, mouseY)
                    ? List.of(Component.translatable("gui.servus_storage.field_head.range_slot", menu.radius()))
                    : List.of();
            default -> List.of();
        };
        if (!lines.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    /** What an amount's label or a buffer part under the mouse means; empty elsewhere. */
    private List<Component> stockTooltip(int mouseX, int mouseY) {
        for (FieldAmount amount : FieldAmount.values()) {
            if (inside(mouseX, mouseY, MARGIN, amountY(amount), AMOUNT_BOX_X - MARGIN, AMOUNT_BOX_H)) {
                return amountTooltip(amount);
            }
        }
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            if (inside(mouseX, mouseY, bufferX(part), BUFFER_Y, BUFFER_STRIDE - 4, 16)) {
                return bufferTooltip(part);
            }
        }
        return List.of();
    }

    /** What the amount does, the produce a level counts, and how to type it. */
    private List<Component> amountTooltip(FieldAmount amount) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(amountKey(amount)));
        lines.add(Component.translatable(amountKey(amount) + ".tooltip"));
        if (amount.level() != null) {
            ItemStack produce = menu.produce();
            lines.add(produce.isEmpty() ? Component.translatable("gui.servus_storage.field_head.produce_unknown")
                    : Component.translatable("gui.servus_storage.field_head.produce_stock", produce.getHoverName(), grouped(menu.produceStock())));
        }
        lines.add(Component.translatable("gui.servus_storage.field_head.amount.typing"));
        return lines;
    }

    /** What a buffer part holds and how it is filled. */
    private List<Component> bufferTooltip(SupplyBuffer.Part part) {
        List<Component> lines = new ArrayList<>();
        String key = "gui.servus_storage.field_head.buffer." + part.name().toLowerCase(Locale.ROOT);
        if (part == SupplyBuffer.Part.ANTI_CORROSION) {
            lines.add(Component.translatable(key, antiCorrosionName(), menu.supplyCount(part), menu.supplyLevel(part)));
            lines.add(Component.translatable("gui.servus_storage.field_head.buffer.anti_corrosion_tooltip"));
        } else {
            lines.add(Component.translatable(key, menu.supplyCount(part), menu.supplyLevel(part)));
        }
        lines.add(Component.translatable("gui.servus_storage.field_head.buffer.filled_by"));
        return lines;
    }

    private Component antiCorrosionName() {
        ItemStack antiCorrosion = menu.antiCorrosion();
        return antiCorrosion.isEmpty() ? Component.translatable("tag.item.servus_storage.anti_corrosion") : antiCorrosion.getHoverName();
    }

    private static int bufferX(SupplyBuffer.Part part) {
        return MARGIN + part.ordinal() * BUFFER_STRIDE;
    }

    /** The item of the buffer part the screen draws: the head's crop or fertilizer, else a typical one. */
    private ItemStack supplyIcon(SupplyBuffer.Part part) {
        ItemStack stack = switch (part) {
            case SEEDS -> menu.crop();
            case FERTILIZER -> menu.fertilizer();
            case ANTI_CORROSION -> menu.antiCorrosion();
        };
        if (!stack.isEmpty()) {
            return stack;
        }
        return new ItemStack(switch (part) {
            case SEEDS -> Items.WHEAT_SEEDS;
            case FERTILIZER -> Items.BONE_MEAL;
            case ANTI_CORROSION -> Items.HONEYCOMB;
        });
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

    /** The produce icon and one icon per buffer part. */
    private void renderStockIcons(GuiGraphicsExtractor graphics) {
        ItemStack produce = menu.produce();
        if (!produce.isEmpty()) {
            graphics.fakeItem(produce, leftPos + MARGIN, topPos + PRODUCE_Y);
        }
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            graphics.fakeItem(supplyIcon(part), leftPos + bufferX(part), topPos + BUFFER_Y);
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
            case STOCK -> renderStockLabels(graphics);
            case UPGRADES -> renderUpgradeLabels(graphics);
        }
    }

    private void renderFertilizerLabels(GuiGraphicsExtractor graphics) {
        if (menu.fertilizeMode() == FertilizeMode.UNTIL_RIPE) {
            graphics.text(font, Component.translatable("gui.servus_storage.field_head.fertilize_at", menu.fertilizePercent()),
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

    /** The produce and its stock, the label and hint of each typed amount, and the buffer counts. */
    private void renderStockLabels(GuiGraphicsExtractor graphics) {
        ItemStack produce = menu.produce();
        Component produceLine = produce.isEmpty() ? Component.translatable("gui.servus_storage.field_head.produce_unknown")
                : Component.translatable("gui.servus_storage.field_head.produce_stock", produce.getHoverName(), grouped(menu.produceStock()));
        int produceX = produce.isEmpty() ? MARGIN : MARGIN + ICON_TEXT_GAP;
        clipped(graphics, produceLine, produceX, PRODUCE_Y + 4, RIGHT - produceX, COLOUR_TEXT);
        for (FieldAmount amount : FieldAmount.values()) {
            int y = amountY(amount) + 3;
            clipped(graphics, Component.translatable(amountKey(amount)), MARGIN, y, AMOUNT_BOX_X - MARGIN - 4, COLOUR_TEXT);
            Component hint = amountHint(amount);
            if (hint != null) {
                clipped(graphics, hint, AMOUNT_HINT_X, y, RIGHT - AMOUNT_HINT_X, COLOUR_HINT);
            }
        }
        graphics.text(font, Component.translatable("gui.servus_storage.field_head.buffer.title"), MARGIN, BUFFER_TITLE_Y, COLOUR_TEXT, false);
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            int level = menu.supplyLevel(part);
            int count = menu.supplyCount(part);
            String text = level > 0 ? count + " / " + level
                    : Component.translatable("gui.servus_storage.field_head.buffer.off").getString();
            int colour = level <= 0 ? COLOUR_HINT : count > 0 ? COLOUR_TEXT : COLOUR_WARN;
            graphics.text(font, text, bufferX(part) + ICON_TEXT_GAP, BUFFER_Y + 4, colour, false);
        }
    }

    /** The grey words right of an amount's field: what 0 means for a level, the item a reserve keeps. */
    private Component amountHint(FieldAmount amount) {
        ItemStack kept = switch (amount) {
            case RESERVE -> menu.fertilizer();
            case ANTI_CORROSION_RESERVE -> menu.antiCorrosion();
            default -> null;
        };
        if (kept == null) {
            return Component.translatable(amountKey(amount) + ".hint");
        }
        return kept.isEmpty() ? null : kept.getHoverName();
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
