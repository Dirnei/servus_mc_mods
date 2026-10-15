package io.github.dirnei.mc.servus.alchemy.client.sight;

import java.util.ArrayList;
import java.util.List;

/**
 * What Elemental Sight draws on a face of a Gnome Hollow, read from the pixels of its crack mask, cave and crystal
 * textures: which cracks the material fills at a ripeness, how bright it flows, which nodules and crystals show and
 * how far the cave entry has opened. Every face uses the same pixels.
 */
public final class HollowLook {

    /** Pixels per face side. */
    public static final int SIZE = 16;
    /** Ripeness from which nodules break out of the filled cracks. */
    public static final float NODULES_FROM = 0.85F;
    /** Ripeness from which crystals line the cave mouth of a bound Hollow. */
    public static final float CRYSTALS_FROM = 0.3F;
    /** How far apart, in radians of the flow wave, the start and the end of a crack are. */
    private static final float FLOW_SPREAD = 9;

    /** Reads one texture: the ARGB colour of the pixel at column {@code x}, row {@code y}. */
    @FunctionalInterface
    public interface Pixels {

        int argb(int x, int y);
    }

    /**
     * A crack pixel.
     *
     * @param position how far along its crack it lies, 0 at the start, 1 at the end
     * @param nodule whether a nodule breaks out here near ripe
     */
    public record Crack(int x, int y, float position, boolean nodule) {
    }

    /**
     * A pixel of the cave or a crystal.
     *
     * @param ring for the cave, how many pixel rings out from the mouth's middle it lies, 0 in the middle
     */
    public record Cell(int x, int y, int ring) {
    }

    private final List<Crack> cracks;
    private final List<Cell> cave;
    private final List<Cell> crystals;
    private final int rings;

    private HollowLook(List<Crack> cracks, List<Cell> cave, List<Cell> crystals) {
        this.cracks = List.copyOf(cracks);
        this.cave = List.copyOf(cave);
        this.crystals = List.copyOf(crystals);
        this.rings = cave.stream().mapToInt(Cell::ring).max().orElse(-1) + 1;
    }

    /**
     * Reads the look from its textures. A mask pixel with alpha is a crack; its red is the position along the crack,
     * a blue above half marks a nodule. A cave or crystal pixel with alpha belongs to the cave or is a crystal.
     */
    public static HollowLook read(Pixels mask, Pixels cave, Pixels crystals) {
        List<Crack> cracks = new ArrayList<>();
        List<int[]> mouth = new ArrayList<>();
        List<Cell> crystalCells = new ArrayList<>();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int m = mask.argb(x, y);
                if (alpha(m) > 0) {
                    cracks.add(new Crack(x, y, (m >> 16 & 0xFF) / 255F, (m & 0xFF) > 127));
                }
                if (alpha(cave.argb(x, y)) > 0) {
                    mouth.add(new int[] {x, y});
                }
                if (alpha(crystals.argb(x, y)) > 0) {
                    crystalCells.add(new Cell(x, y, 0));
                }
            }
        }
        return new HollowLook(cracks, ringed(mouth), crystalCells);
    }

    /** The cave pixels with their ring around the middle of the mouth. */
    private static List<Cell> ringed(List<int[]> mouth) {
        double cx = mouth.stream().mapToDouble(p -> p[0] + 0.5).average().orElse(0);
        double cy = mouth.stream().mapToDouble(p -> p[1] + 0.5).average().orElse(0);
        return mouth.stream()
                .map(p -> new Cell(p[0], p[1], (int) Math.max(Math.abs(p[0] + 0.5 - cx), Math.abs(p[1] + 0.5 - cy))))
                .toList();
    }

    private static int alpha(int argb) {
        return argb >>> 24;
    }

    /** The crack pixels the material fills at this ripeness, from the start of each crack. */
    public List<Crack> filled(float ripeness) {
        return cracks.stream().filter(crack -> crack.position() <= ripeness).toList();
    }

    /** The nodules that have broken out at this ripeness: none before {@link #NODULES_FROM}, then those on filled cracks. */
    public List<Crack> nodules(float ripeness) {
        if (ripeness < NODULES_FROM) {
            return List.of();
        }
        return cracks.stream().filter(crack -> crack.nodule() && crack.position() <= ripeness).toList();
    }

    /** The crystals around the mouth: only on a bound Hollow from {@link #CRYSTALS_FROM} ripeness. */
    public List<Cell> crystals(float ripeness, boolean bound) {
        return bound && ripeness >= CRYSTALS_FROM ? crystals : List.of();
    }

    /** How many rings of the cave have opened, from 0 when {@code opening} is 0 to all of them at 1. */
    public int ringsOpen(float opening) {
        return Math.clamp((int) Math.floor(opening * (rings + 1)), 0, rings);
    }

    /** The cave pixels open at {@code opening}, from 0 (closed) to 1 (fully open). */
    public List<Cell> cave(float opening) {
        int open = ringsOpen(opening);
        return cave.stream().filter(cell -> cell.ring() < open).toList();
    }

    /** The cave pixels of one ring. */
    public List<Cell> caveRing(int ring) {
        return cave.stream().filter(cell -> cell.ring() == ring).toList();
    }

    /**
     * The brightness of the material at a crack position, 0.2 to 1: a bright band runs from the crack's start to its
     * end as {@code phase} grows.
     */
    public static float flow(float phase, float position) {
        return 0.6F + 0.4F * (float) Math.sin(phase - position * FLOW_SPREAD);
    }
}
