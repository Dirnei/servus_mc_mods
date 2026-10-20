package io.github.dirnei.mc.servus.shuttles.shuttle.module;

/**
 * What a tool module is. A shuttle holds one tool.
 *
 * @param id        the module's name, the item path
 * @param fieldOnly whether a shuttle with it takes only the jobs of its own tool and never warehouse picks or put-aways
 * @param wide      whether it is too wide for a lift car, so its shuttle never rides one
 */
public record ModuleSpec(String id, boolean fieldOnly, boolean wide) {

    /** The id of the Harvester module, which field jobs ask for. */
    public static final String HARVESTER = "harvester";

    /** The id of the Spreader module, which fertilize runs ask for. */
    public static final String SPREADER = "spreader";

    /** The id of the Tanker module, which tanker runs ask for. */
    public static final String TANKER = "tanker";
}
