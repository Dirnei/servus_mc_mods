package io.github.dirnei.mc.servus.storage;

import io.github.dirnei.mc.servus.lib.production.ServusLibCapabilities;
import io.github.dirnei.mc.servus.lib.registry.ServusLibCreativeTabs;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import net.minecraft.network.chat.Component;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import io.github.dirnei.mc.servus.storage.gametest.ServusStorageGameTests;

/** Game bus hooks of Servus: Storage. */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class ServusStorageEvents {

    private ServusStorageEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(StorageStatsManager.INSTANCE);
    }

    /** Left click on a crate with items takes a stack (one when sneaking) instead of breaking. A garage with shuttles stays put. */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (ShuttleGarageBlock.occupied(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            if (!event.getLevel().isClientSide() && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
                event.getEntity().displayClientMessage(Component.translatable("message.servus_storage.garage.occupied"), true);
            }
            return;
        }
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CrateBlockEntity crate) || crate.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        if (event.getLevel().isClientSide() || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Player player = event.getEntity();
        int compartment = 0;
        if (player.pick(player.blockInteractionRange(), 1.0F, false) instanceof BlockHitResult hit && hit.getBlockPos().equals(event.getPos())) {
            compartment = CrateBlock.compartmentAt(event.getLevel().getBlockState(event.getPos()), event.getPos(), hit.getDirection(), hit.getLocation());
        }
        if (crate.isEmpty(compartment)) {
            return;
        }
        int amount = player.isShiftKeyDown() ? 1 : crate.type(compartment).getMaxStackSize();
        ItemStack taken = crate.extract(compartment, amount, false);
        if (!taken.isEmpty() && !player.getInventory().add(taken)) {
            player.drop(taken, false);
        }
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusLibCreativeTabs.SERVUS_KEY)) {
                ServusStorageItems.CREATIVE_TAB_ORDER.forEach(event::accept);
            }
        }

        @SubscribeEvent
        public static void onRegisterGameTests(RegisterGameTestsEvent event) {
            event.register(ServusStorageGameTests.class);
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusStorageBlockEntities.CRATE.get(),
                    (crate, side) -> crate.handler());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusStorageBlockEntities.HEAVY_RACK.get(),
                    (rack, side) -> rack.items());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusStorageBlockEntities.INBOUND_STATION.get(),
                    (station, side) -> station.handlerFor(side));
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusStorageBlockEntities.OUTBOUND_STATION.get(),
                    (station, side) -> station.handlerFor(side));
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusStorageBlockEntities.CRAFTING_STATION.get(),
                    (station, side) -> station.sideHandler());
            event.registerBlockEntity(ServusLibCapabilities.PRODUCTION, ServusStorageBlockEntities.CRAFTING_STATION.get(),
                    (station, side) -> station.production());
            event.registerBlockEntity(ServusLibCapabilities.STOCK_PROVIDER, ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(),
                    (controller, side) -> controller.stock());
            event.registerBlockEntity(ServusLibCapabilities.STOCK_SINK, ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(),
                    (controller, side) -> controller.sink());
        }
    }
}
