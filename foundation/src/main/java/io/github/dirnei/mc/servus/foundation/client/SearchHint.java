package io.github.dirnei.mc.servus.foundation.client;

import java.time.Duration;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** The hover tooltip of a search box that reads the search syntax: one short line per prefix, {@code -} and {@code |}. */
public final class SearchHint {

    private static final String KEY = "gui.servus_foundation.search_hint.";
    private static final String[] LINES = {"mod", "tag", "tooltip", "tab", "id", "not", "or"};
    private static final Duration DELAY = Duration.ofMillis(500);

    private SearchHint() {
    }

    /** The hint as one tooltip, a title then one line each. */
    public static Tooltip tooltip() {
        MutableComponent text = Component.translatable(KEY + "title");
        for (String line : LINES) {
            text.append("\n").append(Component.translatable(KEY + line).withColor(0xA0A0A0));
        }
        return Tooltip.create(text);
    }

    /** Shows the hint when the mouse rests on {@code box}. */
    public static void apply(AbstractWidget box) {
        box.setTooltip(tooltip());
        box.setTooltipDelay(DELAY);
    }
}
