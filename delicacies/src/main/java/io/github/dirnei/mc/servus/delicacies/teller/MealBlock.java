package io.github.dirnei.mc.servus.delicacies.teller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.delicacies.food.DelicaciesDataMaps;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A plated meal on its plate. An empty hand right click by a player who can eat eats one portion, with the food the
 * {@link DelicaciesDataMaps#MEAL} data gives the meal's item; the last portion leaves an empty Plate and gives the
 * meal's finish effects, and a meal with a beer hands the first {@link TellerTags#BREAKFAST_BEERS} item over.
 */
public class MealBlock extends Block {

    public static final MapCodec<MealBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(MealKind::byId, MealKind::id).fieldOf("meal").forGetter(MealBlock::kind),
            propertiesCodec()
    ).apply(instance, MealBlock::new));
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Portions still on the plate. */
    public static final IntegerProperty PORTIONS = IntegerProperty.create("portions", 1, MealPortions.FULL);

    private final MealKind kind;
    private final VoxelShape[][] shapes = new VoxelShape[MealPortions.FULL + 1][4];

    public MealBlock(MealKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PORTIONS, MealPortions.FULL));
        for (int left = 1; left <= MealPortions.FULL; left++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                shapes[left][direction.get2DDataValue()] = PartShapes.shapeOf(MealParts.boxes(kind, left), direction);
            }
        }
    }

    /** Which meal this is. */
    public MealKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PORTIONS);
    }

    /** Turned toward the player, and only on a block with a solid top. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!PlateBlock.restsOn(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(PORTIONS)][state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        Meal meal = DelicaciesDataMaps.meal(asItem());
        if (meal == null || !player.canEat(false)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            eatPortion(server, pos, state, player, meal);
        }
        return InteractionResult.SUCCESS;
    }

    private void eatPortion(ServerLevel level, BlockPos pos, BlockState state, Player player, Meal meal) {
        player.getFoodData().eat(meal.portion());
        level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, asItem()), pos.getX() + 0.5, pos.getY() + 0.15,
                pos.getZ() + 0.5, 6, 0.15, 0.05, 0.15, 0.05);
        level.gameEvent(player, GameEvent.EAT, pos);
        int left = state.getValue(PORTIONS);
        if (!MealPortions.isLast(left)) {
            level.setBlock(pos, state.setValue(PORTIONS, MealPortions.afterEating(left)), Block.UPDATE_ALL);
            return;
        }
        level.setBlock(pos, ServusDelicaciesBlocks.PLATE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        meal.freshEffects().forEach(player::addEffect);
        if (kind.withBeer()) {
            handOverBeer(player);
        }
    }

    private static void handOverBeer(Player player) {
        Iterator<Holder<Item>> beers = BuiltInRegistries.ITEM.getTagOrEmpty(TellerTags.BREAKFAST_BEERS).iterator();
        if (beers.hasNext()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(beers.next()));
        }
    }
}
