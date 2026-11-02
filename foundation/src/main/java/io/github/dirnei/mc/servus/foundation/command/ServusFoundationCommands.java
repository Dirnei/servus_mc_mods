package io.github.dirnei.mc.servus.foundation.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Op commands of Servus: Foundation: {@code /servus foundation data move <from> <to> <item> <count> [dimension]}. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class ServusFoundationCommands {

    private ServusFoundationCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(ServusRoot.with(Commands.literal("foundation")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("data")
                        .then(Commands.literal("move")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                        .then(countThenDimension()))))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> countThenDimension() {
        return Commands.argument("count", IntegerArgumentType.integer(1))
                .executes(context -> move(context, context.getSource().getLevel()))
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(context -> move(context, DimensionArgument.getDimension(context, "dimension"))));
    }

    private static int move(CommandContext<CommandSourceStack> context, ServerLevel level) throws CommandSyntaxException {
        BlockPos from = BlockPosArgument.getLoadedBlockPos(context, level, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(context, level, "to");
        ItemStack like = ItemArgument.getItem(context, "item").createItemStack(1);
        return startMove(context.getSource(), level, from, to, like, IntegerArgumentType.getInteger(context, "count"));
    }

    /**
     * Starts a data line move between two members and tells the source the delivery id or why it
     * failed; returns the delivery id, 0 when none started. For commands of every Servus mod.
     */
    public static int startMove(CommandSourceStack source, ServerLevel level, BlockPos from, BlockPos to, ItemStack like, int count) {
        Optional<Delivery> delivery = DataPackets.move(level, from, to, like, count);
        if (delivery.isEmpty()) {
            source.sendFailure(Component.translatable("commands.servus_foundation.data.move.invalid"));
            return 0;
        }
        Delivery started = delivery.get();
        if (started.state() == Delivery.State.FAILED) {
            source.sendFailure(Component.translatable("commands.servus_foundation.data.move.failed",
                    started.id(), started.reason().name().toLowerCase(Locale.ROOT)));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.servus_foundation.data.move.started",
                started.id(), count, like.getHoverName()), true);
        return (int) started.id();
    }
}
