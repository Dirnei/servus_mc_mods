package io.github.dirnei.mc.servus.delicacies.crop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CropStagesTest {

    @Test
    void agesMapLikeVanillaCarrots() {
        int[] stages = IntStream.rangeClosed(0, CropStages.MAX_AGE).map(CropStages::stageOf).toArray();
        assertArrayEquals(new int[] {0, 0, 1, 1, 2, 2, 2, 3}, stages);
    }

    @Test
    void ripeAgeShowsTheLastStage() {
        assertEquals(CropKind.STAGES - 1, CropStages.stageOf(CropStages.MAX_AGE));
    }

    @Test
    void agesOutsideTheRangeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CropStages.stageOf(-1));
        assertThrows(IllegalArgumentException.class, () -> CropStages.stageOf(8));
    }
}
