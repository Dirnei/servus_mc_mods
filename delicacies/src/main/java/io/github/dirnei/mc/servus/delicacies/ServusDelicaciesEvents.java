package io.github.dirnei.mc.servus.delicacies;

import io.github.dirnei.mc.servus.delicacies.firebox.DelicaciesCapabilities;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenStatsManager;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/** Capabilities and reload listeners of Servus: Delicacies. */
@EventBusSubscriber(modid = ServusDelicaciesIds.MOD_ID)
public final class ServusDelicaciesEvents {

    private ServusDelicaciesEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(KitchenStatsManager.LISTENER_ID, KitchenStatsManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(DelicaciesCapabilities.FIREBOX, ServusDelicaciesBlockEntities.STOVE_TILE.get(),
                (tile, context) -> tile.firebox());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusDelicaciesBlockEntities.STOVE_TILE.get(),
                (tile, side) -> tile.fuelCapability());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusDelicaciesBlockEntities.POT.get(),
                (pot, side) -> pot.itemCapability());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusDelicaciesBlockEntities.POT.get(),
                (pot, side) -> pot.fluidCapability());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusDelicaciesBlockEntities.PAN.get(),
                (pan, side) -> pan.itemCapability());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusDelicaciesBlockEntities.BAKING_OVEN.get(),
                (oven, side) -> oven.itemCapability());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusDelicaciesBlockEntities.CUTTING_BOARD.get(),
                (board, side) -> board.itemCapability());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ServusDelicaciesBlockEntities.FERMENTING_CROCK.get(),
                (crock, side) -> crock.itemCapability());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusDelicaciesBlockEntities.FERMENTING_CROCK.get(),
                (crock, side) -> crock.fluidCapability());
    }
}
