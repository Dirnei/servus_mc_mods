package io.github.dirnei.mc.servus.foundation.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

/** Every registered named machine kind. Each mod registers its kinds while it is constructed. */
public final class MachineKinds {

    private static final Map<Identifier, MachineKind> KINDS = new ConcurrentHashMap<>();

    private MachineKinds() {
    }

    /** Registers the kind and returns it; a second kind with the same id is refused. */
    public static MachineKind register(MachineKind kind) {
        if (KINDS.putIfAbsent(kind.id(), kind) != null) {
            throw new IllegalStateException("Machine kind " + kind.id() + " is registered twice");
        }
        return kind;
    }

    /** Every registered kind, ordered by id. */
    public static List<MachineKind> all() {
        List<MachineKind> kinds = new ArrayList<>(KINDS.values());
        kinds.sort((a, b) -> a.id().compareTo(b.id()));
        return kinds;
    }
}
