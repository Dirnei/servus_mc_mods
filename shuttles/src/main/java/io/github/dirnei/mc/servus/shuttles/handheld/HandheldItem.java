package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.HandheldMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The warehouse handheld: scans rack bays in its mode, binds to a Warehouse
 * Controller, and opens its GUI on any other right click. The server owns
 * all state; it lives in data components on the stack.
 */
public class HandheldItem extends Item {

    public HandheldItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity target = level.getBlockEntity(context.getClickedPos());
        boolean controller = target instanceof WarehouseControllerBlockEntity;
        boolean bay = target instanceof StorageNodeBlockEntity node && node.zonable();
        if (!controller && !bay) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!level.isClientSide() && player != null) {
            ItemStack handheld = context.getItemInHand();
            if (target instanceof WarehouseControllerBlockEntity warehouse) {
                HandheldActions.bind(player, handheld, warehouse);
            } else {
                HandheldActions.scan(player, handheld, (StorageNodeBlockEntity) target);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Right click in the air, or on a block the handheld does not scan: opens its GUI. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            UUID team = Teams.teamOf(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new HandheldMenu(id, inventory, hand, team),
                    Component.translatable("gui.servus_shuttles.handheld.title")), buf -> HandheldMenu.write(buf, hand, team));
            if (serverPlayer.containerMenu instanceof HandheldMenu menu) {
                menu.sendState(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
