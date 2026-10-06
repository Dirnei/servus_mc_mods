package io.github.dirnei.mc.servus.storage.network;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Block state properties shared by the stock holding storage blocks. */
public final class StorageNodeBlocks {

    /** Lit while the block belongs to an online warehouse. Set by the network, never by the player. */
    public static final BooleanProperty ONLINE = BooleanProperty.create("online");

    private StorageNodeBlocks() {
    }
}
