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
 * {@link DuctPose}): the centre is shifted to the pose's height, an arm's body
 * is stretched and shifted onto the segment {@link DuctShapes#armBoxes} gives,
 * and its ring is shifted to the height where the arm meets the face. A
 * stepping arm gets its step box built here, with the line's texture for one
 * line and the junction texture for several. Results are cached per part,
 * height and face height; the cache lives as long as the duct model, which is
 * rebuilt on every resource reload.
 */
final class DuctPoseQuads {

    /** The quads of an arm at a pose: the body, drawn always, and the ring, drawn at endpoints. */
    record ArmQuads(List<BakedQuad> body, List<BakedQuad> ring) {
    }

    private record CenterKey(String part, int radius, DuctPose.Height height) {
    }

    private record ArmKey(String part, String stepPart, int side, int armRadius, int centerRadius, DuctPose.Height height,
                          DuctPose.Height face) {
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

    /** The quads of a centre part of the given radius, shifted to the pose's height. */
    List<BakedQuad> center(String part, int radius, int pose) {
        DuctPose.Height height = DuctPose.height(pose);
        if (height == DuctPose.Height.MID) {
            return partQuads.apply(part);
        }
        return centers.computeIfAbsent(new CenterKey(part, radius, height), key -> {
            float shift = offset(radius, height);
            List<BakedQuad> quads = new ArrayList<>();
            MutableQuad quad = new MutableQuad();
            for (BakedQuad source : partQuads.apply(part)) {
                quad.setFrom(source);
                shiftY(quad, shift);
                quads.add(quad.toBakedQuad());
            }
            return List.copyOf(quads);
        });
    }

    /**
     * The quads of an arm part on {@code side} at the pose and faces.
     *
     * @param stepPart     the part whose first quad lends its sprite to the step box
     * @param armRadius    radius of the lines in the arm part
     * @param centerRadius radius of the duct's centre
     */
    ArmQuads arm(String part, String stepPart, Direction side, int armRadius, int centerRadius, int pose, int faces) {
        ArmKey key = new ArmKey(part, stepPart, side.ordinal(), armRadius, centerRadius, DuctPose.height(pose),
                DuctPose.faceHeight(faces, side.ordinal()));
        return arms.computeIfAbsent(key, DuctPoseQuads.this::bakeArm);
    }

    private ArmQuads bakeArm(ArmKey key) {
        int pose = DuctPose.of(key.height(), DuctPose.Rank.FREE);
        int faces = key.face() == DuctPose.Height.LOW ? 1 << key.side() : 0;
        DuctShapes.ArmBoxes boxes = DuctShapes.armBoxes(key.side(), key.armRadius(), key.centerRadius(), pose, faces);
        DuctShapes.Box baked = DuctShapes.arm(key.side(), key.armRadius());
        float ringShift = offset(key.armRadius(), key.face());
        List<BakedQuad> body = new ArrayList<>();
        List<BakedQuad> ring = new ArrayList<>();
        MutableQuad quad = new MutableQuad();
        for (BakedQuad source : partQuads.apply(key.part())) {
            if (source.materialInfo().tintIndex() >= 0) {
                quad.setFrom(source);
                shiftY(quad, ringShift);
                ring.add(quad.toBakedQuad());
            } else {
                body.add(fit(source, baked, boxes.segment(), DuctPose.axisOf(key.side())));
            }
        }
        if (boxes.step() != null) {
            body.addAll(step(key, pose, boxes.step()));
        }
        return new ArmQuads(List.copyOf(body), List.copyOf(ring));
    }

    /** The faces of the step box, without the face toward the centre where the centre covers it. */
    private List<BakedQuad> step(ArmKey key, int pose, DuctShapes.Box step) {
        List<BakedQuad> sources = partQuads.apply(key.stepPart());
        if (sources.isEmpty()) {
            return List.of();
        }
        BakedQuad.MaterialInfo info = sources.get(0).materialInfo();
        Direction inward = Direction.values()[key.side()].getOpposite();
        boolean innerHidden = covers(DuctShapes.center(key.centerRadius(), pose), step, DuctPose.axisOf(key.side()));
        List<BakedQuad> quads = new ArrayList<>(6);
        for (Direction face : Direction.values()) {
            if (face == inward && innerHidden) {
                continue;
            }
            quads.add(new MutableQuad()
                    .setCubeFace(face, step.x0() / 16F, step.y0() / 16F, step.z0() / 16F, step.x1() / 16F, step.y1() / 16F, step.z1() / 16F)
                    .setSprite(info.sprite(), info.layer(), info.itemRenderType())
                    .bakeUvsFromPosition()
                    .setTintIndex(-1)
                    .setShade(true)
                    .toBakedQuad());
        }
        return quads;
    }

    /** Whether {@code outer} spans {@code inner} on both axes other than {@code along}. */
    private static boolean covers(DuctShapes.Box outer, DuctShapes.Box inner, int along) {
        for (int axis = DuctPose.X; axis <= DuctPose.Z; axis++) {
            if (axis != along && (min(inner, axis) < min(outer, axis) || max(inner, axis) > max(outer, axis))) {
                return false;
            }
        }
        return true;
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

    /** How far up, in blocks, a part of the radius moves from the middle to {@code height}. */
    private static float offset(int radius, DuctPose.Height height) {
        return (DuctShapes.low(radius, height) - (8 - radius)) / 16F;
    }

    private static void shiftY(MutableQuad quad, float shift) {
        for (int vertex = 0; vertex < 4; vertex++) {
            quad.setPositionComponent(vertex, DuctPose.Y, quad.positionComponent(vertex, DuctPose.Y) + shift);
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
