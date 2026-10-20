package io.github.dirnei.mc.servus.delicacies.firebox;

import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** How a player puts a Heating Element into a firebox and takes it out again, the same on every firebox. */
public final class ElementUse {

    private ElementUse() {
    }

    public static boolean isElement(ItemStack stack) {
        return stack.is(ServusFoundationItems.HEATING_ELEMENT.get());
    }

    /** Server side: runs {@code install}, uses up one element when it worked, and tells the player. */
    public static void install(Level level, BlockPos pos, Player player, ItemStack stack, BooleanSupplier install) {
        if (!install.getAsBoolean()) {
            player.sendOverlayMessage(Component.translatable("message.servus_delicacies.firebox.element_already"));
            return;
        }
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 1.2F);
        player.sendOverlayMessage(Component.translatable("message.servus_delicacies.firebox.element_in"));
    }

    /** Server side: takes the element out through {@code takeOut} into the player's inventory, and tells the player. */
    public static void takeOut(Level level, BlockPos pos, Player player, Supplier<ItemStack> takeOut) {
        ItemStack element = takeOut.get();
        if (element.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.servus_delicacies.firebox.no_element"));
            return;
        }
        player.getInventory().placeItemBackInInventory(element);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.2F);
        player.sendOverlayMessage(Component.translatable("message.servus_delicacies.firebox.element_out"));
    }

    /** What a player hears who offers fuel to a firebox the element heats. */
    public static Component fuelRefused() {
        return Component.translatable("message.servus_delicacies.firebox.fuel_refused");
    }
}
