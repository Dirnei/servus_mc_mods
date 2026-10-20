package io.github.dirnei.mc.servus.delicacies.client;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client only mod bus hooks of Servus: Delicacies. */
@EventBusSubscriber(modid = ServusDelicaciesIds.MOD_ID, value = Dist.CLIENT)
public final class ServusDelicaciesClient {

    private ServusDelicaciesClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusDelicaciesBlockEntities.POT.get(), VesselRenderer::new);
        event.registerBlockEntityRenderer(ServusDelicaciesBlockEntities.PAN.get(), VesselRenderer::new);
        event.registerBlockEntityRenderer(ServusDelicaciesBlockEntities.CUTTING_BOARD.get(), CuttingBoardRenderer::new);
        event.registerBlockEntityRenderer(ServusDelicaciesBlockEntities.FERMENTING_CROCK.get(), CrockRenderer::new);
    }
}
