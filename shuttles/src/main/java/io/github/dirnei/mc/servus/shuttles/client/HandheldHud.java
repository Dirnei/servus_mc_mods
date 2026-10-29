package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelColours;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBinding;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldFindResultPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldInfoPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The handheld's screen as a HUD panel in the bottom right corner while the
 * handheld is in either hand: a green LCD in a dark body with a yellow
 * bumper. The top line shows the mode and the aimed bay's location code; the
 * lines below show what the mode describes.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class HandheldHud {

    private static final String KEY = "hud.servus_shuttles.handheld.";
    private static final int BUMPER = 0xFFE0A325;
    private static final int BUMPER_EDGE = 0xFF6B4A0C;
    private static final int BODY = 0xFF2E333B;
    private static final int LCD = 0xFF0F1F12;
    private static final int LCD_TEXT = 0xFF8DFF9A;
    private static final int LCD_DIM = 0xFF3F8A4A;
    private static final int LASER = 0xFFC0201F;
    private static final int LCD_WIDTH = 132;
    private static final int LINE = 9;
    private static final int LINES = 7;
    private static final int PAD = 4;
    private static final int RIGHT_GAP = 6;
    private static final int BOTTOM_GAP = 26;
    private static final String NO_CODE = "------";

    private HandheldHud() {
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld"),
                HandheldHud::render);
    }

    /** One line of the LCD: text, dimmed or not, with an optional colour swatch before it. */
    private record Line(String text, boolean dim, int swatch) {
        static Line of(String text) {
            return new Line(text, false, 0);
        }

        static Line dim(String text) {
            return new Line(text, true, 0);
        }
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.level == null) {
            return;
        }
        ItemStack handheld = ClientHandheld.held(player);
        if (handheld.isEmpty()) {
            return;
        }
        HandheldMode mode = handheld.getOrDefault(ServusShuttlesComponents.HANDHELD_MODE.get(), HandheldMode.ZONE);
        StorageNodeBlockEntity aimed = ClientHandheld.aimedBay();
        List<Line> lines = switch (mode) {
            case ZONE -> zoneLines(handheld, aimed);
            case FIND -> findLines(handheld);
            case INFO -> infoLines(handheld, aimed, minecraft.level.getGameTime());
        };
        draw(graphics, minecraft.font, mode, aimed, lines);
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, HandheldMode mode, StorageNodeBlockEntity aimed, List<Line> lines) {
        int lcdHeight = PAD * 2 + LINE * (LINES + 1);
        int x = graphics.guiWidth() - LCD_WIDTH - RIGHT_GAP - 6;
        int y = graphics.guiHeight() - lcdHeight - BOTTOM_GAP - 6;
        int w = LCD_WIDTH + 12;
        int h = lcdHeight + 12;
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, BUMPER_EDGE);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, BUMPER);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, BODY);
        graphics.fill(x + w / 2 - 20, y - 4, x + w / 2 + 20, y - 1, LASER);
        int lx = x + 6;
        int ly = y + 6;
        graphics.fill(lx, ly, lx + LCD_WIDTH, ly + lcdHeight, LCD);
        String code = aimed == null || aimed.locationCode().isEmpty() ? NO_CODE : aimed.locationCode();
        int tx = lx + PAD;
        int ty = ly + PAD;
        graphics.text(font, mode.getSerializedName().toUpperCase(Locale.ROOT), tx, ty, LCD_TEXT, false);
        graphics.text(font, code, lx + LCD_WIDTH - PAD - font.width(code), ty, LCD_TEXT, false);
        graphics.fill(tx, ty + LINE - 1, lx + LCD_WIDTH - PAD, ty + LINE, LCD_DIM);
        for (int i = 0; i < Math.min(LINES, lines.size()); i++) {
            Line line = lines.get(i);
            int lineY = ty + LINE * (i + 1) + 1;
            int textX = tx;
            if (line.swatch() != 0) {
                graphics.fill(tx, lineY, tx + 7, lineY + 7, line.swatch());
                textX += 10;
            }
            String text = font.plainSubstrByWidth(line.text(), lx + LCD_WIDTH - PAD - textX);
            graphics.text(font, text, textX, lineY, line.dim() ? LCD_DIM : LCD_TEXT, false);
        }
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(KEY + key, args).getString();
    }

    private static List<Line> zoneLines(ItemStack handheld, StorageNodeBlockEntity aimed) {
        List<Line> lines = new ArrayList<>();
        Integer active = handheld.get(ServusShuttlesComponents.HANDHELD_ZONE.get());
        ZoneDefinition zone = active == null ? null : ClientZones.definition(active);
        lines.add(Line.dim(tr("active")));
        if (active == null || active != ZoneList.NONE && zone == null) {
            lines.add(Line.of(tr("choose_zone")));
        } else if (zone == null) {
            lines.add(Line.of(tr("no_zone")));
        } else {
            lines.add(new Line(zone.name(), false, ChannelColours.argb(zone.colour())));
        }
        if (aimed != null) {
            ZoneDefinition bayZone = aimed.zoneDefinition();
            lines.add(Line.dim(tr("bay")));
            lines.add(bayZone == null ? Line.of(tr("no_zone")) : new Line(bayZone.name(), false, ChannelColours.argb(bayZone.colour())));
        }
        lines.add(Line.dim(tr("zone_hint")));
        return lines;
    }

    private static List<Line> findLines(ItemStack handheld) {
        List<Line> lines = new ArrayList<>();
        HandheldBinding binding = handheld.get(ServusShuttlesComponents.HANDHELD_BINDING.get());
        if (binding == null) {
            lines.add(Line.of(tr("not_bound")));
            lines.add(Line.dim(tr("bind_hint")));
            return lines;
        }
        lines.add(Line.dim(binding.name()));
        HandheldFindResultPayload found = ClientHandheld.lastFind();
        if (found == null) {
            lines.add(Line.of(tr("find_hint")));
            return lines;
        }
        lines.add(Line.of(found.item().getHoverName().getString()));
        lines.add(Line.of(tr("total", found.total())));
        if (found.codes().isEmpty()) {
            lines.add(Line.dim(tr("no_bays")));
        }
        for (String code : found.codes()) {
            lines.add(Line.of(code));
        }
        return lines;
    }

    private static List<Line> infoLines(ItemStack handheld, StorageNodeBlockEntity aimed, long now) {
        List<Line> lines = new ArrayList<>();
        BlockPos pinned = handheld.get(ServusShuttlesComponents.HANDHELD_PIN.get());
        StorageNodeBlockEntity bay = pinned != null ? pinnedBay(pinned) : aimed;
        if (bay == null) {
            lines.add(Line.of(tr("aim_hint")));
            return lines;
        }
        ClientHandheld.requestInfo(bay.getBlockPos(), now);
        String code = bay.locationCode().isEmpty() ? NO_CODE : bay.locationCode();
        lines.add(Line.of(pinned != null ? tr("pinned", code) : code));
        ZoneDefinition zone = bay.zoneDefinition();
        if (zone == null) {
            lines.add(Line.of(tr("no_zone")));
        } else {
            lines.add(new Line(zone.name(), false, ChannelColours.argb(zone.colour())));
            lines.add(Line.dim(HandheldScreen.filterSummary(zone.filter())));
        }
        HandheldInfoPayload info = ClientHandheld.infoFor(bay.getBlockPos());
        if (info == null) {
            return lines;
        }
        lines.add(Line.of(info.fluid() ? tr("fluid", info.used(), info.total()) : tr("slots", info.used(), info.total())));
        for (int i = 0; i < info.top().size(); i++) {
            String count = info.fluid() ? tr("mb", info.counts().get(i)) : Long.toString(info.counts().get(i));
            lines.add(Line.of(info.top().get(i).getHoverName().getString() + " " + count));
        }
        return lines;
    }

    private static StorageNodeBlockEntity pinnedBay(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.level.isLoaded(pos)
                && minecraft.level.getBlockEntity(pos) instanceof StorageNodeBlockEntity bay && bay.zonable() ? bay : null;
    }
}
