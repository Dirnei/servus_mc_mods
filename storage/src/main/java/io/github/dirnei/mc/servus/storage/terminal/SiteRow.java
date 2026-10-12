package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.site.RemoteSite;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Another site as a GUI lists it.
 *
 * @param key   the site's registry key
 * @param name  the name to show
 * @param state the {@link RemoteSite.State} ordinal
 * @param trade whether its controller is wireless capable, so it can send and receive; its name shows orange otherwise
 */
public record SiteRow(String key, String name, int state, boolean trade) {

    public static final StreamCodec<ByteBuf, SiteRow> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), SiteRow::key,
            ByteBufCodecs.stringUtf8(64), SiteRow::name,
            ByteBufCodecs.VAR_INT, SiteRow::state,
            ByteBufCodecs.BOOL, SiteRow::trade,
            SiteRow::new);

    public RemoteSite.State remoteState() {
        RemoteSite.State[] states = RemoteSite.State.values();
        return state >= 0 && state < states.length ? states[state] : RemoteSite.State.UNREACHABLE;
    }
}
