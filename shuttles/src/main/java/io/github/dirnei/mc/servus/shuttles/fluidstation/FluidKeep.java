package io.github.dirnei.mc.servus.shuttles.fluidstation;

import static io.github.dirnei.mc.servus.shuttles.fluid.FluidRestockPlan.BUCKET;

import org.jspecify.annotations.Nullable;

/**
 * What a Fluid Outbound tank keeps: a fluid, or none, the amount tankers
 * restock it to and the restock level a restock starts at. A tank takes only
 * its keep fluid. Both levels stay within 0 and the capacity, the restock
 * level never above the amount: setting one moves the other along when they
 * would cross. Pure Java; {@code K} names a fluid.
 *
 * @param fluid     the kept fluid, null when the tank keeps nothing
 * @param amount    mB the tank is restocked to
 * @param restockAt mB at or below which a restock starts
 */
public record FluidKeep<K>(@Nullable K fluid, int amount, int restockAt) {

    /** Keeps nothing. */
    public static <K> FluidKeep<K> none() {
        return new FluidKeep<>(null, 0, 0);
    }

    /** A new keep of {@code fluid}: the full capacity, restocked from one bucket. */
    public static <K> FluidKeep<K> started(K fluid, int capacity) {
        return new FluidKeep<>(fluid, capacity, BUCKET).clamp(capacity);
    }

    /** Whether a fluid is kept. */
    public boolean isSet() {
        return fluid != null;
    }

    /** Whether the tank takes this fluid: only its keep fluid. */
    public boolean accepts(K key) {
        return fluid != null && fluid.equals(key);
    }

    /** The same keep with the amount limited to 0 and {@code capacity}, and the restock level to 0 and the amount. */
    public FluidKeep<K> clamp(int capacity) {
        int limited = Math.clamp(amount, 0, Math.max(0, capacity));
        return new FluidKeep<>(fluid, limited, Math.clamp(restockAt, 0, limited));
    }

    /**
     * Keeps another fluid at the same levels, a new {@link #started} keep when
     * nothing was kept, or nothing at all when {@code fluid} is null.
     */
    public FluidKeep<K> withFluid(@Nullable K fluid, int capacity) {
        if (fluid == null) {
            return none();
        }
        return isSet() ? new FluidKeep<>(fluid, amount, restockAt).clamp(capacity) : started(fluid, capacity);
    }

    /** The amount set to {@code millibuckets} within 0 and {@code capacity}; a higher restock level comes down with it. */
    public FluidKeep<K> withAmount(int millibuckets, int capacity) {
        return new FluidKeep<>(fluid, millibuckets, restockAt).clamp(capacity);
    }

    /** The restock level set to {@code millibuckets} within 0 and {@code capacity}; a lower amount goes up with it. */
    public FluidKeep<K> withRestockAt(int millibuckets, int capacity) {
        int level = Math.clamp(millibuckets, 0, Math.max(0, capacity));
        return new FluidKeep<>(fluid, Math.max(amount, level), level).clamp(capacity);
    }

    /** The amount tankers restock to: 0 without a keep fluid. */
    public int target() {
        return fluid == null ? 0 : amount;
    }

    /** The level a restock starts at: 0 without a keep fluid. */
    public int restockLevel() {
        return fluid == null ? 0 : restockAt;
    }
}
