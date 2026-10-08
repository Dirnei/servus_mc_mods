package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import net.minecraft.world.inventory.ContainerData;

/** Six menu data slots that mirror a side configuration to the client. */
public final class SideConfigData implements ContainerData {

    private final SideConfig config;

    public SideConfigData(SideConfig config) {
        this.config = config;
    }

    @Override
    public int get(int index) {
        return config.mode(RelativeSide.byOrdinal(index)).ordinal();
    }

    @Override
    public void set(int index, int value) {
        config.set(RelativeSide.byOrdinal(index), SideMode.byOrdinal(value));
    }

    @Override
    public int getCount() {
        return RelativeSide.values().length;
    }
}
