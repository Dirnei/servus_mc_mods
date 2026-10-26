package io.github.dirnei.mc.servus.shuttles.network;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.order.OrderBook;
import io.github.dirnei.mc.servus.shuttles.order.RuleThresholds;
import io.github.dirnei.mc.servus.shuttles.order.StockRule;
import io.github.dirnei.mc.servus.shuttles.order.StockRules;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Orders for a warehouse's stock rules on the CPUs of its data line, keeps
 * which rules Inbound Stations refused items for, and tracks the state of
 * each rule and of its export.
 */
final class StockRuleKeeper {

    /** The state of a rule whose cap refused items since its previous check. */
    static final String REFUSING = "refusing";
    /** Export states. */
    static final String NO_STATION = "no_station";
    static final String EXPORTING = "exporting";
    static final String STATION_FULL = "station_full";

    private final WarehouseNetwork network;
    /** The state of each rule after its last order attempt, when not ok. */
    private final Map<ItemKey, String> states = new HashMap<>();
    /** The first station that refused each item since the last check. */
    private final Map<ItemKey, BlockPos> refusedSince = new LinkedHashMap<>();
    /** The label of the station that refused each item before the last check. */
    private final Map<ItemKey, Component> refusing = new HashMap<>();
    /** What the last outbound check did for each held export, when not ok. */
    private final Map<ItemKey, String> exportStates = new HashMap<>();
    private int timer;

    StockRuleKeeper(WarehouseNetwork network) {
        this.network = network;
    }

    /**
     * Replaces the rule of the item with the Make pair (null for none), the
     * cap (0 for none) and the export threshold (-1 for none), or refuses it.
     * A station holding the export keeps it while the rule still exports.
     *
     * @return {@link RuleThresholds#OK} or the reason
     */
    String set(ItemKey key, StockRule.Make make, int cap, int export) {
        String check = RuleThresholds.check(make == null ? -1 : make.min(), make == null ? -1 : make.max(), cap, export);
        if (!check.equals(RuleThresholds.OK)) {
            return check;
        }
        Optional<StockRules> rules = network.stockRules();
        if (rules.isEmpty()) {
            return "offline";
        }
        rules.get().set(new StockRule(key, make, cap, export));
        states.remove(key);
        exportStates.remove(key);
        timer = ShuttlesStatsManager.INSTANCE.stockRuleInterval();
        return RuleThresholds.OK;
    }

    /** Sets the Make pair of the item's rule, keeping its cap and export. */
    String setMake(ItemKey key, int min, int max) {
        return set(key, new StockRule.Make(min, max), current(key).map(StockRule::cap).orElse(0),
                current(key).map(StockRule::export).orElse(-1));
    }

    /** Sets the cap of the item's rule, keeping its Make pair and export. */
    String setCap(ItemKey key, int cap) {
        return set(key, current(key).map(StockRule::make).orElse(null), cap, current(key).map(StockRule::export).orElse(-1));
    }

    /** The state of a held export after the last outbound check: exporting, station full or ok. */
    String exportState(ItemKey key) {
        return exportStates.getOrDefault(key, "ok");
    }

    /** Records what an outbound check did for a held export: exporting, station full, or null for ok. */
    void exportChecked(ItemKey key, String state) {
        if (state == null) {
            exportStates.remove(key);
        } else {
            exportStates.put(key, state);
        }
    }

    /** Clears the cap of the item's rule and removes a rule left without a threshold; false when it had no cap. */
    boolean clearCap(ItemKey key) {
        Optional<StockRule> rule = current(key).filter(StockRule::hasCap);
        if (rule.isEmpty()) {
            return false;
        }
        StockRule left = rule.get().withCap(0);
        if (left.empty()) {
            remove(key);
        } else {
            network.stockRules().ifPresent(rules -> rules.set(left));
        }
        return true;
    }

    private Optional<StockRule> current(ItemKey key) {
        return network.stockRules().map(rules -> rules.get(key));
    }

    boolean remove(ItemKey key) {
        states.remove(key);
        exportStates.remove(key);
        refusing.remove(key);
        refusedSince.remove(key);
        return network.stockRules().map(rules -> rules.remove(key)).orElse(false);
    }

    /** An Inbound Station refused part of the item because of its cap; the first station per check is kept. */
    void refused(ItemKey key, BlockPos station) {
        refusedSince.putIfAbsent(key, station);
    }

    void tick(ServerLevel level) {
        if (++timer >= ShuttlesStatsManager.INSTANCE.stockRuleInterval()) {
            timer = 0;
            check(level);
        }
    }

    /**
     * Turns the refusals since the last check into the refusing state, then
     * orders for every rule with a Make pair whose stock plus open orders on
     * the line's CPUs fell below its minimum.
     */
    private void check(ServerLevel level) {
        refusing.clear();
        refusedSince.forEach((key, station) -> refusing.put(key, network.stationLabel(station)));
        refusedSince.clear();
        if (network.status() != WarehouseNetwork.Status.ONLINE) {
            return;
        }
        Optional<StockRules> rules = network.stockRules();
        if (rules.isEmpty()) {
            return;
        }
        Optional<DataNetwork> line = CpuOrders.network(level, network);
        for (StockRule rule : rules.get().all()) {
            if (!rule.hasMake()) {
                states.remove(rule.key());
                continue;
            }
            int open = line.map(data -> CpuOrders.openAmountFor(level, data, rule.key())).orElse(0);
            long have = network.index().count(rule.key()) + open;
            if (have >= rule.make().min()) {
                states.remove(rule.key());
                continue;
            }
            if (line.isEmpty() || CpuOrders.cpus(level, line.get()).isEmpty()) {
                states.put(rule.key(), "no_cpu");
                continue;
            }
            int wanted = (int) Math.min(Integer.MAX_VALUE, rule.make().max() - have);
            CpuOrders.Placed placed = CpuOrders.create(level, line.get(), rule.key(), wanted, OrderBook.RULE_ORIGIN);
            if (placed.made()) {
                states.remove(rule.key());
            } else if (placed.refusal().equals("no_producer")) {
                states.put(rule.key(), "no_producer");
            }
        }
    }

    /** The state of a rule: refusing, ordering, no CPU, no producer, or ok. */
    private String state(StockRule rule) {
        if (refusing.containsKey(rule.key())) {
            return REFUSING;
        }
        ServerLevel level = network.level();
        if (rule.hasMake() && level != null) {
            Optional<DataNetwork> line = CpuOrders.network(level, network);
            if (line.isPresent() && CpuOrders.openAmountFor(level, line.get(), rule.key()) > 0) {
                return "ordering";
            }
        }
        return states.getOrDefault(rule.key(), "ok");
    }

    List<RuleRow> rows() {
        List<RuleRow> rows = new ArrayList<>();
        network.stockRules().ifPresent(rules -> {
            for (StockRule rule : rules.all()) {
                String state = state(rule);
                Component detail = state.equals(REFUSING) ? refusing.get(rule.key()) : Component.empty();
                Optional<BlockPos> holder = rules.exportHolder(rule.key()).map(claim -> claim.station());
                Component label = holder.map(pos -> network.stationLabel(pos)).orElse(Component.empty());
                String exportState = holder.isEmpty() ? NO_STATION : exportStates.getOrDefault(rule.key(), "ok");
                rows.add(RuleRow.of(rule, state, detail, label, exportState));
            }
        });
        return rows;
    }
}
