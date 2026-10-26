package io.github.dirnei.mc.servus.shuttles.lock;

/**
 * How much of a buffer slot's unreserved surplus joins a put-away no shuttle
 * has loaded yet: a job carries at most a stack. Pure Java.
 */
public final class TopUp {

    private TopUp() {
    }

    /**
     * @param jobTotal  how many items the job's OUT locks already reserve
     * @param surplus   the items of the slot no OUT lock reserves
     * @param stackSize the item's stack size
     * @return how many of the surplus join the job, never below 0
     */
    public static int join(int jobTotal, int surplus, int stackSize) {
        return Math.max(0, Math.min(surplus, stackSize - Math.max(0, jobTotal)));
    }
}
