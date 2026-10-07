package io.github.dirnei.mc.servus.logistics.duct;

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
 * the line, or sneaking, places a new duct against the clicked face.
 */
public class LineItem extends BlockItem {

    private final Line line;

    public LineItem(Block block, Line line, Properties properties) {
        super(block, properties);
        this.line = line;
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
        // A duct that already carries this line, or a sneak click, places a new duct against the clicked face.
        if (!sneaking && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && !duct.has(line)) {
            if (!level.isClientSide()) {
                if (!duct.addLine(line)) {
                    return InteractionResult.FAIL;
                }
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
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
            duct.addLine(line);
        }
        return true;
    }

    /** Each line item has its own name instead of the shared block name. */
    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }
}
