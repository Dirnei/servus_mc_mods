package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.foundation.naming.MachineKind;
import io.github.dirnei.mc.servus.foundation.naming.MachineKinds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.registries.DeferredBlock;

/** The named machine kinds of Servus: Shuttles; each warehouse numbers each kind on its own. */
public final class ServusShuttlesMachineKinds {

    public static final MachineKind INBOUND_STATION = kind(ServusShuttlesBlocks.INBOUND_STATION);
    public static final MachineKind OUTBOUND_STATION = kind(ServusShuttlesBlocks.OUTBOUND_STATION);
    public static final MachineKind PACKING_STATION = kind(ServusShuttlesBlocks.PACKING_STATION);
    public static final MachineKind FIELD_HEAD = kind(ServusShuttlesBlocks.FIELD_HEAD);
    public static final MachineKind SHUTTLE_GARAGE = kind(ServusShuttlesBlocks.SHUTTLE_GARAGE);
    /** Lifts read aqua in chat, as shuttles read green. */
    public static final MachineKind ELEVATOR = new MachineKind(ServusShuttlesBlocks.ELEVATOR.getId(), ServusShuttlesBlocks.ELEVATOR,
            ChatFormatting.AQUA);
    public static final MachineKind CRAFTING_STATION = kind(ServusShuttlesBlocks.CRAFTING_STATION);
    public static final MachineKind CRAFTING_CPU = kind(ServusShuttlesBlocks.CRAFTING_CPU);

    private ServusShuttlesMachineKinds() {
    }

    private static MachineKind kind(DeferredBlock<?> block) {
        return new MachineKind(block.getId(), block);
    }

    public static void register() {
        List.of(INBOUND_STATION, OUTBOUND_STATION, PACKING_STATION, FIELD_HEAD, SHUTTLE_GARAGE, ELEVATOR, CRAFTING_STATION, CRAFTING_CPU)
                .forEach(MachineKinds::register);
    }
}
