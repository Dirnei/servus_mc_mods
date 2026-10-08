package io.github.dirnei.mc.servus.manufacturing.machine.crusher;

import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineDefaults;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.menu.CrusherMenu;
import io.github.dirnei.mc.servus.manufacturing.recipe.CrushingRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

/** Runs the crushing recipe of its input: ore to grit and similar. */
public class CrusherBlockEntity extends PoweredMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "crusher");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.CRUSHER.get(), pos, state, 2);
    }

    private Optional<RecipeHolder<CrushingRecipe>> recipeFor(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ServusManufacturingRecipes.CRUSHING.get(), new SingleRecipeInput(stack), level);
    }

    private Optional<CrushingRecipe> currentRecipe() {
        return recipeFor(inventory.getStackInSlot(SLOT_INPUT)).map(RecipeHolder::value);
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
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        if (level == null) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.CRUSHING.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    @Override
    protected boolean canWork() {
        return currentRecipe().map(recipe -> canInsertOutput(SLOT_OUTPUT, recipe.result())).orElse(false);
    }

    @Override
    protected void finishWork() {
        currentRecipe().ifPresent(recipe -> {
            inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
            insertOutput(SLOT_OUTPUT, recipe.result().copy());
        });
    }

    @Override
    protected int workTicks() {
        return currentRecipe().map(recipe -> Math.max(1, recipe.time())).orElse(Integer.MAX_VALUE);
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_WORK_TICKS) {
            return currentRecipe().map(CrushingRecipe::time).orElse(0);
        }
        return super.getData(index);
    }

    private RecipeManager cachedManager;
    private List<ProductionRecipe> cachedRecipes = List.of();

    /** The Foundation production view: one recipe per input item of every crushing recipe, with its count and time. */
    public ProductionProvider production() {
        return new ProductionProvider() {
            @Override
            public List<ProductionRecipe> recipes() {
                return crushingRecipes();
            }

            @Override
            public Optional<ResourceLocation> active() {
                ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
                if (input.isEmpty()) {
                    return Optional.empty();
                }
                for (ProductionRecipe recipe : crushingRecipes()) {
                    if (ItemStack.isSameItemSameComponents(recipe.inputs().get(0), input)) {
                        return Optional.of(recipe.id());
                    }
                }
                return Optional.empty();
            }
        };
    }

    private List<ProductionRecipe> crushingRecipes() {
        if (level == null || level.isClientSide()) {
            return List.of();
        }
        RecipeManager manager = level.getRecipeManager();
        if (manager != cachedManager) {
            cachedManager = manager;
            List<ProductionRecipe> recipes = new ArrayList<>();
            Set<ResourceLocation> ids = new HashSet<>();
            for (RecipeHolder<CrushingRecipe> holder : manager.getAllRecipesFor(ServusManufacturingRecipes.CRUSHING.get())) {
                CrushingRecipe recipe = holder.value();
                if (recipe.result().isEmpty()) {
                    continue;
                }
                for (ItemStack input : recipe.input().ingredient().getItems()) {
                    ResourceLocation item = BuiltInRegistries.ITEM.getKey(input.getItem());
                    ResourceLocation id = holder.id().withSuffix("/" + item.getNamespace() + "/" + item.getPath());
                    if (ids.add(id)) {
                        recipes.add(new ProductionRecipe(id, List.of(input.copyWithCount(recipe.input().count())),
                                List.of(recipe.result().copy()), Math.max(1, recipe.time())));
                    }
                }
            }
            recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
            cachedRecipes = List.copyOf(recipes);
        }
        return cachedRecipes;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrusherMenu(containerId, playerInventory, this);
    }
}
