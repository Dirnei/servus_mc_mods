package io.github.dirnei.mc.servus.alchemy.registry;

import io.github.dirnei.mc.servus.foundation.naming.MachineKind;
import io.github.dirnei.mc.servus.foundation.naming.MachineKinds;
import java.util.List;
import net.neoforged.neoforge.registries.DeferredBlock;

/** The named machine kinds of Servus: Alchemy, the producers on the data line. */
public final class ServusAlchemyMachineKinds {

    public static final MachineKind SALAMANDER_FORGE = kind(ServusAlchemyBlocks.SALAMANDER_FORGE);
    public static final MachineKind RECEPTACULUM = kind(ServusAlchemyBlocks.RECEPTACULUM);
    public static final MachineKind PRELUM = kind(ServusAlchemyBlocks.PRELUM);
    public static final MachineKind COAGULATOR = kind(ServusAlchemyBlocks.COAGULATOR);
    public static final MachineKind ATHANOR = kind(ServusAlchemyBlocks.ATHANOR);
    public static final MachineKind CALCINER = kind(ServusAlchemyBlocks.CALCINER);

    private ServusAlchemyMachineKinds() {
    }

    private static MachineKind kind(DeferredBlock<?> block) {
        return new MachineKind(block.getId(), block);
    }

    public static void register() {
        List.of(SALAMANDER_FORGE, RECEPTACULUM, PRELUM, COAGULATOR, ATHANOR, CALCINER).forEach(MachineKinds::register);
    }
}
