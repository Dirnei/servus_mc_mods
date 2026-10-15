package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Server to client: every loaded vein with its colour and yield. Replaces what the client had. */
public record VeinSyncPayload(List<ShownVein> veins) implements CustomPacketPayload {

    /** A vein as the client knows it. */
    public record ShownVein(Identifier id, int color, ItemStack yield) {
    }

    public static final Type<VeinSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "veins"));

    private static final StreamCodec<RegistryFriendlyByteBuf, ShownVein> VEIN = StreamCodec.composite(
            Identifier.STREAM_CODEC, ShownVein::id,
            ByteBufCodecs.INT, ShownVein::color,
            ItemStack.OPTIONAL_STREAM_CODEC, ShownVein::yield,
            ShownVein::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, VeinSyncPayload> STREAM_CODEC =
            VEIN.apply(ByteBufCodecs.list()).map(VeinSyncPayload::new, VeinSyncPayload::veins);

    /** The payload of the loaded veins, in id order. */
    public static VeinSyncPayload of(Map<Identifier, VeinData.VeinDef> veins) {
        return new VeinSyncPayload(veins.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ShownVein(entry.getKey(), entry.getValue().color(), entry.getValue().yield()))
                .toList());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
