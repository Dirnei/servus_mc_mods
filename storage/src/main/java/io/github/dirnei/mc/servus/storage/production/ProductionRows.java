package io.github.dirnei.mc.servus.storage.production;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** What the Production Terminal shows, as plain rows that travel to the client. */
public final class ProductionRows {

    private ProductionRows() {
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS =
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());

    /**
     * One recipe a producer reports.
     *
     * @param inputs  consumed per run
     * @param outputs made per run
     * @param ticks   time per run
     */
    public record RecipeRow(List<ItemStack> inputs, List<ItemStack> outputs, int ticks) {
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeRow> STREAM_CODEC = StreamCodec.composite(
                STACKS, RecipeRow::inputs,
                STACKS, RecipeRow::outputs,
                ByteBufCodecs.VAR_INT, RecipeRow::ticks,
                RecipeRow::new);
    }

    /**
     * One producer of the data line.
     *
     * @param pos     where it stands
     * @param name    its block name
     * @param working the first output of the active recipe, empty when none runs
     * @param state   working, idle or no_recipes
     * @param recipes the recipes it reports
     */
    public record MachineRow(BlockPos pos, String name, ItemStack working, String state, int left, List<RecipeRow> recipes) {
        public static final StreamCodec<RegistryFriendlyByteBuf, MachineRow> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, MachineRow::pos,
                ByteBufCodecs.STRING_UTF8, MachineRow::name,
                ItemStack.OPTIONAL_STREAM_CODEC, MachineRow::working,
                ByteBufCodecs.STRING_UTF8, MachineRow::state,
                ByteBufCodecs.VAR_INT, MachineRow::left,
                RecipeRow.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRow::recipes,
                MachineRow::new);
    }

    /**
     * One step of an order.
     *
     * @param producer the producer the step runs on
     * @param machine  the producer's block name
     * @param output   the step's first output, may be empty
     * @param batches  how often the recipe runs
     * @param state    waiting_station, delivering, producing or done
     * @param reason   why the step waits, empty when it does not
     */
    public record StepRow(BlockPos producer, String machine, ItemStack output, int batches, String state, String reason) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StepRow> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, StepRow::producer,
                ByteBufCodecs.STRING_UTF8, StepRow::machine,
                ItemStack.OPTIONAL_STREAM_CODEC, StepRow::output,
                ByteBufCodecs.VAR_INT, StepRow::batches,
                ByteBufCodecs.STRING_UTF8, StepRow::state,
                ByteBufCodecs.STRING_UTF8, StepRow::reason,
                StepRow::new);
    }

    /**
     * One work order of a CPU.
     *
     * @param id        the order id within its CPU
     * @param item      a one item stack of what is made
     * @param amount    how many are made
     * @param state     waiting, running, blocked, done or cancelled
     * @param reason    why the order waits or is blocked, empty otherwise
     * @param requestId the terminal request, 0 for a manual order, negative for a stock rule
     * @param steps     the steps in plan order
     */
    public record OrderRow(int id, ItemStack item, int amount, String state, String reason, int requestId, List<StepRow> steps) {
        public static final StreamCodec<RegistryFriendlyByteBuf, OrderRow> STREAM_CODEC = StreamCodec.of(OrderRow::write, OrderRow::read);

        public boolean open() {
            return state.equals("waiting") || state.equals("running") || state.equals("blocked");
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

    /**
     * One pattern of the line.
     *
     * @param storage the Pattern Storage that keeps it
     * @param slot    its slot there
     * @param result  what one craft makes
     * @param inputs  what one craft consumes, with counts
     */
    public record PatternRow(BlockPos storage, int slot, ItemStack result, List<ItemStack> inputs) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PatternRow> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, PatternRow::storage,
                ByteBufCodecs.VAR_INT, PatternRow::slot,
                ItemStack.OPTIONAL_STREAM_CODEC, PatternRow::result,
                STACKS, PatternRow::inputs,
                PatternRow::new);
    }

    /**
     * The patterns of the line and how many fit.
     *
     * @param rows  every pattern, Pattern Storages by lowest position, slots in order
     * @param used  slots holding a pattern
     * @param total slots of every Pattern Storage of the line
     */
    public record PatternsView(List<PatternRow> rows, int used, int total) {
        public static final PatternsView EMPTY = new PatternsView(List.of(), 0, 0);

        public static final StreamCodec<RegistryFriendlyByteBuf, PatternsView> STREAM_CODEC = StreamCodec.composite(
                PatternRow.STREAM_CODEC.apply(ByteBufCodecs.list()), PatternsView::rows,
                ByteBufCodecs.VAR_INT, PatternsView::used,
                ByteBufCodecs.VAR_INT, PatternsView::total,
                PatternsView::new);
    }

    /**
     * One Crafting CPU of the line with its orders.
     *
     * @param cpu     the CPU's master position
     * @param slots   its order slots
     * @param inUse   slots taken by open orders
     * @param waiting open orders beyond the slots
     * @param orders  its orders, oldest first
     */
    public record CpuRow(BlockPos cpu, int slots, int inUse, int waiting, List<OrderRow> orders) {
        public static final StreamCodec<RegistryFriendlyByteBuf, CpuRow> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CpuRow::cpu,
                ByteBufCodecs.VAR_INT, CpuRow::slots,
                ByteBufCodecs.VAR_INT, CpuRow::inUse,
                ByteBufCodecs.VAR_INT, CpuRow::waiting,
                OrderRow.STREAM_CODEC.apply(ByteBufCodecs.list()), CpuRow::orders,
                CpuRow::new);
    }
}
