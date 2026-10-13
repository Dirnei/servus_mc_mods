package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Places the blocks of a warehouse in GameTests. Positions are relative to the test. */
public final class WarehouseFixtures {

    private WarehouseFixtures() {
    }

    /** A Warehouse Controller facing north with a full energy buffer. */
    public static WarehouseControllerBlockEntity controller(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(pos, WarehouseControllerBlockEntity.class);
        PowerFixtures.fill(controller);
        return controller;
    }

    /** A Heavy Duty Rack with its front to {@code facing}. */
    public static HeavyRackBlockEntity rack(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, facing));
        return helper.getBlockEntity(pos, HeavyRackBlockEntity.class);
    }

    /** A Shuttle Garage facing {@code facing} with {@code shuttles} Mk1 shuttles inside; its rail goes behind it. */
    public static ShuttleGarageBlockEntity garage(GameTestHelper helper, BlockPos pos, Direction facing, int shuttles) {
        helper.setBlock(pos, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, facing));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(pos, ShuttleGarageBlockEntity.class);
        for (int i = 0; i < shuttles; i++) {
            helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        }
        return garage;
    }

    /** A warehouse rail shaped by its neighbours, the way a player places it. */
    public static void rail(GameTestHelper helper, BlockPos pos) {
        BlockState state = Block.updateFromNeighbourShapes(ServusStorageBlocks.RAIL.get().defaultBlockState(),
                helper.getLevel(), helper.absolutePos(pos));
        helper.setBlock(pos, state);
    }

    /** Stone under {@code pos}, then a {@link #rail} on it. */
    public static void railOnStone(GameTestHelper helper, BlockPos pos) {
        stone(helper, pos.below());
        rail(helper, pos);
    }

    public static void stone(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.STONE.defaultBlockState());
    }

    /** Fills the slots of the rack at {@code rack} with the stacks, as test setup. */
    public static void stock(GameTestHelper helper, BlockPos rack, ItemStack... stacks) {
        helper.assertTrue(put(helper.getBlockEntity(rack, HeavyRackBlockEntity.class), stacks), "the rack takes the test stock");
    }

    /** Puts the stacks into the rack's slots; false when not all of them fit. */
    public static boolean put(HeavyRackBlockEntity rack, ItemStack... stacks) {
        boolean all = true;
        for (ItemStack stack : stacks) {
            ItemStack left = stack.copy();
            for (int slot = 0; slot < rack.items().getSlots() && !left.isEmpty(); slot++) {
                left = rack.items().insertItem(slot, left, false);
            }
            all &= left.isEmpty();
        }
        return all;
    }

    /** Tops every slot of the rack up: empty slots get full stacks of the item, others their own item. */
    public static void fill(HeavyRackBlockEntity rack, Item item) {
        for (int slot = 0; slot < rack.items().getSlots(); slot++) {
            ItemStack inSlot = rack.items().getStackInSlot(slot);
            rack.items().insertItem(slot, inSlot.isEmpty() ? new ItemStack(item, item.getDefaultMaxStackSize())
                    : inSlot.copyWithCount(inSlot.getMaxStackSize()), false);
        }
    }

    /** How many of the item the rack holds. */
    public static int count(HeavyRackBlockEntity rack, Item item) {
        int total = 0;
        for (int slot = 0; slot < rack.items().getSlots(); slot++) {
            ItemStack stack = rack.items().getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Whether the storage block at {@code pos} shows its online lamp. */
    public static boolean lit(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ONLINE);
    }

    /** The lamp state of the storage block at {@code pos}. */
    public static StorageNodeBlocks.Activity activity(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ACTIVITY);
    }
}
