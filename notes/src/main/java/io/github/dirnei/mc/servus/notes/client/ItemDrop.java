package io.github.dirnei.mc.servus.notes.client;

import java.util.function.Consumer;

/**
 * A place in the notebook that takes an item dragged onto it, for JEI's ghost ingredients.
 *
 * @param accept gets the dropped item's id
 */
public record ItemDrop(int x, int y, int width, int height, Consumer<String> accept) {
}
