package io.github.dirnei.mc.servus.brewery.rausch;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The five status effects of the Rausch: one per level and the Hangover. */
public final class RauschEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ServusBreweryIds.MOD_ID);

    public static final DeferredHolder<MobEffect, RauschEffect> TIPSY = level(RauschLevel.TIPSY, MobEffectCategory.BENEFICIAL, 0xF5C14A);
    public static final DeferredHolder<MobEffect, RauschEffect> MERRY = level(RauschLevel.MERRY, MobEffectCategory.BENEFICIAL, 0xE6A01C);
    public static final DeferredHolder<MobEffect, RauschEffect> DRUNK = level(RauschLevel.DRUNK, MobEffectCategory.NEUTRAL, 0xC77D0E);
    public static final DeferredHolder<MobEffect, RauschEffect> PLASTERED = level(RauschLevel.PLASTERED, MobEffectCategory.NEUTRAL, 0x8A4E06);

    public static final DeferredHolder<MobEffect, HangoverEffect> HANGOVER = EFFECTS.register("hangover",
            () -> new HangoverEffect(0x7A7F8C));

    /** The level effects from the mildest to the strongest, in {@link RauschLevel} order. */
    public static final List<DeferredHolder<MobEffect, RauschEffect>> LEVELS = List.of(TIPSY, MERRY, DRUNK, PLASTERED);

    private RauschEffects() {
    }

    private static DeferredHolder<MobEffect, RauschEffect> level(RauschLevel level, MobEffectCategory category, int color) {
        return EFFECTS.register(level.key(), () -> new RauschEffect(level, category, color));
    }

    /** The effect of {@code level}. */
    public static Holder<MobEffect> effect(RauschLevel level) {
        return LEVELS.get(level.ordinal());
    }

    /** The Rausch level effect {@code entity} has now, if any. */
    public static Optional<RauschEffect> active(LivingEntity entity) {
        for (DeferredHolder<MobEffect, RauschEffect> level : LEVELS) {
            MobEffectInstance instance = entity.getEffect(level);
            if (instance != null) {
                return Optional.of(level.get());
            }
        }
        return Optional.empty();
    }
}
