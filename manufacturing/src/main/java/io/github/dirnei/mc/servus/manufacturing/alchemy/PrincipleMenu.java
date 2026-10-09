package io.github.dirnei.mc.servus.manufacturing.alchemy;

/** A menu that syncs three principle tanks for its screen's gauges. */
public interface PrincipleMenu {

    int principleAmount(Principle principle);

    int principleCapacity();
}
