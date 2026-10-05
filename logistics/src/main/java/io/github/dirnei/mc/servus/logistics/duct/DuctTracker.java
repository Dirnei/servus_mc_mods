package io.github.dirnei.mc.servus.logistics.duct;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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
}
