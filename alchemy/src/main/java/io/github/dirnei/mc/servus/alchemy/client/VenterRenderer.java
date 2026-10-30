package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.alchemy.venter.VenterEquinusBlockEntity;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Draws the bedding of a formed Venter Equinus: straw that sinks as it rots, rotted litter once it is gone, and three dung mats
 * on top. The bedding spans the whole bed, so it is drawn one block cell at a time to keep each face's texture on its sprite.
 * Every box keeps at least 0.05 px from the planes of the frame and the Cucurbit.
 */
public final class VenterRenderer extends BufferedBlockEntityRenderer<VenterEquinusBlockEntity> {

    private static final float PX = 1 / 16F;
    /** The inside of the bed, in blocks from the Cucurbit's corner. */
    private static final float BED_MIN = -13.45F * PX;
    private static final float BED_MAX = 29.45F * PX;
    private static final float BED_FLOOR = 0.05F * PX;
    private static final float MAT_GAP = 0.05F * PX;
    private static final float MAT_THICKNESS = 0.5F * PX;
    /** The dung mats as x0, z0, x1, z1 in pixels from the Cucurbit's corner, each inside one cell of the bed. */
    private static final float[][] MATS = {{-11, -10, -5, -4}, {19, 20, 26, 25}, {20, -9, 27, -4}};

    private static final Identifier HAY_TOP = Identifier.withDefaultNamespace("block/hay_block_top");
    private static final Identifier HAY_SIDE = Identifier.withDefaultNamespace("block/hay_block_side");
    private static final Identifier LITTER = Identifier.withDefaultNamespace("block/mud");
    private static final Identifier MAT = Identifier.withDefaultNamespace("block/packed_mud");

    public VenterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(VenterEquinusBlockEntity venter) {
        return new AABB(venter.getBlockPos()).inflate(1, 0, 1).expandTowards(0, 1, 0);
    }

    @Override
    protected void render(VenterEquinusBlockEntity venter, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int packedLight) {
        if (!venter.formed()) {
            return;
        }
        boolean straw = venter.shownStraw() != VenterEquinusBlockEntity.NO_STRAW;
        TextureAtlasSprite top = sprite(straw ? HAY_TOP : LITTER);
        TextureAtlasSprite side = sprite(straw ? HAY_SIDE : LITTER);
        float height = (float) venter.beddingHeight() * PX;
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                poseStack.pushPose();
                poseStack.translate(cx, 0, cz);
                drawBedding(consumer, poseStack.last(), cx, cz, height, top, side, packedLight);
                poseStack.popPose();
            }
        }
        TextureAtlasSprite mat = sprite(MAT);
        for (float[] box : MATS) {
            int cx = Math.floorDiv((int) box[0], 16);
            int cz = Math.floorDiv((int) box[1], 16);
            poseStack.pushPose();
            poseStack.translate(cx, 0, cz);
            CalcinerRenderer.drawBox(consumer, poseStack.last(), box[0] * PX - cx, height + MAT_GAP, box[1] * PX - cz,
                    box[2] * PX - cx, height + MAT_GAP + MAT_THICKNESS, box[3] * PX - cz, mat, 0xFFFFFFFF, packedLight);
            poseStack.popPose();
        }
    }

    /** The part of the bedding in the cell {@code cx}, {@code cz}: its top, and its sides where they face the walls. */
    private static void drawBedding(VertexConsumer consumer, PoseStack.Pose pose, int cx, int cz, float height,
                                    TextureAtlasSprite top, TextureAtlasSprite side, int light) {
        float x0 = Math.max(BED_MIN, cx) - cx;
        float x1 = Math.min(BED_MAX, cx + 1) - cx;
        float z0 = Math.max(BED_MIN, cz) - cz;
        float z1 = Math.min(BED_MAX, cz + 1) - cz;
        CalcinerRenderer.drawFaces(consumer, pose, x0, BED_FLOOR, z0, x1, height, z1, top, 0xFFFFFFFF, light, EnumSet.of(Direction.UP));
        Set<Direction> sides = EnumSet.noneOf(Direction.class);
        if (cx == -1) {
            sides.add(Direction.WEST);
        }
        if (cx == 1) {
            sides.add(Direction.EAST);
        }
        if (cz == -1) {
            sides.add(Direction.NORTH);
        }
        if (cz == 1) {
            sides.add(Direction.SOUTH);
        }
        CalcinerRenderer.drawFaces(consumer, pose, x0, BED_FLOOR, z0, x1, height, z1, side, 0xFFFFFFFF, light, sides);
    }

    private static TextureAtlasSprite sprite(Identifier texture) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(texture);
    }
}
