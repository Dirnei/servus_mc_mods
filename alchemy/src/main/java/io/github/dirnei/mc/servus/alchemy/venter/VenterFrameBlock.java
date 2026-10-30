package io.github.dirnei.mc.servus.alchemy.venter;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block of the Venter Equinus' frame around the Cucurbit: a Dung Bed or the Bed Hatch. While formed it
 * draws its {@link #PIECE} of the frame with its outward side toward {@link #FACING}, so its Cucurbit
 * stands one step back from that side, and for a corner also one step along the frame.
 */
public abstract class VenterFrameBlock extends Block {

    /** The part of the bed's frame a formed block draws. */
    public enum FramePiece implements StringRepresentable {
        EDGE, CORNER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<FramePiece> PIECE = EnumProperty.create("piece", FramePiece.class);

    private static final VoxelShape FORMED_SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    protected VenterFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(FACING, Direction.NORTH).setValue(PIECE, FramePiece.EDGE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FACING, PIECE);
    }

    /** The Cucurbit of a formed frame block. */
    public static BlockPos cucurbit(BlockPos pos, BlockState state) {
        Direction out = state.getValue(FACING);
        BlockPos edge = pos.relative(out.getOpposite());
        return state.getValue(PIECE) == FramePiece.CORNER ? edge.relative(out.getCounterClockWise().getOpposite()) : edge;
    }

    /** The shape of this block while it stands alone. */
    protected abstract VoxelShape plainShape(BlockState state);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? FORMED_SHAPE : plainShape(state);
    }

    /** The Journal forms the bed; bedding and rot go in, as on every block of the bed. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        return VenterStructure.use(stack, level, pos, state, player, hand);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        VenterStructure.broken(level, state, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }
}
