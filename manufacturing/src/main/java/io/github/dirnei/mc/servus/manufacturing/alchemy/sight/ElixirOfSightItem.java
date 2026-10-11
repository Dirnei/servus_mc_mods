package io.github.dirnei.mc.servus.manufacturing.alchemy.sight;

import net.minecraft.world.InteractionResult;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** The Elixir of Sight: drunk like a potion, gives Elemental Sight and leaves a glass bottle. */
public class ElixirOfSightItem extends Item {

    private static final int DRINK_TICKS = 32;

    public ElixirOfSightItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        if (!level.isClientSide()) {
            drinker.addEffect(new MobEffectInstance(ElementalSight.EFFECT, ElementalSight.DURATION));
            if (drinker instanceof ServerPlayer player && level instanceof ServerLevel server) {
                Journal.recordEvent(server, Teams.teamOf(player), "sight", 1);
            }
        }
        if (drinker instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (drinker instanceof Player player && !player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
            player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
