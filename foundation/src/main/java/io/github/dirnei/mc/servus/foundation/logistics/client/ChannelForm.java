package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.logistics.channel.Channel;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelColours;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelList;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ClientChannels;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The form of the channel popup for a new or an edited channel: a name field,
 * the 16 dye colours and an icon chosen with the item picker, plus Save and
 * Cancel. Name problems show from the synced list before anything is sent.
 * It draws inside the popup's panel; the popup forwards events while it is open.
 */
final class ChannelForm {

    /** What Save sends: the cleaned name, the colour index and the icon item id or null. */
    interface SaveAction {
        void save(String name, int colour, String icon);
    }

    private static final int PAD = 6;
    private static final int CELL = 18;
    private static final int SWATCH = 12;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 16;
    private static final int PROBLEM = 0xFFA02020;

    private final ItemPicker iconPicker;
    private boolean iconEntriesFilled;
    private boolean open;
    private boolean editing;
    private int editId;
    private UUID team;
    private int colour;
    private String icon;
    private SaveAction onSave;
    private Runnable onClose;
    private EditBox name;
    private int x;
    private int y;
    private int width;
    private int height;

    ChannelForm() {
        this.iconPicker = new ItemPicker(Component.translatable("gui.servus_foundation.channel.icon_picker"),
                entry -> icon = entry.isNone() ? null : entry.id());
    }

    /** The panel the form draws in, and the GUI area the icon picker centres on; absolute. */
    void setArea(int panelX, int panelY, int panelWidth, int panelHeight, int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        x = panelX;
        y = panelY;
        width = panelWidth;
        height = panelHeight;
        iconPicker.setArea(areaLeft, areaTop, areaWidth, areaHeight);
        if (name != null) {
            name.setPosition(x + PAD, y + 18);
        }
    }

    boolean isOpen() {
        return open;
    }

    /** Opens the form for a new channel of the team. */
    void openNew(UUID team, SaveAction onSave, Runnable onClose) {
        open(team, false, ChannelList.DEFAULT_ID, "", freeColour(team), null, onSave, onClose);
    }

    /** Opens the form to edit a channel. */
    void openEdit(Channel channel, SaveAction onSave, Runnable onClose) {
        open(channel.team(), true, channel.id(), channel.name(), channel.colour(), channel.icon(), onSave, onClose);
    }

    private void open(UUID team, boolean editing, int editId, String text, int colour, String icon, SaveAction onSave, Runnable onClose) {
        this.open = true;
        this.team = team;
        this.editing = editing;
        this.editId = editId;
        this.colour = colour;
        this.icon = icon;
        this.onSave = onSave;
        this.onClose = onClose;
        name = new EditBox(Minecraft.getInstance().font, x + PAD, y + 18, width - 2 * PAD, 12,
                Component.translatable("gui.servus_foundation.channel.name"));
        name.setHint(Component.translatable("gui.servus_foundation.channel.name"));
        name.setMaxLength(ChannelList.MAX_NAME);
        name.setValue(text);
        name.setFocused(true);
    }

    void close() {
        open = false;
        iconPicker.close();
        name = null;
    }

    private void cancel() {
        close();
        onClose.run();
    }

    /** The first dye colour from orange on that none of the team's channels has yet, else orange. */
    private static int freeColour(UUID team) {
        List<Channel> taken = ClientChannels.list().forTeam(team);
        for (int i = 1; i < ChannelColours.COUNT; i++) {
            int candidate = i;
            if (taken.stream().noneMatch(channel -> channel.colour() == candidate)) {
                return candidate;
            }
        }
        return 1;
    }

    private Optional<ChannelList.NameProblem> problem() {
        return ClientChannels.list().nameProblem(team, name.getValue(), editing ? editId : ChannelList.DEFAULT_ID);
    }

    private int swatchX(int index) {
        return x + PAD + (index % 8) * CELL;
    }

    private int swatchY(int index) {
        return y + 44 + (index / 8) * CELL;
    }

    private int iconY() {
        return y + 86;
    }

    private int buttonsY() {
        return y + height - PAD - BUTTON_HEIGHT;
    }

    private int saveX() {
        return x + PAD;
    }

    private int cancelX() {
        return x + width - PAD - BUTTON_WIDTH;
    }

    private int swatchAt(double mouseX, double mouseY) {
        for (int i = 0; i < ChannelColours.COUNT; i++) {
            if (PopupButtons.over(mouseX, mouseY, swatchX(i), swatchY(i), CELL, CELL)) {
                return i;
            }
        }
        return -1;
    }

