package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.shuttles.field.FieldCargo;
import io.github.dirnei.mc.servus.shuttles.gantry.TankRef;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shuttle.Load;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Writes the fleet's jobs and shuttles into compound tags and reads them
 * back. An entry that cannot be read is skipped as a whole, so no cargo is
 * restored without the job it belongs to.
 */
public final class FleetSaves {

    private FleetSaves() {
    }

    /** One job with its id and fields. */
    public static CompoundTag saveJob(ShuttleJob job, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", job.id());
        switch (job) {
            case PickJob pick -> {
                tag.putString("Type", "pick");
                tag.putInt("Request", pick.requestId());
                tag.put("Key", saveKey(pick.key(), registries));
                tag.putInt("Amount", pick.amount());
                tag.putLong("Source", pick.source().asLong());
                tag.putLong("Terminal", pick.terminal().asLong());
            }
            case PutAwayJob putAway -> {
                tag.putString("Type", "put_away");
                tag.putLong("Station", putAway.station().asLong());
                tag.putInt("Slot", putAway.slot());
                tag.put("Key", saveKey(putAway.key(), registries));
                tag.putInt("Amount", putAway.amount());
                tag.putLong("Target", putAway.target().asLong());
            }
            case FieldJob field -> headJob(tag, "field", field.head());
            case FertilizeJob fertilize -> headJob(tag, "fertilize", fertilize.head());
            case SupplyJob supply -> headJob(tag, "supply", supply.head());
            case FluidJob fluid -> {
                tag.putString("Type", "fluid");
                tag.putLong("Source", fluid.source().asLong());
                tag.putInt("SourceTank", fluid.sourceTank());
                tag.putLong("Target", fluid.target().asLong());
                tag.putInt("TargetTank", fluid.targetTank());
                tag.put("Fluid", saveFluid(fluid.key().stack(1), registries));
                tag.putInt("Amount", fluid.amount());
                tag.putString("Purpose", fluid.purpose().name());
            }
        }
        return tag;
    }

    private static void headJob(CompoundTag tag, String type, BlockPos head) {
        tag.putString("Type", type);
        tag.putLong("Head", head.asLong());
    }

    /** The job in a tag written by {@link #saveJob}, empty when it cannot be read. */
    public static Optional<ShuttleJob> loadJob(CompoundTag tag, HolderLookup.Provider registries) {
        int id = tag.getIntOr("Id", 0);
        if (id <= 0) {
            return Optional.empty();
        }
        return switch (tag.getStringOr("Type", "")) {
            case "pick" -> loadKey(tag, registries).map(key -> new PickJob(id, tag.getIntOr("Request", 0), key, tag.getIntOr("Amount", 0),
                    pos(tag, "Source"), pos(tag, "Terminal")));
            case "put_away" -> loadKey(tag, registries).map(key -> new PutAwayJob(id, pos(tag, "Station"), tag.getIntOr("Slot", 0), key,
                    tag.getIntOr("Amount", 0), pos(tag, "Target")));
            case "field" -> Optional.of(new FieldJob(id, pos(tag, "Head")));
            case "fertilize" -> Optional.of(new FertilizeJob(id, pos(tag, "Head")));
            case "supply" -> Optional.of(new SupplyJob(id, pos(tag, "Head")));
            case "fluid" -> loadFluidJob(id, tag, registries);
            default -> Optional.empty();
        };
    }

    private static Optional<ShuttleJob> loadFluidJob(int id, CompoundTag tag, HolderLookup.Provider registries) {
        FluidStack fluid = loadFluid(tag.get("Fluid"), registries);
        if (fluid.isEmpty()) {
            return Optional.empty();
        }
        FluidJob.Purpose purpose;
        try {
            purpose = FluidJob.Purpose.valueOf(tag.getStringOr("Purpose", ""));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
        return Optional.of(new FluidJob(id, pos(tag, "Source"), tag.getIntOr("SourceTank", 0), pos(tag, "Target"),
                tag.getIntOr("TargetTank", 0), FluidKey.of(fluid), tag.getIntOr("Amount", 0), purpose));
    }

    /** Jobs in their order. */
    public static ListTag saveJobs(Iterable<ShuttleJob> jobs, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ShuttleJob job : jobs) {
            list.add(saveJob(job, registries));
        }
        return list;
    }

