package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.PickerEntries;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidRestockPlan;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidKeep;
import io.github.dirnei.mc.servus.shuttles.fluidstation.SetFluidKeepPayload;
import io.github.dirnei.mc.servus.shuttles.fluidstation.SetFluidLevelPayload;
import io.github.dirnei.mc.servus.shuttles.menu.FluidOutboundMenu;
import io.github.dirnei.mc.servus.shuttles.menu.FluidStationMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Fluid Outbound GUI: the shared station GUI with the keep fluid picker
 * above a tall gauge. A white arrow left of the gauge points at the keep
 * amount, an orange one right of it at the restock level; dragging an arrow
 * or turning the wheel over it sets its level in whole buckets, and a typed
 * field on each side sets it too. Clicking the picker with a filled fluid
 * container on the cursor keeps that container's fluid. While the picker's
 * popup is open nothing below it reacts.
 */
public class FluidOutboundScreen extends FluidStationScreen<FluidOutboundMenu> {

    private static final int COLOUR_KEEP = 0xFFFFFFFF;
    private static final int COLOUR_RESTOCK = 0xFFFFA040;
    private static final int COLOUR_ARROW_EDGE = 0xFF373737;
    private static final int BUCKET = FluidRestockPlan.BUCKET;
    private static final int ARROW_WIDTH = 6;
    private static final int ARROW_HEIGHT = 8;
    private static final int ARROW_GAP = 2;
    private static final int FIELD_WIDTH = 26;
    private static final int FIELD_HEIGHT = 12;
    private static final int MARGIN = 8;
    private static final int UNIT_GAP = 3;
    /** Ticks a sent level shows before the synced one wins again. */
    private static final int ANSWER_TICKS = 20;
    private static List<ItemPicker.Entry> entries;

