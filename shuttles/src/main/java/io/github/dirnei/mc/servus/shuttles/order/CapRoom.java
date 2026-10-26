package io.github.dirnei.mc.servus.shuttles.order;

/** How much of an offer may enter a warehouse under a stock rule's cap. Pure, no Minecraft imports. */
public final class CapRoom {

    private CapRoom() {
    }

    /**
     * @param cap     the cap, 0 or less for none
     * @param held    what the warehouse holds and has on its way in
     * @param offered what is offered
     * @return how many of {@code offered} may enter: all without a cap, else no more than {@code cap - held}
     */
    public static int room(int cap, long held, int offered) {
        if (cap <= 0) {
            return Math.max(0, offered);
        }
        return (int) Math.max(0, Math.min(offered, cap - held));
    }
}
