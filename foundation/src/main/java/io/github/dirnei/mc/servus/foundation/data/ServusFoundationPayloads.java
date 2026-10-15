package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelListPayload;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ClientChannels;
import io.github.dirnei.mc.servus.foundation.logistics.menu.CableChannelsPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.ChannelRequests;
import io.github.dirnei.mc.servus.foundation.logistics.menu.ClientCableChannels;
import io.github.dirnei.mc.servus.foundation.logistics.menu.CreateChannelPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.DeleteChannelPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.EditChannelPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetDuctFilterPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.SetSideChannelPayload;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers the Foundation payloads. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class ServusFoundationPayloads {

    private ServusFoundationPayloads() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(DuctLoadPayload.TYPE, DuctLoadPayload.STREAM_CODEC,
                (payload, context) -> ClientDuctLoads.update(payload));
        registrar.playToClient(ChannelListPayload.TYPE, ChannelListPayload.STREAM_CODEC,
                (payload, context) -> ClientChannels.update(payload));
        registrar.playToServer(SetDuctFilterPayload.TYPE, SetDuctFilterPayload.STREAM_CODEC,
                (payload, context) -> UtilityDuctMenu.setFilterById(context.player(), payload.slot(), payload.item()));
        registrar.playToClient(CableChannelsPayload.TYPE, CableChannelsPayload.STREAM_CODEC,
                (payload, context) -> ClientCableChannels.update(payload));
        registrar.playToServer(SetSideChannelPayload.TYPE, SetSideChannelPayload.STREAM_CODEC,
                (payload, context) -> ChannelRequests.setSideChannel(context.player(), team(context.player()), payload.line(), payload.channel()));
        registrar.playToServer(CreateChannelPayload.TYPE, CreateChannelPayload.STREAM_CODEC,
                (payload, context) -> ChannelRequests.create(context.player(), team(context.player()),
                        payload.line(), payload.name(), payload.colour(), payload.icon()));
        registrar.playToServer(EditChannelPayload.TYPE, EditChannelPayload.STREAM_CODEC,
                (payload, context) -> ChannelRequests.edit(context.player(), team(context.player()),
                        payload.id(), payload.name(), payload.colour(), payload.icon()));
        registrar.playToServer(DeleteChannelPayload.TYPE, DeleteChannelPayload.STREAM_CODEC,
                (payload, context) -> ChannelRequests.delete(context.player(), team(context.player()), payload.line(), payload.id()));
    }

    private static UUID team(Player player) {
        return player instanceof ServerPlayer serverPlayer ? Teams.teamOf(serverPlayer) : player.getUUID();
    }
}
