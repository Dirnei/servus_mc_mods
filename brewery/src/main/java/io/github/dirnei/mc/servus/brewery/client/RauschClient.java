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
 * The screen wobble switch: with {@link ServusBreweryClientConfig#screenWobble()} off, the blend of the Rausch's Nausea
 * on the local player is held at zero, so the wobble and its overlay stay away while the effect itself stays. Switched
 * back on, the wobble fades in as for a new Nausea.
 */
@EventBusSubscriber(modid = ServusBreweryIds.MOD_ID, value = Dist.CLIENT)
public final class RauschClient {

    private static final MobEffectInstance UNBLENDED = new MobEffectInstance(MobEffects.NAUSEA);

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
            nausea.copyBlendState(UNBLENDED);
        }
    }
}
