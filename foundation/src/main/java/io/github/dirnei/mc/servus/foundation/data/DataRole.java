package io.github.dirnei.mc.servus.foundation.data;

/** What a data member offers the network. Pure, no Minecraft types. */
public enum DataRole {
    /** Exposes the production capability: it can make items. */
    PRODUCER,
    /** Exposes the stock provider or stock sink capability: it holds items. */
    STOCK,
    /** Exposes the data link capability: it is on the line without making or holding items. */
    LINK,
    /** Exposes the upkeep capability: it keeps a consumable stocked from suppliers on the line. */
    UPKEEP
}
