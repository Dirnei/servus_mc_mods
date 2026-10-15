package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.shuttles.zone.SetLabelNamePayload;
import io.github.dirnei.mc.servus.shuttles.zone.SetLabelTagsPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Name box, nine ghost filter slots, tag line, player inventory. */
public class ZoneLabelScreen extends AbstractContainerScreen<ZoneLabelMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "textures/gui/zone_label.png");
    private static final int NAME_Y = 18;
    private static final int TAGS_Y = 62;
    private static final int COLOUR_TEXT = 0xFF404040;

    private EditBox name;
    private EditBox tags;
    private boolean nameWasFocused;
    private boolean tagsWasFocused;

    public ZoneLabelScreen(ZoneLabelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ZoneLabelMenu.INVENTORY_Y - 11;
        name = textField(NAME_Y, 32, "name", menu.name());
        tags = textField(TAGS_Y, 200, "tags", menu.zoneFilter().tagLine());
        setInitialFocus(name);
    }

    private EditBox textField(int y, int maxLength, String key, String value) {
        EditBox box = new EditBox(font, leftPos + 8, topPos + y, 160, 12, Component.translatable("gui.servus_shuttles.zone_label." + key));
        box.setMaxLength(maxLength);
        box.setHint(Component.translatable("gui.servus_shuttles.zone_label." + key + "_hint"));
        box.setBordered(true);
        box.setValue(value);
        return addRenderableWidget(box);
    }

    private void sendName() {
        if (!name.getValue().trim().equals(menu.name())) {
            ClientPacketDistributor.sendToServer(new SetLabelNamePayload(name.getValue()));
        }
    }

    private void sendTags() {
        if (!tags.getValue().equals(menu.zoneFilter().tagLine())) {
            ClientPacketDistributor.sendToServer(new SetLabelTagsPayload(tags.getValue()));
        }
    }

    /** Sends a field when it loses focus, and takes the label's values back while the player is not typing. */
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
        if (!nameFocused && !name.getValue().equals(menu.name())) {
            name.setValue(menu.name());
        }
        if (!tagsFocused && !tags.getValue().equals(menu.zoneFilter().tagLine())) {
            tags.setValue(menu.zoneFilter().tagLine());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("gui.servus_shuttles.zone_label.items"), 8, ZoneLabelMenu.GHOST_Y - 9, COLOUR_TEXT, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            sendName();
            sendTags();
            return super.keyPressed(event);
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
            if (focused.keyPressed(event) || focused.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        sendName();
        sendTags();
        super.removed();
    }
}
