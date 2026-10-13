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
 * Ender Anchor GUI: energy bar, two Anchor Upgrade slots, four arrows that
 * shift the area (greyed at the limit), the area size, the status line, the
 * team's load against the cap and the client only "Show range" toggle.
 */
public class EnderAnchorScreen extends MachineScreen {

    private static final String[] STATUS_KEYS = {"no_power", "active", "team_limit", "ftb_chunks", "no_owner"};
    private static final int BUTTON = 11;
    /** Arrow buttons in {@link AnchorArea.Shift} order: north, south, east, west. */
    private static final int[][] ARROWS = {{119, 16}, {119, 38}, {131, 27}, {107, 27}};
    private static final int TOGGLE_X = 8;
    private static final int TOGGLE_Y = 38;
    private static final int SPRITE_V = 170;
    private static final int TOGGLE_U = 44;
    private static final int TEXT = 0xFF404040;
    private static final int ALERT = 0xFFA03030;

    public EnderAnchorScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, MachineClient.ANCHOR);
    }

    private EnderAnchorMenu anchorMenu() {
        return (EnderAnchorMenu) menu;
    }

    private boolean shown() {
        EnderAnchorBlockEntity anchor = anchorMenu().anchor();
        return anchor.getLevel() != null && AnchorRangeView.isShown(anchor.getLevel().dimension(), anchor.getBlockPos());
    }

    private boolean inside(int x, int y, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + BUTTON && mouseY >= topPos + y && mouseY < topPos + y + BUTTON;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        AnchorArea area = anchorMenu().area();
        for (AnchorArea.Shift shift : AnchorArea.Shift.values()) {
            int[] at = ARROWS[shift.ordinal()];
            int state = !area.canShift(shift) ? 2 : inside(at[0], at[1], mouseX, mouseY) ? 1 : 0;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, layout.texture(), leftPos + at[0], topPos + at[1],
                    shift.ordinal() * BUTTON, SPRITE_V + state * BUTTON, BUTTON, BUTTON, 256, 256);
        }
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, layout.texture(), leftPos + TOGGLE_X, topPos + TOGGLE_Y,
                TOGGLE_U, SPRITE_V + (shown() ? BUTTON : 0), BUTTON, BUTTON, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        EnderAnchorMenu anchor = anchorMenu();
        int side = anchor.area().side();
        guiGraphics.text(font, Component.translatable("gui.servus_foundation.ender_anchor.area", side, side), 48, 22, TEXT, false);
        guiGraphics.text(font, Component.translatable("gui.servus_foundation.ender_anchor.show_range"), TOGGLE_X + BUTTON + 3, TOGGLE_Y + 2,
                TEXT, false);
        int status = anchor.status();
        String key = STATUS_KEYS[Math.max(0, Math.min(status, STATUS_KEYS.length - 1))];
        fitted(guiGraphics, Component.translatable("gui.servus_foundation.ender_anchor." + key), 52,
                status == EnderAnchorBlockEntity.STATUS_TEAM_LIMIT ? ALERT : TEXT);
        fitted(guiGraphics, Component.translatable("gui.servus_foundation.ender_anchor.team_load", anchor.teamLoad(), anchor.teamCap()),
                62, TEXT);
    }

    /** A line from the left edge to the energy bar, scaled down when it would run under the bar. */
    private void fitted(GuiGraphicsExtractor guiGraphics, Component line, int y, int color) {
        int available = energyX() - 3 - 8;
        int width = font.width(line);
        float scale = width > available ? (float) available / width : 1F;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(8, y + (1F - scale) * font.lineHeight / 2F);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.text(font, line, 0, 0, color, false);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (inside(TOGGLE_X, TOGGLE_Y, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_foundation.ender_anchor.show_range.tooltip"),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            AnchorArea area = anchorMenu().area();
            for (AnchorArea.Shift shift : AnchorArea.Shift.values()) {
                int[] at = ARROWS[shift.ordinal()];
                if (inside(at[0], at[1], event.x(), event.y())) {
                    if (area.canShift(shift) && minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, shift.ordinal());
                        click();
                    }
                    return true;
                }
            }
            EnderAnchorBlockEntity anchor = anchorMenu().anchor();
            if (inside(TOGGLE_X, TOGGLE_Y, event.x(), event.y()) && anchor.getLevel() != null) {
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
