package io.github.dirnei.mc.servus.delicacies.gametest;

import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesCreativeTabs;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.ModList;

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
    public static void creativeTabListsThePretzel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());
        boolean listed = ServusDelicaciesCreativeTabs.DELICACIES.get().getDisplayItems().stream()
                .anyMatch(stack -> stack.is(ServusDelicaciesItems.PRETZEL.get()));
        helper.assertTrue(listed, "the Delicacies tab should list the Pretzel");
        helper.succeed();
    }
}
