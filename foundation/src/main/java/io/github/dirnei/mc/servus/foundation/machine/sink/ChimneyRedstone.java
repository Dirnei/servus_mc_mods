package io.github.dirnei.mc.servus.foundation.machine.sink;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** The redstone mode of a Chimney block, a block state value over {@link ChimneyRules.Mode}. */
public enum ChimneyRedstone implements StringRepresentable {
    ALWAYS(ChimneyRules.Mode.ALWAYS),
    POWERED(ChimneyRules.Mode.POWERED),
    UNPOWERED(ChimneyRules.Mode.UNPOWERED);

    private final ChimneyRules.Mode rule;

    ChimneyRedstone(ChimneyRules.Mode rule) {
        this.rule = rule;
    }

    public ChimneyRules.Mode rule() {
        return rule;
    }

    public static ChimneyRedstone of(ChimneyRules.Mode rule) {
        return values()[rule.ordinal()];
    }

    /** The mode after this one in carrot order. */
    public ChimneyRedstone next() {
        return of(ChimneyRules.nextMode(rule));
    }

    /** The lang key of the mode's name. */
    public String translationKey() {
        return "message.servus_foundation.chimney.redstone." + getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
