package io.github.dirnei.mc.servus.storage.cpu;

import java.util.Set;

/**
 * Checks whether the unformed CPU and Buffer blocks joined to a clicked block
 * fill a solid box that can form a Crafting CPU, and gives every block of a
 * box its place class (see {@link CpuShape}).
 */
public final class CpuBox {

    /** Why joined blocks do not form, in the order they are checked. */
    public enum Reason {
        TOO_BIG("too_big"),
        TOO_SMALL("too_small"),
        NOT_A_BOX("not_a_box"),
        GAP("gap"),
        NO_CPU_BLOCK("no_cpu_block");

        private final String id;

        Reason(String id) {
            this.id = id;
        }

        /** The suffix of the action bar message key. */
        public String id() {
            return id;
        }
    }

    /**
     * A box that forms.
     *
     * @param corner      the lowest corner (smallest x, y and z)
     * @param size        the length of every side
     * @param cpuBlocks   how many CPU blocks it has
     * @param bufferBlocks how many Buffer blocks it has
     */
    public record Box(CpuShape.Vec corner, CpuShape.Vec size, int cpuBlocks, int bufferBlocks) {

        public int blocks() {
            return size.x() * size.y() * size.z();
        }

        /** The place class of the block at an offset from the corner. */
        public int part(CpuShape.Vec offset) {
            return CpuBox.part(offset, size);
        }
    }

    /**
     * The outcome of a check: a box, or a reason with the lowest missing
     * position for a gap.
     */
    public record Result(Box box, Reason reason, CpuShape.Vec gap) {

        public boolean formed() {
            return box != null;
        }

        static Result of(Box box) {
            return new Result(box, null, null);
        }

        static Result failed(Reason reason) {
            return new Result(null, reason, null);
        }
    }

    private CpuBox() {
    }

    /**
     * @param joined    the unformed blocks joined face to face to the clicked one
     * @param cpuBlocks those of them that are CPU blocks (the rest are Buffer blocks)
     * @param maxBlocks how many blocks a box may have at most
     */
    public static Result check(Set<CpuShape.Vec> joined, Set<CpuShape.Vec> cpuBlocks, int maxBlocks) {
        if (joined.size() > maxBlocks) {
            return Result.failed(Reason.TOO_BIG);
        }
        if (joined.isEmpty()) {
            return Result.failed(Reason.TOO_SMALL);
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (CpuShape.Vec at : joined) {
            minX = Math.min(minX, at.x());
            minY = Math.min(minY, at.y());
            minZ = Math.min(minZ, at.z());
            maxX = Math.max(maxX, at.x());
            maxY = Math.max(maxY, at.y());
            maxZ = Math.max(maxZ, at.z());
        }
        CpuShape.Vec corner = new CpuShape.Vec(minX, minY, minZ);
        CpuShape.Vec size = new CpuShape.Vec(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        if (size.x() < 2 || size.y() < 2 || size.z() < 2) {
            return Result.failed(Reason.TOO_SMALL);
        }
        for (int x : new int[] {minX, maxX}) {
            for (int y : new int[] {minY, maxY}) {
                for (int z : new int[] {minZ, maxZ}) {
                    if (!joined.contains(new CpuShape.Vec(x, y, z))) {
                        return Result.failed(Reason.NOT_A_BOX);
                    }
                }
            }
        }
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    CpuShape.Vec at = new CpuShape.Vec(x, y, z);
                    if (!joined.contains(at)) {
                        return new Result(null, Reason.GAP, at);
                    }
                }
            }
        }
        int cpus = 0;
        for (CpuShape.Vec at : joined) {
            if (cpuBlocks.contains(at)) {
                cpus++;
            }
        }
        if (cpus == 0) {
            return Result.failed(Reason.NO_CPU_BLOCK);
        }
        return Result.of(new Box(corner, size, cpus, joined.size() - cpus));
    }

    /** The place class on one axis: 0 low, 1 middle, 2 high; a side of 2 has no middle. */
    public static int placeClass(int offset, int size) {
        if (offset < 0 || offset >= size) {
            throw new IllegalArgumentException("Offset " + offset + " outside a side of " + size);
        }
        if (offset == 0) {
            return 0;
        }
        return offset == size - 1 ? 2 : 1;
    }

    /** The place class of the block at an offset from the corner of a box of the given size. */
    public static int part(CpuShape.Vec offset, CpuShape.Vec size) {
        return CpuShape.part(placeClass(offset.x(), size.x()), placeClass(offset.y(), size.y()),
                placeClass(offset.z(), size.z()));
    }
}
