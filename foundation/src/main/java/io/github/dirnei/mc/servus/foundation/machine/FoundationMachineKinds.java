package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.naming.MachineKind;
import io.github.dirnei.mc.servus.foundation.naming.MachineKinds;
import java.util.List;
import net.neoforged.neoforge.registries.DeferredBlock;

/** The named machine kinds of Foundation's machines that sit on a data line. */
public final class FoundationMachineKinds {

    public static final MachineKind COAL_GENERATOR = kind(MachineContent.COAL_GENERATOR);
    public static final MachineKind BURNER_PUMP = kind(MachineContent.BURNER_PUMP);

    private FoundationMachineKinds() {
    }

    private static MachineKind kind(DeferredBlock<?> block) {
        return new MachineKind(block.getId(), block);
    }

    static void register() {
        List.of(COAL_GENERATOR, BURNER_PUMP).forEach(MachineKinds::register);
    }
}
