package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.block.TankLabel;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Reveal;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Draws the side modes on the block while the player holds the carrot or
 * wears the goggles: a frame in the mode colour and a glyph on every face.
 * A carrot set to Fluids, or a block with only a Fluids layer, shows the Fluids
 * layer instead: the mode's glyph in the mode colour, framed in the colour of the
 * side's tank, or the All colour on All.
 */
public class SideMarkerRenderer<T extends BlockEntity & SidedBlockEntity>
        implements BlockEntityRenderer<T, SideMarkerRenderer.State> {

    private static final float FRAME = 1.0F / 16.0F;
    private static final float OFFSET = 1.0F / 256.0F;
    private static final float GLYPH = 4.0F / 16.0F;
    private static final float STROKE = 1.5F / 16.0F;

    /** Per face what to draw this frame: no mode means nothing on that face. */
    public static final class State extends BlockEntityRenderState {
        final SideMode[] modes = new SideMode[6];
        final int[] colors = new int[6];
        final int[] frames = new int[6];
    }

    public SideMarkerRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Whether the local player holds the carrot or wears the goggles. */
    public static boolean configuring() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return Reveal.configuring(player);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T blockEntity, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        Arrays.fill(state.modes, null);
        if (!configuring()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        SideLayer held = player == null ? null : ConfigurationCarrotItem.heldLayer(player);
        if (blockEntity.layerFor(held == null ? SideLayer.ITEMS : held) == SideLayer.FLUIDS) {
            extractFluids(blockEntity, blockEntity.fluidSides(), state);
            return;
        }
        for (Direction direction : Direction.values()) {
            if (blockEntity.sideConfig().isLocked(blockEntity.relativeSide(direction))) {
                continue;
            }
            SideMode mode = blockEntity.sideMode(direction);
            state.modes[direction.ordinal()] = mode;
            state.colors[direction.ordinal()] = SideConfigPanel.color(mode);
            state.frames[direction.ordinal()] = SideConfigPanel.color(mode);
        }
    }

    private void extractFluids(T blockEntity, FluidSides fluids, State state) {
        List<TankLabel> labels = blockEntity.sideTankLabels();
        for (Direction direction : Direction.values()) {
            RelativeSide side = blockEntity.relativeSide(direction);
            if (fluids.isLocked(side)) {
                continue;
            }
            SideMode mode = fluids.mode(side);
            state.modes[direction.ordinal()] = mode;
            state.colors[direction.ordinal()] = SideConfigPanel.color(mode);
            state.frames[direction.ordinal()] = SideConfigPanel.tankColor(fluids.tank(side), labels);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        SideMode[] modes = state.modes.clone();
        int[] colors = state.colors.clone();
        int[] frames = state.frames.clone();
        boolean any = false;
        for (SideMode mode : modes) {
            any |= mode != null;
        }
        if (!any) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            for (Direction direction : Direction.values()) {
                SideMode mode = modes[direction.ordinal()];
                if (mode == null) {
                    continue;
                }
                Face face = new Face(direction);
                face.frame(buffer, matrix, frames[direction.ordinal()]);
                face.glyph(buffer, matrix, mode, colors[direction.ordinal()]);
            }
        });
    }

    /** Geometry of one block face: its centre just outside the block and two in plane axes. */
    private static final class Face {
        private final float cx;
        private final float cy;
        private final float cz;
        private final float ux;
        private final float uy;
        private final float uz;
        private final float vx;
        private final float vy;
        private final float vz;

        Face(Direction direction) {
            float nx = direction.getStepX();
            float ny = direction.getStepY();
            float nz = direction.getStepZ();
            cx = 0.5F + nx * (0.5F + OFFSET);
            cy = 0.5F + ny * (0.5F + OFFSET);
            cz = 0.5F + nz * (0.5F + OFFSET);
            if (direction.getAxis().isVertical()) {
                ux = 1;
                uy = 0;
                uz = 0;
                vx = 0;
                vy = 0;
                vz = 1;
            } else {
                ux = -nz;
                uy = 0;
                uz = nx;
                vx = 0;
                vy = 1;
                vz = 0;
            }
        }

        private void vertex(VertexConsumer buffer, Matrix4f pose, float a, float b, int color) {
            buffer.addVertex(pose, cx + ux * a + vx * b, cy + uy * a + vy * b, cz + uz * a + vz * b).setColor(color);
        }

        /** An axis aligned rectangle in face coordinates, drawn twice so it shows from both sides. */
        private void rect(VertexConsumer buffer, Matrix4f pose, float a0, float b0, float a1, float b1, int color) {
            vertex(buffer, pose, a0, b0, color);
            vertex(buffer, pose, a0, b1, color);
            vertex(buffer, pose, a1, b1, color);
            vertex(buffer, pose, a1, b0, color);
            vertex(buffer, pose, a1, b0, color);
            vertex(buffer, pose, a1, b1, color);
            vertex(buffer, pose, a0, b1, color);
            vertex(buffer, pose, a0, b0, color);
        }

        void frame(VertexConsumer buffer, Matrix4f pose, int color) {
            float h = 0.5F;
            rect(buffer, pose, -h, h - FRAME, h, h, color);
            rect(buffer, pose, -h, -h, h, -h + FRAME, color);
            rect(buffer, pose, -h, -h + FRAME, -h + FRAME, h - FRAME, color);
            rect(buffer, pose, h - FRAME, -h + FRAME, h, h - FRAME, color);
        }

        /** Square for in, diamond for out, square with a dot for both, cross for off; active modes filled. */
        void glyph(VertexConsumer buffer, Matrix4f pose, SideMode mode, int color) {
            float g = GLYPH;
            switch (mode) {
                case INPUT -> outline(buffer, pose, g, color);
                case PULL -> rect(buffer, pose, -g, -g, g, g, color);
                case OUTPUT -> diamondOutline(buffer, pose, g, color);
                case PUSH -> diamond(buffer, pose, g, color);
                case BOTH -> {
                    outline(buffer, pose, g, color);
                    rect(buffer, pose, -STROKE, -STROKE, STROKE, STROKE, color);
                }
                case DISABLED -> {
                    bar(buffer, pose, -g, -g, g, g, color);
                    bar(buffer, pose, -g, g, g, -g, color);
                }
                case AUTO -> rect(buffer, pose, -STROKE / 2, -STROKE / 2, STROKE / 2, STROKE / 2, color);
            }
        }

        private void outline(VertexConsumer buffer, Matrix4f pose, float g, int color) {
            rect(buffer, pose, -g, g - STROKE, g, g, color);
            rect(buffer, pose, -g, -g, g, -g + STROKE, color);
            rect(buffer, pose, -g, -g + STROKE, -g + STROKE, g - STROKE, color);
            rect(buffer, pose, g - STROKE, -g + STROKE, g, g - STROKE, color);
        }

        private void quad(VertexConsumer buffer, Matrix4f pose, float[] a, float[] b, int color) {
            for (int i = 0; i < 4; i++) {
                vertex(buffer, pose, a[i], b[i], color);
            }
            for (int i = 3; i >= 0; i--) {
                vertex(buffer, pose, a[i], b[i], color);
            }
        }

        private void diamond(VertexConsumer buffer, Matrix4f pose, float g, int color) {
            quad(buffer, pose, new float[] {0, -g, 0, g}, new float[] {g, 0, -g, 0}, color);
        }

        private void diamondOutline(VertexConsumer buffer, Matrix4f pose, float g, int color) {
            bar(buffer, pose, 0, g, g, 0, color);
            bar(buffer, pose, g, 0, 0, -g, color);
            bar(buffer, pose, 0, -g, -g, 0, color);
            bar(buffer, pose, -g, 0, 0, g, color);
        }

        /** A stroke from one point to another in face coordinates. */
        private void bar(VertexConsumer buffer, Matrix4f pose, float a0, float b0, float a1, float b1, int color) {
            float da = a1 - a0;
            float db = b1 - b0;
            float length = (float) Math.sqrt(da * da + db * db);
            float na = -db / length * STROKE / 2;
            float nb = da / length * STROKE / 2;
            quad(buffer, pose, new float[] {a0 + na, a1 + na, a1 - na, a0 - na}, new float[] {b0 + nb, b1 + nb, b1 - nb, b0 - nb}, color);
        }
    }
}
