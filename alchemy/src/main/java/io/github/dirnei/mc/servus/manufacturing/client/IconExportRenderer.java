package io.github.dirnei.mc.servus.manufacturing.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jspecify.annotations.Nullable;

/**
 * Dev only: draws every item of an {@link IconExportRenderState} into an own 64x64 texture, the way the GUI item
 * atlas draws a slot, reads the pixels back from the GPU and writes them as PNGs. Runs inside the GUI render pass so
 * the GUI lightmap and lighting apply. The picture in picture texture of the base class stays empty: it cannot be a
 * copy source.
 */
public final class IconExportRenderer extends PictureInPictureRenderer<IconExportRenderState> {

    /** Pixels per side: the GUI's 16 units drawn at four times their size. */
    static final int SIZE = 64;

    private static final int FULL_BRIGHT = 15728880;
    private static final long FENCE_TIMEOUT_MS = 10_000L;

    private final Path output;
    private final IntConsumer onDone;
    private final Projection projection = new Projection();
    private @Nullable ProjectionMatrixBuffer projectionBuffer;
    private @Nullable GpuTexture color;
    private @Nullable GpuTextureView colorView;
    private @Nullable GpuTexture depth;
    private @Nullable GpuTextureView depthView;
    private @Nullable GpuBuffer readback;

    /** {@code onDone} receives the number of icons written, also after a failure. */
    public IconExportRenderer(MultiBufferSource.BufferSource bufferSource, Path output, IntConsumer onDone) {
        super(bufferSource);
        this.output = output;
        this.onDone = onDone;
    }

    @Override
    public Class<IconExportRenderState> getRenderStateClass() {
        return IconExportRenderState.class;
    }

    @Override
    protected void renderToTexture(IconExportRenderState state, PoseStack unused) {
        GpuTextureView colorOverride = RenderSystem.outputColorTextureOverride;
        GpuTextureView depthOverride = RenderSystem.outputDepthTextureOverride;
        RenderSystem.backupProjectionMatrix();
        int written = 0;
        try {
            createTargets();
            for (IconExportRenderState.Icon icon : state.icons()) {
                draw(icon);
                Path file = output.resolve(icon.id().getNamespace()).resolve(icon.id().getPath() + ".png");
                Files.createDirectories(file.getParent());
                readTo(file);
                written++;
            }
        } catch (IOException | RuntimeException e) {
            ServusManufacturing.LOGGER.error("Icon export failed after {} icons", written, e);
        } finally {
            RenderSystem.outputColorTextureOverride = colorOverride;
            RenderSystem.outputDepthTextureOverride = depthOverride;
            RenderSystem.restoreProjectionMatrix();
            closeTargets();
            onDone.accept(written);
        }
    }

    private void createTargets() {
        GpuDevice device = RenderSystem.getDevice();
        color = device.createTexture(() -> "Servus icon export", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC
                | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, SIZE, SIZE, 1, 1);
        colorView = device.createTextureView(color);
        // Same depth format as the main target, as the picture in picture renderers do (stencil when enabled).
        TextureFormat depthFormat = Minecraft.getInstance().getMainRenderTarget().getDepthTexture().getFormat();
        depth = device.createTexture(() -> "Servus icon export depth",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_RENDER_ATTACHMENT, depthFormat, SIZE, SIZE, 1, 1);
        depthView = device.createTextureView(depth);
        readback = device.createBuffer(() -> "Servus icon export readback",
                GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, (long) SIZE * SIZE * TextureFormat.RGBA8.pixelSize());
        projectionBuffer = new ProjectionMatrixBuffer("Servus icon export");
        projection.setupOrtho(-1000.0F, 1000.0F, SIZE, SIZE, true);
    }

    /** Draws one item centred in the cleared texture, as {@code GuiItemAtlas.drawToSlot} draws a slot. */
    private void draw(IconExportRenderState.Icon icon) {
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(color, 0, depth, 1.0);
        RenderSystem.outputColorTextureOverride = colorView;
        RenderSystem.outputDepthTextureOverride = depthView;
        RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC);
        PoseStack pose = new PoseStack();
        pose.translate(SIZE / 2.0F, SIZE / 2.0F, 0.0F);
        pose.scale(SIZE, -SIZE, SIZE);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting()
                .setupFor(icon.item().usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
        FeatureRenderDispatcher dispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
        icon.item().submit(pose, dispatcher.getSubmitNodeStorage(), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        dispatcher.renderAllFeatures();
        bufferSource.endBatch();
    }

    /** Copies the texture into the readback buffer, waits for the GPU and writes the pixels top row first. */
    private void readTo(Path file) throws IOException {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToBuffer(color, readback, 0L, () -> {
        }, 0);
        // The OpenGL fence hands its timeout to glClientWaitSync, which counts nanoseconds; poll with a short wait
        // and keep the deadline here.
        long deadline = System.nanoTime() + FENCE_TIMEOUT_MS * 1_000_000L;
        try (GpuFence fence = encoder.createFence()) {
            while (!fence.awaitCompletion(1L)) {
                if (System.nanoTime() > deadline) {
                    throw new IOException("GPU did not finish the icon copy for " + file);
                }
                Thread.onSpinWait();
            }
        }
        try (GpuBuffer.MappedView view = encoder.mapBuffer(readback, true, false);
             NativeImage image = new NativeImage(SIZE, SIZE, false)) {
            ByteBuffer data = view.data();
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int at = (x + y * SIZE) * 4;
                    int r = data.get(at) & 0xFF;
                    int g = data.get(at + 1) & 0xFF;
                    int b = data.get(at + 2) & 0xFF;
                    int a = data.get(at + 3) & 0xFF;
                    // The texture's first row is the bottom one.
                    image.setPixelABGR(x, SIZE - 1 - y, a << 24 | b << 16 | g << 8 | r);
                }
            }
            image.writeToFile(file);
        }
    }

    private void closeTargets() {
        if (readback != null) {
            readback.close();
            readback = null;
        }
        if (colorView != null) {
            colorView.close();
            colorView = null;
        }
        if (color != null) {
            color.close();
            color = null;
        }
        if (depthView != null) {
            depthView.close();
            depthView = null;
        }
        if (depth != null) {
            depth.close();
            depth = null;
        }
        if (projectionBuffer != null) {
            projectionBuffer.close();
            projectionBuffer = null;
        }
    }

    @Override
    public void close() {
        closeTargets();
        super.close();
    }

    @Override
    protected String getTextureLabel() {
        return "servus_icon_export";
    }
}
