package io.github.dirnei.mc.servus.alchemy.client;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

/** Client looks of the principles: tinted water textures. */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID, value = Dist.CLIENT)
public final class AlchemyClient {

    private static final Identifier STILL = Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier FLOWING = Identifier.withDefaultNamespace("block/water_flow");

    private AlchemyClient() {
    }

    /** Every principle flows like water, tinted in its colour. */
    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        for (Principle principle : Principle.values()) {
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            int colour = principle.color();
            FluidTintSource tint = state -> colour;
            event.register(new FluidModel.Unbaked(new Material(STILL), new Material(FLOWING), null, tint),
                    entry.source(), entry.flowing());
        }
    }
}
