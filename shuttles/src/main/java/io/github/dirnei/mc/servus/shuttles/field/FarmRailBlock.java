package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import net.minecraft.world.InteractionResult;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlockEntity;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Warehouse Rail for fields: it is a rail in every way, and a connected run
 * of farm rails is one field that a Field Head waters and watches. Placing or
 * removing one tells the Field Heads around it. Each rail's copper corrodes
 * while it is watered (see {@link FieldCorrosion}); an axe scrapes the field back.
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
     * An axe scrapes the rails of the field outward from the clicked rail, each
     * one stage back for {@code axe_damage_per_rail} durability; a worn axe
     * spends what it has left and the rest stays on the next rail as progress.
     * A field of fresh copper passes the click on.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.canPerformAction(ItemAbilities.AXE_SCRAPE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        FieldGraph.Result<BlockPos> field = FieldHeads.field(level, pos);
        if (field.tooLarge()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.servus_shuttles.farm_rail.too_large"));
            }
            return InteractionResult.CONSUME;
        }
        List<RailBlockEntity> rails = new ArrayList<>(field.members().size());
        for (BlockPos rail : field.members()) {
            if (level.getBlockEntity(rail) instanceof RailBlockEntity entity) {
                rails.add(entity);
            }
        }
        if (rails.stream().noneMatch(rail -> rail.corrosion() > FieldCorrosion.COPPER)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int budget = stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : -1;
        FieldCorrosion.Scrape plan = FieldCorrosion.scrape(rails.stream().map(RailBlockEntity::corrosion).toList(),
                rails.stream().map(RailBlockEntity::scrapeProgress).toList(), ShuttlesStatsManager.INSTANCE.axeDamagePerRail(), budget);
        for (int i = 0; i < rails.size(); i++) {
            RailBlockEntity rail = rails.get(i);
            if (plan.stages()[i] != rail.corrosion()) {
                level.levelEvent(null, LevelEvent.PARTICLES_SCRAPE, rail.getBlockPos(), 0);
            }
            rail.setCorrosion(plan.stages()[i], plan.progress()[i]);
        }
        level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendOverlayMessage(plan.left() > 0
                ? Component.translatable("message.servus_shuttles.farm_rail.scraped_left", plan.scraped(), plan.left())
                : Component.translatable("message.servus_shuttles.farm_rail.scraped", plan.scraped()));
        FieldHeads.servingAt(level, pos).ifPresent(FieldHeadBlockEntity::railsChanged);
        if (plan.spent() > 0) {
            stack.hurtAndBreak(plan.spent(), player, hand.asEquipmentSlot());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        FieldHeads.railChanged(level, pos);
    }
}
