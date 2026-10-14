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
 * {@link DuctPose}): the centre is fitted onto the centre box
 * {@link DuctShapes#center(int, int, int, int[])} gives, which grows up or
 * down to reach the arms, an arm's body is stretched and shifted onto its
 * straight box, and its ring is shifted to the height where the arm meets the
 * face. Results are cached per part and box; the cache lives as long as the
 * duct model, which is rebuilt on every resource reload.
 */
final class DuctPoseQuads {

    /** The quads of an arm at a pose: the body, drawn always, and the ring, drawn at endpoints. */
    record ArmQuads(List<BakedQuad> body, List<BakedQuad> ring) {
    }

    private record CenterKey(String part, int radius, float y0, float y1) {
    }

    private record ArmKey(String part, int side, int armRadius, float inner, DuctPose.Height face) {
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

    /**
     * The quads of a centre part of the given radius fitted onto the centre
     * box. Like an arm, only a plain line face takes its UVs from the
     * position; band faces keep theirs, so the bands stay in order.
     */
    List<BakedQuad> center(String part, int radius, DuctShapes.Box centre) {
        DuctShapes.Box baked = DuctShapes.center(radius);
        if (centre.equals(baked)) {
            return partQuads.apply(part);
        }
        return centers.computeIfAbsent(new CenterKey(part, radius, centre.y0(), centre.y1()), key -> {
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedQuad source : partQuads.apply(part)) {
                quads.add(fit(source, baked, centre, DuctPose.Y));
            }
            return List.copyOf(quads);
        });
    }

    /**
     * The quads of an arm part on {@code side}, fitted onto its straight box
     * from the centre box to the block edge.
     *
     * @param armRadius radius of the lines in the arm part
     * @param centre    the duct's centre box
     */
    ArmQuads arm(String part, Direction side, int armRadius, DuctShapes.Box centre, int faces) {
        int along = DuctPose.axisOf(side.ordinal());
        float inner = DuctPose.isPositive(side.ordinal()) ? DuctShapes.max(centre, along) : DuctShapes.min(centre, along);
        ArmKey key = new ArmKey(part, side.ordinal(), armRadius, inner, DuctPose.faceHeight(faces, side.ordinal()));
        return arms.computeIfAbsent(key, k -> bakeArm(k, DuctShapes.arm(side.ordinal(), armRadius, centre, faces)));
    }

    private ArmQuads bakeArm(ArmKey key, DuctShapes.Box segment) {
        DuctShapes.Box baked = DuctShapes.arm(key.side(), key.armRadius());
        float ringShift = (DuctShapes.low(key.armRadius(), key.face()) - (8 - key.armRadius())) / 16F;
        List<BakedQuad> body = new ArrayList<>();
        List<BakedQuad> ring = new ArrayList<>();
        MutableQuad quad = new MutableQuad();
        for (BakedQuad source : partQuads.apply(key.part())) {
            if (source.materialInfo().tintIndex() >= 0) {
                quad.setFrom(source);
                shiftY(quad, ringShift);
                ring.add(quad.toBakedQuad());
            } else {
                body.add(fit(source, baked, segment, DuctPose.axisOf(key.side())));
            }
        }
        return new ArmQuads(List.copyOf(body), List.copyOf(ring));
    }

    /**
     * The quad moved from the box {@code from} onto the box {@code to}. A
     * plain line face takes its UVs from the position stretched along
     * {@code along} but not yet shifted across it, so its texture lines up
     * with the parts beside it; other faces keep their UVs.
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
        float from0 = DuctShapes.min(from, axis) / 16F;
        float from1 = DuctShapes.max(from, axis) / 16F;
        float to0 = DuctShapes.min(to, axis) / 16F;
        float to1 = DuctShapes.max(to, axis) / 16F;
        return to0 + (value - from0) * (to1 - to0) / (from1 - from0);
    }

    private static void shiftY(MutableQuad quad, float shift) {
        for (int vertex = 0; vertex < 4; vertex++) {
            quad.setPositionComponent(vertex, DuctPose.Y, quad.positionComponent(vertex, DuctPose.Y) + shift);
        }
    }
}
