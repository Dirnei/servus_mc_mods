package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.machine.anchor.AnchorArea;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.EnderAnchorMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ender Anchor GUI in the anchor's obsidian and purple: two Anchor Upgrade
 * slots, the area size, upkeep and team load, a 5 by 5 chunk map with the
 * current area lit and the anchor's chunk marked, four arrows round the map
 * that shift the area (greyed at the limit), the client only "Show range"
 * toggle, the energy bar and, only while inactive, the reason in red.
 * Positions match the texture drawn by the job script {@code anchor-range/gui.py}.
 */
public class EnderAnchorScreen extends MachineScreen {

    private static final String[] STATUS_KEYS = {"no_power", "active", "team_limit", "ftb_chunks", "no_owner"};
    private static final String KEY = "gui.servus_foundation.ender_anchor.";

    private static final int ARROW = 16;
    /** Arrow buttons in {@link AnchorArea.Shift} order: north, south, east, west. */
    private static final int[][] ARROWS = {{130, 16}, {130, 86}, {165, 51}, {95, 51}};
    private static final int ARROW_U = 206;
    private static final int ARROW_V = 88;

    /** The chunk map: top left of the first cell, 10 px cells, 5 by 5. */
    private static final int MAP_X = 113;
    private static final int MAP_Y = 34;
    private static final int CELL = 10;
    private static final int MAP_RADIUS = 2;
    private static final int LIT_U = 206;
    private static final int MARKER_U = 218;
    private static final int CELL_V = 168;

    private static final int ENERGY_LEFT = 186;
    private static final int ENERGY_TOP = 17;
    private static final int ENERGY_W = 10;
    private static final int ENERGY_H = 84;
    private static final int ENERGY_U = 206;

    private static final int TOGGLE = 11;
    private static final int TOGGLE_X = 8;
    private static final int TOGGLE_Y = 80;
    private static final int TOGGLE_U = 206;
    private static final int TOGGLE_V = 154;

    private static final int LINE_X = 8;
    private static final int VALUE_X = 46;
    /** Info lines end before the west arrow. */
    private static final int LINE_END = 92;
    private static final int AREA_Y = 42;
    private static final int UPKEEP_Y = 54;
    private static final int TEAM_Y = 66;
    private static final int STATUS_Y = 104;

    private static final int TITLE = 0xFFDCC8FA;
    private static final int LABEL = 0xFFB4A0D2;
    private static final int VALUE = 0xFFF0E6FF;
    private static final int ALERT = 0xFFFF6464;

