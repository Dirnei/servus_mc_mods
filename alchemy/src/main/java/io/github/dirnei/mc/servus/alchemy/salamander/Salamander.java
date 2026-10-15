package io.github.dirnei.mc.servus.alchemy.salamander;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * The salamander, a fire elemental from the Nether's lava. Wild, only Elemental Sight shows it; a Gold Ingot may befriend it,
 * and its owner feeds it gold that it gives back as Sal Ammoniac. Fire immune, harmless, walks on lava and land.
 */
public class Salamander extends TamableAnimal {

    /** Entity event: hearts, the taming succeeded. */
    private static final byte EVENT_BEFRIENDED = 7;
    /** Entity event: smoke, the taming failed. */
    private static final byte EVENT_REFUSED = 6;
    private static final String DIGEST_TAG = "digest_ticks";
    /** One flame particle about every two seconds marks a wild salamander for everyone. */
    private static final int FLAME_HINT_CHANCE = 40;
    /** About every ten seconds a wild salamander's flame dances: a small rising circle of flames. */
    private static final int FLAME_DANCE_CHANCE = 200;
    private static final int FLAME_DANCE_STEPS = 6;
    private static final double FLAME_DANCE_RADIUS = 0.3;

    private final Digestion digestion = new Digestion();

    public Salamander(EntityType<? extends Salamander> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.LAVA, 0.0F);
        setPathfindingMalus(PathType.FIRE, 0.0F);
        setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    /** Spawns only on lava whose column is topped by air, like the Strider. */
    public static boolean checkSpawnRules(EntityType<Salamander> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos,
                                          RandomSource random) {
        BlockPos.MutableBlockPos check = pos.mutable();
        do {
            check.move(Direction.UP);
        } while (level.getFluidState(check).is(FluidTags.LAVA));
        return level.getBlockState(check).isAir();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new TamableAnimalPanicGoal(1.5));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
        goalSelector.addGoal(4, new LieInCampfireGoal(this, 1.0));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0, 60));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.GOLD_INGOT)) {
            if (level().isClientSide()) {
                return !isTame() || isOwnedBy(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            return offerGold(player, stack, temper());
        }
        InteractionResult result = super.mobInteract(player, hand);
        if (!result.consumesAction() && isTame() && isOwnedBy(player) && stack.isEmpty()) {
            setOrderedToSit(!isOrderedToSit());
            jumping = false;
            navigation.stop();
            return InteractionResult.SUCCESS.withoutItem();
        }
        return result;
    }

    /**
     * A Gold Ingot held out by {@code player}: a wild salamander takes it and may befriend them; a befriended one takes it
     * from its owner to digest while its stomach is empty. Server side.
     *
     * @return {@link InteractionResult#SUCCESS_SERVER} when the ingot was taken, {@link InteractionResult#PASS} otherwise
     */
    public InteractionResult offerGold(Player player, ItemStack stack, SalamanderTemper temper) {
        if (!isTame()) {
            stack.consume(1, player);
            tryToBefriend(player, temper);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!isOwnedBy(player) || digestion.busy()) {
            return InteractionResult.PASS;
        }
        stack.consume(1, player);
        digestion.start(temper.digestTicks());
        playSound(SoundEvents.STRIDER_EAT, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }

    private void tryToBefriend(Player player, SalamanderTemper temper) {
        if (temper.befriends(random.nextDouble()) && !EventHooks.onAnimalTame(this, player)) {
            tame(player);
            navigation.stop();
            level().broadcastEntityEvent(this, EVENT_BEFRIENDED);
        } else {
            level().broadcastEntityEvent(this, EVENT_REFUSED);
        }
    }

    /** Whether a viewer sees this salamander: a befriended one always, a wild one only with Elemental Sight. */
    public boolean shownTo(boolean sighted) {
        return isTame() || sighted;
    }

    /** Whether a Gold Ingot is still being digested. */
    public boolean isDigesting() {
        return digestion.busy();
    }

    private SalamanderTemper temper() {
        return SalamanderTemperData.INSTANCE.forType(getType());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            if (digestion.tick()) {
                spawnAtLocation(server, new ItemStack(ServusAlchemyItems.SAL_AMMONIAC.get()));
            }
        } else if (!isTame()) {
            if (random.nextInt(FLAME_HINT_CHANCE) == 0) {
                level().addParticle(ParticleTypes.FLAME, getRandomX(0.4), getY() + 0.3, getRandomZ(0.4), 0.0, 0.02, 0.0);
            }
            if (random.nextInt(FLAME_DANCE_CHANCE) == 0) {
                danceFlame();
            }
        }
    }

    /** A few flames that circle up from where the wild salamander swims. */
    private void danceFlame() {
        for (int i = 0; i < FLAME_DANCE_STEPS; i++) {
            double angle = i * Math.PI * 2 / FLAME_DANCE_STEPS;
            double x = getX() + Math.cos(angle) * FLAME_DANCE_RADIUS;
            double z = getZ() + Math.sin(angle) * FLAME_DANCE_RADIUS;
            level().addParticle(ParticleTypes.FLAME, x, getY() + 0.2 + i * 0.08, z,
                    -Math.sin(angle) * 0.03, 0.05, Math.cos(angle) * 0.03);
        }
    }

    @Override
    public void tick() {
        super.tick();
        floatInLava();
    }

    /** Lifts the salamander to the lava surface and lets it stand there. */
    private void floatInLava() {
        if (!isInLava()) {
            return;
        }
        CollisionContext context = CollisionContext.of(this);
        if (context.isAbove(getLiquidCollisionShape(), blockPosition(), true)
                && !level().getFluidState(blockPosition().above()).is(FluidTags.LAVA)) {
            setOnGround(true);
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
        }
    }

    @Override
    public VoxelShape getLiquidCollisionShape() {
        return Block.column(16.0, 0.0, 8.0);
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new SalamanderNavigation(this, level);
    }

    /** A wild salamander keeps to the lava; a befriended one walks where its owner goes. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (isTame()) {
            return super.getWalkTargetValue(pos, level);
        }
        if (level.getBlockState(pos).getFluidState().is(FluidTags.LAVA)) {
            return 10.0F;
        }
        return isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (isInLava()) {
            resetFallDistance();
        } else {
            super.checkFallDamage(ya, onGround, onState, pos);
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !isTame();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt(DIGEST_TAG, digestion.remaining());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        digestion.restore(input.getIntOr(DIGEST_TAG, 0));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }
}
