package io.github.dirnei.mc.servus.foundation.data;

import java.util.Set;
import net.minecraft.core.BlockPos;

/** A block on a data network and what it offers. */
public record DataMember(BlockPos pos, Set<DataRole> roles) {

    public boolean has(DataRole role) {
        return roles.contains(role);
    }
}
