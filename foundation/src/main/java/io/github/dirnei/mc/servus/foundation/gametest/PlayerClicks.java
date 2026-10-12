package io.github.dirnei.mc.servus.foundation.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Right clicks the way a client does: a server player sends a use packet and the server's packet listener
 * handles it, with its range and hit location checks and the full block, empty hand and item dispatch.
 * Tests that call {@code useItemOn} on a block state skip all of that.
 */
public final class PlayerClicks {

    private PlayerClicks() {
    }

    /**
     * A survival server player standing two blocks north of {@code standNear}, holding {@code held}, with a packet
     * listener that drops everything the server sends. It never joins the player list: joining sends mod payloads
     * (Modonomicon) that a channel without a negotiated client refuses.
     */
    public static ServerPlayer player(GameTestHelper helper, BlockPos standNear, ItemStack held) {
        return player(helper, standNear, held, GameType.SURVIVAL);
    }

    /** As {@link #player(GameTestHelper, BlockPos, ItemStack)} in the given game mode. */
    public static ServerPlayer player(GameTestHelper helper, BlockPos standNear, ItemStack held, GameType gameType) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-click-player"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(Packet<?> packet, @Nullable ChannelFutureListener listener) {
                // No client: drop what the server sends, mod payloads included.
            }
        };
        player.setGameMode(gameType);
        BlockPos stand = helper.absolutePos(standNear).north(2);
        player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
        player.connection.markClientLoaded();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    /** Right clicks the block at {@code pos} where the look hits {@code location} (absolute) on its north face. */
    public static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Vec3 location) {
        BlockHitResult hit = new BlockHitResult(location, Direction.NORTH, helper.absolutePos(pos), false);
        player.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
    }

    /**
     * Looks from the player's eyes at {@code target} (absolute) and right clicks whatever block the look picks,
     * as the client's pick does: the hit block may be a different one than the block {@code target} lies in.
     */
    public static BlockHitResult lookAndUse(GameTestHelper helper, ServerPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(target.subtract(eye).normalize().scale(player.blockInteractionRange()));
        BlockHitResult hit = helper.getLevel().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            player.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
        }
        return hit;
    }

    /** Right clicks the centre of the north face of the block at {@code pos}. */
    public static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        use(helper, player, pos, new Vec3(absolute.getX() + 0.5, absolute.getY() + 0.5, absolute.getZ()));
    }
}
