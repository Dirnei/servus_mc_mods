package io.github.dirnei.mc.servus.foundation.machine.mixer;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.drive.Drivable;
import io.github.dirnei.mc.servus.foundation.drive.Flywheel;
import io.github.dirnei.mc.servus.foundation.drive.WorkProgress;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.production.KindSlots;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import io.github.dirnei.mc.servus.foundation.side.TankRole;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSideBlock;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSideMover;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * Mixes what a mixing recipe takes, but only with the work a crank or motor hands it through {@link #drive}. Without a
 * driver the progress stays where it is. Four input slots, one kind each, one output pushed out of the front chute, and
 * one tank that sides and buckets fill and only a mixing drains. Clients learn the tank's fluid and whether any input
 * holds items, for the contents layer in the tub, and only when that changes.
 */
public class MixerBlockEntity extends MachineBlockEntity implements FluidSideBlock {

    public static final int INPUTS = 4;
    public static final int SLOT_OUTPUT = INPUTS;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "mixer");
    private static final int DEFAULT_FLUID_CAPACITY = 4000;
    /** Each recipe's work decides the time; the stats only carry the tank's capacity. */
    private static final MachineStats DEFAULTS = MachineStats.fluidHolder(DEFAULT_FLUID_CAPACITY);
    /** Ticks after the last turn in which the Mixer still counts as turning; bridges a tick without drive. */
    private static final int TURNING_WINDOW = 5;
    private static final float PADDLE_DEGREES_PER_TICK = 9;
    private static final int FLUID_MOVE_INTERVAL = 8;
    private static final List<SideTank> SIDE_TANKS = List.of(new SideTank(TankRole.FILL_ONLY));
    private static final String MIX_FLUID = "MixFluid";
    private static final String MIX_ITEMS = "MixItems";

    private final WorkProgress work = new WorkProgress();
    private final Flywheel paddle = new Flywheel(PADDLE_DEGREES_PER_TICK);
    private final FluidSlot tank = new FluidSlot(DEFAULT_FLUID_CAPACITY, this::usesFluid) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private long lastTurned = Long.MIN_VALUE / 2;
    /** Server: what watching clients were last sent; null before the first send. */
    private @Nullable Contents sent;
    /** Client: what the tub shows, as the server last sent it. */
    private Contents shown = Contents.EMPTY;
    private final Drivable drive = new Drivable() {
        @Override
        public boolean wantsWork() {
            return runnableRecipe().isPresent();
        }

        @Override
        public void addWork(int amount) {
            turn();
            runnableRecipe().ifPresent(recipe -> mix(recipe, amount));
        }

        @Override
        public void turn() {
            lastTurned = level.getGameTime();
        }
    };

    /** What the tub shows: the tank's fluid, else a mix surface while any input holds items. */
    private record Contents(Fluid fluid, boolean items) {
        static final Contents EMPTY = new Contents(Fluids.EMPTY, false);
    }

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.MIXER_ENTITY.get(), pos, state, INPUTS + 1);
        sideConfig().lock(RelativeSide.FRONT, SideMode.PUSH);
        for (RelativeSide side : new RelativeSide[] {RelativeSide.TOP, RelativeSide.BACK, RelativeSide.LEFT, RelativeSide.RIGHT}) {
            sideConfig().set(side, SideMode.INPUT);
        }
    }

    /** The drive link on {@code side}: the back and both sides take drive, the top, bottom and chute front do not. */
    public @Nullable Drivable drive(@Nullable Direction side) {
        if (side == null) {
            return drive;
        }
        RelativeSide relative = relativeSide(side);
        return relative == RelativeSide.BACK || relative == RelativeSide.LEFT || relative == RelativeSide.RIGHT ? drive : null;
    }

    /** The paddle's flywheel on the client. */
    public Flywheel paddle() {
        return paddle;
    }

    /** The tank itself, for buckets by hand; automation gets the fill only side handlers. */
    public FluidSlot tank() {
        return tank;
    }

    /** Client: the fluid the tub shows, empty when it shows none. */
    public FluidStack shownFluid() {
        return shown.fluid() == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(shown.fluid(), 1000);
    }

    /** Client: whether the tub shows the dry mix surface. */
    public boolean showsMix() {
        return shown.fluid() == Fluids.EMPTY && shown.items();
    }

    @Override
    protected Identifier machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot < INPUTS;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    /**
     * What some mixing recipe takes, into the slot holding that kind or an empty slot when no other holds it. The
     * recipes are synced, so the client's slots agree with the server.
     */
    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return level != null && ServusRecipes.all(level, MachineContent.MIXING.get()).stream().anyMatch(holder -> holder.value().accepts(stack))
                && KindSlots.accepts(inputs(), slot, stack, ItemStack::isEmpty, ItemStack::isSameItemSameComponents);
    }

    /** Whether some mixing recipe uses this fluid. */
    private boolean usesFluid(FluidStack stack) {
        return level != null && ServusRecipes.all(level, MachineContent.MIXING.get()).stream().anyMatch(holder -> holder.value().acceptsFluid(stack));
    }

    private List<ItemStack> inputs() {
        List<ItemStack> inputs = new ArrayList<>(INPUTS);
        for (int slot = 0; slot < INPUTS; slot++) {
            inputs.add(inventory.getStackInSlot(slot));
        }
        return inputs;
    }

    /** The matching recipe; one with a fluid wins over one without, so a dry recipe never shadows a wet one. */
    private Optional<MixingRecipe> currentRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        MixingInput input = new MixingInput(inputs(), tank.getFluid());
        if (input.isEmpty()) {
            return Optional.empty();
        }
        List<MixingRecipe> matching = ServusRecipes.all(level, MachineContent.MIXING.get()).stream().map(RecipeHolder::value)
                .filter(recipe -> recipe.matches(input, level)).toList();
        return matching.stream().filter(recipe -> recipe.fluid().isPresent()).findFirst().or(() -> matching.stream().findFirst());
    }

    /** The current recipe while its result fits into the output. */
    private Optional<MixingRecipe> runnableRecipe() {
        return currentRecipe().filter(recipe -> canInsertOutput(SLOT_OUTPUT, recipe.result()));
    }

    private void mix(MixingRecipe recipe, int amount) {
        if (work.add(amount, recipe.work())) {
            int[] slots = recipe.slotsFor(new MixingInput(inputs(), tank.getFluid())).orElseThrow();
            for (int i = 0; i < slots.length; i++) {
                inventory.extractItem(slots[i], recipe.ingredients().get(i).count(), false);
            }
            recipe.fluid().ifPresent(fluid -> tank.drain(fluid.amount(), false));
            insertOutput(SLOT_OUTPUT, recipe.result());
        }
        setChanged();
    }

    /**
     * Forgets the progress when nothing can be mixed, follows the tank capacity from data, pulls fluid on Pull sides and
     * tells clients when the contents changed; reports working while a driver turned it just now.
     */
    @Override
    protected boolean tick() {
        if (work.value() > 0 && currentRecipe().isEmpty()) {
            work.reset();
            setChanged();
        }
        tank.setCapacity(stats().fluidCapacity());
        FluidSideMover.tick(level, worldPosition, this, FLUID_MOVE_INTERVAL);
        sendContents();
        return level.getGameTime() - lastTurned < TURNING_WINDOW;
    }

    private void sendContents() {
        Contents now = new Contents(tank.getFluid().getFluid(), inputs().stream().anyMatch(stack -> !stack.isEmpty()));
        if (!now.equals(sent)) {
            sent = now;
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public List<SideTank> sideTanks() {
        return SIDE_TANKS;
    }

    @Override
    public List<FluidSlot> sideSlots() {
        return List.of(tank);
    }

    @Override
    public boolean sideTankAccepts(int index, FluidResource fluid) {
        return usesFluid(fluid.toStack(1));
    }

    @Override
    protected List<SideMode> fluidModes() {
        return SideConfig.INBOUND_MODES;
    }

    /** The back, left and right are fluid sides; the top and bottom are fixed Off besides the front. */
    @Override
    public FluidSides fluidSides() {
        FluidSides sides = super.fluidSides();
        if (sides != null && !sides.isLocked(RelativeSide.TOP)) {
            sides.lock(RelativeSide.TOP);
            sides.lock(RelativeSide.BOTTOM);
        }
        return sides;
    }

    @Override
    protected Fluid tankFluid(int index) {
        return tank.getFluid().getFluid();
    }

    @Override
    protected int dataCount() {
        return DATA_COUNT_FLUID;
    }

    /** The arrow shows the work collected toward the recipe's work; the gauge the tank. */
    @Override
    protected int getData(int index) {
        return switch (index) {
            case DATA_PROGRESS -> work.value();
            case DATA_WORK_TICKS -> currentRecipe().map(MixingRecipe::work).orElse(1);
            case DATA_FLUID_AMOUNT -> tank.getFluidAmount();
            case DATA_FLUID_CAPACITY -> tank.getCapacity();
            case DATA_FLUID_ID -> BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
            default -> super.getData(index);
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Work", work.value());
        tank.save(output, "Tank");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        work.set(input.getIntOr("Work", 0));
        tank.load(input, "Tank");
        shown = new Contents(input.read(MIX_FLUID, BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY),
                input.getBooleanOr(MIX_ITEMS, false));
    }

    /** Sends what the tub shows along with the side modes. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!tank.isEmpty()) {
            tag.store(MIX_FLUID, BuiltInRegistries.FLUID.byNameCodec(), tank.getFluid().getFluid());
        }
        tag.putBoolean(MIX_ITEMS, inputs().stream().anyMatch(stack -> !stack.isEmpty()));
        return tag;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MixerMenu(containerId, playerInventory, this);
    }
}
