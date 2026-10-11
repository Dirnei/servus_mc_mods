package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.StockProvider;
import io.github.dirnei.mc.servus.foundation.production.StockSink;
import io.github.dirnei.mc.servus.foundation.production.TransferGate;
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
                && (level.getCapability(Capabilities.Item.BLOCK, target, face) != null
                || level.getCapability(ServusFoundationCapabilities.STOCK_SINK, target, face) != null);
    }

    /** Inserts as much as fits; returns what did not. */
    static ItemStack insert(ServerLevel level, BlockPos target, Direction face, ItemStack stack) {
        if (stack.isEmpty() || !level.isLoaded(target)) {
            return stack;
        }
        IItemHandler handler = level.getCapability(Capabilities.Item.BLOCK, target, face);
        if (handler != null) {
            return ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
        }
        StockSink sink = level.getCapability(ServusFoundationCapabilities.STOCK_SINK, target, face);
        return sink != null ? sink.insert(stack.copy()) : stack;
    }

    /** The member's transfer gate on that face, or null when it limits nothing. */
    static TransferGate gate(ServerLevel level, BlockPos member, Direction face) {
        return level.isLoaded(member) ? level.getCapability(ServusFoundationCapabilities.TRANSFER_GATE, member, face) : null;
    }

    /** How many of {@code stack} the target would take right now; a stock sink is trusted to take all. */
    static int acceptable(ServerLevel level, BlockPos target, Direction face, ItemStack stack) {
        if (stack.isEmpty() || !level.isLoaded(target)) {
            return 0;
        }
        IItemHandler handler = level.getCapability(Capabilities.Item.BLOCK, target, face);
        if (handler != null) {
            return stack.getCount() - ItemHandlerHelper.insertItemStacked(handler, stack.copy(), true).getCount();
        }
        return level.getCapability(ServusFoundationCapabilities.STOCK_SINK, target, face) != null ? stack.getCount() : 0;
    }

    /** How many items like {@code like}, up to {@code max}, the source could give right now. */
    static int available(ServerLevel level, BlockPos source, Direction face, IItemHandler view, ItemStack like, int max) {
        if (max <= 0) {
            return 0;
        }
        if (view != null) {
            return count(view, like, max);
        }
        if (!level.isLoaded(source)) {
            return 0;
        }
        StockProvider provider = level.getCapability(ServusFoundationCapabilities.STOCK_PROVIDER, source, face);
        if (provider != null) {
            return (int) Math.min(max, Math.max(0, provider.count(like)));
        }
        IItemHandler handler = level.getCapability(Capabilities.Item.BLOCK, source, face);
        return handler == null ? 0 : count(handler, like, max);
    }

    private static int count(IItemHandler handler, ItemStack like, int max) {
        int found = 0;
        for (int slot = 0; slot < handler.getSlots() && found < max; slot++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), like)) {
                found += handler.extractItem(slot, max - found, true).getCount();
            }
        }
        return Math.min(found, max);
    }

    /** Takes up to {@code max} items like {@code like} from the source view, or from the source's capabilities without one. */
    static ItemStack extract(ServerLevel level, BlockPos source, Direction face, IItemHandler view, ItemStack like, int max) {
        return view != null ? take(view, like, max) : extract(level, source, face, like, max);
    }

    /** Gives items back to the source; returns what it did not take. */
    static ItemStack giveBack(ServerLevel level, BlockPos source, Direction face, IItemHandler view, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        if (view != null) {
            ItemStack rest = ItemHandlerHelper.insertItemStacked(view, stack.copy(), false);
            if (rest.isEmpty()) {
                return rest;
            }
            stack = rest;
        }
        return insert(level, source, face, stack);
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
        IItemHandler handler = level.getCapability(Capabilities.Item.BLOCK, source, face);
        return handler == null ? ItemStack.EMPTY : take(handler, like, max);
    }

    private static ItemStack take(IItemHandler handler, ItemStack like, int max) {
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
     * Puts items that cannot continue somewhere safe: back into the source,
     * into the target, onto the ground at a loaded place, or into the stash.
     */
    static void settle(ServerLevel level, ItemStack stack, BlockPos target, Direction targetFace,
                       BlockPos source, Direction sourceFace, BlockPos last) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack rest = stack;
        if (source != null) {
            rest = insert(level, source, sourceFace, rest);
        }
        rest = insert(level, target, targetFace, rest);
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
