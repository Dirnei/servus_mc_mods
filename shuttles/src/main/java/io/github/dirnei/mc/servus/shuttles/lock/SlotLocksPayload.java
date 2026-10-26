package io.github.dirnei.mc.servus.shuttles.lock;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The locks on the slots of an open menu, server to client.
 *
 * @param containerId the menu the locks belong to
 * @param entries     one entry per lock
 */
public record SlotLocksPayload(int containerId, List<Entry> entries) implements CustomPacketPayload {

    /**
     * One lock on one menu slot.
     *
     * @param slot    the menu slot index
     * @param in      whether the job fills the slot, else it empties it
     * @param item    the item it brings or takes
     * @param amount  how many
     * @param jobId   the job holding the lock
     * @param shuttle the label of the shuttle on the job, empty while the job waits for one
     * @param pipe    whether an Item Pipe's grant holds the slot; {@code amount} is then what the pipe still brings
     */
    public record Entry(int slot, boolean in, ItemStack item, int amount, int jobId, Optional<Component> shuttle, boolean pipe) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::slot,
                ByteBufCodecs.BOOL, Entry::in,
                ItemStack.STREAM_CODEC, Entry::item,
                ByteBufCodecs.VAR_INT, Entry::amount,
                ByteBufCodecs.VAR_INT, Entry::jobId,
                ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Entry::shuttle,
                ByteBufCodecs.BOOL, Entry::pipe,
                Entry::new);
    }

    public static final Type<SlotLocksPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "slot_locks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlotLocksPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlotLocksPayload::containerId,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), SlotLocksPayload::entries,
            SlotLocksPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
