package io.github.dirnei.mc.servus.shuttles.menu;

/** A station GUI whose title row renames the station. */
public interface StationNameMenu {

    /** Server side: the name typed in the GUI, cleaned by the station; ignored once the player may no longer use the GUI. */
    void rename(String typed);
}
