package io.github.dirnei.mc.servus.shuttles.fluidrack;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.item.PotatoInspectable;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.RackFacing;
import io.github.dirnei.mc.servus.shuttles.rack.RackRow;
import io.github.dirnei.mc.servus.shuttles.rack.RackSection;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

/**
 * Fluid Rack: a bay that joins the bays beside it into a section of up to
 * {@code bays_per_section}, like the Heavy Duty Rack, decided at placement.
 * A section holds one fluid, shared evenly by its bays, and has one port on
 * its {@link #PORT} bay. It opens no GUI: buckets fill and empty it by hand,
 * pipes reach it only while no rail stands in front of the port, and
 * tankers serve it through the port.
 */
public class FluidRackBlock extends BaseEntityBlock implements Wrenchable, PotatoInspectable {

    public static final MapCodec<FluidRackBlock> CODEC = simpleCodec(FluidRackBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Joined to the bay on the counter clockwise side: no post there, the frame runs through. */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    /** Joined to the bay on the clockwise side: no post there, the frame runs through. */
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    /** The bay carries the section's port. */
    public static final BooleanProperty PORT = BooleanProperty.create("port");

    private static final int MAX_SECTION_WALK = 64;

    public FluidRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LEFT, false).setValue(RIGHT, false).setValue(PORT, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT, PORT);
    }

    private static boolean sameRow(BlockState state, BlockState neighbour) {
        return neighbour.getBlock() instanceof FluidRackBlock && neighbour.getValue(FACING) == state.getValue(FACING);
    }

    private static Identifier woodAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FluidRackBlockEntity rack ? rack.wood() : FluidRackBlockEntity.DEFAULT_WOOD;
    }

    /** Same row, same wood and a fluid the bay's section takes: only such bays join. */
    private static boolean sameBay(BlockState state, Identifier wood, @Nullable FluidKey fluid, BlockGetter level, BlockPos pos) {
        return sameRow(state, level.getBlockState(pos)) && wood.equals(woodAt(level, pos))
                && FluidRackSection.canJoin(sectionFluid(level, pos), fluid);
    }

    /** The fluid the section at that position holds, null when empty. */
    private static @Nullable FluidKey sectionFluid(BlockGetter level, BlockPos pos) {
        for (FluidRackBlockEntity bay : bays(level, pos)) {
            if (!bay.share().isEmpty()) {
                return FluidKey.of(bay.share());
            }
        }
        return null;
    }

    /**
     * Bays in the section that starts at the neighbour in {@code direction}
     * and extends away from {@code pos}; zero when that bay may not be joined.
     */
    private static int sectionSize(BlockState state, Identifier wood, @Nullable FluidKey fluid, BlockGetter level, BlockPos pos,
                                   Direction direction) {
        BlockPos start = pos.relative(direction);
        if (!sameBay(state, wood, fluid, level, start)) {
            return 0;
        }
        return section(level, start).size();
    }

    /**
     * The bays of the section the rack at {@code pos} belongs to, from left to
     * right as a player standing in front of it sees them. Empty when no
     * Fluid Rack stands there.
     */
    public static List<BlockPos> section(BlockGetter level, BlockPos pos) {
        BlockState start = level.getBlockState(pos);
        if (!(start.getBlock() instanceof FluidRackBlock)) {
            return List.of();
        }
        // The flags name the rack's own sides; a player facing the front sees them swapped.
        Direction viewerRight = start.getValue(FACING).getCounterClockWise();
        int[] offsets = RackSection.bays(
                offset -> joined(start, level.getBlockState(pos.relative(viewerRight, offset)), RIGHT),
                offset -> joined(start, level.getBlockState(pos.relative(viewerRight, offset)), LEFT),
                MAX_SECTION_WALK);
        List<BlockPos> bays = new ArrayList<>(offsets.length);
        for (int offset : offsets) {
            bays.add(pos.relative(viewerRight, offset));
        }
        return List.copyOf(bays);
    }

    /** The block entities of the section at that position, from left to right. */
    public static List<FluidRackBlockEntity> bays(BlockGetter level, BlockPos pos) {
        List<FluidRackBlockEntity> bays = new ArrayList<>();
        for (BlockPos bay : section(level, pos)) {
            if (level.getBlockEntity(bay) instanceof FluidRackBlockEntity rack) {
                bays.add(rack);
            }
        }
        return bays;
    }

    /** The port bay of the section at that position, or null when no Fluid Rack stands there. */
    public static @Nullable BlockPos port(BlockGetter level, BlockPos pos) {
        List<BlockPos> bays = section(level, pos);
        return bays.isEmpty() ? null : bays.get(FluidRackSection.portIndex(bays.size()));
    }

    private static boolean joined(BlockState start, BlockState state, BooleanProperty flag) {
        return sameRow(start, state) && state.getValue(flag);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockGetter level = context.getLevel();
        Direction facing = RackFacing.of(context, block -> block instanceof FluidRackBlock);
        BlockState state = defaultBlockState().setValue(FACING, facing);
        ItemStack stack = context.getItemInHand();
        Identifier wood = FluidRackBlockEntity.woodOf(stack);
        FluidKey fluid = FluidRackBlockEntity.fluidOf(stack).map(FluidKey::of).orElse(null);
        int max = ShuttlesStatsManager.INSTANCE.fluidRackBaysPerSection();
        int leftSize = sectionSize(state, wood, fluid, level, pos, facing.getCounterClockWise());
        int rightSize = sectionSize(state, wood, fluid, level, pos, facing.getClockWise());
        if (leftSize > 0 && rightSize > 0 && !FluidRackSection.canJoin(sectionFluid(level, pos.relative(facing.getCounterClockWise())),
                sectionFluid(level, pos.relative(facing.getClockWise())))) {
            // Two sections of different fluids would meet in this bay: it joins the left one only.
            rightSize = 0;
        }
        Boolean leftBelow = joinFromBelow(state, level, pos, facing.getCounterClockWise(), LEFT, RIGHT);
        Boolean rightBelow = joinFromBelow(state, level, pos, facing.getClockWise(), RIGHT, LEFT);
        boolean joinLeft = leftBelow != null ? leftBelow && leftSize > 0 : RackRow.joinLeft(leftSize, max);
        boolean joinRight = rightBelow != null ? rightBelow && rightSize > 0 : RackRow.joinRight(joinLeft, leftSize, rightSize, max);
        return state.setValue(LEFT, joinLeft).setValue(RIGHT, joinRight).setValue(PORT, false);
    }

    /**
     * The row below decides where sections split: the rack under this bay, or
     * the rack under the neighbour on that side, says whether the join exists.
     * Null when no rack stands under either.
     */
    private static Boolean joinFromBelow(BlockState state, BlockGetter level, BlockPos pos, Direction side,
                                         BooleanProperty ownFlag, BooleanProperty neighbourFlag) {
        BlockState below = level.getBlockState(pos.below());
        if (sameRow(state, below)) {
            return below.getValue(ownFlag);
        }
        BlockState belowNeighbour = level.getBlockState(pos.relative(side).below());
        if (sameRow(state, belowNeighbour)) {
            return belowNeighbour.getValue(neighbourFlag);
        }
        return null;
    }

    /**
     * Mirrors a fresh bay's joins onto its neighbours, since a join is mutual.
     * The section settles on the next tick, once a placed item's share and
     * zone are on the block entity.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || oldState.is(this)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        mirror(level, pos.relative(facing.getCounterClockWise()), RIGHT, state.getValue(LEFT), state);
        mirror(level, pos.relative(facing.getClockWise()), LEFT, state.getValue(RIGHT), state);
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        settle(level, pos);
    }

    private static void mirror(Level level, BlockPos neighbourPos, BooleanProperty flag, boolean joined, BlockState placed) {
        BlockState neighbour = level.getBlockState(neighbourPos);
        if (joined && sameRow(placed, neighbour) && !neighbour.getValue(flag)) {
            level.setBlock(neighbourPos, neighbour.setValue(flag, true), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Brings the section at {@code pos} in order after it changed: one zone
     * for all bays (the zone of the bays already there wins), the fluid split
     * into even shares, the port on its bay, links and pipe access
     * looked at again and the warehouse told. Server only, event driven.
     */
    public static void settle(Level level, BlockPos pos) {
        List<BlockPos> positions = section(level, pos);
        List<FluidRackBlockEntity> bays = bays(level, pos);
        if (bays.isEmpty()) {
            return;
        }
        shareZone(bays, level.getBlockEntity(pos) instanceof FluidRackBlockEntity placed ? placed : null);
        FluidRackBlockEntity.rebalance(bays);
        int port = FluidRackSection.portIndex(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            BlockPos bay = positions.get(i);
            BlockState state = level.getBlockState(bay);
            if (state.getBlock() instanceof FluidRackBlock && state.getValue(PORT) != (i == port)) {
                level.setBlock(bay, state.setValue(PORT, i == port), Block.UPDATE_ALL);
            }
        }
        for (FluidRackBlockEntity bay : bays) {
            bay.refreshLinks();
            bay.sectionChanged();
        }
        refreshDock(level, positions);
    }

    /** Gives every bay the zone of the first other zoned bay, else the zone of {@code placed}. */
    private static void shareZone(List<FluidRackBlockEntity> bays, @Nullable FluidRackBlockEntity placed) {
        int zone = ZoneList.NONE;
        for (FluidRackBlockEntity bay : bays) {
            if (bay != placed && bay.zoneDefinition() != null) {
                zone = bay.zoneId();
                break;
            }
        }
        if (zone == ZoneList.NONE && placed != null && placed.zoneDefinition() != null) {
            zone = placed.zoneId();
        }
        for (FluidRackBlockEntity bay : bays) {
            bay.setZone(zone);
        }
    }

    /** Puts every bay of the section at that position into the zone with that id, or clears it with {@link ZoneList#NONE}. */
    public static void stampSection(Level level, BlockPos pos, int zone) {
        for (FluidRackBlockEntity bay : bays(level, pos)) {
            bay.setZone(zone);
        }
        for (FluidRackBlockEntity bay : bays(level, pos)) {
            bay.sectionChanged();
        }
    }

    /**
     * Only ever clears side joins: a neighbour that is gone or turned away
     * takes its join with it. Woods never change in place, so this skips the
     * wood check, which would misread a rack still being placed as oak.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction facing = state.getValue(FACING);
        boolean same = sameRow(state, neighborState);
        if (direction == facing.getCounterClockWise()) {
            return same ? state : state.setValue(LEFT, false);
        }
        if (direction == facing.getClockWise()) {
            return same ? state : state.setValue(RIGHT, false);
        }
        return state;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidRackBlockEntity(pos, state);
    }

    /** A fluid container on the front fills or empties the section; every other click passes, so no GUI opens. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof HandheldItem
                || stack.getItem() instanceof DataPotatoItem || hitResult.getDirection() != state.getValue(FACING)
                || !Transfer.isFluidContainer(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidRackBlockEntity rack) {
            // ItemAccess keeps the container of a creative player, like a vanilla bucket.
            FluidUtil.interactWithFluidHandler(player, hand, pos, rack.sectionHandler());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        return FluidRackReport.of(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide() && state.getValue(PORT) && level.getBlockEntity(pos) instanceof FluidRackBlockEntity rack
                && rack.frontRailChanged()) {
            refreshDock(level, section(level, pos));
        }
    }

    /** Whether a rail stands in front of the bay at that position. */
    public static boolean railInFront(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof FluidRackBlock
                && level.getBlockState(pos.relative(state.getValue(FACING))).getBlock() instanceof RailBlock;
    }

    /**
     * The section's pipe access may have changed: every bay drops its cached
     * capability and tells its neighbours, so ducts recompute their arms.
     * Event driven, never polled.
     */
    public static void refreshDock(Level level, List<BlockPos> bays) {
        for (BlockPos bay : bays) {
            level.invalidateCapabilities(bay);
            level.updateNeighborsAt(bay, level.getBlockState(bay).getBlock());
        }
    }

    /** A creative player's break drops nothing. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative() && level.getBlockEntity(pos) instanceof FluidRackBlockEntity rack) {
            rack.dropNothing();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** A bay that leaves took its share with it: the sections beside it settle again. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Direction facing = state.getValue(FACING);
        for (BlockPos side : List.of(pos.relative(facing.getCounterClockWise()), pos.relative(facing.getClockWise()))) {
            if (level.getBlockState(side).getBlock() instanceof FluidRackBlock) {
                settle(level, side);
            }
        }
    }

    /** Sneaking dismantles the bay into an item that keeps its share, wood and zone; the carrot never turns a rack. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean dismantle = player != null && player.isShiftKeyDown();
        if (dismantle && !level.isClientSide() && level.getBlockEntity(pos) instanceof FluidRackBlockEntity rack) {
            ItemStack drop = rack.dropItem();
            rack.dropNothing();
            Block.popResource(level, pos, drop);
            level.removeBlock(pos, false);
        }
        return dismantle ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
