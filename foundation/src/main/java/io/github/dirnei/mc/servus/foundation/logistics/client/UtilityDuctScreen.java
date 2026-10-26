package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.logistics.duct.FluidFilter;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.menu.ClientCableChannels;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetDuctFilterPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetDuctFluidFilterPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The endpoint GUI of a duct: a side picker row on top, the selected side's
 * neighbour name, one tab per present line on the left edge, and the page of
 * the selected line with its mode, Control, channel and remove button; the
 * Item Pipe page adds the filter slots and the priority, the Fluid Pipe page
 * the fluid filter slots and the fluid priority. The Power Cable page has no
 * filter or priority; the data page only switches the side on or off and
 * removes the line. A left click with an empty cursor on a filter slot opens a
 * search popup over every item, or every fluid, for that slot; a fluid slot
 * clicked with a fluid container takes its fluid and leaves the container as
 * it is. The channel button opens the {@link ChannelPopup}.
 */
public class UtilityDuctScreen extends AbstractContainerScreen<UtilityDuctMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "textures/gui/utility_duct.png");
    private static final int MARGIN = 8;
    private static final int PICKER_Y = 16;
    private static final int SIDE_NAME_Y = 39;
    private static final int MODE_Y = 52;
    private static final int MODE_HEIGHT = 20;
    private static final int HALF_GAP = 4;
    private static final int CHANNEL_Y = 76;
    private static final int FILTER_LABEL_Y = 96;
    private static final int PRIORITY_Y = 128;
    private static final int REMOVE_WIDTH = 16;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_TOP = 50;
    private static final int TAB_STRIDE = 28;
    private static final int TEXT = 0xFF404040;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF373737;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;

    private Button modeButton;
    private Button controlButton;
    private Line shownPage;
    private Direction shownSide;
    private final DuctSidePicker sidePicker;
    private int linesSeen = -1;
    private final ItemPicker filterPicker;
    private final ItemPicker fluidPicker;
    private final FluidFilterRow fluidRow;
    private final ChannelPopup channelPopup;
    private boolean filterPickerFilled;
    private boolean fluidPickerFilled;
    /** The filter slot the open popup sets. */
    private int pickerSlot;

    public UtilityDuctScreen(UtilityDuctMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 238);
        this.sidePicker = new DuctSidePicker(menu.duct());
        this.filterPicker = new ItemPicker(Component.translatable("gui.servus_foundation.filter_picker"),
                entry -> sendFilter(pickerSlot, entry.itemId()));
        this.fluidPicker = new ItemPicker(Component.translatable("gui.servus_foundation.fluid_filter_picker"),
                entry -> sendFluidFilter(pickerSlot, entry.fluidId()));
        this.fluidRow = new FluidFilterRow(() -> menu.duct().fluidFilter(menu.side()));
        this.channelPopup = new ChannelPopup(menu);
        ClientCableChannels.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = MARGIN;
        this.inventoryLabelY = UtilityDuctMenu.INVENTORY_Y - 11;
        filterPicker.setArea(leftPos, topPos, imageWidth, imageHeight);
        fluidPicker.setArea(leftPos, topPos, imageWidth, imageHeight);
        fluidRow.place(leftPos + UtilityDuctMenu.SLOT_X, topPos + UtilityDuctMenu.FILTER_Y);
        channelPopup.setArea(leftPos, topPos, imageWidth, imageHeight);
        sidePicker.place(leftPos + (imageWidth - DuctSidePicker.width()) / 2, topPos + PICKER_Y);
        rebuildButtons();
    }

    /** Asks the server to set a filter slot to the item with that id; empty clears it. */
    public void sendFilter(int slot, Optional<Identifier> item) {
        ClientPacketDistributor.sendToServer(new SetDuctFilterPayload(slot, item));
    }

    /** Asks the server to set a fluid filter slot to the fluid with that id; empty clears it. */
    public void sendFluidFilter(int slot, Optional<Identifier> fluid) {
        ClientPacketDistributor.sendToServer(new SetDuctFluidFilterPayload(slot, fluid));
    }

    private void sendFluidFilter(int slot, Fluid fluid) {
        sendFluidFilter(slot, Optional.of(BuiltInRegistries.FLUID.getKey(fluid)));
    }

    /** Whether the filter slots show, that is the Item Pipe page is selected. */
    public boolean showsFilter() {
        return shownPage == Line.ITEM;
    }

    /** Whether the fluid filter slots show, that is the Fluid Pipe page is selected. */
    public boolean showsFluidFilter() {
        return shownPage == Line.FLUID;
    }

    /** Whether a popup covers the GUI: a filter picker or the channel popup. */
    public boolean isPickerOpen() {
        return filterPicker.isOpen() || fluidPicker.isOpen() || channelPopup.isOpen();
    }

    /** The 16 by 16 item area of a filter slot, absolute screen coordinates. */
    public Rect2i filterSlotArea(int slot) {
        return new Rect2i(leftPos + UtilityDuctMenu.SLOT_X + slot * 18, topPos + UtilityDuctMenu.FILTER_Y, 16, 16);
    }

    private void openFilterPicker(int slot) {
        if (!filterPickerFilled) {
            filterPicker.setEntries(FilterPickerEntries.get());
            filterPickerFilled = true;
        }
        pickerSlot = slot;
        filterPicker.open(ItemPicker.Entry.idOf(menu.getSlot(slot).getItem()));
    }

    private void openFluidPicker(int slot) {
        if (!fluidPickerFilled) {
            fluidPicker.setEntries(FilterPickerEntries.fluids());
            fluidPickerFilled = true;
        }
        pickerSlot = slot;
        String current = menu.duct().fluidFilter(menu.side()).get(slot);
        fluidPicker.open(current == null ? PickerList.NONE_ID : current);
    }

    /**
     * A left click with an empty cursor on a filter slot opens the popup and sends nothing. On the Fluid Pipe
     * page a shift click on an inventory item sets the first free fluid slot to its fluid and moves nothing.
     */
    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (slot != null && slot.index < UtilityDuctBlockEntity.FILTER_SLOTS && shownPage == Line.ITEM
                && containerInput == ContainerInput.PICKUP && buttonNum == 0 && menu.getCarried().isEmpty()) {
            openFilterPicker(slot.index);
            return;
        }
        if (slot != null && shownPage == Line.FLUID && containerInput == ContainerInput.QUICK_MOVE) {
            int free = menu.duct().fluidFilter(menu.side()).firstFree();
            Optional<Fluid> fluid = Transfer.fluidOf(slot.getItem());
            if (free != FluidFilter.NONE && fluid.isPresent()) {
                sendFluidFilter(free, fluid.get());
            }
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
    }

    /** Right click clears a fluid slot; a left click takes the carried container's fluid, or opens the picker with an empty cursor. */
    private void clickFluidSlot(int slot, int button) {
        if (button == 1) {
            sendFluidFilter(slot, Optional.empty());
        } else if (button == 0 && menu.getCarried().isEmpty()) {
            openFluidPicker(slot);
        } else if (button == 0) {
            Transfer.fluidOf(menu.getCarried()).ifPresent(fluid -> sendFluidFilter(slot, fluid));
        }
    }

    /** The present lines in tab order. */
    private List<Line> tabs() {
        List<Line> lines = new ArrayList<>();
        for (Line line : Line.VALUES) {
            if (menu.duct().has(line)) {
                lines.add(line);
            }
        }
        return lines;
    }

    private void rebuildButtons() {
        clearWidgets();
        modeButton = null;
        controlButton = null;
        linesSeen = menu.duct().linesMask();
        shownSide = menu.side();
        List<Line> tabs = tabs();
        if (!tabs.contains(menu.page())) {
            menu.setPage(tabs.isEmpty() ? null : tabs.get(0));
        }
        shownPage = menu.page();
        Line line = shownPage;
        if (line == null) {
            return;
        }
        int fullWidth = imageWidth - 2 * MARGIN;
        int halfWidth = (fullWidth - HALF_GAP) / 2;
        modeButton = Button.builder(Component.empty(), button -> press(UtilityDuctMenu.BUTTON_CYCLE_MODE + line.ordinal()))
                .bounds(leftPos + MARGIN, topPos + MODE_Y, line == Line.DATA ? fullWidth : halfWidth, MODE_HEIGHT).build();
        addRenderableWidget(modeButton);
        if (line != Line.DATA) {
            controlButton = Button.builder(Component.empty(), button -> press(UtilityDuctMenu.BUTTON_CYCLE_CONTROL + line.ordinal()))
                    .bounds(leftPos + imageWidth - MARGIN - halfWidth, topPos + MODE_Y, halfWidth, MODE_HEIGHT).build();
            controlButton.setTooltip(Tooltip.create(Component.translatable("gui.servus_foundation.control.tooltip")));
            addRenderableWidget(controlButton);
            addRenderableWidget(new ChannelButton(leftPos + MARGIN, topPos + CHANNEL_Y, imageWidth - 2 * MARGIN,
                    () -> menu.duct().channel(line, menu.side()), () -> channelPopup.open(line)));
        }
        int removeY = line == Line.ITEM || line == Line.FLUID ? PRIORITY_Y : line == Line.DATA ? CHANNEL_Y : CHANNEL_Y + ChannelButton.HEIGHT + 8;
        Button remove = Button.builder(Component.literal("x"), button -> press(UtilityDuctMenu.BUTTON_REMOVE_LINE + line.ordinal()))
                .bounds(leftPos + imageWidth - MARGIN - REMOVE_WIDTH, topPos + removeY, REMOVE_WIDTH, 16).build();
        remove.setTooltip(Tooltip.create(Component.translatable("gui.servus_foundation.remove." + line.id())));
        addRenderableWidget(remove);
        if (line == Line.ITEM || line == Line.FLUID) {
            int down = line == Line.ITEM ? UtilityDuctMenu.BUTTON_PRIORITY_DOWN : UtilityDuctMenu.BUTTON_FLUID_PRIORITY_DOWN;
            int up = line == Line.ITEM ? UtilityDuctMenu.BUTTON_PRIORITY_UP : UtilityDuctMenu.BUTTON_FLUID_PRIORITY_UP;
            addRenderableWidget(Button.builder(Component.literal("-"), button -> press(down))
                    .bounds(leftPos + MARGIN, topPos + PRIORITY_Y, 16, 16).build());
            addRenderableWidget(Button.builder(Component.literal("+"), button -> press(up))
                    .bounds(leftPos + MARGIN + 76, topPos + PRIORITY_Y, 16, 16).build());
        }
    }

    private void press(int id) {
        if (MenuButtons.press(minecraft, menu, id)) {
            menu.clickMenuButton(minecraft.player, id);
        }
    }

    private int tabX() {
        return leftPos - TAB_WIDTH;
    }

    private int tabY(int index) {
        return topPos + TAB_TOP + index * TAB_STRIDE;
    }

    private Line tabAt(double mouseX, double mouseY) {
        List<Line> tabs = tabs();
        for (int i = 0; i < tabs.size(); i++) {
            int x = tabX();
            int y = tabY(i);
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
                return tabs.get(i);
            }
        }
        return null;
    }

    private void renderTabs(GuiGraphicsExtractor graphics) {
        List<Line> tabs = tabs();
        for (int i = 0; i < tabs.size(); i++) {
            Line line = tabs.get(i);
            boolean selected = line == shownPage;
            int x = tabX();
            int y = tabY(i);
            int width = selected ? TAB_WIDTH + 2 : TAB_WIDTH;
            graphics.fill(x, y, x + width, y + TAB_HEIGHT, PANEL_DARK);
            graphics.fill(x + 1, y + 1, x + width, y + TAB_HEIGHT - 1, selected ? PANEL : 0xFF9A9A9A);
            graphics.fill(x + 1, y + 1, x + width, y + 2, PANEL_LIGHT);
            graphics.fakeItem(new ItemStack(menu.duct().lineItem(line)), x + 6, y + 5);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (linesSeen != menu.duct().linesMask() || shownPage != menu.page() || shownSide != menu.side()) {
            rebuildButtons();
        }
        if (modeButton != null && shownPage != null) {
            SideMode mode = menu.duct().mode(shownPage, menu.side());
            String key = shownPage == Line.DATA
                    ? (mode == SideMode.DISABLED ? "gui.servus_foundation.data.off" : "gui.servus_foundation.data.on")
                    : "gui.servus_foundation.mode." + mode.name().toLowerCase(Locale.ROOT);
            modeButton.setMessage(Component.translatable(key));
        }
        if (controlButton != null && shownPage != null) {
            controlButton.setMessage(Component.translatable("gui.servus_foundation.control",
                    Component.translatable("gui.servus_foundation.control." + menu.duct().control(shownPage, menu.side()).key())));
        }
        if (filterPicker.isOpen() && shownPage != Line.ITEM) {
            filterPicker.close();
        }
        if (fluidPicker.isOpen() && shownPage != Line.FLUID) {
            fluidPicker.close();
        }
        if (channelPopup.isOpen() && channelPopup.line() != shownPage) {
            channelPopup.close();
        }
        if (channelPopup.isOpen()) {
            super.extractRenderState(guiGraphics, -1, -1, partialTick);
            channelPopup.render(guiGraphics, font, mouseX, mouseY, partialTick);
            return;
        }
        for (ItemPicker picker : List.of(filterPicker, fluidPicker)) {
            if (picker.isOpen()) {
                super.extractRenderState(guiGraphics, -1, -1, partialTick);
                picker.renderPopup(guiGraphics, font, mouseX, mouseY, partialTick);
                picker.renderTooltip(guiGraphics, font, mouseX, mouseY);
                return;
            }
        }
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        Line hovered = tabAt(mouseX, mouseY);
        if (hovered != null) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_foundation.line." + hovered.id()), mouseX, mouseY);
        } else if (!sidePicker.renderTooltip(guiGraphics, font, mouseX, mouseY) && shownPage == Line.FLUID) {
            fluidRow.renderTooltip(guiGraphics, font, mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderTabs(guiGraphics);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        sidePicker.render(guiGraphics, menu.side(), mouseX, mouseY);
        if (shownPage == Line.ITEM || shownPage == Line.FLUID) {
            for (int i = 0; i < 9; i++) {
                int x = leftPos + UtilityDuctMenu.SLOT_X + i * 18 - 1;
                int y = topPos + UtilityDuctMenu.FILTER_Y - 1;
                guiGraphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
                guiGraphics.fill(x, y, x + 18, y + 1, PANEL_DARK);
                guiGraphics.fill(x, y, x + 1, y + 18, PANEL_DARK);
                guiGraphics.fill(x, y + 17, x + 18, y + 18, PANEL_LIGHT);
                guiGraphics.fill(x + 17, y, x + 18, y + 18, PANEL_LIGHT);
            }
        }
        if (shownPage == Line.FLUID) {
            fluidRow.render(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        Component sideName = Component.translatable("gui.servus_foundation.utility_duct.side",
                DuctSidePicker.sideName(menu.side()), menu.duct().touchedBlockName(menu.side()));
        guiGraphics.text(font, font.plainSubstrByWidth(sideName.getString(), imageWidth - 2 * MARGIN), MARGIN, SIDE_NAME_Y, TEXT, false);
        if (shownPage == Line.ITEM) {
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.filter"), MARGIN, FILTER_LABEL_Y, TEXT, false);
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.priority", menu.priority()), MARGIN + 20, PRIORITY_Y + 4, TEXT, false);
        }
        if (shownPage == Line.FLUID) {
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.fluid_filter"), MARGIN, FILTER_LABEL_Y, TEXT, false);
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.priority", menu.fluidPriority()), MARGIN + 20, PRIORITY_Y + 4, TEXT, false);
        }
        if (shownPage == null) {
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.no_lines"), MARGIN, MODE_Y + 5, TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (channelPopup.isOpen()) {
            return channelPopup.mouseClicked(event, doubleClick);
        }
        if (filterPicker.isOpen()) {
            return filterPicker.mouseClicked(event, doubleClick);
        }
        if (fluidPicker.isOpen()) {
            return fluidPicker.mouseClicked(event, doubleClick);
        }
        int fluidSlot = shownPage == Line.FLUID ? fluidRow.slotAt(event.x(), event.y()) : FluidFilter.NONE;
        if (fluidSlot != FluidFilter.NONE) {
            clickFluidSlot(fluidSlot, event.button());
            return true;
        }
        Line tab = tabAt(event.x(), event.y());
        if (tab != null) {
            menu.setPage(tab);
            rebuildButtons();
            return true;
        }
        Direction picked = sidePicker.sideAt(event.x(), event.y());
        if (picked != null) {
            if (picked != menu.side() && menu.duct().isSelectable(picked)) {
                press(UtilityDuctMenu.BUTTON_SELECT_SIDE + picked.ordinal());
                rebuildButtons();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return channelPopup.mouseDragged(event) || filterPicker.mouseDragged(event.x(), event.y())
                || fluidPicker.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return channelPopup.mouseReleased() || filterPicker.mouseReleased() || fluidPicker.mouseReleased() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return channelPopup.mouseScrolled(scrollY) || filterPicker.mouseScrolled(scrollY) || fluidPicker.mouseScrolled(scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** While the popup is open every key goes to it, so typing E does not close the GUI and Escape closes only the popup. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return channelPopup.keyPressed(event) || filterPicker.keyPressed(event) || fluidPicker.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return channelPopup.charTyped(event) || filterPicker.charTyped(event) || fluidPicker.charTyped(event) || super.charTyped(event);
    }
}
