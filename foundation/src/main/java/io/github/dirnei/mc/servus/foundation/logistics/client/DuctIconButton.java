package io.github.dirnei.mc.servus.foundation.logistics.client;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * A small framed button of the endpoint GUI showing an icon instead of text: a
 * 16 px item, a 16 px texture or a short label. A toggle draws its on state
 * with a light face and a green bar under the icon, its off state darker with
 * the icon dimmed. A disabled button is dimmed further and ignores clicks but
 * still has its tooltip, which the screen draws from {@link #tooltip()}.
 */
final class DuctIconButton extends AbstractButton {

    /** Draws the button's content into the box {@code x, y, width, height}, absolute. */
    @FunctionalInterface
    interface Icon {
        void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height);

        /** An item icon, centred. */
        static Icon item(ItemStack stack) {
            return (graphics, x, y, width, height) -> graphics.item(stack, x + (width - 16) / 2, y + (height - 16) / 2);
        }

        /** A 16 by 16 texture file such as a block texture, centred. */
        static Icon texture(Identifier texture) {
            return (graphics, x, y, width, height) -> graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x + (width - 16) / 2, y + (height - 16) / 2, 0, 0, 16, 16, 16, 16);
        }

        /** A trash can 8 px wide and 9 px high, centred: handle, lid and a ribbed body. */
        static Icon trash() {
            return (graphics, x, y, width, height) -> {
                int left = x + (width - 8) / 2;
                int top = y + (height - 9) / 2;
                graphics.fill(left + 3, top, left + 5, top + 1, TRASH_DARK);
                graphics.fill(left, top + 1, left + 8, top + 2, TRASH_DARK);
                graphics.fill(left + 1, top + 3, left + 7, top + 9, TRASH_DARK);
                graphics.fill(left + 2, top + 4, left + 3, top + 8, TRASH_LIGHT);
                graphics.fill(left + 5, top + 4, left + 6, top + 8, TRASH_LIGHT);
            };
        }

        /** A short label, centred. */
        static Icon label(Component text) {
            return (graphics, x, y, width, height) -> {
                Font font = Minecraft.getInstance().font;
                graphics.text(font, text, x + (width - font.width(text)) / 2, y + (height - font.lineHeight) / 2 + 1, TEXT, false);
            };
        }
    }

    private static final int TEXT = 0xFF202020;
    private static final int TRASH_DARK = 0xFF3A3A3A;
    private static final int TRASH_LIGHT = 0xFF9A9A9A;
    private static final int DARK = 0xFF373737;
    private static final int FACE_ON = 0xFFC8C8C8;
    private static final int FACE_OFF = 0xFF8B8B8B;
    private static final int FACE_HOVER = 0xFFDDDDDD;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int ON_BAR = 0xFF3FAF3F;
    private static final int DIM = 0x90707070;
    private static final int DISABLED_DIM = 0xC0606060;

    private final Supplier<Icon> icon;
    private final BooleanSupplier lit;
    private final BooleanSupplier enabled;
    private final Supplier<List<Component>> tooltip;
    private final Runnable onPress;
    private final boolean toggle;

    private DuctIconButton(int x, int y, int width, int height, Supplier<Icon> icon, boolean toggle, BooleanSupplier lit,
                           BooleanSupplier enabled, Supplier<List<Component>> tooltip, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.icon = icon;
        this.toggle = toggle;
        this.lit = lit;
        this.enabled = enabled;
        this.tooltip = tooltip;
        this.onPress = onPress;
    }

    /** A toggle whose on state is {@code lit}; it works only while {@code enabled}. */
    static DuctIconButton toggle(int x, int y, int width, int height, Supplier<Icon> icon, BooleanSupplier lit,
                                 BooleanSupplier enabled, Supplier<List<Component>> tooltip, Runnable onPress) {
        return new DuctIconButton(x, y, width, height, icon, true, lit, enabled, tooltip, onPress);
    }

    /** A plain button without an on and off state. */
    static DuctIconButton plain(int x, int y, int width, int height, Supplier<Icon> icon, Supplier<List<Component>> tooltip, Runnable onPress) {
        return new DuctIconButton(x, y, width, height, icon, false, () -> true, () -> true, tooltip, onPress);
    }

    List<Component> tooltip() {
        return tooltip.get();
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (enabled.getAsBoolean()) {
            onPress.run();
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean usable = enabled.getAsBoolean();
        boolean on = lit.getAsBoolean();
        int x0 = getX();
        int y0 = getY();
        int face = usable && isHovered() ? FACE_HOVER : on ? FACE_ON : FACE_OFF;
        graphics.fill(x0, y0, x0 + width, y0 + height, DARK);
        graphics.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + height - 1, face);
        graphics.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + 2, LIGHT);
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y0 + height - 1, LIGHT);
        icon.get().draw(graphics, x0, y0, width, height);
        if (!usable) {
            graphics.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + height - 1, DISABLED_DIM);
        } else if (toggle && !on) {
            graphics.fill(x0 + 2, y0 + 2, x0 + width - 1, y0 + height - 1, DIM);
        }
        if (toggle && on && usable) {
            graphics.fill(x0 + 2, y0 + height - 3, x0 + width - 2, y0 + height - 1, ON_BAR);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public Component getMessage() {
        List<Component> lines = tooltip.get();
        return lines.isEmpty() ? Component.empty() : lines.getFirst();
    }
}
