package io.github.dirnei.mc.servus.delicacies.teller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;

/**
 * What a plated meal gives, from the {@code servus_delicacies:meal} data map: the food of one portion and the effects
 * the player gets after the last portion.
 */
public record Meal(FoodProperties portion, List<MobEffectInstance> effects) {

    public static final Codec<Meal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FoodProperties.DIRECT_CODEC.fieldOf("portion").forGetter(Meal::portion),
            MobEffectInstance.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Meal::effects)
    ).apply(instance, Meal::new));

    /** Fresh copies of the finish effects, so the shared data stays untouched. */
    public List<MobEffectInstance> freshEffects() {
        return effects.stream().map(MobEffectInstance::new).toList();
    }
}
