package io.github.dirnei.mc.servus.logistics.client;

import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.logistics.duct.Line;
import io.github.dirnei.mc.servus.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The endpoint GUI of one duct side: a row per present line with the line's
 * item as icon, the mode button, eight channel buttons and a remove button;
 * the Item Pipe row adds the filter slots and the priority.
 */
public class UtilityDuctScreen extends AbstractContainerScreen<UtilityDuctMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "textures/gui/utility_duct.png");
    private static final int ITEM_ROW_Y = 18;
    private static final int PRIORITY_Y = 72;
    private static final int ENERGY_ROW_Y = 94;
    private static final int ICON_X = 8;
    private static final int MODE_X = 28;
    private static final int MODE_WIDTH = 58;
    private static final int CHANNEL_X = 90;
    private static final int CHANNEL_SIZE = 10;
    private static final int CHANNEL_STRIDE = 11;
    private static final int REMOVE_X = 180;
    private static final int REMOVE_WIDTH = 12;
    private static final int TEXT = 0x404040;

    private final Map<Line, Button> modeButtons = new EnumMap<>(Line.class);
    private int linesSeen = -1;

    public UtilityDuctScreen(UtilityDuctMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 204;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = UtilityDuctMenu.SLOT_X;
        this.inventoryLabelY = UtilityDuctMenu.INVENTORY_Y - 11;
        rebuildButtons();
    }

    private static int rowY(Line line) {
        return line == Line.ITEM ? ITEM_ROW_Y : ENERGY_ROW_Y;
    }

    private void rebuildButtons() {
        clearWidgets();
        modeButtons.clear();
        linesSeen = menu.duct().linesMask();
        for (Line line : Line.VALUES) {
            if (!menu.duct().has(line)) {
                continue;
            }
            int y = topPos + rowY(line);
            Button mode = Button.builder(Component.empty(), button -> press(UtilityDuctMenu.BUTTON_CYCLE_MODE + line.ordinal()))
                    .bounds(leftPos + MODE_X, y, MODE_WIDTH, 18).build();
            modeButtons.put(line, mode);
            addRenderableWidget(mode);
            for (int channel = 0; channel < DuctChannels.COUNT; channel++) {
                int id = UtilityDuctMenu.BUTTON_CHANNEL + line.ordinal() * DuctChannels.COUNT + channel;
                int current = channel;
                addRenderableWidget(new ChannelButton(leftPos + CHANNEL_X + channel * CHANNEL_STRIDE, y + 4, CHANNEL_SIZE, channel,
                        () -> menu.duct().channel(line, menu.side()) == current, () -> press(id)));
            }
            Button remove = Button.builder(Component.literal("x"), button -> press(UtilityDuctMenu.BUTTON_REMOVE_LINE + line.ordinal()))
                    .bounds(leftPos + REMOVE_X, y, REMOVE_WIDTH, 18).build();
            remove.setTooltip(Tooltip.create(Component.translatable("gui.servus_logistics.remove." + line.id())));
            addRenderableWidget(remove);
        }
        if (menu.duct().has(Line.ITEM)) {
            addRenderableWidget(Button.builder(Component.literal("-"), button -> press(UtilityDuctMenu.BUTTON_PRIORITY_DOWN))
                    .bounds(leftPos + ICON_X, topPos + PRIORITY_Y, 16, 16).build());
            addRenderableWidget(Button.builder(Component.literal("+"), button -> press(UtilityDuctMenu.BUTTON_PRIORITY_UP))
                    .bounds(leftPos + ICON_X + 72, topPos + PRIORITY_Y, 16, 16).build());
        }
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            menu.clickMenuButton(minecraft.player, id);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (linesSeen != menu.duct().linesMask()) {
            rebuildButtons();
        }
        for (Map.Entry<Line, Button> entry : modeButtons.entrySet()) {
            SideMode mode = menu.duct().mode(entry.getKey(), menu.side());
            entry.getValue().setMessage(Component.translatable("gui.servus_logistics.mode." + mode.name().toLowerCase(Locale.ROOT)));
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (Line line : Line.VALUES) {
            if (menu.duct().has(line)) {
                guiGraphics.renderFakeItem(new ItemStack(ServusLogisticsItems.lineItem(line)), leftPos + ICON_X, topPos + rowY(line) + 1);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        if (menu.duct().has(Line.ITEM)) {
            guiGraphics.drawString(font, Component.translatable("gui.servus_logistics.filter"), UtilityDuctMenu.SLOT_X, UtilityDuctMenu.FILTER_Y - 10, TEXT, false);
            guiGraphics.drawString(font, Component.translatable("gui.servus_logistics.priority", menu.priority()), ICON_X + 20, PRIORITY_Y + 4, TEXT, false);
        }
        if (menu.duct().lines().isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("gui.servus_logistics.no_lines"), ICON_X, ITEM_ROW_Y + 5, TEXT, false);
        }
    }
}
