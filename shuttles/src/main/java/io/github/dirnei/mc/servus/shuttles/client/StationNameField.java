package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.site.SiteName;
import io.github.dirnei.mc.servus.shuttles.station.RenameStationPayload;
import io.github.dirnei.mc.servus.shuttles.station.StationBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The title row of a station GUI: a borderless field holding the station's name,
 * showing its numbered label while unnamed, with the number at the row's
 * right end while named. The name is sent when the field loses focus, on
 * Enter and when the GUI closes.
 */
final class StationNameField {

    /** Room at the right end of the title row for the number of a named station. */
    private static final int NUMBER_ROOM = 26;

    private final StationBlockEntity station;
    private final EditBox box;
    private String sent;
    private boolean wasFocused;

    StationNameField(Font font, int x, int y, int width, StationBlockEntity station, int colour) {
        this.station = station;
        box = new EditBox(font, x, y, width - NUMBER_ROOM, 10, Component.translatable("gui.servus_shuttles.station.name"));
        box.setMaxLength(SiteName.MAX_LENGTH);
        box.setHint(station.numberLabel());
        box.setBordered(false);
        box.setTextColor(colour);
        box.setTextShadow(false);
        box.setTooltip(Tooltip.create(Component.translatable("gui.servus_shuttles.station.name_hint")));
        box.setValue(station.name());
        sent = station.name();
    }

    EditBox box() {
        return box;
    }

    /** Draws "#3" right aligned at {@code right} while the field shows a name; the hint already carries the number. */
    void renderNumber(GuiGraphicsExtractor graphics, Font font, int right, int y, int colour) {
        if (station.number() > 0 && !SiteName.clean(box.getValue()).isEmpty()) {
            Component number = Component.literal("#" + station.number());
            graphics.text(font, number, right - font.width(number), y, colour, false);
        }
    }

    /** Sends the name once the field lost focus. Call every container tick. */
    void tick() {
        boolean focused = box.isFocused();
        if (wasFocused && !focused) {
            send();
        }
        wasFocused = focused;
    }

    /** Whether the field took the key; Escape sends the name and leaves the key to the screen. */
    boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            send();
            return false;
        }
        if (!box.isFocused()) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            send();
            box.setFocused(false);
            return true;
        }
        return box.keyPressed(event) || box.canConsumeInput();
    }

    void send() {
        String typed = box.getValue();
        if (!SiteName.clean(typed).equals(SiteName.clean(sent))) {
            sent = typed;
            ClientPacketDistributor.sendToServer(new RenameStationPayload(typed));
        }
    }
}
