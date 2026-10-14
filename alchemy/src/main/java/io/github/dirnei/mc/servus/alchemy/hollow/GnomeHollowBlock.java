package io.github.dirnei.mc.servus.alchemy.hollow;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * The Gnome Hollow: principle buckets fill it, a Salt Crystal binds a gnome to a ripe vein,
 * a plain click opens the GUI where the sample goes.
 */
public class GnomeHollowBlock extends MachineBlock {

    public static final MapCodec<GnomeHollowBlock> CODEC = simpleCodec(GnomeHollowBlock::new);

    public GnomeHollowBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusAlchemyBlockEntities.GNOME_HOLLOW.get();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || !(level.getBlockEntity(pos) instanceof GnomeHollowBlockEntity hollow)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (!FluidUtil.getFirstStackContained(stack).isEmpty()) {
            if (!level.isClientSide()) {
                FluidUtil.interactWithFluidHandler(player, hand, pos, hollow.tanks(), null);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ServusAlchemyItems.SALT_CRYSTAL.get())) {
            if (!level.isClientSide()) {
                GnomeHollowBlockEntity.Binding binding = hollow.bind(player);
                if (binding == GnomeHollowBlockEntity.Binding.BOUND && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.sendOverlayMessage(Component.translatable(
                        "message.servus_manufacturing.gnome_hollow." + binding.name().toLowerCase(Locale.ROOT)));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
