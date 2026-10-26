package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctRedstone;
import io.github.dirnei.mc.servus.foundation.logistics.duct.FluidFilter;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.menu.ClientCableChannels;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetDuctFilterAmountPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetDuctFilterPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetDuctFluidFilterPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import io.github.dirnei.mc.servus.foundation.picker.PickerStock;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The endpoint GUI of a duct: side tabs on top, the selected side's neighbour
 * name, one tab per present line on the left edge, and the page of the
 * selected line: a row of small icon buttons (In, Out, Pull on the Item Pipe,
 * the redstone setting and On/Off), the channel button, and a trash can in the
 * bottom right corner that removes the line. The Item Pipe page adds the
 * filter slots and the priority, the Fluid Pipe page the fluid filter slots and
 * the fluid priority. The Power Cable page has no filter or priority; the data
 * page only connects or disconnects the side and has the trash can. Every icon
 * button explains itself in its tooltip. A left click with an empty cursor on a filter slot opens a
 * search popup over every item, or every fluid, for that slot; a fluid slot
 * clicked with a fluid container takes its fluid and leaves the container as
 * it is. Right click on a set filter slot opens the {@link AmountPopup}
 * for its keep or fill up to amount, shown in the slot's corner; shift right
 * click clears the slot. The channel
 * button opens the {@link ChannelPopup}.
 */
