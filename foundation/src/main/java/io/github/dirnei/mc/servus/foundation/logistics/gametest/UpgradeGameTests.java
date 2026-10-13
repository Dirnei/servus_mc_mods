package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradesManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** The Speed Upgrade and upgrade slots. */
public final class UpgradeGameTests {

    private static final String EMPTY = "empty_12x6x12";

    private UpgradeGameTests() {
    }

    /** Upgrade slots take one Speed Upgrade each, refuse cobblestone, and two upgrades of 2 multiply by 4. */
    @ServusGameTest(timeoutTicks = 20)
    public static void upgradeSlotsTakeOnlyUpgrades(GameTestHelper helper) {
        UpgradeSlots slots = new UpgradeSlots(2, null);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        helper.assertTrue(slots.insertItem(0, cobble, false).getCount() == 1, "cobblestone is refused");
        ItemStack upgrades = new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get(), 3);
        ItemStack left = slots.insertItem(0, upgrades, false);
        helper.assertTrue(left.getCount() == 2, "one upgrade per slot, left " + left.getCount());
        helper.assertTrue(slots.multiplier() == 2, "one Speed Upgrade doubles, got " + slots.multiplier());
        slots.insertItem(1, left, false);
        helper.assertTrue(slots.multiplier() == 4, "two Speed Upgrades give 4, got " + slots.multiplier());
        helper.assertTrue(8 * slots.multiplier() == 32, "a base rate of 8 becomes 32");
        helper.succeed();
    }

    /** A datapack multiplier overrides the item's default. */
    @ServusGameTest(timeoutTicks = 20)
    public static void upgradeMultiplierFromData(GameTestHelper helper) {
        Identifier id = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "speed_upgrade");
        Double before = UpgradesManager.INSTANCE.multiplier(id);
        UpgradesManager.INSTANCE.set(id, 4);
        try {
            UpgradeSlots slots = new UpgradeSlots(1, null);
            slots.insertItem(0, new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get()), false);
            helper.assertTrue(slots.multiplier() == 4, "the multiplier from data is used, got " + slots.multiplier());
        } finally {
            if (before == null) {
                UpgradesManager.INSTANCE.remove(id);
            } else {
                UpgradesManager.INSTANCE.set(id, before);
            }
        }
        helper.succeed();
    }

    /** Redstone, gold ingots and a copper ingot craft one Speed Upgrade. */
    @ServusGameTest(timeoutTicks = 20)
    public static void speedUpgradeCrafts(GameTestHelper helper) {
        List<ItemStack> grid = new ArrayList<>();
        for (char c : "RGRGCGRGR".toCharArray()) {
            grid.add(new ItemStack(c == 'R' ? Items.REDSTONE : c == 'G' ? Items.GOLD_INGOT : Items.COPPER_INGOT));
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        ItemStack result = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(ServusFoundationItems.SPEED_UPGRADE.get()) && result.getCount() == 1,
                "the recipe makes one Speed Upgrade, got " + result);
        helper.succeed();
    }
}
