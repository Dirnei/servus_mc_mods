package io.github.dirnei.mc.servus.alchemy.hint;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Game bus hooks of the old gnome: a player breaking a block and a player picking up a trace. */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID)
public final class HintEvents {

    private HintEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getPlayer() instanceof ServerPlayer player) {
            OldGnome.onBroken(level, OldGnome.teamOf(player), event.getPos(), event.getState(), OldGnomeData.INSTANCE.settings(),
                    level.getRandom());
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player && player.level() instanceof ServerLevel level
                && event.getOriginalStack().is(OldGnome.TRACES)) {
            OldGnome.onTracePickedUp(level, player, event.getOriginalStack());
        }
    }
}
