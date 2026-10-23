package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.athanor.PlacedBlocks;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Turn;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualTimeline;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.ModelPoses;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the item lying on top of the Athanor, or the result once it is made; it turns slowly while
 * the fire burns. The pipes to the tanks are the Copper Ducts of the build. While a ritual runs it draws
 * the ritual from the game time, so every player sees the same moment: the offering rising and spinning
 * over the plate, the flame in the ritual's colour, the streams of light to the blocks it turns, and those
 * blocks growing until the server turns them.
 */
public class AthanorRenderer extends BufferedBlockEntityRenderer<AthanorBlockEntity> {

    /** Where the item rests: on the tower's iron plate when formed, on the block's top when not. */
    private static final double PLATE = 23.5 / 16.0 + 0.02;
    private static final int FULL_LIGHT = LightCoordsUtil.pack(15, 15);
    /** The white flame, tinted in the ritual's colour over the stage's own. */
    private static final Identifier FIRE = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "block/athanor_fire_1");
    private static final Identifier MOTE = Identifier.withDefaultNamespace("block/white_concrete");
    /** The fire door's window in pixels of the north built tower; the tint lies a twentieth of a pixel before its flame. */
    private static final float WINDOW_X0 = 6;
    private static final float WINDOW_X1 = 10;
    private static final float WINDOW_Y0 = 7;
    private static final float WINDOW_Y1 = 10.5F;
    private static final float WINDOW_Z = 0.7F;
    private static final float FLAME_ALPHA = 0.85F;
    /** How high the streams arc over the burst point, in blocks. */
    private static final double ARC = 1.6;
    /** Where a stream ends above its block's floor, in blocks. */
    private static final double LANDING = 0.75;

    public AthanorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(AthanorBlockEntity athanor) {
        AABB own = new AABB(athanor.getBlockPos()).expandTowards(0, 1, 0);
        return athanor.ritual() == null ? own : own.inflate(4, 2, 4).expandTowards(0, 2, 0);
    }

    @Override
    protected void render(AthanorBlockEntity athanor, float partialTick, PoseStack poseStack, SubmitBuffers buffers,
                          int packedLight) {
        Level level = athanor.getLevel();
        if (level == null) {
            return;
        }
        drawItem(athanor, level, partialTick, poseStack, buffers);
        AthanorRitual ritual = athanor.ritual();
        if (ritual != null && athanor.formed()) {
            float t = level.getGameTime() - athanor.ritualStart() + partialTick;
            drawRitual(athanor, level, new RitualTimeline(ritual), t, poseStack, buffers);
        }
    }

    private static void drawItem(AthanorBlockEntity athanor, Level level, float partialTick, PoseStack poseStack, SubmitBuffers buffers) {
        boolean formed = athanor.formed();
        ItemStack stack = athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT);
        if (stack.isEmpty()) {
            stack = athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT);
        }
        if (stack.isEmpty()) {
            return;
        }
        boolean lit = athanor.getBlockState().hasProperty(MachineBlock.LIT) && athanor.getBlockState().getValue(MachineBlock.LIT);
        float turn = lit ? (level.getGameTime() + partialTick) * 2F : 0F;
        poseStack.pushPose();
        poseStack.translate(0.5, formed ? PLATE : 1.02, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(turn));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        RenderSubmit.item(poseStack, buffers.collector(), stack, ItemDisplayContext.FIXED, level, 0, FULL_LIGHT);
        poseStack.popPose();
    }

    private static void drawRitual(AthanorBlockEntity athanor, Level level, RitualTimeline timeline, float t, PoseStack poseStack,
                                   SubmitBuffers buffers) {
        if (timeline.offeringShown(t)) {
            drawOffering(timeline, level, t, poseStack, buffers);
        }
        drawFlame(athanor, timeline, t, poseStack, buffers);
        List<Turn> turns = athanor.ritualTurns();
        for (int i = 0; i < turns.size(); i++) {
            Turn turn = turns.get(i);
            drawStream(timeline, turn, i, t, poseStack, buffers);
            BlockPos pos = athanor.getBlockPos().offset(turn.to().dx(), turn.to().dy(), turn.to().dz());
            RitualLooks.Look look = RitualLooks.of(turn.to().block());
            if (look != null && turn.from().matches(PlacedBlocks.placed(level.getBlockState(pos)))) {
                poseStack.pushPose();
                poseStack.translate(turn.to().dx(), turn.to().dy(), turn.to().dz());
                look.draw(poseStack, buffers, pos, turn.to(), timeline.fill(t, i), timeline.growth(t, i, turns.size()));
                poseStack.popPose();
            }
        }
    }

    /** The offering rises from the plate, spins faster and faster and swells to a hum. */
    private static void drawOffering(RitualTimeline timeline, Level level, float t, PoseStack poseStack, SubmitBuffers buffers) {
        ItemStack offering = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(timeline.ritual().offering())));
        float seconds = t / 20;
        float hum = 1 + (float) Math.sin(seconds * 14) * 0.06F * RitualTimeline.clamp01(t / (timeline.ritual().phases().burst() * 0.83F));
        poseStack.pushPose();
        poseStack.translate(0.5, AthanorBlockEntity.OFFERING_Y + AthanorBlockEntity.OFFERING_RISE * timeline.offeringRise(t)
                + Math.sin(seconds * 6) * 0.03, 0.5);
        poseStack.mulPose(Axis.YP.rotation(seconds * seconds * 3));
        poseStack.scale(0.8F * hum, 0.8F * hum, 0.8F * hum);
        RenderSubmit.item(poseStack, buffers.collector(), offering, ItemDisplayContext.FIXED, level, 0, FULL_LIGHT);
        poseStack.popPose();
    }

    /** The ritual's colour over the flame behind the fire door's window. */
    private static void drawFlame(AthanorBlockEntity athanor, RitualTimeline timeline, float t, PoseStack poseStack, SubmitBuffers buffers) {
        float alpha = timeline.flameTint(t) * FLAME_ALPHA;
        if (alpha <= 0) {
            return;
        }
        TextureAtlasSprite fire = sprite(FIRE);
        int argb = Math.round(alpha * 255) << 24 | timeline.flameColor(t);
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, athanor.getBlockState().getValue(MachineBlock.FACING));
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        float z = WINDOW_Z / 16;
        float x0 = WINDOW_X0 / 16;
        float x1 = WINDOW_X1 / 16;
        float y0 = WINDOW_Y0 / 16;
        float y1 = WINDOW_Y1 / 16;
        float u0 = fire.getU(0);
        float u1 = fire.getU(1);
        float v0 = fire.getV(0);
        float v1 = fire.getV(0.75F);
        // Counter clockwise seen from the front, the viewer's left at +x.
        flameVertex(consumer, pose, x1, y1, z, u0, v0, argb);
        flameVertex(consumer, pose, x1, y0, z, u0, v1, argb);
        flameVertex(consumer, pose, x0, y0, z, u1, v1, argb);
        flameVertex(consumer, pose, x0, y1, z, u1, v0, argb);
        poseStack.popPose();
    }

    private static void flameVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
        consumer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_LIGHT)
                .setNormal(pose, 0, 0, -1);
    }

    /** The lights of one stream along its arc from the burst over the tank into its block, turning to the result's colour. */
    private static void drawStream(RitualTimeline timeline, Turn turn, int index, float t, PoseStack poseStack, SubmitBuffers buffers) {
        AthanorRitual ritual = timeline.ritual();
        Vec3 from = new Vec3(0.5, AthanorBlockEntity.OFFERING_Y + AthanorBlockEntity.OFFERING_RISE, 0.5);
        Vec3 to = new Vec3(turn.to().dx() + 0.5, turn.to().dy() + LANDING, turn.to().dz() + 0.5);
        Vec3 mid = from.lerp(to, 0.5);
        mid = new Vec3(mid.x, from.y + ARC, mid.z);
        int start = RitualTimeline.mix(ritual.color(), 0xFFFFFF, 0.35F);
        int end = RitualTimeline.mix(ritual.resultColor(), 0xFFFFFF, 0.25F);
        TextureAtlasSprite white = sprite(MOTE);
        VertexConsumer consumer = null;
        for (int mote = 0; mote < RitualTimeline.MOTES; mote++) {
            float u = timeline.stream(t, index, mote);
            if (u <= 0 || u >= 1) {
                continue;
            }
            if (consumer == null) {
                consumer = buffers.getBuffer(Sheets.translucentBlockSheet());
            }
            double s = RitualTimeline.ease(u);
            Vec3 at = from.scale((1 - s) * (1 - s)).add(mid.scale(2 * (1 - s) * s)).add(to.scale(s * s));
            float half = 0.07F - mote * 0.005F;
            int alpha = Math.round(255 * (1 - mote / 12F));
            int argb = alpha << 24 | RitualTimeline.mix(start, end, u * 1.3F);
            // The box drawer maps the texture by the box's place in the unit cube, so the box is drawn at its centre.
            poseStack.pushPose();
            poseStack.translate(at.x - 0.5, at.y - 0.5, at.z - 0.5);
            CalcinerRenderer.drawBox(consumer, poseStack.last(), 0.5F - half, 0.5F - half, 0.5F - half,
                    0.5F + half, 0.5F + half, 0.5F + half, white, argb, FULL_LIGHT);
            poseStack.popPose();
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }
}
