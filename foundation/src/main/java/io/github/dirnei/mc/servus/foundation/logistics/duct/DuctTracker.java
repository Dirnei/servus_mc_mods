package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.world.level.Level;

/**
 * The utility ducts loaded on the client, for the hidden duct overlay. Lives
 * in common code without client imports; only client block entities register.
 */
public final class DuctTracker {

    private static final Set<UtilityDuctBlockEntity> DUCTS = new LinkedHashSet<>();

    private DuctTracker() {
    }

    static void add(UtilityDuctBlockEntity duct) {
        DUCTS.add(duct);
    }

    static void remove(UtilityDuctBlockEntity duct) {
        DUCTS.remove(duct);
    }

    public static Set<UtilityDuctBlockEntity> ducts() {
        return Collections.unmodifiableSet(DUCTS);
    }

    /**
     * Drops ducts the level no longer holds at their position (broken, replaced or from another
     * level) and returns the rest. A duct that is broken does not always arrive here as removed, so
     * the overlay asks the level instead of trusting the set.
     */
    public static Set<UtilityDuctBlockEntity> live(Level level) {
        DUCTS.removeIf(duct -> duct.isRemoved() || duct.getLevel() != level
                || level.getBlockEntity(duct.getBlockPos()) != duct);
        return Collections.unmodifiableSet(DUCTS);
    }
}
