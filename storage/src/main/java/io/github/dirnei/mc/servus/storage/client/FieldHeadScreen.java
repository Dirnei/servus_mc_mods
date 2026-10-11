package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu.Page;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Field Head GUI in three tabs on the left edge, drawn like the Utility
 * Duct tabs: Field (crop, harvest only, ripe share, threshold, work status),
 * Water (water bar, anti-corrosion slot, corrosion stage) and Upgrades (Range
 * Upgrade slot, radius, field size). The player inventory is always below. A
 * tab whose page holds a problem gets a red marker.
 */
public class FieldHeadScreen extends AbstractContainerScreen<FieldHeadMenu> {

    /** The plain panel of the garage GUI; slot frames and bars are drawn on it. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/shuttle_garage.png");
    private static final int MARGIN = 8;
    private static final int WIDTH = 160;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_DUE = 0x2E7D32;
    private static final int COLOUR_WARN = 0xA04000;
    private static final int COLOUR_WATER = 0xFF3F76E4;
    private static final int COLOUR_DRY = 0xFF9A8F80;
    private static final int COLOUR_RIPE = 0xFFD8B33A;
    private static final int COLOUR_MARK = 0xFF202020;
    private static final int COLOUR_PROBLEM = 0xFFD02020;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF373737;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_TOP = 10;
    private static final int TAB_STRIDE = 28;
    // Field page
    private static final int RIPE_Y = 40;
    private static final int RIPE_X = 60;
    private static final int RIPE_W = MARGIN + WIDTH - RIPE_X;
    private static final int RIPE_H = 7;
    private static final int THRESHOLD_Y = 53;
    private static final int WORK_Y = 66;
    // Water page
    private static final int WATER_Y = 20;
    private static final int BAR_Y = 31;
    private static final int BAR_H = 8;
    private static final int HYDRATED_Y = 43;
    private static final int CORROSION_TEXT_X = 30;
    // Upgrades page
    private static final int RADIUS_TEXT_X = 30;
    private static final int SIZE_Y = 44;

    private Button toggle;
    private Button down;
    private Button up;

    public FieldHeadScreen(FieldHeadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 178;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = FieldHeadMenu.INVENTORY_Y - 11;
        toggle = addRenderableWidget(Button.builder(Component.empty(), button -> press(FieldHeadMenu.BUTTON_HARVEST_ONLY))
                .bounds(leftPos + 30, topPos + FieldHeadMenu.CROP_Y + 1, 92, 14).build());
        down = addRenderableWidget(Button.builder(Component.literal("-"), button -> press(FieldHeadMenu.BUTTON_THRESHOLD_DOWN))
                .bounds(leftPos + 136, topPos + THRESHOLD_Y - 2, 14, 11).build());
        up = addRenderableWidget(Button.builder(Component.literal("+"), button -> press(FieldHeadMenu.BUTTON_THRESHOLD_UP))
                .bounds(leftPos + 152, topPos + THRESHOLD_Y - 2, 14, 11).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private boolean serving() {
        return menu.status() == FieldStatus.SERVING;
    }

    // Problems

    /** The work status is a warning, drawn in the warning colour. */
    private static boolean warning(FieldWork work) {
        return switch (work) {
            case WORKING, HARVEST_DUE, READY, GROWING, WAITING_RIPE -> false;
            default -> true;
        };
    }

    /** The problem the page shows, or null when it has none. */
    private Component problem(Page page) {
        switch (page) {
            case FIELD -> {
                if (!serving()) {
                    return statusLine();
                }
                FieldWork work = menu.work();
                return work != null && warning(work) ? workLine(work) : null;
            }
            case WATER -> {
                if (menu.corrosion() == FieldCorrosion.CORRODED) {
                    return Component.translatable("gui.servus_storage.field_head.corroded");
                }
                return serving() && !menu.hydrated() ? Component.translatable("gui.servus_storage.field_head.dry") : null;
            }
            default -> {
                return null;
            }
        }
    }

    private Component statusLine() {
        return Component.translatable("gui.servus_storage.field_head.status." + menu.status().name().toLowerCase(Locale.ROOT));
    }

    private Component workLine(FieldWork work) {
        String worker = ClientFieldWorker.name();
        return work == FieldWork.WORKING && !worker.isEmpty()
                ? Component.translatable("gui.servus_storage.field_head.work.working_named", worker)
                : Component.translatable("gui.servus_storage.field_head.work." + work.name().toLowerCase(Locale.ROOT));
    }

