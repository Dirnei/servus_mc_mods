package io.github.dirnei.mc.servus.shuttles.outbound;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: the exports of the open Outbound Station's keep slots and
 * the exports of its warehouse it may claim.
 *
 * @param slots  the exports this station holds
 * @param offers the exports no station holds, then those other stations hold
 */
public record ExportListPayload(List<Slot> slots, List<Offer> offers) implements CustomPacketPayload {

    /**
     * An export in a keep slot.
     *
     * @param state the rule's export state: {@code exporting}, {@code station_full} or {@code ok}
     */
    public record Slot(int slot, ItemStack item, int threshold, String state) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Slot::slot,
                ItemStack.STREAM_CODEC, Slot::item,
                ByteBufCodecs.VAR_INT, Slot::threshold,
                ByteBufCodecs.STRING_UTF8, Slot::state,
                Slot::new);
    }

    /**
     * An export the station may claim.
     *
     * @param holder the label of the station holding it, empty while none does
     */
    public record Offer(ItemStack item, int threshold, Component holder) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Offer> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Offer::item,
                ByteBufCodecs.VAR_INT, Offer::threshold,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Offer::holder,
                Offer::new);

        public boolean held() {
            return !holder.getString().isEmpty();
        }
    }

    public static final ExportListPayload EMPTY = new ExportListPayload(List.of(), List.of());

    public static final Type<ExportListPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "export_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExportListPayload> STREAM_CODEC = StreamCodec.composite(
            Slot.STREAM_CODEC.apply(ByteBufCodecs.list()), ExportListPayload::slots,
            Offer.STREAM_CODEC.apply(ByteBufCodecs.list()), ExportListPayload::offers,
            ExportListPayload::new);

    /** The export in a keep slot, or null. */
    public Slot slot(int index) {
        for (Slot slot : slots) {
            if (slot.slot() == index) {
                return slot;
            }
        }
        return null;
    }

    /** A signature of the content, so the menu sends it only when it changed. */
    public long signature() {
        long hash = 1;
        for (Slot slot : slots) {
            hash = hash * 31 + slot.slot();
            hash = hash * 31 + BuiltInRegistries.ITEM.getKey(slot.item().getItem()).hashCode();
            hash = hash * 31 + slot.threshold();
            hash = hash * 31 + slot.state().hashCode();
        }
        for (Offer offer : offers) {
            hash = hash * 31 + BuiltInRegistries.ITEM.getKey(offer.item().getItem()).hashCode();
            hash = hash * 31 + offer.threshold();
            hash = hash * 31 + offer.holder().getString().hashCode();
        }
        return hash;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
