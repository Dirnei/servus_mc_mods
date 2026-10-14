package io.github.dirnei.mc.servus.foundation.logistics.duct;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.data.DataSummary;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Utility duct block. The six connection flags are the union of the per line
 * arms the block entity computes from its lines, side modes and neighbours.
 */
public class UtilityDuctBlock extends ConnectableBlock {

    public static final MapCodec<UtilityDuctBlock> CODEC = simpleCodec(UtilityDuctBlock::new);

    /** Cached shapes by lines, per line arms and panels packed into a long, plus the pose's sides and the faces. */
    private record ShapeKey(long linesArmsPanels, int poseSides, int faces) {
    }

    private static final Map<ShapeKey, VoxelShape> SHAPES = new ConcurrentHashMap<>();

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
    protected boolean connectsTo(LevelReader level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
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
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof LineItem || stack.getItem() instanceof FacadeItem) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * The shape follows the lines and the resting pose: a centre as thick as
     * the line count and, per side, an arm as thick as the lines connected
     * there with its step, plus a slab or a frame per panel. Without a block
     * entity the state flags decide.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            return shape(duct.linesMask(), duct.armMasks(), duct.solidPanelMask(), duct.hollowPanelMask(), duct.pose(), duct.poseFaces());
        }
        return super.getShape(state, level, pos, context);
    }

    /**
     * The shape for a line set, the per line arm masks, the six bit solid and
     * hollow panel masks, the resting pose and the faces (see {@link DuctPose}), cached.
     */
    public static VoxelShape shape(int linesMask, int[] arms, int solidPanels, int hollowPanels, int pose, int faces) {
        long key = linesMask & DuctParts.ALL;
        int all = 0;
        int shift = Line.VALUES.length;
        for (int i = 0; i < arms.length && i < Line.VALUES.length; i++) {
            key |= (long) (arms[i] & 63) << (shift + 6 * i);
            all |= arms[i] & 63;
        }
        shift += 6 * Line.VALUES.length;
        key |= (long) (solidPanels & 63) << shift;
        key |= (long) (hollowPanels & 63) << (shift + 6);
        int centerRadius = DuctShapes.centerRadius(Integer.bitCount(linesMask & DuctParts.ALL), all);
        return SHAPES.computeIfAbsent(new ShapeKey(key, DuctPose.sides(pose), faces), k -> {
            VoxelShape shape = box(DuctShapes.center(centerRadius, pose));
            for (Direction direction : Direction.values()) {
                int armLines = armLines(arms, direction);
                if (armLines > 0) {
                    for (DuctShapes.Box arm : DuctShapes.arm(direction.ordinal(), DuctShapes.radius(armLines), centerRadius, pose, faces)) {
                        shape = Shapes.or(shape, box(arm));
                    }
                }
                int bit = DuctModelData.bit(direction);
                if (((solidPanels | hollowPanels) & bit) != 0) {
                    for (DuctShapes.Box panel : DuctShapes.panel(direction.ordinal(), (solidPanels & bit) == 0)) {
                        shape = Shapes.or(shape, box(panel));
                    }
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
    public BlockState getAppearance(BlockState state, BlockAndLightGetter level, BlockPos pos, Direction side, BlockState queryState, BlockPos queryPos) {
        if (level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && duct.closed(side)) {
            return duct.panel(side);
        }
        return state;
    }

    /**
     * A bare hand opens nothing; the endpoint GUI is opened with the Configuration Carrot.
     * On a duct with the data line an empty hand shows the data network summary.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct)
                || !duct.has(Line.DATA)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel) {
            duct.dataNetwork().ifPresent(network -> player.sendOverlayMessage(DataSummary.of(network, pos, duct.dataTier())));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Sneak wrench on a panel takes that panel off; anywhere else it removes
     * the duct, whose lines and panels drop when its block entity is removed. Without
     * sneaking, a click on an arm that ends at an inventory or machine opens
     * that side's endpoint GUI; the centre, an arm toward another duct and a
     * panel open nothing.
     */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                UtilityDuctBlockEntity duct = level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity d ? d : null;
                Direction panel = duct == null ? null : hitPanel(duct, context.getClickLocation(), pos, context.getClickedFace());
                if (panel != null) {
                    Block.popResource(level, pos, duct.removePanel(panel));
                } else {
                    level.removeBlock(pos, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (player == null || !(level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct)
                || hitPanel(duct, context.getClickLocation(), pos, context.getClickedFace()) != null) {
            return InteractionResult.PASS;
        }
        Direction side = hitArm(duct, context.getClickLocation(), pos);
        if (side == null || !duct.isEndpoint(side)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.openMenu(duct.menuProvider(side), buf -> buf.writeBlockPos(pos).writeEnum(side));
        }
        return InteractionResult.SUCCESS;
    }

    /** The side whose panel contains the click location, or null. */
    private static Direction hitPanel(UtilityDuctBlockEntity duct, Vec3 location, BlockPos pos, Direction clickedFace) {
        int side = PanelHit.face((location.x - pos.getX()) * 16, (location.y - pos.getY()) * 16, (location.z - pos.getZ()) * 16,
                clickedFace.ordinal(), duct.solidPanelMask(), duct.hollowPanelMask());
        return side == PanelHit.NONE ? null : Direction.values()[side];
    }

    /** The side whose arm box contains the click location, or null for the centre or nothing. */
    private static Direction hitArm(UtilityDuctBlockEntity duct, Vec3 location, BlockPos pos) {
        int[] armRadius = new int[6];
        for (Direction direction : Direction.values()) {
            int lines = armLines(duct.armMasks(), direction);
            armRadius[direction.ordinal()] = lines == 0 ? 0 : DuctShapes.radius(lines);
        }
        int centerRadius = DuctShapes.centerRadius(duct.lines().size(), duct.connections());
        int side = ArmHit.hit((location.x - pos.getX()) * 16, (location.y - pos.getY()) * 16, (location.z - pos.getZ()) * 16,
                centerRadius, armRadius, duct.pose(), duct.poseFaces());
        return side >= 0 ? Direction.values()[side] : null;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        if (level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && !duct.lines().isEmpty()) {
            return new ItemStack(duct.lineItem(duct.lines().iterator().next()));
        }
        return new ItemStack(ServusLogisticsItems.ITEM_PIPE.get());
    }
}
