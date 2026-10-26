package io.github.dirnei.mc.servus.foundation.naming;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The block entity hooks of a named machine, each a no-op for block entities
 * that are not one, so a base class can call them for all its subclasses.
 */
public final class NamedMachines {

    private NamedMachines() {
    }

    /** From {@code saveAdditional}. */
    public static void save(BlockEntity blockEntity, ValueOutput output) {
        if (blockEntity instanceof NamedMachine named) {
            named.nameSlot().save(output);
        }
    }

    /** From {@code loadAdditional}. */
    public static void load(BlockEntity blockEntity, ValueInput input) {
        if (blockEntity instanceof NamedMachine named) {
            named.nameSlot().load(input);
        }
    }

    /** From {@code getUpdateTag}. */
    public static void writeUpdate(BlockEntity blockEntity, CompoundTag tag) {
        if (blockEntity instanceof NamedMachine named) {
            named.nameSlot().writeUpdate(tag);
        }
    }

    /** From {@code collectImplicitComponents}. */
    public static void collect(BlockEntity blockEntity, DataComponentMap.Builder builder) {
        if (blockEntity instanceof NamedMachine named) {
            named.nameSlot().collect(builder);
        }
    }

    /** From {@code applyImplicitComponents}. */
    public static void apply(BlockEntity blockEntity, DataComponentGetter input) {
        if (blockEntity instanceof NamedMachine named) {
            named.nameSlot().apply(input);
        }
    }

    /** From {@code removeComponentsFromTag}. */
    public static void removeFromTag(BlockEntity blockEntity, ValueOutput output) {
        if (blockEntity instanceof NamedMachine) {
            MachineNameSlot.removeFromTag(output);
        }
    }

    /** The label of a named machine, else the block's name: for {@code getDisplayName}. */
    public static Component displayName(BlockEntity blockEntity) {
        return blockEntity instanceof NamedMachine named ? named.label() : blockEntity.getBlockState().getBlock().getName();
    }

    /** Puts the number and name of a named machine onto the item its dismantle drops. */
    public static ItemStack stamp(ItemStack stack, BlockEntity blockEntity) {
        if (blockEntity instanceof NamedMachine) {
            DataComponentMap.Builder builder = DataComponentMap.builder();
            collect(blockEntity, builder);
            stack.applyComponents(builder.build());
        }
        return stack;
    }
}
