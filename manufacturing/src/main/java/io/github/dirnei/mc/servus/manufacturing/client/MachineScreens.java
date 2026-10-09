package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen.Layout;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.resources.ResourceLocation;

/** Screen layouts of Manufacturing's machines on the shared {@link MachineScreen}. */
public final class MachineScreens {

    public static final Layout LAB = new Layout(false, true, true, gui("research_lab"));

    private MachineScreens() {
    }

    static ResourceLocation gui(String name) {
        return Layout.gui(ServusManufacturingIds.MOD_ID, name);
    }
}
