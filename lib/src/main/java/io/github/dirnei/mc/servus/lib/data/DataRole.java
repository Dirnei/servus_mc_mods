package io.github.dirnei.mc.servus.lib.data;

/** What a data member offers the network. Pure, no Minecraft types. */
public enum DataRole {
    /** Exposes the production capability: it can make items. */
    PRODUCER,
    /** Exposes the stock provider or stock sink capability: it holds items. */
    STOCK
}
