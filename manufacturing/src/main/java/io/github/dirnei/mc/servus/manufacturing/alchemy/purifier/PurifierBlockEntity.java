package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.MachineStatsManager;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.TeamOwned;
import io.github.dirnei.mc.servus.manufacturing.alchemy.TeamOwnership;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.SlotPort;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.TankPort;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
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
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;

/**
 * The Leaching Vat, controller of the Purifier build: washes Ash with water into liquid Salt over
 * time, with no power and no GUI, and keeps the residue. Water goes into the Vat, Salt comes out of
 * the Catch Basin and the residue out of the Residue Crock, so pipes and hoppers see one thing at each
 * block without any setting.
 */
public class PurifierBlockEntity extends BlockEntity implements TeamOwned {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "purifier");
    private static final int PUSH_INTERVAL = 10;

    /** Slot 0 holds what is washed, slot 1 what stays behind. */
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RESIDUE = 1;

    private final ItemSlots inventory = new ItemSlots(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT ? purifiable(stack) : true;
        }
    };
    private final FluidSlot water = new FluidSlot(defaultCapacity(), stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidSlot salt = new FluidSlot(defaultCapacity(), stack -> stack.getFluid().isSame(PrincipleFluids.fluid(Principle.SALT))) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final TeamOwnership ownership = new TeamOwnership(this);
    private final LegacyItemHandler itemPort = SlotPort.insertOnly(inventory, SLOT_INPUT, this::purifiable);
    private final LegacyItemHandler residuePort = SlotPort.extractOnly(inventory, SLOT_RESIDUE);
    private final LegacyFluidHandler fluidPort = TankPort.fillOnly(water);
    private final LegacyFluidHandler saltPort = TankPort.drainOnly(salt);
    private int progress;
    private int pushCooldown;
    /** The brine level last sent to the clients, in sixteenths with the fluid kind, -1 before the first send. */
    private int drawn = -1;
    /** The item count last sent to the clients, -1 before the first send. */
    private int drawnItems = -1;
    /** The residue count last sent to the clients, -1 before the first send. */
    private int drawnResidue = -1;
    /** On the client: how many items lie on the vat floor. */
    private int shownItems;
    /** On the client: how much residue lies in the Crock. */
    private int shownResidue;

    public PurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.PURIFIER.get(), pos, state);
    }

    private static int defaultCapacity() {
        return MachineDefaults.stats(ID.toString()).fluidCapacity();
    }

    private MachineStats stats() {
        return MachineStatsManager.INSTANCE.get(ID, MachineDefaults.stats(ID.toString()));
    }

    @Override
    public TeamOwnership ownership() {
        return ownership;
    }


    public ItemSlots inventory() {
        return inventory;
    }

    public FluidSlot water() {
        return water;
    }

    public FluidSlot salt() {
        return salt;
    }

    /** The Vat: takes items it can purify, gives none back. */
    public LegacyItemHandler itemPort() {
        return itemPort;
    }

    /** The Residue Crock: gives the residue, takes nothing. */
    public LegacyItemHandler residuePort() {
        return residuePort;
    }

    /** The Vat: takes only water, gives nothing. */
    public LegacyFluidHandler fluidPort() {
        return fluidPort;
    }

    /** The Catch Basin: gives only Salt, takes nothing. */
    public LegacyFluidHandler saltPort() {
        return saltPort;
    }

    /** Whether the Purifier stands formed; only then it works. */
    public boolean formed() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof PurifierBlock && state.getValue(PurifierBlock.FORMED);
    }

    /** Takes the residue out. */
    public ItemStack takeResidue() {
        ItemStack taken = inventory.getStackInSlot(SLOT_RESIDUE).copy();
        inventory.setStackInSlot(SLOT_RESIDUE, ItemStack.EMPTY);
        return taken;
    }

    /** True when a purification recipe takes {@code stack}. */
    public boolean purifiable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return ServusRecipes.all(level, ServusManufacturingRecipes.PURIFICATION.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    /** Puts purifiable items into the slot, returning what did not fit. */
    public ItemStack insert(ItemStack stack) {
        return purifiable(stack) ? inventory.insertItem(SLOT_INPUT, stack, false) : stack;
    }

    /** Takes the whole slot out. */
    public ItemStack takeItems() {
        ItemStack taken = inventory.getStackInSlot(SLOT_INPUT).copy();
        inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
        return taken;
    }

    private Optional<PurificationRecipe> currentRecipe() {
        ItemStack stack = inventory.getStackInSlot(SLOT_INPUT);
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, ServusManufacturingRecipes.PURIFICATION.get(), new SingleRecipeInput(stack))
                .map(RecipeHolder::value);
    }

    /** How far the current cycle is, 0 to 100. */
    public int progressPercent() {
        return currentRecipe().map(recipe -> Math.min(100, progress * 100 / Math.max(1, recipe.time()))).orElse(0);
    }


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
        return formed()
                && water.getFluidAmount() >= recipe.water()
                && salt.getCapacity() - salt.getFluidAmount() >= recipe.salt()
                && residueFits(recipe.residue());
    }

    private boolean residueFits(ItemStack residue) {
        if (residue.isEmpty()) {
            return true;
        }
        ItemStack held = inventory.getStackInSlot(SLOT_RESIDUE);
        return held.isEmpty() || ItemStack.isSameItemSameComponents(held, residue) && held.getCount() + residue.getCount() <= held.getMaxStackSize();
    }

    private void finish(PurificationRecipe recipe) {
        inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
        water.drain(recipe.water(), false);
        salt.fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), recipe.salt()), false);
        if (!recipe.residue().isEmpty()) {
            ItemStack held = inventory.getStackInSlot(SLOT_RESIDUE);
            inventory.setStackInSlot(SLOT_RESIDUE, held.isEmpty() ? recipe.residue().copy() : held.copyWithCount(held.getCount() + recipe.residue().getCount()));
        }
        if (level instanceof ServerLevel server) {
            Journal.recordEvent(server, team(), "purify", recipe.salt());
        }
    }

    private void pushSalt() {
        if (level == null || salt.isEmpty() || !formed()) {
            return;
        }
        BlockPos basin = PurifierStructure.partPos(level, worldPosition, PurifierLayout.Role.BASIN);
        for (Direction side : Direction.values()) {
            FluidView neighbour = FluidView.of(level.getCapability(Capabilities.Fluid.BLOCK, basin.relative(side), side.getOpposite()));
            if (neighbour == null) {
                continue;
            }
            int filled = neighbour.fill(salt.getFluid().copy(), false);
            if (filled > 0) {
                salt.drain(filled, false);
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

    /** On the client: how much residue lies in the Crock, for its heap. */
    public int shownResidue() {
        return shownResidue;
    }

    /** Sends the brine and the items to the clients when the drawn level, kind or item count changed. */
    private void syncBrine() {
        int now = brineKey();
        int items = inventory.getStackInSlot(SLOT_INPUT).getCount();
        int residue = inventory.getStackInSlot(SLOT_RESIDUE).getCount();
        if ((now != drawn || items != drawnItems || residue != drawnResidue) && level != null) {
            drawn = now;
            drawnItems = items;
            drawnResidue = residue;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private int brineKey() {
        int waterFill = water.isEmpty() ? -1 : water.getFluidAmount() * 16 / Math.max(1, water.getCapacity());
        int saltFill = salt.isEmpty() ? -1 : salt.getFluidAmount() * 16 / Math.max(1, salt.getCapacity());
        return (waterFill + 1) * 100 + saltFill + 1;
    }

    /** A broken Purifier drops its stock. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropContents();
        super.preRemoveSideEffects(pos, state);
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropContents(level, worldPosition, NonNullList.of(ItemStack.EMPTY, takeItems(), takeResidue()));
        }
    }


    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Salt", salt.writeToNBT(registries, new CompoundTag()));
        output.putInt("Progress", progress);
        ownership.save(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompoundOrEmpty("Inventory"));
        }
        water.readFromNBT(registries, tag.getCompoundOrEmpty("Water"));
        salt.readFromNBT(registries, tag.getCompoundOrEmpty("Salt"));
        progress = input.getIntOr("Progress", 0);
        ownership.load(tag);
        if (tag.contains("ShownItems")) {
            shownItems = input.getIntOr("ShownItems", 0);
            shownResidue = input.getIntOr("ShownResidue", 0);
        }
    }

    /** The clients draw the brine. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Salt", salt.writeToNBT(registries, new CompoundTag()));
        tag.putInt("ShownItems", inventory.getStackInSlot(SLOT_INPUT).getCount());
        tag.putInt("ShownResidue", inventory.getStackInSlot(SLOT_RESIDUE).getCount());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
