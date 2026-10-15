package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.homunculus.HomunculusReach.Offset;
import io.github.dirnei.mc.servus.alchemy.homunculus.VasBlock;
import io.github.dirnei.mc.servus.alchemy.homunculus.VasBlockEntity;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Draws what Elemental Sight shows of the Vas Hermeticum: the pale homunculus in the flask, as small as its growth, and
 * a sagging chain from the neck to every Gnome Hollow in its reach; a freed homunculus sits on the rim without chains.
 * Without the sight it draws nothing.
 */
public final class VasRenderer extends BufferedBlockEntityRenderer<VasBlockEntity> {

    private static final float PX = 1 / 16F;
    private static final Identifier SKIN = Identifier.withDefaultNamespace("block/white_concrete");
    private static final Identifier EYES = Identifier.withDefaultNamespace("block/black_concrete");
    private static final Identifier STEEL = Identifier.fromNamespaceAndPath("servus_foundation", "block/electric_pump_steel");
    private static final int SKIN_TINT = 0xFFE8D2C4;
    private static final int CHAIN_TINT = 0xFF8D959F;
    /** Where the body's centre sits, in pixels: on the liquid in the flask, or on the open neck once freed. */
    private static final float INSIDE_Y = 4.3F;
    private static final float RIM_Y = 18.4F;
    /** The neck the chains hang from, and how far from its centre they leave it, in blocks. */
    private static final Vec3 NECK = new Vec3(0.5, 0.78, 0.5);
    private static final double NECK_RADIUS = 2.2 * PX;
    /** Chain links: length and the two sides of their cross section in pixels, and the spacing along the chain in blocks. */
    private static final float LINK_LENGTH = 1.8F;
    private static final float LINK_WIDE = 0.9F;
    private static final float LINK_THIN = 0.5F;
    private static final double LINK_SPACING = 1.5 * PX;
    /** How far a chain sags in the middle per block of horizontal length. */
    private static final double SAG = 0.1;

    public VasRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(VasBlockEntity vas) {
        return new AABB(vas.getBlockPos()).inflate(3, 1, 3);
    }

    @Override
    protected void render(VasBlockEntity vas, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ElementalSight.active(minecraft.player)) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        float time = (minecraft.level.getGameTime() % 2400 + partialTick) / 2400F;
        boolean freed = vas.getBlockState().getValue(VasBlock.FREED);
        drawHomunculus(consumer, poseStack, freed, vas.shownGrowth(), time, light);
        if (vas.life().grown() && !freed) {
            TextureAtlasSprite steel = sprite(STEEL);
            for (Offset offset : vas.shownReach()) {
                drawChain(consumer, poseStack, new Vec3(offset.dx() + 0.5, 0.03, offset.dz() + 0.5), steel, light);
            }
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    /** The little pale figure: body, head with two dark eyes, and two arms; boxes in pixels around the body's centre. */
    private static void drawHomunculus(VertexConsumer consumer, PoseStack poseStack, boolean freed, float growth, float time, int light) {
        TextureAtlasSprite skin = sprite(SKIN);
        TextureAtlasSprite eyes = sprite(EYES);
        float scale = freed ? 1F : 0.35F + 0.65F * growth;
        float sway = (float) Math.sin(time * Math.PI * 2 * (freed ? 4 : 2)) * (freed ? 35F : 15F);
        poseStack.pushPose();
        poseStack.translate(0.5F, (freed ? RIM_Y : INSIDE_Y) * PX, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(sway));
        poseStack.scale(scale, scale, scale);
        pixelBox(consumer, poseStack, -2, -1.6F, -1.5F, 2, 1.6F, 1.5F, skin, SKIN_TINT, light);
        pixelBox(consumer, poseStack, -2, 1.65F, -1.4F, 2, 5.25F, 2.2F, skin, SKIN_TINT, light);
        pixelBox(consumer, poseStack, -3.25F, -1.5F, 0.3F, -2.05F, 0.9F, 1.5F, skin, SKIN_TINT, light);
        pixelBox(consumer, poseStack, 2.05F, -1.5F, 0.3F, 3.25F, 0.9F, 1.5F, skin, SKIN_TINT, light);
        pixelBox(consumer, poseStack, -1.3F, 3.9F, 2.2F, -0.7F, 4.5F, 2.3F, eyes, 0xFFFFFFFF, light);
        pixelBox(consumer, poseStack, 0.7F, 3.9F, 2.2F, 1.3F, 4.5F, 2.3F, eyes, 0xFFFFFFFF, light);
        poseStack.popPose();
    }

    /** A chain from the neck to {@code target}, sagging in the middle, of links that turn by a quarter each. */
    private static void drawChain(VertexConsumer consumer, PoseStack poseStack, Vec3 target, TextureAtlasSprite steel, int light) {
        Vec3 across = new Vec3(target.x - NECK.x, 0, target.z - NECK.z);
        double horizontal = across.length();
        if (horizontal < 1e-3) {
            return;
        }
        Vec3 start = NECK.add(across.scale(NECK_RADIUS / horizontal));
        double sag = SAG * horizontal;
        int links = Math.max(2, (int) Math.round(start.distanceTo(target) / LINK_SPACING));
        for (int i = 0; i < links; i++) {
            double t = (i + 0.5) / links;
            Vec3 at = pointOnChain(start, target, sag, t);
            Vec3 ahead = pointOnChain(start, target, sag, t + 0.01).subtract(at).normalize();
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(new Quaternionf().rotationTo(0, 0, 1, (float) ahead.x, (float) ahead.y, (float) ahead.z));
            float wide = i % 2 == 0 ? LINK_WIDE : LINK_THIN;
            float high = i % 2 == 0 ? LINK_THIN : LINK_WIDE;
            pixelBox(consumer, poseStack, -wide / 2, -high / 2, -LINK_LENGTH / 2, wide / 2, high / 2, LINK_LENGTH / 2,
                    steel, CHAIN_TINT, light);
            poseStack.popPose();
        }
    }

    /**
     * A box around the pose's origin, in pixels. The block box drawer takes its texture coordinates from the box's place in
     * the unit cube, so the box is drawn half a block over and the pose moved back.
     */
    private static void pixelBox(VertexConsumer consumer, PoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1,
                                 TextureAtlasSprite sprite, int color, int light) {
        poseStack.pushPose();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        CalcinerRenderer.drawBox(consumer, poseStack.last(), 0.5F + x0 * PX, 0.5F + y0 * PX, 0.5F + z0 * PX,
                0.5F + x1 * PX, 0.5F + y1 * PX, 0.5F + z1 * PX, sprite, color, light);
        poseStack.popPose();
    }

    private static Vec3 pointOnChain(Vec3 start, Vec3 end, double sag, double t) {
        return start.lerp(end, t).subtract(0, sag * Math.sin(Math.PI * Math.min(1, t)), 0);
    }
}
