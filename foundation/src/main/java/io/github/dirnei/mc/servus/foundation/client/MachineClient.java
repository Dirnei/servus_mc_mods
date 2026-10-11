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
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client hooks of the machines Foundation owns: screens and side marker renderers. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class MachineClient {

    public static final Layout GENERATOR = new Layout(true, false, true, Layout.gui(ServusFoundationIds.MOD_ID, "coal_generator"));
    public static final Layout LOADER = new Layout(false, false, true, Layout.gui(ServusFoundationIds.MOD_ID, "chunk_loader"));
    /** Pumps: a fluid gauge and the water source status line, no side panel. */
    public static final Layout BURNER_PUMP = new Layout(true, true, false, true, false, Layout.gui(ServusFoundationIds.MOD_ID, "burner_pump"));
    public static final Layout ELECTRIC_PUMP = new Layout(false, true, true, true, false, Layout.gui(ServusFoundationIds.MOD_ID, "electric_pump"));

    private MachineClient() {
    }

    public static MachineScreen generator(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, GENERATOR);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MachineContent.COAL_GENERATOR_MENU.get(), MachineClient::generator);
        event.register(MachineContent.BURNER_PUMP_MENU.get(),
                (MachineMenu menu, Inventory inventory, Component title) -> new MachineScreen(menu, inventory, title, BURNER_PUMP));
        event.register(MachineContent.ELECTRIC_PUMP_MENU.get(),
                (MachineMenu menu, Inventory inventory, Component title) -> new MachineScreen(menu, inventory, title, ELECTRIC_PUMP));
        event.register(MachineContent.SINK_MENU.get(), SinkScreen::new);
        event.register(MachineContent.CHUNK_LOADER_MENU.get(), ChunkLoaderScreen::new);
    }

    /** The Sink fan and the Burner Pump beam and rods are standalone models drawn by block entity renderers. */
    @SubscribeEvent
    public static void onRegisterStandaloneModels(ModelEvent.RegisterStandalone event) {
        ModelKeys.register(event, SinkRenderer.FAN_MODEL, "block/sink_fan");
        ModelKeys.register(event, BurnerPumpRenderer.BEAM_MODEL, "block/burner_pump_beam");
        ModelKeys.register(event, BurnerPumpRenderer.ROD_MODEL, "block/burner_pump_rod");
        ModelKeys.register(event, BurnerPumpRenderer.PISTON_MODEL, "block/burner_pump_piston");
    }

    /** Every machine draws its side modes on the block. */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MachineContent.COAL_GENERATOR_ENTITY.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.BURNER_PUMP_ENTITY.get(), BurnerPumpRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.ELECTRIC_PUMP_ENTITY.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.SINK_ENTITY.get(), SinkRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.CHUNK_LOADER_ENTITY.get(), SideMarkerRenderer::new);
    }
}
