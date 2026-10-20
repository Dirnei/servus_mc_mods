package io.github.dirnei.mc.servus.brewery.gametest;

import java.util.List;

/** The Brewery GameTest classes, registered by the mod constructor. */
public final class BreweryGameTests {

    public static final List<Class<?>> CLASSES = List.of(
            BrewerySetupGameTests.class,
            BreweryCropGameTests.class,
            MaltingGameTests.class,
            BrewhouseGameTests.class,
            BeerGameTests.class,
            RauschGameTests.class);

    private BreweryGameTests() {
    }
}
