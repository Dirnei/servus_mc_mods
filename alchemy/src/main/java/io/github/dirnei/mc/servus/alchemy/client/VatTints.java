package io.github.dirnei.mc.servus.alchemy.client;

import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CrystalRamp;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * The colours a running ritual gives grown vats that it turns into another crystal, written by the Athanor's renderer each
 * frame and read by the vat's own. A tint not renewed for a few ticks is stale: the ritual ended or is out of sight.
 */
public final class VatTints {

    /**
     * @param ramp  colours of the crystal's body
     * @param color RGB of its liquid
     */
    public record Tint(CrystalRamp ramp, int color, float time) {
    }

    private static final float STALE = 3;
    private static final Map<BlockPos, Tint> TINTS = new HashMap<>();

    private VatTints() {
    }

    public static void put(BlockPos vat, CrystalRamp ramp, int color, float time) {
        TINTS.put(vat.immutable(), new Tint(ramp, color, time));
    }

    /** The tint of the vat at {@code vat} at game time {@code time}, or null when no ritual turns it now. */
    public static @Nullable Tint at(BlockPos vat, float time) {
        Tint tint = TINTS.get(vat);
        if (tint != null && Math.abs(time - tint.time()) > STALE) {
            TINTS.remove(vat);
            return null;
        }
        return tint;
    }
}
