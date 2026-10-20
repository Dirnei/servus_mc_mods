package io.github.dirnei.mc.servus.brewery.rausch;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** The Hangover: slower mining and faster hunger, both from the loaded {@link RauschStats}. */
public class HangoverEffect extends MobEffect {

    private static final int TICKS_PER_SECOND = 20;

    public HangoverEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
        addAttributeModifier(Attributes.BLOCK_BREAK_SPEED,
                Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "effect.hangover.mining_speed"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> stats().miningSpeed());
    }

    private static HangoverStats stats() {
        return RauschStatsManager.INSTANCE.stats().hangover();
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (mob instanceof Player player) {
            player.causeFoodExhaustion((float) stats().exhaustion());
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return tickCount % TICKS_PER_SECOND == 0;
    }
}
