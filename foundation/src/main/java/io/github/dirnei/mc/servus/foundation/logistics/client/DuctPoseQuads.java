package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctUv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

/**
 * Moves the centred duct part quads to a duct's resting pose (see
 * {@link DuctPose}) and gives them their texture positions (see
 * {@link DuctUv}): a centre is fitted onto the centre box
 * {@link DuctShapes#center(int, int, int, int[])} gives, an arm's body is
 * stretched and shifted onto its straight box, and its ring is shifted to the
 * height where the arm meets the face. Side faces of centres and arms sample
 * their texture along the run; a fitting is mapped face by face onto its
 * texture. Results are cached per part and box; the cache lives as long as
 * the duct model, which is rebuilt on every resource reload.
 */
final class DuctPoseQuads {

    /** The quads of an arm at a pose: the body, drawn always, and the ring, drawn at endpoints. */
    record ArmQuads(List<BakedQuad> body, List<BakedQuad> ring) {
    }

    private record CenterKey(String part, int radius, float y0, float y1, int runAxis) {
    }

    private record ArmKey(String part, int side, int armRadius, float inner, DuctPose.Height face) {
    }

    /** {@link CenterKey#runAxis} of a fitting, which has no run. */
    private static final int FITTING = -1;

    private final Function<String, List<BakedQuad>> partQuads;
    private final Map<CenterKey, List<BakedQuad>> centers = new ConcurrentHashMap<>();
    private final Map<ArmKey, ArmQuads> arms = new ConcurrentHashMap<>();

    /** @param partQuads the unculled quads of a part model by {@code DuctParts} name */
    DuctPoseQuads(Function<String, List<BakedQuad>> partQuads) {
        this.partQuads = partQuads;
    }

    /**
     * The quads of a centre part of the given radius fitted onto the centre
     * box, its side faces sampling the texture along the run.
     */
    List<BakedQuad> center(String part, int radius, DuctShapes.Box centre, Direction.Axis run) {
        return centers.computeIfAbsent(new CenterKey(part, radius, centre.y0(), centre.y1(), run.ordinal()),
                key -> fitted(part, radius, centre, run.ordinal()));
    }

    /** The quads of a fitting part of the given radius fitted onto the centre box, each face mapped onto the fitting texture. */
    List<BakedQuad> fitting(String part, int radius, DuctShapes.Box centre) {
        return centers.computeIfAbsent(new CenterKey(part, radius, centre.y0(), centre.y1(), FITTING),
                key -> fitted(part, radius, centre, FITTING));
    }

    private List<BakedQuad> fitted(String part, int radius, DuctShapes.Box centre, int runAxis) {
        DuctShapes.Box baked = DuctShapes.center(radius);
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedQuad source : partQuads.apply(part)) {
            MutableQuad quad = fit(source, baked, centre, DuctPose.Y);
            if (runAxis == FITTING) {
                fittingUvs(quad, radius);
            } else {
                sideUvs(quad, runAxis, middle(centre));
            }
            quads.add(quad.toBakedQuad());
        }
        return List.copyOf(quads);
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
        int along = DuctPose.axisOf(key.side());
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
                MutableQuad fitted = fit(source, baked, segment, along);
                sideUvs(fitted, along, middle(segment));
                body.add(fitted.toBakedQuad());
            }
        }
        return new ArmQuads(List.copyOf(body), List.copyOf(ring));
    }

    /** The quad moved from the box {@code from} onto the box {@code to}, stretched along {@code along} and shifted across it. */
    private static MutableQuad fit(BakedQuad source, DuctShapes.Box from, DuctShapes.Box to, int along) {
        MutableQuad quad = new MutableQuad().setFrom(source);
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int axis = DuctPose.X; axis <= DuctPose.Z; axis++) {
                quad.setPositionComponent(vertex, axis, remap(quad.positionComponent(vertex, axis), from, to, axis));
            }
        }
        return quad;
    }

    /** Side faces sample their texture along the run; end faces keep their UVs. */
    private static void sideUvs(MutableQuad quad, int runAxis, float[] middle) {
        int normal = quad.direction().getAxis().ordinal();
        for (int vertex = 0; vertex < 4; vertex++) {
            DuctUv.Uv uv = DuctUv.side(pixels(quad, vertex), normal, runAxis, middle);
            if (uv == null) {
                return;
            }
            quad.setUvFromSprite(vertex, uv.u() / 16F, uv.v() / 16F);
        }
    }

    /** Each face of a fitting covers the square of its radius on the fitting texture. */
    private static void fittingUvs(MutableQuad quad, int radius) {
        int normal = quad.direction().getAxis().ordinal();
        float[] min = pixels(quad, 0);
        float[] max = pixels(quad, 0);
        for (int vertex = 1; vertex < 4; vertex++) {
            float[] point = pixels(quad, vertex);
            for (int axis = DuctPose.X; axis <= DuctPose.Z; axis++) {
                min[axis] = Math.min(min[axis], point[axis]);
                max[axis] = Math.max(max[axis], point[axis]);
            }
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            DuctUv.Uv uv = DuctUv.fitting(pixels(quad, vertex), normal, min, max, radius);
            quad.setUvFromSprite(vertex, uv.u() / 16F, uv.v() / 16F);
        }
    }

    private static float[] pixels(MutableQuad quad, int vertex) {
        return new float[] {quad.positionComponent(vertex, DuctPose.X) * 16F, quad.positionComponent(vertex, DuctPose.Y) * 16F,
                quad.positionComponent(vertex, DuctPose.Z) * 16F};
    }

    private static float[] middle(DuctShapes.Box box) {
        return new float[] {(box.x0() + box.x1()) / 2, (box.y0() + box.y1()) / 2, (box.z0() + box.z1()) / 2};
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
