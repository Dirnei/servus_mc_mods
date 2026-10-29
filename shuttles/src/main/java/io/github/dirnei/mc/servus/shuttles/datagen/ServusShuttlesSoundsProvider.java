package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesSounds;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/**
 * Shuttles' sounds.json: the shuttle drive and the chainsaw from our own
 * recordings under {@code sounds/}, and the Lumber hold and crane as heavier, lower pitched
 * vanilla door and piston sounds.
 */
final class ServusShuttlesSoundsProvider extends SoundDefinitionsProvider {

    /** How many takes of the drive loop there are; each play picks one, so shuttles side by side are out of step. */
    private static final int DRIVE_TAKES = 3;
    /** Blocks within which a shuttle's drive is heard. */
    private static final int DRIVE_RANGE = 10;
    private static final double DRIVE_VOLUME = 0.25;
    private static final double DRIVE_MUFFLED_VOLUME = 0.08;

    ServusShuttlesSoundsProvider(PackOutput output) {
        super(output, ServusShuttlesIds.MOD_ID);
    }

    @Override
    public void registerSounds() {
        drive(ServusShuttlesSounds.SHUTTLE_DRIVE, "shuttle.drive", "shuttle_drive_", DRIVE_VOLUME);
        drive(ServusShuttlesSounds.SHUTTLE_DRIVE_MUFFLED, "shuttle.drive_muffled", "shuttle_drive_muffled_", DRIVE_MUFFLED_VOLUME);
        add(ServusShuttlesSounds.CHAINSAW_IDLE, definition()
                .subtitle("subtitles.servus_shuttles.chainsaw.idle")
                .with(sound(ServusShuttlesIds.MOD_ID + ":chainsaw_idle_loop")));
        add(ServusShuttlesSounds.CHAINSAW_CUT, definition()
                .subtitle("subtitles.servus_shuttles.chainsaw.cut")
                .with(sound(ServusShuttlesIds.MOD_ID + ":chainsaw_cut")));
        add(ServusShuttlesSounds.CHAINSAW_STOP, definition()
                .subtitle("subtitles.servus_shuttles.chainsaw.stop")
                .with(sound(ServusShuttlesIds.MOD_ID + ":chainsaw_stop")));
        lowered(ServusShuttlesSounds.HOLD_DOORS_OPEN, "hold.doors_open", "minecraft:block.iron_trapdoor.open", 0.7);
        lowered(ServusShuttlesSounds.HOLD_WALLS_DOWN, "hold.walls_down", "minecraft:block.iron_door.close", 0.55);
        lowered(ServusShuttlesSounds.CRANE_UP, "crane.up", "minecraft:block.piston.extend", 0.6);
        lowered(ServusShuttlesSounds.CRANE_DOWN, "crane.down", "minecraft:block.piston.contract", 0.6);
        lowered(ServusShuttlesSounds.HOLD_WALLS_UP, "hold.walls_up", "minecraft:block.iron_door.open", 0.55);
        lowered(ServusShuttlesSounds.HOLD_DOORS_SHUT, "hold.doors_shut", "minecraft:block.iron_trapdoor.close", 0.7);
        lowered(ServusShuttlesSounds.HANDHELD_BEEP, "handheld.beep", "minecraft:block.note_block.bit", 1.0);
        lowered(ServusShuttlesSounds.HANDHELD_BUZZ, "handheld.buzz", "minecraft:block.note_block.didgeridoo", 0.6);
        lowered(ServusShuttlesSounds.HANDHELD_CLICK, "handheld.click", "minecraft:block.stone_button.click_on", 1.8);
    }

    /** A drive loop: every take streamed at the volume and heard within {@link #DRIVE_RANGE}. */
    private void drive(Holder<SoundEvent> sound, String name, String file, double volume) {
        SoundDefinition definition = definition().subtitle("subtitles.servus_shuttles." + name);
        for (int take = 1; take <= DRIVE_TAKES; take++) {
            definition.with(sound(ServusShuttlesIds.MOD_ID + ":" + file + take).stream().volume(volume).attenuationDistance(DRIVE_RANGE));
        }
        add(sound, definition);
    }

    /** A sound that plays a vanilla sound event at another pitch, under its own subtitle. */
    private void lowered(Holder<SoundEvent> sound, String name, String vanilla, double pitch) {
        add(sound, definition()
                .subtitle("subtitles.servus_shuttles." + name)
                .with(sound(vanilla, SoundDefinition.SoundType.EVENT).pitch(pitch)));
    }
}
