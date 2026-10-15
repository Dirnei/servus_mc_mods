package io.github.dirnei.mc.servus.alchemy.gnome;

import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowBlock;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The Lantern Bearer walks ahead to the open face of the Ancient Hollow and waits beside it. */
public class LeadToHollowGoal extends Goal {

    private static final double SPEED = 0.9;
    /** Squared distance to the spot in front of the open face within which he waits. */
    private static final double WAIT_SQ = 2.0 * 2.0;
    private static final int REPATH_TICKS = 20;

    private final Gnome gnome;
    private BlockPos spot = BlockPos.ZERO;
    private int walking;

    public LeadToHollowGoal(Gnome gnome) {
        this.gnome = gnome;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (gnome.kind() != GnomeKind.LANTERN) {
            return false;
        }
        Optional<BlockPos> front = frontOfHollow();
        front.ifPresent(found -> spot = found);
        return front.isPresent() && !waiting();
    }

    @Override
    public boolean canContinueToUse() {
        return !waiting();
    }

    @Override
    public void start() {
        walking = 0;
        walk();
    }

    @Override
    public void stop() {
        gnome.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (++walking % REPATH_TICKS == 0 && gnome.getNavigation().isDone()) {
            walk();
        }
    }

    private void walk() {
        gnome.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, SPEED);
    }

    private boolean waiting() {
        return gnome.position().distanceToSqr(Vec3.atBottomCenterOf(spot)) <= WAIT_SQ;
    }

    /** The block in front of the open face of his Ancient Hollow, while it is loaded. */
    private Optional<BlockPos> frontOfHollow() {
        return gnome.hollow().filter(pos -> gnome.level().isLoaded(pos)).flatMap(pos -> {
            BlockState state = gnome.level().getBlockState(pos);
            return state.hasProperty(AncientHollowBlock.FACING)
                    ? Optional.of(Gnome.standingSpot(gnome.level(), pos.relative(state.getValue(AncientHollowBlock.FACING))))
                    : Optional.empty();
        });
    }
}
