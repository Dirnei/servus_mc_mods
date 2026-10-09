package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.Optional;
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

/** Burns its input into Salt, Sulfur and Mercury and pushes them out through its sides. */
public class CalcinerBlockEntity extends PoweredMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    private static final int PUSH_INTERVAL = 10;

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), true, this::setChanged);
    private int pushCooldown;

    public CalcinerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.CALCINER.get(), pos, state, 1);
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    /** What a neighbour sees on {@code side}: the three tanks to drain, or nothing where the side gives nothing. */
    public IFluidHandler fluidHandler(Direction side) {
        return side == null || sideMode(side).allowsExtract() ? tanks : null;
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
    protected boolean isInputSlot(int slot) {
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        if (level == null) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.CALCINATION.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    @Override
    protected boolean canWork() {
        tanks.setCapacity(stats().fluidCapacity());
        return currentRecipe().map(recipe -> tanks.contents().fits(recipe.output(), tanks.capacity())).orElse(false);
    }

    @Override
    protected void finishWork() {
        currentRecipe().ifPresent(recipe -> {
            inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
            tanks.insert(recipe.output());
        });
    }

    @Override
    protected boolean tick() {
        boolean working = super.tick();
        if (level != null && ++pushCooldown >= PUSH_INTERVAL) {
            pushCooldown = 0;
            AlchemyMachines.push(level, worldPosition, tanks, this::sideMode);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks.load(tag.getCompound("Principles"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CalcinerMenu(containerId, playerInventory, this);
    }
}
