package io.github.dirnei.mc.servus.alchemy.forge;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMachineKinds;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.naming.MachineNameSlot;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.SingleInputRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Smelts every one item smelting recipe without fuel, one item per work cycle of the machine's
 * {@code work_ticks}. Its fire never goes out, so it counts as working for the lit state at all times.
 */
public class SalamanderForgeBlockEntity extends MachineBlockEntity implements NamedMachine {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "salamander_forge");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> smelting = RecipeManager.createCheck(RecipeType.SMELTING);
    private final SingleInputRecipes productionRecipes = new SingleInputRecipes();
    private final ProductionProvider production = new ProductionProvider() {
        @Override
        public List<ProductionRecipe> recipes() {
            if (!(level instanceof ServerLevel server)) {
                return List.of();
            }
            int ticks = workTicks();
            return productionRecipes.list(server, RecipeType.SMELTING, recipe -> ticks);
        }

        @Override
        public Optional<Identifier> active() {
            ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
            if (input.isEmpty()) {
                return Optional.empty();
            }
            return recipes().stream()
                    .filter(recipe -> ItemStack.isSameItemSameComponents(recipe.inputs().getFirst(), input))
                    .map(ProductionRecipe::id)
                    .findFirst();
        }
    };

    private final MachineNameSlot nameSlot = new MachineNameSlot(this, ServusAlchemyMachineKinds.SALAMANDER_FORGE);

    @Override
    public MachineNameSlot nameSlot() {
        return nameSlot;
    }

    public SalamanderForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.SALAMANDER_FORGE.get(), pos, state, 2);
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
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    /** What a furnace takes; the property set is synced, so the client's slot agrees with the server. */
    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return level != null && level.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(stack);
    }

    @Override
    protected boolean tick() {
        ItemStack result = result();
        if (result.isEmpty() || !canInsertOutput(SLOT_OUTPUT, result)) {
            resetProgress();
        } else if (stepProgress()) {
            inventory.extractItem(SLOT_INPUT, 1, false);
            insertOutput(SLOT_OUTPUT, result);
        }
        return true;
    }

    /** What one item of the input smelts into, or empty. */
    private ItemStack result() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty() || !(level instanceof ServerLevel server)) {
            return ItemStack.EMPTY;
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return smelting.getRecipeFor(recipeInput, server).map(holder -> holder.value().assemble(recipeInput)).orElse(ItemStack.EMPTY);
    }

    /** The eternal flame burns full in the screen. */
    @Override
    protected int getData(int index) {
        if (index == DATA_BURN_TIME || index == DATA_BURN_DURATION) {
            return 1;
        }
        return super.getData(index);
    }

    public ProductionProvider production() {
        return production;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SalamanderForgeMenu(containerId, playerInventory, this);
    }
}
