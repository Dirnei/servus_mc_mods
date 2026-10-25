package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server to client, once per felled tree to the players tracking its chunk:
 * everything the client needs to draw the tree toppling and breaking apart.
 *
 * @param base   the smaller corner of the trunk's plots on the ground
 * @param away   the way the tree falls, straight away from the rail
 * @param rail   the rail the Lumber shuttle stands on, where the drops fly to
 * @param ticks  how long the fall and the break apart last
 * @param blocks every removed block, relative to the base
 * @param drops  the tree's drops merged by item, at most {@link #MAX_DROPS} entries
 */
public record TreeFallPayload(BlockPos base, Direction away, BlockPos rail, int ticks, List<FallingBlock> blocks, List<ItemStack> drops)
        implements CustomPacketPayload {

    /** The most drop entries the payload carries; the client only shows them flying. */
    public static final int MAX_DROPS = 16;

    public static final Type<TreeFallPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "tree_fall"));

    /** One removed block: its offset from the base and its state. */
    public record FallingBlock(int dx, int dy, int dz, BlockState state) {

        public static final StreamCodec<RegistryFriendlyByteBuf, FallingBlock> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FallingBlock::dx,
                ByteBufCodecs.VAR_INT, FallingBlock::dy,
                ByteBufCodecs.VAR_INT, FallingBlock::dz,
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), FallingBlock::state,
                FallingBlock::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TreeFallPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TreeFallPayload::base,
            Direction.STREAM_CODEC, TreeFallPayload::away,
            BlockPos.STREAM_CODEC, TreeFallPayload::rail,
            ByteBufCodecs.VAR_INT, TreeFallPayload::ticks,
            FallingBlock.STREAM_CODEC.apply(ByteBufCodecs.list()), TreeFallPayload::blocks,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_DROPS)), TreeFallPayload::drops,
            TreeFallPayload::new);

    /** The drops merged by item and components, each at most a stack, at most {@link #MAX_DROPS} entries. */
    public static List<ItemStack> merged(List<ItemStack> drops) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack same = null;
            for (ItemStack kept : merged) {
                if (ItemStack.isSameItemSameComponents(kept, drop)) {
                    same = kept;
                    break;
                }
            }
            if (same != null) {
                same.setCount(Math.min(same.getMaxStackSize(), same.getCount() + drop.getCount()));
            } else if (merged.size() < MAX_DROPS && !drop.isEmpty()) {
                merged.add(drop.copy());
            }
        }
        return merged;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
