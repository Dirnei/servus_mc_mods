package io.github.dirnei.mc.servus.shuttles.production;

import io.github.dirnei.mc.servus.shuttles.pattern.AnyGroup;
import io.github.dirnei.mc.servus.shuttles.storage.StorageFill;
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
     * @param any     what the cells of an Any pattern accept, per ingredient; empty for an Exact pattern
     * @param follows for an Any material pattern the ingredient its material follows, else empty
     */
    public record PatternRow(BlockPos storage, int slot, ItemStack result, List<ItemStack> inputs, List<AnyGroup> any,
                             List<AnyGroup> follows) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PatternRow> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, PatternRow::storage,
                ByteBufCodecs.VAR_INT, PatternRow::slot,
                ItemStack.OPTIONAL_STREAM_CODEC, PatternRow::result,
                STACKS, PatternRow::inputs,
                AnyGroup.STREAM_CODEC.apply(ByteBufCodecs.list()), PatternRow::any,
                AnyGroup.STREAM_CODEC.apply(ByteBufCodecs.list(1)), PatternRow::follows,
                PatternRow::new);

        /** Whether the pattern is shown as Any. */
        public boolean isAny() {
            return !any.isEmpty() || !follows.isEmpty();
        }
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
     * An item with a count; the count travels as a var int next to a one item
     * stack, so counts above the max stack size travel safely.
     */
    public record ItemCount(ItemStack item, int count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemCount> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ItemCount::item,
                ByteBufCodecs.VAR_INT, ItemCount::count,
                ItemCount::new);
        static final StreamCodec<RegistryFriendlyByteBuf, List<ItemCount>> LIST = STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    /**
     * Items waiting in a CPU's buffer for one job.
     *
     * @param item    a one item stack
     * @param count   how many
     * @param order   the job's order id on that CPU
     * @param leaving whether its job is done or cancelled and the items wait to leave for storage
     */
    public record BufferItem(ItemStack item, int count, int order, boolean leaving) {
        public static final StreamCodec<RegistryFriendlyByteBuf, BufferItem> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, BufferItem::item,
                ByteBufCodecs.VAR_INT, BufferItem::count,
                ByteBufCodecs.VAR_INT, BufferItem::order,
                ByteBufCodecs.BOOL, BufferItem::leaving,
                BufferItem::new);
    }

    /**
     * One formed Crafting CPU of the line, for the header tooltip and the CPUs tab.
     *
     * @param cpu          the CPU's master position
     * @param slots        its order slots
     * @param inUse        slots taken by open orders
     * @param waiting      open orders beyond the slots
     * @param sizeX        the box's length along x
     * @param sizeY        the box's height
     * @param sizeZ        the box's length along z
     * @param cpuBlocks    its CPU blocks
     * @param bufferBlocks its Buffer blocks
     * @param room         buffer stacks it has
     * @param used         buffer stacks in use
     * @param buffered     what the buffer holds, oldest first
     */
    public record CpuRow(BlockPos cpu, int slots, int inUse, int waiting, int sizeX, int sizeY, int sizeZ, int cpuBlocks,
                         int bufferBlocks, int room, int used, List<BufferItem> buffered) {
        public static final StreamCodec<RegistryFriendlyByteBuf, CpuRow> STREAM_CODEC = StreamCodec.of(CpuRow::write, CpuRow::read);
        private static final StreamCodec<RegistryFriendlyByteBuf, List<BufferItem>> ITEMS = BufferItem.STREAM_CODEC.apply(ByteBufCodecs.list());

        private static void write(RegistryFriendlyByteBuf buf, CpuRow row) {
            BlockPos.STREAM_CODEC.encode(buf, row.cpu);
            buf.writeVarInt(row.slots);
            buf.writeVarInt(row.inUse);
            buf.writeVarInt(row.waiting);
            buf.writeVarInt(row.sizeX);
            buf.writeVarInt(row.sizeY);
            buf.writeVarInt(row.sizeZ);
            buf.writeVarInt(row.cpuBlocks);
            buf.writeVarInt(row.bufferBlocks);
            buf.writeVarInt(row.room);
            buf.writeVarInt(row.used);
            ITEMS.encode(buf, row.buffered);
        }

        private static CpuRow read(RegistryFriendlyByteBuf buf) {
            return new CpuRow(BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    ITEMS.decode(buf));
        }
    }

    /**
     * Intermediates of a job that wait in its CPU's buffer for one later step.
     *
     * @param machine the name of the later step's machine
     * @param items   the buffered items with counts
     */
    public record BufferLine(String machine, List<ItemCount> items) {
        public static final StreamCodec<RegistryFriendlyByteBuf, BufferLine> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BufferLine::machine,
                ItemCount.LIST, BufferLine::items,
                BufferLine::new);
        static final StreamCodec<RegistryFriendlyByteBuf, List<BufferLine>> LIST = STREAM_CODEC.apply(ByteBufCodecs.list());
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
     * @param buffer    what waits in the CPU's buffer for its later steps, one line per step
     * @param overflow  intermediates that went to storage because the buffer was full
     */
    public record JobRow(BlockPos cpu, int id, ItemStack item, int amount, int crafted, String state, long doneAt, String status,
                         List<String> args, int requestId, List<BufferLine> buffer, List<ItemCount> overflow) {

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
            BufferLine.LIST.encode(buf, row.buffer);
            ItemCount.LIST.encode(buf, row.overflow);
        }

        private static JobRow read(RegistryFriendlyByteBuf buf) {
            return new JobRow(BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(), ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt(),
                    buf.readVarInt(), buf.readUtf(), buf.readLong(), buf.readUtf(), STRINGS.decode(buf), buf.readInt(),
                    BufferLine.LIST.decode(buf), ItemCount.LIST.decode(buf));
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

    /**
     * One zone of a warehouse with its rack slots in use.
     *
     * @param name  the zone's name, empty for the racks without a zone
     * @param used  slots in use
     * @param total all slots of the zone's racks
     */
    public record ZoneRow(String name, long used, long total) {
        public static final StreamCodec<ByteBuf, ZoneRow> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ZoneRow::name,
                ByteBufCodecs.VAR_LONG, ZoneRow::used,
                ByteBufCodecs.VAR_LONG, ZoneRow::total,
                ZoneRow::new);

        public boolean full() {
            return total > 0 && used >= total;
        }

        public boolean unzoned() {
            return name.isEmpty();
        }
    }

    /**
     * How full one warehouse is.
     *
     * @param name          the site name
     * @param bays          rack bays that count
     * @param used          rack slots in use
     * @param total         all rack slots
     * @param zones         one row per zone and one for unzoned racks, full first, then the fullest
     * @param gantries      Storage gantries that count
     * @param fluid         mB in their tanks
     * @param fluidCapacity mB their tanks hold at most
     */
    public record StorageRow(String name, int bays, long used, long total, List<ZoneRow> zones, int gantries, long fluid,
                             long fluidCapacity) {

        public static final StorageRow EMPTY = new StorageRow("", 0, 0, 0, List.of(), 0, 0, 0);

        public static final StreamCodec<ByteBuf, StorageRow> STREAM_CODEC = StreamCodec.of(
                (buf, row) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, row.name());
                    ByteBufCodecs.VAR_INT.encode(buf, row.bays());
                    ByteBufCodecs.VAR_LONG.encode(buf, row.used());
                    ByteBufCodecs.VAR_LONG.encode(buf, row.total());
                    ZoneRow.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, row.zones());
                    ByteBufCodecs.VAR_INT.encode(buf, row.gantries());
                    ByteBufCodecs.VAR_LONG.encode(buf, row.fluid());
                    ByteBufCodecs.VAR_LONG.encode(buf, row.fluidCapacity());
                },
                buf -> new StorageRow(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),
                        ZoneRow.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf)));

        /** The row of a warehouse named {@code name} with this fill and this many Storage gantries. */
        public static StorageRow of(String name, StorageFill.Report report, int gantries) {
            List<ZoneRow> zones = report.zones().stream().map(zone -> new ZoneRow(zone.name(), zone.used(), zone.total())).toList();
            return new StorageRow(name, report.bays(), report.items().used(), report.items().total(), zones, gantries, report.fluid(),
                    report.fluidCapacity());
        }

        public boolean full() {
            return total > 0 && used >= total;
        }

        public boolean fluidFull() {
            return fluidCapacity > 0 && fluid >= fluidCapacity;
        }

        /** The zones that are full, by name. */
        public List<String> fullZones() {
            return zones.stream().filter(zone -> zone.full() && !zone.unzoned()).map(ZoneRow::name).toList();
        }
    }
}
