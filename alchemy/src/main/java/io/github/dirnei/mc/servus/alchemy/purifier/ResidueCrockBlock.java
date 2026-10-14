package io.github.dirnei.mc.servus.alchemy.purifier;

import io.github.dirnei.mc.servus.alchemy.PlayerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The Residue Crock on the other side of the Purifier's log: the Caput Mortuum falls into it and is taken from it. */
public class ResidueCrockBlock extends PurifierSideBlock {

    public ResidueCrockBlock(Properties properties) {
        super(properties);
    }

    /** An empty hand takes the Caput Mortuum. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(vat(pos, state)) instanceof PurifierBlockEntity purifier && purifier.formed()) {
            PlayerItems.give(player, purifier.takeResidue());
        }
        return InteractionResult.SUCCESS;
    }
}
