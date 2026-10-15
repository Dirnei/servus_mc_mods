package io.github.dirnei.mc.servus.alchemy.salamander;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/** Ground navigation that also walks over lava and through fire, as the Strider's does. */
final class SalamanderNavigation extends GroundPathNavigation {

    SalamanderNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        nodeEvaluator = new WalkNodeEvaluator();
        return new PathFinder(nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean hasValidPathType(PathType pathType) {
        return pathType == PathType.LAVA || pathType == PathType.FIRE || pathType == PathType.FIRE_IN_NEIGHBOR
                || super.hasValidPathType(pathType);
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.LAVA) || super.isStableDestination(pos);
    }
}
