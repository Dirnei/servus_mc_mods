package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.StockRule;
import io.github.dirnei.mc.servus.storage.order.StockRules;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;

/** Orders for a warehouse's stock rules on the CPUs of its data line and tracks the state of each rule. */
final class StockRuleKeeper {

    private final WarehouseNetwork network;
    /** The state of each rule after its last order attempt, when not ok. */
    private final Map<ItemKey, String> states = new HashMap<>();
    private int timer;

    StockRuleKeeper(WarehouseNetwork network) {
        this.network = network;
    }

    void set(ItemKey key, int min, int max) {
        network.stockRules().ifPresent(rules -> {
            rules.set(new StockRule(key, min, max));
            states.remove(key);
        });
        timer = StorageStatsManager.INSTANCE.stockRuleInterval();
    }

    boolean remove(ItemKey key) {
        states.remove(key);
        return network.stockRules().map(rules -> rules.remove(key)).orElse(false);
    }

    void tick(ServerLevel level) {
        if (++timer >= StorageStatsManager.INSTANCE.stockRuleInterval()) {
            timer = 0;
            check(level);
        }
    }

    /** Orders for every rule whose stock plus open orders on the line's CPUs fell below its minimum. */
    private void check(ServerLevel level) {
        if (network.status() != WarehouseNetwork.Status.ONLINE) {
            return;
        }
        Optional<StockRules> rules = network.stockRules();
        if (rules.isEmpty()) {
            return;
        }
        Optional<DataNetwork> line = CpuOrders.network(level, network);
        for (StockRule rule : rules.get().all()) {
            int open = line.map(data -> CpuOrders.openAmountFor(level, data, rule.key())).orElse(0);
            long have = network.index().count(rule.key()) + open;
            if (have >= rule.min()) {
                states.remove(rule.key());
                continue;
            }
            if (line.isEmpty() || CpuOrders.cpus(level, line.get()).isEmpty()) {
                states.put(rule.key(), "no_cpu");
                continue;
            }
            int wanted = (int) Math.min(Integer.MAX_VALUE, rule.max() - have);
            CpuOrders.Placed placed = CpuOrders.create(level, line.get(), rule.key(), wanted, OrderBook.RULE_ORIGIN);
            if (placed.made()) {
                states.remove(rule.key());
            } else if (placed.refusal().equals("no_producer")) {
                states.put(rule.key(), "no_producer");
            }
        }
    }

    /** The state of a rule: ordering, no CPU, no producer, or ok. */
    private String state(StockRule rule) {
        ServerLevel level = network.level();
        if (level != null) {
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
                rows.add(new RuleRow(rule.key().stack(1), rule.min(), rule.max(), state(rule)));
            }
        });
        return rows;
    }
}
