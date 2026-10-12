package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.site.RemoteSite;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Another site as a GUI lists it.
 *
 * @param key      the site's registry key
 * @param name     the name to show
 * @param state    the {@link RemoteSite.State} ordinal
 * @param wireless whether it has a powered Wireless Packing Station of the team, so the wireless line reaches it
 * @param station  whether it has a Wireless Packing Station of the team at all, powered or not
 */
public record SiteRow(String key, String name, int state, boolean wireless, boolean station) {

    public static final StreamCodec<ByteBuf, SiteRow> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), SiteRow::key,
            ByteBufCodecs.stringUtf8(64), SiteRow::name,
            ByteBufCodecs.VAR_INT, SiteRow::state,
            ByteBufCodecs.BOOL, SiteRow::wireless,
            ByteBufCodecs.BOOL, SiteRow::station,
            SiteRow::new);

    /** A station of the team stands there but none is powered: the site is off the wireless line. */
    public boolean unpowered() {
        return station && !wireless;
    }

    public RemoteSite.State remoteState() {
        RemoteSite.State[] states = RemoteSite.State.values();
        return state >= 0 && state < states.length ? states[state] : RemoteSite.State.UNREACHABLE;
    }
}
