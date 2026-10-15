package io.github.dirnei.mc.servus.storage.shuttle.module;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A work bench for shuttles. It keeps the shuttle and the supplies a player
 * leaves in it, installs upgrades and tools, and builds the next tier. The
 * sneaking carrot dismantles it into one item that carries its wood and contents.
 */
public class ShuttleWorkshopBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<ShuttleWorkshopBlock> CODEC = simpleCodec(ShuttleWorkshopBlock::new);

    public ShuttleWorkshopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShuttleWorkshopBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShuttleWorkshopBlockEntity workshop) {
            player.openMenu(workshop, buf -> ShuttleWorkshopMenu.writeOpening(buf, pos, StorageStatsManager.INSTANCE.shuttleTiers()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Sneaking dismantles the workshop into one item with its wood, shuttle and supplies; a plain click does nothing. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide()) {
            ItemStack drop = new ItemStack(this);
            if (level.getBlockEntity(pos) instanceof ShuttleWorkshopBlockEntity workshop) {
                drop.applyComponents(workshop.collectComponents());
                workshop.clearForDismantle();
            }
            Block.popResource(level, pos, drop);
            level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }
}
