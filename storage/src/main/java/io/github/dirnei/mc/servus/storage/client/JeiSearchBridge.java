package io.github.dirnei.mc.servus.storage.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * JEI's search box as seen by the Counter Terminal. The JEI plugin connects
 * it when JEI's runtime is available; without JEI it stays empty and the
 * terminal keeps its search to itself. Client only.
 */
public final class JeiSearchBridge {

    private static volatile Consumer<String> writer;
    private static volatile Supplier<String> reader;

    private JeiSearchBridge() {
    }

    public static void connect(Consumer<String> write, Supplier<String> read) {
        writer = write;
        reader = read;
    }

    public static void disconnect() {
        writer = null;
        reader = null;
    }

    public static boolean available() {
        return writer != null && reader != null;
    }

    public static void write(String text) {
        Consumer<String> target = writer;
        if (target != null) {
            target.accept(text);
        }
    }

    /** JEI's current search text, or null without JEI. */
    public static String read() {
        Supplier<String> source = reader;
        return source == null ? null : source.get();
    }
}
