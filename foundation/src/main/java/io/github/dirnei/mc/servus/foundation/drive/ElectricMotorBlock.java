package io.github.dirnei.mc.servus.foundation.drive;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import io.github.dirnei.mc.servus.foundation.machine.PixelBoxes;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Electric Motor: the Electric Pump's motor on iron feet, with a shaft
 * that drives the block in front of it. {@link #FACING} is the way the shaft
 * points; power comes in at the back. While it works the coupling and fan
 * turn and now and then a spark jumps at the shaft.
 */
public class ElectricMotorBlock extends MachineBlock {

    public static final MapCodec<ElectricMotorBlock> CODEC = simpleCodec(ElectricMotorBlock::new);

    /** One spark in this many client ticks on average while working. */
    private static final int SPARK_CHANCE = 8;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, PartShapes.shapeOf(ElectricMotorParts.shapeParts(), facing));
        }
    }

    public ElectricMotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.ELECTRIC_MOTOR_ENTITY.get();
    }

    /** The shaft faces the clicked block when that side takes drive, else away from the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction shaft = context.getHorizontalDirection();
        if (face.getAxis().isHorizontal() && context.getLevel().getCapability(ServusFoundationCapabilities.DRIVE,
                context.getClickedPos().relative(face.getOpposite()), face) != null) {
            shaft = face.getOpposite();
        }
        return defaultBlockState().setValue(FACING, shaft);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** The server runs the motor; the client turns the shaft and fan while it works. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return super.getTicker(level, state, type);
        }
        return createTickerHelper(type, MachineContent.ELECTRIC_MOTOR_ENTITY.get(),
                (tickLevel, pos, tickState, motor) -> motor.spin().tick(tickState.getValue(LIT)));
    }

    /** Now and then a single spark at the shaft while the motor works; never a stream. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(SPARK_CHANCE) != 0) {
            return;
        }
        float[] spark = PixelBoxes.rotatedPoint(ElectricMotorParts.SPARK_X, ElectricMotorParts.SPARK_Z, PartShapes.quarters(state.getValue(FACING)));
        level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + spark[0] / 16.0,
                pos.getY() + ElectricMotorParts.SPARK_Y / 16.0 + random.nextDouble() * 0.05,
                pos.getZ() + spark[1] / 16.0,
                (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05);
    }
}
