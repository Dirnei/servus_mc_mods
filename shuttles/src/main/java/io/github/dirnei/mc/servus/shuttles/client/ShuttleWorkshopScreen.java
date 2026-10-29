package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelColours;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.EngraveShuttlePayload;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleBuild;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleBuildStepPayload;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers.Kind;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleWorkshopBlockEntity;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.BoltBoard;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.BuildStep;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.HandBuild;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.NamePlate;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.Spot;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.WeldBoard;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.WireBoard;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The shuttle and install slots, the shuttle's tier with its upgrades, tool
 * and Muffler, each with a button that takes one out, the next tier's build sheet
 * with what is in and what is missing, the supply slots and Build, which
 * reads Skip while a build runs. On the right a work panel shows the shuttle
 * large, turning slowly or by a mouse drag; a build swings it to the work
 * angle and plays its steps there, and after the last one the panel asks for
 * the name plate.
 */
public class ShuttleWorkshopScreen extends AbstractContainerScreen<ShuttleWorkshopMenu> {

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_MISSING = 0xFFA02020;
    private static final int COLOUR_DONE = 0xFF207020;
    private static final int COLOUR_CLOSED = 0x80373737;
    private static final int COLOUR_BOARD = 0xFF2B2B2B;
    private static final int COLOUR_BOLT_DONE = 0xFF8A8A8A;
    private static final int COLOUR_BOLT_WAITING = 0xFF5A5A40;
    private static final int COLOUR_WELDED = 0xFFFFB030;
    private static final int COLOUR_SEAM = 0xFF805020;
    private static final int COLOUR_SEAM_LATER = 0x60FFFFFF;
    /** The dye colour of each wire colour index: white, orange, magenta, light blue, yellow, lime, pink, cyan. */
    private static final int[] WIRE_DYES = {0, 1, 2, 3, 4, 5, 6, 9};
    private static final int INFO_X = 30;
    private static final int SHEET_X = 104;
    /** The remove buttons sit at the right edge of the left panel, clear of the info lines and the sheet. */
    private static final int REMOVE_X = 164;
    private static final int LINE = 11;

    /** The work panel right of the slots. */
    private static final int PANEL_X = 180;
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
    /** How fast the idle shuttle turns, in degrees per second: one turn in ten seconds. */
    private static final float TURN_SPEED = 36.0F;
    /** Degrees the shuttle turns per GUI pixel of a mouse drag. */
    private static final float DRAG_TURN = 1.5F;
    /** The yaw a build by hand plays its steps at; the step spots are drawn for this view. */
    private static final float WORK_YAW = 0.0F;
    /** Seconds in which the swing to the work angle closes most of the way; it is done after about four of them. */
    private static final float SWING_TIME = 0.075F;
    /** The longest frame the turn takes into account, so a stalled frame does not jump the shuttle. */
    private static final float MAX_FRAME = 0.1F;
    private static final Component BUILD = Component.translatable("gui.servus_shuttles.workshop.build");
    private static final Component SKIP = Component.translatable("gui.servus_shuttles.workshop.skip");

    private Button removeSpeed;
    private Button removeHandling;
    private Button removeTool;
    private Button removeMuffler;
    /** Build while no build runs, Skip while one does. */
    private Button build;
    private Button cancel;
    private Button engrave;
    private EditBox name;

    /** The step whose board is shown; -1 when none. At most one board is set. */
    private int boardStep = -1;
    private BoltBoard bolts;
    private WireBoard wires;
    private WeldBoard weld;
    /** The step whose completion was sent last, so each step is reported once. */
    private int reported = -1;
    /** The large shuttle's turn about its upright axis, in degrees. */
    private float yaw = WORK_YAW;
    /** When {@link #yaw} was last moved on, in milliseconds; 0 before the first frame. */
    private long turnedAt;
    /** Whether the player is turning the shuttle by dragging the board. */
    private boolean turning;