    public EnderAnchorScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, MachineClient.ANCHOR, EnderAnchorMenu.WIDTH, EnderAnchorMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelX = EnderAnchorMenu.INVENTORY_X;
        inventoryLabelY = EnderAnchorMenu.INVENTORY_Y - 12;
    }

    private EnderAnchorMenu anchorMenu() {
        return (EnderAnchorMenu) menu;
    }

    @Override
    protected int energyX() {
        return ENERGY_LEFT;
    }

    @Override
    protected int energyY() {
        return ENERGY_TOP;
    }

    @Override
    protected int energyWidth() {
        return ENERGY_W;
    }

    @Override
    protected int energyHeight() {
        return ENERGY_H;
    }

    private boolean shown() {
        EnderAnchorBlockEntity anchor = anchorMenu().anchor();
        return anchor.getLevel() != null && AnchorRangeView.isShown(anchor.getLevel().dimension(), anchor.getBlockPos());
    }

    private boolean inside(int x, int y, int size, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + size && mouseY >= topPos + y && mouseY < topPos + y + size;
    }

    private void sprite(GuiGraphicsExtractor guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, layout.texture(), leftPos + x, topPos + y, u, v, width, height, 256, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        AnchorArea area = anchorMenu().area();
        for (int cz = -MAP_RADIUS; cz <= MAP_RADIUS; cz++) {
            for (int cx = -MAP_RADIUS; cx <= MAP_RADIUS; cx++) {
                if (area.contains(0, 0, cx, cz)) {
                    sprite(guiGraphics, cellX(cx), cellY(cz), LIT_U, CELL_V, CELL, CELL);
                }
            }
        }
        sprite(guiGraphics, cellX(0), cellY(0), MARKER_U, CELL_V, CELL, CELL);
        for (AnchorArea.Shift shift : AnchorArea.Shift.values()) {
            int[] at = ARROWS[shift.ordinal()];
            int state = !area.canShift(shift) ? 2 : inside(at[0], at[1], ARROW, mouseX, mouseY) ? 1 : 0;
            sprite(guiGraphics, at[0], at[1], ARROW_U + state * ARROW, ARROW_V + shift.ordinal() * ARROW, ARROW, ARROW);
        }
        sprite(guiGraphics, TOGGLE_X, TOGGLE_Y, TOGGLE_U + (shown() ? TOGGLE + 1 : 0), TOGGLE_V, TOGGLE, TOGGLE);
    }

    private static int cellX(int cx) {
        return MAP_X + (cx + MAP_RADIUS) * CELL;
    }

    private static int cellY(int cz) {
        return MAP_Y + (cz + MAP_RADIUS) * CELL;
    }

    /** A purple bar from the sheet, filled from the bottom. */
    @Override
    protected void extractEnergyBar(GuiGraphicsExtractor guiGraphics) {
        int filled = menu.energyScaled(ENERGY_H);
        if (filled > 0) {
            sprite(guiGraphics, ENERGY_LEFT, ENERGY_TOP + ENERGY_H - filled, ENERGY_U, ENERGY_H - filled, ENERGY_W, filled);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        EnderAnchorMenu anchor = anchorMenu();
        guiGraphics.text(font, title, titleLabelX, titleLabelY, TITLE, false);
        guiGraphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TITLE, false);
        int side = anchor.area().side();
        info(guiGraphics, "area", Component.translatable(KEY + "area.value", side, side), AREA_Y);
        info(guiGraphics, "upkeep", Component.translatable(KEY + "upkeep.value", anchor.upkeep()), UPKEEP_Y);
        info(guiGraphics, "team", Component.translatable(KEY + "team.value", anchor.teamLoad(), anchor.teamCap()), TEAM_Y);
        guiGraphics.text(font, Component.translatable(KEY + "show_range"), TOGGLE_X + TOGGLE + 3, TOGGLE_Y + 2, LABEL, false);
        int status = anchor.status();
        if (status != EnderAnchorBlockEntity.STATUS_ACTIVE) {
            String key = STATUS_KEYS[Math.max(0, Math.min(status, STATUS_KEYS.length - 1))];
            fitted(guiGraphics, Component.translatable(KEY + key), LINE_X, STATUS_Y, ENERGY_LEFT + ENERGY_W - LINE_X, ALERT);
        }
    }

    /** One info line: the label left, the value in its column, scaled down when it would run into the map. */
    private void info(GuiGraphicsExtractor guiGraphics, String label, Component value, int y) {
        guiGraphics.text(font, Component.translatable(KEY + label), LINE_X, y, LABEL, false);
        fitted(guiGraphics, value, VALUE_X, y, LINE_END - VALUE_X, VALUE);
    }

    private void fitted(GuiGraphicsExtractor guiGraphics, Component line, int x, int y, int available, int color) {
        int width = font.width(line);
        float scale = width > available ? (float) available / width : 1F;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y + (1F - scale) * font.lineHeight / 2F);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.text(font, line, 0, 0, color, false);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (inside(TOGGLE_X, TOGGLE_Y, TOGGLE, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable(KEY + "show_range.tooltip"), mouseX, mouseY);
        } else if (isHovering(LINE_X, TEAM_Y - 1, LINE_END - LINE_X, font.lineHeight + 1, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable(KEY + "team.tooltip"), mouseX, mouseY);
        } else if (isHovering(LINE_X, UPKEEP_Y - 1, LINE_END - LINE_X, font.lineHeight + 1, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable(KEY + "upkeep.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            AnchorArea area = anchorMenu().area();
            for (AnchorArea.Shift shift : AnchorArea.Shift.values()) {
                int[] at = ARROWS[shift.ordinal()];
                if (inside(at[0], at[1], ARROW, event.x(), event.y())) {
                    if (area.canShift(shift) && minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, shift.ordinal());
                        click();
                    }
                    return true;
                }
            }
            EnderAnchorBlockEntity anchor = anchorMenu().anchor();
            if (inside(TOGGLE_X, TOGGLE_Y, TOGGLE, event.x(), event.y()) && anchor.getLevel() != null) {
                AnchorRangeView.toggle(anchor.getLevel().dimension(), anchor.getBlockPos());
                click();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
