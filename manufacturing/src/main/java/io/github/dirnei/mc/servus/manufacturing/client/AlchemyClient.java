package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

/** Client looks of the principles: tinted water textures and tinted buckets. */
@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID, value = Dist.CLIENT)
public final class AlchemyClient {

    private static final Identifier STILL = Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier FLOWING = Identifier.withDefaultNamespace("block/water_flow");

    private AlchemyClient() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (Principle principle : Principle.values()) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public Identifier getStillTexture() {
                    return STILL;
                }

                @Override
                public Identifier getFlowingTexture() {
                    return FLOWING;
                }

                @Override
                public int getTintColor() {
                    return principle.color();
                }
            }, PrincipleFluids.entry(principle).type().get());
        }
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        for (Principle principle : Principle.values()) {
            event.register(new DynamicFluidContainerModel.Colors(), PrincipleFluids.entry(principle).bucket().get());
        }
    }
}
