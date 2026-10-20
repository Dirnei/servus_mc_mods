package io.github.dirnei.mc.servus.delicacies.client;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.firebox.HeaterParts;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlock;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlockEntity;
import io.github.dirnei.mc.servus.foundation.client.ModelKeys;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jspecify.annotations.Nullable;

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
        event.registerBlockEntityRenderer(ServusDelicaciesBlockEntities.STOVE_TILE.get(),
                context -> new GaugeRenderer<>(ServusDelicaciesClient::stoveGauge));
    }

    @SubscribeEvent
    public static void onRegisterStandaloneModels(ModelEvent.RegisterStandalone event) {
        ModelKeys.register(event, GaugeRenderer.NEEDLE);
    }

    /** The thermometer above the door of a stove with a Heating Element. */
    private static GaugeRenderer.@Nullable Reading stoveGauge(StoveTileBlockEntity tile, float partialTick) {
        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof StoveTileBlock) || !state.getValue(StoveTileBlock.FORMED) || !state.getValue(StoveTileBlock.DOOR)
                || !state.getValue(StoveTileBlock.HEATER)) {
            return null;
        }
        float[] centre = HeaterParts.dialCentre(HeaterParts.STOVE_DIAL);
        return new GaugeRenderer.Reading(state.getValue(StoveTileBlock.FACING), centre[0], centre[1], centre[2],
                tile.shownHeat(partialTick));
    }
}
