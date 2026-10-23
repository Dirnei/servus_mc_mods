package io.github.dirnei.mc.servus.shuttles.gantry;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.item.PotatoInspectable;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.menu.FluidGantryMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

/**
 * One of the five parts of a Fluid Gantry (see {@link GantryLayout}). The
 * {@link GantryPart#LEFT_LOW} part is the master with the block entity; the
 * others forward clicks to it. Removing any part removes the whole gantry and
 * the master drops the gantry item. {@link #AXIS} is the run of the rail the
 * gantry stands over; {@link #MODE}, {@link StorageNodeBlocks#ONLINE} and
 * {@link StorageNodeBlocks#ACTIVITY} are the master's, copied onto the beam
 * for its sign.
 */
public class FluidGantryBlock extends BaseEntityBlock implements Wrenchable, PotatoInspectable {

    public static final MapCodec<FluidGantryBlock> CODEC = simpleCodec(FluidGantryBlock::new);
    public static final EnumProperty<GantryPart> PART = EnumProperty.create("part", GantryPart.class);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final EnumProperty<GantryMode> MODE = EnumProperty.create("mode", GantryMode.class);

    private static final VoxelShape BEAM_X = Shapes.or(Block.box(0, 9, 2, 16, 16, 14), Block.box(6, 2.5, 6, 10, 9, 10));
    private static final VoxelShape BEAM_Z = Shapes.or(Block.box(2, 9, 0, 14, 16, 16), Block.box(6, 2.5, 6, 10, 9, 10));

