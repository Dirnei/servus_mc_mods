package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.logistics.channel.Channel;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelList;
import io.github.dirnei.mc.servus.foundation.logistics.channel.DuctChannelData;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.network.ChannelUses;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

/**
 * The channel requests of the endpoint GUI, server side. Each needs the
 * player to have a usable {@link UtilityDuctMenu} open, and acts only on
 * channels of the player's team; a refused request changes nothing and
 * returns false. After a change the player gets fresh cable channels.
 */
public final class ChannelRequests {

    private ChannelRequests() {
    }

    /** Puts the open side's line on Default or one of the team's channels. */
    public static boolean setSideChannel(Player player, UUID team, int lineOrdinal, int id) {
        Optional<UtilityDuctMenu> menu = openMenu(player);
        Optional<Line> line = channelLine(menu, lineOrdinal);
        if (line.isEmpty() || id != ChannelList.DEFAULT_ID && ownChannel(player, team, id).isEmpty()) {
            return false;
        }
        menu.get().duct().setChannel(line.get(), menu.get().side(), id);
        menu.get().sendCableChannels(true);
        return true;
    }

    /** Creates a channel for the team and puts the open side's line on it. */
    public static boolean create(Player player, UUID team, int lineOrdinal, String name, int colour, String icon) {
        Optional<UtilityDuctMenu> menu = openMenu(player);
        Optional<Line> line = channelLine(menu, lineOrdinal);
        if (line.isEmpty()) {
            return false;
        }
        Optional<Channel> created = data(player).create(team, name, colour, icon);
        if (created.isEmpty()) {
            return false;
        }
        menu.get().duct().setChannel(line.get(), menu.get().side(), created.get().id());
        menu.get().sendCableChannels(true);
        return true;
    }

    /** Changes name, colour and icon of one of the team's channels. */
    public static boolean edit(Player player, UUID team, int id, String name, int colour, String icon) {
        Optional<UtilityDuctMenu> menu = openMenu(player);
        if (menu.isEmpty() || ownChannel(player, team, id).isEmpty() || data(player).edit(id, name, colour, icon).isEmpty()) {
            return false;
        }
        menu.get().sendCableChannels(true);
        return true;
    }

    /**
     * Deletes one of the team's channels when no loaded side uses it, or when
     * the only loaded use is the open side's line, which then goes to Default.
     */
    public static boolean delete(Player player, UUID team, int lineOrdinal, int id) {
        Optional<UtilityDuctMenu> menu = openMenu(player);
        Optional<Line> line = channelLine(menu, lineOrdinal);
        if (line.isEmpty() || ownChannel(player, team, id).isEmpty()) {
            return false;
        }
        UtilityDuctBlockEntity duct = menu.get().duct();
        Direction side = menu.get().side();
        int loaded = ChannelUses.loaded(server(player)).getOrDefault(id, 0);
        boolean openSideOnIt = duct.channel(line.get(), side) == id;
        int openSideUses = openSideOnIt && duct.hasArm(line.get(), side) && duct.isEndpoint(side) ? 1 : 0;
        if (!ChannelList.deletable(id, loaded, openSideUses)) {
            return false;
        }
        if (openSideOnIt) {
            duct.setChannel(line.get(), side, ChannelList.DEFAULT_ID);
        }
        data(player).delete(id);
        menu.get().sendCableChannels(true);
        return true;
    }

    /** What the open GUI shows about channels: per line the ids on the run, and the loaded uses of the team's channels. */
    static CableChannelsPayload cableChannels(UtilityDuctMenu menu, UUID team, MinecraftServer server) {
        List<Set<Integer>> onCable = new ArrayList<>();
        for (Line line : Line.VALUES) {
            onCable.add(new HashSet<>(ChannelUses.onCable(menu.duct(), line).keySet()));
        }
        Map<Integer, Integer> loaded = ChannelUses.loaded(server);
        Map<Integer, Integer> teamUses = new HashMap<>();
        for (Channel channel : DuctChannelData.get(server).list().forTeam(team)) {
            teamUses.put(channel.id(), loaded.getOrDefault(channel.id(), 0));
        }
        return new CableChannelsPayload(team, onCable, teamUses);
    }

    private static Optional<UtilityDuctMenu> openMenu(Player player) {
        if (player.level().isClientSide() || !(player.containerMenu instanceof UtilityDuctMenu menu) || !menu.stillValid(player)) {
            return Optional.empty();
        }
        return Optional.of(menu);
    }

    /** The line with that ordinal when the menu is open and its duct carries it; never the data line. */
    private static Optional<Line> channelLine(Optional<UtilityDuctMenu> menu, int ordinal) {
        if (menu.isEmpty() || ordinal < 0 || ordinal >= Line.VALUES.length) {
            return Optional.empty();
        }
        Line line = Line.VALUES[ordinal];
        return line != Line.DATA && menu.get().duct().has(line) ? Optional.of(line) : Optional.empty();
    }

    private static Optional<Channel> ownChannel(Player player, UUID team, int id) {
        Channel channel = id == ChannelList.DEFAULT_ID ? null : data(player).list().get(id);
        return channel != null && channel.team().equals(team) ? Optional.of(channel) : Optional.empty();
    }

    private static MinecraftServer server(Player player) {
        return player.level().getServer();
    }

    private static DuctChannelData data(Player player) {
        return DuctChannelData.get(server(player));
    }
}
