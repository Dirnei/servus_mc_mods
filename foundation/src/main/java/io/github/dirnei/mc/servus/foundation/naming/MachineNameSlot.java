package io.github.dirnei.mc.servus.foundation.naming;

import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationComponents;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The number, name and numbering warehouse of one named machine. The owner
 * forwards its save, load, update tag and component calls here, usually
 * through {@link NamedMachines}.
 */
public final class MachineNameSlot {

    private static final String NUMBER = "MachineNumber";
    private static final String NAME = "MachineName";
    private static final String WAREHOUSE = "MachineWarehouse";

    private final BlockEntity owner;
    private final MachineKind kind;
    /** The number its warehouse gave, 0 while it has none. */
    private int number;
    private String name = "";
    /** The warehouse that gave the number. */
    private @Nullable UUID numberedBy;

    public MachineNameSlot(BlockEntity owner, MachineKind kind) {
        this.owner = owner;
        this.kind = kind;
    }

    public MachineKind kind() {
        return kind;
    }

    /** Where the machine's block entity stands. */
    public BlockPos pos() {
        return owner.getBlockPos();
    }

    public int number() {
        return number;
    }

    public String name() {
        return name;
    }

    /** Whether the warehouse with that id has to give this machine a number. */
    public boolean needsNumber(UUID warehouse) {
        return MachineNumber.needsNumber(numberedBy, number, warehouse);
    }

    /** Takes a number from the warehouse with that id. Server side. */
    public void assign(UUID warehouse, int given) {
        numberedBy = warehouse;
        number = Math.max(0, given);
        changed();
    }

    /** Sets the name after cleaning it; an empty name removes it. Returns the kept name. */
    public String rename(String typed) {
        String cleaned = NameText.clean(typed);
        if (!cleaned.equals(name)) {
            name = cleaned;
            changed();
        }
        return name;
    }

    /** Hands number, name and warehouse to another slot, as when an elevator shaft's lowest segment changes, and clears this one. */
    public void moveTo(MachineNameSlot heir) {
        heir.number = number;
        heir.name = name;
        heir.numberedBy = numberedBy;
        heir.changed();
        clear();
    }

    /** Forgets number, name and warehouse. */
    public void clear() {
        if (number == 0 && name.isEmpty() && numberedBy == null) {
            return;
        }
        number = 0;
        name = "";
        numberedBy = null;
        changed();
    }

    /** Whether the slot holds a number or a name. */
    public boolean isEmpty() {
        return number == 0 && name.isEmpty();
    }

    /** The label with the given name: the machine's own name or none, for the hint of the name field. */
    Component label(String shownName) {
        MachineLabel label = MachineLabel.of(number, shownName);
        return switch (label.form()) {
            case NUMBERED_NAMED -> Component.translatable(NamedMachine.NAMED_KEY, label.name(), kind.numbered(label.number()));
            case NUMBERED -> kind.numbered(label.number());
            case NAMED -> Component.literal(label.name());
            case PLAIN -> kind.plain();
        };
    }

    private void changed() {
        owner.setChanged();
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = owner.getBlockState();
            level.sendBlockUpdated(owner.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** From {@code saveAdditional}. */
    public void save(ValueOutput output) {
        if (number > 0) {
            output.putInt(NUMBER, number);
        }
        if (!name.isEmpty()) {
            output.putString(NAME, name);
        }
        if (numberedBy != null) {
            output.store(WAREHOUSE, UUIDUtil.CODEC, numberedBy);
        }
    }

    /** From {@code getUpdateTag}: number and name, read back by {@link #load}. */
    public void writeUpdate(CompoundTag tag) {
        tag.putInt(NUMBER, number);
        tag.putString(NAME, name);
    }

    /** From {@code loadAdditional}, for saves and update tags. */
    public void load(ValueInput input) {
        number = Math.max(0, input.getIntOr(NUMBER, 0));
        name = NameText.clean(input.getStringOr(NAME, ""));
        numberedBy = input.read(WAREHOUSE, UUIDUtil.CODEC).orElse(null);
    }

    /** From {@code collectImplicitComponents}: number, name and warehouse go with a dismantled machine. */
    public void collect(DataComponentMap.Builder builder) {
        if (number > 0) {
            builder.set(ServusFoundationComponents.MACHINE_NUMBER.get(), number);
        }
        if (!name.isEmpty()) {
            builder.set(ServusFoundationComponents.MACHINE_NAME.get(), name);
        }
        if (numberedBy != null) {
            builder.set(ServusFoundationComponents.MACHINE_WAREHOUSE.get(), numberedBy);
        }
    }

    /** From {@code applyImplicitComponents}: what the placed item carried. */
    public void apply(DataComponentGetter input) {
        Integer carried = input.get(ServusFoundationComponents.MACHINE_NUMBER.get());
        number = carried == null ? 0 : Math.max(0, carried);
        name = NameText.clean(input.getOrDefault(ServusFoundationComponents.MACHINE_NAME.get(), ""));
        numberedBy = input.get(ServusFoundationComponents.MACHINE_WAREHOUSE.get());
    }

    /** From {@code removeComponentsFromTag}. */
    public static void removeFromTag(ValueOutput output) {
        output.discard(NUMBER);
        output.discard(NAME);
        output.discard(WAREHOUSE);
    }
}
