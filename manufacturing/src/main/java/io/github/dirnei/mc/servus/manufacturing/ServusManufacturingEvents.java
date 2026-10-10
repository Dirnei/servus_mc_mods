package io.github.dirnei.mc.servus.manufacturing;

import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPorts;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorStructure;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierStructure;
import io.github.dirnei.mc.servus.manufacturing.gametest.AlchemyGameTests;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.gametest.CalcinerGameTests;
import io.github.dirnei.mc.servus.manufacturing.gametest.PurifierGameTests;
import io.github.dirnei.mc.servus.manufacturing.gametest.CoagulatorGameTests;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Event hooks of Servus: Manufacturing. */
public final class ServusManufacturingEvents {

    private ServusManufacturingEvents() {
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
                event.accept(ElementalSight.ELIXIR.get());
                for (Principle principle : Principle.values()) {
                    event.accept(PrincipleFluids.entry(principle).bucket().get());
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterGameTests(RegisterGameTestsEvent event) {
            event.register(AlchemyGameTests.class);
            event.register(CalcinerGameTests.class);
            event.register(PurifierGameTests.class);
            event.register(CoagulatorGameTests.class);
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            // The Purifier build: Ash and water into the Vat, the Salt out of the Catch Basin, the residue out of the Residue Crock.
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.PURIFIER.get(),
                    (purifier, side) -> purifier.formed() ? purifier.itemPort() : null);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ServusManufacturingBlockEntities.PURIFIER.get(),
                    (purifier, side) -> purifier.formed() ? purifier.fluidPort() : null);
            event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, entity, side) -> PurifierStructure.basinHandler(level, pos, state),
                    ServusManufacturingBlocks.CATCH_BASIN.get());
            event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, entity, side) -> PurifierStructure.crockHandler(level, pos, state),
                    ServusManufacturingBlocks.RESIDUE_CROCK.get());
            // The Calciner structure has fixed ports by position instead of sides.
            event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, entity, side) -> CalcinerPorts.items(level, pos, state),
                    ServusManufacturingBlocks.CALCINER.get(), ServusManufacturingBlocks.FIREBOX.get(), ServusManufacturingBlocks.ASH_PAN.get());
            event.registerBlock(ServusFoundationCapabilities.UPKEEP, (level, pos, state, entity, side) -> CalcinerPorts.upkeep(level, pos, state),
                    ServusManufacturingBlocks.FIREBOX.get());
            event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, entity, side) -> CalcinerPorts.fluid(level, pos, state),
                    ServusManufacturingBlocks.RECEIVER.get(), ServusManufacturingBlocks.COPPER_DUCT.get());
            // The Coagulator build: items at the casting table while formed, each principle into its Vat.
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.COAGULATOR.get(),
                    (machine, side) -> machine.formed() ? machine.itemPort() : null);
            event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, entity, side) -> CoagulatorStructure.vatHandler(level, pos, state),
                    ServusManufacturingBlocks.VAT.get());
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusManufacturingBlockEntities.COAGULATOR.get(),
                    (machine, side) -> machine.production());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.ATHANOR.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusManufacturingBlockEntities.ATHANOR.get(),
                    (machine, side) -> machine.production());
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ServusManufacturingBlockEntities.GNOME_HOLLOW.get(),
                    (machine, side) -> machine.sideHandler(side));
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ServusManufacturingBlockEntities.GNOME_HOLLOW.get(),
                    (machine, side) -> machine.fluidHandler(side));
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusManufacturingBlockEntities.GNOME_HOLLOW.get(),
                    (machine, side) -> machine.production());
        }
    }
}
