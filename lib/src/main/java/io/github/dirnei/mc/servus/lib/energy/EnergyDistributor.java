package io.github.dirnei.mc.servus.lib.energy;

import java.util.List;

/**
 * Moves energy from sources to sinks under a per endpoint rate. Sinks share
 * short supply fairly (equal rounds, with leftovers from satisfied sinks going
 * to the others); sources are drained in order. Pure Java.
 */
public final class EnergyDistributor {

    private EnergyDistributor() {
    }

    /**
     * @param sources energy providers, drained in list order
     * @param sinks   energy receivers, served fairly
     * @param rate    most energy per source and per sink in this pass
     * @return energy moved
     */
    public static int distribute(List<? extends EnergySource> sources, List<? extends EnergySink> sinks, int rate) {
        if (sources.isEmpty() || sinks.isEmpty() || rate <= 0) {
            return 0;
        }
        int[] demand = new int[sinks.size()];
        int totalDemand = 0;
        for (int i = 0; i < sinks.size(); i++) {
            demand[i] = Math.max(0, sinks.get(i).receive(rate, true));
            totalDemand += demand[i];
        }
        int totalSupply = 0;
        for (EnergySource source : sources) {
            totalSupply += Math.max(0, source.extract(rate, true));
        }
        int budget = Math.min(totalDemand, totalSupply);
        if (budget <= 0) {
            return 0;
        }

        int[] allocation = allocate(demand, budget);

        int moved = 0;
        int sourceIndex = 0;
        for (int i = 0; i < sinks.size(); i++) {
            int wanted = allocation[i];
            int pulled = 0;
            while (pulled < wanted && sourceIndex < sources.size()) {
                int got = sources.get(sourceIndex).extract(wanted - pulled, false);
                pulled += got;
                if (got <= 0 || pulled < wanted) {
                    sourceIndex++;
                }
            }
            if (pulled > 0) {
                moved += sinks.get(i).receive(pulled, false);
            }
        }
        return moved;
    }

    /** Water filling: equal shares per round, satisfied sinks drop out. */
    static int[] allocate(int[] demand, int budget) {
        int[] allocation = new int[demand.length];
        int active = 0;
        for (int d : demand) {
            if (d > 0) {
                active++;
            }
        }
        while (budget > 0 && active > 0) {
            int share = Math.max(1, budget / active);
            boolean progressed = false;
            for (int i = 0; i < demand.length && budget > 0; i++) {
                int remaining = demand[i] - allocation[i];
                if (remaining <= 0) {
                    continue;
                }
                int give = Math.min(Math.min(share, remaining), budget);
                allocation[i] += give;
                budget -= give;
                progressed |= give > 0;
                if (allocation[i] == demand[i]) {
                    active--;
                }
            }
            if (!progressed) {
                break;
            }
        }
        return allocation;
    }
}
