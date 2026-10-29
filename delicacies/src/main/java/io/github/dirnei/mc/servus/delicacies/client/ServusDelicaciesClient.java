package io.github.dirnei.mc.servus.delicacies.client;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.firebox.HeaterParts;
import io.github.dirnei.mc.servus.delicacies.registry.DelicaciesFluids;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesMenus;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlock;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlockEntity;
import io.github.dirnei.mc.servus.foundation.client.ModelKeys;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import org.jspecify.annotations.Nullable;

/** Client only mod bus hooks of Servus: Delicacies. */
@EventBusSubscriber(modid = ServusDelicaciesIds.MOD_ID, value = Dist.CLIENT)
public final class ServusDelicaciesClient {

    private static final Identifier WATER_STILL = Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier WATER_FLOW = Identifier.withDefaultNamespace("block/water_flow");
    private static final int OPAQUE = 0xFF000000;

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
        event.registerBlockEntityRenderer(ServusDelicaciesBlockEntities.AQUARIUM_PART.get(), AquariumRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusDelicaciesMenus.AQUARIUM.get(), AquariumScreen::new);
    }

    /** Murky Water flows like water, tinted brown green. */
    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        int colour = OPAQUE | DelicaciesFluids.MURKY_WATER_COLOR;
        FluidTintSource tint = state -> colour;
        event.register(new FluidModel.Unbaked(new Material(WATER_STILL), new Material(WATER_FLOW), null, tint),
                DelicaciesFluids.MURKY_WATER, DelicaciesFluids.FLOWING_MURKY_WATER);
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
