package io.github.dirnei.mc.servus.foundation.item;

import io.github.dirnei.mc.servus.foundation.naming.MachineLabels;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Data Potato, Servus' investigation tool. Held, it shows analysis views
 * such as the data duct load; used on a named machine it prints the
 * machine's label, and on a {@link PotatoInspectable} block that block's
 * report, into chat.
 * It never configures or dismantles anything, also when sneaking.
 */
public class DataPotatoItem extends Item {

    public DataPotatoItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** A named machine answers before its block is used: its label, then its block's report, and no GUI opens. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Optional<NamedMachine> found = MachineLabels.machineAt(level, pos);
        if (found.isEmpty()) {
            return InteractionResult.PASS;
        }
        NamedMachine named = found.get();
        Player player = context.getPlayer();
        if (!level.isClientSide() && player != null) {
            List<Component> report = new ArrayList<>(named.potatoLines());
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof PotatoInspectable inspectable) {
                report.addAll(inspectable.potatoReport(level, pos, state, context.getClickLocation()));
            }
            report.forEach(player::sendSystemMessage);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PotatoInspectable inspectable)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!level.isClientSide() && player != null) {
            List<Component> report = inspectable.potatoReport(level, pos, state, context.getClickLocation());
            report.forEach(player::sendSystemMessage);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.servus_foundation.data_potato").withStyle(ChatFormatting.GRAY));
    }
}
