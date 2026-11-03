package io.github.dirnei.mc.servus.notes.gametest;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

/** Notes loads and asks for no other Servus mod. */
final class NotesSetupGameTests {

    private NotesSetupGameTests() {
    }

    static void loadsAlone(GameTestHelper helper) {
        ModContainer notes = ModList.get().getModContainerById(ServusNotesIds.MOD_ID).orElse(null);
        helper.assertTrue(notes != null, "Servus: Notes should be loaded");
        for (IModInfo.ModVersion dependency : notes.getModInfo().getDependencies()) {
            helper.assertFalse(dependency.getModId().startsWith("servus_"),
                    "Servus: Notes should not depend on " + dependency.getModId());
        }
        helper.succeed();
    }
}
