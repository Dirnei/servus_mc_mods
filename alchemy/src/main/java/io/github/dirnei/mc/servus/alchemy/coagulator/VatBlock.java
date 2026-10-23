package io.github.dirnei.mc.servus.alchemy.coagulator;

import io.github.dirnei.mc.servus.alchemy.PrincipleBuckets;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

/**
 * A Principle Vat of the Coagulator, placed three times above it. While formed, {@link #PIECE}
 * says which one it is and so which principle it takes: Sulfur, Salt or Mercury from left to right.
 * An Athanor ritual may grow a crystal out of a plain vat ({@link #CRYSTAL}); a grown vat gives light,
 * has a block entity for its cluster and is no Coagulator vat.
 */
public class VatBlock extends Block implements Wrenchable, EntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** The Vat cell of {@link CoagulatorLayout}, 0 to 2 from left to right. */
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, 2);
    public static final EnumProperty<VatCrystal> CRYSTAL = EnumProperty.create("crystal", VatCrystal.class);

    public VatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(PIECE, 0)
                .setValue(CRYSTAL, VatCrystal.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, PIECE, CRYSTAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A bucket of this Vat's principle pours into the Coagulator; any other bucket does nothing here. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!state.getValue(FORMED) || !PrincipleBuckets.holdsPrinciple(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        LegacyFluidHandler vat = CoagulatorStructure.vatHandler(level, pos, state);
        if (vat != null && !level.isClientSide()) {
            FluidUtil.interactWithFluidHandler(player, hand, pos, vat, null);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        CoagulatorStructure.neighbourChanged(level, state, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        CoagulatorStructure.broken(level, state, pos);
        if (state.getValue(CRYSTAL).grown()) {
            AthanorBlockEntity.grownBlockBroken(level, pos, state);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    /** The light a vat gives in {@code state}, for its block properties. */
    public static int light(BlockState state) {
        return state.getValue(CRYSTAL).light();
    }

    /** Only a grown vat has a block entity, which draws its cluster. */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(CRYSTAL).grown() ? new GrownVatBlockEntity(pos, state) : null;
    }

    /** Now and then a glint rises from a grown crystal. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        VatCrystal crystal = state.getValue(CRYSTAL);
        if (!crystal.grown() || random.nextInt(3) != 0) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.9;
        double y = pos.getY() + 1 + random.nextDouble() * 1.8;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.9;
        level.addParticle(new DustParticleOptions(crystal.color(), 0.6F), x, y, z, 0, 0.02, 0);
    }

    /** The carrot never turns a Vat. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        return InteractionResult.PASS;
    }
}
