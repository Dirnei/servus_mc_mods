package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleBuild;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers.Kind;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The shuttle and install slots, the shuttle's tier with its upgrades and
 * tool, each with a button that takes one out, the next tier's build sheet
 * with what is in and what is missing, the supply slots and Build now.
 */
public class ShuttleWorkshopScreen extends AbstractContainerScreen<ShuttleWorkshopMenu> {

    private static final int COLOUR_PANEL = 0xFFC6C6C6;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_MISSING = 0xA02020;
    private static final int COLOUR_DONE = 0x207020;
    private static final int COLOUR_CLOSED = 0x80373737;
    private static final int INFO_X = 30;
    private static final int SHEET_X = 104;
    private static final int LINE = 11;

    private Button removeSpeed;
    private Button removeHandling;
    private Button removeTool;
    private Button buildNow;

    public ShuttleWorkshopScreen(ShuttleWorkshopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ShuttleWorkshopMenu.INVENTORY_Y - 11;
        int infoY = topPos + ShuttleWorkshopMenu.SHUTTLE_Y + LINE - 2;
        removeSpeed = addRenderableWidget(small(infoY, ShuttleWorkshopMenu.BUTTON_REMOVE_SPEED));
        removeHandling = addRenderableWidget(small(infoY + LINE, ShuttleWorkshopMenu.BUTTON_REMOVE_HANDLING));
        removeTool = addRenderableWidget(small(infoY + 2 * LINE, ShuttleWorkshopMenu.BUTTON_REMOVE_TOOL));
        buildNow = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.workshop.build_now"), button -> click(ShuttleWorkshopMenu.BUTTON_BUILD_NOW))
                .bounds(leftPos + 120, topPos + ShuttleWorkshopMenu.SUPPLY_Y - 2, 50, 20).build());
        updateButtons();
    }

    private Button small(int y, int id) {
        return Button.builder(Component.literal("-"), button -> click(id)).bounds(leftPos + 92, y, 10, 10).build();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void updateButtons() {
        ItemStack shuttle = menu.shuttle();
        ShuttleModules modules = ShuttleModules.of(shuttle);
        boolean present = !shuttle.isEmpty();
        removeSpeed.visible = present;
        removeHandling.visible = present;
        removeTool.visible = present;
        removeSpeed.active = modules.speed() > 0;
        removeHandling.active = modules.handling() > 0;
        removeTool.active = modules.tool().isPresent();
        buildNow.visible = present && !menu.sheet().isEmpty();
        buildNow.active = menu.canBuild();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOUR_PANEL);
        frame(graphics, ShuttleWorkshopMenu.SHUTTLE_X, ShuttleWorkshopMenu.SHUTTLE_Y);
        frame(graphics, ShuttleWorkshopMenu.INSTALL_X, ShuttleWorkshopMenu.INSTALL_Y);
        for (int i = 0; i < 6; i++) {
            frame(graphics, ShuttleWorkshopMenu.SUPPLY_X + i * 18, ShuttleWorkshopMenu.SUPPLY_Y);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                frame(graphics, 8 + column * 18, ShuttleWorkshopMenu.INVENTORY_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            frame(graphics, 8 + column * 18, ShuttleWorkshopMenu.HOTBAR_Y);
        }
    }

    private void frame(GuiGraphics graphics, int slotX, int slotY) {
        int x = leftPos + slotX - 1;
        int y = topPos + slotY - 1;
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 18, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 18, 0xFF373737);
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        // The install slot never holds anything; a plus marks it.
        graphics.drawString(font, "+", ShuttleWorkshopMenu.INSTALL_X + 5, ShuttleWorkshopMenu.INSTALL_Y + 4, 0x707070, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.supplies"), ShuttleWorkshopMenu.SUPPLY_X,
                ShuttleWorkshopMenu.SUPPLY_Y - 10, COLOUR_TEXT, false);
        ItemStack shuttle = menu.shuttle();
        if (shuttle.isEmpty()) {
            graphics.fill(ShuttleWorkshopMenu.INSTALL_X, ShuttleWorkshopMenu.INSTALL_Y, ShuttleWorkshopMenu.INSTALL_X + 16,
                    ShuttleWorkshopMenu.INSTALL_Y + 16, COLOUR_CLOSED);
            graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.hint"), INFO_X, ShuttleWorkshopMenu.SHUTTLE_Y + 4,
                    COLOUR_TEXT, false);
            return;
        }
        ShuttleTiers tiers = menu.tiers();
        int tier = ShuttleItem.tier(shuttle);
        ShuttleModules modules = ShuttleModules.of(shuttle);
        int y = ShuttleWorkshopMenu.SHUTTLE_Y;
        graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.tier", tier, tiers.tier(tier).stacks()), INFO_X, y,
                COLOUR_TEXT, false);
        y += LINE;
        graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.speed", modules.speed(), tiers.tier(tier).max(Kind.SPEED)),
                INFO_X, y, COLOUR_TEXT, false);
        y += LINE;
        graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.handling", modules.handling(),
                tiers.tier(tier).max(Kind.HANDLING)), INFO_X, y, COLOUR_TEXT, false);
        y += LINE;
        Component tool = modules.tool().map(item -> (Component) item.getDescription())
                .orElse(Component.translatable("gui.servus_storage.workshop.no_tool"));
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("gui.servus_storage.workshop.tool", tool).getString(), 60),
                INFO_X, y, COLOUR_TEXT, false);
        drawSheet(graphics, tier);
    }

    /** The next tier's supplies, each as have and need, green when complete. */
    private void drawSheet(GuiGraphics graphics, int tier) {
        List<ShuttleTiers.Supply> sheet = menu.sheet();
        int y = ShuttleWorkshopMenu.SHUTTLE_Y;
        if (sheet.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.top_tier"), SHEET_X, y, COLOUR_TEXT, false);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.next", tier + 1), SHEET_X, y, COLOUR_TEXT, false);
        for (Map.Entry<String, Integer> need : ShuttleBuild.needs(sheet).entrySet()) {
            y += LINE;
            if (y > ShuttleWorkshopMenu.SUPPLY_Y - 22) {
                break;
            }
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(need.getKey())));
            int have = Math.min(menu.supplied(need.getKey()), need.getValue());
            graphics.pose().pushPose();
            graphics.pose().translate(SHEET_X, y - 1, 0);
            graphics.pose().scale(0.6F, 0.6F, 1);
            graphics.renderItem(icon, 0, 0);
            graphics.pose().popPose();
            graphics.drawString(font, have + "/" + need.getValue(), SHEET_X + 12, y, have >= need.getValue() ? COLOUR_DONE : COLOUR_MISSING, false);
        }
    }
}
