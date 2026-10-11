package io.github.dirnei.mc.servus.foundation.logistics.facade;

import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlock;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsComponents;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
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

    /** The facades shown in the creative tab and in JEI; any facadeable block works the same way. */
    public static final List<Block> EXAMPLES =
            List.of(Blocks.STONE, Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.BRICKS, Blocks.DEEPSLATE_TILES);

    private final boolean hollow;

    public FacadeItem(Properties properties, boolean hollow) {
        super(properties);
        this.hollow = hollow;
    }

    /** Whether this item places hollow panels. */
    public boolean hollow() {
        return hollow;
    }

    public static ItemStack of(Block block, int count) {
        return of(block, count, false);
    }

    public static ItemStack of(Block block, int count, boolean hollow) {
        Item item = hollow ? ServusLogisticsItems.HOLLOW_FACADE.get() : ServusLogisticsItems.FACADE.get();
        ItemStack stack = new ItemStack(item, count);
        stack.set(ServusLogisticsComponents.FACADE_BLOCK.get(), block);
        return stack;
    }

    /** One facade of each example block. */
    public static List<ItemStack> examples() {
        return EXAMPLES.stream().map(block -> of(block, 1)).toList();
    }

    /** One Hollow Facade of each example block. */
    public static List<ItemStack> hollowExamples() {
        return EXAMPLES.stream().map(block -> of(block, 1, true)).toList();
    }

    public static Block blockOf(ItemStack stack) {
        Block block = stack.get(ServusLogisticsComponents.FACADE_BLOCK.get());
        return block == null ? Blocks.STONE : block;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(hollow ? "item.servus_foundation.hollow_facade.named" : "item.servus_foundation.facade.named",
                blockOf(stack).getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.servus_foundation.facade.hint"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof UtilityDuctBlock)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            if (!duct.setPanel(context.getClickedFace(), blockOf(context.getItemInHand()).defaultBlockState(), hollow)) {
                return InteractionResult.FAIL;
            }
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
