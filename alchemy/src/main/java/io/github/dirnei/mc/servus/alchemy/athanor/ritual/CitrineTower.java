package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import java.util.ArrayList;
import java.util.List;

/**
 * The citrine a ritual grows out of a vat: a stepped tower of upright boxes with a stepped tip, and small buds on its sides
 * and steps that sprout once the tower stands. Sizes are in pixels from the vat's block corner, y from the vat's bottom.
 * Pure Java.
 */
public final class CitrineTower {

    /** The side of a tower block a bud grows on. */
    public enum Face {
        UP(0, 0), NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);

        private final int dx;
        private final int dz;

        Face(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public int dx() {
            return dx;
        }

        public int dz() {
            return dz;
        }
    }

    /** Whether a bud may grow on {@code face} of the tower at block level {@code level} above the vat. */
    @FunctionalInterface
    public interface Openness {
        Openness ALL = (face, level) -> true;

        boolean open(Face face, int level);
    }

    /** An upright box: its lowest corner and its size. */
    public record Box(float x, float y, float z, float width, float height, float depth) {
    }

    /**
     * One shard of a bud, standing on the bud's root along the bud's axis.
     *
     * @param x         offset from the bud's axis across the face
     * @param z         offset from the bud's axis along the face's second direction
     * @param width     of its square body
     * @param body      height of the body
     * @param tipWidth  of the square tip on the body, 0 when it has none
     * @param tipHeight of that tip
     * @param tiltX     tilt about the bud's x axis, in radians: 0 or a 22.5 degree step
     * @param tiltZ     tilt about the bud's z axis, in radians: 0 or a 22.5 degree step
     */
    public record Shard(float x, float z, int width, int body, int tipWidth, int tipHeight, float tiltX, float tiltZ) {
    }

    /**
     * One bud on the tower.
     *
     * @param face  the side it grows on; its axis points out of that side
     * @param x     its root
     * @param y     its root
     * @param z     its root
     * @param stage 1 to 4, from small to full
     * @param lean  toward up, in radians; 0 on top of a step
     * @param spin  about its own axis, in radians
     * @param scale of its height, around 1
     */
    public record Bud(Face face, float x, float y, float z, int stage, float lean, float spin, float scale) {

        /** The shards at this bud's stage and scale. */
        public List<Shard> shards() {
            List<Shard> shards = new ArrayList<>(SHARDS.size());
            for (ShardPlan plan : SHARDS) {
                int width = (stage == 1 ? Math.max(1, plan.width() - 1) : plan.width()) + (scale > 1.15F ? 1 : 0);
                int height = Math.max(1, Math.round(plan.height() * scale * stage / 4F));
                int body = Math.max(1, height - (width >= 3 ? 2 : 1));
                int tipHeight = height - body;
                int tipWidth = tipHeight <= 0 ? 0 : width >= 3 ? 1 : Math.max(1, width - 1);
                shards.add(new Shard(plan.x(), plan.z(), width, body, tipWidth, tipHeight, plan.tiltX(), plan.tiltZ()));
            }
            return shards;
        }
    }

    /** What has grown at one moment: the tower's boxes, lowest first, then the tip's steps, and the buds. */
    public record Shape(List<Box> tower, List<Bud> buds) {
    }

    private record Segment(int width, int height, int dx, int dz) {
    }

    private record Placement(int segment, Face face, float along, float up, float start) {
    }

    private record ShardPlan(float x, float z, int width, int height, float tiltX, float tiltZ) {
    }

    /** The share of the growth in which the tower rises; the buds sprout in the rest. */
    public static final float TOWER_SHARE = 0.55F;
    /** The largest lean of a bud on a side of the tower. */
    public static final float MAX_LEAN = (float) Math.toRadians(38);
    /** How much bigger or smaller a bud may be than its plan. */
    public static final float SIZE_VARIATION = 0.4F;
    /** How many different towers there are; vats pick one by their position. */
    public static final int SEEDS = 35;
    /** How many blocks the tower has below its tip; the vat's opening is 10 pixels, the lowest block narrower. */
    public static final int SEGMENTS = 3;
    /** Where the tower stands in the vat, in pixels from the vat's bottom. */
    public static final float FOOT = 2;

    private static final float CENTER = 8;
    private static final float TILT = (float) (Math.PI / 8);
    private static final float BUD_SPAN = 0.32F;
    private static final float BUD_GROW = 0.13F;
    private static final float OFF_FACE = 0.05F;
    private static final int TIP_STEP = 2;
    private static final int[] TIP = {2};

    private static final List<Segment> BLOCKS = List.of(new Segment(8, 18, 0, 0), new Segment(6, 16, 1, 0), new Segment(4, 12, 0, 1));

