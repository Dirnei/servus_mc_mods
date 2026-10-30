package io.github.dirnei.mc.servus.delicacies.client;

import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumLook;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumMasterState;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumMenu;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumSettingsPayload;
import io.github.dirnei.mc.servus.delicacies.aquarium.FishSpecies;
import io.github.dirnei.mc.servus.delicacies.registry.DelicaciesFluids;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.foundation.client.InlineStepper;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

/**
 * The aquarium GUI: a fish bar for stock and capacity, a water quality meter with the dying mark, a power bolt, the
 * feed, filter and catch slots with faded ghost items while empty, the Murky Water gauge, the auto catch switch and the
 * keep level. Exact numbers sit in tooltips. Drawn without a texture: a plain panel with slot frames.
 */
public class AquariumScreen extends AbstractContainerScreen<AquariumMenu> {

    private static final String KEY = "gui.servus_delicacies.aquarium.";
    private static final int TEXT = 0xFF404040;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int MAX_KEEP = 9999;
    private static final int SETTINGS_Y = 84;
    private static final int TOGGLE_WIDTH = 80;
    private static final int KEEP_X = 116;
    private static final int ICON_X = 8;
    private static final int BAR_X = 28;
    private static final int BAR_HEIGHT = 10;
    private static final int FISH_Y = 17;
    private static final int FISH_BAR_WIDTH = 140;
    private static final int QUALITY_Y = 35;
    private static final int QUALITY_BAR_WIDTH = 122;
    private static final int BOLT_X = 158;
    private static final int FISH_BAR = 0xFF3F7FBF;
    private static final int BAR_BACK = 0xFF1C1C1C;
    private static final int BOLT_ON = 0xFFF0C020;
    private static final int BOLT_OFF = 0xFF6A6A6A;
    private static final int GHOST_VEIL = 0xB08B8B8B;
    private static final int UNREACHED_ALPHA = 0x38;
    private static final String[] BOLT = {
            "....###",
            "...###.",
            "..###..",
            ".###...",
            "#######",
            "...###.",
            "..###..",
            ".###...",
            "###....",
            "##....."};

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
        keep = new InlineStepper(font, Component.translatable(KEY + "keep"), 0, MAX_KEEP, () -> 64,
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
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        murky.renderTooltip(graphics, font, murkyStack(), menu.murkyCapacity(), mouseX, mouseY);
        List<Component> lines = tooltipAt(mouseX - leftPos, mouseY - topPos);
        if (!lines.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    /** The tooltip of the bar, bolt or empty aquarium slot at a point relative to the panel, or none. */
    private List<Component> tooltipAt(int x, int y) {
        if (inside(x, y, BAR_X, FISH_Y + 3, FISH_BAR_WIDTH, BAR_HEIGHT)) {
            return List.of(Component.translatable(KEY + "fullness", speciesName(),
                            AquariumLook.fullnessPercent(menu.stock(), menu.capacity())),
                    Component.translatable(KEY + "stock", menu.stock(), menu.capacity()));
        }
        if (inside(x, y, BAR_X, QUALITY_Y + 3, QUALITY_BAR_WIDTH, BAR_HEIGHT)) {
            return List.of(Component.translatable(KEY + "water_quality", menu.qualityPercent()),
                    Component.translatable(KEY + "dying_below", menu.dyingBelowPercent()));
        }
        if (inside(x, y, BOLT_X - 2, QUALITY_Y + 1, BOLT[0].length() + 4, BOLT.length + 4)) {
            return List.of(Component.translatable(KEY + (menu.powered() ? "powered" : "unpowered")));
        }
        int hovered = hoveredSlot == null ? -1 : menu.slots.indexOf(hoveredSlot);
        if (hovered >= 0 && hovered < AquariumMasterState.SLOT_COUNT && !hoveredSlot.hasItem()) {
            String slot = slotKey(hovered);
            return List.of(Component.translatable(KEY + slot), Component.translatable(KEY + slot + "_hint"));
        }
        return List.of();
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static String slotKey(int index) {
        return switch (index) {
            case AquariumMasterState.FEED_SLOT -> "feed";
            case AquariumMasterState.FILTER_SLOT -> "filter";
            default -> "catch";
        };
    }

    private Component speciesName() {
        FishSpecies species = menu.species();
        return species == null || menu.stock() == 0 ? Component.translatable(KEY + "no_fish") : species.displayName();
    }

    private ItemStack rawFish() {
        FishSpecies species = menu.species();
        return new ItemStack(species == null ? Items.COD : species.rawFish());
    }

    /** The faded item an empty aquarium slot shows. */
    private ItemStack ghost(int index) {
        return switch (index) {
            case AquariumMasterState.FEED_SLOT -> new ItemStack(Items.WHEAT_SEEDS);
            case AquariumMasterState.FILTER_SLOT -> new ItemStack(ServusFoundationItems.CARBON_FILTER.get());
            default -> rawFish();
        };
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
        graphics.item(rawFish(), x + ICON_X, y + FISH_Y);
        graphics.item(new ItemStack(Items.WATER_BUCKET), x + ICON_X, y + QUALITY_Y);
        for (int i = 0; i < AquariumMasterState.SLOT_COUNT; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                graphics.item(ghost(i), x + slot.x, y + slot.y);
            }
        }
        graphics.nextStratum();
        for (int i = 0; i < AquariumMasterState.SLOT_COUNT; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, GHOST_VEIL);
            }
        }
        fishBar(graphics, x + BAR_X, y + FISH_Y + 3);
        qualityBar(graphics, x + BAR_X, y + QUALITY_Y + 3);
        bolt(graphics, x + BOLT_X, y + QUALITY_Y + 3);
        murky.render(graphics, murkyStack(), menu.murkyCapacity());
    }

