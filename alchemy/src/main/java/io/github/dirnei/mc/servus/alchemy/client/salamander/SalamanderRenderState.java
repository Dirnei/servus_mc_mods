package io.github.dirnei.mc.servus.alchemy.client.salamander;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the salamander model needs from its entity for one frame. */
public class SalamanderRenderState extends LivingEntityRenderState {

    /** Sitting on command or lying in a campfire: legs spread, body down. */
    public boolean lying;
}
