package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctTracker;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Marks the loaded ducts on recoloured channels dirty, so their rings are tinted again. */
final class ChannelRetint {

    private ChannelRetint() {
    }

    static void retint(Set<Integer> ids) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        for (UtilityDuctBlockEntity duct : DuctTracker.live(minecraft.level)) {
            if (duct.storesChannel(ids)) {
                BlockPos pos = duct.getBlockPos();
                minecraft.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }
}
