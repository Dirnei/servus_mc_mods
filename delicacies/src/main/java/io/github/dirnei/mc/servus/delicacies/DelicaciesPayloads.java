package io.github.dirnei.mc.servus.delicacies;

import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumMenu;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumSettingsPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the Delicacies payloads. */
@EventBusSubscriber(modid = ServusDelicaciesIds.MOD_ID)
public final class DelicaciesPayloads {

    private DelicaciesPayloads() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AquariumSettingsPayload.TYPE, AquariumSettingsPayload.STREAM_CODEC,
                (payload, context) -> AquariumMenu.applySettings(context.player(), payload));
    }
}
