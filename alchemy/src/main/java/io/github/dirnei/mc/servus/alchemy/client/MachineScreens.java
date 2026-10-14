package io.github.dirnei.mc.servus.alchemy.client;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen.Layout;
import net.minecraft.resources.Identifier;

/** Screen layouts of Manufacturing's machines on the shared {@link MachineScreen}. */
public final class MachineScreens {

    private MachineScreens() {
    }

    static Identifier gui(String name) {
        return Layout.gui(ServusAlchemyIds.MOD_ID, name);
    }
}
