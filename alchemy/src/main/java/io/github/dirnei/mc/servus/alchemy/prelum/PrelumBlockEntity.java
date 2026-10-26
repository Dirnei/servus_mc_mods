package io.github.dirnei.mc.servus.alchemy.prelum;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMachineKinds;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.naming.MachineNameSlot;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.TeamOwned;
import io.github.dirnei.mc.servus.alchemy.TeamOwnership;
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
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Runs the pressing recipe that matches its input once the input holds the recipe's count, one result per pressing,
 * without fuel. Each pressed result counts as a {@code press} journal event for the owning team.
 */
public class PrelumBlockEntity extends MachineBlockEntity implements TeamOwned, NamedMachine {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "prelum");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private final TeamOwnership ownership = new TeamOwnership(this);
    private final ProductionProvider production = new ProductionProvider() {
        @Override
        public List<ProductionRecipe> recipes() {
            if (level == null) {
                return List.of();
            }
            return ServusRecipes.all(level, ServusAlchemyRecipes.PRESSING.get()).stream()
                    .map(PrelumBlockEntity::productionRecipe)
                    .flatMap(Optional::stream)
                    .toList();
        }

        @Override
        public Optional<Identifier> active() {
            return currentRecipe().map(holder -> holder.id().identifier());
        }
    };

    private final MachineNameSlot nameSlot = new MachineNameSlot(this, ServusAlchemyMachineKinds.PRELUM);

    @Override
    public MachineNameSlot nameSlot() {
        return nameSlot;
    }

    public PrelumBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.PRELUM.get(), pos, state, 2);
    }

    /** One pressing recipe as the data line sees it: its count of the first item it accepts in, its result out. */
    private static Optional<ProductionRecipe> productionRecipe(RecipeHolder<PressingRecipe> holder) {
        PressingRecipe recipe = holder.value();
        return ServusRecipes.items(recipe.input()).stream().findFirst()
                .map(input -> new ProductionRecipe(holder.id().identifier(), List.of(input), List.of(recipe.result()), recipe.time()));
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
    public TeamOwnership ownership() {
        return ownership;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    /** What some pressing recipe takes; the recipes are synced, so the client's slot agrees with the server. */
    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return pressable(stack);
    }

    /** Whether some pressing recipe takes {@code stack}, regardless of count. */
    public boolean pressable(ItemStack stack) {
        return level != null && ServusRecipes.all(level, ServusAlchemyRecipes.PRESSING.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    /** The pressing recipe the input holds enough for, if any. */
    private Optional<RecipeHolder<PressingRecipe>> currentRecipe() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, ServusAlchemyRecipes.PRESSING.get(), new SingleRecipeInput(input));
    }

    @Override
    protected boolean tick() {
        Optional<PressingRecipe> recipe = currentRecipe().map(RecipeHolder::value)
                .filter(found -> canInsertOutput(SLOT_OUTPUT, found.result()));
        if (recipe.isEmpty()) {
            resetProgress();
            return false;
        }
        progress++;
        setChanged();
        if (progress >= Math.max(1, recipe.get().time())) {
            progress = 0;
            press(recipe.get());
        }
        return true;
    }

    private void press(PressingRecipe recipe) {
        ItemStack result = recipe.result();
        inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
        insertOutput(SLOT_OUTPUT, result.copy());
        if (level instanceof ServerLevel server) {
            Journal.recordItem(server, team(), "press", result, result.getCount());
        }
    }

    /** The arrow fills at the pace of the recipe that runs, not the default press time. */
    @Override
    protected int getData(int index) {
        if (index == DATA_WORK_TICKS) {
            return currentRecipe().map(holder -> holder.value().time()).orElse(workTicks());
        }
        return super.getData(index);
    }

    public ProductionProvider production() {
        return production;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        ownership.save(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownership.load(ValueIo.whole(input));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PrelumMenu(containerId, playerInventory, this);
    }
}
