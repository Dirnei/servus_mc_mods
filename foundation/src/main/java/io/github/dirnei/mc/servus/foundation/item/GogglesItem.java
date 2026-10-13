package io.github.dirnei.mc.servus.foundation.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/** Engineer goggles: worn on the head, they reveal hidden Servus blocks. Right click swaps them onto the head. */
public class GogglesItem extends Item {

    public GogglesItem(Properties properties) {
        super(properties.stacksTo(1).equippable(EquipmentSlot.HEAD));
    }

    public static boolean isWearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GogglesItem;
    }

    /** The shared rule: goggles on the head or a Configuration Carrot in either hand. */
    public static boolean reveals(Player player) {
        return isWearing(player) || ConfigurationCarrotItem.heldLayer(player) != null;
    }
}
