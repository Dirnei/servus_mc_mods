package io.github.dirnei.mc.servus.foundation.adapter;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import java.util.List;
import java.util.Optional;

/** Turns a profile role into the absolute side of the machine to use. */
public final class ProfileSides {

    /** The order in which sides are tried when several share a role. */
    public static final List<RelativeSide> ORDER = List.of(RelativeSide.TOP, RelativeSide.BOTTOM, RelativeSide.BACK,
            RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.FRONT);

    private ProfileSides() {
    }

    /** The absolute side of the first relative side with the role, for a machine facing {@code facing}. */
    public static Optional<Side> firstOf(MachineProfile profile, MachineProfile.Role role, Side facing) {
        for (RelativeSide side : ORDER) {
            if (profile.role(side) == role) {
                return Optional.of(side.toSide(facing));
            }
        }
        return Optional.empty();
    }
}
