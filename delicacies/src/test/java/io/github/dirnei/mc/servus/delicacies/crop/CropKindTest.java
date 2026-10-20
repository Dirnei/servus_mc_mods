package io.github.dirnei.mc.servus.delicacies.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CropKindTest {

    @Test
    void everyIdIsUnique() {
        Set<String> blocks = new HashSet<>();
        Set<String> items = new HashSet<>();
        for (CropKind crop : CropKind.values()) {
            assertTrue(blocks.add(crop.blockId()), crop.blockId());
            items.add(crop.harvestId());
            items.add(crop.plantingId());
        }
        long seededCrops = Arrays.stream(CropKind.values()).filter(crop -> !crop.selfPlanting()).count();
        assertEquals(CropKind.values().length + seededCrops, items.size());
    }

    @Test
    void selfPlantingCropsPlantTheirHarvest() {
        assertEquals("onion", CropKind.ONION.plantingId());
        assertEquals("horseradish", CropKind.HORSERADISH.plantingId());
        assertEquals(Optional.empty(), CropKind.ONION.seedId());
    }

    @Test
    void seededCropsPlantTheirSeeds() {
        assertEquals("cabbage_seeds", CropKind.CABBAGE.plantingId());
        assertEquals("red_cabbage_seeds", CropKind.RED_CABBAGE.plantingId());
        assertEquals("parsley_seeds", CropKind.PARSLEY.plantingId());
        assertEquals(Optional.of("parsley_seeds"), CropKind.PARSLEY.seedId());
        assertEquals("cabbage", CropKind.CABBAGE.harvestId());
    }

    @Test
    void mustardSeedsAreHarvestAndSeed() {
        assertEquals("mustard_seeds", CropKind.MUSTARD.harvestId());
        assertEquals("mustard_seeds", CropKind.MUSTARD.plantingId());
        assertEquals(Optional.of("mustard_seeds"), CropKind.MUSTARD.seedId());
        assertEquals("mustard_crop", CropKind.MUSTARD.blockId());
    }

    @Test
    void tagPathsFollowTheCommonConvention() {
        assertEquals("crops/red_cabbage", CropKind.RED_CABBAGE.cropTagPath());
        assertEquals("seeds/mustard", CropKind.MUSTARD.seedTagPath());
    }

    @Test
    void stageNames() {
        assertEquals("onion_stage0", CropKind.ONION.stageName(0));
        assertEquals("onion_stage3", CropKind.ONION.stageName(3));
        assertThrows(IllegalArgumentException.class, () -> CropKind.ONION.stageName(4));
    }
}
