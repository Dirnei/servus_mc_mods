package io.github.dirnei.mc.servus.manufacturing.research;

import java.util.List;
import java.util.Map;

/**
 * A tech as the research rules see it. Pure Java: ids are strings.
 *
 * @param id      tech id
 * @param parents ids of techs that must be completed first
 * @param cost    pack item id to count
 */
public record TechNode(String id, List<String> parents, Map<String, Integer> cost) {

    public int totalCost() {
        return cost.values().stream().mapToInt(Integer::intValue).sum();
    }
}
