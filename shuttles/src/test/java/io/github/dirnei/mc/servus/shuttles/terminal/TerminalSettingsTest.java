package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSettings.Height;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSettings.Sort;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class TerminalSettingsTest {

    @Test
    void roundTrip() {
        TerminalSettings settings = TerminalSettings.DEFAULT.withSort(Sort.MOD).withHeight(Height.TALL);
        assertEquals(settings, TerminalSettings.fromProperties(settings.toProperties()));
    }

    @Test
    void unknownValuesFallBack() {
        Properties properties = new Properties();
        properties.setProperty("sort", "colour");
        properties.setProperty("height", "tall");
        TerminalSettings settings = TerminalSettings.fromProperties(properties);
        assertEquals(TerminalSettings.DEFAULT.sort(), settings.sort());
        assertEquals(Height.TALL, settings.height());
        assertEquals(TerminalSettings.DEFAULT.view(), settings.view());
    }

    @Test
    void nextWraps() {
        assertEquals(Sort.AMOUNT, TerminalSettings.next(Sort.NAME));
        assertEquals(Sort.NAME, TerminalSettings.next(Sort.MOD));
    }
}
