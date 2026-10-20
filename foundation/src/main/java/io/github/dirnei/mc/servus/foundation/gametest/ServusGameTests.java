package io.github.dirnei.mc.servus.foundation.gametest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the {@link ServusGameTest} methods of a mod. Each method becomes a
 * test function in {@code minecraft:test_function} and a function test
 * instance in the empty environment of its batch, named
 * {@code <mod>:<class>.<method>} in lower case.
 */
public final class ServusGameTests {

    /** The shared 12 by 6 by 12 empty structure every Servus mod ships. */
    public static final String EMPTY = "empty_12x6x12";

    /** The batch, and test environment, of every test that names no other. */
    public static final String DEFAULT_BATCH = "default";

    private record Test(Identifier id, Method method, ServusGameTest settings) {
    }

    private ServusGameTests() {
    }

    /** Registers the tests of {@code classes} for {@code modId}. Call from the mod constructor. */
    public static void register(IEventBus modBus, String modId, List<Class<?>> classes) {
        List<Test> tests = scan(modId, classes);
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, modId);
        for (Test test : tests) {
            functions.register(test.id().getPath(), () -> helper -> invoke(test.method(), helper));
        }
        functions.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, event -> {
            Map<String, Holder<TestEnvironmentDefinition<?>>> batches = new HashMap<>();
            for (Test test : tests) {
                Holder<TestEnvironmentDefinition<?>> environment = batches.computeIfAbsent(test.settings().batch(),
                        batch -> event.registerEnvironment(Identifier.fromNamespaceAndPath(modId, batch)));
                TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment,
                        Identifier.fromNamespaceAndPath(modId, test.settings().template()),
                        test.settings().timeoutTicks(), 0, true);
                event.registerTest(test.id(), new FunctionGameTestInstance(
                        ResourceKey.create(Registries.TEST_FUNCTION, test.id()), data));
            }
        });
    }

    private static List<Test> scan(String modId, List<Class<?>> classes) {
        List<Test> tests = new ArrayList<>();
        for (Class<?> type : classes) {
            for (Method method : type.getDeclaredMethods()) {
                ServusGameTest settings = method.getAnnotation(ServusGameTest.class);
                if (settings == null) {
                    continue;
                }
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                        || method.getParameterTypes()[0] != GameTestHelper.class) {
                    throw new IllegalStateException("GameTest " + type.getName() + "#" + method.getName()
                            + " must be static and take only a GameTestHelper");
                }
                method.setAccessible(true);
                String path = (type.getSimpleName() + "." + method.getName()).toLowerCase(Locale.ROOT);
                tests.add(new Test(Identifier.fromNamespaceAndPath(modId, path), method, settings));
            }
        }
        tests.sort(Comparator.comparing(test -> test.id().getPath()));
        return tests;
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
