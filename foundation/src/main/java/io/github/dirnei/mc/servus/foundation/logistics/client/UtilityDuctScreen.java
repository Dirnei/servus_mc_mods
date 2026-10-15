package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetDuctFilterPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The endpoint GUI of one duct side: one tab per present line on the left
 * edge, and the page of the selected line with its mode, channels and remove
 * button; the Item Pipe page adds the filter slots and the priority. The
 * Power Cable and Fluid Pipe pages have no filter or priority; the data page
 * only switches the side on or off and removes the line. A left click with an
 * empty cursor on a filter slot opens a search popup over every item for that slot.
 */
public class UtilityDuctScreen extends AbstractContainerScreen<UtilityDuctMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "textures/gui/utility_duct.png");
    private static final int MARGIN = 8;
    private static final int MODE_Y = 18;
    private static final int MODE_HEIGHT = 20;
    private static final int CHANNEL_Y = 44;
    private static final int CHANNEL_SIZE = 16;
    private static final int CHANNEL_STRIDE = 20;
    private static final int FILTER_LABEL_Y = 62;
    private static final int PRIORITY_Y = 94;
    private static final int REMOVE_WIDTH = 16;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_TOP = 10;
    private static final int TAB_STRIDE = 28;
    private static final int TEXT = 0xFF404040;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF373737;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;

    private Button modeButton;
    private Line shownPage;
    private int linesSeen = -1;
    private final ItemPicker filterPicker;
    private boolean filterPickerFilled;
    /** The filter slot the open popup sets. */
    private int pickerSlot;

    public UtilityDuctScreen(UtilityDuctMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
        this.filterPicker = new ItemPicker(Component.translatable("gui.servus_foundation.filter_picker"),
                entry -> sendFilter(pickerSlot, entry.itemId()));
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = MARGIN;
        this.inventoryLabelY = UtilityDuctMenu.INVENTORY_Y - 11;
        filterPicker.setArea(leftPos, topPos, imageWidth, imageHeight);
        rebuildButtons();
    }

    /** Asks the server to set a filter slot to the item with that id; empty clears it. */
    public void sendFilter(int slot, Optional<Identifier> item) {
        ClientPacketDistributor.sendToServer(new SetDuctFilterPayload(slot, item));
    }

    /** Whether the filter slots show, that is the Item Pipe page is selected. */
    public boolean showsFilter() {
        return shownPage == Line.ITEM;
    }

    public boolean isPickerOpen() {
        return filterPicker.isOpen();
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

    /** A left click with an empty cursor on a filter slot opens the popup and sends nothing. */
    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (slot != null && slot.index < UtilityDuctBlockEntity.FILTER_SLOTS && shownPage == Line.ITEM
                && containerInput == ContainerInput.PICKUP && buttonNum == 0 && menu.getCarried().isEmpty()) {
            openFilterPicker(slot.index);
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
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
        linesSeen = menu.duct().linesMask();
        List<Line> tabs = tabs();
        if (!tabs.contains(menu.page())) {
            menu.setPage(tabs.isEmpty() ? null : tabs.get(0));
        }
        shownPage = menu.page();
        Line line = shownPage;
        if (line == null) {
            return;
        }
        modeButton = Button.builder(Component.empty(), button -> press(UtilityDuctMenu.BUTTON_CYCLE_MODE + line.ordinal()))
                .bounds(leftPos + MARGIN, topPos + MODE_Y, imageWidth - 2 * MARGIN, MODE_HEIGHT).build();
        addRenderableWidget(modeButton);
        for (int channel = 0; line != Line.DATA && channel < DuctChannels.COUNT; channel++) {
            int id = UtilityDuctMenu.BUTTON_CHANNEL + line.ordinal() * DuctChannels.COUNT + channel;
            int current = channel;
            addRenderableWidget(new ChannelButton(leftPos + MARGIN + channel * CHANNEL_STRIDE, topPos + CHANNEL_Y, CHANNEL_SIZE, channel,
                    () -> menu.duct().channel(line, menu.side()) == current, () -> press(id)));
        }
        int removeY = line == Line.ITEM ? PRIORITY_Y : line == Line.DATA ? CHANNEL_Y : CHANNEL_Y + CHANNEL_SIZE + 10;
        Button remove = Button.builder(Component.literal("x"), button -> press(UtilityDuctMenu.BUTTON_REMOVE_LINE + line.ordinal()))
                .bounds(leftPos + imageWidth - MARGIN - REMOVE_WIDTH, topPos + removeY, REMOVE_WIDTH, 16).build();
        remove.setTooltip(Tooltip.create(Component.translatable("gui.servus_foundation.remove." + line.id())));
        addRenderableWidget(remove);
        if (line == Line.ITEM) {
            addRenderableWidget(Button.builder(Component.literal("-"), button -> press(UtilityDuctMenu.BUTTON_PRIORITY_DOWN))
                    .bounds(leftPos + MARGIN, topPos + PRIORITY_Y, 16, 16).build());
            addRenderableWidget(Button.builder(Component.literal("+"), button -> press(UtilityDuctMenu.BUTTON_PRIORITY_UP))
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
        if (linesSeen != menu.duct().linesMask() || shownPage != menu.page()) {
            rebuildButtons();
        }
        if (modeButton != null && shownPage != null) {
            SideMode mode = menu.duct().mode(shownPage, menu.side());
            String key = shownPage == Line.DATA
                    ? (mode == SideMode.DISABLED ? "gui.servus_foundation.data.off" : "gui.servus_foundation.data.on")
                    : "gui.servus_foundation.mode." + mode.name().toLowerCase(Locale.ROOT);
            modeButton.setMessage(Component.translatable(key));
        }
        if (filterPicker.isOpen() && shownPage != Line.ITEM) {
            filterPicker.close();
        }
        if (filterPicker.isOpen()) {
            super.extractRenderState(guiGraphics, -1, -1, partialTick);
            filterPicker.renderPopup(guiGraphics, font, mouseX, mouseY, partialTick);
            filterPicker.renderTooltip(guiGraphics, font, mouseX, mouseY);
            return;
        }
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        Line hovered = tabAt(mouseX, mouseY);
        if (hovered != null) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_foundation.line." + hovered.id()), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderTabs(guiGraphics);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (shownPage == Line.ITEM) {
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
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        if (shownPage == Line.ITEM) {
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.filter"), MARGIN, FILTER_LABEL_Y, TEXT, false);
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.priority", menu.priority()), MARGIN + 20, PRIORITY_Y + 4, TEXT, false);
        }
        if (shownPage == null) {
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.no_lines"), MARGIN, MODE_Y + 5, TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (filterPicker.isOpen()) {
            return filterPicker.mouseClicked(event, doubleClick);
        }
        Line tab = tabAt(event.x(), event.y());
        if (tab != null) {
            menu.setPage(tab);
            rebuildButtons();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return filterPicker.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return filterPicker.mouseReleased() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return filterPicker.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** While the popup is open every key goes to it, so typing E does not close the GUI and Escape closes only the popup. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return filterPicker.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return filterPicker.charTyped(event) || super.charTyped(event);
    }
}
