package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.site.SiteCapacity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: what the open controller GUI shows about its site.
 *
 * @param name     the name the player gave the site, empty when unnamed
 * @param title    the name to show
 * @param online   0 online, 1 no controller, 2 more than one controller
 * @param summary  the warehouse summary, the tooltip of the online line
 * @param chunks   the chunk keys of the span
 * @param home     the chunk key of the controller
 * @param upgrades Chunk Loader Upgrades held
 * @param capacity chunks the upgrades cover
 * @param state    the {@link SiteCapacity.State} ordinal
 * @param wake     the wake line, empty when there is nothing to say
 * @param wakeAlert whether the wake line explains a problem (drawn red)
 * @param sites    the team's other sites in the dimension
 */
public record SiteStatusPayload(String name, String title, int online, Component summary, List<Long> chunks, long home,
                                int upgrades, int capacity, int state, Component wake, boolean wakeAlert,
                                List<io.github.dirnei.mc.servus.storage.terminal.SiteRow> sites) implements CustomPacketPayload {

    public static final Type<SiteStatusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "site_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SiteStatusPayload> STREAM_CODEC =
            StreamCodec.ofMember(SiteStatusPayload::write, SiteStatusPayload::read);

    public static final SiteStatusPayload EMPTY =
            new SiteStatusPayload("", "", 1, Component.empty(), List.of(), 0L, 0, 0, 0, Component.empty(), false, List.of());

    public SiteCapacity.State capacityState() {
        SiteCapacity.State[] states = SiteCapacity.State.values();
        return state >= 0 && state < states.length ? states[state] : SiteCapacity.State.NO_UPGRADE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(name, 64);
        buf.writeUtf(title, 64);
        buf.writeVarInt(online);
        ComponentSerialization.STREAM_CODEC.encode(buf, summary);
        buf.writeVarInt(chunks.size());
        chunks.forEach(buf::writeLong);
        buf.writeLong(home);
        buf.writeVarInt(upgrades);
        buf.writeVarInt(capacity);
        buf.writeVarInt(state);
        ComponentSerialization.STREAM_CODEC.encode(buf, wake);
        buf.writeBoolean(wakeAlert);
        List<io.github.dirnei.mc.servus.storage.terminal.SiteRow> sent = sites.subList(0, Math.min(sites.size(), 256));
        buf.writeVarInt(sent.size());
        sent.forEach(row -> io.github.dirnei.mc.servus.storage.terminal.SiteRow.STREAM_CODEC.encode(buf, row));
    }

    private static SiteStatusPayload read(RegistryFriendlyByteBuf buf) {
        String name = buf.readUtf(64);
        String title = buf.readUtf(64);
        int online = buf.readVarInt();
        Component summary = ComponentSerialization.STREAM_CODEC.decode(buf);
        int count = Math.min(buf.readVarInt(), 4096);
        List<Long> chunks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            chunks.add(buf.readLong());
        }
        long home = buf.readLong();
        int upgrades = buf.readVarInt();
        int capacity = buf.readVarInt();
        int state = buf.readVarInt();
        Component wake = ComponentSerialization.STREAM_CODEC.decode(buf);
        boolean wakeAlert = buf.readBoolean();
        int siteCount = Math.min(buf.readVarInt(), 256);
        List<io.github.dirnei.mc.servus.storage.terminal.SiteRow> sites = new ArrayList<>(siteCount);
        for (int i = 0; i < siteCount; i++) {
            sites.add(io.github.dirnei.mc.servus.storage.terminal.SiteRow.STREAM_CODEC.decode(buf));
        }
        return new SiteStatusPayload(name, title, online, summary, chunks, home, upgrades, capacity, state, wake, wakeAlert, sites);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
