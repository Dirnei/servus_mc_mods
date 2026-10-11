package io.github.dirnei.mc.servus.foundation.production;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * The block capabilities Servus mods use to talk to each other without a
 * compile dependency. Content mods register providers with
 * {@code RegisterCapabilitiesEvent.registerBlockEntity} like for item handlers.
 */
public final class ServusFoundationCapabilities {

    public static final BlockCapability<ProductionProvider, Direction> PRODUCTION =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "production"), ProductionProvider.class);

    public static final BlockCapability<StockProvider, Direction> STOCK_PROVIDER =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "stock_provider"), StockProvider.class);

    public static final BlockCapability<StockSink, Direction> STOCK_SINK =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "stock_sink"), StockSink.class);

    public static final BlockCapability<DataLink, Direction> DATA_LINK =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "data_link"), DataLink.class);

    public static final BlockCapability<ItemSupplier, Direction> SUPPLIER =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "supplier"), ItemSupplier.class);

    public static final BlockCapability<UpkeepHolder, Direction> UPKEEP =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "upkeep"), UpkeepHolder.class);

    public static final BlockCapability<TransferGate, Direction> TRANSFER_GATE =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "transfer_gate"), TransferGate.class);

    private ServusFoundationCapabilities() {
    }
}
