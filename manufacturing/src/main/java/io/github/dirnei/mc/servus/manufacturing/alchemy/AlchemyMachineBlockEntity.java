package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A powered alchemy machine that belongs to a team. The owner is the team of the placing player,
 * resolved at placement, so its work counts for that team's journal even while the player is offline.
 */
public abstract class AlchemyMachineBlockEntity extends PoweredMachineBlockEntity implements OwnedMachine {

    private UUID team;

    protected AlchemyMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Override
    public Optional<UUID> owner() {
        return Optional.ofNullable(team);
    }

    /** Takes the placing player's id and stores their team. */
    @Override
    public void setOwner(UUID player) {
        UUID resolved = player;
        if (level instanceof ServerLevel server) {
            ServerPlayer online = server.getServer().getPlayerList().getPlayer(player);
            if (online != null) {
                resolved = Teams.teamOf(online);
            }
        }
        team = resolved;
        setChanged();
    }

    /** Sets the owning team directly, for tests and commands. */
    public void setTeam(UUID team) {
        this.team = team;
        setChanged();
    }

    /** The owning team, or {@code null} for a machine nobody placed. */
    public UUID team() {
        return team;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (team != null) {
            tag.putUUID("Team", team);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        team = tag.hasUUID("Team") ? tag.getUUID("Team") : null;
    }
}
