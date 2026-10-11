package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The crop slot and harvest only toggle, the field's numbers, a water bar, the player inventory. */
public class FieldHeadScreen extends AbstractContainerScreen<FieldHeadMenu> {

    /** The plain panel of the garage GUI; slot frame and water bar are drawn here. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/shuttle_garage.png");
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_DUE = 0x2E7D32;
    private static final int COLOUR_WARN = 0xA04000;
    private static final int COLOUR_WATER = 0xFF3F76E4;
    private static final int COLOUR_DRY = 0xFF9A8F80;
    private static final int COLOUR_RIPE = 0xFFD8B33A;
    private static final int COLOUR_MARK = 0xFF202020;
    private static final int RIPE_X = 72;
    private static final int RIPE_W = 74;
    private static final int RIPE_H = 7;
    private static final int BAR_X = 156;
    private static final int BAR_Y = 18;
    private static final int BAR_W = 12;
    private static final int BAR_H = 64;
    private static final int LINES_Y = 42;
    /** Rows the inventory moves down for the corrosion row; the panel texture is drawn in two parts. */
    private static final int SHIFT = FieldHeadMenu.INVENTORY_Y - 96;
    private static final int SPLIT = 84;
    private static final int CORROSION_TEXT_X = 30;

    private Button toggle;
    private Button down;
    private Button up;

