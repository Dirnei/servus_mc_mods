package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.item.Reveal;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctTracker;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Whether the player reveals hidden things (goggles, carrot or Data Potato), for the duct
 * model: while revealing, facade panels render half transparent. Panels live in
 * the chunk mesh, so when the state flips every loaded duct with panels is
 * marked dirty once; nothing is rebuilt while the state holds.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class PanelReveal {

    /** Read by chunk builder threads. */
    private static volatile boolean revealing;

    private PanelReveal() {
    }

    public static boolean revealing() {
        return revealing;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean now = minecraft.player != null && Reveal.seesThroughPanels(minecraft.player);
        if (now == revealing) {
            return;
        }
        revealing = now;
        // Without a world there are no sections to mark; the next world builds with the current state.
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        for (UtilityDuctBlockEntity duct : DuctTracker.ducts()) {
            if (!duct.isRemoved() && duct.getLevel() == minecraft.level && duct.hasPanels()) {
                BlockPos pos = duct.getBlockPos();
                minecraft.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }
}
