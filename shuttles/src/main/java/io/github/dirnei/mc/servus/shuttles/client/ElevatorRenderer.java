package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.ModelPoses;
import io.github.dirnei.mc.servus.foundation.client.RecordedGeometry;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlock;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.RailLook;
import io.github.dirnei.mc.servus.shuttles.shuttle.Flaps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import io.github.dirnei.mc.servus.foundation.material.client.MaterialTextures;
import io.github.dirnei.mc.servus.foundation.material.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the moving parts of a whole shaft from its lowest segment: the deck
 * of every rail floor (two flaps that fold up for the car) and the lift car,
 * a platform with a sleeve around each post. Posts and head are block model.
 * All sizes below are in pixels, 16 to a block.
 */
public class ElevatorRenderer extends LiveBlockEntityRenderer<ElevatorBlockEntity> {

    private static final Identifier FRAME = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/elevator");
    /** Rail textures: the Warehouse Rail's steel heads and oak sleepers, which each segment draws in its own wood. */
    private static final Identifier[] RAIL = {
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/rail_head"),
            Identifier.withDefaultNamespace("block/oak_planks")};
    /** The shaft area between the posts that the car and the decks cover. */
    private static final float[] INNER = {2, 2, 14, 14};

    /** One box of the rail track in the segment, and the clockwise rotation of the piece it belongs to. */
    private record Piece(RailLook.Box box, int rotation) {

        /** The box seen from above as x0, z0, x1, z1. */
        float[] area() {
            return new float[] {(float) box.x0(), (float) box.z0(), (float) box.x1(), (float) box.z1()};
        }

        Piece within(float[] part) {
            return new Piece(new RailLook.Box(box.part(), part[0], box.y0(), part[1], part[2], box.y1(), part[3]), rotation);
        }
    }

    /** Shaft edges, in the order of {@link #arms}. */
    private enum Edge { NORTH, SOUTH, WEST, EAST }

    /** Where and with what one part of the shaft is drawn: pose, buffer, the frame and rail sprites, light and overlay. */
    private record Brush(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite frame, TextureAtlasSprite[] rail, int light,
                         int overlay) {

        Brush lit(int newLight) {
            return new Brush(poseStack, buffer, frame, rail, newLight, overlay);
        }

        /** The same brush with the sleepers in that wood. */
        Brush wood(Identifier wood) {
            TextureAtlasSprite[] sprites = rail.clone();
            sprites[1] = MaterialTextures.spriteOf(wood);
            return new Brush(poseStack, buffer, frame, sprites, light, overlay);
        }
    }

