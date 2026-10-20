package io.github.dirnei.mc.servus.delicacies.crop;

/** Maps a crop's age to the growth look it shows, the way vanilla carrots do. */
public final class CropStages {

    /** The oldest age of a Delicacies crop. */
    public static final int MAX_AGE = 7;

    private static final int[] STAGE_BY_AGE = {0, 0, 1, 1, 2, 2, 2, 3};

    private CropStages() {
    }

    /** The growth look, 0 to {@link CropKind#STAGES} - 1, for an age from 0 to {@link #MAX_AGE}. */
    public static int stageOf(int age) {
        if (age < 0 || age > MAX_AGE) {
            throw new IllegalArgumentException("age " + age + " is outside 0 to " + MAX_AGE);
        }
        return STAGE_BY_AGE[age];
    }
}
