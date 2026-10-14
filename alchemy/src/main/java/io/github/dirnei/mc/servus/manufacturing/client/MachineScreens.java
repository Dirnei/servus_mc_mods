package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen.Layout;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.resources.Identifier;

/** Screen layouts of Manufacturing's machines on the shared {@link MachineScreen}. */
public final class MachineScreens {

    private MachineScreens() {
    }

    static Identifier gui(String name) {
        return Layout.gui(ServusManufacturingIds.MOD_ID, name);
    }
}
