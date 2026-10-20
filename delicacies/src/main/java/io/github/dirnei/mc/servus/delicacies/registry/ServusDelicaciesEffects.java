package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.teller.ProperlyFullEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mob effects of Servus: Delicacies. */
public final class ServusDelicaciesEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ServusDelicaciesIds.MOD_ID);

    /** Properly Full, given by a finished plated meal. */
    public static final DeferredHolder<MobEffect, ProperlyFullEffect> PROPERLY_FULL = EFFECTS.register("properly_full", ProperlyFullEffect::new);

    private ServusDelicaciesEffects() {
    }
}
