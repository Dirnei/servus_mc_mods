package io.github.dirnei.mc.servus.storage.packing;

import io.github.dirnei.mc.servus.foundation.block.TooltipBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import net.minecraft.core.BlockPos;
import io.github.dirnei.mc.servus.storage.menu.PackingStationMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.wood.Wood;

/**
 * The Packing Station: a station on the rails that is also a link member of the
 * data line, packing items from the warehouse into packets and unpacking
 * arriving packets for storage. Right click shows both buffers read-only; the
 * carrot dismantles it while sneaking. Its sides have no modes.
 */
public class PackingStationBlock extends InboundStationBlock implements TooltipBlock {

    public static final MapCodec<PackingStationBlock> CODEC = simpleCodec(PackingStationBlock::new);

    public PackingStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PackingStationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PackingStationBlockEntity point) {
            player.openMenu(point, buf -> PackingStationMenu.writeOpening(buf, point));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(tooltipKey()).withStyle(ChatFormatting.GRAY));
    }

    /** The lang key of the item tooltip. */
    protected String tooltipKey() {
        return "tooltip.servus_storage.packing_station";
    }

    /** Sneaking dismantles and drops both buffers; a plain click does nothing, there are no side modes. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof PackingStationBlockEntity point) {
                point.dropContents(level, pos);
            }
            Block.popResource(level, pos, Wood.stamp(new ItemStack(this), level.getBlockEntity(pos)));
            level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ServusStorageBlockEntities.PACKING_STATION.get(), PackingStationBlockEntity::serverTick);
    }
}
