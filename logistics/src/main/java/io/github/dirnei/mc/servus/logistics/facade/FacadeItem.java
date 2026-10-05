package io.github.dirnei.mc.servus.logistics.facade;

import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlock;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsComponents;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** A facade: applied to a duct, the duct looks like the stored block. */
public class FacadeItem extends Item {

    public FacadeItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Block block, int count) {
        ItemStack stack = new ItemStack(ServusLogisticsItems.FACADE.get(), count);
        stack.set(ServusLogisticsComponents.FACADE_BLOCK.get(), block);
        return stack;
    }

    public static Block blockOf(ItemStack stack) {
        Block block = stack.get(ServusLogisticsComponents.FACADE_BLOCK.get());
        return block == null ? Blocks.STONE : block;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.servus_logistics.facade.named", blockOf(stack).getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.servus_logistics.facade.hint"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof UtilityDuctBlock)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            if (duct.facade() != null) {
                return InteractionResult.FAIL;
            }
            duct.setFacade(blockOf(context.getItemInHand()).defaultBlockState());
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
