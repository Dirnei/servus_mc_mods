package io.github.dirnei.mc.servus.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlock;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.Flaps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the moving parts of a whole shaft from its lowest segment: the deck
 * of every rail floor (two flaps that fold up for the car) and the lift car,
 * a platform with a sleeve around each post. Posts and head are block model.
 * All sizes below are in pixels, 16 to a block.
 */
public class ElevatorRenderer implements BlockEntityRenderer<ElevatorBlockEntity> {

    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "block/elevator");
    private static final ResourceLocation RAIL = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "block/rail");
    /** Rail pieces as x0, z0, x1, z1: the centre plate and the arm towards north, south, west, east. */
    private static final float[] CENTER = {5, 5, 11, 11};
    private static final float[][] ARMS = {{5, 0, 11, 5}, {5, 11, 11, 16}, {0, 5, 5, 11}, {11, 5, 16, 11}};

    public ElevatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(ElevatorBlockEntity segment) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ElevatorBlockEntity segment) {
        BlockPos pos = segment.getBlockPos();
        int height = segment.getLevel() == null ? 1 : column(segment.getLevel(), pos).size();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
    }

    @Override
    public void render(ElevatorBlockEntity segment, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        Level level = segment.getLevel();
        BlockState own = segment.getBlockState();
        if (level == null || !(own.getBlock() instanceof ElevatorBlock) || own.getValue(ConnectableBlock.DOWN)) {
            return;
        }
        BlockPos bottom = segment.getBlockPos();
        List<BlockState> floors = column(level, bottom);
        double carY = carY(segment, level.getGameTime() + partialTick) - bottom.getY();
        carY = Math.max(0, Math.min(floors.size() - 1, carY));
        TextureAtlasSprite frame = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(FRAME);
        TextureAtlasSprite rail = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(RAIL);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));

        for (int floor = 0; floor < floors.size(); floor++) {
            BlockState state = floors.get(floor);
            if (!hasRail(state)) {
                continue;
            }
            int light = LevelRenderer.getLightColor(level, bottom.above(floor));
            poseStack.pushPose();
            poseStack.translate(0, floor, 0);
            deck(poseStack, buffer, frame, rail, state, (float) Flaps.angle(carY, floor), light, packedOverlay);
            poseStack.popPose();
        }

        int nearest = (int) Math.round(carY);
        int light = LevelRenderer.getLightColor(level, bottom.above(nearest));
        poseStack.pushPose();
        poseStack.translate(0, carY, 0);
        car(poseStack, buffer, frame, rail, floors.get(nearest), light, packedOverlay);
        poseStack.popPose();
    }

    /** The segments of the shaft from this one up, as their block states. */
    private static List<BlockState> column(Level level, BlockPos bottom) {
        List<BlockState> floors = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = bottom.mutable();
        BlockState state = level.getBlockState(cursor);
        while (state.getBlock() instanceof ElevatorBlock) {
            floors.add(state);
            cursor.move(0, 1, 0);
            state = level.getBlockState(cursor);
        }
        if (floors.isEmpty()) {
            floors.add(level.getBlockState(bottom));
        }
        return floors;
    }

    /** The car's absolute y: from the last sync, else where it was parked when the chunk loaded, else the bottom. */
    private static double carY(ElevatorBlockEntity segment, double time) {
        ClientLifts.Lift lift = ClientLifts.lift(segment.getBlockPos());
        if (lift != null) {
            return lift.yAt(time);
        }
        return Float.isNaN(segment.liftY()) ? segment.getBlockPos().getY() : segment.liftY();
    }

    private static boolean hasRail(BlockState state) {
        return state.getValue(ConnectableBlock.NORTH) || state.getValue(ConnectableBlock.SOUTH)
                || state.getValue(ConnectableBlock.WEST) || state.getValue(ConnectableBlock.EAST);
    }

    private static boolean[] arms(BlockState state) {
        return new boolean[] {state.getValue(ConnectableBlock.NORTH), state.getValue(ConnectableBlock.SOUTH),
                state.getValue(ConnectableBlock.WEST), state.getValue(ConnectableBlock.EAST)};
    }

    /** The rail pieces of a floor, as x0, z0, x1, z1 rectangles. */
    private static List<float[]> railPieces(BlockState state) {
        List<float[]> pieces = new ArrayList<>();
        pieces.add(CENTER);
        boolean[] arms = arms(state);
        for (int i = 0; i < ARMS.length; i++) {
            if (arms[i]) {
                pieces.add(ARMS[i]);
            }
        }
        return pieces;
    }

    /**
     * Two flaps hinged on axle bars between the posts. Along the west and
     * east edges when a rail lies west or east, along north and south otherwise.
     */
    private static void deck(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite frame, TextureAtlasSprite rail,
                             BlockState state, float angle, int light, int overlay) {
        boolean westEast = state.getValue(ConnectableBlock.WEST) || state.getValue(ConnectableBlock.EAST);
        List<float[]> pieces = railPieces(state);
        // Flap areas as x0, z0, x1, z1 with the hinge on the first or the second edge.
        float[][] flaps = westEast
                ? new float[][] {{2, 2, 8, 14}, {8, 2, 14, 14}}
                : new float[][] {{2, 2, 14, 8}, {2, 8, 14, 14}};
        if (westEast) {
            box(poseStack, buffer, frame, 0.5F, 0, 2, 1.5F, 1, 14, light, overlay);
            box(poseStack, buffer, frame, 14.5F, 0, 2, 15.5F, 1, 14, light, overlay);
        } else {
            box(poseStack, buffer, frame, 2, 0, 0.5F, 14, 1, 1.5F, light, overlay);
            box(poseStack, buffer, frame, 2, 0, 14.5F, 14, 1, 15.5F, light, overlay);
        }
        for (int i = 0; i < 2; i++) {
            float[] area = flaps[i];
            poseStack.pushPose();
            if (westEast) {
                float hinge = i == 0 ? 2 : 14;
                poseStack.translate(hinge / 16F, 1 / 16F, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(i == 0 ? angle : -angle));
                poseStack.translate(-hinge / 16F, -1 / 16F, 0);
            } else {
                float hinge = i == 0 ? 2 : 14;
                poseStack.translate(0, 1 / 16F, hinge / 16F);
                poseStack.mulPose(Axis.XP.rotationDegrees(i == 0 ? -angle : angle));
                poseStack.translate(0, -1 / 16F, -hinge / 16F);
            }
            box(poseStack, buffer, frame, area[0], 0, area[1], area[2], 1, area[3], light, overlay);
            for (float[] piece : pieces) {
                float[] part = clip(piece, area);
                if (part != null) {
                    box(poseStack, buffer, rail, part[0], 1, part[1], part[2], 2, part[3], light, overlay);
                }
            }
            poseStack.popPose();
        }
        // Rail stubs over the shaft edge, outside both flaps, stay put.
        float[] inner = {2, 2, 14, 14};
        for (float[] piece : pieces) {
            for (float[] stub : outside(piece, inner)) {
                box(poseStack, buffer, rail, stub[0], 1, stub[1], stub[2], 2, stub[3], light, overlay);
            }
        }
    }

    /** The car: a platform between the posts at rail height with a sleeve around each post, carrying the nearest floor's rail. */
    private static void car(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite frame, TextureAtlasSprite rail,
                            BlockState floor, int light, int overlay) {
        box(poseStack, buffer, frame, 2, -1, 2, 14, 1, 14, light, overlay);
        float[][] sleeves = {{-0.25F, -0.25F}, {13, -0.25F}, {-0.25F, 13}, {13, 13}};
        for (float[] sleeve : sleeves) {
            box(poseStack, buffer, frame, sleeve[0], -2, sleeve[1], sleeve[0] + 3.25F, 3, sleeve[1] + 3.25F, light, overlay);
        }
        for (float[] piece : railPieces(floor)) {
            float[] part = clip(piece, new float[] {2, 2, 14, 14});
            if (part != null) {
                box(poseStack, buffer, rail, part[0], 1, part[1], part[2], 2, part[3], light, overlay);
            }
        }
    }

    /** The part of rectangle a inside rectangle b, or null. */
    private static float[] clip(float[] a, float[] b) {
        float x0 = Math.max(a[0], b[0]);
        float z0 = Math.max(a[1], b[1]);
        float x1 = Math.min(a[2], b[2]);
        float z1 = Math.min(a[3], b[3]);
        return x0 < x1 && z0 < z1 ? new float[] {x0, z0, x1, z1} : null;
    }

    /** The parts of rectangle a outside rectangle b, as up to four rectangles. */
    private static List<float[]> outside(float[] a, float[] b) {
        List<float[]> parts = new ArrayList<>();
        addIfAny(parts, a[0], a[1], Math.min(a[2], b[0]), a[3]);
        addIfAny(parts, Math.max(a[0], b[2]), a[1], a[2], a[3]);
        float x0 = Math.max(a[0], b[0]);
        float x1 = Math.min(a[2], b[2]);
        addIfAny(parts, x0, a[1], x1, Math.min(a[3], b[1]));
        addIfAny(parts, x0, Math.max(a[1], b[3]), x1, a[3]);
        return parts;
    }

    private static void addIfAny(List<float[]> parts, float x0, float z0, float x1, float z1) {
        if (x0 < x1 && z0 < z1) {
            parts.add(new float[] {x0, z0, x1, z1});
        }
    }

    /** A textured box in pixel coordinates; the texture is mapped by position like a block model's default UVs. */
    private static void box(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite,
                            float x0, float y0, float z0, float x1, float y1, float z1, int light, int overlay) {
        PoseStack.Pose pose = poseStack.last();
        // Down and up
        face(buffer, pose, sprite, light, overlay, 0, -1, 0,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, z0, x1, z1);
        face(buffer, pose, sprite, light, overlay, 0, 1, 0,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, z0, x1, z1);
        // North and south
        face(buffer, pose, sprite, light, overlay, 0, 0, -1,
                x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, 16 - y1, x1, 16 - y0);
        face(buffer, pose, sprite, light, overlay, 0, 0, 1,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, 16 - y1, x1, 16 - y0);
        // West and east
        face(buffer, pose, sprite, light, overlay, -1, 0, 0,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z0, 16 - y1, z1, 16 - y0);
        face(buffer, pose, sprite, light, overlay, 1, 0, 0,
                x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, z0, 16 - y1, z1, 16 - y0);
    }

    /** One quad; u0..v1 are pixel texture coordinates, clamped to the sprite. */
    private static void face(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, int light, int overlay,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        float su0 = sprite.getU(Math.clamp(u0 / 16F, 0F, 1F));
        float su1 = sprite.getU(Math.clamp(u1 / 16F, 0F, 1F));
        float sv0 = sprite.getV(Math.clamp(v0 / 16F, 0F, 1F));
        float sv1 = sprite.getV(Math.clamp(v1 / 16F, 0F, 1F));
        vertex(buffer, pose, ax, ay, az, su0, sv1, light, overlay, nx, ny, nz);
        vertex(buffer, pose, bx, by, bz, su0, sv0, light, overlay, nx, ny, nz);
        vertex(buffer, pose, cx, cy, cz, su1, sv0, light, overlay, nx, ny, nz);
        vertex(buffer, pose, dx, dy, dz, su1, sv1, light, overlay, nx, ny, nz);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int light, int overlay, float nx, float ny, float nz) {
        buffer.addVertex(pose, x / 16F, y / 16F, z / 16F).setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
