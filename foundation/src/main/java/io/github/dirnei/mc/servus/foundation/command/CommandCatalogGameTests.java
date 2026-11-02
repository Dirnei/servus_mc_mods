package io.github.dirnei.mc.servus.foundation.command;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.gametest.CommandCatalogCheck;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** The Foundation commands match the Edict catalog the mod ships. */
public final class CommandCatalogGameTests {

    private CommandCatalogGameTests() {
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void catalogListsEveryCommand(GameTestHelper helper) {
        CommandCatalogCheck.assertMatches(helper, "foundation", CommandCatalogGameTests.class, ServusFoundationIds.MOD_ID);
        helper.succeed();
    }
}
