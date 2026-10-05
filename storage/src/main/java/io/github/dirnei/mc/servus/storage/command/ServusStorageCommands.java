package io.github.dirnei.mc.servus.storage.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /servus_warehouse info <pos>}: prints the index of the network at a storage block. */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class ServusStorageCommands {

    private static final int MAX_LINES = 30;

    private ServusStorageCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("servus_warehouse")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::info)))
                .then(Commands.literal("take")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(ServusStorageCommands::take))))));
    }

    private static int take(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack wanted = ItemArgument.getItem(context, "item").createItemStack(1, false);
        int count = IntegerArgumentType.getInteger(context, "count");
        ItemStack taken = network.get().extract(ItemKey.of(wanted), count);
        if (taken.isEmpty()) {
            source.sendFailure(Component.literal("Nothing taken"));
            return 0;
        }
        Block.popResource(source.getLevel(), pos.above(), taken);
        source.sendSuccess(() -> Component.literal("Took " + taken.getCount() + " " + taken.getHoverName().getString()), false);
        return taken.getCount();
    }

    private static Optional<WarehouseNetwork> networkAt(CommandSourceStack source, BlockPos pos) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof StorageNodeBlockEntity node)) {
            source.sendFailure(Component.literal("No storage block at " + pos.toShortString()));
            return Optional.empty();
        }
        Optional<WarehouseNetwork> network = node.network();
        if (network.isEmpty()) {
            source.sendFailure(Component.literal("Storage block at " + pos.toShortString() + " is not in a network"));
        }
        return network;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        WarehouseNetwork warehouse = network.get();
        source.sendSuccess(warehouse::summary, false);
        List<Map.Entry<ItemKey, Long>> entries = warehouse.index().entries();
        int shown = 0;
        for (Map.Entry<ItemKey, Long> entry : entries) {
            if (shown++ >= MAX_LINES) {
                source.sendSuccess(() -> Component.literal("  ... " + (entries.size() - MAX_LINES) + " more"), false);
                break;
            }
            String name = entry.getKey().stack(1).getHoverName().getString();
            long count = entry.getValue();
            source.sendSuccess(() -> Component.literal("  - " + name + " x " + count), false);
        }
        return entries.size();
    }
}
