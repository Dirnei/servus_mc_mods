package io.github.dirnei.mc.servus.manufacturing.network;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.research.ClientResearch;
import io.github.dirnei.mc.servus.manufacturing.research.ResearchManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the custom payloads of Servus: Manufacturing. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class ServusManufacturingNetwork {

    private ServusManufacturingNetwork() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ResearchSyncPayload.TYPE, ResearchSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientResearch.update(payload));
        registrar.playToServer(SelectResearchPayload.TYPE, SelectResearchPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        ResearchManager.start(player, payload.tech());
                    }
                });
    }
}
