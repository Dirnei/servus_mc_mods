package io.github.dirnei.mc.servus.alchemy.gnome;

import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyEntities;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A gnome, an earth elemental drawn only for players with Elemental Sight. He cannot be hurt, pushed or leashed and never
 * despawns. A gnome with a hollow belongs to it: he leaves once his hollow no longer keeps him.
 */
public class Gnome extends PathfinderMob {

    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(Gnome.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ACTION = SynchedEntityData.defineId(Gnome.class, EntityDataSerializers.BYTE);
    /** The saved kind, by {@link GnomeKind#serializedName()}; a spawn egg sets it in its entity data. */
    public static final String KIND_TAG = "Kind";
    private static final String HOLLOW_TAG = "Hollow";
    private static final String LIFE_TAG = "Life";
    /** How often a gnome asks his hollow whether it still keeps him. */
    private static final int HOLLOW_CHECK_INTERVAL = 20;

    /** Whether the player of this client has Elemental Sight; set by the client, so a server sees every gnome. */
    private static BooleanSupplier viewerSees = () -> true;

    private @Nullable BlockPos hollow;
    private int life;

    public Gnome(EntityType<? extends Gnome> type, Level level) {
        super(type, level);
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    /**
     * Brings a gnome of {@code kind} that belongs to the hollow at {@code hollow} into the world at {@code at}, looking
     * along {@code yRot}.
     *
     * @return the gnome, empty when the level refused him
     */
    public static Optional<Gnome> spawn(ServerLevel level, GnomeKind kind, BlockPos hollow, Vec3 at, float yRot) {
        Gnome gnome = ServusAlchemyEntities.GNOME.get().create(level, EntitySpawnReason.TRIGGERED);
        if (gnome == null) {
            return Optional.empty();
        }
        gnome.setKind(kind);
        gnome.hollow = hollow.immutable();
        gnome.snapTo(at.x, at.y, at.z, yRot, 0);
        gnome.setYHeadRot(yRot);
        gnome.setYBodyRot(yRot);
        return level.addFreshEntity(gnome) ? Optional.of(gnome) : Optional.empty();
    }

    /** Lets the client tell whether its player sees gnomes; without the sight they cannot be clicked or hit there. */
    public static void watchSightWith(BooleanSupplier sees) {
        viewerSees = sees;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, (byte) GnomeKind.MINER.ordinal());
        builder.define(ACTION, (byte) GnomeAction.IDLE.ordinal());
    }

    public GnomeKind kind() {
        return GnomeKind.byIndex(entityData.get(KIND));
    }

    public void setKind(GnomeKind kind) {
        entityData.set(KIND, (byte) kind.ordinal());
    }

    /** What his hands do now, synced for the animation. */
    public GnomeAction action() {
        return GnomeAction.byIndex(entityData.get(ACTION));
    }

    public void setAction(GnomeAction action) {
        entityData.set(ACTION, (byte) action.ordinal());
    }

    /** The hollow he belongs to, empty for a gnome from a spawn egg. */
    public Optional<BlockPos> hollow() {
        return Optional.ofNullable(hollow);
    }

    /** Whether he belongs to the hollow at {@code pos}. */
    public boolean livesAt(BlockPos pos) {
        return pos.equals(hollow);
    }

    /** Ticks since he came, counted only while he is loaded. */
    public int life() {
        return life;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            life++;
            if (hollow != null && life % HOLLOW_CHECK_INTERVAL == 0 && server.isLoaded(hollow) && !keptByHollow(server, hollow)) {
                discard();
            }
        }
    }

    /** Whether the hollow he belongs to still keeps him; a gnome nobody keeps leaves. */
    private boolean keptByHollow(ServerLevel level, BlockPos pos) {
        BlockEntity host = level.getBlockEntity(pos);
        return switch (kind()) {
            case MINER -> host instanceof GnomeHollowBlockEntity hollow && hollow.keepsGnome(getUUID());
            case LANTERN, HOUSE -> true;
        };
    }

    /** Whether a gnome fits into the block at {@code pos}: nothing there to collide with. */
    public static boolean roomAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public boolean isPickable() {
        return super.isPickable() && (!level().isClientSide() || viewerSees.getAsBoolean());
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString(KIND_TAG, kind().serializedName());
        if (hollow != null) {
            output.store(HOLLOW_TAG, BlockPos.CODEC, hollow);
        }
        output.putInt(LIFE_TAG, life);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setKind(GnomeKind.byName(input.getStringOr(KIND_TAG, GnomeKind.MINER.serializedName())));
        hollow = input.read(HOLLOW_TAG, BlockPos.CODEC).orElse(null);
        life = input.getIntOr(LIFE_TAG, 0);
    }
}
