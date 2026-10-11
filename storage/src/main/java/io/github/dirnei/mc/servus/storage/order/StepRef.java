package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Names one step of one order of one CPU, so a producer's job can say whose
 * it is across reloads.
 *
 * @param cpu   the CPU's master position
 * @param order the order id on that CPU
 * @param step  the step's place in the order
 */
public record StepRef(BlockPos cpu, int order, int step) {

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Cpu", ValueIo.posTag(cpu));
        tag.putInt("Order", order);
        tag.putInt("Step", step);
        return tag;
    }

    public static Optional<StepRef> load(CompoundTag tag) {
        return ValueIo.readPos(tag, "Cpu").map(cpu -> new StepRef(cpu, tag.getIntOr("Order", 0), tag.getIntOr("Step", 0)));
    }
}
