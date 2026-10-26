package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.forge.SalamanderForgeBlockEntity;
import io.github.dirnei.mc.servus.alchemy.forge.SalamanderForgeMenu;
import io.github.dirnei.mc.servus.foundation.gametest.MachineNameContract;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Names of the Alchemy machines on the shared naming rules, without Servus: Shuttles. */
public final class MachineNameGameTests {

    private static final BlockPos FORGE = AlchemyGameTests.MACHINE.above();

    private MachineNameGameTests() {
    }

    /** Every Alchemy machine kind follows the same naming rules. */
    @ServusGameTest(timeoutTicks = 40)
    public static void everyAlchemyKindFollowsTheRules(GameTestHelper helper) {
        MachineNameContract.checkAll(helper, ServusAlchemyIds.MOD_ID);
    }

    /** A Salamander Forge renamed through its GUI reads its name alone: no warehouse numbered it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void forgeRenamedWithoutNumber(GameTestHelper helper) {
        SalamanderForgeBlockEntity forge = ForgeGameTests.forge(helper, FORGE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(FORGE)).add(1, 0, 0));
        new SalamanderForgeMenu(1, player.getInventory(), forge).renameMachine(player, "Big Forge");
        Component label = forge.label();
        helper.assertTrue(forge.number() == 0 && label.getString().equals("Big Forge"), "the forge reads Big Forge, got " + label);
        helper.succeed();
    }
}
