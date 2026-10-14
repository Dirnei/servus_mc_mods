package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

/**
 * Moves the centred duct part quads to a duct's resting pose (see
 * {@link DuctPose}): the centre is shifted by the pose, an arm's body is
 * stretched and shifted onto the segment {@link DuctShapes#armBoxes} gives,
 * and its ring is shifted to where the arm meets the face. Results are cached
 * per part, pose and face; the cache lives as long as the duct model, which is
 * rebuilt on every resource reload.
 */
final class DuctPoseQuads {

    /** The quads of an arm at a pose: the body, drawn always, and the ring, drawn at endpoints. */
    record ArmQuads(List<BakedQuad> body, List<BakedQuad> ring) {
    }

    private record CenterKey(String part, int radius, int poseSides) {
    }

    private record ArmKey(String part, int side, int armRadius, int centerRadius, int poseSides, int sideFaces) {
    }

    /** Sprite names of the single line textures, whose UVs follow the position. */
    private static final Set<String> PLAIN_TEXTURES = Stream.of(Line.VALUES)
            .map(line -> "block/duct_" + line.id())
            .collect(Collectors.toUnmodifiableSet());

    private final Function<String, List<BakedQuad>> partQuads;
    private final Map<CenterKey, List<BakedQuad>> centers = new ConcurrentHashMap<>();
    private final Map<ArmKey, ArmQuads> arms = new ConcurrentHashMap<>();

    /** @param partQuads the unculled quads of a part model by {@code DuctParts} name */
    DuctPoseQuads(Function<String, List<BakedQuad>> partQuads) {
        this.partQuads = partQuads;
    }

    /** The quads of a centre part of the given radius, shifted to the pose. */
    List<BakedQuad> center(String part, int radius, int pose) {
        int sides = DuctPose.sides(pose);
        if (sides == DuctPose.CENTRED) {
            return partQuads.apply(part);
        }
        return centers.computeIfAbsent(new CenterKey(part, radius, sides), key -> {
            float[] shift = new float[3];
            for (int axis = DuctPose.X; axis <= DuctPose.Z; axis++) {
                shift[axis] = offset(radius, DuctPose.side(sides, axis));
            }
            List<BakedQuad> quads = new ArrayList<>();
            MutableQuad quad = new MutableQuad();
            for (BakedQuad source : partQuads.apply(part)) {
                quad.setFrom(source);
                translate(quad, shift);
                quads.add(quad.toBakedQuad());
            }
            return List.copyOf(quads);
        });
    }

    /**
     * The quads of an arm part on {@code side} at the pose and faces.
     *
     * @param armRadius    radius of the lines in the arm part
     * @param centerRadius radius of the duct's centre
     */
    ArmQuads arm(String part, Direction side, int armRadius, int centerRadius, int pose, int faces) {
        int sideFaces = faces >> side.ordinal() * 4 & 15;
        ArmKey key = new ArmKey(part, side.ordinal(), armRadius, centerRadius, DuctPose.sides(pose), sideFaces);
        return arms.computeIfAbsent(key, DuctPoseQuads.this::bakeArm);
    }

    private ArmQuads bakeArm(ArmKey key) {
        int faces = key.sideFaces() << key.side() * 4;
        DuctShapes.ArmBoxes boxes = DuctShapes.armBoxes(key.side(), key.armRadius(), key.centerRadius(), key.poseSides(), faces);
        DuctShapes.Box baked = DuctShapes.arm(key.side(), key.armRadius());
        float[] ringShift = new float[3];
        for (int slot = 0; slot < 2; slot++) {
            int axis = DuctPose.acrossAxis(key.side(), slot);
            ringShift[axis] = offset(key.armRadius(), DuctPose.faceSide(faces, key.side(), axis));
        }
        List<BakedQuad> body = new ArrayList<>();
        List<BakedQuad> ring = new ArrayList<>();
        MutableQuad quad = new MutableQuad();
        for (BakedQuad source : partQuads.apply(key.part())) {
            if (source.materialInfo().tintIndex() >= 0) {
                quad.setFrom(source);
                translate(quad, ringShift);
                ring.add(quad.toBakedQuad());
            } else if (boxes.segment() != null) {
                body.add(fit(source, baked, boxes.segment(), DuctPose.axisOf(key.side())));
            }
        }
        return new ArmQuads(List.copyOf(body), List.copyOf(ring));
    }

    /**
     * The quad moved from the box {@code from} onto the box {@code to}. A
     * plain line face takes its UVs from the position stretched along the arm
     * but not yet shifted across it, so its texture lines up with the centre's.
     */
    private static BakedQuad fit(BakedQuad source, DuctShapes.Box from, DuctShapes.Box to, int along) {
        MutableQuad quad = new MutableQuad().setFrom(source);
        for (int vertex = 0; vertex < 4; vertex++) {
            quad.setPositionComponent(vertex, along, remap(quad.positionComponent(vertex, along), from, to, along));
        }
        if (PLAIN_TEXTURES.contains(source.materialInfo().sprite().contents().name().getPath())) {
            quad.bakeUvsFromPosition();
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int axis = DuctPose.X; axis <= DuctPose.Z; axis++) {
                if (axis != along) {
                    quad.setPositionComponent(vertex, axis, remap(quad.positionComponent(vertex, axis), from, to, axis));
                }
            }
        }
        return quad.toBakedQuad();
    }

    /** A coordinate in blocks mapped from the span of {@code from} on an axis to the span of {@code to}. */
    private static float remap(float value, DuctShapes.Box from, DuctShapes.Box to, int axis) {
        float from0 = min(from, axis) / 16F;
        float from1 = max(from, axis) / 16F;
        float to0 = min(to, axis) / 16F;
        float to1 = max(to, axis) / 16F;
        return to0 + (value - from0) * (to1 - to0) / (from1 - from0);
    }

    /** How far, in blocks, a part of the radius moves from the middle to {@code side}. */
    private static float offset(int radius, DuctPose.Side side) {
        return (DuctShapes.low(radius, side) - (8 - radius)) / 16F;
    }

    private static void translate(MutableQuad quad, float[] shift) {
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int axis = DuctPose.X; axis <= DuctPose.Z; axis++) {
                quad.setPositionComponent(vertex, axis, quad.positionComponent(vertex, axis) + shift[axis]);
            }
        }
    }

    private static float min(DuctShapes.Box box, int axis) {
        return switch (axis) {
            case DuctPose.X -> box.x0();
            case DuctPose.Y -> box.y0();
            default -> box.z0();
        };
    }

    private static float max(DuctShapes.Box box, int axis) {
        return switch (axis) {
            case DuctPose.X -> box.x1();
            case DuctPose.Y -> box.y1();
            default -> box.z1();
        };
    }
}
