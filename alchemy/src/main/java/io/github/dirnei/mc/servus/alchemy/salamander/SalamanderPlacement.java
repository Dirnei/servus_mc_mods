package io.github.dirnei.mc.servus.alchemy.salamander;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

/** A salamander spawns either in lava, like the Strider, or on the ground, like most creatures. */
public final class SalamanderPlacement implements SpawnPlacementType {

    public static final SalamanderPlacement INSTANCE = new SalamanderPlacement();

    private SalamanderPlacement() {
    }

    @Override
    public boolean isSpawnPositionOk(LevelReader level, BlockPos pos, @Nullable EntityType<?> type) {
        return SpawnPlacementTypes.IN_LAVA.isSpawnPositionOk(level, pos, type)
                || SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, pos, type);
    }

    @Override
    public BlockPos adjustSpawnPosition(LevelReader level, BlockPos candidate) {
        return level.getFluidState(candidate).is(FluidTags.LAVA)
                ? candidate : SpawnPlacementTypes.ON_GROUND.adjustSpawnPosition(level, candidate);
    }
}
