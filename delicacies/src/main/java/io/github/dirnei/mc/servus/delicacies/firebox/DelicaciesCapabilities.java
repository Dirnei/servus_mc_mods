package io.github.dirnei.mc.servus.delicacies.firebox;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;

/** The block capabilities Servus: Delicacies offers to its own blocks and to other Servus mods. */
public final class DelicaciesCapabilities {

    /**
     * The firebox a block is part of. A cooking spot asks the block below it; a block that heats nothing returns null.
     * Other mods (the Brewery's Brewhouse) register it on their own blocks.
     */
    public static final BlockCapability<Firebox, Void> FIREBOX =
            BlockCapability.createVoid(Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "firebox"), Firebox.class);

    private DelicaciesCapabilities() {
    }
}
