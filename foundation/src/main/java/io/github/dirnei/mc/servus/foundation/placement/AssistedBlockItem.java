package io.github.dirnei.mc.servus.foundation.placement;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** A block item whose sneak placement extends a straight run (see {@link PlacementAssists}). */
public class AssistedBlockItem extends BlockItem implements AssistedItem {

    private final PlacementAssist assist;

    public AssistedBlockItem(Block block, PlacementAssist assist, Properties properties) {
        super(block, properties);
        this.assist = assist;
    }

    @Override
    public PlacementAssist placementAssist() {
        return assist;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return PlacementAssists.place(context, assist, super::place);
    }

    /** Places like a plain block item, without the assist, for items that place several blocks for one click. */
    protected InteractionResult placeWithoutAssist(BlockPlaceContext context) {
        return super.place(context);
    }
}