public class UtilityDuctScreen extends AbstractContainerScreen<UtilityDuctMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "textures/gui/utility_duct.png");
    private static final int MARGIN = 8;
    private static final int SIDE_NAME_Y = 6;
    private static final int ROW_Y = 17;
    private static final int ICON_SIZE = 20;
    private static final int IN_X = 8;
    private static final int OUT_X = 29;
    private static final int PULL_X = 50;
    private static final int REDSTONE_X = 80;
    private static final int SWITCH_X = 101;
    private static final int SWITCH_WIDTH = 28;
    private static final int CHANNEL_Y = 41;
    private static final int FILTER_LABEL_Y = 63;
    private static final int PRIORITY_Y = 93;
    private static final int TRASH_SIZE = 12;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_TOP = 17;
    private static final int TAB_STRIDE = 28;
    private static final int TEXT = 0xFF404040;
    private static final int HINT = 0x808080;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF373737;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final ItemStack IN_ICON = new ItemStack(Items.HOPPER);
    private static final ItemStack OUT_ICON = new ItemStack(Items.DISPENSER);
    private static final ItemStack PULL_ICON = new ItemStack(Items.STICKY_PISTON);
    private static final ItemStack IGNORE_ICON = new ItemStack(Items.GUNPOWDER);
    private static final ItemStack POWERED_ICON = new ItemStack(Items.REDSTONE_TORCH);
    private static final Identifier UNPOWERED_ICON = Identifier.withDefaultNamespace("textures/block/redstone_torch_off.png");

    private final List<DuctIconButton> iconButtons = new ArrayList<>();
    private Line shownPage;
    private Direction shownSide;
    private final DuctSideTabs sideTabs;
    private int linesSeen = -1;
    private final ItemPicker filterPicker;
    private final ItemPicker fluidPicker;
    private final FluidFilterRow fluidRow;
    private final ChannelPopup channelPopup;
    private final AmountPopup amountPopup;
    /** The line and slot the open amount popup sets. */
    private Line amountLine;
    private int amountSlot;
    private boolean filterPickerFilled;
    private boolean fluidPickerFilled;
    /** The filter slot the open popup sets. */
    private int pickerSlot;

    public UtilityDuctScreen(UtilityDuctMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
        this.sideTabs = new DuctSideTabs(menu.duct());
        this.filterPicker = new ItemPicker(Component.translatable("gui.servus_foundation.filter_picker"),
                entry -> sendFilter(pickerSlot, entry.itemId()));
        this.fluidPicker = new ItemPicker(Component.translatable("gui.servus_foundation.fluid_filter_picker"),
                entry -> sendFluidFilter(pickerSlot, entry.fluidId()));
        this.fluidRow = new FluidFilterRow(() -> menu.duct().fluidFilter(menu.side()));
        this.channelPopup = new ChannelPopup(menu);
        this.amountPopup = new AmountPopup(amount -> ClientPacketDistributor.sendToServer(
                new SetDuctFilterAmountPayload(amountLine.ordinal(), amountSlot, amount)));
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
        amountPopup.setArea(leftPos, topPos, imageWidth, imageHeight);
        sideTabs.place(leftPos, topPos);
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

    /** Whether a popup covers the GUI: a filter picker, the amount popup or the channel popup. */
    public boolean isPickerOpen() {
        return filterPicker.isOpen() || fluidPicker.isOpen() || channelPopup.isOpen() || amountPopup.isOpen();
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

    /** What an amount means on the shown side of a line: a giving side keeps, a receiving side fills up to. */
    private String amountMeaning(Line line) {
        DuctMode mode = menu.duct().mode(line, menu.side());
        boolean gives = line == Line.ITEM ? mode.pull() : mode.in();
        if (gives == mode.out()) {
            return "keep_up_to";
        }
        return gives ? "keep" : "up_to";
    }

    /** "Keep 64" or "Up to 8,000 mB" for an amount of a line's filter slot. */
    private Component amountText(Line line, int amount) {
        String number = String.format(Locale.ROOT, "%,d", amount);
        Component value = line == Line.FLUID ? Component.translatable("gui.servus_foundation.amount.mb", number) : Component.literal(number);
        return Component.translatable("gui.servus_foundation.amount." + amountMeaning(line), value);
    }

    /** The tooltip lines after a set filter slot's name: its amount and meaning, then how to edit it. */
    private List<Component> amountLines(Line line, int amount) {
        List<Component> lines = new ArrayList<>();
        if (amount > 0) {
            lines.add(amountText(line, amount));
        }
        lines.add(Component.translatable("gui.servus_foundation.amount.hint").withColor(0x808080));
        return lines;
    }

    private void openAmountPopup(Line line, int slot, Component entry, int amount) {
        amountLine = line;
        amountSlot = slot;
        Component title = Component.translatable("gui.servus_foundation.amount.title",
                Component.translatable("gui.servus_foundation.amount.label." + amountMeaning(line)), entry);
        if (line == Line.FLUID) {
            amountPopup.open(title, amount, Integer.MAX_VALUE, 100, 1000, 8000);
        } else {
            amountPopup.open(title, amount, UtilityDuctBlockEntity.MAX_ITEM_AMOUNT, 1, 16, 64);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        amountPopup.tick();
    }

    /**
     * A left click with an empty cursor on a filter slot opens the popup and sends nothing, a right click with
     * an empty cursor on a set one the amount popup, a shift right click clears it. On the Fluid Pipe page a shift click on an inventory item sets the first
     * free fluid slot to its fluid and moves nothing.
     */
    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (slot != null && slot.index < UtilityDuctBlockEntity.FILTER_SLOTS && shownPage == Line.ITEM
                && containerInput == ContainerInput.QUICK_MOVE && buttonNum == 1) {
            super.slotClicked(slot, slotId, buttonNum, ContainerInput.PICKUP);
            return;
        }
        if (slot != null && slot.index < UtilityDuctBlockEntity.FILTER_SLOTS && shownPage == Line.ITEM
                && containerInput == ContainerInput.PICKUP && buttonNum == 1 && menu.getCarried().isEmpty()) {
            if (slot.hasItem()) {
                openAmountPopup(Line.ITEM, slot.index, slot.getItem().getHoverName(), menu.duct().itemAmount(menu.side(), slot.index));
            }
            return;
        }
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

    /**
     * Right click opens the amount popup of a set fluid slot, shift right click clears it; a left click takes
     * the carried container's fluid, or opens the picker with an empty cursor.
     */
    private void clickFluidSlot(int slot, int button, boolean shift) {
        FluidStack fluid = fluidRow.fluid(slot);
        if (button == 1 && shift) {
            sendFluidFilter(slot, Optional.empty());
        } else if (button == 1) {
            if (!fluid.isEmpty()) {
                openAmountPopup(Line.FLUID, slot, fluid.getHoverName(), menu.duct().fluidFilter(menu.side()).amount(slot));
            }
        } else if (button == 0 && menu.getCarried().isEmpty()) {
            openFluidPicker(slot);
        } else if (button == 0) {
            Transfer.fluidOf(menu.getCarried()).ifPresent(carried -> sendFluidFilter(slot, carried));
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

    private DuctMode shownMode() {
        return menu.duct().mode(shownPage, menu.side());
    }

    private void rebuildButtons() {
        clearWidgets();
        iconButtons.clear();
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
        if (line == Line.DATA) {
            addIconButton(DuctIconButton.toggle(leftPos + IN_X, topPos + ROW_Y, SWITCH_WIDTH, ICON_SIZE,
                    () -> switchLabel(shownMode().connects()), () -> shownMode().connects(), () -> true,
                    () -> List.of(Component.translatable(shownMode().connects() ? "gui.servus_foundation.data.on" : "gui.servus_foundation.data.off")),
                    () -> press(UtilityDuctMenu.BUTTON_TOGGLE_ON + line.ordinal())));
        } else {
            addIconButton(flagButton(line, IN_X, "in", IN_ICON, UtilityDuctMenu.BUTTON_TOGGLE_IN, () -> shownMode().in()));
            addIconButton(flagButton(line, OUT_X, "out", OUT_ICON, UtilityDuctMenu.BUTTON_TOGGLE_OUT, () -> shownMode().out()));
            if (line == Line.ITEM) {
                addIconButton(flagButton(line, PULL_X, "pull", PULL_ICON, UtilityDuctMenu.BUTTON_TOGGLE_PULL, () -> shownMode().pull()));
            }
            addIconButton(DuctIconButton.plain(leftPos + REDSTONE_X, topPos + ROW_Y, ICON_SIZE, ICON_SIZE,
                    () -> redstoneIcon(menu.duct().redstone(line, menu.side())), () -> redstoneTooltip(line),
                    () -> press(UtilityDuctMenu.BUTTON_CYCLE_REDSTONE + line.ordinal())));
            addIconButton(DuctIconButton.toggle(leftPos + SWITCH_X, topPos + ROW_Y, SWITCH_WIDTH, ICON_SIZE,
                    () -> switchLabel(menu.duct().isOn(line, menu.side())), () -> menu.duct().isOn(line, menu.side()), () -> true,
                    () -> switchTooltip(line), () -> press(UtilityDuctMenu.BUTTON_TOGGLE_ON + line.ordinal())));
            addRenderableWidget(new ChannelButton(leftPos + MARGIN, topPos + CHANNEL_Y, imageWidth - 2 * MARGIN,
                    () -> menu.duct().channel(line, menu.side()), () -> channelPopup.open(line)));
        }
        addIconButton(DuctIconButton.plain(leftPos + imageWidth - MARGIN - TRASH_SIZE, topPos + UtilityDuctMenu.INVENTORY_Y - TRASH_SIZE - 2,
                TRASH_SIZE, TRASH_SIZE, DuctIconButton.Icon::trash,
                () -> List.of(Component.translatable("gui.servus_foundation.remove." + line.id()),
                        Component.translatable("gui.servus_foundation.remove.hint").withColor(HINT)),
                () -> press(UtilityDuctMenu.BUTTON_REMOVE_LINE + line.ordinal())));
        if (line == Line.ITEM || line == Line.FLUID) {
            int down = line == Line.ITEM ? UtilityDuctMenu.BUTTON_PRIORITY_DOWN : UtilityDuctMenu.BUTTON_FLUID_PRIORITY_DOWN;
            int up = line == Line.ITEM ? UtilityDuctMenu.BUTTON_PRIORITY_UP : UtilityDuctMenu.BUTTON_FLUID_PRIORITY_UP;
            addRenderableWidget(Button.builder(Component.literal("-"), button -> press(down))
                    .bounds(leftPos + MARGIN, topPos + PRIORITY_Y, 16, 16).build());
            addRenderableWidget(Button.builder(Component.literal("+"), button -> press(up))
                    .bounds(leftPos + MARGIN + 76, topPos + PRIORITY_Y, 16, 16).build());
        }
    }

    private void addIconButton(DuctIconButton button) {
        iconButtons.add(button);
        addRenderableWidget(button);
    }

    /** The In, Out or Pull toggle of a line: lit while the flag is set. */
    private DuctIconButton flagButton(Line line, int x, String flag, ItemStack icon, int buttonBase, BooleanSupplier set) {
        return DuctIconButton.toggle(leftPos + x, topPos + ROW_Y, ICON_SIZE, ICON_SIZE, () -> DuctIconButton.Icon.item(icon), set, () -> true,
                () -> flagTooltip(line, flag, set.getAsBoolean()), () -> press(buttonBase + line.ordinal()));
    }

    private List<Component> flagTooltip(Line line, String flag, boolean set) {
        List<Component> lines = new ArrayList<>();
        Component name = Component.translatable("gui.servus_foundation.flag." + flag);
        lines.add(Component.translatable(set ? "gui.servus_foundation.flag.on" : "gui.servus_foundation.flag.off", name));
        lines.add(Component.translatable("gui.servus_foundation.flag." + flag + "." + line.id()).withColor(HINT));
        if (!flag.equals("pull") && !shownMode().connects()) {
            lines.add(Component.translatable("gui.servus_foundation.flag.none").withColor(HINT));
        }
        return lines;
    }

    private static DuctIconButton.Icon redstoneIcon(DuctRedstone setting) {
        return switch (setting) {
            case IGNORE -> DuctIconButton.Icon.item(IGNORE_ICON);
            case POWERED -> DuctIconButton.Icon.item(POWERED_ICON);
            case UNPOWERED -> DuctIconButton.Icon.texture(UNPOWERED_ICON);
        };
    }

    private List<Component> redstoneTooltip(Line line) {
        DuctRedstone setting = menu.duct().redstone(line, menu.side());
        return List.of(Component.translatable("gui.servus_foundation.redstone",
                        Component.translatable("gui.servus_foundation.redstone." + setting.key())),
                Component.translatable("gui.servus_foundation.redstone.tooltip").withColor(HINT));
    }

    private static DuctIconButton.Icon switchLabel(boolean on) {
        return DuctIconButton.Icon.label(Component.translatable(on ? "gui.servus_foundation.switch.on" : "gui.servus_foundation.switch.off"));
    }

    private List<Component> switchTooltip(Line line) {
        boolean on = menu.duct().isOn(line, menu.side());
        return List.of(Component.translatable(on ? "gui.servus_foundation.switch.on" : "gui.servus_foundation.switch.off"),
                Component.translatable(on ? "gui.servus_foundation.switch.tooltip.on" : "gui.servus_foundation.switch.tooltip.off").withColor(HINT));
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
        if (amountPopup.isOpen() && shownPage != amountLine) {
            amountPopup.close();
        }
        if (amountPopup.isOpen()) {
            super.extractRenderState(guiGraphics, -1, -1, partialTick);
            renderAmounts(guiGraphics);
            amountPopup.render(guiGraphics, font, mouseX, mouseY, partialTick);
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
        renderAmounts(guiGraphics);
        Line hovered = tabAt(mouseX, mouseY);
        if (hovered != null) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_foundation.line." + hovered.id()), mouseX, mouseY);
        } else if (!sideTabs.renderTooltip(guiGraphics, font, mouseX, mouseY) && !renderIconTooltip(guiGraphics, mouseX, mouseY)) {
            renderFilterTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    /** The tooltip of the hovered icon button, also of a disabled one. Returns whether one was drawn. */
    private boolean renderIconTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        for (DuctIconButton button : iconButtons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                guiGraphics.setComponentTooltipForNextFrame(font, button.tooltip(), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    /** The amount of every set filter slot of the shown page, in the slot's corner. */
    private void renderAmounts(GuiGraphicsExtractor guiGraphics) {
        if (shownPage == Line.FLUID) {
            fluidRow.renderAmounts(guiGraphics, font);
        } else if (shownPage == Line.ITEM) {
            for (int slot = 0; slot < UtilityDuctBlockEntity.FILTER_SLOTS; slot++) {
                int amount = menu.duct().itemAmount(menu.side(), slot);
                if (amount > 0) {
                    Rect2i area = filterSlotArea(slot);
                    guiGraphics.itemDecorations(font, menu.getSlot(slot).getItem(), area.getX(), area.getY(), PickerStock.shortCount(amount));
                }
            }
        }
    }

    /** A set filter slot's name with its amount and the edit hint. */
    private void renderFilterTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (shownPage == Line.FLUID) {
            fluidRow.renderTooltip(guiGraphics, font, mouseX, mouseY,
                    slot -> amountLines(Line.FLUID, menu.duct().fluidFilter(menu.side()).amount(slot)));
        } else if (shownPage == Line.ITEM && hoveredSlot != null && hoveredSlot.index < UtilityDuctBlockEntity.FILTER_SLOTS
                && hoveredSlot.hasItem()) {
            List<Component> lines = new ArrayList<>();
            lines.add(hoveredSlot.getItem().getHoverName());
            lines.addAll(amountLines(Line.ITEM, menu.duct().itemAmount(menu.side(), hoveredSlot.index)));
            guiGraphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderTabs(guiGraphics);
        sideTabs.renderBehind(guiGraphics, menu.side(), mouseX, mouseY);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        sideTabs.renderSelected(guiGraphics, menu.side());
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
        guiGraphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        Component sideName = Component.translatable("gui.servus_foundation.utility_duct.side",
                DuctSideTabs.sideName(menu.side()), menu.duct().touchedBlockName(menu.side()));
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
            guiGraphics.text(font, Component.translatable("gui.servus_foundation.no_lines"), MARGIN, ROW_Y + 5, TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (amountPopup.isOpen()) {
            return amountPopup.mouseClicked(event, doubleClick);
        }
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
            clickFluidSlot(fluidSlot, event.button(), event.hasShiftDown());
            return true;
        }
        Line tab = tabAt(event.x(), event.y());
        if (tab != null) {
            menu.setPage(tab);
            rebuildButtons();
            return true;
        }
        Direction picked = sideTabs.sideAt(event.x(), event.y());
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
        return amountPopup.mouseScrolled(mouseX, mouseY, scrollY) || channelPopup.mouseScrolled(scrollY) || filterPicker.mouseScrolled(scrollY) || fluidPicker.mouseScrolled(scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** While the popup is open every key goes to it, so typing E does not close the GUI and Escape closes only the popup. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return amountPopup.keyPressed(event) || channelPopup.keyPressed(event) || filterPicker.keyPressed(event) || fluidPicker.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return amountPopup.charTyped(event) || channelPopup.charTyped(event) || filterPicker.charTyped(event) || fluidPicker.charTyped(event) || super.charTyped(event);
    }
}
