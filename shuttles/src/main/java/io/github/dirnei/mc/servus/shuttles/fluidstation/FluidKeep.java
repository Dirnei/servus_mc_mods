package io.github.dirnei.mc.servus.shuttles.fluidstation;

import org.jspecify.annotations.Nullable;

/**
 * What a Fluid Outbound tank keeps: a fluid, or none, the amount tankers
 * restock it to and the restock level a restock starts at. A tank takes only
 * its keep fluid. The restock level stays within 0 and the amount and moves
 * with the amount while the two are equal. Pure Java; {@code K} names a fluid.
 *
 * @param fluid     the kept fluid, null when the tank keeps nothing
 * @param amount    mB the tank is restocked to
 * @param restockAt mB at or below which a restock starts
 */
public record FluidKeep<K>(@Nullable K fluid, int amount, int restockAt) {

    /** Keeps {@code amount} and restocks at that amount. */
    public FluidKeep(@Nullable K fluid, int amount) {
        this(fluid, amount, amount);
    }

    /** Keeps nothing. */
    public static <K> FluidKeep<K> none() {
        return new FluidKeep<>(null, 0, 0);
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

    /** Keeps another fluid at the same amounts, or nothing at all when {@code fluid} is null. */
    public FluidKeep<K> withFluid(@Nullable K fluid) {
        return fluid == null ? none() : new FluidKeep<>(fluid, amount, restockAt);
    }

    /**
     * The amount stepped by {@code delta}, limited to 0 and {@code capacity}.
     * A restock level equal to the amount moves along; a higher one is cut to the new amount.
     */
    public FluidKeep<K> step(int delta, int capacity) {
        FluidKeep<K> stepped = new FluidKeep<>(fluid, amount + delta, restockAt).clamp(capacity);
        return restockAt == amount ? new FluidKeep<>(fluid, stepped.amount, stepped.amount) : stepped;
    }

    /** The restock level stepped by {@code delta}, limited to 0 and the amount. */
    public FluidKeep<K> stepRestock(int delta) {
        return new FluidKeep<>(fluid, amount, Math.clamp((long) restockAt + delta, 0, amount));
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
