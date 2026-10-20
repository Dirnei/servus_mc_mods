package io.github.dirnei.mc.servus.delicacies.gametest;

import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesCreativeTabs;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredItem;

/** Delicacies loads on top of Foundation and fills its creative tab. */
public final class DelicaciesSetupGameTests {

    private DelicaciesSetupGameTests() {
    }

    @ServusGameTest
    public static void foundationIsLoaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(ServusFoundationIds.MOD_ID), "Servus: Foundation should be loaded");
        helper.succeed();
    }

    @ServusGameTest
    public static void creativeTabListsEveryItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());
        Set<Item> listed = ServusDelicaciesCreativeTabs.DELICACIES.get().getDisplayItems().stream()
                .map(ItemStack::getItem).collect(Collectors.toSet());
        for (DeferredItem<? extends Item> item : ServusDelicaciesItems.CREATIVE_TAB_ORDER) {
            helper.assertTrue(listed.contains(item.get()), "the Delicacies tab should list " + item.getId());
        }
        helper.succeed();
    }
}
