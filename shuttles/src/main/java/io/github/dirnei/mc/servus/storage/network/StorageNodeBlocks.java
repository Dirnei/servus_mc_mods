package io.github.dirnei.mc.servus.storage.network;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Block state properties shared by the stock holding storage blocks. */
public final class StorageNodeBlocks {

    /** Lit while the block belongs to an online warehouse. Set by the network, never by the player. */
    public static final BooleanProperty ONLINE = BooleanProperty.create("online");

    /** What an inbound station is doing, shown by its second lamp. Set by the network, never by the player. */
    public enum Activity implements StringRepresentable {
        IDLE("idle"), WORKING("working"), BLOCKED("blocked");

        private final String name;

        Activity(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Activity> ACTIVITY = EnumProperty.create("activity", Activity.class);

    private StorageNodeBlocks() {
    }
}
