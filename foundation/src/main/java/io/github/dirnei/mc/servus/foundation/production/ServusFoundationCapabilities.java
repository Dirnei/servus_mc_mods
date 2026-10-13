package io.github.dirnei.mc.servus.foundation.production;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The block capabilities Servus mods use to talk to each other without a
 * compile dependency. Content mods register providers with
 * {@code RegisterCapabilitiesEvent.registerBlockEntity} like for item handlers.
 */
public final class ServusFoundationCapabilities {

    public static final BlockCapability<ProductionProvider, Direction> PRODUCTION = sided("production", ProductionProvider.class);

    public static final BlockCapability<StockProvider, Direction> STOCK_PROVIDER = sided("stock_provider", StockProvider.class);

    public static final BlockCapability<StockSink, Direction> STOCK_SINK = sided("stock_sink", StockSink.class);

    public static final BlockCapability<DataLink, Direction> DATA_LINK = sided("data_link", DataLink.class);

    public static final BlockCapability<ItemSupplier, Direction> SUPPLIER = sided("supplier", ItemSupplier.class);

    public static final BlockCapability<UpkeepHolder, Direction> UPKEEP = sided("upkeep", UpkeepHolder.class);

    public static final BlockCapability<TransferGate, Direction> TRANSFER_GATE = sided("transfer_gate", TransferGate.class);

    /**
     * The door through which data line packets enter a block. It has the same
     * type as {@code Capabilities.Item.BLOCK} but its own id, so item pipes,
     * hoppers and other mods never see it: a block that takes packets but no
     * pipes registers this instead of the item capability. A block whose items
     * the data line collects lets this handler give them out as well.
     */
    public static final BlockCapability<ResourceHandler<ItemResource>, Direction> PACKET_INLET = sided("packet_inlet", ResourceHandler.asClass());

    private ServusFoundationCapabilities() {
    }

    private static <T> BlockCapability<T, Direction> sided(String name, Class<T> type) {
        return BlockCapability.createSided(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, name), type);
    }
}
