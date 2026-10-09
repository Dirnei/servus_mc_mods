package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import net.minecraft.server.level.ServerLevel;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Makes its selected recipe's result from piped principles and an optional item. */
public class CoagulatorBlockEntity extends AlchemyMachineBlockEntity {

    public static final int SLOT_INGREDIENT = 0;
    public static final int SLOT_OUTPUT = 1;
    /** Container data: index of the selected recipe in {@link #sortedRecipes} plus one, 0 for none. */
    public static final int DATA_SELECTED = AlchemyMachines.DATA_COUNT;
    public static final int DATA_COUNT = DATA_SELECTED + 1;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coagulator");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), false, this::setChanged);
    private ResourceLocation selected;

    public CoagulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.COAGULATOR.get(), pos, state, 2);
    }

    /** Every coagulation recipe in a fixed order, the same on server and client. */
    public static List<RecipeHolder<CoagulationRecipe>> sortedRecipes(RecipeManager manager) {
        return manager.getAllRecipesFor(ServusManufacturingRecipes.COAGULATION.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    public Optional<ResourceLocation> selected() {
        return Optional.ofNullable(selected);
    }

    /** Selects a recipe by id, or none with {@code null}. Progress starts over. */
    public void select(ResourceLocation recipe) {
        if (recipe == null ? selected != null : !recipe.equals(selected)) {
            selected = recipe;
            progress = 0;
            setChanged();
        }
    }

    /** Whether the owning team may select the recipe: it names no chapter, or the team finished it. */
    public boolean unlocked(CoagulationRecipe recipe) {
        if (recipe.chapter().isEmpty()) {
            return true;
        }
        return level instanceof ServerLevel server && Journal.isFinished(server.getServer(), team(), recipe.chapter().get().toString());
    }

    /** What a neighbour sees on {@code side}: the three tanks to fill, or nothing where the side takes nothing. */
    public IFluidHandler fluidHandler(Direction side) {
        return side == null || sideMode(side).allowsInsert() ? tanks : null;
    }

    @SuppressWarnings("unchecked")
    private Optional<RecipeHolder<CoagulationRecipe>> selectedHolder() {
        if (level == null || selected == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().byKey(selected)
                .filter(holder -> holder.value() instanceof CoagulationRecipe)
                .map(holder -> (RecipeHolder<CoagulationRecipe>) holder);
    }

    private Optional<CoagulationRecipe> currentRecipe() {
        return selectedHolder().map(RecipeHolder::value);
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
        return slot == SLOT_INGREDIENT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return currentRecipe().map(recipe -> recipe.acceptsItem(stack)).orElse(false);
    }

    @Override
    protected boolean canWork() {
        tanks.setCapacity(stats().fluidCapacity());
        return currentRecipe().map(recipe -> tanks.contents().covers(recipe.input())
                && recipe.itemReady(inventory.getStackInSlot(SLOT_INGREDIENT))
                && canInsertOutput(SLOT_OUTPUT, recipe.result())).orElse(false);
    }

    @Override
    protected void finishWork() {
        currentRecipe().ifPresent(recipe -> {
            tanks.take(recipe.input());
            recipe.ingredient().ifPresent(sized -> inventory.extractItem(SLOT_INGREDIENT, sized.count(), false));
            insertOutput(SLOT_OUTPUT, recipe.result().copy());
            if (level instanceof ServerLevel server) {
                Journal.recordItem(server, team(), "coagulate", recipe.result(), recipe.result().getCount());
            }
        });
    }

    @Override
    protected int workTicks() {
        return currentRecipe().map(recipe -> Math.max(1, recipe.time())).orElse(Integer.MAX_VALUE);
    }

    @Override
    protected int dataCount() {
        return DATA_COUNT;
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_WORK_TICKS) {
            return currentRecipe().map(CoagulationRecipe::time).orElse(0);
        }
        if (index == DATA_SELECTED) {
            if (level == null || selected == null) {
                return 0;
            }
            List<RecipeHolder<CoagulationRecipe>> recipes = sortedRecipes(level.getRecipeManager());
            for (int i = 0; i < recipes.size(); i++) {
                if (recipes.get(i).id().equals(selected)) {
                    return i + 1;
                }
            }
            return 0;
        }
        int tank = AlchemyMachines.tankData(tanks, index);
        return tank >= 0 ? tank : super.getData(index);
    }

    /** The Foundation production view: only the selected recipe, item ingredient in, result out. Principles come by pipe. */
    public ProductionProvider production() {
        return new ProductionProvider() {
            @Override
            public List<ProductionRecipe> recipes() {
                return selectedHolder().map(holder -> {
                    CoagulationRecipe recipe = holder.value();
                    List<ItemStack> inputs = recipe.ingredient()
                            .map(sized -> {
                                ItemStack[] items = sized.ingredient().getItems();
                                return items.length == 0 ? List.<ItemStack>of() : List.of(items[0].copyWithCount(sized.count()));
                            })
                            .orElse(List.of());
                    return List.of(new ProductionRecipe(holder.id(), inputs, List.of(recipe.result().copy()), Math.max(1, recipe.time())));
                }).orElse(List.of());
            }

            @Override
            public Optional<ResourceLocation> active() {
                return selectedHolder().map(RecipeHolder::id);
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Principles", tanks.save(registries));
        if (selected != null) {
            tag.putString("Recipe", selected.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks.load(tag.getCompound("Principles"));
        selected = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoagulatorMenu(containerId, playerInventory, this);
    }
}
