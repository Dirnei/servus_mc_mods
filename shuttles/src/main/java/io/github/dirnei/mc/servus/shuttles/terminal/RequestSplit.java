package io.github.dirnei.mc.servus.shuttles.terminal;

/**
 * How a Storage Terminal request is split between stock and crafting: what
 * the amount prompt starts at, how high it may go, and how many items come
 * from stock or a work order. Pure, no Minecraft imports.
 *
 * @param fromStock items shipped from stock
 * @param toCraft   items a work order crafts
 */
public record RequestSplit(int fromStock, int toCraft) {

    /** The amount the prompt starts at: a stack when the rest can be crafted, else no more than is requestable. */
    public static int prefill(int stackSize, long requestable, boolean craftable) {
        int stack = Math.max(1, stackSize);
        return craftable ? stack : (int) Math.max(AmountSteps.MIN, Math.min(stack, requestable));
    }

    /** The highest amount the prompt allows. */
    public static int cap(long requestable, boolean craftable) {
        return craftable ? AmountSteps.MAX : (int) Math.max(AmountSteps.MIN, Math.min(AmountSteps.MAX, requestable));
    }

    /**
     * Splits an asked amount.
     *
     * @param asked         how many the player asked for
     * @param available     how many can be shipped from stock
     * @param restCraftable whether a work order can craft what stock lacks
     */
    public static RequestSplit of(int asked, long available, boolean restCraftable) {
        int fromStock = (int) Math.max(0, Math.min(asked, available));
        return new RequestSplit(fromStock, restCraftable ? asked - fromStock : 0);
    }

    /** Items the request is for. */
    public int total() {
        return fromStock + toCraft;
    }
}
