package io.github.dirnei.mc.servus.alchemy.salamander;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CampfireBlock;

/** A befriended salamander that idles walks into a lit campfire nearby and lies there. */
final class LieInCampfireGoal extends MoveToBlockGoal {

    private static final int SEARCH_RANGE = 8;

    private final Salamander salamander;

    LieInCampfireGoal(Salamander salamander, double speedModifier) {
        super(salamander, speedModifier, SEARCH_RANGE, 2);
        this.salamander = salamander;
    }

    @Override
    public boolean canUse() {
        return salamander.isTame() && !salamander.isOrderedToSit() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !salamander.isOrderedToSit() && super.canContinueToUse();
    }

    @Override
    protected void moveMobToBlock() {
        mob.getNavigation().moveTo(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, speedModifier);
    }

    @Override
    protected BlockPos getMoveToTarget() {
        return blockPos;
    }

    @Override
    public void tick() {
        super.tick();
        salamander.setInSittingPose(isReachedTarget());
    }

    @Override
    public void stop() {
        super.stop();
        salamander.setInSittingPose(salamander.isOrderedToSit());
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return CampfireBlock.isLitCampfire(level.getBlockState(pos));
    }
}
