package io.github.dirnei.mc.servus.lib.network.core;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import java.util.ArrayList;
import java.util.List;

/** Records every hook call so tests can assert order and arguments. */
final class TestNetwork extends ServusNetwork<String, String> {

    static final List<String> LOG = new ArrayList<>();

    TestNetwork(int id, NetworkSet<String, String, ?> owner) {
        super(id, owner);
        LOG.add("create " + id);
    }

    @Override
    protected void handle(String event) {
        LOG.add("handle " + id() + " " + event);
        if (event.startsWith("echo")) {
            post("echoed");
        }
    }

    @Override
    protected void onTick() {
        LOG.add("tick " + id());
    }

    @Override
    protected void onNodeAdded(NodePos pos, String node) {
        LOG.add("added " + id() + " " + node);
    }

    @Override
    protected void onNodeRemoved(NodePos pos, String node) {
        LOG.add("removed " + id() + " " + node + " size=" + size());
    }

    @Override
    protected void onAbsorb(ServusNetwork<String, String> other) {
        LOG.add("absorb " + id() + " <- " + other.id() + " disposed=" + other.isDisposed());
    }

    @Override
    protected void onDisposed() {
        LOG.add("dispose " + id());
    }
}
