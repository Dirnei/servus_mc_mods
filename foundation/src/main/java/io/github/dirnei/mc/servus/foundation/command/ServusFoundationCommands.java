package io.github.dirnei.mc.servus.foundation.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Op commands of Servus: Foundation: {@code /servus_foundation data move <from> <to> <item> <count>}. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class ServusFoundationCommands {

    private ServusFoundationCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ServusFoundationIds.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("data")
                        .then(Commands.literal("move")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(ServusFoundationCommands::move))))))));
    }

    private static int move(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(context, "to");
        ItemStack like = ItemArgument.getItem(context, "item").createItemStack(1, false);
        int count = IntegerArgumentType.getInteger(context, "count");
        Optional<Delivery> delivery = DataPackets.move(context.getSource().getLevel(), from, to, like, count);
        if (delivery.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.servus_foundation.data.move.invalid"));
            return 0;
        }
        Delivery started = delivery.get();
        if (started.state() == Delivery.State.FAILED) {
            context.getSource().sendFailure(Component.translatable("commands.servus_foundation.data.move.failed",
                    started.id(), started.reason().name().toLowerCase(Locale.ROOT)));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.servus_foundation.data.move.started",
                started.id(), count, like.getHoverName()), true);
        return (int) started.id();
    }
}
