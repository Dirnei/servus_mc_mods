package io.github.dirnei.mc.servus.manufacturing.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * Dev only: renders every item of the game data export as an inventory icon PNG for the documentation site, then
 * quits the game. Active only when the {@code iconExport} run sets {@code servus.icons.output} (the icon folder) and
 * {@code servus.icons.gamedata} (the {@code gamedata.json} listing the items). Icons go to
 * {@code <output>/<namespace>/<path>.png}.
 */
public final class IconExporter {

    /** Pixels per side: the GUI's 16 units drawn at four times their size. */
    private static final int SIZE = 64;

    private static boolean done;

    private IconExporter() {
    }

    /** Hooks the exporter in when the run asks for it. */
    public static void registerIfRequested() {
        if (System.getProperty("servus.icons.output") != null && System.getProperty("servus.icons.gamedata") != null) {
            NeoForge.EVENT_BUS.addListener(IconExporter::onScreenRender);
        }
    }

    /** Runs once, on the first title screen frame, where the render thread and a GL context are ready. */
    private static void onScreenRender(ScreenEvent.Render.Post event) {
        if (done || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        done = true;
        Path output = Path.of(System.getProperty("servus.icons.output"));
        try {
            List<String> ids = itemIds(Path.of(System.getProperty("servus.icons.gamedata")));
            int written = export(ids, output);
            ServusManufacturing.LOGGER.info("Icon export: {} icons written to {}", written, output);
        } catch (IOException | RuntimeException e) {
            ServusManufacturing.LOGGER.error("Icon export failed", e);
        }
        Minecraft.getInstance().stop();
    }

    private static List<String> itemIds(Path gamedata) throws IOException {
        try (Reader reader = Files.newBufferedReader(gamedata, StandardCharsets.UTF_8)) {
            JsonObject items = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("items");
            return new ArrayList<>(items.keySet());
        }
    }

    private static int export(List<String> ids, Path output) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        TextureTarget target = new TextureTarget(SIZE, SIZE, true, Minecraft.ON_OSX);
        target.setClearColor(0, 0, 0, 0);

        // The GUI projection of GameRenderer, scaled so one 16 unit item fills the target.
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 16, 16, 0, 1000, ClientHooks.getGuiFarPlane()),
                VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.translation(0, 0, 10000F - ClientHooks.getGuiFarPlane());
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();

        int written = 0;
        try {
            for (String id : ids) {
                Identifier location = Identifier.parse(id);
                Item item = BuiltInRegistries.ITEM.getValue(location);
                if (item == Items.AIR) {
                    ServusManufacturing.LOGGER.warn("Icon export: unknown item {}", id);
                    continue;
                }
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(minecraft, minecraft.renderBuffers().bufferSource());
                graphics.item(new ItemStack(item), 0, 0);
                graphics.flush();

                Path file = output.resolve(location.getNamespace()).resolve(location.getPath() + ".png");
                Files.createDirectories(file.getParent());
                try (NativeImage image = new NativeImage(SIZE, SIZE, false)) {
                    RenderSystem.bindTexture(target.getColorTextureId());
                    image.downloadTexture(0, false);
                    image.flipY();
                    image.writeToFile(file);
                }
                written++;
            }
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
        return written;
    }
}
