package io.github.dirnei.mc.servus.shuttles.client;

import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Greys a button out with the reason as its tooltip. */
final class GreyOut {

    private GreyOut() {
    }

    /**
     * Greys the button out with the reason as its tooltip, or enables it
     * without one; the tooltip is only replaced when the reason changes.
     *
     * @return the reason now shown
     */
    static String apply(Button button, String shown, String reason) {
        button.active = reason == null;
        if (!Objects.equals(shown, reason)) {
            button.setTooltip(reason == null ? null : Tooltip.create(Component.translatable(reason)));
        }
        return reason;
    }
}
