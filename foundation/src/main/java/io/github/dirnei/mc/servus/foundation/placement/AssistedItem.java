package io.github.dirnei.mc.servus.foundation.placement;

/** An item whose sneak placement walks a run; the client preview looks it up through this. */
public interface AssistedItem {

    PlacementAssist placementAssist();
}
