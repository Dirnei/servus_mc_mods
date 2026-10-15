package io.github.dirnei.mc.servus.alchemy;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Makes its selected recipe's result from piped principles and an optional item. It needs no power:
 * the joined principles only take the recipe's time to cool and set.
 */
public class CoagulatorBlockEntity extends MachineBlockEntity implements TeamOwned {

    public static final int SLOT_INGREDIENT = 0;
    public static final int SLOT_OUTPUT = 1;
    /** Container data: index of the selected recipe in {@link #sortedRecipes} plus one, 0 for none. */
    public static final int DATA_SELECTED = AlchemyMachines.DATA_COUNT;
    public static final int DATA_COUNT = DATA_SELECTED + 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "coagulator");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    private static final int SYNC_INTERVAL = 10;

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), false, this::setChanged);
    private final TeamOwnership ownership = new TeamOwnership(this);
    private final LegacyItemHandler itemPort = new ItemPort();
    private final DrawnFill drawnFill = new DrawnFill();
    private Identifier selected;
    private int syncCooldown;

    public CoagulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.COAGULATOR.get(), pos, state, 2);
        AlchemyMachines.closeAllSides(this);
    }

    @Override
    public TeamOwnership ownership() {
        return ownership;
    }

    /** Every coagulation recipe in a fixed order, the same on server and client. */
    public static List<RecipeHolder<CoagulationRecipe>> sortedRecipes(Level level) {
        return ServusRecipes.all(level, ServusAlchemyRecipes.COAGULATION.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .toList();
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    public Optional<Identifier> selected() {
        return Optional.ofNullable(selected);
    }

    /** Selects a recipe by id, or none with {@code null}. Progress starts over. */
    public void select(Identifier recipe) {
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

    /** Whether the build around this Coagulator is formed; only then it works. */
    public boolean formed() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof CoagulatorBlock && state.getValue(CoagulatorBlock.FORMED);
    }

    /** The casting table: takes the ingredient into slot 0, gives the products from slot 1. */
    public LegacyItemHandler itemPort() {
        return itemPort;
    }

    /** Sends the tanks to the clients when a Vat's drawn level changed by a sixteenth. */
    private void syncFill() {
        if (drawnFill.update(tanks) && level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Slot 0 takes what the selected recipe needs and gives nothing; slot 1 gives the products and takes nothing. */
    private final class ItemPort extends LegacyItemHandler {

        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot == 0 ? SLOT_INGREDIENT : SLOT_OUTPUT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 && acceptsInput(SLOT_INGREDIENT, stack) ? inventory.insertItem(SLOT_INGREDIENT, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? inventory.extractItem(SLOT_OUTPUT, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot == 0 ? SLOT_INGREDIENT : SLOT_OUTPUT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && acceptsInput(SLOT_INGREDIENT, stack);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<RecipeHolder<CoagulationRecipe>> selectedHolder() {
        if (level == null || selected == null) {
            return Optional.empty();
        }
        return ServusRecipes.byId(level, selected)
                .filter(holder -> holder.value() instanceof CoagulationRecipe)
                .map(holder -> (RecipeHolder<CoagulationRecipe>) holder);
    }

    private Optional<CoagulationRecipe> currentRecipe() {
        return selectedHolder().map(RecipeHolder::value);
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

    private boolean canWork() {
        tanks.setCapacity(stats().fluidCapacity());
        if (++syncCooldown >= SYNC_INTERVAL) {
            syncCooldown = 0;
            syncFill();
        }
        return formed() && currentRecipe().map(recipe -> tanks.contents().covers(recipe.input())
                && recipe.itemReady(inventory.getStackInSlot(SLOT_INGREDIENT))
                && canInsertOutput(SLOT_OUTPUT, recipe.result())).orElse(false);
    }

    @Override
    protected boolean tick() {
        if (!canWork()) {
            resetProgress();
            return false;
        }
        if (stepProgress()) {
            finishWork();
        }
        return true;
    }

    private void finishWork() {
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
            List<RecipeHolder<CoagulationRecipe>> recipes = sortedRecipes(level);
            for (int i = 0; i < recipes.size(); i++) {
                if (recipes.get(i).id().identifier().equals(selected)) {
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
                if (!formed()) {
                    return List.of();
                }
                return selectedHolder().map(holder -> {
                    CoagulationRecipe recipe = holder.value();
                    List<ItemStack> inputs = recipe.ingredient()
                            .map(sized -> {
                                ItemStack item = ServusRecipes.firstItem(sized.ingredient());
                                return item.isEmpty() ? List.<ItemStack>of() : List.of(item.copyWithCount(sized.count()));
                            })
                            .orElse(List.of());
                    return List.of(new ProductionRecipe(holder.id().identifier(), inputs, List.of(recipe.result().copy()), Math.max(1, recipe.time())));
                }).orElse(List.of());
            }

            @Override
            public Optional<Identifier> active() {
                return selectedHolder().map(holder -> holder.id().identifier());
            }
        };
    }

    /** The clients draw the principles in the Vats. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Principles", tanks.save(registries));
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Principles", tanks.save(registries));
        if (selected != null) {
            output.putString("Recipe", selected.toString());
        }
        ownership.save(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        tanks.load(tag.getCompoundOrEmpty("Principles"));
        selected = tag.contains("Recipe") ? Identifier.tryParse(input.getStringOr("Recipe", "")) : null;
        ownership.load(tag);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoagulatorMenu(containerId, playerInventory, this);
    }
}
