package io.github.dirnei.mc.servus.manufacturing.alchemy.sight;

import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;

/**
 * The Elixir of Sight: drunk like a potion, gives Elemental Sight and leaves a glass bottle. Drinking
 * (time, animation, sound) and the bottle come from the consumable and use remainder components set in
 * {@link #properties()}.
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
        if (!level.isClientSide()) {
            drinker.addEffect(new MobEffectInstance(ElementalSight.EFFECT, ElementalSight.DURATION));
            if (drinker instanceof ServerPlayer player && level instanceof ServerLevel server) {
                Journal.recordEvent(server, Teams.teamOf(player), "sight", 1);
            }
        }
        return super.finishUsingItem(stack, level, drinker);
    }
}