    public FluidGantryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, GantryPart.LEFT_LOW).setValue(AXIS, Direction.Axis.Z)
                .setValue(MODE, GantryMode.STORAGE).setValue(StorageNodeBlocks.ONLINE, false)
                .setValue(StorageNodeBlocks.ACTIVITY, StorageNodeBlocks.Activity.IDLE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, AXIS, MODE, StorageNodeBlocks.ONLINE, StorageNodeBlocks.ACTIVITY);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == GantryPart.LEFT_LOW ? new FluidGantryBlockEntity(pos, state) : null;
    }

    /** The rail's run as the layout names it. */
    public static GantryLayout.Run run(Direction.Axis axis) {
        return axis == Direction.Axis.X ? GantryLayout.Run.X : GantryLayout.Run.Z;
    }

    /** Every part's position of the gantry over the rail at {@code rail}. */
    public static Map<GantryLayout.Part, GantryLayout.Vec> parts(BlockPos rail, Direction.Axis axis) {
        return GantryLayout.parts(vec(rail), run(axis));
    }

    /** The position of the rail the part {@code state} at {@code pos} stands beside or over. */
    public static BlockPos railPos(BlockPos pos, BlockState state) {
        return pos(GantryLayout.rail(state.getValue(PART).layout(), vec(pos), run(state.getValue(AXIS))));
    }

    /** The master's position of the gantry the part {@code state} at {@code pos} belongs to. */
    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        return pos(GantryLayout.master(state.getValue(PART).layout(), vec(pos), run(state.getValue(AXIS))));
    }

    /** The position of one part of the gantry the part {@code state} at {@code pos} belongs to. */
    public static BlockPos partPos(BlockPos pos, BlockState state, GantryPart part) {
        return pos(parts(railPos(pos, state), state.getValue(AXIS)).get(part.layout()));
    }

    /** The master of the gantry the block at {@code pos} belongs to, or null when it is not loaded or gone. */
    public static @Nullable FluidGantryBlockEntity master(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidGantryBlock)) {
            return null;
        }
        BlockPos master = masterPos(pos, state);
        if (level instanceof Level world && !world.isLoaded(master)) {
            return null;
        }
        return level.getBlockEntity(master) instanceof FluidGantryBlockEntity gantry
                && gantry.getBlockState().getValue(AXIS) == state.getValue(AXIS) ? gantry : null;
    }

    static GantryLayout.Vec vec(BlockPos pos) {
        return new GantryLayout.Vec(pos.getX(), pos.getY(), pos.getZ());
    }

    static BlockPos pos(GantryLayout.Vec vec) {
        return new BlockPos(vec.x(), vec.y(), vec.z());
    }

    /** Whether {@code state} is the part {@code part} of a gantry over a rail running along {@code axis}. */
    private boolean isPart(BlockState state, GantryPart part, Direction.Axis axis) {
        return state.is(this) && state.getValue(PART) == part && state.getValue(AXIS) == axis;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(PART) != GantryPart.BEAM) {
            return Shapes.block();
        }
        return state.getValue(AXIS) == Direction.Axis.X ? BEAM_X : BEAM_Z;
    }

    /** A part whose master is gone turns into air, so a gantry never stays half built. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(PART) != GantryPart.LEFT_LOW
                && !isPart(level.getBlockState(masterPos(pos, state)), GantryPart.LEFT_LOW, state.getValue(AXIS))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** A creative player breaking any part gets no item; in survival the master drops it. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            FluidGantryBlockEntity master = master(level, pos);
            if (master != null) {
                master.dropNothing();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** One part went away: the rest of the gantry goes with it. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos rail = railPos(pos, state);
        Direction.Axis axis = state.getValue(AXIS);
        for (Map.Entry<GantryLayout.Part, GantryLayout.Vec> entry : parts(rail, axis).entrySet()) {
            BlockPos at = pos(entry.getValue());
            if (!at.equals(pos) && isPart(level.getBlockState(at), GantryPart.of(entry.getKey()), axis)) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** The master copies its mode and lamps onto the beam, whose sign shows them. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (state.getValue(PART) != GantryPart.LEFT_LOW || level.isClientSide()) {
            return;
        }
        BlockPos beamPos = partPos(pos, state, GantryPart.BEAM);
        BlockState beam = level.getBlockState(beamPos);
        if (!isPart(beam, GantryPart.BEAM, state.getValue(AXIS))) {
            return;
        }
        BlockState shown = beam.setValue(MODE, state.getValue(MODE)).setValue(StorageNodeBlocks.ONLINE, state.getValue(StorageNodeBlocks.ONLINE))
                .setValue(StorageNodeBlocks.ACTIVITY, state.getValue(StorageNodeBlocks.ACTIVITY));
        if (shown != beam) {
            level.setBlock(beamPos, shown, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Tells pipes and other mods beside every part of the gantry to look at it again: capability caches
     * see the new handler, and the neighbour update lets ducts redraw their arms towards the parts.
     */
    public static void invalidateCapabilities(Level level, BlockPos pos, BlockState state) {
        for (GantryLayout.Vec part : parts(railPos(pos, state), state.getValue(AXIS)).values()) {
            BlockPos at = pos(part);
            level.invalidateCapabilities(at);
            level.updateNeighborsAt(at, state.getBlock());
        }
    }

    /** What a pipe on a face of a leg sees: the master's handler for its mode; the beam takes no pipes. */
    public static @Nullable ResourceHandler<FluidResource> pipeHandler(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof FluidGantryBlock) || state.getValue(PART) == GantryPart.BEAM) {
            return null;
        }
        FluidGantryBlockEntity master = master(level, pos);
        return master == null ? null : master.pipeHandler();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof DataPotatoItem) {
            return InteractionResult.PASS;
        }
        if (Transfer.isFluidContainer(stack)) {
            FluidGantryBlockEntity master = master(level, pos);
            if (!level.isClientSide() && master != null) {
                // ItemAccess keeps the container of a creative player, like a vanilla bucket.
                FluidUtil.interactWithFluidHandler(player, hand, pos, master.manualHandler());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        FluidGantryBlockEntity master = master(level, pos);
        return master == null ? List.of() : GantryReport.of(master);
    }

    /** The empty hand, or any item that is no fluid container, opens the master's GUI. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            FluidGantryBlockEntity master = master(level, pos);
            if (master != null) {
                player.openMenu(master, buf -> FluidGantryMenu.writeOpening(buf, master));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Sneaking with the carrot dismantles the whole gantry into its item. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            FluidGantryBlockEntity master = master(level, context.getClickedPos());
            level.removeBlock(master != null ? master.getBlockPos() : context.getClickedPos(), false);
        }
        return InteractionResult.SUCCESS;
    }
}
