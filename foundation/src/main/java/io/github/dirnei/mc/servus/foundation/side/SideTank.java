package io.github.dirnei.mc.servus.foundation.side;

/**
 * One tank a block offers in its Fluids layer.
 *
 * @param key   translation key of the tank's name, for example the fluid type's description id
 * @param color ARGB colour the side panel and the markers use for sides set to this tank
 * @param input whether the tank takes fluid in (an input tank) or gives it out (an output tank)
 */
public record SideTank(String key, int color, boolean input) {
}
