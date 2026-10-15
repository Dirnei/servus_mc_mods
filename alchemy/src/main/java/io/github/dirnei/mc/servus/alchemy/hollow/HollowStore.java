package io.github.dirnei.mc.servus.alchemy.hollow;

/** The Gnome Hollow's internal store: how many of its vein's yield the gnome has dug and not yet handed out. Pure Java. */
public final class HollowStore {

    private int count;
    private int capacity;

    public HollowStore(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public int count() {
        return count;
    }

    public int capacity() {
        return capacity;
    }

    /** A smaller capacity keeps what is stored; the store only takes nothing more until it is below the new capacity. */
    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public boolean hasRoom() {
        return count < capacity;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    /** Adds one item if there is room; returns whether it did. */
    public boolean add() {
        if (!hasRoom()) {
            return false;
        }
        count++;
        return true;
    }

    /** Empties the store and returns how many items it held. */
    public int takeAll() {
        int taken = count;
        count = 0;
        return taken;
    }

    /** Sets the count as saved, never below zero. */
    public void load(int count) {
        this.count = Math.max(0, count);
    }
}
