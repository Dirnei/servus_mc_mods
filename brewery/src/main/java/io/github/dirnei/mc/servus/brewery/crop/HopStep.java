package io.github.dirnei.mc.servus.brewery.crop;

/**
 * One growth step of a hop vine: the part of the column, counted from the
 * foot at 0, and the stage it turns into.
 */
public record HopStep(int part, HopStage stage) {
}
