package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesSounds;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/**
 * Shuttles' sounds.json: the chainsaw from our own recordings under
 * {@code sounds/}, and the Lumber hold and crane as heavier, lower pitched
 * vanilla door and piston sounds.
 */
final class ServusShuttlesSoundsProvider extends SoundDefinitionsProvider {

    ServusShuttlesSoundsProvider(PackOutput output) {
        super(output, ServusShuttlesIds.MOD_ID);
    }

    @Override
    public void registerSounds() {
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
    }

    /** A sound that plays a vanilla sound event at a lower pitch, under its own subtitle. */
    private void lowered(Holder<SoundEvent> sound, String name, String vanilla, double pitch) {
        add(sound, definition()
                .subtitle("subtitles.servus_shuttles." + name)
                .with(sound(vanilla, SoundDefinition.SoundType.EVENT).pitch(pitch)));
    }
}
