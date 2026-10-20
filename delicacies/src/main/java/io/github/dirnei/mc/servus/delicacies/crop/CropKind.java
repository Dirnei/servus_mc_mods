package io.github.dirnei.mc.servus.delicacies.crop;

import java.util.Optional;

/**
 * The Delicacies crops and the ids derived from their names. A seeded crop is
 * planted from {@code <name>_seeds} and harvested as {@code <name>}; a self
 * planting crop is planted from its harvest, like a vanilla carrot.
 */
public enum CropKind {
    CABBAGE("cabbage", "cabbage", false),
    RED_CABBAGE("red_cabbage", "red_cabbage", false),
    ONION("onion", "onion", true),
    PARSLEY("parsley", "parsley", false),
    HORSERADISH("horseradish", "horseradish", true),
    MUSTARD("mustard", "mustard_seeds", true);

    /** Number of growth looks a crop shows over its ages. */
    public static final int STAGES = 4;

    private final String name;
    private final String harvestId;
    private final boolean selfPlanting;

    CropKind(String name, String harvestId, boolean selfPlanting) {
        this.name = name;
        this.harvestId = harvestId;
        this.selfPlanting = selfPlanting;
    }

    /** The crop's name, also the last part of its tag paths. */
    public String cropName() {
        return name;
    }

    /** Whether the harvest itself plants the crop. */
    public boolean selfPlanting() {
        return selfPlanting;
    }

    /** Id of the crop block. */
    public String blockId() {
        return name + "_crop";
    }

    /** Id of the item a ripe crop gives. */
    public String harvestId() {
        return harvestId;
    }

    /** Id of the item that plants the crop. */
    public String plantingId() {
        return selfPlanting ? harvestId : name + "_seeds";
    }

    /** Id of the item tagged as this crop's seed, empty for a crop planted from a vegetable. */
    public Optional<String> seedId() {
        return plantingId().endsWith("_seeds") ? Optional.of(plantingId()) : Optional.empty();
    }

    /** Path of the common crop tag, {@code crops/<name>}. */
    public String cropTagPath() {
        return "crops/" + name;
    }

    /** Path of the common seed tag, {@code seeds/<name>}. */
    public String seedTagPath() {
        return "seeds/" + name;
    }

    /** Texture and model name of a growth stage, {@code <name>_stage<n>}. */
    public String stageName(int stage) {
        if (stage < 0 || stage >= STAGES) {
            throw new IllegalArgumentException("stage " + stage + " is outside 0 to " + (STAGES - 1));
        }
        return name + "_stage" + stage;
    }
}
