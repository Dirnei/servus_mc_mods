package io.github.dirnei.mc.servus.lib.client;

import io.github.dirnei.mc.servus.lib.placement.AssistedItem;
import io.github.dirnei.mc.servus.lib.placement.PlacementAssist;
import io.github.dirnei.mc.servus.lib.placement.PlacementAssists;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * What a sneak click would place right now, for the world ghost and the
 * crosshair indicator. Runs the same walk as the placement.
 */
public final class PlacementAssistPreview {

    /** The click the player aims at, where it would place and the state it would place there. */
    public record Preview(BlockHitResult hit, PlacementAssists.Target target, @Nullable BlockState state) {

        /** Blocks from the clicked block to the target. */
        public int distance() {
            return hit.getBlockPos().distManhattan(target.pos());
        }
    }

    private PlacementAssistPreview() {
    }

    /** The preview for the local player, or empty unless they sneak with an assisted item at a run with a target. */
    public static Optional<Preview> current() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isSecondaryUseActive()
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AssistedItem assisted) || !(stack.getItem() instanceof BlockItem blockItem)) {
            return Optional.empty();
        }
        PlacementAssist assist = assisted.placementAssist();
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
        if (!PlacementAssists.applies(context, assist)) {
            return Optional.empty();
        }
        return PlacementAssists.target(context, assist).map(target ->
                new Preview(hit, target, blockItem.getBlock().getStateForPlacement(target.context(context))));
    }
}
