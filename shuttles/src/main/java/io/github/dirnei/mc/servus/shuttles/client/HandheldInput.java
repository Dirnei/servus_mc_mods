package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Sneak and scroll with the handheld in the main hand switches its mode instead of the hotbar slot. */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class HandheldInput {

    private HandheldInput() {
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.screen != null || player == null || !player.isShiftKeyDown()
                || !(player.getMainHandItem().getItem() instanceof HandheldItem) || event.getScrollDeltaY() == 0) {
            return;
        }
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new HandheldModePayload(event.getScrollDeltaY() < 0 ? 1 : -1));
        ClientHandheld.clicked();
    }
}
