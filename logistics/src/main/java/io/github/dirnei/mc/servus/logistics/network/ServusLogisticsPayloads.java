package io.github.dirnei.mc.servus.logistics.network;

import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.Channel;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the duct configuration payloads. */
@EventBusSubscriber(modid = ServusLogisticsIds.MOD_ID)
public final class ServusLogisticsPayloads {

    private static final double MAX_DISTANCE_SQ = 64.0;

    private ServusLogisticsPayloads() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetDuctModePayload.TYPE, SetDuctModePayload.STREAM_CODEC, (payload, context) -> {
            UtilityDuctBlockEntity duct = ductFor(context.player(), payload.pos());
            if (duct == null || payload.side() < 0 || payload.side() >= Direction.values().length) {
                return;
            }
            Channel channel = Channel.byOrdinal(payload.channel());
            if (duct.has(channel)) {
                duct.setMode(channel, Direction.values()[payload.side()], SideMode.byOrdinal(payload.mode()));
            }
        });
        registrar.playToServer(RemoveChannelPayload.TYPE, RemoveChannelPayload.STREAM_CODEC, (payload, context) -> {
            UtilityDuctBlockEntity duct = ductFor(context.player(), payload.pos());
            if (duct == null || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Channel channel = Channel.byOrdinal(payload.channel());
            if (!duct.removeChannel(channel)) {
                return;
            }
            ItemStack drop = new ItemStack(ServusLogisticsItems.channelItem(channel));
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
            if (duct.channels().isEmpty()) {
                player.level().removeBlock(payload.pos(), false);
            }
        });
    }

    private static UtilityDuctBlockEntity ductFor(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQ) {
            return null;
        }
        return serverPlayer.level().getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct ? duct : null;
    }
}
