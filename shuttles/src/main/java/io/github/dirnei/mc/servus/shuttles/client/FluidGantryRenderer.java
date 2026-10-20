package io.github.dirnei.mc.servus.shuttles.client;

import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.CAP_BOTTOM;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.FLUID_GAP;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.FLUID_INSET;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.NOZZLE_BOTTOM;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.NOZZLE_HEIGHT;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.NOZZLE_MAX;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.NOZZLE_MIN;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PIPE_MAX;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PIPE_MIN;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PIPE_TOP;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_OUT;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_X0;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_X1;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_Y1;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLINTH_TOP;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.STREAM_MAX;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.STREAM_MIN;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.WINDOW_U0;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.WINDOW_U1;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.WINDOW_V0;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.WINDOW_V1;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlock;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryLook;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryPart;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what the Fluid Gantry shows beyond its block models, from its master
 * (the left leg): the fluid of each tank as a column inside its leg's glass,
 * from the plinth up to the synced fill, with the fluid's still texture and
 * its tint at the gantry; and on both lock plates of a locked tank the locked
 * fluid's texture over the striped Any window; and the spout's inner pipe
 * and nozzle, which lower out of the sleeve onto a tanker that stops below
 * and raise again, with a stream of the fluid between nozzle and cap that
 * flows up or down. Everything is laid out for a rail running north to south
 * and turned a quarter for one running east to west.
 */
public final class FluidGantryRenderer extends LiveBlockEntityRenderer<FluidGantryBlockEntity> {

    /** Gap between a lock plate's face and the fluid drawn on it. */
    private static final float PLATE_GAP = 0.01F;
    private static final float PX = 1 / 16F;
    private static final int UP = 1 << Direction.UP.ordinal();
    private static final int DOWN = 1 << Direction.DOWN.ordinal();
    private static final int SIDES_AND_DOWN = 0b111111 & ~UP;
    private static final int ALL = 0b111111;
    private static final int SIDES = 0b111111 & ~UP & ~DOWN;
    private static final Identifier IRON = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/heavy_rack_iron");
    private static final Identifier COPPER = Identifier.withDefaultNamespace("block/copper_block");
    /** Ticks the spout takes to lower onto a tanker or to rise again. */
    private static final float LOWER_TICKS = 8;
    /** Stream textures flow by this many sprite heights per tick. */
    private static final float FLOW_PER_TICK = 0.05F;
    private static final int OPAQUE = 0xFFFFFFFF;

    public FluidGantryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(FluidGantryBlockEntity gantry, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Level level = gantry.getLevel();
        BlockState state = gantry.getBlockState();
        if (level == null || !(state.getBlock() instanceof FluidGantryBlock)) {
            return;
        }
        BlockPos pos = gantry.getBlockPos();
        poseStack.pushPose();
        if (state.getValue(FluidGantryBlock.AXIS) == Direction.Axis.X) {
            // The models turn a quarter clockwise seen from above for a rail running east to west.
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            poseStack.translate(-0.5, 0, -0.5);
        }
        for (int tank = 0; tank < 2; tank++) {
            BlockPos leg = FluidGantryBlock.partPos(pos, state, tank == 0 ? GantryPart.LEFT_LOW : GantryPart.RIGHT_LOW);
            poseStack.pushPose();
            poseStack.translate(tank * 2, 0, 0);
            column(level, leg, gantry.shownFluid(tank), gantry.shownFill(tank), poseStack, collector);
            Fluid lock = gantry.lock(tank);
            if (lock != null) {
                plates(level, leg.above(), new FluidStack(lock, 1000), poseStack, collector);
            }
            poseStack.popPose();
        }
        spout(gantry, level, partialTick, poseStack, collector);
        poseStack.popPose();
    }

