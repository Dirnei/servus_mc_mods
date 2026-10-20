package io.github.dirnei.mc.servus.brewery.rausch;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.beer.BeerDrunkEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Game events of the Rausch: its data, each beer, the regular update, foreign Nausea and the extra knockback. */
@EventBusSubscriber(modid = ServusBreweryIds.MOD_ID)
public final class RauschEvents {

    private RauschEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(RauschStatsManager.LISTENER_ID, RauschStatsManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(RauschTracker::refresh);
    }

    @SubscribeEvent
    public static void onBeerDrunk(BeerDrunkEvent event) {
        RauschTracker.drink(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % RauschTracker.UPDATE_INTERVAL == 0) {
            RauschTracker.advance(player, RauschTracker.UPDATE_INTERVAL, RauschTracker.day(player));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && event.getApplicationResult()) {
            RauschTracker.makeWayFor(player, event.getEffectInstance());
        }
    }

    /** Knockback resistance cannot go below 0, so a level's extra knockback scales the push itself. */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        RauschEffects.active(event.getEntity())
                .ifPresent(effect -> event.setStrength(event.getStrength() * (float) effect.stats().knockback()));
    }
}
