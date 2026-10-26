package io.github.dirnei.mc.servus.foundation.naming;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;

/**
 * A machine that shuttles or the Crafting CPU work with: it has an optional
 * name and, once a warehouse numbered it, a number per kind. Implemented by
 * its block entity, which keeps a {@link MachineNameSlot} and forwards its
 * save, load, update tag and component hooks through {@link NamedMachines}
 * (base classes such as Foundation's machine base already do).
 */
public interface NamedMachine {

    /** The lang key of a numbered and named label: name, then the numbered kind label. */
    String NAMED_KEY = "machine.servus_foundation.label.named";

    MachineNameSlot nameSlot();

    default MachineKind machineKind() {
        return nameSlot().kind();
    }

    /** The number its warehouse gave, 0 while it has none. */
    default int number() {
        return nameSlot().number();
    }

    /** The name the player gave, empty when unnamed. */
    default String name() {
        return nameSlot().name();
    }

    /**
     * How players read this machine: "Field Head #2", "Wheat North (Field Head #2)",
     * the name alone, or the block name while it has neither. Resolved by the client.
     */
    default Component label() {
        return nameSlot().label(name());
    }

    /** The label as if the machine had no name: the hint of the name field. */
    default Component numberLabel() {
        return nameSlot().label("");
    }

    /** Sets the name after cleaning it; an empty name removes it. Server side. */
    default String rename(String typed) {
        return nameSlot().rename(typed);
    }

    /** Whether the warehouse with that id has to give this machine a number. */
    default boolean needsNumber(UUID warehouse) {
        return takesNumber() && nameSlot().needsNumber(warehouse);
    }

    /** Takes a number from the warehouse with that id. Server side. */
    default void assignNumber(UUID warehouse, int number) {
        nameSlot().assign(warehouse, number);
    }

    /** Whether this block holds the number of its machine; false for the parts of a shaft or box other than the one that keeps it. */
    default boolean takesNumber() {
        return true;
    }

    /** The label for chat and action bar messages: in the kind's chat colour, if it has one. */
    default Component chatLabel() {
        return machineKind().inChat(label());
    }

    /** What the Data Potato prints first: the label. */
    default List<Component> potatoLines() {
        return List.of(chatLabel());
    }
}
