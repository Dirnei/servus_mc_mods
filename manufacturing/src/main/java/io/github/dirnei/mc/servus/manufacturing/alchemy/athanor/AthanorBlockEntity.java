package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.AlchemyMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.AlchemyMachines;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleTanks;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** The alchemist's furnace: burns principles for the craft of the item lying on it, as far as its structure allows. */
public class AthanorBlockEntity extends MachineBlockEntity implements OwnedMachine {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    /** Container data after the tanks: the structure's tier. */
    public static final int DATA_TIER = AlchemyMachines.DATA_COUNT;
    public static final int DATA_COUNT = DATA_TIER + 1;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    /** Ticks between tier checks while nothing else asks for one. */
    private static final int TIER_INTERVAL = 100;

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), false, this::setChanged);
    private UUID team;
    private int tier;
    private int tierCooldown;
    private ItemStack shownInput = ItemStack.EMPTY;

    public AthanorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.ATHANOR.get(), pos, state, 2);
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    public int tier() {
        return tier;
    }

    public String tierName() {
        return AthanorTiers.nameOf(AthanorTierData.INSTANCE.tiers(), tier);
    }

    /** What a neighbour sees on {@code side}: the tanks to fill on every side but the top, where the item lies. */
    public IFluidHandler fluidHandler(Direction side) {
        if (side == Direction.UP) {
            return null;
        }
        return side == null || sideMode(side).allowsInsert() ? tanks : null;
    }

    // Owner

    @Override
    public Optional<UUID> owner() {
        return Optional.ofNullable(team);
    }

    @Override
    public void setOwner(UUID player) {
        team = AlchemyMachineBlockEntity.resolveTeam(level, player);
        setChanged();
    }

    public void setTeam(UUID team) {
        this.team = team;
        setChanged();
    }

    public UUID team() {
        return team;
    }

    // Tier

    /** Reads the structure around the Athanor and stores the tier in the block state. A handful of block reads. */
    public void checkTier() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int found = AthanorTiers.tierOf(AthanorTierData.INSTANCE.tiers(), block -> {
            BlockState around = level.getBlockState(worldPosition.offset(block.dx(), block.dy(), block.dz()));
            return BuiltInRegistries.BLOCK.getKey(around.getBlock()).toString();
        });
        tierCooldown = 0;
        if (found != tier) {
            tier = found;
            setChanged();
        }
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(AthanorBlock.TIER) && state.getValue(AthanorBlock.TIER) != Math.min(found, 3)) {
            level.setBlock(worldPosition, state.setValue(AthanorBlock.TIER, Math.min(found, 3)), Block.UPDATE_CLIENTS);
        }
    }

    // Crafting

    private Optional<RecipeHolder<AthanorRecipe>> currentHolder() {
        ItemStack stack = inventory.getStackInSlot(SLOT_INPUT);
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ServusManufacturingRecipes.ATHANOR.get(), new SingleRecipeInput(stack), level);
    }

    private Optional<AthanorRecipe> currentRecipe() {
        return currentHolder().map(RecipeHolder::value);
    }

    private boolean canCraft(AthanorRecipe recipe) {
        return recipe.tier() <= tier
                && tanks.contents().covers(recipe.principles())
                && canInsertOutput(SLOT_OUTPUT, recipe.result());
    }

    @Override
    protected boolean tick() {
        tanks.setCapacity(stats().fluidCapacity());
        syncInputIfChanged();
        if (++tierCooldown >= TIER_INTERVAL) {
            checkTier();
        }
        Optional<AthanorRecipe> recipe = currentRecipe();
        if (recipe.isPresent() && progress == 0 && canCraft(recipe.get())) {
            // A craft is about to start: read the structure now rather than trust a tier up to five seconds old.
            checkTier();
        }
        if (recipe.isEmpty() || !canCraft(recipe.get())) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return false;
        }
        progress++;
        if (progress >= Math.max(1, recipe.get().time())) {
            progress = 0;
            finishCraft(recipe.get());
        }
        setChanged();
        return true;
    }

    private void finishCraft(AthanorRecipe recipe) {
        tanks.take(recipe.principles());
        inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
        insertOutput(SLOT_OUTPUT, recipe.result().copy());
        if (level instanceof ServerLevel server) {
            Journal.recordAthanor(server, team, recipe.result(), tierName());
        }
    }

    /** Sends the item lying on top to clients when it changes, for the renderer. */
    private void syncInputIfChanged() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (level != null && !ItemStack.isSameItemSameComponents(input, shownInput)) {
            shownInput = input.copy();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected int workTicks() {
        return currentRecipe().map(recipe -> Math.max(1, recipe.time())).orElse(0);
    }

    // Machine contract

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
        return level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.ATHANOR.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    @Override
    protected int dataCount() {
        return DATA_COUNT;
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_TIER) {
            return tier;
        }
        int tank = AlchemyMachines.tankData(tanks, index);
        return tank >= 0 ? tank : super.getData(index);
    }

    /** The Foundation production view: one recipe per craft the current tier allows; principles come by pipe. */
    public ProductionProvider production() {
        return new ProductionProvider() {
            @Override
            public List<ProductionRecipe> recipes() {
                if (level == null || level.isClientSide()) {
                    return List.of();
                }
                List<ProductionRecipe> recipes = new ArrayList<>();
                for (RecipeHolder<AthanorRecipe> holder : level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.ATHANOR.get())) {
                    AthanorRecipe recipe = holder.value();
                    ItemStack[] items = recipe.input().ingredient().getItems();
                    if (recipe.tier() > tier || items.length == 0) {
                        continue;
                    }
                    recipes.add(new ProductionRecipe(holder.id(), List.of(items[0].copyWithCount(recipe.input().count())),
                            List.of(recipe.result().copy()), Math.max(1, recipe.time())));
                }
                recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
                return recipes;
            }

            @Override
            public Optional<ResourceLocation> active() {
                return currentHolder().filter(holder -> holder.value().tier() <= tier).map(RecipeHolder::id);
            }
        };
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Principles", tanks.save(registries));
        tag.putInt("Tier", tier);
        if (team != null) {
            tag.putUUID("Team", team);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks.load(tag.getCompound("Principles"));
        tier = tag.getInt("Tier");
        team = tag.hasUUID("Team") ? tag.getUUID("Team") : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        return tag;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AthanorMenu(containerId, playerInventory, this);
    }
}
