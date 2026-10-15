package io.github.dirnei.mc.servus.alchemy.gnome;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** An Old Miner strolls a few blocks around, on about the level he stands on; his home keeps him near his Hollow. */
public class WanderNearHollowGoal extends RandomStrollGoal {

    private static final double SPEED = 0.7;
    private static final int INTERVAL = 80;
    private static final int REACH = 4;
    private static final int CLIMB = 1;

    private final Gnome gnome;

    public WanderNearHollowGoal(Gnome gnome) {
        super(gnome, SPEED, INTERVAL);
        this.gnome = gnome;
    }

    @Override
    public boolean canUse() {
        return gnome.kind() == GnomeKind.MINER && super.canUse();
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        return DefaultRandomPos.getPos(mob, REACH, CLIMB);
    }
}
