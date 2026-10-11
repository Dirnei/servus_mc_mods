package io.github.dirnei.mc.servus.storage.packing;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Wireless Packing Station: a Packing Station on the rails whose far side
 * is the wireless line of the placing player's team instead of the data line.
 */
public class WirelessPackingStationBlock extends PackingStationBlock {

    public static final MapCodec<WirelessPackingStationBlock> CODEC = simpleCodec(WirelessPackingStationBlock::new);

    public WirelessPackingStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessPackingStationBlockEntity(pos, state);
    }

    @Override
    protected String tooltipKey() {
        return "tooltip.servus_storage.wireless_packing_station";
    }

    /** The placer's team owns the station's wireless line. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof WirelessPackingStationBlockEntity station) {
            station.setTeam(Teams.teamOf(player));
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ServusStorageBlockEntities.WIRELESS_PACKING_STATION.get(), WirelessPackingStationBlockEntity::serverTick);
    }
}
