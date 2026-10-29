package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.PickerEntries;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidKeep;
import io.github.dirnei.mc.servus.shuttles.fluidstation.SetFluidKeepPayload;
import io.github.dirnei.mc.servus.shuttles.menu.FluidOutboundMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Fluid Outbound GUI: the shared station GUI with the keep fluid picker
 * above the gauge, a mark on the gauge at the keep amount, the amount below
 * it and the -1 B, +1 B, -10 B and +10 B buttons. Clicking the picker with a
 * filled fluid container on the cursor keeps that container's fluid. While
 * the picker's popup is open nothing below it reacts.
 */
public class FluidOutboundScreen extends FluidStationScreen<FluidOutboundMenu> {

    private static final int COLOUR_KEEP_MARK = 0xFFFFFFFF;
    private static final String[] STEP_LABELS = {"-1", "+1", "-10", "+10"};
    private static List<ItemPicker.Entry> entries;

    private final ItemPicker picker = new ItemPicker(Component.translatable("gui.servus_shuttles.fluid_outbound.keep_picker"),
            entry -> sendKeep(entry.fluidId()));
    private final Button[] stepButtons = new Button[FluidOutboundMenu.KEEP_STEPS.length];

    public FluidOutboundScreen(FluidOutboundMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FluidOutboundMenu.IMAGE_HEIGHT, FluidOutboundMenu.GAUGE_Y, FluidOutboundMenu.STATE_Y);
        picker.setEntries(keepEntries());
    }

    /** "Keep nothing" and every source fluid, built once per client session. */
    private static List<ItemPicker.Entry> keepEntries() {
        if (entries == null) {
            List<ItemPicker.Entry> list = new ArrayList<>();
            list.add(ItemPicker.Entry.none(Component.translatable("gui.servus_shuttles.fluid_outbound.keep_none")));
            list.addAll(PickerEntries.sourceFluids());
            entries = List.copyOf(list);
        }
        return entries;
    }

    private static void sendKeep(Optional<Identifier> fluid) {
        ClientPacketDistributor.sendToServer(new SetFluidKeepPayload(fluid));
    }

    @Override
    protected void init() {
        super.init();
        picker.setButton(leftPos + FluidOutboundMenu.PICKER_X, topPos + FluidOutboundMenu.PICKER_Y, FluidOutboundMenu.PICKER_WIDTH);
        picker.setArea(leftPos, topPos, imageWidth, imageHeight);
        int width = (menu.pitch() - 6) / 2;
        for (int step = 0; step < stepButtons.length; step++) {
            int id = FluidOutboundMenu.keepButton(step);
            int x = leftPos + menu.columnX() + 2 + (step % 2) * (width + 2);
            int y = topPos + FluidOutboundMenu.BUTTONS_Y + (step / 2) * (FluidOutboundMenu.BUTTON_HEIGHT + 1);
            stepButtons[step] = addRenderableWidget(Button.builder(Component.literal(STEP_LABELS[step]),
                    button -> MenuButtons.press(minecraft, menu, id)).bounds(x, y, width, FluidOutboundMenu.BUTTON_HEIGHT).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (Button button : stepButtons) {
            button.active = menu.keep().isSet();
        }
        Fluid kept = menu.keep().fluid();
        picker.setSelected(kept == null ? PickerList.NONE_ID : BuiltInRegistries.FLUID.getKey(kept).toString());
        if (picker.isOpen()) {
            super.extractRenderState(graphics, -1, -1, partialTick);
            picker.renderPopup(graphics, font, mouseX, mouseY, partialTick);
            picker.renderTooltip(graphics, font, mouseX, mouseY);
            return;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        picker.renderTooltip(graphics, font, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        picker.renderButton(graphics, font, mouseX, mouseY);
        renderKeepMark(graphics, menu.keep());
    }

    /** The line across the gauge at the keep amount. */
    private void renderKeepMark(GuiGraphicsExtractor graphics, FluidKeep<Fluid> keep) {
        int capacity = menu.capacity();
        if (keep.target() <= 0 || capacity <= 0) {
            return;
        }
        int height = FluidOutboundMenu.GAUGE_HEIGHT;
        int bottom = topPos + FluidOutboundMenu.GAUGE_Y + height;
        int y = Math.min(bottom - 1, bottom - (int) Math.min(height, (long) keep.target() * height / capacity));
        int x = leftPos + menu.gaugeX();
        graphics.fill(x, y, x + FluidOutboundMenu.GAUGE_WIDTH, y + 1, COLOUR_KEEP_MARK);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component amount = Component.translatable("gui.servus_shuttles.fluid_outbound.keep_amount", buckets(menu.keep().target()));
        int x = menu.columnX() + (menu.pitch() - font.width(amount)) / 2;
        graphics.text(font, amount, x, FluidOutboundMenu.AMOUNT_Y, COLOUR_TEXT, false);
    }

    /** An amount in buckets: whole buckets plain, else with one decimal. */
    private static String buckets(int millibuckets) {
        return millibuckets % 1000 == 0 ? Integer.toString(millibuckets / 1000) : String.format(Locale.ROOT, "%.1f", millibuckets / 1000.0);
    }

    private boolean overPicker(double mouseX, double mouseY) {
        double x = mouseX - leftPos - FluidOutboundMenu.PICKER_X;
        double y = mouseY - topPos - FluidOutboundMenu.PICKER_Y;
        return x >= 0 && x < FluidOutboundMenu.PICKER_WIDTH && y >= 0 && y < FluidOutboundMenu.PICKER_HEIGHT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (picker.isOpen()) {
            return picker.mouseClicked(event, doubleClick);
        }
        if (overPicker(event.x(), event.y())) {
            Optional<Fluid> carried = Transfer.fluidOf(menu.getCarried());
            if (carried.isPresent()) {
                sendKeep(Optional.of(BuiltInRegistries.FLUID.getKey(carried.get())));
                return true;
            }
        }
        return picker.mouseClicked(event, doubleClick) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return picker.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return picker.mouseReleased() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return picker.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return picker.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return picker.charTyped(event) || super.charTyped(event);
    }
}
