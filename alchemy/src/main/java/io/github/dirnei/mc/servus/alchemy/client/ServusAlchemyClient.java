package io.github.dirnei.mc.servus.alchemy.client;

import com.klikli_dev.modonomicon.client.render.page.PageRendererRegistry;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.client.gnome.GnomeModel;
import io.github.dirnei.mc.servus.alchemy.client.gnome.GnomeRenderer;
import io.github.dirnei.mc.servus.alchemy.client.hollow.VeinColorTint;
import io.github.dirnei.mc.servus.alchemy.client.journal.CalcinationTablePageRenderer;
import io.github.dirnei.mc.servus.alchemy.client.journal.JournalTasksPageRenderer;
import io.github.dirnei.mc.servus.alchemy.client.salamander.SalamanderModel;
import io.github.dirnei.mc.servus.alchemy.client.salamander.SalamanderRenderer;
import io.github.dirnei.mc.servus.alchemy.client.sight.HollowRevealRenderer;
import io.github.dirnei.mc.servus.alchemy.client.sight.RevealRenderers;
import io.github.dirnei.mc.servus.alchemy.gnome.Gnome;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTablePage;
import io.github.dirnei.mc.servus.alchemy.journal.JournalTasksPage;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMenus;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

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
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.VENTER_EQUINUS.get(), VenterRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.COAGULATOR.get(), CoagulatorRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.ATHANOR.get(), AthanorRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.VAS_HERMETICUM.get(), VasRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.PRELUM.get(), PrelumRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.PRINCIPLE_DROPPER.get(), PrincipleDropperRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.GROWN_VAT.get(), GrownVatRenderer::new);
        event.registerBlockEntityRenderer(ServusAlchemyBlockEntities.RUBEDO_OBELISK.get(), RubedoObeliskRenderer::new);
        event.registerEntityRenderer(ServusAlchemyEntities.SALAMANDER.get(), SalamanderRenderer::new);
        event.registerEntityRenderer(ServusAlchemyEntities.GNOME.get(), GnomeRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(VeinColorTint.ID, VeinColorTint.MAP_CODEC);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SalamanderModel.LAYER, SalamanderModel::createBodyLayer);
        event.registerLayerDefinition(GnomeModel.LAYER, GnomeModel::createBodyLayer);
    }

    /**
     * The Elemental Sight renderers, the sight check that lets only the sighted click gnomes, the journal task page and
     * the calcination table.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RevealRenderers.add(new HollowRevealRenderer());
        Gnome.watchSightWith(() -> ElementalSight.active(Minecraft.getInstance().player));
        PageRendererRegistry.registerPageRenderer(JournalTasksPage.ID, page -> new JournalTasksPageRenderer((JournalTasksPage) page));
        PageRendererRegistry.registerPageRenderer(CalcinationTablePage.ID,
                page -> new CalcinationTablePageRenderer((CalcinationTablePage) page));
    }
}
