package io.github.dirnei.mc.servus.shuttles.network;

/** A block whose activity lamp the warehouse sets from its tanker runs. */
public interface FluidActivityHolder {

    /** Sets the lamp and the reasons the GUI names; server side, set by the warehouse only. */
    void setFluidActivity(FluidActivity activity);

    /** The lamp and reasons as last set. */
    FluidActivity fluidActivity();
}
