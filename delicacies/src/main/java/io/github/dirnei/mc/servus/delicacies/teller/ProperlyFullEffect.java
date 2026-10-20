package io.github.dirnei.mc.servus.delicacies.teller;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Properly Full, the reward of a finished plated meal: a slow heal of one health point every {@link #HEAL_INTERVAL}
 * ticks, halved per amplifier level like Regeneration.
 */
public class ProperlyFullEffect extends MobEffect {

    /** Ticks between heals at amplifier 0. */
    public static final int HEAL_INTERVAL = 100;

    private static final int COLOR = 0xC4843E;

    public ProperlyFullEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (mob.getHealth() < mob.getMaxHealth()) {
            mob.heal(1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        int interval = HEAL_INTERVAL >> amplification;
        return interval <= 0 || tickCount % interval == 0;
    }
}
