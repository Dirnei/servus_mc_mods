package io.github.dirnei.mc.servus.logistics.duct;

/** What a utility duct can carry. Each channel is placed as its own item. */
public enum Channel {
    ITEM("item"),
    ENERGY("energy");

    public static final Channel[] VALUES = values();

    private final String id;

    Channel(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Channel byId(String id) {
        for (Channel channel : VALUES) {
            if (channel.id.equals(id)) {
                return channel;
            }
        }
        return null;
    }

    public static Channel byOrdinal(int ordinal) {
        return VALUES[Math.floorMod(ordinal, VALUES.length)];
    }
}
