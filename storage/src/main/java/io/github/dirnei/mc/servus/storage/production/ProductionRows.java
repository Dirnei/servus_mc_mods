package io.github.dirnei.mc.servus.storage.production;

import io.netty.buffer.ByteBuf;
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

    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    /**
     * One formed Crafting CPU of the line, for the header tooltip.
     *
     * @param cpu     the CPU's master position
     * @param slots   its order slots
     * @param inUse   slots taken by open orders
     * @param waiting open orders beyond the slots
     */
    public record CpuRow(BlockPos cpu, int slots, int inUse, int waiting) {
        public static final StreamCodec<RegistryFriendlyByteBuf, CpuRow> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CpuRow::cpu,
                ByteBufCodecs.VAR_INT, CpuRow::slots,
                ByteBufCodecs.VAR_INT, CpuRow::inUse,
                ByteBufCodecs.VAR_INT, CpuRow::waiting,
                CpuRow::new);
    }

    /**
     * One crafting job: a work order of a CPU of the line.
     *
     * @param cpu       the CPU that holds it
     * @param id        the order id within that CPU
     * @param item      a one item stack of what is made
     * @param amount    how many are made
     * @param crafted   how many are made so far
     * @param state     waiting, running, blocked, done or cancelled
     * @param doneAt    the game time it ended, -1 while open
     * @param status    lang key suffix of what it is doing, see {@link JobStatus}
     * @param args      the status arguments
     * @param requestId the terminal request, 0 for a manual order, negative for a stock rule
     */
    public record JobRow(BlockPos cpu, int id, ItemStack item, int amount, int crafted, String state, long doneAt, String status,
                         List<String> args, int requestId) {
        public static final StreamCodec<RegistryFriendlyByteBuf, JobRow> STREAM_CODEC = StreamCodec.of(JobRow::write, JobRow::read);

        public boolean open() {
            return state.equals("waiting") || state.equals("running") || state.equals("blocked");
        }

        private static void write(RegistryFriendlyByteBuf buf, JobRow row) {
            BlockPos.STREAM_CODEC.encode(buf, row.cpu);
            buf.writeVarInt(row.id);
            ItemStack.STREAM_CODEC.encode(buf, row.item);
            buf.writeVarInt(row.amount);
            buf.writeVarInt(row.crafted);
            buf.writeUtf(row.state);
            buf.writeLong(row.doneAt);
            buf.writeUtf(row.status);
            STRINGS.encode(buf, row.args);
            buf.writeInt(row.requestId);
        }

        private static JobRow read(RegistryFriendlyByteBuf buf) {
            return new JobRow(BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(), ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt(),
                    buf.readVarInt(), buf.readUtf(), buf.readLong(), buf.readUtf(), STRINGS.decode(buf), buf.readInt());
        }
    }

    /**
     * One producer or upkeep member of the line, as a status line.
     *
     * @param pos         where it stands
     * @param icon        the block to show, the faced machine's for a Machine Adapter
     * @param name        its block name, or the faced machine's name with "(adapter)" for a Machine Adapter
     * @param state       crafting, idle, out_of_fuel, blocked, no_machine, no_recipes or upkeep_only
     * @param item        what it is crafting, empty otherwise
     * @param left        crafts left of a Crafting Station's job, 0 otherwise
     * @param reason      why a step on it waits, for blocked
     * @param upkeep      its upkeep item, empty when it keeps none
     * @param upkeepCount upkeep items it holds
     * @param upkeepLevel the level it keeps
     * @param makes       how many different items it can make, -1 for any pattern
     */
    public record MachineRow(BlockPos pos, ItemStack icon, String name, String state, ItemStack item, int left, String reason,
                             ItemStack upkeep, int upkeepCount, int upkeepLevel, int makes) {
        public static final StreamCodec<RegistryFriendlyByteBuf, MachineRow> STREAM_CODEC = StreamCodec.of(MachineRow::write, MachineRow::read);

        public boolean problem() {
            return MachineOrder.problem(state);
        }

        private static void write(RegistryFriendlyByteBuf buf, MachineRow row) {
            BlockPos.STREAM_CODEC.encode(buf, row.pos);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, row.icon);
            buf.writeUtf(row.name);
            buf.writeUtf(row.state);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, row.item);
            buf.writeVarInt(row.left);
            buf.writeUtf(row.reason);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, row.upkeep);
            buf.writeVarInt(row.upkeepCount);
            buf.writeVarInt(row.upkeepLevel);
            buf.writeInt(row.makes);
        }

        private static MachineRow read(RegistryFriendlyByteBuf buf) {
            return new MachineRow(BlockPos.STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readUtf(),
                    buf.readUtf(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readUtf(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readInt());
        }
    }

    /**
     * One Packing Station of the line and how much of its rate it used during
     * the last second in each direction.
     *
     * @param pos       where it stands
     * @param toStorage items per second that came in over the line
     * @param toNetwork items per second that went out over the line
     * @param rate      items per second it converts in each direction, upgrades included
     */
    public record PackingRow(BlockPos pos, int toStorage, int toNetwork, int rate) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PackingRow> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, PackingRow::pos,
                ByteBufCodecs.VAR_INT, PackingRow::toStorage,
                ByteBufCodecs.VAR_INT, PackingRow::toNetwork,
                ByteBufCodecs.VAR_INT, PackingRow::rate,
                PackingRow::new);

        /** Whether a direction uses its full rate; one item of slack for fractions. */
        public static boolean busy(int used, int rate) {
            return rate > 0 && used >= rate - 1;
        }

        public boolean busyToStorage() {
            return busy(toStorage, rate);
        }

        public boolean busyToNetwork() {
            return busy(toNetwork, rate);
        }
    }

    /**
     * The busiest data duct of the line.
     *
     * @param pos       where it is
     * @param name      the name of its data cable tier
     * @param load      items per second that passed it during the last second
     * @param bandwidth items per second it lets through
     */
    public record DuctRow(BlockPos pos, String name, int load, int bandwidth) {
        public static final StreamCodec<RegistryFriendlyByteBuf, DuctRow> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, DuctRow::pos,
                ByteBufCodecs.STRING_UTF8, DuctRow::name,
                ByteBufCodecs.VAR_INT, DuctRow::load,
                ByteBufCodecs.VAR_INT, DuctRow::bandwidth,
                DuctRow::new);
    }
}
