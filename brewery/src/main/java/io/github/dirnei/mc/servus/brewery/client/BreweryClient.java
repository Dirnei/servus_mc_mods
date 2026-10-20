package io.github.dirnei.mc.servus.brewery.client;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.BreweryFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

/** Client looks of Servus: Brewery: the worts as tinted water. */
@EventBusSubscriber(modid = ServusBreweryIds.MOD_ID, value = Dist.CLIENT)
public final class BreweryClient {

    private static final Identifier STILL = Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier FLOWING = Identifier.withDefaultNamespace("block/water_flow");
    private static final int OPAQUE = 0xFF000000;

    private BreweryClient() {
    }

    /** Every wort flows like water, tinted in its colour. */
    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        for (BreweryFluids.Wort wort : BreweryFluids.WORTS) {
            int colour = OPAQUE | wort.color();
            FluidTintSource tint = state -> colour;
            event.register(new FluidModel.Unbaked(new Material(STILL), new Material(FLOWING), null, tint),
                    wort.source(), wort.flowing());
        }
    }
}
