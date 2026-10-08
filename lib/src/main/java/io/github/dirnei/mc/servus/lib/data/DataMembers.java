package io.github.dirnei.mc.servus.lib.data;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.graph.Side;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which cable sides of a data network touch which member, and with which
 * roles. A member touched by several cable sides is one member whose roles
 * are the union of its touches; it goes away with its last touch. Pure, no
 * Minecraft imports.
 */
public final class DataMembers {

    /** One cable side touching a member. */
    private record Touch(NodePos cable, Side side) {
    }

    private final Map<NodePos, Map<Touch, Set<DataRole>>> byMember = new LinkedHashMap<>();
    private final Map<Touch, NodePos> byTouch = new HashMap<>();

    /**
     * Sets what the given cable side touches. Empty roles mean the side
     * touches no member; any earlier touch of that side is replaced.
     */
    public void touch(NodePos cable, Side side, NodePos member, Set<DataRole> roles) {
        Touch touch = new Touch(cable, side);
        untouch(touch);
        if (roles.isEmpty()) {
            return;
        }
        byMember.computeIfAbsent(member, m -> new LinkedHashMap<>()).put(touch, EnumSet.copyOf(roles));
        byTouch.put(touch, member);
    }

    /** Forgets every touch of a cable, after it left the network. */
    public void removeCable(NodePos cable) {
        for (Side side : Side.ALL) {
            untouch(new Touch(cable, side));
        }
    }

    /** Takes over every touch of another network, after a merge. */
    public void absorb(DataMembers other) {
        other.byMember.forEach((member, touches) -> touches.forEach((touch, roles) -> {
            untouch(touch);
            byMember.computeIfAbsent(member, m -> new LinkedHashMap<>()).put(touch, EnumSet.copyOf(roles));
            byTouch.put(touch, member);
        }));
    }

    /** Every member with the union of its roles, in the order they joined. */
    public Map<NodePos, Set<DataRole>> all() {
        Map<NodePos, Set<DataRole>> result = new LinkedHashMap<>();
        byMember.forEach((member, touches) -> result.put(member, Collections.unmodifiableSet(union(touches))));
        return Collections.unmodifiableMap(result);
    }

    /** Positions of the members that have the role. */
    public Set<NodePos> withRole(DataRole role) {
        Set<NodePos> result = new LinkedHashSet<>();
        byMember.forEach((member, touches) -> {
            if (union(touches).contains(role)) {
                result.add(member);
            }
        });
        return Collections.unmodifiableSet(result);
    }

    public boolean contains(NodePos member) {
        return byMember.containsKey(member);
    }

    public int size() {
        return byMember.size();
    }

    private void untouch(Touch touch) {
        NodePos member = byTouch.remove(touch);
        if (member == null) {
            return;
        }
        Map<Touch, Set<DataRole>> touches = byMember.get(member);
        touches.remove(touch);
        if (touches.isEmpty()) {
            byMember.remove(member);
        }
    }

    private static Set<DataRole> union(Map<Touch, Set<DataRole>> touches) {
        Set<DataRole> roles = EnumSet.noneOf(DataRole.class);
        touches.values().forEach(roles::addAll);
        return roles;
    }
}
