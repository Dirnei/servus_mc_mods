package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen.Layout;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client hooks of the machines Foundation owns: screens and side marker renderers. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class MachineClient {

    /** The Coal Generator: fuel, flame and energy bar with the side panel, and a status line. */
    public static final Layout GENERATOR = new Layout(true, false, true, false, true, true, gui("coal_generator"));
    public static final Layout ANCHOR = new Layout(false, false, true, gui("ender_anchor"));
    /** Pumps: a fluid gauge and the water source status line, no side panel. */
    public static final Layout BURNER_PUMP = new Layout(true, true, false, true, false, gui("burner_pump"));
    public static final Layout ELECTRIC_PUMP = new Layout(false, true, true, true, false, gui("electric_pump"));
    /** The Mill: input, arrow and output with the side panel; no flame, no energy bar. */
    public static final Layout MILL = new Layout(false, true, false, gui("mill"));
    /** The Electric Motor: its upgrade slot and the energy bar, no side panel. */
    public static final Layout ELECTRIC_MOTOR = new Layout(false, false, true, false, false, gui("electric_motor"));

    private MachineClient() {
    }

    private static Identifier gui(String name) {
        return Layout.gui(ServusFoundationIds.MOD_ID, name);
    }

    private static MenuScreens.ScreenConstructor<MachineMenu, MachineScreen> machineScreen(Layout layout) {
        return (menu, inventory, title) -> new MachineScreen(menu, inventory, title, layout);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MachineContent.COAL_GENERATOR_MENU.get(), machineScreen(GENERATOR));
        event.register(MachineContent.BURNER_PUMP_MENU.get(), machineScreen(BURNER_PUMP));
        event.register(MachineContent.ELECTRIC_PUMP_MENU.get(), machineScreen(ELECTRIC_PUMP));
        event.register(MachineContent.SINK_MENU.get(), SinkScreen::new);
        event.register(MachineContent.MILL_MENU.get(), machineScreen(MILL));
        event.register(MachineContent.ELECTRIC_MOTOR_MENU.get(), machineScreen(ELECTRIC_MOTOR));
        event.register(MachineContent.ENDER_ANCHOR_MENU.get(), EnderAnchorScreen::new);
    }

    /** The Sink fan and the moving parts of the beam pumps, the Mill, the crank and the motor are standalone models drawn by block entity renderers. */
    @SubscribeEvent
    public static void onRegisterStandaloneModels(ModelEvent.RegisterStandalone event) {
        ModelKeys.register(event, SinkRenderer.FAN_MODEL);
        ModelKeys.register(event, BurnerPumpRenderer.BEAM_MODEL);
        ModelKeys.register(event, BurnerPumpRenderer.ROD_MODEL);
        ModelKeys.register(event, BurnerPumpRenderer.PISTON_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.BEAM_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.ROD_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.CON_ROD_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.WHEEL_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.SPOKE_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.FAN_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.RIM_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.AXLE_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.MOTOR_PULLEY_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.BELT_MODEL);
        ModelKeys.register(event, ElectricPumpRenderer.BELT_MARK_MODEL);
        ModelKeys.register(event, MillRenderer.HUB_MODEL);
        ModelKeys.register(event, MillRenderer.SPOKE_MODEL);
        ModelKeys.register(event, MillRenderer.RIM_MODEL);
        ModelKeys.register(event, HandCrankRenderer.HANDLE_MODEL);
        ModelKeys.register(event, ElectricMotorRenderer.COUPLING_MODEL);
        ModelKeys.register(event, ElectricMotorRenderer.FAN_MODEL);
    }

    /** Every machine draws its side modes on the block. */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MachineContent.COAL_GENERATOR_ENTITY.get(), SideMarkerRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.BURNER_PUMP_ENTITY.get(), BurnerPumpRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.ELECTRIC_PUMP_ENTITY.get(), ElectricPumpRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.SINK_ENTITY.get(), SinkRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.ENDER_ANCHOR_ENTITY.get(), EnderAnchorRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.MILL_ENTITY.get(), MillRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.HAND_CRANK_ENTITY.get(), HandCrankRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.ELECTRIC_MOTOR_ENTITY.get(), ElectricMotorRenderer::new);
    }
}
