package io.github.dirnei.mc.servus.alchemy.gnome;

import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlock;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity.Work;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * The Old Miner at work: while his vein ripens he walks to a free side of his Hollow and taps its cracks, once it is ripe
 * he stands at the cave entry and swings his pickaxe while the vein yields. After each stint he rests and wanders; a long
 * shift at the cave entry ends with a walk out.
 */
public class TendHollowGoal extends Goal {

    private static final double SPEED = 0.8;
    /** Squared horizontal distance to the middle of his spot at which he stops walking and works. */
    private static final double AT_SPOT_SQ = 0.75 * 0.75;
    private static final double AT_SPOT_HEIGHT = 0.5;
    private static final int GIVE_UP_TICKS = 200;
    private static final int REPATH_TICKS = 20;
    /** Ticks of one stint: tapping on one side, or a shift of 30 to 90 s at the cave entry. */
    private static final int TAP_MIN = 100;
    private static final int TAP_MAX = 200;
    private static final int DIG_MIN = 600;
    private static final int DIG_MAX = 1800;
    /** Ticks of rest after a stint: a short look around after tapping, a walk out after digging. */
    private static final int TAP_REST_MIN = 40;
    private static final int TAP_REST_MAX = 100;
    private static final int DIG_REST_MIN = 200;
    private static final int DIG_REST_MAX = 400;

    private final Gnome gnome;
    private Work work = Work.REST;
    private BlockPos spot = BlockPos.ZERO;
    private int workLeft;
    private int walking;
    private long restUntil;

    public TendHollowGoal(Gnome gnome) {
        this.gnome = gnome;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (gnome.kind() != GnomeKind.MINER || gnome.level().getGameTime() < restUntil) {
            return false;
        }
        Optional<GnomeHollowBlockEntity> hollow = hollow();
        if (hollow.isEmpty() || hollow.get().work() == Work.REST) {
            return false;
        }
        work = hollow.get().work();
        Optional<BlockPos> chosen = chooseSpot(hollow.get());
        chosen.ifPresent(found -> spot = found);
        return chosen.isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return workLeft > 0 && walking < GIVE_UP_TICKS && hollow().map(GnomeHollowBlockEntity::work).orElse(Work.REST) == work;
    }

    @Override
    public void start() {
        RandomSource random = gnome.getRandom();
        workLeft = work == Work.DIG ? between(random, DIG_MIN, DIG_MAX) : between(random, TAP_MIN, TAP_MAX);
        walking = 0;
        walkToSpot();
    }

    @Override
    public void stop() {
        gnome.setAction(GnomeAction.IDLE);
        gnome.getNavigation().stop();
        RandomSource random = gnome.getRandom();
        int rest = work == Work.DIG ? between(random, DIG_REST_MIN, DIG_REST_MAX) : between(random, TAP_REST_MIN, TAP_REST_MAX);
        restUntil = gnome.level().getGameTime() + rest;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 target = Vec3.atBottomCenterOf(spot);
        Vec3 offset = gnome.position().subtract(target);
        if (offset.horizontalDistanceSqr() <= AT_SPOT_SQ && Math.abs(offset.y) <= AT_SPOT_HEIGHT) {
            gnome.getNavigation().stop();
            gnome.getLookControl().setLookAt(Vec3.atCenterOf(gnome.hollow().orElse(spot)));
            gnome.setAction(work == Work.DIG ? GnomeAction.DIG : GnomeAction.TAP);
            workLeft--;
            return;
        }
        gnome.setAction(GnomeAction.IDLE);
        walking++;
        if (walking % REPATH_TICKS == 0 && gnome.getNavigation().isDone()) {
            walkToSpot();
        }
    }

    /** Walks a path that ends on the spot itself, so the last straight step never cuts a corner of the Hollow. */
    private void walkToSpot() {
        Path path = gnome.getNavigation().createPath(spot, 0);
        if (path == null) {
            walking = GIVE_UP_TICKS;
        } else {
            gnome.getNavigation().moveTo(path, SPEED);
        }
    }

    /** Digging happens at the cave entry; tapping on a random free side, the cave entry if no other side is free. */
    private Optional<BlockPos> chooseSpot(GnomeHollowBlockEntity hollow) {
        BlockPos pos = hollow.getBlockPos();
        Direction entry = hollow.getBlockState().getValue(GnomeHollowBlock.FACING);
        if (!(gnome.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        if (work == Work.DIG) {
            return Optional.of(Gnome.standingSpot(level, pos.relative(GnomeKeeper.exitSide(level, pos, entry))));
        }
        List<BlockPos> free = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (Gnome.roomAt(level, pos.relative(side))) {
                free.add(pos.relative(side));
            }
        }
        BlockPos beside = free.isEmpty() ? pos.relative(entry) : free.get(gnome.getRandom().nextInt(free.size()));
        return Optional.of(Gnome.standingSpot(level, beside));
    }

    private Optional<GnomeHollowBlockEntity> hollow() {
        return gnome.hollow().filter(pos -> gnome.level().isLoaded(pos))
                .map(pos -> gnome.level().getBlockEntity(pos) instanceof GnomeHollowBlockEntity hollow ? hollow : null);
    }

    private static int between(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}
