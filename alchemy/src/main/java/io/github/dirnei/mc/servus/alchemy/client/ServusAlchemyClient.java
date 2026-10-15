package io.github.dirnei.mc.servus.alchemy.client;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.client.salamander.SalamanderModel;
import io.github.dirnei.mc.servus.alchemy.client.salamander.SalamanderRenderer;
import io.github.dirnei.mc.servus.alchemy.client.sight.HollowRevealRenderer;
import io.github.dirnei.mc.servus.alchemy.client.sight.OldGnomeRevealRenderer;
import io.github.dirnei.mc.servus.alchemy.client.sight.RevealRenderers;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMenus;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

/** Client only mod bus hooks. */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID, value = Dist.CLIENT)
public final class ServusAlchemyClient {

    private ServusAlchemyClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusAlchemyMenus.COAGULATOR.get(), CoagulatorScreen::new);
        event.register(ServusAlchemyMenus.SALAMANDER_FORGE.get(), MachineScreens.machineScreen(MachineScreens.SALAMANDER_FORGE));
        event.register(ServusAlchemyMenus.PRELUM.get(), MachineScreens.machineScreen(MachineScreens.PRELUM));
        event.register(ServusAlchemyMenus.RECEPTACULUM.get(), (MachineMenu menu, Inventory inventory, Component title) -> new ReceptaculumScreen(menu, inventory, title));
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.CALCINER.get(), CalcinerRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.PURIFIER.get(), PurifierRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.COAGULATOR.get(), CoagulatorRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.ATHANOR.get(), AthanorRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.VAS_HERMETICUM.get(), VasRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.PRELUM.get(), PrelumRenderer::new);
        event.registerEntityRenderer(ServusAlchemyEntities.SALAMANDER.get(), SalamanderRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SalamanderModel.LAYER, SalamanderModel::createBodyLayer);
    }

    /** Dev only icon export for the docs: its renderer exists only when the iconExport run asks for it. */
    @SubscribeEvent
    public static void onRegisterPictureInPicture(RegisterPictureInPictureRenderersEvent event) {
        IconExporter.registerRendererIfRequested(event);
    }

    /** The dev only icon export, when the iconExport run asks for it, and the Elemental Sight renderers. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        IconExporter.registerIfRequested();
        RevealRenderers.add(new HollowRevealRenderer());
        RevealRenderers.add(new OldGnomeRevealRenderer());
    }
}
