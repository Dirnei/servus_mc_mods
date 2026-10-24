package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.foundation.data.packet.Grant;
import io.github.dirnei.mc.servus.foundation.data.packet.GrantBook;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketEndpoint;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity.Request;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * The packet endpoint of a Storage Terminal: an item an open request waits
 * for is granted the room left in that request's output slots, any other item
 * the room of the Input row. Grants reserve room by count, since only the
 * request's own deliveries land in its slots. Server thread only.
 */
final class TerminalEndpoint implements PacketEndpoint {

    static final String REFUSE_OUTPUT_FULL = "output_full";
    static final String REFUSE_INPUT_FULL = "input_full";
    /** The owner of a grant into the Input row instead of a request. */
    private static final int INPUT = -1;

    private final StorageTerminalBlockEntity terminal;
    private final GrantBook grants = new GrantBook();
    /** The request id each open grant fills, {@link #INPUT} for the Input row. */
    private final Map<Long, Integer> owners = new HashMap<>();
    private long nextTicket = 1;

    TerminalEndpoint(StorageTerminalBlockEntity terminal) {
        this.terminal = terminal;
    }

    @Override
    public Grant offer(ItemStack like, int amount) {
        if (like.isEmpty() || amount < 1 || terminal.getLevel() == null) {
            return Grant.refused(REFUSE_OUTPUT_FULL);
        }
        ItemKey key = ItemKey.of(like);
        Request request = terminal.waitingRequest(key);
        int owner = request == null ? INPUT : request.id;
        int granted = request == null ? inputRoom(key, amount) : requestRoom(request, key, amount);
        if (granted <= 0) {
            return Grant.refused(request == null ? REFUSE_INPUT_FULL : REFUSE_OUTPUT_FULL);
        }
        Grant grant = Grant.of(this, nextTicket++, granted, terminal.getLevel().getGameTime());
        grants.add(grant);
        owners.put(grant.ticket(), owner);
        return grant;
    }

    @Override
    public ItemStack receive(Grant grant, ItemStack stack) {
        Integer owner = owners.get(grant.ticket());
        if (owner == null || !grants.holds(grant)) {
            return stack;
        }
        if (owner == INPUT) {
            return terminal.input().insertItemStacked(stack, false);
        }
        Request request = terminal.request(owner);
        return request == null ? stack : terminal.deliverToSlots(request, stack);
    }

    @Override
    public void close(Grant grant) {
        grants.remove(grant);
        owners.remove(grant.ticket());
    }

    /** Ends grants nobody uses any more. */
    void sweep(long now) {
        grants.sweep(now);
    }

    /** Ends every grant, for a terminal that goes away. */
    void closeAll() {
        grants.closeAll();
        owners.clear();
    }

    private int requestRoom(Request request, ItemKey key, int amount) {
        List<Integer> counts = new ArrayList<>();
        ItemSlots output = terminal.output();
        for (int slot : request.slots) {
            ItemStack stack = output.getStackInSlot(slot);
            counts.add(stack.isEmpty() ? 0 : key.matches(stack) ? stack.getCount() : RequestRoom.OTHER_ITEM);
        }
        int stackSize = Math.min(key.stack(1).getMaxStackSize(), output.getSlotLimit(0));
        return RequestRoom.grant(amount, request.requested - request.delivered, counts, stackSize, held(request.id));
    }

    private int inputRoom(ItemKey key, int amount) {
        ItemSlots input = terminal.input();
        int stackSize = Math.min(key.stack(1).getMaxStackSize(), input.getSlotLimit(0));
        int room = 0;
        for (int slot = 0; slot < input.getSlots(); slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (stack.isEmpty()) {
                room += stackSize;
            } else if (key.matches(stack)) {
                room += Math.max(0, stackSize - stack.getCount());
            }
        }
        return Math.max(0, Math.min(amount, room - held(INPUT)));
    }

    /** How many items the open grants of that owner still reserve. */
    private int held(int owner) {
        int held = 0;
        for (Grant grant : grants.grants()) {
            if (Integer.valueOf(owner).equals(owners.get(grant.ticket()))) {
                held += grant.left();
            }
        }
        return held;
    }
}
