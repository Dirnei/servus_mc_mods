package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.zone.SetLabelNamePayload;
import io.github.dirnei.mc.servus.storage.zone.SetLabelTagsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/** Name box, nine ghost filter slots, tag line, player inventory. */
public class ZoneLabelScreen extends AbstractContainerScreen<ZoneLabelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/zone_label.png");
    private static final int NAME_Y = 18;
    private static final int TAGS_Y = 62;
    private static final int COLOUR_TEXT = 0x404040;

    private EditBox name;
    private EditBox tags;
    private boolean nameWasFocused;
    private boolean tagsWasFocused;

    public ZoneLabelScreen(ZoneLabelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ZoneLabelMenu.INVENTORY_Y - 11;
        name = new EditBox(font, leftPos + 8, topPos + NAME_Y, 160, 12, Component.translatable("gui.servus_storage.zone_label.name"));
        name.setMaxLength(32);
        name.setHint(Component.translatable("gui.servus_storage.zone_label.name_hint"));
        name.setBordered(true);
        name.setValue(menu.name());
        addRenderableWidget(name);
        tags = new EditBox(font, leftPos + 8, topPos + TAGS_Y, 160, 12, Component.translatable("gui.servus_storage.zone_label.tags"));
        tags.setMaxLength(200);
        tags.setHint(Component.translatable("gui.servus_storage.zone_label.tags_hint"));
        tags.setBordered(true);
        tags.setValue(menu.zoneFilter().tagLine());
        addRenderableWidget(tags);
        setInitialFocus(name);
    }

    private void sendName() {
        if (!name.getValue().trim().equals(menu.name())) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SetLabelNamePayload(name.getValue()));
        }
    }

    private void sendTags() {
        if (!tags.getValue().equals(menu.zoneFilter().tagLine())) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SetLabelTagsPayload(tags.getValue()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean nameFocused = name.isFocused();
        if (nameWasFocused && !nameFocused) {
            sendName();
        }
        nameWasFocused = nameFocused;
        boolean tagsFocused = tags.isFocused();
        if (tagsWasFocused && !tagsFocused) {
            sendTags();
        }
        tagsWasFocused = tagsFocused;
        // The server's answer comes back through the hand slot; pick it up when the player is not typing.
        if (!nameFocused && !name.getValue().equals(menu.name())) {
            name.setValue(menu.name());
        }
        if (!tagsFocused && !tags.getValue().equals(menu.zoneFilter().tagLine())) {
            tags.setValue(menu.zoneFilter().tagLine());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.servus_storage.zone_label.items"), 8, ZoneLabelMenu.GHOST_Y - 9, COLOUR_TEXT, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            sendName();
            sendTags();
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        EditBox focused = name.isFocused() ? name : tags.isFocused() ? tags : null;
        if (focused != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (focused == name) {
                    sendName();
                } else {
                    sendTags();
                }
                focused.setFocused(false);
                return true;
            }
            if (focused.keyPressed(keyCode, scanCode, modifiers) || focused.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        sendName();
        sendTags();
        super.removed();
    }
}
