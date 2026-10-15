package io.github.dirnei.mc.servus.shuttles.controller;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.site.SiteCapacity;
import io.github.dirnei.mc.servus.shuttles.terminal.SiteRow;
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
 * @param upgrades Anchor Upgrades held
 * @param capacity chunks the upgrades cover
 * @param state    the {@link SiteCapacity.State} ordinal
 * @param wake     the wake line, empty when there is nothing to say
 * @param wakeAlert whether the wake line explains a problem (drawn red)
 * @param energy   FE stored in the controller
 * @param energyCapacity FE the controller holds at most
 * @param capable  whether the controller is wireless capable
 * @param hint     why the sites panel lists nothing: {@link #HINT_NONE}, {@link #HINT_NO_UPGRADE} or {@link #HINT_NO_POWER}
 * @param needs    FE the controller needs for wireless, shown with {@link #HINT_NO_POWER}
 * @param used     FE the warehouse drew during the last second
 * @param noPower  whether the warehouse is in the no power state
 * @param sites    the other sites in the dimension, empty while the controller is not wireless capable
 */
public record SiteStatusPayload(String name, String title, int online, Component summary, List<Long> chunks, long home,
                                int upgrades, int capacity, int state, Component wake, boolean wakeAlert,
                                int energy, int energyCapacity, boolean capable, int hint, long needs, long used, boolean noPower,
                                List<SiteRow> sites) implements CustomPacketPayload {

    public static final int HINT_NONE = 0;
    public static final int HINT_NO_UPGRADE = 1;
    public static final int HINT_NO_POWER = 2;

    public static final Type<SiteStatusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "site_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SiteStatusPayload> STREAM_CODEC =
            StreamCodec.ofMember(SiteStatusPayload::write, SiteStatusPayload::read);

    public static final SiteStatusPayload EMPTY =
            new SiteStatusPayload("", "", 1, Component.empty(), List.of(), 0L, 0, 0, 0, Component.empty(), false, 0, 0, false, HINT_NONE, 0L, 0L, false,
                    List.of());

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
        buf.writeVarInt(energy);
        buf.writeVarInt(energyCapacity);
        buf.writeBoolean(capable);
        buf.writeVarInt(hint);
        buf.writeVarLong(needs);
        buf.writeVarLong(Math.max(0, used));
        buf.writeBoolean(noPower);
        List<SiteRow> sent = sites.subList(0, Math.min(sites.size(), 256));
        buf.writeVarInt(sent.size());
        sent.forEach(row -> SiteRow.STREAM_CODEC.encode(buf, row));
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
        int energy = buf.readVarInt();
        int energyCapacity = buf.readVarInt();
        boolean capable = buf.readBoolean();
        int hint = buf.readVarInt();
        long needs = buf.readVarLong();
        long used = buf.readVarLong();
        boolean noPower = buf.readBoolean();
        int siteCount = Math.min(buf.readVarInt(), 256);
        List<SiteRow> sites = new ArrayList<>(siteCount);
        for (int i = 0; i < siteCount; i++) {
            sites.add(SiteRow.STREAM_CODEC.decode(buf));
        }
        return new SiteStatusPayload(name, title, online, summary, chunks, home, upgrades, capacity, state, wake, wakeAlert,
                energy, energyCapacity, capable, hint, needs, used, noPower, sites);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
