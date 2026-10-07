package io.github.dirnei.mc.servus.storage.board;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One work order as the board shows it.
 *
 * @param id        the order id
 * @param item      a one item stack of what is made
 * @param amount    how many are made
 * @param state     running, blocked, done or cancelled
 * @param reason    why the order is blocked, empty otherwise
 * @param requestId the terminal request, 0 for a manual order, negative for a stock rule
 * @param steps     the steps in plan order
 */
public record OrderRow(int id, ItemStack item, int amount, String state, String reason, int requestId, List<StepRow> steps) {

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderRow> STREAM_CODEC = StreamCodec.of(OrderRow::write, OrderRow::read);

    public boolean open() {
        return state.equals("running") || state.equals("blocked");
    }

    private static void write(RegistryFriendlyByteBuf buf, OrderRow row) {
        buf.writeVarInt(row.id);
        ItemStack.STREAM_CODEC.encode(buf, row.item);
        buf.writeVarInt(row.amount);
        buf.writeUtf(row.state);
        buf.writeUtf(row.reason);
        buf.writeInt(row.requestId);
        buf.writeVarInt(row.steps.size());
        for (StepRow step : row.steps) {
            StepRow.STREAM_CODEC.encode(buf, step);
        }
    }

    private static OrderRow read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        ItemStack item = ItemStack.STREAM_CODEC.decode(buf);
        int amount = buf.readVarInt();
        String state = buf.readUtf();
        String reason = buf.readUtf();
        int requestId = buf.readInt();
        int count = buf.readVarInt();
        List<StepRow> steps = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            steps.add(StepRow.STREAM_CODEC.decode(buf));
        }
        return new OrderRow(id, item, amount, state, reason, requestId, steps);
    }
}
