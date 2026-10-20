package io.github.dirnei.mc.servus.brewery.crop;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;

/**
 * A thin wooden pole that stacks into a column of at most {@link HopVine#MAX_HEIGHT}
 * blocks. A Hop Rhizome plants a vine at the foot of the column; the vine climbs
 * the poles on random ticks and bears cones on the parts above the foot, which a
 * right click picks.
 */
public class HopPoleBlock extends Block implements BonemealableBlock {

    public static final MapCodec<HopPoleBlock> CODEC = simpleCodec(HopPoleBlock::new);

    /** The {@link HopStage} of this part by ordinal. */
    public static final IntegerProperty HOP = IntegerProperty.create("hop", 0, HopStage.values().length - 1);
    /** True on the foot part that holds the rhizome. */
    public static final BooleanProperty ROOTED = BooleanProperty.create("rooted");

    /** One growth tick in this many on a part with a vine makes a step. */
    public static final int GROWTH_CHANCE = 4;
    private static final int MIN_GROWTH_LIGHT = 9;

    /** The loot table a right click on a part with cones drops. */
    public static final ResourceKey<LootTable> HARVEST_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "harvest/hop_pole"));

    private static final VoxelShape POLE = Block.column(2.0, 0.0, 16.0);
    private static final VoxelShape WITH_VINE = Block.column(6.0, 0.0, 16.0);

    public HopPoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HOP, HopStage.BARE.ordinal()).setValue(ROOTED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HOP, ROOTED);
    }

    /** The stage of the part in {@code state}. */
    public static HopStage stageOf(BlockState state) {
        return HopStage.values()[state.getValue(HOP)];
    }

    /** {@code state} with the part at {@code stage}. */
    public static BlockState withStage(BlockState state, HopStage stage) {
        return state.setValue(HOP, stage.ordinal());
    }

    // ---------- shape and support ----------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return stageOf(state).hasVine() ? WITH_VINE : POLE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return POLE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return HopVine.canPlaceOn(polesBelow(context.getLevel(), context.getClickedPos())) ? defaultBlockState() : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return isPole(below) || below.is(Blocks.FARMLAND) || canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    // ---------- the column ----------

    private static boolean isPole(BlockState state) {
        return state.getBlock() instanceof HopPoleBlock;
    }

    private static int polesBelow(BlockGetter level, BlockPos pos) {
        int count = 0;
        while (count < HopVine.MAX_HEIGHT && isPole(level.getBlockState(pos.below(count + 1)))) {
            count++;
        }
        return count;
    }

    /** The lowest pole of the column {@code pos} belongs to. */
    public static BlockPos footOf(BlockGetter level, BlockPos pos) {
        return pos.below(Math.min(polesBelow(level, pos), HopVine.MAX_HEIGHT - 1));
    }

    /** The stages of the column standing on {@code foot}, from the foot up. */
    public static List<HopStage> columnFrom(BlockGetter level, BlockPos foot) {
        List<HopStage> column = new ArrayList<>(HopVine.MAX_HEIGHT);
        for (int part = 0; part < HopVine.MAX_HEIGHT; part++) {
            BlockState state = level.getBlockState(foot.above(part));
            if (!isPole(state)) {
                break;
            }
            column.add(stageOf(state));
        }
        return column;
    }

    private static Optional<HopStep> firstGrowth(BlockGetter level, BlockPos pos) {
        return HopVine.firstGrowth(columnFrom(level, footOf(level, pos)));
    }

    private static void apply(Level level, BlockPos foot, HopStep step) {
        BlockPos part = foot.above(step.part());
        BlockState grown = withStage(level.getBlockState(part), step.stage());
        level.setBlock(part, grown, UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, part, GameEvent.Context.of(grown));
    }

    // ---------- growth ----------

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return stageOf(state).hasVine();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getRawBrightness(pos, 0) < MIN_GROWTH_LIGHT) {
            return;
        }
        BlockPos foot = footOf(level, pos);
        List<HopStage> column = columnFrom(level, foot);
        int part = pos.getY() - foot.getY();
        if (part >= column.size()) {
            return;
        }
        Optional<HopStep> step = HopVine.growthAt(column, part);
        if (step.isPresent() && CommonHooks.canCropGrow(level, pos, state, random.nextInt(GROWTH_CHANCE) == 0)) {
            apply(level, foot, step.get());
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return firstGrowth(level, pos).isPresent();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos foot = footOf(level, pos);
        HopVine.firstGrowth(columnFrom(level, foot)).ifPresent(step -> apply(level, foot, step));
    }

    // ---------- planting and picking ----------

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ServusBreweryItems.HOP_RHIZOME.get()) && canPlantAt(level, footOf(level, pos))) {
            if (!level.isClientSide()) {
                plant(level, footOf(level, pos));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BONE_MEAL) && isValidBonemealTarget(level, pos, state)) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /** Whether a rhizome can be planted at the foot pole: it has no vine and stands on farmland or dirt. */
    public static boolean canPlantAt(BlockGetter level, BlockPos foot) {
        BlockState ground = level.getBlockState(foot.below());
        return !stageOf(level.getBlockState(foot)).hasVine() && (ground.is(Blocks.FARMLAND) || ground.is(BlockTags.DIRT));
    }

    private static void plant(Level level, BlockPos foot) {
        BlockState planted = withStage(level.getBlockState(foot), HopStage.YOUNG).setValue(ROOTED, true);
        level.setBlock(foot, planted, UPDATE_ALL);
        level.playSound(null, foot, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, foot, GameEvent.Context.of(planted));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (stageOf(state) != HopStage.CONES) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            dropFromBlockInteractLootTable(serverLevel, HARVEST_LOOT, state, null, null, player,
                    (dropLevel, drop) -> popResource(dropLevel, pos, drop));
            serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                    1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
            BlockState picked = withStage(state, HopStage.GROWN);
            serverLevel.setBlock(pos, picked, UPDATE_CLIENTS);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        }
        return InteractionResult.SUCCESS;
    }
}
