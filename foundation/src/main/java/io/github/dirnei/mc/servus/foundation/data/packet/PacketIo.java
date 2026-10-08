package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.StockProvider;
import io.github.dirnei.mc.servus.foundation.production.StockSink;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Moving items into and out of data members, and placing items that have nowhere to go. Server thread only. */
final class PacketIo {

    private PacketIo() {
    }

    /** The face of {@code member} that looks at {@code toward}. */
    static Direction face(BlockPos member, BlockPos toward) {
        Direction direction = Direction.fromDelta(toward.getX() - member.getX(), toward.getY() - member.getY(),
                toward.getZ() - member.getZ());
        return direction == null ? Direction.UP : direction;
    }

    /** Whether the target can take items at all on that face. */
    static boolean accepts(ServerLevel level, BlockPos target, Direction face) {
        return level.isLoaded(target)
                && (level.getCapability(Capabilities.ItemHandler.BLOCK, target, face) != null
                || level.getCapability(ServusFoundationCapabilities.STOCK_SINK, target, face) != null);
    }

    /** Inserts as much as fits; returns what did not. */
    static ItemStack insert(ServerLevel level, BlockPos target, Direction face, ItemStack stack) {
        if (stack.isEmpty() || !level.isLoaded(target)) {
            return stack;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, face);
        if (handler != null) {
            return ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
        }
        StockSink sink = level.getCapability(ServusFoundationCapabilities.STOCK_SINK, target, face);
        return sink != null ? sink.insert(stack.copy()) : stack;
    }

    /** Takes up to {@code max} items like {@code like} from the source. */
    static ItemStack extract(ServerLevel level, BlockPos source, Direction face, ItemStack like, int max) {
        if (max <= 0 || !level.isLoaded(source)) {
            return ItemStack.EMPTY;
        }
        StockProvider provider = level.getCapability(ServusFoundationCapabilities.STOCK_PROVIDER, source, face);
        if (provider != null) {
            return provider.extract(like, max);
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, source, face);
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = ItemStack.EMPTY;
        for (int slot = 0; slot < handler.getSlots() && taken.getCount() < max; slot++) {
            if (!ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), like)) {
                continue;
            }
            ItemStack got = handler.extractItem(slot, max - taken.getCount(), false);
            if (got.isEmpty()) {
                continue;
            }
            if (taken.isEmpty()) {
                taken = got;
            } else {
                taken.grow(got.getCount());
            }
        }
        return taken;
    }

    /**
     * Puts items that cannot continue somewhere safe: into the target, the
     * source, onto the ground at a loaded place, or into the stash.
     */
    static void settle(ServerLevel level, ItemStack stack, BlockPos target, Direction targetFace,
                       BlockPos source, Direction sourceFace, BlockPos last) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack rest = insert(level, target, targetFace, stack);
        if (source != null) {
            rest = insert(level, source, sourceFace, rest);
        }
        if (rest.isEmpty()) {
            return;
        }
        List<BlockPos> places = new ArrayList<>();
        places.add(target);
        if (source != null) {
            places.add(source);
        }
        if (last != null) {
            places.add(last);
        }
        for (BlockPos place : places) {
            if (level.isLoaded(place)) {
                Containers.dropItemStack(level, place.getX() + 0.5, place.getY() + 0.5, place.getZ() + 0.5, rest);
                return;
            }
        }
        PacketStash.of(level).add(rest, places);
    }
}
