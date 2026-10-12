package io.github.dirnei.mc.servus.foundation.machine.pump;

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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Electric Pump: the Burner Pump's electric sibling, a beam engine two
 * blocks tall. The lower half has the motor housing with copper windings and
 * the terminal box; the upper half carries the crank wheel that drives the
 * beam through the con rod, the beam's post and the fan grille. Now and then
 * a spark jumps at the motor while it works.
 */
public class ElectricPumpBlock extends BeamPumpBlock {

    public static final MapCodec<ElectricPumpBlock> CODEC = simpleCodec(ElectricPumpBlock::new);

    /** One spark in this many client ticks on average while working. */
    private static final int SPARK_CHANCE = 6;

    private static final Map<Direction, VoxelShape> LOWER = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> UPPER = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            LOWER.put(facing, shapeOf(ElectricPumpParts.lowerParts(), facing));
            UPPER.put(facing, shapeOf(ElectricPumpParts.upperParts(), facing));
        }
    }

    public ElectricPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.ELECTRIC_PUMP_ENTITY.get();
    }

    @Override
    protected VoxelShape lowerShape(Direction facing) {
        return LOWER.get(facing);
    }

    @Override
    protected VoxelShape upperShape(Direction facing) {
        return UPPER.get(facing);
    }

    /** Now and then a single spark at the motor brushes while the pump works; never a stream. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isUpper(state) || !state.getValue(LIT) || random.nextInt(SPARK_CHANCE) != 0) {
            return;
        }
        float[] brush = BeamPumpParts.rotatedPoint(ElectricPumpParts.SPARK_X, ElectricPumpParts.SPARK_Z, quarters(state.getValue(FACING)));
        level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + brush[0] / 16.0,
                pos.getY() + ElectricPumpParts.SPARK_Y / 16.0 + random.nextDouble() * 0.05,
                pos.getZ() + brush[1] / 16.0,
                (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05);
    }
}