    public ElevatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ElevatorBlockEntity segment) {
        BlockPos pos = segment.getBlockPos();
        int height = segment.getLevel() == null ? 1 : Math.max(1, column(segment.getLevel(), pos).size());
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
    }

    /** Only the lowest segment draws; an empty column means it was just broken. */
    @Override
    protected void render(ElevatorBlockEntity segment, float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                          int packedLight) {
        Level level = segment.getLevel();
        BlockState own = segment.getBlockState();
        if (level == null || !(own.getBlock() instanceof ElevatorBlock) || own.getValue(ConnectableBlock.DOWN)) {
            return;
        }
        BlockPos bottom = segment.getBlockPos();
        List<BlockState> floors = column(level, bottom);
        if (floors.isEmpty()) {
            return;
        }
        double carY = carY(segment, level.getGameTime() + partialTick) - bottom.getY();
        carY = Math.max(0, Math.min(floors.size() - 1, carY));
        TextureAtlas blocks = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        TextureAtlasSprite[] rail = new TextureAtlasSprite[RAIL.length];
        for (int i = 0; i < RAIL.length; i++) {
            rail[i] = blocks.getSprite(RAIL[i]);
        }
        RecordedGeometry buffer = new RecordedGeometry();
        Brush brush = new Brush(poseStack, buffer, blocks.getSprite(FRAME), rail, 0, OverlayTexture.NO_OVERLAY);

        for (int floor = 0; floor < floors.size(); floor++) {
            BlockState state = floors.get(floor);
            if (!hasRail(state)) {
                continue;
            }
            Brush lit = brush.lit(LevelRenderer.getLightCoords(level, bottom.above(floor))).wood(woodAt(level, bottom.above(floor)));
            float angle = (float) Flaps.angle(carY, floor);
            boolean ground = floor == 0;
            ModelPoses.translated(poseStack, 0, floor, 0, () -> {
                if (ground) {
                    groundRail(lit, state, angle < 90F);
                } else {
                    deck(lit, state, angle);
                }
            });
        }

        int nearest = (int) Math.round(carY);
        Brush lit = brush.lit(LevelRenderer.getLightCoords(level, bottom.above(nearest))).wood(woodAt(level, bottom.above(nearest)));
        ModelPoses.translated(poseStack, 0, (float) carY, 0, () -> car(lit, floors.get(nearest)));
        buffer.submit(collector, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS));
    }

    /** The wood of the segment at that position, oak when it has none. */
    private static Identifier woodAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ElevatorBlockEntity segment ? segment.materialSlot().material() : MaterialKind.WOOD.defaultBlock();
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

    /**
     * The track of a floor, the same piece an outside rail with these sides
     * shows. Sleepers stay between the posts, so no sleeper tip is left
     * behind when a flap folds.
     */
    private static List<Piece> railPieces(BlockState state) {
        List<Piece> pieces = new ArrayList<>();
        boolean[] arms = arms(state);
        RailLook.Placed placed = RailLook.of(arms[0], arms[3], arms[1], arms[2]);
        for (RailLook.Box box : RailLook.boxes(placed)) {
            Piece piece = new Piece(box, placed.rotation());
            if (box.part() == RailLook.Part.SLEEPER) {
                float[] inside = clip(piece.area(), INNER);
                if (inside == null) {
                    continue;
                }
                piece = piece.within(inside);
            }
            pieces.add(piece);
        }
        return pieces;
    }

    /**
     * The floor of a rail level. The plate lies just below the segment and
     * the rail on it at the height of the outside rail. It folds away only on
     * edges without a rail, so a folded part never stands in a shuttle's way:
     * two flaps on free opposite edges, else one flap on a free edge, else four
     * quarters that pull back into the corner posts. Rail stubs over the shaft
     * edge stay put.
     */
    private static void deck(Brush brush, BlockState state, float angle) {
        boolean[] arms = arms(state);
        List<Piece> pieces = railPieces(state);
        if (!arms[0] && !arms[1]) {
            flap(brush, pieces, new float[] {2, 2, 14, 8}, Edge.NORTH, angle);
            flap(brush, pieces, new float[] {2, 8, 14, 14}, Edge.SOUTH, angle);
        } else if (!arms[2] && !arms[3]) {
            flap(brush, pieces, new float[] {2, 2, 8, 14}, Edge.WEST, angle);
            flap(brush, pieces, new float[] {8, 2, 14, 14}, Edge.EAST, angle);
        } else {
            Edge free = null;
            for (Edge edge : Edge.values()) {
                if (!arms[edge.ordinal()]) {
                    free = edge;
                    break;
                }
            }
            if (free != null) {
                flap(brush, pieces, new float[] {2, 2, 14, 14}, free, angle);
            } else {
                quarters(brush, pieces, angle);
            }
        }
        stubs(brush, pieces);
    }

    /** The parts of the rail pieces outside the posts, which neither deck nor car covers. */
    private static void stubs(Brush brush, List<Piece> pieces) {
        for (Piece piece : pieces) {
            for (float[] stub : outside(piece.area(), INNER)) {
                railBox(brush, piece.within(stub));
            }
        }
    }

    /**
     * One flap over the area, with an axle bar between the posts on its hinge
     * edge. It pivots on the plate's lower outer edge, so folded up it stands
     * at the shaft edge outside the car's footprint.
     */
    private static void flap(Brush brush, List<Piece> pieces, float[] area, Edge hinge, float angle) {
        switch (hinge) {
            case NORTH -> box(brush, brush.frame(), 2, -1, 0.5F, 14, 0, 1.5F);
            case SOUTH -> box(brush, brush.frame(), 2, -1, 14.5F, 14, 0, 15.5F);
            case WEST -> box(brush, brush.frame(), 0.5F, -1, 2, 1.5F, 0, 14);
            case EAST -> box(brush, brush.frame(), 14.5F, -1, 2, 15.5F, 0, 14);
        }
        Runnable plate = () -> plate(brush, pieces, area);
        PoseStack poseStack = brush.poseStack();
        switch (hinge) {
            case NORTH -> ModelPoses.rotateAbout(poseStack, 0, -1, area[1], Axis.XP, -angle, plate);
            case SOUTH -> ModelPoses.rotateAbout(poseStack, 0, -1, area[3], Axis.XP, angle, plate);
            case WEST -> ModelPoses.rotateAbout(poseStack, area[0], -1, 0, Axis.ZP, angle, plate);
            case EAST -> ModelPoses.rotateAbout(poseStack, area[2], -1, 0, Axis.ZP, -angle, plate);
        }
    }

    /** Four quarter plates that shrink towards their corner posts as the angle goes from 0 to 90. */
    private static void quarters(Brush brush, List<Piece> pieces, float angle) {
        float scale = 1F - angle / 90F;
        if (scale <= 0.01F) {
            return;
        }
        float[][] areas = {{2, 2, 8, 8}, {8, 2, 14, 8}, {2, 8, 8, 14}, {8, 8, 14, 14}};
        float[][] corners = {{2, 2}, {14, 2}, {2, 14}, {14, 14}};
        PoseStack poseStack = brush.poseStack();
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            float cx = corners[i][0] / 16F;
            float cz = corners[i][1] / 16F;
            poseStack.translate(cx, 0, cz);
            poseStack.scale(scale, 1, scale);
            poseStack.translate(-cx, 0, -cz);
            plate(brush, pieces, areas[i]);
            poseStack.popPose();
        }
    }

    /** The lowest segment's rail, lying on the floor block like an outside rail; between the posts only while the car is away. */
    private static void groundRail(Brush brush, BlockState state, boolean carAway) {
        List<Piece> pieces = railPieces(state);
        stubs(brush, pieces);
        if (carAway) {
            clipped(brush, pieces, INNER);
        }
    }

    /** The parts of the rail pieces inside the area. */
    private static void clipped(Brush brush, List<Piece> pieces, float[] area) {
        for (Piece piece : pieces) {
            float[] part = clip(piece.area(), area);
            if (part != null) {
                railBox(brush, piece.within(part));
            }
        }
    }

    /** A deck plate below the segment with the rail pieces over it at outside rail height. */
    private static void plate(Brush brush, List<Piece> pieces, float[] area) {
        box(brush, brush.frame(), area[0], -1, area[1], area[2], 0, area[3]);
        clipped(brush, pieces, area);
    }

    /** The car: a platform between the posts with a sleeve around each post, carrying the nearest floor's rail at outside rail height. */
    private static void car(Brush brush, BlockState floor) {
        box(brush, brush.frame(), 2, -2, 2, 14, -0.1F, 14);
        float[][] sleeves = {{-0.25F, -0.25F}, {13, -0.25F}, {-0.25F, 13}, {13, 13}};
        for (float[] sleeve : sleeves) {
            box(brush, brush.frame(), sleeve[0], -3, sleeve[1], sleeve[0] + 3.25F, 2, sleeve[1] + 3.25F);
        }
        clipped(brush, railPieces(floor), INNER);
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
     * One box of the track: a head in steel or a sleeper in the segment's wood. The top
     * shows its texture mapped by position and turned with the piece like a
     * rotated block model, so boxes cut at the deck and car edges still line
     * up with each other.
     */
    private static void railBox(Brush brush, Piece piece) {
        TextureAtlasSprite sprite = brush.rail()[piece.box().part() == RailLook.Part.SLEEPER ? 1 : 0];
        float[] a = piece.area();
        float y0 = (float) piece.box().y0();
        float y1 = (float) piece.box().y1();
        sidesAndBottom(brush, sprite, a[0], y0, a[1], a[2], y1, a[3]);
        PoseStack.Pose pose = brush.poseStack().last();
        float[][] corners = {{a[0], a[1]}, {a[0], a[3]}, {a[2], a[3]}, {a[2], a[1]}};
        for (float[] corner : corners) {
            float[] uv = turnedUv(piece.rotation(), corner[0], corner[1]);
            vertex(brush, pose, corner[0], y1, corner[1], sprite.getU(uv[0] / 16F), sprite.getV(uv[1] / 16F), 0, 1, 0);
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
    private static void box(Brush brush, TextureAtlasSprite sprite, float x0, float y0, float z0, float x1, float y1, float z1) {
        sidesAndBottom(brush, sprite, x0, y0, z0, x1, y1, z1);
        face(brush, brush.poseStack().last(), sprite, 0, 1, 0,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, z0, x1, z1);
    }

    /** Down, north, south, west and east faces of a box; side rows repeat every block so parts below y 0 still map onto the sprite. */
    private static void sidesAndBottom(Brush brush, TextureAtlasSprite sprite, float x0, float y0, float z0, float x1, float y1, float z1) {
        PoseStack.Pose pose = brush.poseStack().last();
        float shift = (float) Math.floor(y0 / 16F) * 16F;
        float v0 = 16 - (y1 - shift);
        float v1 = 16 - (y0 - shift);
        face(brush, pose, sprite, 0, -1, 0,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, z0, x1, z1);
        face(brush, pose, sprite, 0, 0, -1,
                x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, v0, x1, v1);
        face(brush, pose, sprite, 0, 0, 1,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, v0, x1, v1);
        face(brush, pose, sprite, -1, 0, 0,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z0, v0, z1, v1);
        face(brush, pose, sprite, 1, 0, 0,
                x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, z0, v0, z1, v1);
    }

    /** One quad; u0..v1 are pixel texture coordinates, clamped to the sprite. */
    private static void face(Brush brush, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        float su0 = sprite.getU(Math.clamp(u0 / 16F, 0F, 1F));
        float su1 = sprite.getU(Math.clamp(u1 / 16F, 0F, 1F));
        float sv0 = sprite.getV(Math.clamp(v0 / 16F, 0F, 1F));
        float sv1 = sprite.getV(Math.clamp(v1 / 16F, 0F, 1F));
        vertex(brush, pose, ax, ay, az, su0, sv1, nx, ny, nz);
        vertex(brush, pose, bx, by, bz, su0, sv0, nx, ny, nz);
        vertex(brush, pose, cx, cy, cz, su1, sv0, nx, ny, nz);
        vertex(brush, pose, dx, dy, dz, su1, sv1, nx, ny, nz);
    }

    private static void vertex(Brush brush, PoseStack.Pose pose, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        brush.buffer().addVertex(pose, x / 16F, y / 16F, z / 16F).setColor(-1).setUv(u, v).setOverlay(brush.overlay())
                .setLight(brush.light()).setNormal(pose, nx, ny, nz);
    }
}
