package io.github.dirnei.mc.servus.foundation.machine.sink;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** The smoke setting of a Chimney block, a block state value over {@link ChimneyRules.Smoke}. */
public enum ChimneySmoke implements StringRepresentable {
    OFF(ChimneyRules.Smoke.OFF),
    THIN(ChimneyRules.Smoke.THIN),
    COSY(ChimneyRules.Smoke.COSY),
    SIGNAL(ChimneyRules.Smoke.SIGNAL);

    private final ChimneyRules.Smoke rule;

    ChimneySmoke(ChimneyRules.Smoke rule) {
        this.rule = rule;
    }

    public ChimneyRules.Smoke rule() {
        return rule;
    }

    public static ChimneySmoke of(ChimneyRules.Smoke rule) {
        return values()[rule.ordinal()];
    }

    /** The setting after this one in carrot order. */
    public ChimneySmoke next() {
        return of(ChimneyRules.nextSmoke(rule));
    }

    /** The lang key of the setting's name. */
    public String translationKey() {
        return "message.servus_foundation.chimney.smoke." + getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
