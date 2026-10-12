package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Where the data line puts items into a block and takes them out: the block's
 * packet inlet on that face, else its item capability. Server thread only.
 */
public final class PacketInlets {

    private PacketInlets() {
    }

    /** The packet inlet on that face, else the item capability, or null when the block has neither or is not loaded. */
    public static @Nullable ItemView handler(Level level, BlockPos pos, @Nullable Direction side) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        ResourceHandler<ItemResource> inlet = level.getCapability(ServusFoundationCapabilities.PACKET_INLET, pos, side);
        if (inlet != null) {
            return ItemView.of(inlet);
        }
        return ItemView.of(level.getCapability(Capabilities.Item.BLOCK, pos, side));
    }
}
