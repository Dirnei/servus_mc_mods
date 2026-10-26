package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import io.github.dirnei.mc.servus.foundation.client.Scrollbar;
import io.github.dirnei.mc.servus.foundation.logistics.channel.Channel;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelList;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ClientChannels;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.menu.CableChannelsPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.ClientCableChannels;
import io.github.dirnei.mc.servus.foundation.logistics.menu.CreateChannelPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.DeleteChannelPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.EditChannelPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetSideChannelPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The channel popup of the endpoint GUI for one line of the open side: a
 * search field, a toggle between the channels on this cable and all of the
 * team's channels, "+ New", and the matching channels with colour, icon and
 * name. A row picks its channel; the team's channels also carry a pencil
 * (edit) and a bin (delete, greyed with the use count while other loaded
 * sides use it). Editing and creating happen in a {@link ChannelForm} in the
 * same panel.
 */
public final class ChannelPopup {

    private static final int ROW = 18;
    private static final int ROWS = 6;
    private static final int PAD = 6;
    private static final int WIDTH = 164;
    private static final int SEARCH_Y = 18;
    private static final int TOGGLE_Y = 34;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int TOGGLE_WIDTH = 80;
    private static final int NEW_WIDTH = 50;
    private static final int LIST_Y = 52;
    private static final int BAR_GAP = 3;
    private static final int BAR_WIDTH = 6;
    private static final int LIST_WIDTH = WIDTH - 2 * PAD - BAR_GAP - BAR_WIDTH;
    private static final int HEIGHT = LIST_Y + ROWS * ROW + PAD;
    private static final int ICON_BOX = 10;
    private static final int SELECTED = 0xFF8FC08F;
    private static final int HOVERED = 0x60FFFFFF;
    private static final int SLOT = 0xFF8B8B8B;

    private final UtilityDuctMenu menu;
    private final ChannelForm form = new ChannelForm();
    private Line line;
    private boolean open;
    private boolean allChannels;
    private EditBox search;
    private final Scrollbar bar = new Scrollbar();
    private int areaLeft;
    private int areaTop;
    private int areaWidth;
    private int areaHeight;
    private int x;
    private int y;

    public ChannelPopup(UtilityDuctMenu menu) {
        this.menu = menu;
    }

    /** The GUI area the popup centres on and dims, absolute. */
    public void setArea(int left, int top, int width, int height) {
        areaLeft = left;
        areaTop = top;
        areaWidth = width;
        areaHeight = height;
        x = left + (width - WIDTH) / 2;
        y = top + (height - HEIGHT) / 2;
        if (search != null) {
            search.setPosition(x + PAD, y + SEARCH_Y);
        }
        form.setArea(x, y, WIDTH, HEIGHT, left, top, width, height);
        bar.setArea(listX() + LIST_WIDTH + BAR_GAP + 1, listY() + 1, Scrollbar.NARROW, ROWS * ROW - 2);
    }

    public boolean isOpen() {
        return open;
    }

    /** The line whose channel the popup sets, null while closed. */
    public Line line() {
        return open ? line : null;
    }

    /** Opens on "This cable" for a line of the open side. */
    public void open(Line line) {
        this.line = line;
        open = true;
        allChannels = false;
        bar.mouseReleased();
        bar.setOffset(0);
        search = new EditBox(Minecraft.getInstance().font, x + PAD, y + SEARCH_Y, WIDTH - 2 * PAD, 12,
                Component.translatable("gui.servus_foundation.picker.search"));
        search.setHint(Component.translatable("gui.servus_foundation.picker.search"));
        search.setMaxLength(ChannelList.MAX_NAME);
        search.setResponder(text -> bar.setOffset(0));
        search.setFocused(true);
    }

    public void close() {
        open = false;
        form.close();
        search = null;
    }

    private Direction side() {
        return menu.side();
    }

