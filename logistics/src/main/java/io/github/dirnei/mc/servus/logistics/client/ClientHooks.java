package io.github.dirnei.mc.servus.logistics.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client only entry points called from common code behind an {@code isClientSide} check. */
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openDuctScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new UtilityDuctScreen(pos));
    }
}
