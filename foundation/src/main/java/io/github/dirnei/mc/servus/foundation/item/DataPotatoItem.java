package io.github.dirnei.mc.servus.foundation.item;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctReport;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankReport;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Data Potato, Servus' investigation tool. Held, it shows analysis views
 * such as the data duct load; used on a duct or a Fluid Tank it prints a
 * report into chat.
 * It never configures or dismantles anything, also when sneaking.
 */
public class DataPotatoItem extends Item {

    public DataPotatoItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity target = level.getBlockEntity(context.getClickedPos());
        if (!(target instanceof UtilityDuctBlockEntity) && !(target instanceof FluidTankBlockEntity)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!level.isClientSide() && player != null) {
            List<Component> report = target instanceof UtilityDuctBlockEntity duct
                    ? DuctReport.at(duct, context.getClickLocation())
                    : TankReport.of((FluidTankBlockEntity) target);
            report.forEach(player::sendSystemMessage);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.servus_foundation.data_potato").withStyle(ChatFormatting.GRAY));
    }
}