    public FieldHeadScreen(FieldHeadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 178 + SHIFT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = FieldHeadMenu.INVENTORY_Y - 11;
        toggle = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FieldHeadMenu.BUTTON_HARVEST_ONLY);
            }
        }).bounds(leftPos + 30, topPos + FieldHeadMenu.CROP_Y + 1, 92, 14).build());
        down = addRenderableWidget(Button.builder(Component.literal("-"), button -> press(FieldHeadMenu.BUTTON_THRESHOLD_DOWN))
                .bounds(leftPos + 116, topPos + LINES_Y + 20, 14, 11).build());
        up = addRenderableWidget(Button.builder(Component.literal("+"), button -> press(FieldHeadMenu.BUTTON_THRESHOLD_UP))
                .bounds(leftPos + 132, topPos + LINES_Y + 20, 14, 11).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        toggle.setMessage(Component.translatable(menu.harvestOnly()
                ? "gui.servus_storage.field_head.harvest_only_on" : "gui.servus_storage.field_head.harvest_only_off"));
        boolean serving = menu.status() == FieldStatus.SERVING;
        down.visible = serving;
        up.visible = serving;
        super.render(graphics, mouseX, mouseY, partialTick);
        int barLeft = leftPos + BAR_X;
        int barTop = topPos + BAR_Y;
        if (mouseX >= barLeft && mouseX < barLeft + BAR_W && mouseY >= barTop && mouseY < barTop + BAR_H) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.servus_storage.field_head.water", menu.water(), menu.capacity()),
                    Component.translatable(menu.hydrated() ? "gui.servus_storage.field_head.hydrated" : "gui.servus_storage.field_head.dry")),
                    mouseX, mouseY);
        }
        int ripeLeft = leftPos + RIPE_X;
        int ripeTop = topPos + LINES_Y + 10;
        if (serving && mouseX >= ripeLeft && mouseX < ripeLeft + RIPE_W && mouseY >= ripeTop && mouseY < ripeTop + RIPE_H) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.field_head.ripe_tooltip",
                    menu.ripe(), menu.planted(), percent(menu.ripe(), menu.planted()), menu.threshold()), mouseX, mouseY);
        }
        int rangeLeft = leftPos + FieldHeadMenu.RANGE_X;
        int rangeTop = topPos + FieldHeadMenu.CROP_Y;
        if (menu.getCarried().isEmpty() && !menu.slots.get(FieldHeadMenu.RANGE_SLOT).hasItem()
                && mouseX >= rangeLeft && mouseX < rangeLeft + 16 && mouseY >= rangeTop && mouseY < rangeTop + 16) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.field_head.range_slot", menu.radius()), mouseX, mouseY);
        }
        int slotLeft = leftPos + FieldHeadMenu.ANTI_CORROSION_X;
        int slotTop = topPos + FieldHeadMenu.ANTI_CORROSION_Y;
        if (menu.getCarried().isEmpty() && !menu.slots.get(FieldHeadMenu.ANTI_CORROSION_SLOT).hasItem()
                && mouseX >= slotLeft && mouseX < slotLeft + 16 && mouseY >= slotTop && mouseY < slotTop + 16) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.field_head.anti_corrosion_slot"), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, SPLIT);
        graphics.blit(TEXTURE, leftPos, topPos + SPLIT, 0, SPLIT - SHIFT, imageWidth, imageHeight - SPLIT);
        sunken(graphics, leftPos + FieldHeadMenu.CROP_X - 1, topPos + FieldHeadMenu.CROP_Y - 1, 18, 18);
        sunken(graphics, leftPos + FieldHeadMenu.ANTI_CORROSION_X - 1, topPos + FieldHeadMenu.ANTI_CORROSION_Y - 1, 18, 18);
        sunken(graphics, leftPos + FieldHeadMenu.RANGE_X - 1, topPos + FieldHeadMenu.CROP_Y - 1, 18, 18);
        int barLeft = leftPos + BAR_X;
        int barTop = topPos + BAR_Y;
        sunken(graphics, barLeft - 1, barTop - 1, BAR_W + 2, BAR_H + 2);
        int capacity = Math.max(1, menu.capacity());
        int filled = (int) ((long) BAR_H * Math.min(menu.water(), capacity) / capacity);
        if (filled > 0) {
            graphics.fill(barLeft, barTop + BAR_H - filled, barLeft + BAR_W, barTop + BAR_H, menu.hydrated() ? COLOUR_WATER : COLOUR_DRY);
        }
        if (menu.status() == FieldStatus.SERVING) {
            int ripeLeft = leftPos + RIPE_X;
            int ripeTop = topPos + LINES_Y + 10;
            sunken(graphics, ripeLeft - 1, ripeTop - 1, RIPE_W + 2, RIPE_H + 2);
            int ripeFill = menu.planted() <= 0 ? 0 : RIPE_W * Math.min(menu.ripe(), menu.planted()) / menu.planted();
            if (ripeFill > 0) {
                graphics.fill(ripeLeft, ripeTop, ripeLeft + ripeFill, ripeTop + RIPE_H, menu.harvestDue() ? 0xFF000000 | COLOUR_DUE : COLOUR_RIPE);
            }
            int mark = ripeLeft + Math.min(RIPE_W - 1, RIPE_W * menu.threshold() / 100);
            graphics.fill(mark, ripeTop - 2, mark + 1, ripeTop + RIPE_H + 2, COLOUR_MARK);
        }
    }

    private static int percent(int ripe, int planted) {
        return planted <= 0 ? 0 : 100 * ripe / planted;
    }

    private static void sunken(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int y = LINES_Y;
        FieldStatus status = menu.status();
        if (status != FieldStatus.SERVING) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.status." + status.name().toLowerCase(Locale.ROOT)),
                    8, y, COLOUR_WARN, false);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.size", menu.rails(), menu.plots(), menu.radius()), 8, y, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.ripe", menu.ripe(), menu.planted()), 8, y + 10, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.harvest_at", menu.threshold()), 8, y + 22, COLOUR_TEXT, false);
        FieldWork work = menu.work();
        if (work != null) {
            int colour = switch (work) {
                case WORKING, HARVEST_DUE, READY -> COLOUR_DUE;
                case GROWING, WAITING_RIPE -> COLOUR_TEXT;
                default -> COLOUR_WARN;
            };
            String worker = ClientFieldWorker.name();
            Component line = work == FieldWork.WORKING && !worker.isEmpty()
                    ? Component.translatable("gui.servus_storage.field_head.work.working_named", worker)
                    : Component.translatable("gui.servus_storage.field_head.work." + work.name().toLowerCase(Locale.ROOT));
            graphics.drawString(font, line, 8, y + 34, colour, false);
        }
        int stage = menu.corrosion();
        if (stage >= FieldCorrosion.COPPER) {
            Component corrosion = stage == FieldCorrosion.CORRODED
                    ? Component.translatable("gui.servus_storage.field_head.corroded")
                    : Component.translatable("gui.servus_storage.field_head.corrosion",
                            Component.translatable("gui.servus_storage.field_head.stage." + FieldCorrosion.name(stage)), menu.corrosionProgress());
            graphics.drawString(font, corrosion, CORROSION_TEXT_X, FieldHeadMenu.ANTI_CORROSION_Y + 4,
                    stage == FieldCorrosion.CORRODED ? COLOUR_WARN : COLOUR_TEXT, false);
        }
    }
}
