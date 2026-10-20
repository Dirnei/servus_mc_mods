package io.github.dirnei.mc.servus.brewery.gametest;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryCreativeTabs;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.ModList;

/** Brewery loads on top of Foundation and Delicacies and fills its creative tab. */
public final class BrewerySetupGameTests {

    private BrewerySetupGameTests() {
    }

    @ServusGameTest
    public static void foundationIsLoaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(ServusFoundationIds.MOD_ID), "Servus: Foundation should be loaded");
        helper.succeed();
    }

    @ServusGameTest
    public static void delicaciesIsLoaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(ServusDelicaciesIds.MOD_ID), "Servus: Delicacies should be loaded");
        helper.succeed();
    }

    @ServusGameTest
    public static void creativeTabListsTheBeerGlass(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());
        boolean listed = ServusBreweryCreativeTabs.BREWERY.get().getDisplayItems().stream()
                .anyMatch(stack -> stack.is(ServusBreweryItems.BEER_GLASS.get()));
        helper.assertTrue(listed, "the Brewery tab should list the Beer Glass");
        helper.succeed();
    }
}
