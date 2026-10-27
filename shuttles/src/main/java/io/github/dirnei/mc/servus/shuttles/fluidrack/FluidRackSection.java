package io.github.dirnei.mc.servus.shuttles.fluidrack;

/**
 * The rules of one Fluid Rack section: which bay carries the port, how the
 * section's fluid is split into the bays' shares and which bays may join.
 * Bays are counted from the left as seen from the front. Pure Java.
 */
public final class FluidRackSection {

    private FluidRackSection() {
    }

    /** The bay that carries the port: the middle one of three, the left one of two, the only one of one. */
    public static int portIndex(int size) {
        return size >= 3 ? (size - 1) / 2 : 0;
    }

    /**
     * Splits {@code total} evenly over {@code bays}, the remainder going to
     * the leftmost bays one by one.
     */
    public static int[] shares(int total, int bays) {
        int[] shares = new int[Math.max(0, bays)];
        if (bays <= 0 || total <= 0) {
            return shares;
        }
        int each = total / bays;
        int rest = total % bays;
        for (int i = 0; i < bays; i++) {
            shares[i] = each + (i < rest ? 1 : 0);
        }
        return shares;
    }

    /**
     * Whether a bay holding {@code bayFluid} may join a section holding
     * {@code sectionFluid}; null means empty. {@code K} is compared with {@code equals}.
     */
    public static <K> boolean canJoin(K sectionFluid, K bayFluid) {
        return sectionFluid == null || bayFluid == null || sectionFluid.equals(bayFluid);
    }
}
