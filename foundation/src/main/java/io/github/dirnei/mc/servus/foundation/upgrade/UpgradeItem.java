package io.github.dirnei.mc.servus.foundation.upgrade;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An item that makes a Servus block faster in its upgrade slots. The speed
 * multiplier comes from {@code upgrades.json} keyed by the item id, falling
 * back to the default given here. Other mods register their own upgrade items
 * as {@code UpgradeItem}s (or add any item to the {@code servus_foundation:upgrades}
 * tag with a JSON entry) and every block with {@link UpgradeSlots} accepts them.
 */
public class UpgradeItem extends Item {

    private final double defaultMultiplier;

    public UpgradeItem(double defaultMultiplier, Properties properties) {
        super(properties);
        this.defaultMultiplier = defaultMultiplier;
    }

    /** The multiplier used when no datapack names this item. */
    public double defaultMultiplier() {
        return defaultMultiplier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.servus_foundation.upgrade.speed", Upgrades.format(Upgrades.multiplier(stack))));
    }
}
