package io.github.dirnei.mc.servus.foundation.logistics.channel;

import java.util.UUID;

/**
 * One duct channel of a team's channel list. Pure Java.
 *
 * @param id     the global id, never reused; 0 is the Default channel
 * @param team   the team that owns the channel, null for Default
 * @param name   1 to {@link ChannelList#MAX_NAME} characters; empty for Default
 * @param colour a dye colour index, 0 to 15 in dye order ({@link ChannelColours})
 * @param icon   an item id shown as icon, or null for none
 */
public record Channel(int id, UUID team, String name, int colour, String icon) {

    /** The Default channel: shared by everyone, unnamed, white, without icon. */
    public static final Channel DEFAULT = new Channel(ChannelList.DEFAULT_ID, null, "", 0, null);

    public boolean isDefault() {
        return id == ChannelList.DEFAULT_ID;
    }

    /** The ARGB colour of the channel's ring. */
    public int argb() {
        return ChannelColours.argb(colour);
    }
}
