package io.github.dirnei.mc.servus.brewery.rausch;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The status effect of one Rausch level. Its luck, armor, fall damage and walking speed modifiers read the loaded
 * {@link RauschStats} each time the effect is added; the amplifier plays no part.
 */
public class RauschEffect extends MobEffect {

    private final RauschLevel level;

    public RauschEffect(RauschLevel level, MobEffectCategory category, int color) {
        super(category, color);
        this.level = level;
        modifier(Attributes.LUCK, "luck", AttributeModifier.Operation.ADD_VALUE, LevelStats::luck);
        modifier(Attributes.ARMOR, "armor", AttributeModifier.Operation.ADD_VALUE, LevelStats::armor);
        modifier(Attributes.FALL_DAMAGE_MULTIPLIER, "fall_damage", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                stats -> stats.fallDamage() - 1);
        modifier(Attributes.MOVEMENT_SPEED, "speed", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, LevelStats::speed);
    }

    public RauschLevel level() {
        return level;
    }

    /** This level's numbers in the loaded data. */
    public LevelStats stats() {
        return RauschStatsManager.INSTANCE.stats().level(level);
    }

    private void modifier(Holder<Attribute> attribute, String name, AttributeModifier.Operation operation,
            ToDoubleFunction<LevelStats> amount) {
        Identifier id = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "effect." + level.key() + "." + name);
        addAttributeModifier(attribute, id, operation, amplifier -> amount.applyAsDouble(stats()));
    }
}
