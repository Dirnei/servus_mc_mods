package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.shuttles.gantry.SetGantryLockPayload;
import io.github.dirnei.mc.servus.shuttles.menu.FluidGantryMenu;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Fluid Gantry GUI: a gauge per tank with, in Export mode, its keep level
 * and step buttons below and a mark on the gauge, then its lock picker; the
 * mode button between them over the Capacity Upgrade row, the state line and
 * the player inventory. While a lock picker is open nothing below it reacts.
 */
public class FluidGantryScreen extends AbstractContainerScreen<FluidGantryMenu> {

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_WARN = 0xFFA03020;
    private static final int STATE_ROWS = 2;
    private static final int MARGIN = 8;
    private static final int COLOUR_GREYED = 0xFF8B8B8B;
    private static final int COLOUR_KEEP_MARK = 0xFFFFFFFF;
    private static final int COLOUR_KEEP_MARK_GREYED = 0xFF8B8B8B;
    private static final String[] KEEP_LABELS = {"-10", "-1", "+1", "+10"};
    private static final int[] KEEP_BUTTON_X = {0, 23, 40, 57};
    private static final int[] KEEP_BUTTON_WIDTH = {20, 14, 14, 20};

    private final FluidGauge[] gauges = {new FluidGauge(FluidGantryMenu.GAUGE_WIDTH, FluidGantryMenu.GAUGE_HEIGHT),
            new FluidGauge(FluidGantryMenu.GAUGE_WIDTH, FluidGantryMenu.GAUGE_HEIGHT)};
    private final ItemPicker[] pickers = {picker(0), picker(1)};
    private Button modeButton;
    private final Button[][] keepButtons = new Button[2][FluidGantryMenu.KEEP_STEPS.length];

