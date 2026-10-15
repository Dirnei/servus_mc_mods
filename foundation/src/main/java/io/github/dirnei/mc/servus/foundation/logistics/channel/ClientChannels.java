package io.github.dirnei.mc.servus.foundation.logistics.channel;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The client's copy of the duct channels, replaced whenever the server sends
 * the list. No client imports: the client code installs a listener that
 * re-tints the ducts whose ring colour changed.
 */
public final class ClientChannels {

    private static volatile ChannelList list = new ChannelList();
    private static Consumer<Set<Integer>> recolourListener = ids -> {
    };

    private ClientChannels() {
    }

    public static ChannelList list() {
        return list;
    }

    /** The ring colour of a channel id; unknown ids show Default's. */
    public static int argb(int id) {
        Channel channel = list.get(id);
        return (channel == null ? Channel.DEFAULT : channel).argb();
    }

    /** Called with the ids whose ring colour changed after each update; client thread. */
    public static void onRecolour(Consumer<Set<Integer>> listener) {
        recolourListener = listener;
    }

    public static void update(ChannelListPayload payload) {
        ChannelList old = list;
        ChannelList next = payload.toList();
        list = next;
        Set<Integer> ids = new HashSet<>();
        old.all().forEach(channel -> ids.add(channel.id()));
        next.all().forEach(channel -> ids.add(channel.id()));
        Set<Integer> recoloured = new HashSet<>();
        for (int id : ids) {
            if (colourIn(old, id) != colourIn(next, id)) {
                recoloured.add(id);
            }
        }
        if (!recoloured.isEmpty()) {
            recolourListener.accept(recoloured);
        }
    }

    private static int colourIn(ChannelList channels, int id) {
        Channel channel = channels.get(id);
        return (channel == null ? Channel.DEFAULT : channel).argb();
    }
}
