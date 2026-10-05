package io.github.dirnei.mc.servus.manufacturing.research;

import io.github.dirnei.mc.servus.lib.team.Teams;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import io.github.dirnei.mc.servus.manufacturing.network.ResearchSyncPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server side facade over research state, validation and sync. */
public final class ResearchManager {

    private ResearchManager() {
    }

    public static ResearchState state(MinecraftServer server, UUID team) {
        return ResearchSavedData.get(server).forTeam(team);
    }

    /** A player picked a research target. Validated against availability. */
    public static boolean start(ServerPlayer player, ResourceLocation techId) {
        MinecraftServer server = player.server;
        Optional<Techs.Entry> entry = Techs.get(server.registryAccess(), techId);
        if (entry.isEmpty()) {
            return false;
        }
        UUID team = Teams.teamOf(player);
        ResearchSavedData data = ResearchSavedData.get(server);
        ResearchState state = data.forTeam(team);
        if (!state.start(entry.get().node())) {
            return false;
        }
        data.setDirty();
        sync(server, team);
        return true;
    }

    /**
     * A lab consumed one pack for the team's current research.
     *
     * @return true when the tech just completed
     */
    public static boolean labConsumed(MinecraftServer server, UUID team, TechNode tech, String item) {
        ResearchSavedData data = ResearchSavedData.get(server);
        ResearchState state = data.forTeam(team);
        boolean completed = state.consume(tech, item);
        data.setDirty();
        if (completed) {
            ServusManufacturing.LOGGER.info("Team {} completed research {}", team, tech.id());
        }
        sync(server, team);
        return completed;
    }

    public static void sync(MinecraftServer server, UUID team) {
        ResearchSyncPayload payload = payloadFor(state(server, team));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (team.equals(Teams.teamOf(player))) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void syncPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payloadFor(state(player.server, Teams.teamOf(player))));
    }

    private static ResearchSyncPayload payloadFor(ResearchState state) {
        List<ResourceLocation> completed = new ArrayList<>();
        state.completed().forEach(id -> completed.add(ResourceLocation.parse(id)));
        Optional<ResourceLocation> current = state.current().map(ResourceLocation::parse);
        Map<String, Integer> progress = new HashMap<>();
        state.current().ifPresent(id -> progress.putAll(state.progress().getOrDefault(id, Map.of())));
        return new ResearchSyncPayload(completed, current, progress);
    }
}