    public FluidGantryScreen(FluidGantryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FluidGantryMenu.WIDTH, FluidGantryMenu.IMAGE_HEIGHT);
    }

    private static ItemPicker picker(int tank) {
        ItemPicker picker = new ItemPicker(Component.translatable("gui.servus_shuttles.fluid_gantry.lock_picker"), entry -> pick(tank, entry));
        picker.setEntries(FluidLockEntries.build());
        return picker;
    }

    private static void pick(int tank, ItemPicker.Entry entry) {
        SetGantryLockPayload payload;
        if (entry.isNone()) {
            payload = new SetGantryLockPayload(tank, SetGantryLockPayload.ANY, Optional.empty());
        } else if (FluidLockEntries.CONTENT_ID.equals(entry.id())) {
            payload = new SetGantryLockPayload(tank, SetGantryLockPayload.CONTENT, Optional.empty());
        } else {
            payload = new SetGantryLockPayload(tank, SetGantryLockPayload.FLUID, Optional.ofNullable(Identifier.tryParse(entry.id())));
        }
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = FluidGantryMenu.INVENTORY_Y - 11;
        gauges[0].place(leftPos + FluidGantryMenu.LEFT_GAUGE_X, topPos + FluidGantryMenu.GAUGE_Y);
        gauges[1].place(leftPos + FluidGantryMenu.RIGHT_GAUGE_X, topPos + FluidGantryMenu.GAUGE_Y);
        pickers[0].setButton(leftPos + FluidGantryMenu.LEFT_LOCK_X, topPos + FluidGantryMenu.LOCK_Y, FluidGantryMenu.LOCK_WIDTH);
        pickers[1].setButton(leftPos + FluidGantryMenu.RIGHT_LOCK_X, topPos + FluidGantryMenu.LOCK_Y, FluidGantryMenu.LOCK_WIDTH);
        for (ItemPicker picker : pickers) {
            picker.setArea(leftPos, topPos, imageWidth, imageHeight);
        }
        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> MenuButtons.press(minecraft, menu, FluidGantryMenu.BUTTON_MODE))
                .bounds(leftPos + FluidGantryMenu.MODE_X, topPos + FluidGantryMenu.MODE_Y, FluidGantryMenu.MODE_WIDTH, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.servus_shuttles.fluid_gantry.mode_button.tooltip"))).build());
        for (int tank = 0; tank < 2; tank++) {
            for (int step = 0; step < FluidGantryMenu.KEEP_STEPS.length; step++) {
                int id = FluidGantryMenu.keepButton(tank, step);
                keepButtons[tank][step] = addRenderableWidget(Button.builder(Component.literal(KEEP_LABELS[step]),
                                button -> MenuButtons.press(minecraft, menu, id))
                        .bounds(leftPos + keepX(tank) + KEEP_BUTTON_X[step], topPos + FluidGantryMenu.KEEP_BUTTONS_Y, KEEP_BUTTON_WIDTH[step],
                                FluidGantryMenu.KEEP_BUTTON_HEIGHT)
                        .build());
            }
        }
    }

    private static int keepX(int tank) {
        return tank == 0 ? FluidGantryMenu.LEFT_LOCK_X : FluidGantryMenu.RIGHT_LOCK_X;
    }

    private static int gaugeX(int tank) {
        return tank == 0 ? FluidGantryMenu.LEFT_GAUGE_X : FluidGantryMenu.RIGHT_GAUGE_X;
    }

    private void updateKeepButtons() {
        for (int tank = 0; tank < 2; tank++) {
            Tooltip tooltip = Tooltip.create(Component.translatable(menu.keepApplies(tank)
                    ? "gui.servus_shuttles.fluid_gantry.keep.tooltip" : "gui.servus_shuttles.fluid_gantry.keep.needs_lock"));
            for (Button button : keepButtons[tank]) {
                button.visible = menu.showsKeep();
                button.active = menu.keepApplies(tank);
                button.setTooltip(tooltip);
            }
        }
    }

    /** "Keep: 8 B", the keep level of a tank in buckets. */
    private Component keepLabel(int tank) {
        int keep = menu.keep(tank);
        String buckets = keep % 1000 == 0 ? Integer.toString(keep / 1000) : String.format(Locale.ROOT, "%.1f", keep / 1000.0);
        return Component.translatable("gui.servus_shuttles.fluid_gantry.keep", buckets);
    }

    /** The line across a gauge at the keep level, grey while the keep level does not apply. */
    private void renderKeepMark(GuiGraphicsExtractor graphics, int tank) {
        int keep = menu.keep(tank);
        int capacity = menu.capacity();
        if (!menu.showsKeep() || keep <= 0 || capacity <= 0) {
            return;
        }
        int height = FluidGantryMenu.GAUGE_HEIGHT;
        int bottom = topPos + FluidGantryMenu.GAUGE_Y + height;
        int y = Math.min(bottom - 1, bottom - (int) Math.min(height, (long) keep * height / capacity));
        int x = leftPos + gaugeX(tank);
        graphics.fill(x, y, x + FluidGantryMenu.GAUGE_WIDTH, y + 1, menu.keepApplies(tank) ? COLOUR_KEEP_MARK : COLOUR_KEEP_MARK_GREYED);
    }

    private Component modeLabel() {
        return Component.translatable("gui.servus_shuttles.fluid_gantry.mode_button",
                Component.translatable("gui.servus_shuttles.fluid_gantry.mode." + menu.mode().getSerializedName()));
    }

    /** The picker whose popup is open, or null. */
    private ItemPicker openPicker() {
        for (ItemPicker picker : pickers) {
            if (picker.isOpen()) {
                return picker;
            }
        }
        return null;
    }

    private Rect upgradesRow() {
        return new Rect(menu.upgradeX(0), FluidGantryMenu.UPGRADES_Y, menu.upgradeSlots() * 18, 18);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel());
        updateKeepButtons();
        for (int tank = 0; tank < 2; tank++) {
            pickers[tank].setSelected(FluidLockEntries.idOf(menu.lock(tank)));
        }
        ItemPicker open = openPicker();
        if (open != null) {
            super.extractRenderState(graphics, -1, -1, partialTick);
            open.renderPopup(graphics, font, mouseX, mouseY, partialTick);
            open.renderTooltip(graphics, font, mouseX, mouseY);
            return;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (int tank = 0; tank < 2; tank++) {
            gauges[tank].renderTooltip(graphics, font, menu.fluid(tank), menu.capacity(), mouseX, mouseY);
            pickers[tank].renderTooltip(graphics, font, mouseX, mouseY);
            if (menu.showsKeep() && overKeepLabel(tank, mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(font, font.split(Component.translatable(menu.keepApplies(tank)
                        ? "gui.servus_shuttles.fluid_gantry.keep.tooltip" : "gui.servus_shuttles.fluid_gantry.keep.needs_lock"), 180), mouseX, mouseY);
            }
        }
        boolean overRow = upgradesRow().coversRow(mouseX - leftPos, mouseY - topPos, FluidGantryMenu.UPGRADES_Y - 10);
        if (menu.getCarried().isEmpty() && overRow && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            graphics.setTooltipForNextFrame(font, font.split(Component.translatable("gui.servus_shuttles.fluid_gantry.upgrades.tooltip"), 180),
                    mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (int tank = 0; tank < 2; tank++) {
            gauges[tank].render(graphics, menu.fluid(tank), menu.capacity());
            renderKeepMark(graphics, tank);
            pickers[tank].renderButton(graphics, font, mouseX, mouseY);
        }
        if (menu.upgradeSlots() > 0) {
            PanelPainter.slots(graphics, leftPos, topPos, upgradesRow(), menu.upgradeSlots(), menu.upgradeSlots());
        }
        PanelPainter.playerInventory(graphics, leftPos, topPos, FluidGantryMenu.INVENTORY_Y, menu.hotbarY());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component upgrades = Component.translatable("gui.servus_shuttles.fluid_gantry.upgrades");
        graphics.text(font, upgrades, (imageWidth - font.width(upgrades)) / 2, FluidGantryMenu.UPGRADES_Y - 10, COLOUR_TEXT, false);
        if (menu.showsKeep()) {
            for (int tank = 0; tank < 2; tank++) {
                graphics.text(font, keepLabel(tank), keepX(tank), FluidGantryMenu.KEEP_Y, menu.keepApplies(tank) ? COLOUR_TEXT : COLOUR_GREYED, false);
            }
        }
        boolean warning = menu.stateWarns();
        List<FormattedCharSequence> rows = font.split(menu.stateLine(), imageWidth - 2 * MARGIN);
        for (int i = 0; i < Math.min(STATE_ROWS, rows.size()); i++) {
            graphics.text(font, rows.get(i), MARGIN, FluidGantryMenu.STATE_Y + i * 9, warning ? COLOUR_WARN : COLOUR_TEXT, false);
        }
    }

    private boolean overKeepLabel(int tank, int mouseX, int mouseY) {
        int x = mouseX - leftPos - keepX(tank);
        int y = mouseY - topPos - FluidGantryMenu.KEEP_Y;
        return x >= 0 && x < FluidGantryMenu.LOCK_WIDTH && y >= -1 && y < 9;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ItemPicker open = openPicker();
        if (open != null) {
            return open.mouseClicked(event, doubleClick);
        }
        for (ItemPicker picker : pickers) {
            if (picker.mouseClicked(event, doubleClick)) {
                return true;
            }
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
        if (open != null) {
            return open.keyPressed(event) || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        ItemPicker open = openPicker();
        return open != null && open.charTyped(event) || super.charTyped(event);
    }
}
