package io.github.dirnei.mc.servus.alchemy;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerPorts;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorStructure;
import io.github.dirnei.mc.servus.alchemy.hollow.ClientVeins;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinSeedItem;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierStructure;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Event hooks of Servus: Alchemy. */
public final class ServusAlchemyEvents {

    private ServusAlchemyEvents() {
    }

    /** Mod bus hooks. */
    @EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(ServusFoundationCreativeTabs.SERVUS_KEY)) {
                ServusAlchemyItems.CREATIVE_TAB_ORDER.forEach(event::accept);
                ClientVeins.ids().forEach(vein -> event.accept(VeinSeedItem.of(vein)));
                event.accept(ElementalSight.ELIXIR.get());
                for (Principle principle : Principle.values()) {
                    event.accept(PrincipleFluids.entry(principle).bucket().get());
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            registerPurifier(event);
            registerCalciner(event);
            registerCoagulator(event);
            registerAthanor(event);
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusAlchemyBlockEntities.SALAMANDER_FORGE.get(),
                    (forge, side) -> forge.itemCapability(side));
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusAlchemyBlockEntities.SALAMANDER_FORGE.get(),
                    (forge, side) -> forge.production());
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusAlchemyBlockEntities.RECEPTACULUM.get(),
                    (receptaculum, side) -> receptaculum.itemCapability(side));
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusAlchemyBlockEntities.RECEPTACULUM.get(),
                    (receptaculum, side) -> receptaculum.production());
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusAlchemyBlockEntities.PRELUM.get(),
                    (prelum, side) -> prelum.itemCapability(side));
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusAlchemyBlockEntities.PRELUM.get(),
                    (prelum, side) -> prelum.production());
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusAlchemyBlockEntities.PRINCIPLE_DROPPER.get(),
                    (dropper, side) -> dropper.tanks());
        }

        /** Ash and water go into the Vat, the Salt comes out of the Catch Basin, the residue out of the Residue Crock. */
        private static void registerPurifier(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusAlchemyBlockEntities.PURIFIER.get(),
                    (purifier, side) -> purifier.formed() ? purifier.itemPort() : null);
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, ServusAlchemyBlockEntities.PURIFIER.get(),
                    (purifier, side) -> purifier.formed() ? purifier.fluidPort() : null);
            event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, entity, side) -> PurifierStructure.basinHandler(level, pos, state),
                    ServusAlchemyBlocks.CATCH_BASIN.get());
            event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, entity, side) -> PurifierStructure.crockHandler(level, pos, state),
                    ServusAlchemyBlocks.RESIDUE_CROCK.get());
        }

        /** The Calciner structure has fixed ports by position, not by side. */
        private static void registerCalciner(RegisterCapabilitiesEvent event) {
            event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, entity, side) -> CalcinerPorts.items(level, pos, state),
                    ServusAlchemyBlocks.CALCINER.get(), ServusAlchemyBlocks.FIREBOX.get(), ServusAlchemyBlocks.ASH_PAN.get());
            event.registerBlock(ServusFoundationCapabilities.UPKEEP, (level, pos, state, entity, side) -> CalcinerPorts.upkeep(level, pos, state),
                    ServusAlchemyBlocks.FIREBOX.get());
        }

        /** Items at the casting table while formed, each principle into its Vat. */
        private static void registerCoagulator(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusAlchemyBlockEntities.COAGULATOR.get(),
                    (machine, side) -> machine.formed() ? machine.itemPort() : null);
            event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, entity, side) -> CoagulatorStructure.vatHandler(level, pos, state),
                    ServusAlchemyBlocks.VAT.get());
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusAlchemyBlockEntities.COAGULATOR.get(),
                    (machine, side) -> machine.production());
        }

        private static void registerAthanor(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, ServusAlchemyBlockEntities.ATHANOR.get(),
                    (machine, side) -> machine.itemCapability(side));
            event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, ServusAlchemyBlockEntities.ATHANOR.get(),
                    (machine, side) -> machine.production());
        }
    }
}
