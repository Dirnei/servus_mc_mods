package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen.Layout;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client hooks of the machines Foundation owns: screens and side marker renderers. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class MachineClient {

    public static final Layout GENERATOR = new Layout(true, false, true, Layout.gui(ServusFoundationIds.MOD_ID, "coal_generator"));

    private MachineClient() {
    }

    public static MachineScreen generator(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, GENERATOR);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MachineContent.COAL_GENERATOR_MENU.get(), MachineClient::generator);
    }

    /** Every machine draws its side modes on the block. */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MachineContent.COAL_GENERATOR_ENTITY.get(), SideMarkerRenderer::new);
    }
}