    // Tabs

    private int tabX() {
        return leftPos - TAB_WIDTH;
    }

    private int tabY(int index) {
        return topPos + TAB_TOP + index * TAB_STRIDE;
    }

    private Page tabAt(double mouseX, double mouseY) {
        for (Page page : Page.VALUES) {
            int x = tabX();
            int y = tabY(page.ordinal());
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
                return page;
            }
        }
        return null;
    }

    private static ItemStack icon(Page page) {
        return switch (page) {
            case FIELD -> new ItemStack(Items.WHEAT);
            case WATER -> new ItemStack(Items.WATER_BUCKET);
            case UPGRADES -> new ItemStack(ServusStorageItems.RANGE_UPGRADE.get());
        };
    }

    private void renderTabs(GuiGraphics graphics) {
        for (Page page : Page.VALUES) {
            boolean selected = page == menu.page();
            boolean problem = problem(page) != null;
            int x = tabX();
            int y = tabY(page.ordinal());
            int width = selected ? TAB_WIDTH + 2 : TAB_WIDTH;
            graphics.fill(x, y, x + width, y + TAB_HEIGHT, problem ? COLOUR_PROBLEM : PANEL_DARK);
            graphics.fill(x + 1, y + 1, x + width, y + TAB_HEIGHT - 1, selected ? PANEL : 0xFF9A9A9A);
            graphics.fill(x + 1, y + 1, x + width, y + 2, PANEL_LIGHT);
            if (problem) {
                graphics.fill(x + 1, y + 1, x + 3, y + TAB_HEIGHT - 1, COLOUR_PROBLEM);
            }
            graphics.renderFakeItem(icon(page), x + 6, y + 5);
        }
    }

    // Rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Page page = menu.page();
        boolean serving = serving();
        toggle.setMessage(Component.translatable(menu.harvestOnly()
                ? "gui.servus_storage.field_head.harvest_only_on" : "gui.servus_storage.field_head.harvest_only_off"));
        toggle.visible = page == Page.FIELD;
        down.visible = page == Page.FIELD && serving;
        up.visible = page == Page.FIELD && serving;
        super.render(graphics, mouseX, mouseY, partialTick);
        Page hovered = tabAt(mouseX, mouseY);
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.servus_storage.field_head.tab." + hovered.name().toLowerCase(Locale.ROOT)));
            Component problem = problem(hovered);
            if (problem != null) {
                lines.add(problem.copy().withColor(COLOUR_PROBLEM & 0xFFFFFF));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (page == Page.FIELD && serving && inside(mouseX, mouseY, RIPE_X, RIPE_Y, RIPE_W, RIPE_H)) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.field_head.ripe_tooltip",
                    menu.ripe(), menu.planted(), percent(menu.ripe(), menu.planted()), menu.threshold()), mouseX, mouseY);
        }
        if (page == Page.WATER && inside(mouseX, mouseY, MARGIN, BAR_Y, WIDTH, BAR_H)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.servus_storage.field_head.water", menu.water(), menu.capacity()),
                    Component.translatable(menu.hydrated() ? "gui.servus_storage.field_head.hydrated" : "gui.servus_storage.field_head.dry")),
                    mouseX, mouseY);
        }
        if (page == Page.UPGRADES && emptySlotHovered(FieldHeadMenu.RANGE_SLOT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.field_head.range_slot", menu.radius()), mouseX, mouseY);
        }
        if (page == Page.WATER && emptySlotHovered(FieldHeadMenu.ANTI_CORROSION_SLOT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.field_head.anti_corrosion_slot"), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private boolean emptySlotHovered(int index, int mouseX, int mouseY) {
        var slot = menu.slots.get(index);
        return menu.getCarried().isEmpty() && !slot.hasItem() && inside(mouseX, mouseY, slot.x, slot.y, 16, 16);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderTabs(graphics);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        switch (menu.page()) {
            case FIELD -> {
                sunken(graphics, leftPos + FieldHeadMenu.CROP_X - 1, topPos + FieldHeadMenu.CROP_Y - 1, 18, 18);
                if (serving()) {
                    int ripeLeft = leftPos + RIPE_X;
                    int ripeTop = topPos + RIPE_Y;
                    sunken(graphics, ripeLeft - 1, ripeTop - 1, RIPE_W + 2, RIPE_H + 2);
                    int ripeFill = menu.planted() <= 0 ? 0 : RIPE_W * Math.min(menu.ripe(), menu.planted()) / menu.planted();
                    if (ripeFill > 0) {
                        graphics.fill(ripeLeft, ripeTop, ripeLeft + ripeFill, ripeTop + RIPE_H, menu.harvestDue() ? 0xFF000000 | COLOUR_DUE : COLOUR_RIPE);
                    }
                    int mark = ripeLeft + Math.min(RIPE_W - 1, RIPE_W * menu.threshold() / 100);
                    graphics.fill(mark, ripeTop - 2, mark + 1, ripeTop + RIPE_H + 2, COLOUR_MARK);
                }
            }
            case WATER -> {
                int barLeft = leftPos + MARGIN;
                int barTop = topPos + BAR_Y;
                sunken(graphics, barLeft - 1, barTop - 1, WIDTH + 2, BAR_H + 2);
                int capacity = Math.max(1, menu.capacity());
                int filled = (int) ((long) WIDTH * Math.min(menu.water(), capacity) / capacity);
                if (filled > 0) {
                    graphics.fill(barLeft, barTop, barLeft + filled, barTop + BAR_H, menu.hydrated() ? COLOUR_WATER : COLOUR_DRY);
                }
                sunken(graphics, leftPos + FieldHeadMenu.ANTI_CORROSION_X - 1, topPos + FieldHeadMenu.ANTI_CORROSION_Y - 1, 18, 18);
            }
            case UPGRADES -> sunken(graphics, leftPos + FieldHeadMenu.RANGE_X - 1, topPos + FieldHeadMenu.RANGE_Y - 1, 18, 18);
        }
    }

    private static int percent(int ripe, int planted) {
        return planted <= 0 ? 0 : 100 * ripe / planted;
    }

    private static void sunken(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x, y, x + width, y + 1, PANEL_DARK);
        graphics.fill(x, y, x + 1, y + height, PANEL_DARK);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_LIGHT);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_LIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        switch (menu.page()) {
            case FIELD -> renderFieldLabels(graphics);
            case WATER -> renderWaterLabels(graphics);
            case UPGRADES -> renderUpgradeLabels(graphics);
        }
    }

    private void renderFieldLabels(GuiGraphics graphics) {
        if (!serving()) {
            wrapped(graphics, statusLine(), RIPE_Y, COLOUR_WARN);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.ripe", menu.ripe(), menu.planted()), MARGIN, RIPE_Y, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.harvest_at", menu.threshold()), MARGIN, THRESHOLD_Y, COLOUR_TEXT, false);
        FieldWork work = menu.work();
        if (work != null) {
            int colour = switch (work) {
                case WORKING, HARVEST_DUE, READY -> COLOUR_DUE;
                default -> warning(work) ? COLOUR_WARN : COLOUR_TEXT;
            };
            wrapped(graphics, workLine(work), WORK_Y, colour);
        }
    }

    private void renderWaterLabels(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.water", menu.water(), menu.capacity()), MARGIN, WATER_Y, COLOUR_TEXT, false);
        if (serving()) {
            boolean hydrated = menu.hydrated();
            graphics.drawString(font, Component.translatable(hydrated ? "gui.servus_storage.field_head.hydrated" : "gui.servus_storage.field_head.dry"),
                    MARGIN, HYDRATED_Y, hydrated ? COLOUR_DUE : COLOUR_WARN, false);
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

    private void renderUpgradeLabels(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.radius", menu.radius()),
                RADIUS_TEXT_X, FieldHeadMenu.RANGE_Y + 4, COLOUR_TEXT, false);
        if (!serving()) {
            wrapped(graphics, statusLine(), SIZE_Y, COLOUR_WARN);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.size", menu.rails(), menu.plots()), MARGIN, SIZE_Y, COLOUR_TEXT, false);
    }

    /** Draws a line wrapped to the panel width, at most two rows. */
    private void wrapped(GuiGraphics graphics, Component text, int y, int colour) {
        List<FormattedCharSequence> rows = font.split(text, WIDTH);
        for (int i = 0; i < Math.min(2, rows.size()); i++) {
            graphics.drawString(font, rows.get(i), MARGIN, y + i * 9, colour, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Page tab = tabAt(mouseX, mouseY);
        if (tab != null) {
            menu.setPage(tab);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
