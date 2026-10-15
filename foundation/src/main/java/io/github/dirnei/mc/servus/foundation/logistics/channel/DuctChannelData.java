package io.github.dirnei.mc.servus.foundation.logistics.channel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The duct channels of every team, saved with the overworld. Every create,
 * edit and delete goes through here on the server thread and is sent to
 * every player.
 */
public final class DuctChannelData extends SavedData {

    private static final Codec<Channel> CHANNEL_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("Id").forGetter(Channel::id),
            UUIDUtil.CODEC.fieldOf("Team").forGetter(Channel::team),
            Codec.STRING.fieldOf("Name").forGetter(Channel::name),
            Codec.INT.optionalFieldOf("Colour", 0).forGetter(Channel::colour),
            Codec.STRING.optionalFieldOf("Icon").forGetter(channel -> Optional.ofNullable(channel.icon())))
            .apply(i, (id, team, name, colour, icon) -> new Channel(id, team, name, colour, icon.orElse(null))));

    private static final Codec<DuctChannelData> CODEC = RecordCodecBuilder.create(i -> i.group(
            CHANNEL_CODEC.listOf().optionalFieldOf("Channels", List.of()).forGetter(data -> List.copyOf(data.list.all())),
            Codec.INT.optionalFieldOf("NextId", ChannelList.DEFAULT_ID + 1).forGetter(data -> data.list.nextId()))
            .apply(i, (channels, nextId) -> new DuctChannelData(new ChannelList(channels, nextId))));

    private static final SavedDataType<DuctChannelData> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("servus_duct_channels"), DuctChannelData::new, CODEC);

    private final ChannelList list;

    public DuctChannelData() {
        this(new ChannelList());
    }

    private DuctChannelData(ChannelList list) {
        this.list = list;
    }

    public static DuctChannelData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The channels; change them only through this class. */
    public ChannelList list() {
        return list;
    }

    public Optional<Channel> create(UUID team, String name, int colour, String icon) {
        return changedIfPresent(list.create(team, name, colour, icon));
    }

    public Optional<Channel> edit(int id, String name, int colour, String icon) {
        return changedIfPresent(list.edit(id, name, colour, icon));
    }

    public boolean delete(int id) {
        boolean deleted = list.delete(id);
        if (deleted) {
            changed();
        }
        return deleted;
    }

    private Optional<Channel> changedIfPresent(Optional<Channel> channel) {
        channel.ifPresent(c -> changed());
        return channel;
    }

    private void changed() {
        setDirty();
        ChannelSync.broadcast(list);
    }
}
