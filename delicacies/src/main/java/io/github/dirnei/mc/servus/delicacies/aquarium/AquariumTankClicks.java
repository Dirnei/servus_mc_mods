package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/** A fish bucket used on the glass of an aquarium's tank pours the fish in, as it does on a Floor or Lid. */
@EventBusSubscriber(modid = ServusDelicaciesIds.MOD_ID)
public final class AquariumTankClicks {

    private AquariumTankClicks() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (pourThroughGlass(event.getLevel(), event.getPos(), event.getEntity(), event.getHand(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /** Pours the fish of {@code stack} into the aquarium under the tank block at {@code pos}; false when this is no such use. */
    public static boolean pourThroughGlass(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        FishSpecies species = FishSpecies.ofBucket(stack);
        AquariumPartBlockEntity master = species == null ? null : masterBelow(level, pos);
        if (master == null) {
            return false;
        }
        if (!level.isClientSide()) {
            AquariumPartBlock.pour(master, species, player, hand, stack, pos);
        }
        return true;
    }

    /** The master of the aquarium whose Floor lies under the tank column at {@code pos}, or null. */
    private static @Nullable AquariumPartBlockEntity masterBelow(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof FluidTankBlock)) {
            return null;
        }
        BlockPos below = pos.below();
        while (level.getBlockState(below).getBlock() instanceof FluidTankBlock) {
            below = below.below();
        }
        return level.getBlockEntity(below) instanceof AquariumPartBlockEntity part
                && level.getBlockState(below).getBlock() instanceof AquariumFloorBlock ? part.master() : null;
    }
}