    /** The readable jobs of a list written by {@link #saveJobs}, in their order. */
    public static List<ShuttleJob> loadJobs(ListTag list, HolderLookup.Provider registries) {
        List<ShuttleJob> jobs = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            loadJob(list.getCompoundOrEmpty(i), registries).ifPresent(jobs::add);
        }
        return jobs;
    }

    /** A shuttle that is out: slot, leg, motion, loads and field run. */
    static CompoundTag saveShuttle(Shuttle shuttle, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Slot", shuttle.slot);
        tag.putString("Leg", shuttle.leg.name());
        ShuttleMotion motion = shuttle.motion;
        long[] path = new long[motion.path().size()];
        for (int i = 0; i < path.length; i++) {
            path[i] = Conversions.toBlockPos(motion.path().get(i)).asLong();
        }
        tag.putLongArray("Path", path);
        tag.putInt("Segment", motion.segment());
        tag.putDouble("Progress", motion.progress());
        ListTag loads = new ListTag();
        for (Load load : shuttle.loads) {
            loads.add(saveLoad(load, registries));
        }
        tag.put("Loads", loads);
        FieldRun run = shuttle.fieldRun();
        if (run != null) {
            tag.put("Run", saveRun(run, registries));
        }
        if (shuttle.strandReason != null) {
            tag.putString("Strand", shuttle.strandReason.name());
        }
        return tag;
    }

    /** The slot a tag written by {@link #saveShuttle} belongs to, -1 when it names none. */
    static int slotOf(CompoundTag tag) {
        return tag.getIntOr("Slot", -1);
    }

    /**
     * Puts a saved shuttle's leg, motion, loads and field run onto the
     * shuttle; false when the tag cannot be read, and then nothing changed.
     */
    static boolean restoreShuttle(Shuttle shuttle, CompoundTag tag, HolderLookup.Provider registries) {
        Leg leg;
        try {
            leg = Leg.valueOf(tag.getStringOr("Leg", ""));
        } catch (IllegalArgumentException unknown) {
            return false;
        }
        long[] longs = tag.getLongArray("Path").orElse(new long[0]);
        if (longs.length == 0) {
            return false;
        }
        List<NodePos> path = new ArrayList<>(longs.length);
        for (long packed : longs) {
            path.add(Conversions.toNodePos(BlockPos.of(packed)));
        }
        List<Load> loads = new ArrayList<>();
        ListTag savedLoads = tag.getListOrEmpty("Loads");
        for (int i = 0; i < savedLoads.size(); i++) {
            Optional<Load> load = loadLoad(savedLoads.getCompoundOrEmpty(i), registries);
            if (load.isEmpty()) {
                return false;
            }
            loads.add(load.get());
        }
        FieldRun run = null;
        if (tag.contains("Run")) {
            run = loadRun(tag.getCompoundOrEmpty("Run"), shuttle.profile.stacks(), registries);
            if (run == null) {
                return false;
            }
        }
        shuttle.leg = leg;
        shuttle.strandReason = strandReason(tag.getStringOr("Strand", ""));
        shuttle.motion = ShuttleMotion.restored(path, tag.getIntOr("Segment", 0), tag.getDoubleOr("Progress", 0));
        shuttle.loads.clear();
        shuttle.loads.addAll(loads);
        shuttle.run = run instanceof HarvestRun harvest ? harvest : null;
        shuttle.spread = run instanceof SpreadRun spread ? spread : null;
        shuttle.supply = run instanceof SupplyRun supply ? supply : null;
        return true;
    }

    private static StrandReason strandReason(String name) {
        for (StrandReason reason : StrandReason.values()) {
            if (reason.name().equals(name)) {
                return reason;
            }
        }
        return null;
    }

    private static CompoundTag saveLoad(Load load, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Job", saveJob(load.job, registries));
        if (!load.cargo.isEmpty()) {
            tag.put("Cargo", ValueIo.saveStack(registries, load.cargo));
        }
        if (!load.fluid.isEmpty()) {
            tag.put("Fluid", saveFluid(load.fluid, registries));
        }
        if (load.fluidTarget != null) {
            tag.put("FluidTarget", saveTank(load.fluidTarget));
        }
        ListTag tried = new ListTag();
        for (TankRef tank : load.tried) {
            tried.add(saveTank(tank));
        }
        tag.put("Tried", tried);
        long[] stops = new long[load.stops.size()];
        for (int i = 0; i < stops.length; i++) {
            stops[i] = load.stops.get(i).asLong();
        }
        tag.putLongArray("Stops", stops);
        if (load.storing) {
            tag.putBoolean("Storing", true);
        }
        return tag;
    }

    private static Optional<Load> loadLoad(CompoundTag tag, HolderLookup.Provider registries) {
        Optional<ShuttleJob> job = loadJob(tag.getCompoundOrEmpty("Job"), registries);
        if (job.isEmpty()) {
            return Optional.empty();
        }
        Load load = new Load(job.get());
        if (tag.contains("Cargo")) {
            load.cargo = ValueIo.parseStack(registries, tag.getCompoundOrEmpty("Cargo"));
        }
        if (tag.contains("Fluid")) {
            load.fluid = loadFluid(tag.get("Fluid"), registries);
        }
        load.fluidTarget = tag.contains("FluidTarget") ? loadTank(tag.getCompoundOrEmpty("FluidTarget")) : null;
        ListTag tried = tag.getListOrEmpty("Tried");
        for (int i = 0; i < tried.size(); i++) {
            load.tried.add(loadTank(tried.getCompoundOrEmpty(i)));
        }
        if (tag.contains("Stops")) {
            load.stops.clear();
            for (long stop : tag.getLongArray("Stops").orElse(new long[0])) {
                load.stops.add(BlockPos.of(stop));
            }
        }
        load.storing = tag.getBooleanOr("Storing", false);
        return Optional.of(load);
    }

    private static CompoundTag saveRun(FieldRun run, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Head", run.head.asLong());
        ListTag cargo = new ListTag();
        for (FieldCargo.Entry<ItemKey> entry : run.cargo.entries()) {
            CompoundTag saved = new CompoundTag();
            saved.put("Key", saveKey(entry.key(), registries));
            saved.putInt("Count", entry.count());
            saved.putInt("Max", entry.max());
            cargo.add(saved);
        }
        tag.put("Cargo", cargo);
        ListTag store = new ListTag();
        for (ItemStack stack : run.store) {
            if (!stack.isEmpty()) {
                store.add(ValueIo.saveStack(registries, stack));
            }
        }
        tag.put("Store", store);
        switch (run) {
            case HarvestRun harvest -> {
                tag.putString("Kind", "harvest");
                CompoundTag tally = new CompoundTag();
                harvest.tally.forEach((id, count) -> tally.putLong(id.toString(), count));
                tag.put("Tally", tally);
            }
            case SpreadRun spread -> {
                tag.putString("Kind", "spread");
                tag.put("Fertilizer", saveKey(spread.fertilizer, registries));
                tag.putInt("Doses", spread.doses);
                tag.putInt("ToFetch", spread.toFetch);
                tag.putBoolean("Fetched", spread.fetched);
            }
            case SupplyRun supply -> {
                tag.putString("Kind", "supply");
                tag.putBoolean("Fetched", supply.fetched);
                tag.putBoolean("Unloaded", supply.unloaded);
            }
            default -> throw new IllegalStateException("Unknown field run " + run);
        }
        return tag;
    }

    /** The run in a tag written by {@link #saveRun}, null when it cannot be read. */
    private static FieldRun loadRun(CompoundTag tag, int slots, HolderLookup.Provider registries) {
        BlockPos head = pos(tag, "Head");
        FieldRun run = switch (tag.getStringOr("Kind", "")) {
            case "harvest" -> {
                HarvestRun harvest = new HarvestRun(head, slots);
                CompoundTag tally = tag.getCompoundOrEmpty("Tally");
                for (String key : tally.keySet()) {
                    Identifier id = Identifier.tryParse(key);
                    if (id != null) {
                        harvest.tally.put(id, tally.getLongOr(key, 0L));
                    }
                }
                yield harvest;
            }
            case "spread" -> {
                Optional<ItemKey> fertilizer = keyOf(tag.getCompoundOrEmpty("Fertilizer"), registries);
                if (fertilizer.isEmpty()) {
                    yield null;
                }
                SpreadRun spread = new SpreadRun(head, slots, fertilizer.get(), tag.getIntOr("Doses", 1));
                spread.toFetch = tag.getIntOr("ToFetch", -1);
                spread.fetched = tag.getBooleanOr("Fetched", false);
                yield spread;
            }
            case "supply" -> {
                SupplyRun supply = new SupplyRun(head, slots);
                supply.fetched = tag.getBooleanOr("Fetched", true);
                supply.unloaded = tag.getBooleanOr("Unloaded", false);
                yield supply;
            }
            default -> null;
        };
        if (run == null) {
            return null;
        }
        ListTag cargo = tag.getListOrEmpty("Cargo");
        for (int i = 0; i < cargo.size(); i++) {
            CompoundTag saved = cargo.getCompoundOrEmpty(i);
            Optional<ItemKey> key = loadKey(saved, registries);
            int count = saved.getIntOr("Count", 0);
            if (key.isEmpty() || count <= 0) {
                continue;
            }
            int max = Math.max(1, saved.getIntOr("Max", key.get().stack(1).getMaxStackSize()));
            for (FieldCargo.Entry<ItemKey> left : run.cargo.add(List.of(new FieldCargo.Entry<>(key.get(), count, max)))) {
                run.store.add(left.key().stack(left.count()));
            }
        }
        ListTag store = tag.getListOrEmpty("Store");
        for (int i = 0; i < store.size(); i++) {
            ItemStack stack = ValueIo.parseStack(registries, store.getCompoundOrEmpty(i));
            if (!stack.isEmpty()) {
                run.store.add(stack);
            }
        }
        return run;
    }

    private static CompoundTag saveKey(ItemKey key, HolderLookup.Provider registries) {
        return ValueIo.saveStack(registries, key.stack(1));
    }

    private static Optional<ItemKey> loadKey(CompoundTag tag, HolderLookup.Provider registries) {
        return keyOf(tag.getCompoundOrEmpty("Key"), registries);
    }

    /** The item key of a stack tag written by {@link #saveKey}. */
    private static Optional<ItemKey> keyOf(CompoundTag stackTag, HolderLookup.Provider registries) {
        ItemStack stack = ValueIo.parseStack(registries, stackTag);
        return stack.isEmpty() ? Optional.empty() : Optional.of(ItemKey.of(stack));
    }

    private static Tag saveFluid(FluidStack fluid, HolderLookup.Provider registries) {
        return FluidStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), fluid).getOrThrow();
    }

    private static FluidStack loadFluid(Tag tag, HolderLookup.Provider registries) {
        if (tag == null) {
            return FluidStack.EMPTY;
        }
        return FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(FluidStack.EMPTY);
    }

    private static CompoundTag saveTank(TankRef tank) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Gantry", tank.gantry().asLong());
        tag.putInt("Tank", tank.tank());
        return tag;
    }

    private static TankRef loadTank(CompoundTag tag) {
        return new TankRef(pos(tag, "Gantry"), tag.getIntOr("Tank", 0));
    }

    private static BlockPos pos(CompoundTag tag, String key) {
        return BlockPos.of(tag.getLongOr(key, 0L));
    }
}
