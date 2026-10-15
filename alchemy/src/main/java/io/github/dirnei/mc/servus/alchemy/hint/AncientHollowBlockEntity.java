package io.github.dirnei.mc.servus.alchemy.hint;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The old gnome in his Ancient Hollow: whose he is, whether he was found, and the hints he gives until then. Knocks come in
 * series from the block itself, so players hear where they come from; dust is sent only to players close by.
 */
public class AncientHollowBlockEntity extends BlockEntity {

    /** Knocks in one series and the ticks between them. */
    private static final int KNOCKS = 3;
    private static final int KNOCK_GAP = 5;
    private static final float KNOCK_VOLUME = 1.5F;
    private static final int DUST_INTERVAL = 10;
    private static final BlockParticleOption DUST = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DEEPSLATE.defaultBlockState());

    private UUID team;
    private boolean found;
    private int knockTimer = -1;
    private int knocksLeft;
    private int gapTimer;

    public AncientHollowBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.ANCIENT_HOLLOW.get(), pos, state);
    }

    /** The team the old gnome woke for, or null for a block placed by other means. */
    public UUID team() {
        return team;
    }

    public void setTeam(UUID team) {
        this.team = team;
        setChanged();
    }

    public boolean found() {
        return found;
    }

    /** Marks the old gnome found: he stops knocking and the reveal hides him. */
    public void markFound() {
        found = true;
        knocksLeft = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The side of the block that faces the air. */
    public Direction openFace() {
        return getBlockState().getValue(AncientHollowBlock.FACING);
    }

    /** The middle of the open face, just outside the block. */
    public Vec3 inFront(double outward) {
        Direction face = openFace();
        return worldPosition.getCenter().add(face.getStepX() * (0.5 + outward), 0, face.getStepZ() * (0.5 + outward));
    }

    /** Starts a knocking series now. */
    public void knock() {
        knocksLeft = KNOCKS;
        gapTimer = 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AncientHollowBlockEntity hollow) {
        if (level instanceof ServerLevel server && !hollow.found) {
            hollow.tick(server, OldGnomeData.INSTANCE.settings());
        }
    }

    private void tick(ServerLevel server, OldGnomeSettings settings) {
        if (knockTimer < 0) {
            knockTimer = server.getRandom().nextInt(settings.knockInterval());
        }
        if (--knockTimer <= 0) {
            knockTimer = settings.knockInterval();
            Vec3 center = worldPosition.getCenter();
            if (server.getNearestPlayer(center.x, center.y, center.z, settings.knockRange(), EntitySelector.NO_SPECTATORS) != null) {
                knock();
            }
        }
        if (knocksLeft > 0 && --gapTimer <= 0) {
            knocksLeft--;
            gapTimer = KNOCK_GAP;
            server.playSound(null, worldPosition, HintSounds.KNOCK.get(), SoundSource.BLOCKS, KNOCK_VOLUME,
                    0.55F + server.getRandom().nextFloat() * 0.1F);
        }
        if (server.getGameTime() % DUST_INTERVAL == 0) {
            dropDust(server, settings.dustRange());
        }
    }

    private void dropDust(ServerLevel server, int range) {
        Vec3 dust = inFront(0.05).add(0, 0.45, 0);
        double rangeSq = (double) range * range;
        for (ServerPlayer player : server.players()) {
            if (!player.isSpectator() && player.distanceToSqr(worldPosition.getCenter()) <= rangeSq) {
                server.sendParticles(player, DUST, false, false, dust.x, dust.y, dust.z, 2, 0.2, 0, 0.2, 0);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server && team != null && !found) {
            OldGnome.lost(server, team, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            AncientHollowTracker.add(this);
        }
    }

    @Override
    public void setRemoved() {
        AncientHollowTracker.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        AncientHollowTracker.remove(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (team != null) {
            output.store("Team", UUIDUtil.CODEC, team);
        }
        output.putBoolean("Found", found);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        team = input.read("Team", UUIDUtil.CODEC).orElse(null);
        found = input.getBooleanOr("Found", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Found", found);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
