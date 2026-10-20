package io.github.dirnei.mc.servus.foundation.machine.pump;

import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import io.github.dirnei.mc.servus.foundation.machine.PixelBoxes;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Burner Pump: a small steam beam engine two blocks tall. The lower half
 * has the brick firebox and the boiler drum; the upper half carries the
 * cylinder, the beam's post and the chimney, which smokes while it burns.
 */
public class BurnerPumpBlock extends BeamPumpBlock {

    public static final MapCodec<BurnerPumpBlock> CODEC = simpleCodec(BurnerPumpBlock::new);

    /** Symmetric, so one shape fits every facing; the small rod socket takes no space. */
    private static final VoxelShape LOWER = Shapes.or(PartShapes.box(BeamPumpParts.BASE), PartShapes.box(BurnerPumpParts.FIREBOX),
            PartShapes.box(BurnerPumpParts.DRUM_LOWER));
    /** The upper parts per facing; the moving beam and rods take no space. */
    private static final Map<Direction, VoxelShape> UPPER = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            UPPER.put(facing, PartShapes.shapeOf(new float[][] {BurnerPumpParts.DRUM_UPPER, BurnerPumpParts.CYLINDER, BeamPumpParts.POST,
                    BurnerPumpParts.CHIMNEY}, facing));
        }
    }

    public BurnerPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.BURNER_PUMP_ENTITY.get();
    }

    @Override
    protected VoxelShape lowerShape(Direction facing) {
        return LOWER;
    }

    @Override
    protected VoxelShape upperShape(Direction facing) {
        return UPPER.get(facing);
    }

    /** Smoke from the chimney top while the lower half is lit. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isUpper(state) || !state.getValue(LIT) || random.nextFloat() >= 0.5F) {
            return;
        }
        float[] top = PixelBoxes.rotatedPoint(BurnerPumpParts.SMOKE_X, BurnerPumpParts.SMOKE_Z, PartShapes.quarters(state.getValue(FACING)));
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                pos.getX() + top[0] / 16.0 + (random.nextDouble() - 0.5) / 16.0,
                pos.getY() + 1 + BurnerPumpParts.SMOKE_Y / 16.0 + random.nextDouble() * 0.1,
                pos.getZ() + top[1] / 16.0 + (random.nextDouble() - 0.5) / 16.0,
                0.0, 0.05, 0.0);
    }
}
