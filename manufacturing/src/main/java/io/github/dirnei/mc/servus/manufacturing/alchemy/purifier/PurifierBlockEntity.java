package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.MachineStatsManager;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.TeamOwnership;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Purifier's vat: leaches Ash with water into liquid Salt over time, with no power and no
 * GUI. Water goes in and Salt comes out through the same fluid handler, so the Fluid Pipe sees
 * it as a sink of water and a source of Salt without any setting.
 */
public class PurifierBlockEntity extends BlockEntity {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "purifier");
    private static final int PUSH_INTERVAL = 10;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return purifiable(stack);
        }
    };
    private final FluidTank water = new FluidTank(defaultCapacity(), stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank salt = new FluidTank(defaultCapacity(), stack -> stack.getFluid().isSame(PrincipleFluids.fluid(Principle.SALT))) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final TeamOwnership ownership = new TeamOwnership(this::setChanged);
    private final IItemHandler itemPort = new ItemPort();
    private final IFluidHandler fluidPort = new FluidPort();
    private int progress;
    private int pushCooldown;
    /** The brine level last sent to the clients, in sixteenths with the fluid kind, -1 before the first send. */
    private int drawn = -1;
    /** The item count last sent to the clients, -1 before the first send. */
    private int drawnItems = -1;
    /** On the client: how many items lie on the vat floor. */
    private int shownItems;

    public PurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.PURIFIER.get(), pos, state);
    }

    private static int defaultCapacity() {
        return MachineDefaults.stats(ID.toString()).fluidCapacity();
    }

    private MachineStats stats() {
        return MachineStatsManager.INSTANCE.get(ID, MachineDefaults.stats(ID.toString()));
    }

    // Owner

    public Optional<UUID> owner() {
        return ownership.owner();
    }

    public void setOwner(UUID player) {
        ownership.setOwner(level, player);
    }

    public void setTeam(UUID team) {
        ownership.setTeam(team);
    }

    // Contents

    public ItemStackHandler inventory() {
        return inventory;
    }

    public FluidTank water() {
        return water;
    }

    public FluidTank salt() {
        return salt;
    }

    /** Takes items it can purify, gives none back. */
    public IItemHandler itemPort() {
        return itemPort;
    }

    /** Takes only water, gives only Salt. */
    public IFluidHandler fluidPort() {
        return fluidPort;
    }

    /** True when a purification recipe takes {@code stack}. */
    public boolean purifiable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.PURIFICATION.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    /** Puts purifiable items into the slot, returning what did not fit. */
    public ItemStack insert(ItemStack stack) {
        return purifiable(stack) ? inventory.insertItem(0, stack, false) : stack;
    }

    /** Takes the whole slot out. */
    public ItemStack takeItems() {
        ItemStack taken = inventory.getStackInSlot(0).copy();
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        return taken;
    }

    private Optional<PurificationRecipe> currentRecipe() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ServusManufacturingRecipes.PURIFICATION.get(), new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value);
    }

    /** How far the current cycle is, 0 to 100. */
    public int progressPercent() {
        return currentRecipe().map(recipe -> Math.min(100, progress * 100 / Math.max(1, recipe.time()))).orElse(0);
    }

    // Work

    public static void serverTick(Level level, BlockPos pos, BlockState state, PurifierBlockEntity purifier) {
        purifier.tick();
    }

    private void tick() {
        int capacity = stats().fluidCapacity();
        water.setCapacity(capacity);
        salt.setCapacity(capacity);
        Optional<PurificationRecipe> recipe = currentRecipe().filter(this::canRun);
        if (recipe.isEmpty()) {
            progress = 0;
        } else if (++progress >= Math.max(1, recipe.get().time())) {
            progress = 0;
            finish(recipe.get());
        }
        if (++pushCooldown >= PUSH_INTERVAL) {
            pushCooldown = 0;
            pushSalt();
            syncBrine();
        }
    }

    private boolean canRun(PurificationRecipe recipe) {
        return water.getFluidAmount() >= recipe.water() && salt.getCapacity() - salt.getFluidAmount() >= recipe.salt();
    }

    private void finish(PurificationRecipe recipe) {
        inventory.extractItem(0, recipe.input().count(), false);
        water.drain(recipe.water(), IFluidHandler.FluidAction.EXECUTE);
        salt.fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), recipe.salt()), IFluidHandler.FluidAction.EXECUTE);
        if (level instanceof ServerLevel server) {
            Journal.recordEvent(server, ownership.team(), "purify", recipe.salt());
        }
    }

    /** Fills touching fluid handlers that take Salt. */
    private void pushSalt() {
        if (level == null || salt.isEmpty()) {
            return;
        }
        for (Direction side : Direction.values()) {
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
            if (neighbour == null) {
                continue;
            }
            int filled = neighbour.fill(salt.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                salt.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
            if (salt.isEmpty()) {
                return;
            }
        }
    }

    /** On the client: how many items lie on the vat floor, for the ash layer. */
    public int shownItems() {
        return shownItems;
    }

    /** Sends the brine and the items to the clients when the drawn level, kind or item count changed. */
    private void syncBrine() {
        int now = brineKey();
        int items = inventory.getStackInSlot(0).getCount();
        if ((now != drawn || items != drawnItems) && level != null) {
            drawn = now;
            drawnItems = items;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private int brineKey() {
        FluidTank shown = salt.isEmpty() ? water : salt;
        if (shown.isEmpty()) {
            return -1;
        }
        int level = shown.getFluidAmount() * 16 / Math.max(1, shown.getCapacity());
        return (shown == salt ? 100 : 0) + level;
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropContents(level, worldPosition, NonNullList.of(ItemStack.EMPTY, takeItems()));
        }
    }

    // Saving and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Salt", salt.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        ownership.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        water.readFromNBT(registries, tag.getCompound("Water"));
        salt.readFromNBT(registries, tag.getCompound("Salt"));
        progress = tag.getInt("Progress");
        ownership.load(tag);
        if (tag.contains("ShownItems")) {
            shownItems = tag.getInt("ShownItems");
        }
    }

    /** The clients draw the brine. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Salt", salt.writeToNBT(registries, new CompoundTag()));
        tag.putInt("ShownItems", inventory.getStackInSlot(0).getCount());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** One slot that takes what a purification recipe accepts and never gives back. */
    private final class ItemPort implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return purifiable(stack) ? inventory.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return purifiable(stack);
        }
    }

    /** Water in, Salt out: tank 0 is the water, tank 1 the Salt. */
    private final class FluidPort implements IFluidHandler {

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? water.getFluid() : salt.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? water.getCapacity() : salt.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && water.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return water.isFluidValid(resource) ? water.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return salt.isFluidValid(resource) ? salt.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return salt.drain(maxDrain, action);
        }
    }
}