    private static final List<Placement> PLACEMENTS = List.of(
            new Placement(0, Face.NORTH, 1, 12, 0.0F), new Placement(0, Face.EAST, -2, 10, 0.1F), new Placement(0, Face.SOUTH, 1, 14, 0.2F),
            new Placement(0, Face.WEST, 2, 11, 0.3F), new Placement(0, Face.UP, 0, 0, 0.35F), new Placement(1, Face.EAST, 1, 9, 0.45F),
            new Placement(1, Face.WEST, -1, 7, 0.5F), new Placement(1, Face.NORTH, -1, 11, 0.6F), new Placement(1, Face.UP, 0, 0, 0.62F),
            new Placement(2, Face.SOUTH, 0, 6, 0.7F), new Placement(2, Face.EAST, 0, 4, 0.8F));

    private static final List<ShardPlan> SHARDS = List.of(
            new ShardPlan(0, 0, 2, 6, 0, 0), new ShardPlan(-1.6F, 0, 1, 4, 0, TILT), new ShardPlan(1.6F, 0, 1, 3, 0, -TILT),
            new ShardPlan(0, -1.6F, 1, 3, -TILT, 0));

    private CitrineTower() {
    }

    /** The seed of the vat at block {@code x}, {@code z}, so neighbouring vats grow different towers. */
    public static int seed(int x, int z) {
        return Math.floorMod(x * 7 + z * 13, SEEDS);
    }

    /** The tower of {@code seed} grown {@code growth} of the way, 0 to 1, with buds only on open sides. */
    public static Shape grown(int seed, float growth, Openness openness) {
        List<Box> tower = new ArrayList<>();
        float rise = RitualTimeline.clamp01(growth) / TOWER_SHARE;
        float y = FOOT;
        for (int i = 0; i < BLOCKS.size(); i++) {
            Segment segment = BLOCKS.get(i);
            float own = smooth(rise * BLOCKS.size() - i);
            int height = Math.round(segment.height() * own);
            if (height > 0) {
                int width = Math.max(2, Math.round(segment.width() * (0.5F + 0.5F * own)));
                tower.add(new Box(CENTER - width / 2F + segment.dx(), y, CENTER - width / 2F + segment.dz(), width, height, width));
            }
            y += segment.height();
        }
        List<Bud> buds = new ArrayList<>();
        if (rise < 1) {
            return new Shape(tower, buds);
        }
        Box top = tower.getLast();
        float centerX = top.x() + top.width() / 2;
        float centerZ = top.z() + top.depth() / 2;
        for (int step : TIP) {
            tower.add(new Box(centerX - step / 2F, y, centerZ - step / 2F, step, TIP_STEP, step));
            y += TIP_STEP;
        }
        for (int k = 0; k < PLACEMENTS.size(); k++) {
            Placement placement = PLACEMENTS.get(k);
            float own = (growth - TOWER_SHARE - placement.start() * BUD_SPAN) / BUD_GROW;
            if (own <= 0) {
                continue;
            }
            Bud bud = bud(placement, tower.get(placement.segment()), Math.min(4, 1 + (int) (own * 4)), k + seed);
            if (placement.face() == Face.UP || openness.open(placement.face(), (int) Math.floor(bud.y() / 16))) {
                buds.add(bud);
            }
        }
        return new Shape(tower, buds);
    }

    private static Bud bud(Placement placement, Box segment, int stage, int variant) {
        Face face = placement.face();
        float centerX = segment.x() + segment.width() / 2;
        float centerZ = segment.z() + segment.depth() / 2;
        float spin = MAX_LEAN * (variant * 3 % 5 / 2F - 1);
        float scale = 1 + SIZE_VARIATION * (variant * 5 % 7 / 3F - 1);
        if (face == Face.UP) {
            return new Bud(face, centerX + placement.along(), segment.y() + segment.height() + OFF_FACE, centerZ + placement.up(), stage, 0,
                    spin, scale);
        }
        float lean = MAX_LEAN * (0.5F + 0.5F * (variant * 7 % 5) / 4F);
        float out = segment.width() / 2 + OFF_FACE;
        float alongX = face.dx() == 0 ? placement.along() : 0;
        float alongZ = face.dx() == 0 ? 0 : placement.along();
        float y = segment.y() + Math.min(placement.up(), segment.height() - 3);
        return new Bud(face, centerX + face.dx() * out + alongX, y, centerZ + face.dz() * out + alongZ, stage, lean, spin, scale);
    }

    private static float smooth(float t) {
        if (t <= 0) {
            return 0;
        }
        return t >= 1 ? 1 : t * t * (3 - 2 * t);
    }
}
