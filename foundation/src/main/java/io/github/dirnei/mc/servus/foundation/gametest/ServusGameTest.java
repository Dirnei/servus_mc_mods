package io.github.dirnei.mc.servus.foundation.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code public static void name(GameTestHelper helper)} method as a
 * GameTest. {@link ServusGameTests} registers it as a test function and a test
 * instance, the registry based replacement of the annotation driven GameTests
 * that vanilla dropped in 1.21.5.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServusGameTest {

    /** The structure the test runs in, a path in the mod's namespace. */
    String template() default ServusGameTests.EMPTY;

    /** Ticks before the test fails. */
    int timeoutTicks() default 100;
}
