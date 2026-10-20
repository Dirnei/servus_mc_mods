package io.github.dirnei.mc.servus.brewery.brewhouse;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Kind;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * A Brew Kettle block. Four of them on the left of the Brewhouse bricks form the copper kettle with its dome and vent
 * pipe. Grist and hops go in by hand, water by bucket, and wort comes out into an empty bucket.
 */
public class BrewKettleBlock extends BrewhousePartBlock {

    public static final MapCodec<BrewKettleBlock> CODEC = simpleCodec(BrewKettleBlock::new);
    public static final EnumProperty<BrewhousePart> PART =
            EnumProperty.create("part", BrewhousePart.class, part -> part.cell().kind() == Kind.KETTLE);
    /** The kettle brews over a hot fire; the vent pipe steams. */
    public static final BooleanProperty BREWING = BooleanProperty.create("brewing");

    public BrewKettleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(PART, BrewhousePart.COPPER_FRONT_LEFT)
                .setValue(FACING, Direction.NORTH).setValue(BREWING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, PART, FACING, BREWING);
    }

    @Override
    public Kind kind() {
        return Kind.KETTLE;
    }

    @Override
    public BrewhousePart part(BlockState state) {
        return state.getValue(PART);
    }

    @Override
    protected Component wrenchFormed(BrewhouseBlockEntity part, BlockState state) {
        return part.kettleStatus();
    }

    /** A fluid container fills or empties the kettle; an ingredient goes in; other items pass. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        BrewhouseBlockEntity part = formedPart(level, pos, state);
        BrewKettle kettle = part == null ? null : part.kettle();
        if (isCarrot(stack) || kettle == null) {
            return InteractionResult.PASS;
        }
        if (Transfer.isFluidContainer(stack)) {
            if (!level.isClientSide()) {
                FluidUtil.interactWithFluidHandler(player, hand, pos, kettle.fluids(), null);
            }
            return InteractionResult.SUCCESS;
        }
        if (!kettle.accepts(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack left = kettle.insertInputs(stack.copy());
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, left);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand tells water and wort; sneaking takes back the last ingredient. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof BrewhouseBlockEntity part)) {
            return InteractionResult.SUCCESS;
        }
        BrewKettle kettle = part.kettle();
        if (kettle != null && player.isShiftKeyDown()) {
            ItemStack taken = kettle.takeLast();
            if (!taken.isEmpty()) {
                player.getInventory().placeItemBackInInventory(taken);
            }
        } else {
            player.sendOverlayMessage(part.kettleStatus());
        }
        return InteractionResult.SUCCESS;
    }

    /** Steam from the top of the vent pipe while the kettle brews. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED) || !state.getValue(BREWING) || state.getValue(PART).cell() != Cell.COPPER_FRONT_LEFT
                || random.nextFloat() > 0.7F) {
            return;
        }
        Vec3 top = BrewhouseShapes.worldPoint(pos, Cell.COPPER_FRONT_LEFT, state.getValue(FACING),
                BrewhouseModel.PIPE_U, BrewhouseModel.PIPE_V, BrewhouseModel.PIPE_TOP + 0.5F);
        level.addParticle(ParticleTypes.CLOUD, top.x, top.y, top.z, (random.nextDouble() - 0.5) * 0.01, 0.05, (random.nextDouble() - 0.5) * 0.01);
    }
}
