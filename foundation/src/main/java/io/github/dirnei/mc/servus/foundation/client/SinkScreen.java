package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.menu.SinkMenu;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkRates;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Sink GUI in soot and brick: the buffer rows with a sideways tank beside each,
 * locked rows and tanks behind a closed iron gate, an ember glow on the slots
 * the next batch takes from, the batch bar, the chimney pips, a fan that turns
 * while the Sink is active, the rates and the Capacity Upgrade tab. The sheet
 * comes from {@code tools/sink-textures/gen.py}.
 */
public class SinkScreen extends AbstractContainerScreen<SinkMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "textures/gui/sink.png");
    private static final int SHEET = 256;
    private static final int TAB_U = 176;
    private static final int TAB_HEIGHT = 68;
    private static final int SPRITE_V = 200;
    private static final int GATE_SLOT_U = 0;
    private static final int GATE_TANK_U = 18;
    private static final int EMBER_U = 48;
    private static final int PIP_U = 66;
    private static final int PIP_CAP_U = 71;
    private static final int FAN_U = 80;
    private static final int BAR_V = 220;
    private static final int BAR_FILL = SinkMenu.BAR_WIDTH - 2;
    private static final int PIPS = 5;
    private static final int PIP_X = 116;
    private static final int PIP_Y = 4;
    private static final int PIP_WIDTH = 5;
    private static final int PIP_HEIGHT = 8;
    private static final int FAN_X = 158;
    private static final int FAN_Y = 3;
    private static final int FAN_SIZE = 14;
    private static final int TILE = 16;
    private static final int TEXT = 0xFFF0DCC4;
    private static final int DIM = 0xFFB99D88;
    private static final int WARN = 0xFFFF8A6A;

    public SinkScreen(SinkMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, SinkMenu.WIDTH, SinkMenu.IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = SinkMenu.MESSAGE_Y;
    }

    private static int tankY(int tank) {
        return SinkMenu.BUFFER_Y + tank * 18;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        blit(graphics, 0, 0, 0, 0, imageWidth, imageHeight);
        blit(graphics, SinkMenu.TAB_X, SinkMenu.TAB_Y, TAB_U, 0, SinkMenu.TAB_WIDTH, TAB_HEIGHT);
        boolean[] marked = menu.nextBatchSlots();
        for (int index = 0; index < menu.bufferSlots(); index++) {
            Slot slot = menu.getSlot(index);
            if (!menu.isSlotOpen(index)) {
                blit(graphics, slot.x - 1, slot.y - 1, GATE_SLOT_U, SPRITE_V, 18, 18);
            } else if (marked[index]) {
                blit(graphics, slot.x, slot.y, EMBER_U, SPRITE_V, TILE, TILE);
            }
        }
        for (int tank = 0; tank < menu.maxRows(); tank++) {
            if (tank < menu.openRows()) {
                drawFluid(graphics, leftPos + SinkMenu.TANK_X, topPos + tankY(tank), menu.fluid(tank), menu.tankCapacity());
            } else {
                blit(graphics, SinkMenu.TANK_X - 1, tankY(tank) - 1, GATE_TANK_U, SPRITE_V, SinkMenu.TANK_WIDTH, SinkMenu.TANK_HEIGHT);
            }
        }
        int filled = batchFill();
        if (filled > 0) {
            blit(graphics, SinkMenu.BAR_X + 1, SinkMenu.BAR_Y + 1, 0, BAR_V, filled, 2);
        }
        int height = menu.chimneyHeight();
        for (int pip = 0; pip < Math.min(height, PIPS); pip++) {
            blit(graphics, PIP_X + pip * 7, PIP_Y, pip == PIPS - 1 ? PIP_CAP_U : PIP_U, SPRITE_V, PIP_WIDTH, PIP_HEIGHT);
        }
        blit(graphics, FAN_X, FAN_Y, FAN_U + fanFrame(height) * FAN_SIZE, SPRITE_V, FAN_SIZE, FAN_SIZE);
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x, topPos + y, u, v, width, height, SHEET, SHEET);
    }

    /** How many pixels of the batch bar are lit: it fills towards the next batch. */
    private int batchFill() {
        int ticks = menu.ticksToBatch();
        if (ticks == SinkBlockEntity.NO_BATCH) {
            return 0;
        }
        int done = SinkRates.BATCH_TICKS - Math.clamp(ticks, 0, SinkRates.BATCH_TICKS);
        return done * BAR_FILL / SinkRates.BATCH_TICKS;
    }

    /** The fan frame: still while idle, alternating faster with a taller chimney while active. */
    private int fanFrame(int height) {
        if (!menu.active()) {
            return 0;
        }
        long frameMillis = Math.max(30, Math.round(110 / (1 + 0.6 * height)));
        return (int) (Util.getMillis() / frameMillis % 2);
    }

    /** A sideways tank: the fluid's still texture fills from the left, tiled and tinted. */
    private static void drawFluid(GuiGraphicsExtractor graphics, int x, int y, FluidStack fluid, int capacity) {
        int width = SinkMenu.TANK_WIDTH - 2;
        int height = SinkMenu.TANK_HEIGHT - 2;
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        int filled = (int) Math.min(width, Math.max(1, (long) fluid.getAmount() * width / capacity));
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        int tint = FluidSprites.tint(fluid);
        graphics.enableScissor(x, y, x + filled, y + height);
        for (int tileX = x; tileX < x + filled; tileX += TILE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tileX, y, TILE, height, tint);
        }
        graphics.disableScissor();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT, true);
        Component chimney = Component.translatable("gui.servus_foundation.sink.chimney");
        graphics.text(font, chimney, PIP_X - 3 - font.width(chimney), titleLabelY, DIM, true);
        graphics.text(font, ratesLine(), SinkMenu.BUFFER_X, SinkMenu.RATES_Y, TEXT, true);
        if (menu.upgradeJustRefused()) {
            graphics.text(font, Component.translatable("gui.servus_foundation.sink.upgrade_refused"), inventoryLabelX, inventoryLabelY,
                    WARN, true);
        } else {
            graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DIM, true);
        }
    }

    /** "4/s  1,000 mB/s  100 FE/t", or one Unlimited line. */
    private Component ratesLine() {
        if (menu.itemRate() < 0 && menu.fluidRate() < 0 && menu.energyRate() < 0) {
            return Component.translatable("gui.servus_foundation.sink.all_unlimited");
        }
        return Component.empty().append(rate(menu.itemRate(), "items", false)).append("   ")
                .append(rate(menu.fluidRate(), "fluid", false)).append("   ")
                .append(rate(menu.energyRate(), "energy", false));
    }

    /** "16/s" or "16 per second", or Unlimited for -1. */
    private static Component rate(int value, String unit, boolean longForm) {
        if (value < 0) {
            return Component.translatable("gui.servus_foundation.sink.unlimited");
        }
        String key = "gui.servus_foundation.sink.unit." + unit + (longForm ? ".long" : "");
        return Component.translatable(key, "%,d".formatted(value));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        List<Component> tooltip = tooltipAt(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    /** The tooltip for the tanks, locked slots, empty upgrade slots, the bar, the pips and the rates line. */
    private List<Component> tooltipAt(int mouseX, int mouseY) {
        for (int tank = 0; tank < menu.maxRows(); tank++) {
            if (isHovering(SinkMenu.TANK_X - 1, tankY(tank) - 1, SinkMenu.TANK_WIDTH, SinkMenu.TANK_HEIGHT, mouseX, mouseY)) {
                return tankTooltip(tank);
            }
        }
        for (int index = 0; index < menu.bufferSlots(); index++) {
            Slot slot = menu.getSlot(index);
            if (!menu.isSlotOpen(index) && isHovering(slot.x, slot.y, TILE, TILE, mouseX, mouseY)) {
                return List.of(Component.translatable("gui.servus_foundation.sink.locked"));
            }
        }
        for (int index = 0; index < menu.upgradeSlots(); index++) {
            Slot slot = menu.getSlot(menu.bufferSlots() + index);
            if (!slot.hasItem() && isHovering(slot.x, slot.y, TILE, TILE, mouseX, mouseY)) {
                return List.of(Component.translatable("gui.servus_foundation.sink.upgrade_slot"));
            }
        }
        if (isHovering(SinkMenu.BAR_X, SinkMenu.BAR_Y, SinkMenu.BAR_WIDTH, 4, mouseX, mouseY)) {
            int ticks = menu.ticksToBatch();
            return List.of(ticks == SinkBlockEntity.NO_BATCH
                    ? Component.translatable("gui.servus_foundation.sink.batch.idle")
                    : Component.translatable("gui.servus_foundation.sink.batch", "%.1f".formatted(ticks / 20.0)));
        }
        if (isHovering(PIP_X, PIP_Y, PIPS * 7, PIP_HEIGHT, mouseX, mouseY)) {
            return List.of(Component.translatable("gui.servus_foundation.sink.height", menu.chimneyHeight()),
                    Component.translatable("gui.servus_foundation.sink.taller"));
        }
        if (isHovering(SinkMenu.BUFFER_X, SinkMenu.RATES_Y - 1, SinkMenu.BAR_WIDTH, 10, mouseX, mouseY)) {
            return List.of(Component.translatable("gui.servus_foundation.sink.items", rate(menu.itemRate(), "items", true)),
                    Component.translatable("gui.servus_foundation.sink.fluid", rate(menu.fluidRate(), "fluid", true)),
                    Component.translatable("gui.servus_foundation.sink.energy", rate(menu.energyRate(), "energy", true)));
        }
        return List.of();
    }

    private List<Component> tankTooltip(int tank) {
        if (tank >= menu.openRows()) {
            return List.of(Component.translatable("gui.servus_foundation.sink.locked"));
        }
        FluidStack fluid = menu.fluid(tank);
        Component name = fluid.isEmpty() ? Component.translatable("gui.servus_foundation.sink.tank.empty") : fluid.getHoverName();
        return List.of(name, Component.translatable("gui.servus_foundation.sink.tank",
                "%,d".formatted(fluid.getAmount()), "%,d".formatted(menu.tankCapacity())));
    }

    /** The upgrade tab right of the panel counts as inside, so clicks there never throw the carried stack. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        boolean onTab = mouseX >= left + SinkMenu.TAB_X && mouseX < left + SinkMenu.TAB_X + SinkMenu.TAB_WIDTH
                && mouseY >= top + SinkMenu.TAB_Y && mouseY < top + SinkMenu.TAB_Y + TAB_HEIGHT;
        return !onTab && super.hasClickedOutside(mouseX, mouseY, left, top);
    }
}
