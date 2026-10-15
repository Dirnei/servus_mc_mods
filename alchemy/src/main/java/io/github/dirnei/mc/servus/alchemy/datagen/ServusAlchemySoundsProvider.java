package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hint.HintSounds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/** Alchemy's sounds.json: our sound events made from vanilla sound files. */
final class ServusAlchemySoundsProvider extends SoundDefinitionsProvider {

    /** Stone dug at a low pitch reads as a knock in the rock. */
    private static final float KNOCK_PITCH = 0.6F;

    ServusAlchemySoundsProvider(PackOutput output) {
        super(output, ServusAlchemyIds.MOD_ID);
    }

    @Override
    public void registerSounds() {
        add(HintSounds.KNOCK, definition()
                .subtitle("subtitles.servus_alchemy.hint.knock")
                .with(sound("minecraft:dig/stone1").pitch(KNOCK_PITCH),
                        sound("minecraft:dig/stone2").pitch(KNOCK_PITCH),
                        sound("minecraft:dig/stone3").pitch(KNOCK_PITCH),
                        sound("minecraft:dig/stone4").pitch(KNOCK_PITCH)));
    }
}
