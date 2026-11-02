package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.CommandCatalogCheck;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.gametest.framework.GameTestHelper;

/** The Shuttles commands match the Edict catalog the mod ships. */
public final class CommandCatalogGameTests {

    private CommandCatalogGameTests() {
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void catalogListsEveryCommand(GameTestHelper helper) {
        CommandCatalogCheck.assertMatches(helper, "shuttles", CommandCatalogGameTests.class, ServusShuttlesIds.MOD_ID);
        helper.succeed();
    }
}