    private UUID team() {
        CableChannelsPayload cable = ClientCableChannels.get();
        if (cable != null) {
            return cable.team();
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? new UUID(0, 0) : minecraft.player.getUUID();
    }

    private List<Channel> rows() {
        ChannelList list = ClientChannels.list();
        Set<Integer> used = null;
        if (!allChannels) {
            CableChannelsPayload cable = ClientCableChannels.get();
            used = cable == null ? Set.of() : cable.onCable(line);
        }
        return ChannelList.matching(list.forTeam(team()), search.getValue(), used);
    }

    private int current() {
        return menu.duct().channel(line, side());
    }

    private int uses(int id) {
        CableChannelsPayload cable = ClientCableChannels.get();
        return cable == null ? 0 : cable.loadedUses(id);
    }

    private boolean deletable(int id) {
        boolean openSideUses = current() == id && menu.duct().hasArm(line, side()) && menu.duct().isEndpoint(side());
        return ChannelList.deletable(id, uses(id), openSideUses ? 1 : 0);
    }

    private int listX() {
        return x + PAD;
    }

    private int listY() {
        return y + LIST_Y;
    }

    private int pencilX() {
        return listX() + LIST_WIDTH - 2 * (ICON_BOX + 4);
    }

    private int trashX() {
        return listX() + LIST_WIDTH - (ICON_BOX + 4);
    }

    private int newX() {
        return x + WIDTH - PAD - NEW_WIDTH;
    }

    /** The index into {@code rows} under the mouse, or -1. */
    private int rowAt(List<Channel> rows, double mouseX, double mouseY) {
        if (!PopupButtons.over(mouseX, mouseY, listX(), listY(), LIST_WIDTH, ROWS * ROW)) {
            return -1;
        }
        int index = bar.offset() + (int) ((mouseY - listY()) / ROW);
        return index < rows.size() ? index : -1;
    }

    private boolean overPencil(int row, double mouseX, double mouseY) {
        return PopupButtons.over(mouseX, mouseY, pencilX() - 1, listY() + row * ROW + 3, ICON_BOX + 2, ICON_BOX + 2);
    }

    private boolean overTrash(int row, double mouseX, double mouseY) {
        return PopupButtons.over(mouseX, mouseY, trashX() - 1, listY() + row * ROW + 3, ICON_BOX + 2, ICON_BOX + 2);
    }

    /** The popup, while open, over everything; call at the end of {@code extractRenderState}. */
    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            return;
        }
        PopupPanel.draw(graphics, areaLeft, areaTop, areaWidth, areaHeight, x, y, WIDTH, HEIGHT);
        if (form.isOpen()) {
            form.render(graphics, font, mouseX, mouseY, partialTick);
            return;
        }
        graphics.text(font, Component.translatable("gui.servus_foundation.channel.popup",
                Component.translatable("gui.servus_foundation.line." + line.id())), x + PAD, y + 5, 0xFF404040, false);
        search.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Component toggle = Component.translatable(allChannels ? "gui.servus_foundation.channel.all" : "gui.servus_foundation.channel.this_cable");
        PopupButtons.button(graphics, font, x + PAD, y + TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT, toggle,
                PopupButtons.over(mouseX, mouseY, x + PAD, y + TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT), true);
        PopupButtons.button(graphics, font, newX(), y + TOGGLE_Y, NEW_WIDTH, TOGGLE_HEIGHT, Component.translatable("gui.servus_foundation.channel.add"),
                PopupButtons.over(mouseX, mouseY, newX(), y + TOGGLE_Y, NEW_WIDTH, TOGGLE_HEIGHT), true);
        List<Channel> rows = rows();
        bar.setContent(rows.size(), ROWS);
        int offset = bar.offset();
        int lx = listX();
        int ly = listY();
        PopupPanel.bordered(graphics, lx, ly, LIST_WIDTH, ROWS * ROW, PopupButtons.DARK, SLOT);
        int hovered = rowAt(rows, mouseX, mouseY);
        int current = current();
        Component tooltip = null;
        for (int row = 0; row < ROWS && offset + row < rows.size(); row++) {
            Channel channel = rows.get(offset + row);
            int top = ly + row * ROW;
            if (channel.id() == current) {
                graphics.fill(lx, top, lx + LIST_WIDTH, top + ROW, SELECTED);
            }
            ChannelDisplay.swatch(graphics, lx + 3, top + 4, ICON_BOX, channel.argb());
            int textX = lx + 17;
            ItemStack icon = ChannelDisplay.icon(channel);
            if (!icon.isEmpty()) {
                graphics.item(icon, textX, top + 1);
                textX += 18;
            }
            int textRight = channel.isDefault() ? lx + LIST_WIDTH - 3 : pencilX() - 3;
            graphics.text(font, font.plainSubstrByWidth(ChannelDisplay.name(channel).getString(), textRight - textX), textX, top + 5,
                    PopupButtons.TEXT, false);
            if (!channel.isDefault()) {
                boolean canDelete = deletable(channel.id());
                PopupButtons.pencil(graphics, pencilX(), top + 4, overPencil(row, mouseX, mouseY) ? 0xFFFFFFFF : PopupButtons.TEXT);
                PopupButtons.trash(graphics, trashX(), top + 4,
                        !canDelete ? PopupButtons.GREY : overTrash(row, mouseX, mouseY) ? 0xFFFFFFFF : PopupButtons.TEXT);
                if (overPencil(row, mouseX, mouseY)) {
                    tooltip = Component.translatable("gui.servus_foundation.channel.edit");
                } else if (overTrash(row, mouseX, mouseY)) {
                    tooltip = canDelete
                            ? Component.translatable("gui.servus_foundation.channel.delete")
                            : Component.translatable("gui.servus_foundation.channel.in_use", uses(channel.id()));
                }
            }
            if (offset + row == hovered) {
                graphics.fill(lx, top, lx + LIST_WIDTH, top + ROW, HOVERED);
            }
        }
        bar.render(graphics, mouseX, mouseY);
        if (tooltip != null) {
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    /** While open, every click goes here; a click outside the panel closes the popup. */
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!open) {
            return false;
        }
        if (form.isOpen()) {
            return form.mouseClicked(event, doubleClick);
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (bar.mouseClicked(mouseX, mouseY, event.button())) {
            return true;
        }
        if (search.mouseClicked(event, doubleClick)) {
            search.setFocused(true);
            return true;
        }
        if (PopupButtons.over(mouseX, mouseY, x + PAD, y + TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            allChannels = !allChannels;
            bar.setOffset(0);
            return true;
        }
        if (PopupButtons.over(mouseX, mouseY, newX(), y + TOGGLE_Y, NEW_WIDTH, TOGGLE_HEIGHT)) {
            Line target = line;
            form.openNew(team(), (name, colour, icon) -> {
                send(new CreateChannelPayload(target.ordinal(), name, colour, icon == null ? "" : icon));
                close();
            }, () -> {
            });
            return true;
        }
        List<Channel> rows = rows();
        int index = rowAt(rows, mouseX, mouseY);
        if (index >= 0) {
            clickRow(rows.get(index), index - bar.offset(), mouseX, mouseY);
            return true;
        }
        if (!PopupButtons.over(mouseX, mouseY, x, y, WIDTH, HEIGHT)) {
            close();
        }
        return true;
    }

    private void clickRow(Channel channel, int row, double mouseX, double mouseY) {
        if (!channel.isDefault() && overPencil(row, mouseX, mouseY)) {
            form.openEdit(channel, (name, colour, icon) ->
                    send(new EditChannelPayload(channel.id(), name, colour, icon == null ? "" : icon)), () -> {
            });
            return;
        }
        if (!channel.isDefault() && overTrash(row, mouseX, mouseY)) {
            if (deletable(channel.id())) {
                send(new DeleteChannelPayload(line.ordinal(), channel.id()));
            }
            return;
        }
        send(new SetSideChannelPayload(line.ordinal(), channel.id()));
        close();
    }

    private static void send(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    public boolean mouseDragged(MouseButtonEvent event) {
        if (!open) {
            return false;
        }
        if (form.isOpen()) {
            form.mouseDragged(event);
        } else {
            bar.mouseDragged(event.y());
        }
        return true;
    }

    public boolean mouseReleased() {
        if (!open) {
            return false;
        }
        if (form.isOpen()) {
            form.mouseReleased();
        } else {
            bar.mouseReleased();
        }
        return true;
    }

    public boolean mouseScrolled(double amount) {
        if (!open) {
            return false;
        }
        if (form.isOpen()) {
            form.mouseScrolled(amount);
            return true;
        }
        bar.setContent(rows().size(), ROWS);
        bar.scrollBy(-(int) Math.signum(amount));
        return true;
    }

    /** Escape closes the form or the popup; every other key goes to the focused field while open. */
    public boolean keyPressed(KeyEvent event) {
        if (!open) {
            return false;
        }
        if (form.isOpen()) {
            return form.keyPressed(event);
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        search.keyPressed(event);
        return true;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!open) {
            return false;
        }
        if (form.isOpen()) {
            return form.charTyped(event);
        }
        search.charTyped(event);
        return true;
    }
}
