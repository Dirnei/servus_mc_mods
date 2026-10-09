package io.github.dirnei.mc.servus.manufacturing;

import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.gametest.AlchemyGameTests;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
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
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Game bus hooks of Servus: Manufacturing. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class ServusManufacturingEvents {

    private ServusManufacturingEvents() {
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
                for (Principle principle : Principle.values()) {
                    event.accept(PrincipleFluids.entry(principle).bucket().get());
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterGameTests(RegisterGameTestsEvent event) {
            event.register(AlchemyGameTests.class);
        }

        @SubscribeEvent
        public static void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(Techs.REGISTRY, TechDefinition.CODEC, TechDefinition.CODEC);
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.RESEARCH_LAB.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ServusManufacturingBlockEntities.RESEARCH_LAB.get(),
                    (machine, side) -> machine.energyStorage());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.CALCINER.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ServusManufacturingBlockEntities.CALCINER.get(),
                    (machine, side) -> machine.energyStorage());
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ServusManufacturingBlockEntities.CALCINER.get(),
                    (machine, side) -> machine.fluidHandler(side));
        }
    }
}
