package io.github.dirnei.mc.servus.alchemy.client.gnome;

import io.github.dirnei.mc.servus.alchemy.gnome.GnomeAction;
import io.github.dirnei.mc.servus.alchemy.gnome.GnomeKind;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the gnome model needs from its entity for one frame. */
public class GnomeRenderState extends LivingEntityRenderState {

    public GnomeKind kind = GnomeKind.MINER;
    public GnomeAction action = GnomeAction.IDLE;
}
