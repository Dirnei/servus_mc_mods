package io.github.dirnei.mc.servus.logistics.duct;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Utility duct block. The six connection flags are the union of the per line
 * arms the block entity computes from its lines, side modes and neighbours.
 */
public class UtilityDuctBlock extends ConnectableBlock {

    public static final MapCodec<UtilityDuctBlock> CODEC = simpleCodec(UtilityDuctBlock::new);

    private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public UtilityDuctBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UtilityDuctBlockEntity(pos, state);
    }

    /** A side connects when any line has an arm there; see {@link UtilityDuctBlockEntity#computeArm}. */
    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        BlockPos pos = neighborPos.relative(direction.getOpposite());
        if (!(level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct)) {
            return false;
        }
        for (Line line : Line.VALUES) {
            if (duct.computeArm(line, direction)) {
                return true;
            }
        }
        return false;
    }

    /** A neighbour was placed, removed or changed: the block entity refreshes its arms. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            duct.neighbourChanged();
        }
    }

    /** Recomputes the six connection flags, after lines, modes or neighbours changed. */
    public void refreshConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof UtilityDuctBlock)) {
            return;
        }
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            updated = updated.setValue(property(direction), connectsTo(level, neighborPos, level.getBlockState(neighborPos), direction));
        }
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    /** Wrench, line and facade items handle the click themselves instead of opening the menu. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof LineItem || stack.getItem() instanceof FacadeItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * The shape follows the lines: a centre as thick as the line count and,
     * per side, an arm as thick as the lines connected there. A facade is a
     * full block. Without a block entity the state flags decide, as in Lib.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            if (duct.facade() != null) {
                return Shapes.block();
            }
            return shape(duct.linesMask(), duct.armMasks());
        }
        return super.getShape(state, level, pos, context);
    }

    /** The shape for a line set and the per line arm masks (indexed by line ordinal), cached. */
    public static VoxelShape shape(int linesMask, int[] arms) {
        int key = linesMask & DuctParts.ALL;
        int all = 0;
        for (int i = 0; i < arms.length && i < Line.VALUES.length; i++) {
            key |= (arms[i] & 63) << (Line.VALUES.length + 6 * i);
            all |= arms[i] & 63;
        }
        int armsUnion = all;
        return SHAPES.computeIfAbsent(key, k -> {
            VoxelShape shape = box(DuctShapes.center(DuctShapes.centerRadius(Integer.bitCount(linesMask & DuctParts.ALL), armsUnion)));
            for (Direction direction : Direction.values()) {
                int armLines = armLines(arms, direction);
                if (armLines > 0) {
                    shape = Shapes.or(shape, box(DuctShapes.arm(direction.ordinal(), DuctShapes.radius(armLines))));
                }
            }
            return shape;
        });
    }

    /** How many lines have an arm on that side. */
    private static int armLines(int[] arms, Direction direction) {
        int bit = DuctModelData.bit(direction);
        int count = 0;
        for (int i = 0; i < arms.length && i < Line.VALUES.length; i++) {
            if ((arms[i] & bit) != 0) {
                count++;
            }
        }
        return count;
    }

    private static VoxelShape box(DuctShapes.Box box) {
        return Block.box(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1());
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side, BlockState queryState, BlockPos queryPos) {
        if (level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && duct.facade() != null) {
            return duct.facade();
        }
        return state;
    }

    /** A bare hand opens nothing; the endpoint GUI is opened with the Configuration Carrot. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            for (Line line : duct.lines()) {
                Block.popResource(level, pos, new ItemStack(ServusLogisticsItems.lineItem(line)));
            }
            ItemStack facade = duct.removeFacade();
            if (!facade.isEmpty()) {
                Block.popResource(level, pos, facade);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Sneak wrench takes the facade off first, then removes the duct; lines
     * drop through {@link #onRemove}. Without sneaking, a click on an arm that
     * ends at an inventory or machine opens that side's endpoint GUI; the
     * centre, an arm toward another duct and a facaded duct open nothing.
     */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && duct.facade() != null) {
                    Block.popResource(level, pos, duct.removeFacade());
                } else {
                    level.removeBlock(pos, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (player == null || !(level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) || duct.facade() != null) {
            return InteractionResult.PASS;
        }
        Direction side = hitArm(duct, context.getClickLocation(), pos);
        if (side == null || !duct.isEndpoint(side)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.openMenu(duct.menuProvider(side), buf -> buf.writeBlockPos(pos).writeEnum(side));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The side whose arm box contains the click location, or null for the centre or nothing. */
    private static Direction hitArm(UtilityDuctBlockEntity duct, Vec3 location, BlockPos pos) {
        int[] armRadius = new int[6];
        for (Direction direction : Direction.values()) {
            int lines = armLines(duct.armMasks(), direction);
            armRadius[direction.ordinal()] = lines == 0 ? 0 : DuctShapes.radius(lines);
        }
        int centerRadius = DuctShapes.radius(duct.lines().size());
        int side = ArmHit.hit((location.x - pos.getX()) * 16, (location.y - pos.getY()) * 16, (location.z - pos.getZ()) * 16,
                centerRadius, armRadius);
        return side >= 0 ? Direction.values()[side] : null;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && !duct.lines().isEmpty()) {
            return new ItemStack(ServusLogisticsItems.lineItem(duct.lines().iterator().next()));
        }
        return new ItemStack(ServusLogisticsItems.ITEM_PIPE.get());
    }
}
