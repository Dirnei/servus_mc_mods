package io.github.dirnei.mc.servus.foundation.gametest;

import io.github.dirnei.mc.servus.foundation.naming.MachineKind;
import io.github.dirnei.mc.servus.foundation.naming.MachineKinds;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The naming rules every named machine kind follows, checked the same way for
 * each registered kind: plain, numbered and named labels, name cleaning,
 * saving and carrying in the dismantled item.
 */
public final class MachineNameContract {

    private static final BlockPos FIRST = new BlockPos(2, 1, 2);
    private static final BlockPos SECOND = new BlockPos(8, 1, 8);
    private static final UUID WAREHOUSE = UUID.fromString("00000000-0000-0000-0000-00000000abcd");

    private MachineNameContract() {
    }

    /** Checks every registered kind of the namespace, one after another in one structure, then succeeds. */
    public static void checkAll(GameTestHelper helper, String namespace) {
        int checked = 0;
        for (MachineKind kind : MachineKinds.all()) {
            if (kind.id().getNamespace().equals(namespace)) {
                check(helper, kind);
                checked++;
            }
        }
        helper.assertTrue(checked > 0, "no machine kind of " + namespace + " is registered");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, MachineKind kind) {
        String name = kind.id().toString();
        NamedMachine machine = place(helper, kind, FIRST);
        assertKey(helper, machine.label(), kind.block().get().getDescriptionId(), name + ": a fresh machine reads as its block");
        machine.assignNumber(WAREHOUSE, 3);
        Object[] numbered = assertKey(helper, machine.label(), kind.numberKey(), name + ": a numbered machine reads <kind> #n");
        helper.assertTrue(numbered[0].equals(3), name + ": the label carries number 3, got " + numbered[0]);
        machine.rename("  Big\nOne  ");
        helper.assertTrue(machine.name().equals("BigOne"), name + ": the name is cleaned, got '" + machine.name() + "'");
        Object[] named = assertKey(helper, machine.label(), NamedMachine.NAMED_KEY, name + ": a named machine reads <name> (<kind> #n)");
        helper.assertTrue(named[0].equals("BigOne"), name + ": the label names BigOne, got " + named[0]);
        assertKey(helper, (Component) named[1], kind.numberKey(), name + ": the named label carries the numbered kind");
        machine.rename(" \n ");
        helper.assertTrue(machine.name().isEmpty(), name + ": an empty name removes the name");
        machine.rename("Kept");

        BlockEntity blockEntity = (BlockEntity) machine;
        CompoundTag saved = blockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(blockEntity.getBlockPos(), blockEntity.getBlockState(), saved, helper.getLevel().registryAccess());
        assertKept(helper, (NamedMachine) loaded, name + ": after saving and loading");

        ItemStack item = new ItemStack(kind.block().get());
        item.applyComponents(blockEntity.collectComponents());
        NamedMachine placed = place(helper, kind, SECOND);
        ((BlockEntity) placed).applyComponentsFromItemStack(item);
        assertKept(helper, placed, name + ": placed from the dismantled item");

        helper.assertTrue(Language.getInstance().has(kind.numberKey()), name + ": the kind's lang key " + kind.numberKey() + " is missing");
        helper.setBlock(FIRST, Blocks.AIR.defaultBlockState());
        helper.setBlock(SECOND, Blocks.AIR.defaultBlockState());
    }

    private static NamedMachine place(GameTestHelper helper, MachineKind kind, BlockPos pos) {
        Block block = kind.block().get();
        helper.setBlock(pos, block.defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof NamedMachine, kind.id() + ": the block entity is no named machine");
        return (NamedMachine) blockEntity;
    }

    private static void assertKept(GameTestHelper helper, NamedMachine machine, String context) {
        helper.assertTrue(machine != null && machine.number() == 3 && machine.name().equals("Kept")
                        && !machine.nameSlot().needsNumber(WAREHOUSE),
                context + " the machine is still Kept (#3) of the same warehouse");
    }

    private static Object[] assertKey(GameTestHelper helper, Component label, String key, String message) {
        helper.assertTrue(label.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                message + ", expected " + key + ", got " + label);
        return ((TranslatableContents) label.getContents()).getArgs();
    }
}
