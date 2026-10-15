package io.github.dirnei.mc.servus.alchemy.sight;

import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;

/**
 * The Elixir of Sight: drunk like a potion, gives Elemental Sight, may poison the drinker and leaves a glass bottle. Drinking
 * (time, animation, sound) and the bottle come from the consumable and use remainder components set in {@link #properties()};
 * the poison numbers come from {@link ElixirDoseData}.
 */
public class ElixirOfSightItem extends Item {

    public ElixirOfSightItem(Properties properties) {
        super(properties);
    }

    /** Item properties of the elixir: a potion style drink that leaves a glass bottle. */
    public static Properties properties() {
        return new Properties().stacksTo(16)
                .component(DataComponents.CONSUMABLE, Consumables.defaultDrink().build())
                .usingConvertsTo(Items.GLASS_BOTTLE);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        if (level instanceof ServerLevel server) {
            drink(server, drinker, ElixirDoseData.INSTANCE.forItem(this));
            if (drinker instanceof ServerPlayer player) {
                Journal.recordEvent(server, Teams.teamOf(player), "sight", 1);
            }
        }
        return super.finishUsingItem(stack, level, drinker);
    }

    /**
     * Gives {@code drinker} Elemental Sight and rolls {@code dose} with the level's random source; a poisoned drinker gets
     * Poison and Nausea and, as a player, Paracelsus' scolding in the action bar.
     *
     * @return whether the drink poisoned
     */
    public static boolean drink(ServerLevel level, LivingEntity drinker, ElixirDose dose) {
        boolean sighted = drinker.hasEffect(ElementalSight.EFFECT);
        drinker.addEffect(new MobEffectInstance(ElementalSight.EFFECT, ElementalSight.DURATION));
        if (!dose.poisons(sighted, level.getRandom().nextDouble())) {
            return false;
        }
        if (dose.poisonTicks() > 0) {
            drinker.addEffect(new MobEffectInstance(MobEffects.POISON, dose.poisonTicks()));
        }
        if (dose.nauseaTicks() > 0) {
            drinker.addEffect(new MobEffectInstance(MobEffects.NAUSEA, dose.nauseaTicks()));
        }
        if (drinker instanceof Player player) {
            player.sendOverlayMessage(Component.translatable("message.servus_alchemy.elixir_of_sight.poisoned"));
        }
        return true;
    }
}
