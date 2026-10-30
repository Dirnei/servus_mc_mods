package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server, from the handheld GUI's FIND screen: edit the basket of
 * the open handheld. {@code item} is empty for {@link Op#CLEAR}, {@code amount}
 * is unused for {@link Op#REMOVE} and {@link Op#CLEAR}.
 */
public record HandheldBasketPayload(Op op, ItemStack item, int amount) implements CustomPacketPayload {

    /** What to do with the basket. */
    public enum Op {
        /** Adds the amount, raising the item's entry when it has one. */
        ADD,
        /** Sets the item's amount; below 1 removes it. */
        SET,
        REMOVE,
        CLEAR
    }

    public static final Type<HandheldBasketPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_basket"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Op> OP_CODEC =
            ByteBufCodecs.VAR_INT.map(index -> Op.values()[Math.floorMod(index, Op.values().length)], Op::ordinal).cast();

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldBasketPayload> STREAM_CODEC = StreamCodec.composite(
            OP_CODEC, HandheldBasketPayload::op,
            ItemStack.OPTIONAL_STREAM_CODEC, HandheldBasketPayload::item,
            ByteBufCodecs.VAR_INT, HandheldBasketPayload::amount,
            HandheldBasketPayload::new);

    public static HandheldBasketPayload add(ItemStack item, int amount) {
        return new HandheldBasketPayload(Op.ADD, item, amount);
    }

    public static HandheldBasketPayload set(ItemStack item, int amount) {
        return new HandheldBasketPayload(Op.SET, item, amount);
    }

    public static HandheldBasketPayload remove(ItemStack item) {
        return new HandheldBasketPayload(Op.REMOVE, item, 0);
    }

    public static HandheldBasketPayload clear() {
        return new HandheldBasketPayload(Op.CLEAR, ItemStack.EMPTY, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