    public ShuttleWorkshopScreen(ShuttleWorkshopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 320, 204);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ShuttleWorkshopMenu.INVENTORY_Y - 11;
        int infoY = topPos + ShuttleWorkshopMenu.SHUTTLE_Y + LINE - 2;
        removeSpeed = addRenderableWidget(removeButton(infoY, ShuttleWorkshopMenu.BUTTON_REMOVE_SPEED));
        removeHandling = addRenderableWidget(removeButton(infoY + LINE, ShuttleWorkshopMenu.BUTTON_REMOVE_HANDLING));
        removeTool = addRenderableWidget(removeButton(infoY + 2 * LINE, ShuttleWorkshopMenu.BUTTON_REMOVE_TOOL));
        removeMuffler = addRenderableWidget(removeButton(infoY + 3 * LINE, ShuttleWorkshopMenu.BUTTON_REMOVE_MUFFLER));
        build = addRenderableWidget(Button.builder(BUILD, button -> press(menu.building() ? ShuttleWorkshopMenu.BUTTON_SKIP : ShuttleWorkshopMenu.BUTTON_BUILD))
                .bounds(leftPos + 120, topPos + ShuttleWorkshopMenu.SUPPLY_Y - 2, 50, 20).build());
        cancel = addRenderableWidget(panelButton("gui.servus_shuttles.workshop.cancel", ShuttleWorkshopMenu.BUTTON_CANCEL));
        name = new EditBox(font, leftPos + BOARD_X, topPos + NAME_Y, BOARD_W, 12, Component.translatable("gui.servus_shuttles.workshop.name"));
        name.setMaxLength(NamePlate.MAX);
        name.setHint(Component.translatable("gui.servus_shuttles.workshop.name_hint"));
        addRenderableWidget(name);
        engrave = addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.workshop.engrave"), button -> sendEngrave())
                .bounds(leftPos + BOARD_X, topPos + ENGRAVE_Y, BOARD_W, 14).build());
        updateButtons();
    }

    private Button removeButton(int y, int id) {
        return Button.builder(Component.literal("-"), button -> press(id)).bounds(leftPos + REMOVE_X, y, 10, 10).build();
    }

    private Button panelButton(String key, int id) {
        return Button.builder(Component.translatable(key), button -> press(id))
                .bounds(leftPos + BOARD_X, topPos + PANEL_BUTTON_Y, BOARD_W, 20).build();
    }

    private void press(int id) {
        MenuButtons.press(minecraft, menu, id);
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
        removeMuffler.visible = present;
        removeSpeed.active = !building && modules.speed() > 0;
        removeHandling.active = !building && modules.handling() > 0;
        removeTool.active = !building && modules.tool().isPresent();
        removeMuffler.active = !building && modules.muffled();
        boolean sheet = present && !menu.sheet().isEmpty();
        build.visible = sheet;
        build.active = building || menu.canBuild();
        build.setMessage(building ? SKIP : BUILD);
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PanelPainter.FACE);
        graphics.fill(leftPos + PANEL_X - 3, topPos + 4, leftPos + PANEL_X - 2, topPos + imageHeight - 4, PanelPainter.SLOT);
        slotFrame(graphics, ShuttleWorkshopMenu.SHUTTLE_X, ShuttleWorkshopMenu.SHUTTLE_Y);
        slotFrame(graphics, ShuttleWorkshopMenu.INSTALL_X, ShuttleWorkshopMenu.INSTALL_Y);
        for (int i = 0; i < ShuttleWorkshopBlockEntity.SUPPLY_SLOTS; i++) {
            slotFrame(graphics, ShuttleWorkshopMenu.SUPPLY_X + i * 18, ShuttleWorkshopMenu.SUPPLY_Y);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slotFrame(graphics, 8 + column * 18, ShuttleWorkshopMenu.INVENTORY_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slotFrame(graphics, 8 + column * 18, ShuttleWorkshopMenu.HOTBAR_Y);
        }
        int boardLeft = leftPos + BOARD_X;
        int boardTop = topPos + BOARD_Y;
        PanelPainter.bordered(graphics, boardLeft, boardTop, BOARD_W, BOARD_H, PanelPainter.DARK, COLOUR_BOARD);
        ItemStack shuttle = menu.shuttle();
        turn();
        if (!shuttle.isEmpty()) {
            GuiLargeItemRenderState.submit(graphics, shuttle, boardLeft, boardTop, boardLeft + BOARD_W, boardTop + BOARD_H,
                    16 * MODEL_SCALE, yaw);
        }
        if (menu.building() && !done()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(boardLeft, boardTop);
            drawBoard(graphics, mouseX - boardLeft, mouseY - boardTop);
            graphics.pose().popMatrix();
        }
    }

    /**
     * Moves the yaw on by the time since the last frame: during a build it
     * swings to the work angle and holds there, otherwise it turns slowly
     * unless the player is dragging it.
     */
    private void turn() {
        long now = Util.getMillis();
        float seconds = turnedAt == 0 ? 0 : Math.min(MAX_FRAME, (now - turnedAt) / 1000.0F);
        turnedAt = now;
        if (menu.building()) {
            turning = false;
            float off = Mth.wrapDegrees(WORK_YAW - yaw);
            yaw = Math.abs(off) < 0.5F ? WORK_YAW : yaw + off * (1 - (float) Math.exp(-seconds / SWING_TIME));
        } else if (!turning) {
            yaw = Mth.wrapDegrees(yaw + TURN_SPEED * seconds);
        }
    }

    private void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
        PanelPainter.sunken(graphics, leftPos + slotX - 1, topPos + slotY - 1, 18, 18);
    }

    /** Draws the current step's board in board coordinates. */
    private void drawBoard(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (bolts != null) {
            drawBolts(graphics);
        } else if (wires != null) {
            drawWires(graphics, mouseX, mouseY);
        } else if (weld != null) {
            drawWeld(graphics);
        }
    }

    /** Tightened bolts grey, the next one blinking, the rest dull. */
    private void drawBolts(GuiGraphicsExtractor graphics) {
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
    }

    /** Wires already connected, the one being dragged to the mouse, and the contacts on both edges over them. */
    private void drawWires(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Integer> left = wires.left();
        List<Integer> right = wires.right();
        for (int i = 0; i < left.size(); i++) {
            int target = wires.wiredTo(i);
            if (target >= 0) {
                line(graphics, contact(true, i, left.size()), contact(false, target, right.size()), wireColour(left.get(i)));
            }
        }
        if (wires.selected() >= 0) {
            line(graphics, contact(true, wires.selected(), left.size()), new Spot(mouseX, mouseY), wireColour(left.get(wires.selected())));
        }
        for (int i = 0; i < left.size(); i++) {
            square(graphics, contact(true, i, left.size()), wireColour(left.get(i)), i == wires.selected());
            square(graphics, contact(false, i, right.size()), wireColour(right.get(i)), false);
        }
    }

    private static int wireColour(int wire) {
        return ChannelColours.argb(WIRE_DYES[wire]);
    }

    /** Seam points, welded ones bright, the current seam darker, later seams faint. */
    private void drawWeld(GuiGraphicsExtractor graphics) {
        List<List<Spot>> seams = weld.seams();
        for (int s = 0; s < seams.size(); s++) {
            List<Spot> points = seams.get(s);
            for (int p = 0; p < points.size(); p++) {
                int colour = weld.welded(s, p) ? COLOUR_WELDED : s == weld.current() ? COLOUR_SEAM : COLOUR_SEAM_LATER;
                dot(graphics, points.get(p), 1, colour);
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && onBoard(event.x(), event.y()) && play(boardX(event.x()), boardY(event.y()), true)) {
            return true;
        }
        if (event.button() == 0 && onView(event.x(), event.y())) {
            turning = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && turning) {
            yaw = Mth.wrapDegrees(yaw + (float) dragX * DRAG_TURN);
            return true;
        }
        if (event.button() == 0 && weld != null && onBoard(event.x(), event.y())) {
            play(boardX(event.x()), boardY(event.y()), false);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && turning) {
            turning = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Whether the mouse is over the large shuttle while no build runs, where a drag turns it. */
    private boolean onView(double mouseX, double mouseY) {
        double x = mouseX - leftPos - BOARD_X;
        double y = mouseY - topPos - BOARD_Y;
        return !menu.building() && !menu.shuttle().isEmpty() && x >= 0 && y >= 0 && x < BOARD_W && y < BOARD_H;
    }

    private int boardX(double mouseX) {
        return (int) mouseX - leftPos - BOARD_X;
    }

    private int boardY(double mouseY) {
        return (int) mouseY - topPos - BOARD_Y;
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
                clickContact(x, y);
            }
            return true;
        }
        if (weld != null) {
            weld.cover(x, y);
            return true;
        }
        return false;
    }

    /** A left contact picks up its wire, a right contact takes the wire held. */
    private void clickContact(int x, int y) {
        int count = wires.left().size();
        for (int i = 0; i < count; i++) {
            if (contact(true, i, count).near(x, y, CONTACT)) {
                wires.select(i);
                return;
            }
            if (contact(false, i, count).near(x, y, CONTACT)) {
                wires.connect(i);
                return;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
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

    /** The install slot never holds anything; a plus marks it, greyed while there is no shuttle. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, "+", ShuttleWorkshopMenu.INSTALL_X + 5, ShuttleWorkshopMenu.INSTALL_Y + 4, 0xFF707070, false);
        graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.supplies"), ShuttleWorkshopMenu.SUPPLY_X,
                ShuttleWorkshopMenu.SUPPLY_Y - 10, COLOUR_TEXT, false);
        drawPanelLabels(graphics);
        ItemStack shuttle = menu.shuttle();
        if (shuttle.isEmpty()) {
            graphics.fill(ShuttleWorkshopMenu.INSTALL_X, ShuttleWorkshopMenu.INSTALL_Y, ShuttleWorkshopMenu.INSTALL_X + 16,
                    ShuttleWorkshopMenu.INSTALL_Y + 16, COLOUR_CLOSED);
            graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.hint"), INFO_X, ShuttleWorkshopMenu.SHUTTLE_Y + 4,
                    COLOUR_TEXT, false);
            return;
        }
        int tier = ShuttleItem.tier(shuttle);
        drawShuttleInfo(graphics, tier, ShuttleModules.of(shuttle));
        drawSheet(graphics, tier);
    }

    /** The tier's stacks, the upgrade counts against their maxes, the tool and whether it is muffled. */
    private void drawShuttleInfo(GuiGraphicsExtractor graphics, int tier, ShuttleModules modules) {
        ShuttleTiers.Tier stats = menu.tiers().tier(tier);
        int y = ShuttleWorkshopMenu.SHUTTLE_Y;
        graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.tier", tier, stats.stacks()), INFO_X, y, COLOUR_TEXT, false);
        y += LINE;
        graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.speed", modules.speed(), stats.max(Kind.SPEED)),
                INFO_X, y, COLOUR_TEXT, false);
        y += LINE;
        graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.handling", modules.handling(), stats.max(Kind.HANDLING)),
                INFO_X, y, COLOUR_TEXT, false);
        y += LINE;
        Component tool = modules.tool().map(item -> (Component) Component.translatable(item.getDescriptionId()))
                .orElse(Component.translatable("gui.servus_shuttles.workshop.no_tool"));
        graphics.text(font, font.plainSubstrByWidth(Component.translatable("gui.servus_shuttles.workshop.tool", tool).getString(), SHEET_X - INFO_X - 4),
                INFO_X, y, COLOUR_TEXT, false);
        y += LINE;
        graphics.text(font, Component.translatable(modules.muffled() ? "gui.servus_shuttles.workshop.muffled" : "gui.servus_shuttles.workshop.not_muffled"),
                INFO_X, y, COLOUR_TEXT, false);
    }

    /** The panel's header over the board and the line under it: the step and what to do, or the shuttle's plate. */
    private void drawPanelLabels(GuiGraphicsExtractor graphics) {
        ItemStack shuttle = menu.shuttle();
        if (menu.building()) {
            List<BuildStep> steps = menu.steps();
            int step = menu.buildStep();
            if (done()) {
                graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.name"), BOARD_X, 6, COLOUR_TEXT, false);
            } else if (step < steps.size()) {
                graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.step", step + 1, steps.size()), BOARD_X, 6,
                        COLOUR_TEXT, false);
                Component hint = Component.translatable("gui.servus_shuttles.workshop.step." + steps.get(step).kind().id());
                graphics.text(font, font.plainSubstrByWidth(hint.getString(), BOARD_W), BOARD_X, BOARD_Y + BOARD_H + 4, COLOUR_TEXT, false);
            }
        } else if (!shuttle.isEmpty()) {
            graphics.text(font, font.plainSubstrByWidth(shuttle.getHoverName().getString(), BOARD_W), BOARD_X, 6, COLOUR_TEXT, false);
        }
        if (!shuttle.isEmpty() && !(menu.building() && done())) {
            ShuttleItem.plate(shuttle).ifPresent(plate -> graphics.text(font,
                    font.plainSubstrByWidth(Component.translatable("gui.servus_shuttles.workshop.plate", plate).getString(), BOARD_W),
                    BOARD_X, BOARD_Y + BOARD_H + 16, COLOUR_TEXT, false));
        }
    }

    /** The next tier's supplies, each as have and need, green when complete. */
    private void drawSheet(GuiGraphicsExtractor graphics, int tier) {
        List<ShuttleTiers.Supply> sheet = menu.sheet();
        int y = ShuttleWorkshopMenu.SHUTTLE_Y;
        if (sheet.isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.top_tier"), SHEET_X, y, COLOUR_TEXT, false);
            return;
        }
        graphics.text(font, Component.translatable("gui.servus_shuttles.workshop.next", tier + 1), SHEET_X, y, COLOUR_TEXT, false);
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
