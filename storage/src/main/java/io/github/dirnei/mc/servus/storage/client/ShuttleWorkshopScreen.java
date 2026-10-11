package io.github.dirnei.mc.servus.storage.client;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.storage.shuttle.module.EngraveShuttlePayload;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleBuild;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleBuildStepPayload;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers.Kind;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.BoltBoard;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.BuildStep;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.HandBuild;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.NamePlate;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.Spot;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.WeldBoard;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.WireBoard;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The shuttle and install slots, the shuttle's tier with its upgrades and
 * tool, each with a button that takes one out, the next tier's build sheet
 * with what is in and what is missing, the supply slots and Build now. On
 * the right a work panel shows the shuttle large; a build by hand plays its
 * steps there, and after the last one the panel asks for the name plate.
 */
public class ShuttleWorkshopScreen extends AbstractContainerScreen<ShuttleWorkshopMenu> {

    private static final int COLOUR_PANEL = 0xFFC6C6C6;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_MISSING = 0xA02020;
    private static final int COLOUR_DONE = 0x207020;
    private static final int COLOUR_CLOSED = 0x80373737;
    private static final int COLOUR_BOARD = 0xFF2B2B2B;
    private static final int COLOUR_BOLT_DONE = 0xFF8A8A8A;
    private static final int COLOUR_BOLT_WAITING = 0xFF5A5A40;
    private static final int COLOUR_WELDED = 0xFFFFB030;
    private static final int COLOUR_SEAM = 0xFF805020;
    private static final int COLOUR_SEAM_LATER = 0x60FFFFFF;
    private static final int INFO_X = 30;
    private static final int SHEET_X = 104;
    private static final int LINE = 11;

    /** The work panel to the right of the old layout. */
    private static final int PANEL_X = 180;
    private static final int PANEL_W = 136;
    /** The board over the shuttle, where the steps are played; board coordinates start at its top left. */
    private static final int BOARD_X = PANEL_X + 4;
    private static final int BOARD_Y = 18;
    private static final int BOARD_W = 128;
    private static final int BOARD_H = 100;
    private static final float MODEL_SCALE = 5.0F;
    private static final int CONTACT = 6;
    private static final int NAME_Y = 148;
    private static final int ENGRAVE_Y = 164;
    private static final int PANEL_BUTTON_Y = 180;

    private Button removeSpeed;
    private Button removeHandling;
    private Button removeTool;
    private Button buildNow;
    private Button byHand;
    private Button cancel;
    private Button engrave;
    private EditBox name;

    /** The board of the step being played and the step it belongs to; -1 when none. */
    private int boardStep = -1;
    private BoltBoard bolts;
    private WireBoard wires;
    private WeldBoard weld;
    /** The step whose completion was sent last, so each step is reported once. */
    private int reported = -1;

