package io.github.dirnei.mc.servus.alchemy.client;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen.Layout;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;

/** Screen layouts of Alchemy's machines on the shared {@link MachineScreen}. */
public final class MachineScreens {

    /** The Salamander Forge: the furnace's flame, always full, and its arrow; no energy bar. */
    public static final Layout SALAMANDER_FORGE = new Layout(true, true, false, gui("salamander_forge"));

    private MachineScreens() {
    }

    /** A shared machine screen with {@code layout}. */
    static MenuScreens.ScreenConstructor<MachineMenu, MachineScreen> machineScreen(Layout layout) {
        return (menu, inventory, title) -> new MachineScreen(menu, inventory, title, layout);
    }

    static Identifier gui(String name) {
        return Layout.gui(ServusAlchemyIds.MOD_ID, name);
    }
}
