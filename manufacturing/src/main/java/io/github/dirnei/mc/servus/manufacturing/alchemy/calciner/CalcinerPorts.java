package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleTanks;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;

/**
 * The ports of a formed Calciner, fixed by position: matter in at the mouth, fuel and upkeep at
 * the Firebox, Ash out of the Ash Pan, Mercury out of the Receiver and Sulfur out of the lowest duct.
 */
public final class CalcinerPorts {

    private CalcinerPorts() {
    }

    /** The principle a cell gives out, or null for cells without a fluid port. */
    public static Principle principle(Cell cell) {
        return switch (cell.role()) {
            case RECEIVER -> Principle.MERCURY;
            case DUCT -> cell.index() == CalcinerLayout.SULFUR_OUTLET ? Principle.SULFUR : null;
            default -> null;
        };
    }

    /** The Calciner of the formed structure the block at {@code pos} belongs to, or null. */
    public static CalcinerBlockEntity calciner(Level level, BlockPos pos, BlockState state) {
        Optional<BlockPos> controller = CalcinerStructure.controllerOf(state, pos);
        return controller.map(at -> level.getBlockEntity(at) instanceof CalcinerBlockEntity calciner ? calciner : null).orElse(null);
    }

    /** Matter at the Calciner block, fuel at the Firebox; nothing elsewhere or while unformed. */
    public static IItemHandler items(Level level, BlockPos pos, BlockState state) {
        CalcinerBlockEntity calciner = calciner(level, pos, state);
        if (calciner == null) {
            return null;
        }
        if (state.getBlock() instanceof CalcinerBlock) {
            return calciner.inputPort();
        }
        if (state.getBlock() instanceof CalcinerPartBlock part && part.role() == Role.FIREBOX) {
            return calciner.fuelPort();
        }
        if (state.getBlock() instanceof CalcinerPartBlock part && part.role() == Role.ASH_PAN) {
            return calciner.ashPort();
        }
        return null;
    }

    /** The fuel upkeep at the Firebox of a formed Calciner. */
    public static UpkeepHolder upkeep(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CalcinerPartBlock part) || part.role() != Role.FIREBOX) {
            return null;
        }
        CalcinerBlockEntity calciner = calciner(level, pos, state);
        return calciner == null ? null : calciner.upkeep();
    }

    /** The drain only tank of the port's principle on the Ash Pan, the Receiver or the lowest Copper Duct of a formed Calciner. */
    public static IFluidHandler fluid(Level level, BlockPos pos, BlockState state) {
        Cell cell;
        if (state.getBlock() instanceof CalcinerPartBlock part) {
            cell = CalcinerLayout.part(part.role());
        } else if (state.getBlock() instanceof CopperDuctBlock && state.getValue(CopperDuctBlock.FORMED)) {
            cell = CopperDuctBlock.cell(state);
        } else {
            return null;
        }
        Principle principle = principle(cell);
        if (principle == null) {
            return null;
        }
        CalcinerBlockEntity calciner = calciner(level, pos, state);
        return calciner == null ? null : calciner.tanks().view(principle);
    }

    /** Pushes each principle from its port into touching fluid handlers outside the structure. */
    public static void push(Level level, BlockPos controller, Direction direction, PrincipleTanks tanks) {
        Facing facing = CalcinerStructure.facing(direction);
        for (Cell cell : CalcinerLayout.cells()) {
            Principle principle = principle(cell);
            if (principle == null) {
                continue;
            }
            FluidTank tank = tanks.tank(principle);
            if (tank.isEmpty()) {
                continue;
            }
            BlockPos port = CalcinerStructure.pos(controller, cell, facing);
            for (Direction side : Direction.values()) {
                BlockPos target = port.relative(side);
                if (CalcinerStructure.controllerOf(level.getBlockState(target), target).filter(controller::equals).isPresent()) {
                    continue;
                }
                IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side.getOpposite());
                if (neighbour == null) {
                    continue;
                }
                int filled = neighbour.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
                if (tank.isEmpty()) {
                    break;
                }
            }
        }
    }

    /** The Ash Pan: the Calciner's Salt as whole Ash, at most a stack at a time, taken out only. */
    public static final class AshPort implements IItemHandler {

        private final CalcinerBlockEntity calciner;

        public AshPort(CalcinerBlockEntity calciner) {
            this.calciner = calciner;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            int ash = Math.min(calciner.ashCount(), getSlotLimit(0));
            return ash <= 0 ? ItemStack.EMPTY : new ItemStack(ServusManufacturingItems.ASH.get(), ash);
        }

        @Override
        public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int index, int amount, boolean simulate) {
            int taken = calciner.takeAsh(Math.min(amount, getSlotLimit(0)), simulate);
            return taken <= 0 ? ItemStack.EMPTY : new ItemStack(ServusManufacturingItems.ASH.get(), taken);
        }

        @Override
        public int getSlotLimit(int index) {
            return 64;
        }

        @Override
        public boolean isItemValid(int index, ItemStack stack) {
            return false;
        }
    }

    /** One slot of a machine inventory that only takes what {@code accepts} allows and never gives out. */
    public static final class InsertOnly implements IItemHandler {

        private final ItemStackHandler inventory;
        private final int slot;
        private final Predicate<ItemStack> accepts;

        public InsertOnly(ItemStackHandler inventory, int slot, Predicate<ItemStack> accepts) {
            this.inventory = inventory;
            this.slot = slot;
            this.accepts = accepts;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
            return accepts.test(stack) ? inventory.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int index, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int index) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int index, ItemStack stack) {
            return accepts.test(stack);
        }
    }
}