    private static void sunken(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BAR_BACK);
        graphics.fill(x, y, x + width, y + 1, DARK);
        graphics.fill(x, y, x + 1, y + height, DARK);
        graphics.fill(x + 1, y + height - 1, x + width, y + height, LIGHT);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, LIGHT);
    }

    private void fishBar(GuiGraphicsExtractor graphics, int x, int y) {
        sunken(graphics, x, y, FISH_BAR_WIDTH, BAR_HEIGHT);
        int inner = FISH_BAR_WIDTH - 2;
        int filled = menu.capacity() <= 0 ? 0 : (int) Math.min(inner, (long) inner * menu.stock() / menu.capacity());
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + BAR_HEIGHT - 1, FISH_BAR);
    }

    /** Red to yellow to green, dimmed beyond the current quality, with a dark mark where fish start dying. */
    private void qualityBar(GuiGraphicsExtractor graphics, int x, int y) {
        sunken(graphics, x, y, QUALITY_BAR_WIDTH, BAR_HEIGHT);
        int inner = QUALITY_BAR_WIDTH - 2;
        int reached = inner * menu.qualityPercent() / 100;
        for (int i = 0; i < inner; i++) {
            int color = qualityColor(i / (double) (inner - 1));
            if (i >= reached) {
                color = (color & 0x00FFFFFF) | UNREACHED_ALPHA << 24;
            }
            graphics.fill(x + 1 + i, y + 1, x + 2 + i, y + BAR_HEIGHT - 1, color);
        }
        int mark = x + 1 + inner * menu.dyingBelowPercent() / 100;
        graphics.fill(mark, y - 1, mark + 1, y + BAR_HEIGHT + 1, 0xFF000000);
    }

    private static int qualityColor(double t) {
        return t < 0.5 ? blend(0xC83228, 0xDCBE32, t / 0.5) : blend(0xDCBE32, 0x46B446, (t - 0.5) / 0.5);
    }

    private static int blend(int from, int to, double t) {
        int r = (int) Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int g = (int) Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private void bolt(GuiGraphicsExtractor graphics, int x, int y) {
        int color = menu.powered() ? BOLT_ON : BOLT_OFF;
        for (int row = 0; row < BOLT.length; row++) {
            for (int col = 0; col < BOLT[row].length(); col++) {
                if (BOLT[row].charAt(col) == '#') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        String count = menu.stock() + " / " + menu.capacity();
        graphics.text(font, count, BAR_X + (FISH_BAR_WIDTH - font.width(count)) / 2, FISH_Y + 4, WHITE, true);
        graphics.text(font, Component.translatable(KEY + "keep"), KEEP_X - font.width(
                Component.translatable(KEY + "keep")) - 4, SETTINGS_Y + 4, TEXT, false);
    }
}