    void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        boolean picking = iconPicker.isOpen();
        int mx = picking ? -1 : mouseX;
        int my = picking ? -1 : mouseY;
        Component title = Component.translatable(editing ? "gui.servus_foundation.channel.edit" : "gui.servus_foundation.channel.new");
        graphics.text(font, title, x + PAD, y + 5, 0xFF404040, false);
        name.extractRenderState(graphics, mx, my, partialTick);
        Optional<ChannelList.NameProblem> problem = problem();
        problem.ifPresent(p -> graphics.text(font,
                Component.translatable("gui.servus_foundation.channel.problem." + p.name().toLowerCase(Locale.ROOT), ChannelList.MAX_NAME),
                x + PAD, y + 33, PROBLEM, false));
        int hoveredSwatch = swatchAt(mx, my);
        for (int i = 0; i < ChannelColours.COUNT; i++) {
            int sx = swatchX(i);
            int sy = swatchY(i);
            if (i == colour) {
                graphics.fill(sx, sy, sx + CELL - 2, sy + CELL - 2, 0xFFFFFFFF);
            } else if (i == hoveredSwatch) {
                graphics.fill(sx, sy, sx + CELL - 2, sy + CELL - 2, 0xFF707070);
            }
            ChannelDisplay.swatch(graphics, sx + 2, sy + 2, SWATCH, ChannelColours.argb(i));
        }
        int iconWidth = width - 2 * PAD;
        boolean overIcon = PopupButtons.over(mx, my, x + PAD, iconY(), iconWidth, 18);
        PopupButtons.button(graphics, font, x + PAD, iconY(), iconWidth, 18, Component.empty(), overIcon, true);
        ItemStack stack = ChannelDisplay.iconOf(icon);
        int textX = x + PAD + 4;
        if (!stack.isEmpty()) {
            graphics.item(stack, x + PAD + 1, iconY() + 1);
            textX += 16;
        }
        Component iconLabel = stack.isEmpty()
                ? Component.translatable("gui.servus_foundation.channel.no_icon")
                : Component.translatable("gui.servus_foundation.channel.icon", stack.getHoverName());
        graphics.text(font, font.plainSubstrByWidth(iconLabel.getString(), x + PAD + iconWidth - 4 - textX), textX, iconY() + 5,
                PopupButtons.TEXT, false);
        boolean canSave = problem.isEmpty();
        PopupButtons.button(graphics, font, saveX(), buttonsY(), BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.servus_foundation.channel.save"),
                PopupButtons.over(mx, my, saveX(), buttonsY(), BUTTON_WIDTH, BUTTON_HEIGHT), canSave);
        PopupButtons.button(graphics, font, cancelX(), buttonsY(), BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.servus_foundation.channel.cancel"),
                PopupButtons.over(mx, my, cancelX(), buttonsY(), BUTTON_WIDTH, BUTTON_HEIGHT), true);
        if (picking) {
            iconPicker.renderPopup(graphics, font, mouseX, mouseY, partialTick);
            iconPicker.renderTooltip(graphics, font, mouseX, mouseY);
        } else if (hoveredSwatch >= 0) {
            graphics.setTooltipForNextFrame(font, Component.translatable("color.minecraft." + ChannelColours.dye(hoveredSwatch)), mouseX, mouseY);
        }
    }

    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (iconPicker.isOpen()) {
            return iconPicker.mouseClicked(event, doubleClick);
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (name.mouseClicked(event, doubleClick)) {
            name.setFocused(true);
            return true;
        }
        int swatch = swatchAt(mouseX, mouseY);
        if (swatch >= 0) {
            colour = swatch;
            return true;
        }
        if (PopupButtons.over(mouseX, mouseY, x + PAD, iconY(), width - 2 * PAD, 18)) {
            openIconPicker();
            return true;
        }
        if (PopupButtons.over(mouseX, mouseY, saveX(), buttonsY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            save();
            return true;
        }
        if (PopupButtons.over(mouseX, mouseY, cancelX(), buttonsY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            cancel();
        }
        return true;
    }

    private void openIconPicker() {
        if (!iconEntriesFilled) {
            List<ItemPicker.Entry> entries = new ArrayList<>(FilterPickerEntries.get());
            entries.removeIf(ItemPicker.Entry::isNone);
            entries.add(ItemPicker.Entry.none(Component.translatable("gui.servus_foundation.channel.no_icon")));
            iconPicker.setEntries(entries);
            iconEntriesFilled = true;
        }
        iconPicker.open(icon == null ? PickerList.NONE_ID : icon);
    }

    private void save() {
        if (problem().isPresent()) {
            return;
        }
        String cleaned = ChannelList.clean(name.getValue());
        SaveAction action = onSave;
        close();
        action.save(cleaned, colour, icon);
    }

    boolean mouseDragged(MouseButtonEvent event) {
        return iconPicker.mouseDragged(event.x(), event.y());
    }

    boolean mouseReleased() {
        return iconPicker.mouseReleased();
    }

    boolean mouseScrolled(double amount) {
        return iconPicker.mouseScrolled(amount);
    }

    /** Escape leaves the icon picker or cancels the form; Enter saves; other keys go to the name field. */
    boolean keyPressed(KeyEvent event) {
        if (iconPicker.isOpen()) {
            return iconPicker.keyPressed(event);
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        name.keyPressed(event);
        return true;
    }

    boolean charTyped(CharacterEvent event) {
        if (iconPicker.isOpen()) {
            return iconPicker.charTyped(event);
        }
        name.charTyped(event);
        return true;
    }
}
