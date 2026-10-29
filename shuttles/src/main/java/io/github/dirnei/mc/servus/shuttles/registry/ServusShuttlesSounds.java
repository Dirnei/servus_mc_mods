package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The sounds of Shuttles: the drive of a moving shuttle, the Lumber shuttle's chainsaw, its hold and crane folding out and in, and the handheld. */
public final class ServusShuttlesSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ServusShuttlesIds.MOD_ID);

    /** A shuttle drives, looped while it moves. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SHUTTLE_DRIVE = register("shuttle.drive");
    /** A muffled shuttle drives: the same loop damped and quieter. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SHUTTLE_DRIVE_MUFFLED = register("shuttle.drive_muffled");
    /** The chainsaw idles, looped, while the crane is out and the chain does not cut. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAINSAW_IDLE = register("chainsaw.idle");
    /** The chainsaw revs up and cuts while the bar swings across the log. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAINSAW_CUT = register("chainsaw.cut");
    /** The chainsaw winds down as the head lets go of the tree. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAINSAW_STOP = register("chainsaw.stop");
    /** The hold's lid doors swing open. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_DOORS_OPEN = register("hold.doors_open");
    /** The hold's walls land on the ground as stabilizers. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_WALLS_DOWN = register("hold.walls_down");
    /** The crane rises out of the hold. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRANE_UP = register("crane.up");
    /** The crane sinks into the hold. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRANE_DOWN = register("crane.down");
    /** The hold's walls swing back up. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_WALLS_UP = register("hold.walls_up");
    /** The hold's lid doors close. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_DOORS_SHUT = register("hold.doors_shut");

    /** The handheld scanned a bay; its pitch rises along a sweep. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDHELD_BEEP = register("handheld.beep");
    /** The handheld's scan changed nothing or found nothing. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDHELD_BUZZ = register("handheld.buzz");
    /** The handheld switched its mode. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDHELD_CLICK = register("handheld.click");

    private ServusShuttlesSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, name)));
    }
}
