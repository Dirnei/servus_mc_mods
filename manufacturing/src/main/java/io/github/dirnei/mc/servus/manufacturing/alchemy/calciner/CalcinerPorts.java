package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The ports of a formed Calciner, fixed by position: matter in at the mouth, fuel and upkeep at
 * the Firebox and Ash out of the Ash Pan. Sulfur and Mercury go straight into the structure's own
 * two tanks; no block of the structure has a fluid port.
 */
public final class CalcinerPorts {

    private CalcinerPorts() {
    }

    /** The Calciner of the formed structure the block at {@code pos} belongs to, or null. */
    public static CalcinerBlockEntity calciner(Level level, BlockPos pos, BlockState state) {
        Optional<BlockPos> controller = CalcinerStructure.controllerOf(state, pos);
        return controller.map(at -> level.getBlockEntity(at) instanceof CalcinerBlockEntity calciner ? calciner : null).orElse(null);
    }

    /** Matter at the Calciner block, fuel at the Firebox; nothing elsewhere or while unformed. */
    public static LegacyItemHandler items(Level level, BlockPos pos, BlockState state) {
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

    /** Whether the structure's own tanks take all of {@code output}'s Sulfur and Mercury. */
    public static boolean tanksTake(Level level, BlockPos controller, Direction direction, Principles output) {
        return fillTanks(level, controller, direction, output, true);
    }

    /** Puts {@code output}'s Sulfur and Mercury into the structure's own tanks; ask {@link #tanksTake} first. */
    public static void fillTanks(Level level, BlockPos controller, Direction direction, Principles output) {
        fillTanks(level, controller, direction, output, false);
    }

    private static boolean fillTanks(Level level, BlockPos controller, Direction direction, Principles output, boolean simulate) {
        Facing facing = CalcinerStructure.facing(direction);
        BlockPos sulfur = CalcinerStructure.pos(controller, CalcinerLayout.cell(CalcinerLayout.SULFUR_TANK), facing);
        BlockPos mercury = CalcinerStructure.pos(controller, CalcinerLayout.cell(CalcinerLayout.MERCURY_TANK), facing);
        return fill(level, sulfur, Principle.SULFUR, output.sulfur(), simulate)
                & fill(level, mercury, Principle.MERCURY, output.mercury(), simulate);
    }

    private static boolean fill(Level level, BlockPos tank, Principle principle, int amount, boolean simulate) {
        if (amount <= 0) {
            return true;
        }
        FluidView view = FluidView.of(level.getCapability(Capabilities.Fluid.BLOCK, tank, Direction.UP));
        return view != null && view.fill(new FluidStack(PrincipleFluids.fluid(principle), amount), simulate) == amount;
    }

    /** The Ash Pan: the Calciner's Salt as whole Ash, at most a stack at a time, taken out only. */
    public static final class AshPort extends LegacyItemHandler {

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
}
