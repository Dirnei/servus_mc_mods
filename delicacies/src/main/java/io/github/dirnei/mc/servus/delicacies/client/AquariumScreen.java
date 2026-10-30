package io.github.dirnei.mc.servus.delicacies.client;

import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumLook;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumMenu;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumSettingsPayload;
import io.github.dirnei.mc.servus.delicacies.aquarium.FishSpecies;
import io.github.dirnei.mc.servus.delicacies.registry.DelicaciesFluids;
import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.foundation.client.InlineStepper;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

/**
 * The aquarium GUI: species and fullness, stock and capacity, water quality and power, the feed, filter and catch
 * slots, the Murky Water gauge, the auto catch switch and the keep level. Drawn without a texture: a plain panel with
 * slot frames.
 */
public class AquariumScreen extends AbstractContainerScreen<AquariumMenu> {

    private static final int TEXT = 0xFF404040;
    private static final int WARN = 0xFFA02020;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int MAX_KEEP = 9999;
    private static final int SETTINGS_Y = 84;
    private static final int TOGGLE_WIDTH = 80;
    private static final int KEEP_X = 116;

    private final FluidGauge murky = new FluidGauge(AquariumMenu.MURKY_WIDTH, 16);
    private Button autoCatch;
    private InlineStepper keep;
    private boolean shownAutoCatch;
    private int shownKeep = -1;

    public AquariumScreen(AquariumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AquariumMenu.WIDTH, AquariumMenu.HEIGHT);
        this.inventoryLabelY = AquariumMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        autoCatch = addRenderableWidget(Button.builder(autoCatchLabel(menu.autoCatch()), button -> send(!menu.autoCatch(), keep.value()))
                .bounds(leftPos + 8, topPos + SETTINGS_Y, TOGGLE_WIDTH, 16).build());
        keep = new InlineStepper(font, Component.translatable("gui.servus_delicacies.aquarium.keep"), 0, MAX_KEEP, () -> 64,
                value -> send(menu.autoCatch(), value));
        keep.widgets().forEach(this::addRenderableWidget);
        keep.place(leftPos + KEEP_X, topPos + SETTINGS_Y + 1, imageWidth - KEEP_X - 8);
        keep.setValue(menu.keep());
        shownKeep = menu.keep();
        shownAutoCatch = menu.autoCatch();
        murky.place(leftPos + AquariumMenu.MURKY_X, topPos + AquariumMenu.SLOT_Y);
    }

    private FluidStack murkyStack() {
        return menu.murky() <= 0 ? FluidStack.EMPTY : new FluidStack(DelicaciesFluids.MURKY_WATER.get(), menu.murky());
    }

    /** The Murky Water gauge's fluid and screen area while the mouse is over it, for JEI. */
    public Optional<MurkyGauge> murkyGaugeAt(double mouseX, double mouseY) {
        FluidStack stack = murkyStack();
        if (stack.isEmpty() || !murky.isHovering(mouseX, mouseY)) {
            return Optional.empty();
        }
        return Optional.of(new MurkyGauge(stack, new Rect2i(leftPos + AquariumMenu.MURKY_X, topPos + AquariumMenu.SLOT_Y,
                AquariumMenu.MURKY_WIDTH, 16)));
    }

    /** The Murky Water the gauge shows and where it is drawn. */
    public record MurkyGauge(FluidStack stack, Rect2i area) {
    }

    private void send(boolean autoCatchOn, int keepLevel) {
        ClientPacketDistributor.sendToServer(new AquariumSettingsPayload(menu.masterPos(), autoCatchOn, keepLevel));
    }

    private static Component autoCatchLabel(boolean on) {
        return Component.translatable(on ? "gui.servus_delicacies.aquarium.auto_catch_on" : "gui.servus_delicacies.aquarium.auto_catch_off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        keep.tick();
        if (menu.autoCatch() != shownAutoCatch) {
            shownAutoCatch = menu.autoCatch();
            autoCatch.setMessage(autoCatchLabel(shownAutoCatch));
        }
        if (menu.keep() != shownKeep && !keep.field().isFocused()) {
            shownKeep = menu.keep();
            keep.setValue(shownKeep);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return keep.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (keep.field().isFocused() && event.key() != GLFW.GLFW_KEY_ESCAPE) {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                keep.commit();
                keep.field().setFocused(false);
                return true;
            }
            keep.field().keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (menu.getCarried().isEmpty()) {
            murky.renderTooltip(graphics, font, murkyStack(), menu.murkyCapacity(), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x, y, x + imageWidth, y + 1, LIGHT);
        graphics.fill(x, y, x + 1, y + imageHeight, LIGHT);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, DARK);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, SLOT);
            graphics.fill(sx, sy, sx + 17, sy + 1, DARK);
            graphics.fill(sx, sy, sx + 1, sy + 17, DARK);
            graphics.fill(sx + 1, sy + 17, sx + 18, sy + 18, LIGHT);
            graphics.fill(sx + 17, sy + 1, sx + 18, sy + 18, LIGHT);
        }
        murky.render(graphics, murkyStack(), menu.murkyCapacity());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        FishSpecies species = menu.species();
        Component kind = species == null || menu.stock() == 0
                ? Component.translatable("gui.servus_delicacies.aquarium.no_fish")
                : species.displayName();
        graphics.text(font, Component.translatable("gui.servus_delicacies.aquarium.fullness", kind,
                AquariumLook.fullnessPercent(menu.stock(), menu.capacity())), 8, 18, TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_delicacies.aquarium.stock", menu.stock(), menu.capacity()), 8, 28, TEXT, false);
        graphics.text(font, Component.translatable(menu.powered() ? "gui.servus_delicacies.aquarium.water_powered"
                : "gui.servus_delicacies.aquarium.water_unpowered", menu.qualityPercent()), 8, 38, menu.powered() ? TEXT : WARN, false);
        int labelY = AquariumMenu.SLOT_Y - 10;
        graphics.text(font, Component.translatable("gui.servus_delicacies.aquarium.feed"), AquariumMenu.FEED_X, labelY, TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_delicacies.aquarium.filter"), AquariumMenu.FILTER_X, labelY, TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_delicacies.aquarium.catch"), AquariumMenu.CATCH_X, labelY, TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_delicacies.aquarium.murky"), AquariumMenu.MURKY_X, labelY, TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_delicacies.aquarium.keep"), KEEP_X - font.width(
                Component.translatable("gui.servus_delicacies.aquarium.keep")) - 4, SETTINGS_Y + 4, TEXT, false);
    }
}
