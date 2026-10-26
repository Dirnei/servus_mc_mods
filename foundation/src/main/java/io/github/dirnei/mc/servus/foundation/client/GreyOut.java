package io.github.dirnei.mc.servus.foundation.client;

import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Greys a button out with the reason as its tooltip. Client only. */
public final class GreyOut {

    private GreyOut() {
    }

    /**
     * Greys the button out with the reason as its tooltip, or enables it
     * without one; the tooltip is only replaced when the reason changes.
     *
     * @param shown  the reason key shown now, null for none
     * @param reason the lang key of the reason, null while the button can be pressed
     * @return the reason now shown
     */
    public static String apply(Button button, String shown, String reason) {
        button.active = reason == null;
        if (!Objects.equals(shown, reason)) {
            button.setTooltip(reason == null ? null : Tooltip.create(Component.translatable(reason)));
        }
        return reason;
    }
}
