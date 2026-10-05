package io.github.dirnei.mc.servus.manufacturing.machine.extractor;

import io.github.dirnei.mc.servus.manufacturing.deposit.DepositDefinition;
import io.github.dirnei.mc.servus.manufacturing.deposit.DepositManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Shared by every extractor: what the deposit under a machine yields. */
public final class DepositExtraction {

    private DepositExtraction() {
    }

    public static Optional<DepositDefinition> depositBelow(Level level, BlockPos pos) {
        if (level == null) {
            return Optional.empty();
        }
        return DepositManager.INSTANCE.forBlock(level.getBlockState(pos.below()).getBlock());
    }
}
