package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Converts between block positions and the {@link FieldPos} of the pure field logic. */
public final class FieldPositions {

    private FieldPositions() {
    }

    public static FieldPos of(BlockPos pos) {
        return new FieldPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static List<FieldPos> of(Collection<BlockPos> positions) {
        List<FieldPos> list = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            list.add(of(pos));
        }
        return list;
    }

    public static BlockPos toBlockPos(FieldPos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }
}
