package io.github.dirnei.mc.servus.foundation.production;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * The block capabilities Servus mods use to talk to each other without a
 * compile dependency. Content mods register providers with
 * {@code RegisterCapabilitiesEvent.registerBlockEntity} like for item handlers.
 */
public final class ServusFoundationCapabilities {

    public static final BlockCapability<ProductionProvider, Direction> PRODUCTION =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "production"), ProductionProvider.class);

    public static final BlockCapability<StockProvider, Direction> STOCK_PROVIDER =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "stock_provider"), StockProvider.class);

    public static final BlockCapability<StockSink, Direction> STOCK_SINK =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "stock_sink"), StockSink.class);

    private ServusFoundationCapabilities() {
    }
}
