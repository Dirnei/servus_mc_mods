package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.foundation.gametest.CommandCatalogCheck;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** The Alchemy commands match the Edict catalog the mod ships. */
public final class CommandCatalogGameTests {

    private CommandCatalogGameTests() {
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void catalogListsEveryCommand(GameTestHelper helper) {
        CommandCatalogCheck.assertMatches(helper, "alchemy", CommandCatalogGameTests.class, ServusAlchemyIds.MOD_ID);
        helper.succeed();
    }
}
