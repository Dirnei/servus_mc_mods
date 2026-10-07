package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.station.CycleSidePayload;
import io.github.dirnei.mc.servus.storage.station.SideModes;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Six buttons, one per world side, that show a station's side modes and cycle them on click. */
public final class SideButtons {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 14;
    private static final int GAP = 1;

    private final Button[] buttons = new Button[Direction.values().length];
    private final Direction front;
    private String sides = "";

    public SideButtons(Screen screen, int x, int y, Direction front, Consumer<Button> add) {
        this.front = front;
        for (Direction direction : Direction.values()) {
            int i = direction.ordinal();
            Button button = Button.builder(label(direction, SideModes.Mode.OFF), b -> PacketDistributor.sendToServer(new CycleSidePayload(i)))
                    .bounds(x + i * (WIDTH + GAP), y, WIDTH, HEIGHT).build();
            buttons[i] = button;
            add.accept(button);
        }
    }

    private Component label(Direction direction, SideModes.Mode mode) {
        String letter = direction.getSerializedName().substring(0, 1).toUpperCase(Locale.ROOT);
        return Component.literal(letter + " " + mode.symbol());
    }

    /** Updates the labels from the six symbol string of the last sync. */
    public void refresh(String symbols) {
        if (symbols.equals(sides)) {
            return;
        }
        sides = symbols;
        for (Direction direction : Direction.values()) {
            buttons[direction.ordinal()].setMessage(label(direction, mode(direction)));
        }
    }

    private SideModes.Mode mode(Direction direction) {
        return direction.ordinal() < sides.length() ? SideModes.Mode.bySymbol(sides.charAt(direction.ordinal())) : SideModes.Mode.OFF;
    }

    /** Draws the tooltip of the hovered side button; returns whether one was drawn. */
    public boolean renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        for (Direction direction : Direction.values()) {
            if (buttons[direction.ordinal()].isHovered()) {
                Component side = Component.translatable("gui.servus_storage.station.side." + direction.getSerializedName());
                Component mode = Component.translatable("gui.servus_storage.station.mode." + mode(direction).name().toLowerCase(Locale.ROOT));
                Component text = Component.translatable("gui.servus_storage.station.side_line", side, mode);
                if (direction == front) {
                    text = text.copy().append(" ").append(Component.translatable("gui.servus_storage.station.front"));
                }
                graphics.renderTooltip(font, text, mouseX, mouseY);
                return true;
            }
        }
        return false;
    }
}
