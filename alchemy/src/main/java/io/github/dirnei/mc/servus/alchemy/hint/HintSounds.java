package io.github.dirnei.mc.servus.alchemy.hint;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The sounds of the hints at hidden things. */
public final class HintSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ServusAlchemyIds.MOD_ID);

    /** Something knocks in the rock: a working gnome or the old gnome. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KNOCK = SOUNDS.register("hint.knock",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "hint.knock")));

    private HintSounds() {
    }
}
