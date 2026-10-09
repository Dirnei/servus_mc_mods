package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen.Layout;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Screen layouts and factories of Manufacturing's machines on the shared {@link MachineScreen}. */
public final class MachineScreens {

    public static final Layout ASSEMBLER = new Layout(false, false, true, gui("assembler"));
    public static final Layout LAB = new Layout(false, true, true, gui("research_lab"));
    public static final Layout ELECTRIC = new Layout(false, true, true, gui("electric_extractor"));
    public static final Layout CRUSHER = new Layout(false, true, true, gui("crusher"));

    private MachineScreens() {
    }

    private static ResourceLocation gui(String name) {
        return Layout.gui(ServusManufacturingIds.MOD_ID, name);
    }

    public static MachineScreen electric(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, ELECTRIC);
    }

    public static MachineScreen crusher(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, CRUSHER);
    }
}
