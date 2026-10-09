package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The one carrot gesture for blocks with sides: a click cycles the clicked
 * face in the carrot's layer, sneaking dismantles and keeps the configuration
 * in the dropped item.
 */
public final class SideConfigWrench {

    private SideConfigWrench() {
    }

    /**
     * @param dropContents what to spill before the block is saved to its item, so nothing is duplicated
     */
    public static <T extends BlockEntity & SidedBlockEntity> InteractionResult use(UseOnContext context, Block block, T blockEntity,
                                                                                Runnable dropContents) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            dropContents.run();
            ItemStack drop = new ItemStack(block);
            blockEntity.saveToItem(drop, level.registryAccess());
            Block.popResource(level, pos, drop);
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }
        Direction face = context.getClickedFace();
        RelativeSide side = blockEntity.relativeSide(face);
        Component message;
        if (ConfigurationCarrotItem.layer(context.getItemInHand()) == SideLayer.FLUIDS && blockEntity.fluidSides() != null) {
            int value = blockEntity.cycleFluidSide(side);
            message = fluidMessage(side, value, blockEntity.sideTanks());
        } else {
            message = message(side, blockEntity.cycleSide(side));
        }
        if (player != null) {
            player.displayClientMessage(message, true);
        }
        return InteractionResult.SUCCESS;
    }

    public static Component sideName(RelativeSide side) {
        return Component.translatable("gui.servus_foundation.side." + side.name().toLowerCase(Locale.ROOT));
    }

    public static Component modeName(SideMode mode) {
        return Component.translatable("gui.servus_foundation.side_mode." + mode.key());
    }

    public static Component layerName(SideLayer layer) {
        return Component.translatable("gui.servus_foundation.side_layer." + layer.key());
    }

    /** The name of a Fluids layer value: Off, All fluids, or the tank's name. */
    public static Component fluidName(int value, List<SideTank> tanks) {
        if (value == FluidSides.OFF) {
            return Component.translatable("gui.servus_foundation.side_mode.off");
        }
        int tank = value - FluidSides.TANK_FIRST;
        if (value == FluidSides.ALL || tank < 0 || tank >= tanks.size()) {
            return Component.translatable("gui.servus_foundation.fluid_side.all");
        }
        return Component.translatable(tanks.get(tank).key());
    }

    /** "Left: Input" for the action bar and the panel tooltip. */
    public static Component message(RelativeSide side, SideMode mode) {
        return Component.translatable("gui.servus_foundation.side_line", sideName(side), modeName(mode));
    }

    /** "Left: Salt" for the Fluids layer. */
    public static Component fluidMessage(RelativeSide side, int value, List<SideTank> tanks) {
        return Component.translatable("gui.servus_foundation.side_line", sideName(side), fluidName(value, tanks));
    }
}
