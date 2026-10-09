package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.BurnerMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import net.minecraft.server.level.ServerLevel;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Burns its input into Salt, Sulfur and Mercury over a fuel fire and pushes them out through its sides. */
public class CalcinerBlockEntity extends BurnerMachineBlockEntity implements OwnedMachine {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    private static final int PUSH_INTERVAL = 10;

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), true, this::setChanged);
    private final TeamOwnership ownership = new TeamOwnership(this::setChanged);
    private int pushCooldown;

    public CalcinerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.CALCINER.get(), pos, state, 2);
    }

    // Owner

    @Override
    public Optional<UUID> owner() {
        return ownership.owner();
    }

    @Override
    public void setOwner(UUID player) {
        ownership.setOwner(level, player);
    }

    public void setTeam(UUID team) {
        ownership.setTeam(team);
    }

    /** The owning team, or {@code null} for a machine nobody placed. */
    public UUID team() {
        return ownership.team();
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    /** What a neighbour sees on {@code side}: by the Fluids layer, all three tanks, one of them, or nothing. */
    public IFluidHandler fluidHandler(Direction side) {
        return AlchemyMachines.sideHandler(this, tanks, side);
    }

    private static final List<SideTank> SIDE_TANKS = PrincipleTanks.sideTanks(false);

    /** Salt, Sulfur and Mercury, all three given out. */
    @Override
    public List<SideTank> sideTanks() {
        return SIDE_TANKS;
    }

    private Optional<CalcinationRecipe> currentRecipe() {
        ItemStack stack = inventory.getStackInSlot(SLOT_INPUT);
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ServusManufacturingRecipes.CALCINATION.get(), new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value);
    }

    @Override
    protected ResourceLocation machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected int fuelSlot() {
        return SLOT_FUEL;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return calcinable(stack);
    }

    /** True when a calcination recipe takes {@code stack}. */
    public boolean calcinable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.CALCINATION.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    /** Puts calcinable matter into the input, returning what did not fit. */
    public ItemStack insertInput(ItemStack stack) {
        return calcinable(stack) ? inventory.insertItem(SLOT_INPUT, stack, false) : stack;
    }

    /** Puts furnace fuel into the fuel slot, returning what did not fit. */
    public ItemStack insertFuel(ItemStack stack) {
        return stack.getBurnTime(null) > 0 ? inventory.insertItem(SLOT_FUEL, stack, false) : stack;
    }

    /** Takes the whole input out. */
    public ItemStack takeInput() {
        ItemStack taken = inventory.getStackInSlot(SLOT_INPUT).copy();
        inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
        return taken;
    }

    @Override
    protected boolean canWork() {
        tanks.setCapacity(stats().fluidCapacity());
        return currentRecipe().map(recipe -> tanks.contents().fits(recipe.output(), tanks.capacity())).orElse(false);
    }

    @Override
    protected void finishWork() {
        currentRecipe().ifPresent(recipe -> {
            ItemStack used = inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
            tanks.insert(recipe.output());
            if (level instanceof ServerLevel server) {
                Journal.recordItem(server, team(), "calcine", used, used.getCount());
                for (Principle principle : Principle.values()) {
                    Journal.recordPrinciple(server, team(), principle, recipe.output().get(principle));
                }
            }
        });
    }

    @Override
    protected boolean tick() {
        boolean working = super.tick();
        if (level != null && ++pushCooldown >= PUSH_INTERVAL) {
            pushCooldown = 0;
            AlchemyMachines.push(level, worldPosition, tanks, this);
        }
        return working;
    }

    @Override
    protected int workTicks() {
        return currentRecipe().map(recipe -> Math.max(1, recipe.time())).orElse(Integer.MAX_VALUE);
    }

    @Override
    protected int dataCount() {
        return AlchemyMachines.DATA_COUNT;
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_WORK_TICKS) {
            return currentRecipe().map(CalcinationRecipe::time).orElse(0);
        }
        int tank = AlchemyMachines.tankData(tanks, index);
        return tank >= 0 ? tank : super.getData(index);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Principles", tanks.save(registries));
        ownership.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks.load(tag.getCompound("Principles"));
        ownership.load(tag);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CalcinerMenu(containerId, playerInventory, this);
    }
}
