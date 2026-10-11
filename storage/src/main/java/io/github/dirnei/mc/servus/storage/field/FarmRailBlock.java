package io.github.dirnei.mc.servus.storage.field;

import net.minecraft.world.InteractionResult;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.rail.RailBlockEntity;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Warehouse Rail for fields: it is a rail in every way, and a connected run
 * of farm rails is one field that a Field Head waters and watches. Placing or
 * removing one tells the Field Heads around it. Its copper corrodes with the
 * field (see {@link FieldCorrosion}); an axe scrapes the field back.
 */
public class FarmRailBlock extends RailBlock {

    public static final MapCodec<FarmRailBlock> CODEC = simpleCodec(FarmRailBlock::new);

    public FarmRailBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            FieldHeads.railChanged(level, pos);
        }
    }

    /**
     * An axe scrapes the whole field back one stage, one durability per rail;
     * an axe with too little durability left does nothing and says what the
     * field needs. Fresh copper passes the click on.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.canPerformAction(ItemAbilities.AXE_SCRAPE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (!(level.getBlockEntity(pos) instanceof RailBlockEntity clicked)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return clicked.corrosion() > FieldCorrosion.COPPER
                    ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        Optional<FieldHeadBlockEntity> head = FieldHeads.servingAt(level, pos);
        Collection<BlockPos> rails;
        int stage;
        if (head.isPresent()) {
            rails = head.get().rails();
            stage = head.get().corrosion();
        } else {
            FieldGraph.Result<BlockPos> field = FieldHeads.field(level, pos);
            if (field.tooLarge()) {
                player.sendOverlayMessage(Component.translatable("message.servus_storage.farm_rail.too_large"));
                return InteractionResult.CONSUME;
            }
            rails = field.members();
            stage = clicked.corrosion();
        }
        if (stage <= FieldCorrosion.COPPER) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int count = rails.size();
        if (!FieldCorrosion.axeCanScrape(stack.isDamageableItem(), stack.getMaxDamage() - stack.getDamageValue(), count)) {
            player.sendOverlayMessage(Component.translatable("message.servus_storage.farm_rail.axe_worn", count, count));
            return InteractionResult.CONSUME;
        }
        int younger = FieldCorrosion.previous(stage);
        if (head.isPresent()) {
            head.get().setStage(younger);
        } else {
            for (BlockPos rail : rails) {
                if (level.getBlockEntity(rail) instanceof RailBlockEntity entity) {
                    entity.setCorrosion(younger);
                }
            }
        }
        level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
        for (BlockPos rail : rails) {
            level.levelEvent(null, LevelEvent.PARTICLES_SCRAPE, rail, 0);
        }
        stack.hurtAndBreak(count, player, hand.asEquipmentSlot());
        return InteractionResult.SUCCESS;
    }

    /**
     * The rail's tick after a change (see {@link RailBlock}): a loose rail, one
     * placed since it last was in a field, joins the stage of the farm rails
     * beside it. Placement itself cannot do this, the block entity comes after.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        if (level.getBlockEntity(pos) instanceof RailBlockEntity rail && rail.corrosion() == FieldCorrosion.LOOSE
                && besideStagedRail(level, pos)) {
            FieldHeads.loosePlaced(level, pos);
        }
    }

    private static boolean besideStagedRail(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(direction);
            if (level.isLoaded(next) && level.getBlockEntity(next) instanceof RailBlockEntity rail
                    && level.getBlockState(next).getBlock() instanceof FarmRailBlock && rail.corrosion() != FieldCorrosion.LOOSE) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        FieldHeads.railChanged(level, pos);
    }
}