    /**
     * The inner pipe and nozzle in the beam block, lowered by how far the
     * last stop start or end is past, and the stream once the nozzle is down.
     */
    private static void spout(FluidGantryBlockEntity gantry, Level level, float partialTick, PoseStack poseStack, SubmitNodeCollector collector) {
        FluidGantryBlockEntity.Pumping pumping = gantry.shownPumping();
        float since = (level.getGameTime() - gantry.pumpChangedAt() + partialTick) / LOWER_TICKS;
        float down = Mth.clamp(pumping != null ? since : 1 - since, 0, 1);
        float eased = down * down * (3 - 2 * down);
        float nozzleBottom = NOZZLE_BOTTOM - GantryLook.spoutDrop() * eased;
        float nozzleTop = nozzleBottom + NOZZLE_HEIGHT;
        BlockPos beam = FluidGantryBlock.partPos(gantry.getBlockPos(), gantry.getBlockState(), GantryPart.BEAM);
        int light = LevelRenderer.getLightCoords(level, beam);
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        TextureAtlasSprite iron = atlas.getSprite(IRON);
        TextureAtlasSprite copper = atlas.getSprite(COPPER);
        FluidBox pipe = new FluidBox(PIPE_MIN * PX, nozzleTop * PX, PIPE_MIN * PX, PIPE_MAX * PX, PIPE_TOP * PX, PIPE_MAX * PX);
        FluidBox nozzle = new FluidBox(NOZZLE_MIN * PX, nozzleBottom * PX, NOZZLE_MIN * PX, NOZZLE_MAX * PX, nozzleTop * PX, NOZZLE_MAX * PX);
        poseStack.pushPose();
        poseStack.translate(1, 1, 0);
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, consumer) -> {
            pipe.draw(consumer, pose, ALL & ~DOWN, iron, OPAQUE, light);
            nozzle.draw(consumer, pose, ALL, copper, OPAQUE, light);
        });
        if (pumping != null && down >= 1) {
            FluidStack fluid = new FluidStack(pumping.fluid(), 1000);
            float ticks = level.getGameTime() + partialTick;
            stream(level, beam, fluid, nozzleBottom - GantryLook.STREAM_GAP, nozzleBottom, pumping.intoGantry() ? ticks : -ticks,
                    poseStack, collector);
        }
        poseStack.popPose();
    }

    /**
     * A thin column of the fluid's still texture from {@code bottom} to
     * {@code top} (pixels of the beam block), sides only, its texture moved
     * up by {@code flow} ticks so it runs into the gantry or out of it.
     */
    private static void stream(Level level, BlockPos beam, FluidStack fluid, float bottom, float top, float flow, PoseStack poseStack,
                               SubmitNodeCollector collector) {
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        int color = level instanceof BlockAndTintGetter tinted ? FluidSprites.tint(tinted, beam, fluid) : FluidSprites.tint(fluid);
        int light = glowing(LevelRenderer.getLightCoords(level, beam), fluid);
        float length = (top - bottom) / 16;
        float start = Mth.frac(-flow * FLOW_PER_TICK);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, consumer) -> {
            float y0 = bottom * PX;
            float y1 = top * PX;
            if (start + length <= 1) {
                streamSides(consumer, pose, y0, y1, start, start + length, sprite, color, light);
                return;
            }
            // The texture window wraps past the sprite's end: the lower part shows its end, the upper its start.
            float split = y0 + (1 - start) * 16 * PX;
            streamSides(consumer, pose, y0, split, start, 1, sprite, color, light);
            streamSides(consumer, pose, split, y1, 0, start + length - 1, sprite, color, light);
        });
    }

    /** The four sides of the stream between heights {@code y0} and {@code y1}, with v from {@code v0} at the bottom to {@code v1} at the top. */
    private static void streamSides(VertexConsumer consumer, PoseStack.Pose pose, float y0, float y1, float v0, float v1,
                                    TextureAtlasSprite sprite, int color, int light) {
        float a = STREAM_MIN * PX;
        float b = STREAM_MAX * PX;
        float u0 = sprite.getU(0.45F);
        float u1 = sprite.getU(0.55F);
        float vb = sprite.getV(1 - v0);
        float vt = sprite.getV(1 - v1);
        side(consumer, pose, a, a, b, a, y0, y1, u0, u1, vb, vt, color, light, Direction.NORTH);
        side(consumer, pose, b, b, a, b, y0, y1, u0, u1, vb, vt, color, light, Direction.SOUTH);
        side(consumer, pose, a, b, a, a, y0, y1, u0, u1, vb, vt, color, light, Direction.WEST);
        side(consumer, pose, b, a, b, b, y0, y1, u0, u1, vb, vt, color, light, Direction.EAST);
    }

    /** One upright quad from (xa, za) to (xb, zb), counter clockwise seen from its {@code face} side. */
    private static void side(VertexConsumer consumer, PoseStack.Pose pose, float xa, float za, float xb, float zb, float y0, float y1,
                             float u0, float u1, float vBottom, float vTop, int color, int light, Direction face) {
        vertex(consumer, pose, xb, y1, zb, u1, vTop, color, light, face);
        vertex(consumer, pose, xb, y0, zb, u1, vBottom, color, light, face);
        vertex(consumer, pose, xa, y0, za, u0, vBottom, color, light, face);
        vertex(consumer, pose, xa, y1, za, u0, vTop, color, light, face);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, int light,
                               Direction face) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
    }

    /** The fluid of one tank: a box in the lower leg block and, once it reaches higher, one in the upper block. */
    private static void column(Level level, BlockPos leg, FluidStack fluid, float fill, PoseStack poseStack, SubmitNodeCollector collector) {
        if (fluid.isEmpty() || fill <= 0) {
            return;
        }
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        int color = level instanceof BlockAndTintGetter tinted ? FluidSprites.tint(tinted, leg, fluid) : FluidSprites.tint(fluid);
        int light = glowing(LevelRenderer.getLightCoords(level, leg.above()), fluid);
        float in = FLUID_INSET * PX;
        float bottom = (PLINTH_TOP + FLUID_GAP) * PX;
        float top = bottom + ((1 + (CAP_BOTTOM - FLUID_GAP) * PX) - bottom) * fill;
        if (top <= 1) {
            FluidBox box = new FluidBox(in, bottom, in, 1 - in, top, 1 - in);
            collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(),
                    (pose, consumer) -> box.draw(consumer, pose, ALL, sprite, color, light));
            return;
        }
        FluidBox lower = new FluidBox(in, bottom, in, 1 - in, 1, 1 - in);
        FluidBox upper = new FluidBox(in, 0, in, 1 - in, top - 1, 1 - in);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(),
                (pose, consumer) -> lower.draw(consumer, pose, SIDES_AND_DOWN, sprite, color, light));
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(),
                (pose, consumer) -> upper.draw(consumer, pose, ALL & ~DOWN, sprite, color, light));
        poseStack.popPose();
    }

    /** The locked fluid over the window of both lock plates of the upper leg block. */
    private static void plates(Level level, BlockPos upperLeg, FluidStack lock, PoseStack poseStack, SubmitNodeCollector collector) {
        TextureAtlasSprite sprite = FluidSprites.still(lock);
        int color = FluidSprites.tint(lock);
        int light = glowing(LevelRenderer.getLightCoords(level, upperLeg), lock);
        float texel = (PLATE_X1 - PLATE_X0) / 16;
        float x0 = (PLATE_X0 + WINDOW_U0 * texel) * PX;
        float x1 = (PLATE_X0 + WINDOW_U1 * texel) * PX;
        float y0 = (PLATE_Y1 - WINDOW_V1 * texel) * PX;
        float y1 = (PLATE_Y1 - WINDOW_V0 * texel) * PX;
        float south = 1 + (PLATE_OUT + PLATE_GAP) * PX;
        float north = -(PLATE_OUT + PLATE_GAP) * PX;
        FluidBox front = new FluidBox(x0, y0, north, x1, y1, south);
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, consumer) -> {
            front.quad(consumer, pose, Direction.SOUTH, sprite, color, light);
            front.quad(consumer, pose, Direction.NORTH, sprite, color, light);
        });
        poseStack.popPose();
    }

    /** The light at the leg, at least the fluid's own glow. */
    private static int glowing(int packedLight, FluidStack fluid) {
        int glow = fluid.getFluidType().getLightLevel(fluid);
        return LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(packedLight), glow), LightCoordsUtil.sky(packedLight));
    }

    /** The whole frame, three blocks across the rail and two tall. */
    @Override
    public AABB getRenderBoundingBox(FluidGantryBlockEntity gantry) {
        BlockPos pos = gantry.getBlockPos();
        boolean alongX = gantry.getBlockState().getValue(FluidGantryBlock.AXIS) == Direction.Axis.X;
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + (alongX ? 1 : 3), pos.getY() + 2, pos.getZ() + (alongX ? 3 : 1))
                .inflate(PX);
    }
}
