package io.github.dirnei.mc.servus.shuttles.lock;

import java.util.List;

/** A block whose slots shuttle jobs lock: rack bays and the stations' buffers. */
public interface LockHolder {

    /** Every lockable inventory of the block, in a fixed order. */
    List<LockedSlots> lockedSlots();
}
