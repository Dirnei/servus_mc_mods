package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
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
 * are the union of its touches; it goes away with its last touch.
 */
public final class DataMembers {

    /** One cable side touching a member; a null side is a node that is a member itself. */
    private record Touch(NodePos cable, Side side) {
    }

    private final Map<NodePos, Map<Touch, Set<DataRole>>> byMember = new LinkedHashMap<>();
    private final Map<Touch, NodePos> byTouch = new HashMap<>();
    private int revision;

    /**
     * Sets what the given cable side touches. Empty roles mean the side
     * touches no member; any earlier touch of that side is replaced.
     */
    public void touch(NodePos cable, Side side, NodePos member, Set<DataRole> roles) {
        revision++;
        Touch touch = new Touch(cable, side);
        untouch(touch);
        if (!roles.isEmpty()) {
            add(touch, member, roles);
        }
    }

    /**
     * Sets the roles of a node that is a member itself, such as a terminal
     * carrying the data network. Empty roles remove it.
     */
    public void self(NodePos node, Set<DataRole> roles) {
        touch(node, null, node, roles);
    }

    /** Forgets every touch of a cable, its own membership included, after it left the network. */
    public void removeCable(NodePos cable) {
        revision++;
        for (Side side : Side.ALL) {
            untouch(new Touch(cable, side));
        }
        untouch(new Touch(cable, null));
    }

    /** Takes over every touch of another network, after a merge. */
    public void absorb(DataMembers other) {
        revision++;
        other.byMember.forEach((member, touches) -> touches.forEach((touch, roles) -> {
            untouch(touch);
            add(touch, member, roles);
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

    /** Counts every change of the members, so a reader can tell whether they changed since it last looked. */
    public int revision() {
        return revision;
    }

    public boolean contains(NodePos member) {
        return byMember.containsKey(member);
    }

    public int size() {
        return byMember.size();
    }

    private void add(Touch touch, NodePos member, Set<DataRole> roles) {
        byMember.computeIfAbsent(member, m -> new LinkedHashMap<>()).put(touch, EnumSet.copyOf(roles));
        byTouch.put(touch, member);
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
