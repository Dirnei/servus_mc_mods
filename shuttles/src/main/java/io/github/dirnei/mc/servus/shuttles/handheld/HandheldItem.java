package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.HandheldMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The warehouse handheld: scans rack bays in its mode, also beyond the normal
 * reach up to its scan range, binds to a Warehouse Controller, and opens its
 * GUI on any other right click. The server owns
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

    /** Right click in the air, or on a block the handheld does not scan: scans the bay in range, else opens its GUI. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            StorageNodeBlockEntity bay = aimedBay(player);
            if (bay != null) {
                HandheldActions.scan(player, player.getItemInHand(hand), bay);
                return InteractionResult.SUCCESS;
            }
        }
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

    /** The block the player looks at within the scan range; a miss when there is none. */
    public static BlockHitResult aim(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(ShuttlesStatsManager.INSTANCE.handheldScanRange()));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    /** The zonable bay the player looks at within the scan range, or null. */
    public static StorageNodeBlockEntity aimedBay(Player player) {
        BlockHitResult hit = aim(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return player.level().getBlockEntity(hit.getBlockPos()) instanceof StorageNodeBlockEntity bay && bay.zonable() ? bay : null;
    }
}
