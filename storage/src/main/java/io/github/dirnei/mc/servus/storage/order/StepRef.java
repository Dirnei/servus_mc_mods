package io.github.dirnei.mc.servus.storage.order;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

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
        tag.put("Cpu", NbtUtils.writeBlockPos(cpu));
        tag.putInt("Order", order);
        tag.putInt("Step", step);
        return tag;
    }

    public static Optional<StepRef> load(CompoundTag tag) {
        return NbtUtils.readBlockPos(tag, "Cpu").map(cpu -> new StepRef(cpu, tag.getInt("Order"), tag.getInt("Step")));
    }
}