    public ShuttleWorkshopScreen(ShuttleWorkshopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 320, 204);
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
        byHand = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.workshop.by_hand"), button -> click(ShuttleWorkshopMenu.BUTTON_BUILD_BY_HAND))
                .bounds(leftPos + BOARD_X, topPos + PANEL_BUTTON_Y, BOARD_W, 20).build());
        cancel = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.workshop.cancel"), button -> click(ShuttleWorkshopMenu.BUTTON_CANCEL))
                .bounds(leftPos + BOARD_X, topPos + PANEL_BUTTON_Y, BOARD_W, 20).build());
        name = new EditBox(font, leftPos + BOARD_X, topPos + NAME_Y, BOARD_W, 12, Component.translatable("gui.servus_storage.workshop.name"));
        name.setMaxLength(NamePlate.MAX);
        name.setHint(Component.translatable("gui.servus_storage.workshop.name_hint"));
        addRenderableWidget(name);
        engrave = addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.workshop.engrave"), button -> sendEngrave())
                .bounds(leftPos + BOARD_X, topPos + ENGRAVE_Y, BOARD_W, 14).build());
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

    private void sendEngrave() {
        if (menu.building() && done()) {
            ClientPacketDistributor.sendToServer(new EngraveShuttlePayload(name.getValue()));
            name.setValue("");
            name.setFocused(false);
        }
    }

    /** Whether every step of the running build is done and it waits for its name. */
    private boolean done() {
        return HandBuild.done(menu.buildStep(), menu.steps().size());
    }

    private void updateButtons() {
        ItemStack shuttle = menu.shuttle();
        ShuttleModules modules = ShuttleModules.of(shuttle);
        boolean present = !shuttle.isEmpty();
        boolean building = menu.building();
        removeSpeed.visible = present;
        removeHandling.visible = present;
        removeTool.visible = present;
        removeSpeed.active = !building && modules.speed() > 0;
        removeHandling.active = !building && modules.handling() > 0;
        removeTool.active = !building && modules.tool().isPresent();
        boolean sheet = present && !menu.sheet().isEmpty();
        buildNow.visible = sheet && !building;
        buildNow.active = menu.canBuild();
        byHand.visible = sheet && !building;
        byHand.active = menu.canBuild();
        cancel.visible = building;
        boolean naming = building && done();
        name.visible = naming;
        engrave.visible = naming;
        if (!naming) {
            name.setFocused(false);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
        updateBoard();
    }

    /** Makes a new board when the build moves to another step, and reports a finished board once. */
    private void updateBoard() {
        if (!menu.building() || done()) {
            boardStep = -1;
            return;
        }
        int step = menu.buildStep();
        if (step != boardStep) {
            boardStep = step;
            reported = -1;
            BuildStep current = menu.steps().get(step);
            long seed = HandBuild.seed(menu.pos().asLong(), step);
            bolts = null;
            wires = null;
            weld = null;
            switch (current.kind()) {
                case BOLTS -> bolts = new BoltBoard(seed, current.count(), BOARD_W, BOARD_H);
                case WIRES -> wires = new WireBoard(seed, current.count());
                case WELD -> weld = new WeldBoard(seed, current.count(), BOARD_W, BOARD_H);
            }
        }
        boolean finished = bolts != null ? bolts.done() : wires != null ? wires.done() : weld != null && weld.done();
        if (finished && reported != step) {
            reported = step;
            ClientPacketDistributor.sendToServer(new ShuttleBuildStepPayload(step));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOUR_PANEL);
        graphics.fill(leftPos + PANEL_X - 3, topPos + 4, leftPos + PANEL_X - 2, topPos + imageHeight - 4, 0xFF8B8B8B);
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
        int boardLeft = leftPos + BOARD_X;
        int boardTop = topPos + BOARD_Y;
        graphics.fill(boardLeft - 1, boardTop - 1, boardLeft + BOARD_W + 1, boardTop + BOARD_H + 1, 0xFF373737);
        graphics.fill(boardLeft, boardTop, boardLeft + BOARD_W, boardTop + BOARD_H, COLOUR_BOARD);
        ItemStack shuttle = menu.shuttle();
        if (!shuttle.isEmpty()) {
            // The composed item model, five times its size, so the shuttle's look shows.
            graphics.pose().pushMatrix();
            graphics.pose().translate(boardLeft + BOARD_W / 2.0F - 8 * MODEL_SCALE, boardTop + BOARD_H / 2.0F - 8 * MODEL_SCALE);
            graphics.pose().scale(MODEL_SCALE, MODEL_SCALE);
            graphics.item(shuttle, 0, 0);
            graphics.pose().popMatrix();
        }
        if (menu.building() && !done()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(boardLeft, boardTop);
            drawBoard(graphics, mouseX - boardLeft, mouseY - boardTop);
            graphics.pose().popMatrix();
        }
    }

    private void drawBoard(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (bolts != null) {
            boolean bright = (Util.getMillis() / 300) % 2 == 0;
            List<Spot> spots = bolts.spots();
            for (int i = 0; i < spots.size(); i++) {
                Spot spot = spots.get(i);
                if (i < bolts.tightened()) {
                    dot(graphics, spot, 2, COLOUR_BOLT_DONE);
                } else if (i == bolts.tightened()) {
                    dot(graphics, spot, 4, 0xFF000000);
                    dot(graphics, spot, 3, bright ? 0xFFFFF060 : 0xFFE0B020);
                } else {
                    dot(graphics, spot, 2, COLOUR_BOLT_WAITING);
                }
            }
        } else if (wires != null) {
            List<Integer> left = wires.left();
            List<Integer> right = wires.right();
            for (int i = 0; i < left.size(); i++) {
                int target = wires.wiredTo(i);
                if (target >= 0) {
                    line(graphics, contact(true, i, left.size()), contact(false, target, right.size()), DuctChannels.color(left.get(i)));
                }
            }
            if (wires.selected() >= 0) {
                line(graphics, contact(true, wires.selected(), left.size()), new Spot(mouseX, mouseY), DuctChannels.color(left.get(wires.selected())));
            }
            for (int i = 0; i < left.size(); i++) {
                square(graphics, contact(true, i, left.size()), DuctChannels.color(left.get(i)), i == wires.selected());
                square(graphics, contact(false, i, right.size()), DuctChannels.color(right.get(i)), false);
            }
        } else if (weld != null) {
            List<List<Spot>> seams = weld.seams();
            for (int s = 0; s < seams.size(); s++) {
                List<Spot> points = seams.get(s);
                for (int p = 0; p < points.size(); p++) {
                    int colour = weld.welded(s, p) ? COLOUR_WELDED : s == weld.current() ? COLOUR_SEAM : COLOUR_SEAM_LATER;
                    dot(graphics, points.get(p), 1, colour);
                }
            }
        }
    }

    /** Where a wire contact sits on the board: left or right edge, spread evenly from top to bottom. */
    private static Spot contact(boolean left, int index, int count) {
        int y = BOARD_H * (index + 1) / (count + 1);
        return new Spot(left ? CONTACT : BOARD_W - CONTACT, y);
    }

    private static void dot(GuiGraphicsExtractor graphics, Spot spot, int radius, int colour) {
        graphics.fill(spot.x() - radius, spot.y() - radius, spot.x() + radius + 1, spot.y() + radius + 1, colour);
    }

    private static void square(GuiGraphicsExtractor graphics, Spot spot, int colour, boolean picked) {
        int half = CONTACT / 2;
        graphics.fill(spot.x() - half - 1, spot.y() - half - 1, spot.x() + half + 1, spot.y() + half + 1, picked ? 0xFFFFFFFF : 0xFF000000);
        graphics.fill(spot.x() - half, spot.y() - half, spot.x() + half, spot.y() + half, colour);
    }

    /** A two pixel wide line drawn as small squares along the way. */
    private static void line(GuiGraphicsExtractor graphics, Spot from, Spot to, int colour) {
        int steps = Math.max(Math.abs(to.x() - from.x()), Math.abs(to.y() - from.y()));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.round(from.x() + (to.x() - from.x()) * t);
            int y = (int) Math.round(from.y() + (to.y() - from.y()) * t);
            graphics.fill(x - 1, y - 1, x + 1, y + 1, colour);
        }
    }

    private void frame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
        int x = leftPos + slotX - 1;
        int y = topPos + slotY - 1;
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 18, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 18, 0xFF373737);
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && onBoard(mouseX, mouseY) && play((int) mouseX - leftPos - BOARD_X, (int) mouseY - topPos - BOARD_Y, true)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && weld != null && onBoard(mouseX, mouseY)) {
            play((int) mouseX - leftPos - BOARD_X, (int) mouseY - topPos - BOARD_Y, false);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private boolean onBoard(double mouseX, double mouseY) {
        double x = mouseX - leftPos - BOARD_X;
        double y = mouseY - topPos - BOARD_Y;
        return menu.building() && !done() && boardStep >= 0 && x >= 0 && y >= 0 && x < BOARD_W && y < BOARD_H;
    }

    /** Hands a click or drag on the board to the step being played; a wrong spot simply does nothing. */
    private boolean play(int x, int y, boolean click) {
        if (bolts != null) {
            if (click) {
                bolts.click(x, y);
            }
            return true;
        }
        if (wires != null) {
            if (click) {
                int count = wires.left().size();
                for (int i = 0; i < count; i++) {
                    if (contact(true, i, count).near(x, y, CONTACT)) {
                        wires.select(i);
                        return true;
                    }
                    if (contact(false, i, count).near(x, y, CONTACT)) {
                        wires.connect(i);
                        return true;
                    }
                }
            }
            return true;
        }
        if (weld != null) {
            weld.cover(x, y);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && name.visible && name.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendEngrave();
                return true;
            }
            if (name.keyPressed(event) || name.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        // The install slot never holds anything; a plus marks it.
        graphics.text(font, "+", ShuttleWorkshopMenu.INSTALL_X + 5, ShuttleWorkshopMenu.INSTALL_Y + 4, 0xFF707070, false);
        graphics.text(font, Component.translatable("gui.servus_storage.workshop.supplies"), ShuttleWorkshopMenu.SUPPLY_X,
                ShuttleWorkshopMenu.SUPPLY_Y - 10, COLOUR_TEXT, false);
        drawPanelLabels(graphics);
        ItemStack shuttle = menu.shuttle();
        if (shuttle.isEmpty()) {
            graphics.fill(ShuttleWorkshopMenu.INSTALL_X, ShuttleWorkshopMenu.INSTALL_Y, ShuttleWorkshopMenu.INSTALL_X + 16,
                    ShuttleWorkshopMenu.INSTALL_Y + 16, COLOUR_CLOSED);
            graphics.text(font, Component.translatable("gui.servus_storage.workshop.hint"), INFO_X, ShuttleWorkshopMenu.SHUTTLE_Y + 4,
                    COLOUR_TEXT, false);
            return;
        }
        ShuttleTiers tiers = menu.tiers();
        int tier = ShuttleItem.tier(shuttle);
        ShuttleModules modules = ShuttleModules.of(shuttle);
        int y = ShuttleWorkshopMenu.SHUTTLE_Y;
        graphics.text(font, Component.translatable("gui.servus_storage.workshop.tier", tier, tiers.tier(tier).stacks()), INFO_X, y,
                COLOUR_TEXT, false);
        y += LINE;
        graphics.text(font, Component.translatable("gui.servus_storage.workshop.speed", modules.speed(), tiers.tier(tier).max(Kind.SPEED)),
                INFO_X, y, COLOUR_TEXT, false);
        y += LINE;
        graphics.text(font, Component.translatable("gui.servus_storage.workshop.handling", modules.handling(),
                tiers.tier(tier).max(Kind.HANDLING)), INFO_X, y, COLOUR_TEXT, false);
        y += LINE;
        Component tool = modules.tool().map(item -> (Component) Component.translatable(item.getDescriptionId()))
                .orElse(Component.translatable("gui.servus_storage.workshop.no_tool"));
        graphics.text(font, font.plainSubstrByWidth(Component.translatable("gui.servus_storage.workshop.tool", tool).getString(), 60),
                INFO_X, y, COLOUR_TEXT, false);
        drawSheet(graphics, tier);
    }

    /** The panel's header over the board and the line under it: the step and what to do, or the shuttle's plate. */
    private void drawPanelLabels(GuiGraphicsExtractor graphics) {
        ItemStack shuttle = menu.shuttle();
        if (menu.building()) {
            List<BuildStep> steps = menu.steps();
            int step = menu.buildStep();
            if (done()) {
                graphics.text(font, Component.translatable("gui.servus_storage.workshop.name"), BOARD_X, 6, COLOUR_TEXT, false);
            } else if (step < steps.size()) {
                graphics.text(font, Component.translatable("gui.servus_storage.workshop.step", step + 1, steps.size()), BOARD_X, 6,
                        COLOUR_TEXT, false);
                Component hint = Component.translatable("gui.servus_storage.workshop.step." + steps.get(step).kind().id());
                graphics.text(font, font.plainSubstrByWidth(hint.getString(), BOARD_W), BOARD_X, BOARD_Y + BOARD_H + 4, COLOUR_TEXT, false);
            }
        } else if (!shuttle.isEmpty()) {
            graphics.text(font, font.plainSubstrByWidth(shuttle.getHoverName().getString(), BOARD_W), BOARD_X, 6, COLOUR_TEXT, false);
        }
        if (!shuttle.isEmpty() && !(menu.building() && done())) {
            ShuttleItem.plate(shuttle).ifPresent(plate -> graphics.text(font,
                    font.plainSubstrByWidth(Component.translatable("gui.servus_storage.workshop.plate", plate).getString(), BOARD_W),
                    BOARD_X, BOARD_Y + BOARD_H + 16, COLOUR_TEXT, false));
        }
    }

    /** The next tier's supplies, each as have and need, green when complete. */
    private void drawSheet(GuiGraphicsExtractor graphics, int tier) {
        List<ShuttleTiers.Supply> sheet = menu.sheet();
        int y = ShuttleWorkshopMenu.SHUTTLE_Y;
        if (sheet.isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_storage.workshop.top_tier"), SHEET_X, y, COLOUR_TEXT, false);
            return;
        }
        graphics.text(font, Component.translatable("gui.servus_storage.workshop.next", tier + 1), SHEET_X, y, COLOUR_TEXT, false);
        for (Map.Entry<String, Integer> need : ShuttleBuild.needs(sheet).entrySet()) {
            y += LINE;
            if (y > ShuttleWorkshopMenu.SUPPLY_Y - 22) {
                break;
            }
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(need.getKey())));
            int have = Math.min(menu.supplied(need.getKey()), need.getValue());
            graphics.pose().pushMatrix();
            graphics.pose().translate(SHEET_X, y - 1);
            graphics.pose().scale(0.6F, 0.6F);
            graphics.item(icon, 0, 0);
            graphics.pose().popMatrix();
            graphics.text(font, have + "/" + need.getValue(), SHEET_X + 12, y, have >= need.getValue() ? COLOUR_DONE : COLOUR_MISSING, false);
        }
    }
}
