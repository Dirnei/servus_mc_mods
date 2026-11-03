package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.config.NotesClientConfig;
import io.github.dirnei.mc.servus.notes.model.GoalFill;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.PinLines;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The pinned list on the HUD in the vanilla look: name, open tasks, unreached goals with a bar, on a
 * translucent box in the configured corner. Hidden with F1, while a screen is open and by the toggle key.
 */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID, value = Dist.CLIENT)
public final class HudPin {

    private static final int EDGE = 4;
    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_TEXT_WIDTH = 180;
    private static final int BAR_WIDTH = 50;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_GAP = 4;
    private static final int BOX = 0x60000000;
    private static final int NAME = 0xFFFFD76A;
    private static final int TASK = 0xFFFFFFFF;
    private static final int DIM = 0xFFBBBBBB;
    private static final int BAR_EMPTY = 0xFF333333;
    private static final int BAR_COUNTED = 0xFF6FD14F;
    private static final int BAR_CARRIED = 0xFFFFD34A;

    private static boolean hidden;

    private HudPin() {
    }

    /** Hides the pin, or shows it again. */
    static void toggle() {
        hidden = !hidden;
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "hud_pin"), HudPin::render);
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        hidden = false;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        Optional<NoteList> pinned = ClientNotes.pinned().flatMap(ClientNotes::get).map(ListView::list);
        if (pinned.isEmpty()) {
            return;
        }
        Font font = minecraft.font;
        NoteList list = pinned.get();
        String name = NotebookScreen.ellipsized(font, list.name(), MAX_TEXT_WIDTH);
        List<PinLines.Line> lines = PinLines.of(list, NotesClientConfig.hudLines(), id -> GoalItems.name(id).getString(),
                id -> minecraft.player == null ? 0 : Carried.count(minecraft.player, id));
        int width = font.width(name);
        for (PinLines.Line line : lines) {
            width = Math.max(width, lineWidth(font, line));
        }
        int boxWidth = width + 2 * PADDING;
        int boxHeight = (lines.size() + 1) * LINE_HEIGHT + 2 * PADDING - 1;
        int x = switch (NotesClientConfig.hudCorner()) {
            case TOP_RIGHT -> graphics.guiWidth() - EDGE - boxWidth;
            case TOP_LEFT, LEFT_MIDDLE -> EDGE;
        };
        int y = switch (NotesClientConfig.hudCorner()) {
            case LEFT_MIDDLE -> (graphics.guiHeight() - boxHeight) / 2;
            case TOP_LEFT, TOP_RIGHT -> EDGE;
        };
        graphics.fill(x, y, x + boxWidth, y + boxHeight, BOX);
        int textX = x + PADDING;
        int lineY = y + PADDING;
        graphics.text(font, name, textX, lineY, NAME, true);
        for (PinLines.Line line : lines) {
            lineY += LINE_HEIGHT;
            drawLine(graphics, font, line, textX, lineY);
        }
    }

    private static int lineWidth(Font font, PinLines.Line line) {
        return switch (line) {
            case PinLines.TaskLine task -> Math.min(MAX_TEXT_WIDTH, font.width(task.label()));
            case PinLines.GoalLine goal -> goalNameWidth(font, goal) + 2 * BAR_GAP + BAR_WIDTH + font.width(countText(goal));
            case PinLines.More more -> font.width(moreText(more));
        };
    }

    private static void drawLine(GuiGraphicsExtractor graphics, Font font, PinLines.Line line, int x, int y) {
        switch (line) {
            case PinLines.TaskLine task -> graphics.text(font, NotebookScreen.ellipsized(font, task.label(), MAX_TEXT_WIDTH), x, y, TASK, true);
            case PinLines.GoalLine goal -> drawGoal(graphics, font, goal, x, y);
            case PinLines.More more -> graphics.text(font, moreText(more), x, y, DIM, true);
        }
    }

    private static void drawGoal(GuiGraphicsExtractor graphics, Font font, PinLines.GoalLine goal, int x, int y) {
        int nameWidth = goalNameWidth(font, goal);
        graphics.text(font, NotebookScreen.ellipsized(font, goal.itemName(), nameWidth), x, y, DIM, true);
        int barX = x + nameWidth + BAR_GAP;
        int barY = y + 2;
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BAR_EMPTY);
        GoalFill fill = goal.fill();
        int countedEnd = barX + Math.round(BAR_WIDTH * fill.counted());
        int carriedEnd = barX + Math.round(BAR_WIDTH * (fill.counted() + fill.carried()));
        graphics.fill(barX, barY, countedEnd, barY + BAR_HEIGHT, BAR_COUNTED);
        graphics.fill(countedEnd, barY, carriedEnd, barY + BAR_HEIGHT, BAR_CARRIED);
        graphics.text(font, countText(goal), barX + BAR_WIDTH + BAR_GAP, y, DIM, true);
    }

    private static int goalNameWidth(Font font, PinLines.GoalLine goal) {
        int room = MAX_TEXT_WIDTH - 2 * BAR_GAP - BAR_WIDTH - font.width(countText(goal));
        return Math.min(room, font.width(goal.itemName()));
    }

    private static String countText(PinLines.GoalLine goal) {
        return goal.count() + "/" + goal.target();
    }

    private static Component moreText(PinLines.More more) {
        return Component.translatable("hud.servus_notes.pin.more", more.count());
    }
}
