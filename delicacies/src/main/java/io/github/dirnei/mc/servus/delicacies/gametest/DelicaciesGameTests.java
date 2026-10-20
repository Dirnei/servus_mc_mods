package io.github.dirnei.mc.servus.delicacies.gametest;

import java.util.List;

/** The Delicacies GameTest classes, registered by the mod constructor. */
public final class DelicaciesGameTests {

    public static final List<Class<?>> CLASSES = List.of(
            DelicaciesSetupGameTests.class,
            CropGameTests.class,
            StoveGameTests.class,
            CookingGameTests.class,
            CuttingBoardGameTests.class,
            BakeryGameTests.class,
            MeatGameTests.class,
            SidesGameTests.class,
            TellerGameTests.class);

    private DelicaciesGameTests() {
    }
}
