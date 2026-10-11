package io.github.dirnei.mc.servus.storage.shuttle.module;

/**
 * What a module is. Structural traits live here; balance numbers such as
 * the Cargo Hold's stack count come from warehouse.json.
 *
 * @param id        the module's name, the item path
 * @param slot      the shuttle slot it fits
 * @param fieldOnly whether a shuttle with it serves fields only and never warehouse jobs
 * @param cargoHold whether it lets the shuttle carry several stacks
 */
public record ModuleSpec(String id, ModuleSlot slot, boolean fieldOnly, boolean cargoHold) {
}
