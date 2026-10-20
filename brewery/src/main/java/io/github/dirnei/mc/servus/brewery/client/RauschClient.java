package io.github.dirnei.mc.servus.brewery.client;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.config.ServusBreweryClientConfig;
import io.github.dirnei.mc.servus.brewery.rausch.RauschTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The screen wobble switch: with {@link ServusBreweryClientConfig#screenWobble()} off, the Rausch's Nausea is dropped
 * from the local player on this client only, so nothing reads it for the wobble while the server keeps the rest.
 */
@EventBusSubscriber(modid = ServusBreweryIds.MOD_ID, value = Dist.CLIENT)
public final class RauschClient {

    private RauschClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || ServusBreweryClientConfig.screenWobble()) {
            return;
        }
        MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
        if (nausea != null && RauschTracker.isRauschNausea(nausea)) {
            player.removeEffectNoUpdate(MobEffects.NAUSEA);
        }
    }
}
