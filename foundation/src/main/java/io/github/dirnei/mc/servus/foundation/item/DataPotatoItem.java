package io.github.dirnei.mc.servus.foundation.item;

import java.util.List;
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
 * such as the data duct load; used on a {@link PotatoInspectable} block it
 * prints that block's report into chat.
 * It never configures or dismantles anything, also when sneaking.
 */
public class DataPotatoItem extends Item {

    public DataPotatoItem(Properties properties) {
        super(properties.stacksTo(1));
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
