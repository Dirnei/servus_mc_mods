package io.github.dirnei.mc.servus.manufacturing;

import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import io.github.dirnei.mc.servus.manufacturing.deposit.DepositManager;
import io.github.dirnei.mc.servus.manufacturing.gametest.NativeUpkeepGameTests;
import io.github.dirnei.mc.servus.manufacturing.gametest.ServusManufacturingGameTests;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import io.github.dirnei.mc.servus.manufacturing.research.ResearchManager;
import io.github.dirnei.mc.servus.manufacturing.research.TechDefinition;
import io.github.dirnei.mc.servus.manufacturing.research.Techs;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Game bus hooks of Servus: Manufacturing. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class ServusManufacturingEvents {

    private ServusManufacturingEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(DepositManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchManager.syncPlayer(player);
        }
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusFoundationCreativeTabs.SERVUS_KEY)) {
                ServusManufacturingItems.CREATIVE_TAB_ORDER.forEach(event::accept);
            }
        }

        @SubscribeEvent
        public static void onRegisterGameTests(RegisterGameTestsEvent event) {
            event.register(ServusManufacturingGameTests.class);
            event.register(NativeUpkeepGameTests.class);
        }

        @SubscribeEvent
        public static void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(Techs.REGISTRY, TechDefinition.CODEC, TechDefinition.CODEC);
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.BURNER_EXTRACTOR.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.BURNER_SMELTER.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.ASSEMBLER.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ServusManufacturingBlockEntities.ASSEMBLER.get(),
                    (machine, side) -> machine.energyStorage());
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusManufacturingBlockEntities.ASSEMBLER.get(),
                    (machine, side) -> machine.production());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.ELECTRIC_EXTRACTOR.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ServusManufacturingBlockEntities.ELECTRIC_EXTRACTOR.get(),
                    (machine, side) -> machine.energyStorage());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.CRUSHER.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ServusManufacturingBlockEntities.CRUSHER.get(),
                    (machine, side) -> machine.energyStorage());
            // Burner machines keep their fuel slot stocked from the data line; smelter and crusher run order steps.
            event.registerBlockEntity(ServusFoundationCapabilities.UPKEEP, ServusManufacturingBlockEntities.BURNER_EXTRACTOR.get(),
                    (machine, side) -> machine.upkeep());
            event.registerBlockEntity(ServusFoundationCapabilities.UPKEEP, ServusManufacturingBlockEntities.BURNER_SMELTER.get(),
                    (machine, side) -> machine.upkeep());
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusManufacturingBlockEntities.BURNER_SMELTER.get(),
                    (machine, side) -> machine.production());
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusManufacturingBlockEntities.CRUSHER.get(),
                    (machine, side) -> machine.production());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.RESEARCH_LAB.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ServusManufacturingBlockEntities.RESEARCH_LAB.get(),
                    (machine, side) -> machine.energyStorage());
        }
    }
}
