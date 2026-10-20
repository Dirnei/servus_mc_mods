package io.github.dirnei.mc.servus.brewery.client;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseBlockEntity;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseBricksBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseModel;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhousePartBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer;
import io.github.dirnei.mc.servus.brewery.registry.BreweryFluids;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.delicacies.client.GaugeRenderer;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import org.jspecify.annotations.Nullable;

/** Client looks of Servus: Brewery: the worts and beers as tinted water, and the Brewhouse thermometer's needle. */
@EventBusSubscriber(modid = ServusBreweryIds.MOD_ID, value = Dist.CLIENT)
public final class BreweryClient {

    private static final Identifier STILL = Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier FLOWING = Identifier.withDefaultNamespace("block/water_flow");
    private static final int OPAQUE = 0xFF000000;

    private BreweryClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusBreweryBlockEntities.BREWHOUSE.get(),
                context -> new GaugeRenderer<>(BreweryClient::brewhouseGauge));
    }

    /** The thermometer between the doors of a Brewhouse with a Heating Element, drawn by its brick. */
    private static GaugeRenderer.@Nullable Reading brewhouseGauge(BrewhouseBlockEntity part, float partialTick) {
        BlockState state = part.getBlockState();
        if (!(state.getBlock() instanceof BrewhouseBricksBlock) || !state.getValue(BrewhousePartBlock.FORMED)
                || !state.getValue(BrewhouseBricksBlock.HEATER)
                || state.getValue(BrewhouseBricksBlock.PART).cell() != BrewhouseBlockEntity.GAUGE_CELL) {
            return null;
        }
        float[] centre = ModelSlicer.localPoint(BrewhouseBlockEntity.GAUGE_CELL, BrewhouseModel.GAUGE_U, 0, BrewhouseModel.GAUGE_Y);
        return new GaugeRenderer.Reading(state.getValue(BrewhousePartBlock.FACING), centre[0], centre[1],
                BrewhouseModel.GAUGE_DIAL.v0(), part.shownGaugeHeat(partialTick));
    }

    /** Every wort and beer flows like water, tinted in its colour. */
    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        for (BreweryFluids.BrewFluid fluid : BreweryFluids.ALL) {
            int colour = OPAQUE | fluid.color();
            FluidTintSource tint = state -> colour;
            event.register(new FluidModel.Unbaked(new Material(STILL), new Material(FLOWING), null, tint),
                    fluid.source(), fluid.flowing());
        }
    }
}