    private final ItemPicker picker = new ItemPicker(Component.translatable("gui.servus_shuttles.fluid_outbound.keep_picker"),
            entry -> sendKeep(entry.fluidId()));
    private final List<AmountField> fields = new ArrayList<>();
    /** The level an arrow is dragged for: true restock, false keep, null while no arrow is dragged. */
    private @Nullable Boolean dragging;
    private int dragValue;
    /** The keep as last sent, shown until the server's answer arrives. */
    private @Nullable FluidKeep<Fluid> sent;
    private int answerTicks;

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
        fields.clear();
        fields.add(addField(false));
        fields.add(addField(true));
    }

    /** The typed field of one level, in whole buckets, centred in its side of the gauge. */
    private AmountField addField(boolean restock) {
        int x = leftPos + sideCentre(restock) - (FIELD_WIDTH + UNIT_GAP + font.width(unit())) / 2;
        int y = topPos + fieldY();
        AmountField field = new AmountField(font, x, y, FIELD_WIDTH, FIELD_HEIGHT,
                value -> (int) Math.clamp(value, 0, menu.capacity() / BUCKET), label(restock),
                () -> level(shown(), restock) / BUCKET, value -> sendLevel(restock, (int) value * BUCKET));
        addRenderableWidget(field.box());
        return field;
    }

    private static Component label(boolean restock) {
        return Component.translatable(restock ? "gui.servus_shuttles.fluid_outbound.restock" : "gui.servus_shuttles.fluid_outbound.keep");
    }

    private static Component unit() {
        return Component.translatable("gui.servus_shuttles.fluid_outbound.buckets");
    }

    private int fieldY() {
        return FluidOutboundMenu.GAUGE_Y + (FluidOutboundMenu.OUTBOUND_GAUGE_HEIGHT - FIELD_HEIGHT) / 2 + 5;
    }

    /** The middle of the free space left of the keep arrow or right of the restock arrow. */
    private int sideCentre(boolean restock) {
        if (restock) {
            int left = arrowX(true) + ARROW_WIDTH + ARROW_GAP;
            return (left + imageWidth - MARGIN) / 2;
        }
        return (MARGIN + arrowX(false) - ARROW_GAP) / 2;
    }

    /** The left edge of an arrow, relative to the GUI: left of the gauge for the keep, right of it for the restock level. */
    private int arrowX(boolean restock) {
        return restock ? menu.gaugeX() + FluidStationMenu.GAUGE_WIDTH + 1 + ARROW_GAP : menu.gaugeX() - 1 - ARROW_GAP - ARROW_WIDTH;
    }

    private static int level(FluidKeep<Fluid> keep, boolean restock) {
        return restock ? keep.restockLevel() : keep.target();
    }

    private FluidKeep<Fluid> moved(FluidKeep<Fluid> keep, boolean restock, int millibuckets) {
        return restock ? keep.withRestockAt(millibuckets, menu.capacity()) : keep.withAmount(millibuckets, menu.capacity());
    }

    /** The keep as the GUI shows it: dragged, else just sent, else as synced. */
    private FluidKeep<Fluid> shown() {
        FluidKeep<Fluid> synced = menu.keep();
        if (dragging != null && synced.isSet()) {
            return moved(synced, dragging, dragValue);
        }
        if (sent != null && answerTicks > 0 && !sent.equals(synced) && sent.fluid() == synced.fluid()) {
            return sent;
        }
        sent = null;
        return synced;
    }

    private void sendLevel(boolean restock, int millibuckets) {
        FluidKeep<Fluid> keep = shown();
        if (!keep.isSet()) {
            return;
        }
        sent = moved(keep, restock, millibuckets);
        answerTicks = ANSWER_TICKS;
        ClientPacketDistributor.sendToServer(new SetFluidLevelPayload(restock, millibuckets));
    }

    /** The y of the gauge row at {@code millibuckets}, relative to the GUI. */
    private int levelY(int millibuckets) {
        int capacity = Math.max(1, menu.capacity());
        int height = FluidOutboundMenu.OUTBOUND_GAUGE_HEIGHT;
        int bottom = FluidOutboundMenu.GAUGE_Y + height;
        return Math.min(bottom - 1, bottom - (int) Math.clamp((long) millibuckets * height / capacity, 0, height));
    }

    /** The whole buckets at the gauge row under {@code mouseY}, within 0 and the capacity. */
    private int valueAt(double mouseY) {
        int height = FluidOutboundMenu.OUTBOUND_GAUGE_HEIGHT;
        double fromBottom = topPos + FluidOutboundMenu.GAUGE_Y + height - mouseY;
        long buckets = Math.round(fromBottom * menu.capacity() / height / BUCKET);
        return (int) Math.clamp(buckets, 0, menu.capacity() / BUCKET) * BUCKET;
    }

    /** The arrow under the mouse: true restock, false keep, null for none or without a keep fluid. */
    private @Nullable Boolean arrowAt(double mouseX, double mouseY) {
        FluidKeep<Fluid> keep = shown();
        if (!keep.isSet() || menu.capacity() <= 0) {
            return null;
        }
        for (boolean restock : new boolean[] {true, false}) {
            double x = mouseX - leftPos - arrowX(restock);
            double y = mouseY - topPos - levelY(level(keep, restock));
            if (x >= -1 && x < ARROW_WIDTH + 1 && y >= -ARROW_HEIGHT / 2.0 - 1 && y < ARROW_HEIGHT / 2.0 + 1) {
                return restock;
            }
        }
        return null;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (answerTicks > 0) {
            answerTicks--;
        }
        boolean set = menu.keep().isSet();
        for (AmountField field : fields) {
            field.box().active = set;
            field.box().setEditable(set);
            if (!set && field.isFocused()) {
                clearFieldFocus();
            }
            field.tick();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
        FluidKeep<Fluid> keep = shown();
        if (keep.isSet() && menu.capacity() > 0) {
            renderLevel(graphics, keep.restockLevel(), true, COLOUR_RESTOCK);
            renderLevel(graphics, keep.target(), false, COLOUR_KEEP);
        }
    }

    /** A mark across the gauge at {@code millibuckets} and the arrow pointing at it. */
    private void renderLevel(GuiGraphicsExtractor graphics, int millibuckets, boolean restock, int colour) {
        int y = topPos + levelY(millibuckets);
        int gaugeX = leftPos + menu.gaugeX();
        graphics.fill(gaugeX, y, gaugeX + FluidStationMenu.GAUGE_WIDTH, y + 1, colour);
        int x = leftPos + arrowX(restock);
        renderArrow(graphics, x, y, restock, 1, COLOUR_ARROW_EDGE);
        renderArrow(graphics, x, y, restock, 0, colour);
    }

    /**
     * A triangle {@link #ARROW_WIDTH} wide and {@link #ARROW_HEIGHT} tall with
     * its tip on row {@code tipY}: pointing right when it sits left of the
     * gauge, left otherwise. {@code grow} widens it by pixels for an edge.
     */
    private static void renderArrow(GuiGraphicsExtractor graphics, int x, int tipY, boolean pointsLeft, int grow, int colour) {
        for (int column = -grow; column < ARROW_WIDTH + grow; column++) {
            int fromTip = pointsLeft ? column : ARROW_WIDTH - 1 - column;
            int half = Math.max(0, fromTip) * ARROW_HEIGHT / (2 * ARROW_WIDTH) + grow;
            graphics.fill(x + column, tipY - half, x + column + 1, tipY + half + 1, colour);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int labelY = fieldY() - 11;
        int unitY = fieldY() + (FIELD_HEIGHT - 8) / 2;
        for (boolean restock : new boolean[] {false, true}) {
            Component label = label(restock);
            int centre = sideCentre(restock);
            graphics.text(font, label, centre - font.width(label) / 2, labelY, COLOUR_TEXT, false);
            int unitX = centre - (FIELD_WIDTH + UNIT_GAP + font.width(unit())) / 2 + FIELD_WIDTH + UNIT_GAP;
            graphics.text(font, unit(), unitX, unitY, COLOUR_TEXT, false);
        }
    }

    private boolean overPicker(double mouseX, double mouseY) {
        double x = mouseX - leftPos - FluidOutboundMenu.PICKER_X;
        double y = mouseY - topPos - FluidOutboundMenu.PICKER_Y;
        return x >= 0 && x < FluidOutboundMenu.PICKER_WIDTH && y >= 0 && y < FluidOutboundMenu.PICKER_HEIGHT;
    }

    private @Nullable AmountField focusedField() {
        for (AmountField field : fields) {
            if (field.isFocused()) {
                return field;
            }
        }
        return null;
    }

    /** Takes the keyboard from the typed fields; a field that had it sends its number at the next tick. */
    private void clearFieldFocus() {
        setFocused(null);
        for (AmountField field : fields) {
            field.box().setFocused(false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (picker.isOpen()) {
            return picker.mouseClicked(event, doubleClick);
        }
        AmountField focused = focusedField();
        if (focused != null && !focused.box().isMouseOver(event.x(), event.y())) {
            clearFieldFocus();
        }
        if (overPicker(event.x(), event.y())) {
            Optional<Fluid> carried = Transfer.fluidOf(menu.getCarried());
            if (carried.isPresent()) {
                sendKeep(Optional.of(BuiltInRegistries.FLUID.getKey(carried.get())));
                return true;
            }
        }
        Boolean arrow = event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT ? arrowAt(event.x(), event.y()) : null;
        if (arrow != null) {
            dragValue = valueAt(event.y());
            dragging = arrow;
            return true;
        }
        return picker.mouseClicked(event, doubleClick) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (picker.isOpen()) {
            return picker.mouseDragged(event.x(), event.y());
        }
        if (dragging != null) {
            dragValue = valueAt(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (picker.isOpen()) {
            return picker.mouseReleased();
        }
        if (dragging != null) {
            boolean restock = dragging;
            dragging = null;
            sendLevel(restock, dragValue);
            return true;
        }
        return super.mouseReleased(event);
    }

    /** The wheel over an arrow steps its level by a bucket, with shift by ten. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (picker.isOpen()) {
            return picker.mouseScrolled(scrollY);
        }
        Boolean arrow = dragging == null && scrollY != 0 ? arrowAt(mouseX, mouseY) : null;
        if (arrow != null) {
            int step = (Minecraft.getInstance().hasShiftDown() ? 10 : 1) * BUCKET * (int) Math.signum(scrollY);
            int current = level(shown(), arrow) / BUCKET * BUCKET;
            sendLevel(arrow, (int) Math.clamp((long) current + step, 0, menu.capacity()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** A typed field keeps every key but Escape, so E does not close the GUI; Enter sends its number. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (picker.keyPressed(event)) {
            return true;
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
        return picker.charTyped(event) || super.charTyped(event);
    }

    @Override
    public void removed() {
        AmountField focused = focusedField();
        if (focused != null) {
            focused.commit();
        }
        super.removed();
    }
}
