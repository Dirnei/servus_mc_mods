package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the Foundation payloads. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class ServusFoundationPayloads {

    private ServusFoundationPayloads() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(DuctLoadPayload.TYPE, DuctLoadPayload.STREAM_CODEC,
                (payload, context) -> ClientDuctLoads.update(payload));
    }
}
