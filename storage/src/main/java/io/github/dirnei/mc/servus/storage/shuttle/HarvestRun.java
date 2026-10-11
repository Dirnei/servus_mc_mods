package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.field.FieldCargo;
import io.github.dirnei.mc.servus.storage.field.FieldPos;
import io.github.dirnei.mc.servus.storage.field.PlotReach;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * The state of one harvester's field run: its cargo, which plots it works
 * from which rail, where it is in the run and what it is putting away. The
 * fleet sequences the legs; the decisions are pure ({@code HarvestStep},
 * {@code PlotWork}, {@code SeedPlan}).
 */
final class HarvestRun {

    /** Ticks between two tries to put away a cargo nothing had room for. */
    static final int STORE_RETRY_TICKS = 20;

    final BlockPos head;
    final FieldCargo<ItemKey> cargo;
    /** Plots worked from each rail of the field, nearest first; rebuilt when the field's rails change. */
    private Map<BlockPos, List<BlockPos>> reach = Map.of();
    private Set<BlockPos> reachRails;
    private int reachRadius = -1;
    /** Rails worked in the current work segment; the segment does not go back to them. */
    final Set<BlockPos> visited = new HashSet<>();
    /** Plots of the rail the harvester works now. */
    final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
    /** Whether the rail the harvester works now gets tilled and planted, fixed when it got there. */
    boolean stopPlants;
    int countdown;
    /** The last harvest did not fit. */
    boolean cargoFull;
    /** The cargo filled up with seeds kept for planting: the next put-away keeps none. */
    boolean keepNothing;
    /** Something was done in the current work segment. */
    boolean worked;
    /** A work segment did nothing: the run has no more work it can do. */
    boolean idle;
    /** Seeds were tried from the warehouse since the last work segment. */
    boolean fetchTried;
    /** The source and amount of the seeds the harvester drives to. */
    BlockPos seedSource;
    int seedAmount;
    /** Stacks on their way into storage, and the block the harvester drives to with them. */
    final List<ItemStack> store = new ArrayList<>();
    BlockPos storeTarget;
    int waitTimer;

    HarvestRun(BlockPos head, int slots) {
        this.head = head;
        this.cargo = new FieldCargo<>(slots);
    }

    /** The plots worked from each rail, built from the field's current rails, plots and radius. */
    Map<BlockPos, List<BlockPos>> reach(Set<BlockPos> rails, Set<BlockPos> plots, int radius) {
        if (rails != reachRails || radius != reachRadius) {
            List<FieldPos> railPositions = new ArrayList<>(rails.size());
            for (BlockPos rail : rails) {
                railPositions.add(new FieldPos(rail.getX(), rail.getY(), rail.getZ()));
            }
            List<FieldPos> plotPositions = new ArrayList<>(plots.size());
            for (BlockPos plot : plots) {
                plotPositions.add(new FieldPos(plot.getX(), plot.getY(), plot.getZ()));
            }
            Map<BlockPos, List<BlockPos>> built = new HashMap<>();
            for (Map.Entry<FieldPos, List<FieldPos>> entry : PlotReach.assign(railPositions, plotPositions, radius).entrySet()) {
                List<BlockPos> worked = new ArrayList<>(entry.getValue().size());
                for (FieldPos plot : entry.getValue()) {
                    worked.add(new BlockPos(plot.x(), plot.y(), plot.z()));
                }
                FieldPos rail = entry.getKey();
                built.put(new BlockPos(rail.x(), rail.y(), rail.z()), worked);
            }
            reach = built;
            reachRails = rails;
            reachRadius = radius;
        }
        return reach;
    }

    /** Everything the harvester holds, cargo and stacks being put away, as item stacks. */
    List<ItemStack> everything() {
        List<ItemStack> all = new ArrayList<>();
        for (FieldCargo.Entry<ItemKey> entry : cargo.entries()) {
            all.add(entry.key().stack(entry.count()));
        }
        for (ItemStack stack : store) {
            if (!stack.isEmpty()) {
                all.add(stack.copy());
            }
        }
        return all;
    }

    /** The stack drawn on the cart. */
    ItemStack shown() {
        for (ItemStack stack : store) {
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        List<FieldCargo.Entry<ItemKey>> entries = cargo.entries();
        return entries.isEmpty() ? ItemStack.EMPTY : entries.get(0).key().stack(entries.get(0).count());
    }
}
