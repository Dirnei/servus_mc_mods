package io.github.dirnei.mc.servus.logistics.duct;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Model data keys of the utility duct, kept out of client only classes. */
public final class DuctModelData {

    /** The facade block state, absent without a facade. */
    public static final ModelProperty<BlockState> FACADE = new ModelProperty<>();

    private DuctModelData() {
    }
}
