package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.naming.MachineNameMenu;
import io.github.dirnei.mc.servus.foundation.naming.NameText;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.naming.RenameMachinePayload;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * A machine screen whose title row is the machine's name, renamed with the pencil
 * after it (see {@link TitleNameEditor}); the numbered label shows while unnamed
 * and "#n" sits at the row's right end while named. Without a named machine the
 * plain title shows.
 */
public abstract class NamedMachineScreen<M extends AbstractContainerMenu & MachineNameMenu> extends AbstractContainerScreen<M> {

    /** Room at the right end of the title row for the number of a named machine. */
    private static final int NUMBER_ROOM = 26;
    private static final int TITLE_COLOUR = 0xFF404040;

    private TitleNameEditor nameEditor;

    protected NamedMachineScreen(M menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        super(menu, playerInventory, title, imageWidth, imageHeight);
    }

    @Override
    protected void init() {
        super.init();
        Optional<NamedMachine> machine = menu.namedMachine();
        if (machine.isEmpty()) {
            nameEditor = null;
            return;
        }
        if (nameEditor == null) {
            nameEditor = new TitleNameEditor(NameText.clean(machine.get().name()), machine.get().numberLabel(),
                    Component.translatable("gui.servus_foundation.machine_name"), NameText.MAX_LENGTH, NameText::clean,
                    typed -> ClientPacketDistributor.sendToServer(new RenameMachinePayload(typed)), this::setFocused);
        }
        addRenderableWidget(nameEditor.createBox(font));
        addRenderableWidget(nameEditor.createButton());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (nameEditor != null) {
            nameEditor.layout(font, leftPos + titleLabelX, topPos + titleLabelY, imageWidth - 2 * titleLabelX - NUMBER_ROOM);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (nameEditor != null && nameEditor.mouseClicked(event)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (nameEditor != null) {
            nameEditor.tick();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameEditor != null && nameEditor.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        if (nameEditor != null) {
            nameEditor.removed();
        }
        super.removed();
    }

    /** The title and the inventory label; screens that draw their own labels call {@link #extractTitle} instead. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        extractTitle(graphics);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TITLE_COLOUR, false);
    }

    /** The plain title without a named machine; with one, the name or the numbered label, and "#n" right while named. */
    protected void extractTitle(GuiGraphicsExtractor graphics) {
        if (nameEditor == null) {
            graphics.text(font, title, titleLabelX, titleLabelY, TITLE_COLOUR, false);
            return;
        }
        nameEditor.drawTitle(graphics, font, leftPos, topPos);
        int number = menu.namedMachine().map(NamedMachine::number).orElse(0);
        if (number > 0 && nameEditor.named()) {
            Component shown = Component.literal("#" + number);
            graphics.text(font, shown, imageWidth - titleLabelX - font.width(shown), titleLabelY, TITLE_COLOUR, false);
        }
    }
}
