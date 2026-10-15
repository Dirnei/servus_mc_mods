package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttles;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSettings;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The player's Storage Terminal view settings, shared by every terminal and
 * kept in {@code config/servus_shuttles-terminal.properties}. Client only.
 */
public final class ClientTerminalSettings {

    private static final String FILE = ServusShuttlesIds.MOD_ID + "-terminal.properties";
    private static TerminalSettings settings;

    private ClientTerminalSettings() {
    }

    public static TerminalSettings get() {
        if (settings == null) {
            settings = load();
        }
        return settings;
    }

    public static void set(TerminalSettings value) {
        settings = value;
        save(value);
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static TerminalSettings load() {
        Path path = file();
        if (!Files.exists(path)) {
            return TerminalSettings.DEFAULT;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        } catch (IOException e) {
            ServusShuttles.LOGGER.warn("Could not read {}: {}", path, e.getMessage());
            return TerminalSettings.DEFAULT;
        }
        return TerminalSettings.fromProperties(properties);
    }

    private static void save(TerminalSettings value) {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                value.toProperties().store(writer, "Servus: Shuttles Storage Terminal view settings");
            }
        } catch (IOException e) {
            ServusShuttles.LOGGER.warn("Could not write {}: {}", path, e.getMessage());
        }
    }
}
