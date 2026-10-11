package io.github.dirnei.mc.servus.storage.shuttle.module;

/**
 * What a tool module is. A shuttle holds one tool; balance numbers live in
 * the data files, not here.
 *
 * @param id        the module's name, the item path
 * @param fieldOnly whether a shuttle with it serves fields only and never warehouse jobs
 * @param wide      whether it is too wide for a lift car, so its shuttle never rides one
 */
public record ModuleSpec(String id, boolean fieldOnly, boolean wide) {

    /** The id of the Harvester module, which field jobs ask for. */
    public static final String HARVESTER = "harvester";
}
