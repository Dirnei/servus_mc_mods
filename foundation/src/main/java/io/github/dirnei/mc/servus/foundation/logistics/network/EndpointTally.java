package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.List;

/**
 * Counts the duct sides of one channel that take from a block ({@code in}: pull in,
 * in and out) and that deliver to a block ({@code out}: push out, in and out).
 * An in and out side counts on both; off sides and other channels count on neither.
 *
 * @param in  sides on the channel the run takes from
 * @param out sides on the channel the run delivers to
 */
public record EndpointTally(int in, int out) {

    /** One duct side that touches a block: its channel id and mode. */
    public record Side(int channel, SideMode mode) {
    }

    /** The tally of {@code channel} over {@code sides}. */
    public static EndpointTally of(int channel, List<Side> sides) {
        int in = 0;
        int out = 0;
        for (Side side : sides) {
            if (side.channel() != channel) {
                continue;
            }
            if (takes(side.mode())) {
                in++;
            }
            if (delivers(side.mode())) {
                out++;
            }
        }
        return new EndpointTally(in, out);
    }

    private static boolean takes(SideMode mode) {
        return mode == SideMode.INPUT || mode == SideMode.BOTH;
    }

    private static boolean delivers(SideMode mode) {
        return mode == SideMode.OUTPUT || mode == SideMode.BOTH;
    }
}
