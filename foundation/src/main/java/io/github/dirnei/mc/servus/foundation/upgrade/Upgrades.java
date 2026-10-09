package io.github.dirnei.mc.servus.foundation.upgrade;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.math.BigDecimal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** What counts as an upgrade and how much faster it makes a block. */
public final class Upgrades {

    /** Items of other mods that count as upgrades, with their multiplier in {@code upgrades.json}. */
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "upgrades"));

    private Upgrades() {
    }

    /** Whether the stack goes into upgrade slots. */
    public static boolean isUpgrade(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof UpgradeItem || stack.is(TAG));
    }

    /** The speed multiplier of one upgrade: from data, else the item's default, else 1. */
    public static double multiplier(ItemStack stack) {
        if (!isUpgrade(stack)) {
            return 1;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Double fromData = UpgradesManager.INSTANCE.multiplier(id);
        if (fromData != null) {
            return fromData;
        }
        return stack.getItem() instanceof UpgradeItem upgrade ? upgrade.defaultMultiplier() : 1;
    }

    /** A number for display without trailing zeros: "2", "1.5". */
    public static String format(double value) {
        return BigDecimal.valueOf(Math.round(value * 100) / 100.0).stripTrailingZeros().toPlainString();
    }
}
