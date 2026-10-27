package io.github.dirnei.mc.servus.shuttles.fluidstation;

import org.jspecify.annotations.Nullable;

/**
 * What a Fluid Outbound tank keeps: a fluid, or none, and the amount
 * tankers restock it to. A tank takes only its keep fluid. Pure Java;
 * {@code K} names a fluid.
 *
 * @param fluid  the kept fluid, null when the tank keeps nothing
 * @param amount mB the tank is restocked to
 */
public record FluidKeep<K>(@Nullable K fluid, int amount) {

    /** Keeps nothing. */
    public static <K> FluidKeep<K> none() {
        return new FluidKeep<>(null, 0);
    }

    /** Whether a fluid is kept. */
    public boolean isSet() {
        return fluid != null;
    }

    /** Whether the tank takes this fluid: only its keep fluid. */
    public boolean accepts(K key) {
        return fluid != null && fluid.equals(key);
    }

    /** The same keep with the amount limited to 0 and {@code capacity}. */
    public FluidKeep<K> clamp(int capacity) {
        return new FluidKeep<>(fluid, Math.clamp(amount, 0, Math.max(0, capacity)));
    }

    /** Keeps another fluid at the same amount, or nothing at all when {@code fluid} is null. */
    public FluidKeep<K> withFluid(@Nullable K fluid) {
        return fluid == null ? none() : new FluidKeep<>(fluid, amount);
    }

    /** The amount stepped by {@code delta}, limited to 0 and {@code capacity}. */
    public FluidKeep<K> step(int delta, int capacity) {
        return new FluidKeep<>(fluid, amount + delta).clamp(capacity);
    }

    /** The amount tankers restock to: 0 without a keep fluid. */
    public int target() {
        return fluid == null ? 0 : amount;
    }
}
