package io.github.dirnei.mc.servus.alchemy.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Dev only: renders every item of the game data export as an inventory icon PNG for the documentation site, then
 * quits the game. Active only when the {@code iconExport} run sets {@code servus.icons.output} (the icon folder) and
 * {@code servus.icons.gamedata} (the {@code gamedata.json} listing the items). Icons go to
 * {@code <output>/<namespace>/<path>.png}. The drawing and the GPU readback happen in {@link IconExportRenderer}.
 */
public final class IconExporter {

    private static boolean submitted;
    private static volatile boolean done;

    private IconExporter() {
    }

    private static boolean requested() {
        return System.getProperty("servus.icons.output") != null && System.getProperty("servus.icons.gamedata") != null;
    }

    private static Path output() {
        return Path.of(System.getProperty("servus.icons.output"));
    }

    /** Hooks the exporter in when the run asks for it. */
    public static void registerIfRequested() {
        if (requested()) {
            NeoForge.EVENT_BUS.addListener(IconExporter::onScreenRender);
        }
    }

    /** Registers the icon renderer when the run asks for it; the normal client never gets it. */
    public static void registerRendererIfRequested(RegisterPictureInPictureRenderersEvent event) {
        if (requested()) {
            Path output = output();
            event.register(IconExportRenderState.class, buffers -> new IconExportRenderer(buffers, output, written -> {
                ServusAlchemy.LOGGER.info("Icon export: {} icons written to {}", written, output);
                done = true;
            }));
        }
    }

    /**
     * On the first title screen frame after loading, submits all icons once; the GUI render pass of that frame draws
     * them. The next title screen frame after the export quits the game.
     */
    private static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof TitleScreen) || minecraft.getOverlay() != null) {
            return;
        }
        if (done) {
            minecraft.stop();
            return;
        }
        if (submitted) {
            return;
        }
        submitted = true;
        try {
            bindItemComponents();
            List<IconExportRenderState.Icon> icons = icons(itemIds(Path.of(System.getProperty("servus.icons.gamedata"))));
            event.getGuiGraphics().submitPictureInPictureRenderState(IconExportRenderState.of(icons));
        } catch (IOException | RuntimeException e) {
            ServusAlchemy.LOGGER.error("Icon export failed", e);
            done = true;
        }
    }

    /**
     * Item stacks need their default components, which are bound only when a world loads its registries. The title
     * screen has none, so the export binds them from the built in registries, as vanilla's registry components report
     * does. Tags are not loaded there, so tag based components hold placeholder tag sets; NeoForge's dev only
     * component check rejects those, so it is switched off while binding. The game quits after the export, so no
     * world ever sees these bindings.
     */
    private static void bindItemComponents() {
        boolean ide = SharedConstants.IS_RUNNING_IN_IDE;
        SharedConstants.IS_RUNNING_IN_IDE = false;
        try {
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                    .forEach(DataComponentInitializers.PendingComponents::apply);
        } finally {
            SharedConstants.IS_RUNNING_IN_IDE = ide;
        }
    }

    private static List<String> itemIds(Path gamedata) throws IOException {
        try (Reader reader = Files.newBufferedReader(gamedata, StandardCharsets.UTF_8)) {
            JsonObject items = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("items");
            return new ArrayList<>(items.keySet());
        }
    }

    /** The GUI model of every known item; unknown ids are logged and skipped. */
    private static List<IconExportRenderState.Icon> icons(List<String> ids) {
        Minecraft minecraft = Minecraft.getInstance();
        List<IconExportRenderState.Icon> icons = new ArrayList<>();
        for (String id : ids) {
            Identifier location = Identifier.parse(id);
            Item item = BuiltInRegistries.ITEM.getValue(location);
            if (item == null || item == Items.AIR) {
                ServusAlchemy.LOGGER.warn("Icon export: unknown item {}", id);
                continue;
            }
            ItemStackRenderState model = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(model, new ItemStack(item), ItemDisplayContext.GUI, null,
                    null, 0);
            icons.add(new IconExportRenderState.Icon(location, model));
        }
        return icons;
    }
}
