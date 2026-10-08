package io.github.dirnei.mc.servus.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlock;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailCenter;
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
import org.joml.Quaternionf;

/**
 * Draws the moving parts of a whole shaft from its lowest segment: the deck
 * of every rail floor (two flaps that fold up for the car) and the lift car,
 * a platform with a sleeve around each post. Posts and head are block model.
 * All sizes below are in pixels, 16 to a block.
 */
public class ElevatorRenderer implements BlockEntityRenderer<ElevatorBlockEntity> {

    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "block/elevator");
    /** Rail textures in the order of {@link RailCenter.Shape}, the same as the outside rail's models. */
    private static final ResourceLocation[] RAIL = {
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "block/rail"),
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "block/rail_curve"),
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "block/rail_cross")};
    /** Rail pieces as x0, z0, x1, z1: the centre plate and the arm towards north, south, west, east. */
    private static final float[] CENTER = {5, 5, 11, 11};
    private static final float[][] ARMS = {{5, 0, 11, 5}, {5, 11, 11, 16}, {0, 5, 5, 11}, {11, 5, 16, 11}};
    /** The shaft area between the posts that the car and the decks cover. */
    private static final float[] INNER = {2, 2, 14, 14};

    /** One rail piece: its rectangle, the texture of its shape and that texture's clockwise rotation in degrees. */
    private record Piece(float[] area, RailCenter.Shape shape, int rotation) {

        Piece within(float[] part) {
            return new Piece(part, shape, rotation);
        }
    }

    public ElevatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(ElevatorBlockEntity segment) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ElevatorBlockEntity segment) {
        BlockPos pos = segment.getBlockPos();
        int height = segment.getLevel() == null ? 1 : Math.max(1, column(segment.getLevel(), pos).size());
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
        if (floors.isEmpty()) {
            // The segment was just broken and the world already holds something else.
            return;
        }
        double carY = carY(segment, level.getGameTime() + partialTick) - bottom.getY();
        carY = Math.max(0, Math.min(floors.size() - 1, carY));
        TextureAtlasSprite frame = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(FRAME);
        TextureAtlasSprite[] rail = new TextureAtlasSprite[RAIL.length];
        for (int i = 0; i < RAIL.length; i++) {
            rail[i] = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(RAIL[i]);
        }
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));

        for (int floor = 0; floor < floors.size(); floor++) {
            BlockState state = floors.get(floor);
            if (!hasRail(state)) {
                continue;
            }
            int light = LevelRenderer.getLightColor(level, bottom.above(floor));
            poseStack.pushPose();
            poseStack.translate(0, floor, 0);
            float angle = (float) Flaps.angle(carY, floor);
            if (floor == 0) {
                // The floor below is the ground level's deck; the parked car brings its own rail between the posts.
                groundRail(poseStack, buffer, rail, state, angle < 90F, light, packedOverlay);
            } else {
                deck(poseStack, buffer, frame, rail, state, angle, light, packedOverlay);
            }
            poseStack.popPose();
        }

        int nearest = (int) Math.round(carY);
        int light = LevelRenderer.getLightColor(level, bottom.above(nearest));
        poseStack.pushPose();
        poseStack.translate(0, carY, 0);
        car(poseStack, buffer, frame, rail, floors.get(nearest), light, packedOverlay);
        poseStack.popPose();
    }

    /** The segments of the shaft from this one up, as their block states; empty once this segment is gone. */
    private static List<BlockState> column(Level level, BlockPos bottom) {
        List<BlockState> floors = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = bottom.mutable();
        BlockState state = level.getBlockState(cursor);
        while (state.getBlock() instanceof ElevatorBlock) {
            floors.add(state);
            cursor.move(0, 1, 0);
            state = level.getBlockState(cursor);
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

    /** The rail pieces of a floor: the centre shaped like an outside rail's and a straight arm per connected side. */
    private static List<Piece> railPieces(BlockState state) {
        List<Piece> pieces = new ArrayList<>();
        boolean[] arms = arms(state);
        RailCenter center = RailCenter.of(arms[0], arms[3], arms[1], arms[2]);
        pieces.add(new Piece(CENTER, center.shape(), center.rotation()));
        for (int i = 0; i < ARMS.length; i++) {
            if (arms[i]) {
                pieces.add(new Piece(ARMS[i], RailCenter.Shape.STRAIGHT, i < 2 ? 0 : 90));
            }
        }
        return pieces;
    }

    /** Shaft edges, in the order of {@link #arms}. */
    private enum Edge { NORTH, SOUTH, WEST, EAST }

    /**
     * The floor of a rail level. The plate lies just below the segment and
     * the rail on it at the height of the outside rail. It folds away only on
     * edges without a rail, so a folded part never stands in a shuttle's way:
     * two flaps on free opposite edges, else one flap on a free edge, else four
     * quarters that pull back into the corner posts.
     */
    private static void deck(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite frame, TextureAtlasSprite[] rail,
                             BlockState state, float angle, int light, int overlay) {
        boolean[] arms = arms(state);
        List<Piece> pieces = railPieces(state);
        boolean northSouthFree = !arms[0] && !arms[1];
        boolean westEastFree = !arms[2] && !arms[3];
        if (northSouthFree) {
            flap(poseStack, buffer, frame, rail, pieces, new float[] {2, 2, 14, 8}, Edge.NORTH, angle, light, overlay);
            flap(poseStack, buffer, frame, rail, pieces, new float[] {2, 8, 14, 14}, Edge.SOUTH, angle, light, overlay);
        } else if (westEastFree) {
            flap(poseStack, buffer, frame, rail, pieces, new float[] {2, 2, 8, 14}, Edge.WEST, angle, light, overlay);
            flap(poseStack, buffer, frame, rail, pieces, new float[] {8, 2, 14, 14}, Edge.EAST, angle, light, overlay);
        } else {
            Edge free = null;
            for (Edge edge : Edge.values()) {
                if (!arms[edge.ordinal()]) {
                    free = edge;
                    break;
                }
            }
            if (free != null) {
                flap(poseStack, buffer, frame, rail, pieces, new float[] {2, 2, 14, 14}, free, angle, light, overlay);
            } else {
                quarters(poseStack, buffer, frame, rail, pieces, angle, light, overlay);
            }
        }
        // Rail stubs over the shaft edge, outside the deck, stay put.
        stubs(poseStack, buffer, rail, pieces, light, overlay);
    }

    /** The parts of the rail pieces outside the posts, which neither deck nor car covers. */
    private static void stubs(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite[] rail, List<Piece> pieces,
                              int light, int overlay) {
        for (Piece piece : pieces) {
            for (float[] stub : outside(piece.area(), INNER)) {
                railBox(poseStack, buffer, rail, piece.within(stub), light, overlay);
            }
        }
    }

    /**
     * One flap over the area, with an axle bar between the posts on its hinge
     * edge. It pivots on the plate's lower outer edge, so folded up it stands
     * at the shaft edge outside the car's footprint.
     */
    private static void flap(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite frame, TextureAtlasSprite[] rail,
                             List<Piece> pieces, float[] area, Edge hinge, float angle, int light, int overlay) {
        switch (hinge) {
            case NORTH -> box(poseStack, buffer, frame, 2, -1, 0.5F, 14, 0, 1.5F, light, overlay);
            case SOUTH -> box(poseStack, buffer, frame, 2, -1, 14.5F, 14, 0, 15.5F, light, overlay);
            case WEST -> box(poseStack, buffer, frame, 0.5F, -1, 2, 1.5F, 0, 14, light, overlay);
            case EAST -> box(poseStack, buffer, frame, 14.5F, -1, 2, 15.5F, 0, 14, light, overlay);
        }
        poseStack.pushPose();
        float pivotY = -1 / 16F;
        switch (hinge) {
            case NORTH -> rotate(poseStack, 0, pivotY, area[1] / 16F, Axis.XP.rotationDegrees(-angle));
            case SOUTH -> rotate(poseStack, 0, pivotY, area[3] / 16F, Axis.XP.rotationDegrees(angle));
            case WEST -> rotate(poseStack, area[0] / 16F, pivotY, 0, Axis.ZP.rotationDegrees(angle));
            case EAST -> rotate(poseStack, area[2] / 16F, pivotY, 0, Axis.ZP.rotationDegrees(-angle));
        }
        plate(poseStack, buffer, frame, rail, pieces, area, light, overlay);
        poseStack.popPose();
    }

    private static void rotate(PoseStack poseStack, float x, float y, float z, Quaternionf rotation) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(rotation);
        poseStack.translate(-x, -y, -z);
    }

    /** Four quarter plates that shrink towards their corner posts as the angle goes from 0 to 90. */
    private static void quarters(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite frame, TextureAtlasSprite[] rail,
                                 List<Piece> pieces, float angle, int light, int overlay) {
        float scale = 1F - angle / 90F;
        if (scale <= 0.01F) {
            return;
        }
        float[][] areas = {{2, 2, 8, 8}, {8, 2, 14, 8}, {2, 8, 8, 14}, {8, 8, 14, 14}};
        float[][] corners = {{2, 2}, {14, 2}, {2, 14}, {14, 14}};
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            float cx = corners[i][0] / 16F;
            float cz = corners[i][1] / 16F;
            poseStack.translate(cx, 0, cz);
            poseStack.scale(scale, 1, scale);
            poseStack.translate(-cx, 0, -cz);
            plate(poseStack, buffer, frame, rail, pieces, areas[i], light, overlay);
            poseStack.popPose();
        }
    }

    /** The lowest segment's rail, lying on the floor block like an outside rail; between the posts only while the car is away. */
    private static void groundRail(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite[] rail, BlockState state,
                                   boolean carAway, int light, int overlay) {
        List<Piece> pieces = railPieces(state);
        stubs(poseStack, buffer, rail, pieces, light, overlay);
        if (carAway) {
            clipped(poseStack, buffer, rail, pieces, INNER, light, overlay);
        }
    }

    /** The parts of the rail pieces inside the area. */
    private static void clipped(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite[] rail, List<Piece> pieces,
                                float[] area, int light, int overlay) {
        for (Piece piece : pieces) {
            float[] part = clip(piece.area(), area);
            if (part != null) {
                railBox(poseStack, buffer, rail, piece.within(part), light, overlay);
            }
        }
    }

    /** A deck plate below the segment with the rail pieces over it at outside rail height. */
    private static void plate(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite frame, TextureAtlasSprite[] rail,
                              List<Piece> pieces, float[] area, int light, int overlay) {
        box(poseStack, buffer, frame, area[0], -1, area[1], area[2], 0, area[3], light, overlay);
        clipped(poseStack, buffer, rail, pieces, area, light, overlay);
    }

    /** The car: a platform between the posts with a sleeve around each post, carrying the nearest floor's rail at outside rail height. */
    private static void car(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite frame, TextureAtlasSprite[] rail,
                            BlockState floor, int light, int overlay) {
        box(poseStack, buffer, frame, 2, -2, 2, 14, -0.1F, 14, light, overlay);
        float[][] sleeves = {{-0.25F, -0.25F}, {13, -0.25F}, {-0.25F, 13}, {13, 13}};
        for (float[] sleeve : sleeves) {
            box(poseStack, buffer, frame, sleeve[0], -3, sleeve[1], sleeve[0] + 3.25F, 2, sleeve[1] + 3.25F, light, overlay);
        }
        clipped(poseStack, buffer, rail, railPieces(floor), INNER, light, overlay);
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

    /**
     * A rail piece one pixel high. The top shows its shape's texture mapped by
     * position and turned with the piece like a rotated block model, so pieces
     * cut at the deck and car edges still line up with each other.
     */
    private static void railBox(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite[] rail, Piece piece,
                                int light, int overlay) {
        TextureAtlasSprite sprite = rail[piece.shape().ordinal()];
        float[] a = piece.area();
        sidesAndBottom(poseStack, buffer, sprite, a[0], 0, a[1], a[2], 1, a[3], light, overlay);
        PoseStack.Pose pose = poseStack.last();
        float[][] corners = {{a[0], a[1]}, {a[0], a[3]}, {a[2], a[3]}, {a[2], a[1]}};
        for (float[] corner : corners) {
            float[] uv = turnedUv(piece.rotation(), corner[0], corner[1]);
            vertex(buffer, pose, corner[0], 1, corner[1], sprite.getU(uv[0] / 16F), sprite.getV(uv[1] / 16F),
                    light, overlay, 0, 1, 0);
        }
    }

    /** The texture pixel shown at x, z under a model turned clockwise by the rotation. */
    private static float[] turnedUv(int rotation, float x, float z) {
        return switch (rotation) {
            case 90 -> new float[] {z, 16 - x};
            case 180 -> new float[] {16 - x, 16 - z};
            case 270 -> new float[] {16 - z, x};
            default -> new float[] {x, z};
        };
    }

    /** A textured box in pixel coordinates; the texture is mapped by position like a block model's default UVs. */
    private static void box(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite,
                            float x0, float y0, float z0, float x1, float y1, float z1, int light, int overlay) {
        sidesAndBottom(poseStack, buffer, sprite, x0, y0, z0, x1, y1, z1, light, overlay);
        face(buffer, poseStack.last(), sprite, light, overlay, 0, 1, 0,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, z0, x1, z1);
    }

    private static void sidesAndBottom(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite,
                                       float x0, float y0, float z0, float x1, float y1, float z1, int light, int overlay) {
        PoseStack.Pose pose = poseStack.last();
        // Side texture rows repeat every block, so parts below y 0 still map onto the sprite.
        float shift = (float) Math.floor(y0 / 16F) * 16F;
        float v0 = 16 - (y1 - shift);
        float v1 = 16 - (y0 - shift);
        // Down
        face(buffer, pose, sprite, light, overlay, 0, -1, 0,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, z0, x1, z1);
        // North and south
        face(buffer, pose, sprite, light, overlay, 0, 0, -1,
                x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, v0, x1, v1);
        face(buffer, pose, sprite, light, overlay, 0, 0, 1,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, v0, x1, v1);
        // West and east
        face(buffer, pose, sprite, light, overlay, -1, 0, 0,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z0, v0, z1, v1);
        face(buffer, pose, sprite, light, overlay, 1, 0, 0,
                x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, z0, v0, z1, v1);
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
