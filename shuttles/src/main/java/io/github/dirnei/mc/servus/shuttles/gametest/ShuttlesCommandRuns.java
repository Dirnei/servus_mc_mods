package io.github.dirnei.mc.servus.shuttles.gametest;

import com.mojang.authlib.GameProfile;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.site.SiteRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Runs {@code /servus shuttles} commands in GameTests and returns what they said. */
final class ShuttlesCommandRuns {

    private ShuttlesCommandRuns() {
    }

    /** Runs {@code /servus shuttles <command>} as the server console, an operator without a player, in the Overworld. */
    static String console(GameTestHelper helper, String command) {
        return run(helper.getLevel().getServer().createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER), command);
    }

    /** Runs {@code /servus shuttles <command>} as an operator player that looks at the block. */
    static String lookingAt(GameTestHelper helper, BlockPos looked, String command) {
        return run(player(helper, looked).createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER), command);
    }

    /** A fake player standing on the block, looking down at its centre. */
    static FakePlayer player(GameTestHelper helper, BlockPos looked) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "servus-op"));
        Vec3 target = Vec3.atCenterOf(helper.absolutePos(looked));
        player.setPos(target.x, target.y + 0.6, target.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
        return player;
    }

    /** Runs {@code /servus shuttles <command>} with the source, collecting every message it sends. */
    static String run(CommandSourceStack source, String command) {
        List<String> lines = new ArrayList<>();
        CommandSource collector = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                lines.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        source.getServer().getCommands().performPrefixedCommand(source.withSource(collector), "servus shuttles " + command);
        return String.join("\n", lines);
    }

    /** The warehouse number of the controller at the relative position. */
    static int number(GameTestHelper helper, BlockPos controller) {
        return SiteRegistry.of(helper.getLevel()).number(helper.getBlockEntity(controller, WarehouseControllerBlockEntity.class));
    }
}
