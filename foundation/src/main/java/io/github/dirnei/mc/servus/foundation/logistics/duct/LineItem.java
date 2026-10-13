package io.github.dirnei.mc.servus.foundation.logistics.duct;

import io.github.dirnei.mc.servus.foundation.placement.AssistedItem;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssist;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssists;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a utility duct carrying this line (Item Pipe, Power Cable or Fluid Pipe), or
 * adds the line to an existing duct that lacks it. Clicking a duct that already carries
 * the line places a new duct against the clicked face. Sneaking walks the run of ducts
 * carrying the line in the direction of the clicked face and places past its end.
 */
public class LineItem extends BlockItem implements AssistedItem {

    private final Line line;
    private final DuctAssist assist;

    public LineItem(Block block, Line line, Properties properties) {
        super(block, properties);
        this.line = line;
        this.assist = new DuctAssist(line);
    }

    @Override
    public PlacementAssist placementAssist() {
        return assist;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return PlacementAssists.place(context, assist, super::place);
    }

    public Line line() {
        return line;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isSecondaryUseActive();
        if (!sneaking && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && !duct.has(line)) {
            if (!level.isClientSide()) {
                if (!addTo(duct)) {
                    return InteractionResult.FAIL;
                }
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!super.placeBlock(context, state)) {
            return false;
        }
        Level level = context.getLevel();
        if (!level.isClientSide() && level.getBlockEntity(context.getClickedPos()) instanceof UtilityDuctBlockEntity duct) {
            addTo(duct);
        }
        return true;
    }

    /** Adds this item's line to the duct. */
    protected boolean addTo(UtilityDuctBlockEntity duct) {
        return duct.addLine(line);
    }
}
