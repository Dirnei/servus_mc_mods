package io.github.dirnei.mc.servus.storage.crafting;

import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.craft.IngredientGatherer;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Storage's own producer and the hands of the Crafting CPU: crafts the
 * pattern the CPU hands it as a job, without power. A job's results wait in the
 * outputs for the CPU; without a job it crafts nothing, automation inserts
 * nothing and leftover outputs are pushed into neighbours.
 */
public class CraftingStationBlockEntity extends BlockEntity implements MenuProvider, SidedBlockEntity {

    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 4;
    private static final int PUSH_INTERVAL = 8;

    private record Plan(int[] take, ItemStack result, NonNullList<ItemStack> remainders) {
    }

    private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler outputs = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** The pattern the CPU handed this station, the crafts left and the step that owns it. Not saved. */
    private EncodedPattern job;
    private int jobLeft;
    private Object jobOwner;
    private ItemStack clientJobResult = ItemStack.EMPTY;
    private int clientJobLeft;
    private int progress;
    private int pushTimer;

    private final SideConfig sides = SideConfig.machine();
    private final java.util.Map<Direction, IItemHandler> sideHandlers = new java.util.EnumMap<>(Direction.class);
    private final IItemHandler anySide = new SideHandler(null);

    /** All thirteen slots for automation: inputs take only used items, outputs only give, both only where the side allows it. */
    private final class SideHandler implements IItemHandler {
        private final Direction side;

        private SideHandler(Direction side) {
            this.side = side;
        }

        private SideMode mode() {
            return side == null ? SideMode.AUTO : sideMode(side);
        }

        @Override
        public int getSlots() {
            return INPUT_SLOTS + OUTPUT_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < INPUT_SLOTS ? inputs.getStackInSlot(slot) : outputs.getStackInSlot(slot - INPUT_SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= INPUT_SLOTS || !mode().allowsInsert() || !uses(stack)) {
                return stack;
            }
            return inputs.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < INPUT_SLOTS || !mode().allowsExtract() ? ItemStack.EMPTY : outputs.extractItem(slot - INPUT_SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS && mode().allowsInsert() && uses(stack);
        }
    }

    public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRAFTING_STATION.get(), pos, state);
        // The front is where the player stands: never an item side.
        sides.lock(RelativeSide.FRONT, SideMode.DISABLED);
    }

    public ItemStackHandler inputs() {
        return inputs;
    }

    public ItemStackHandler outputs() {
        return outputs;
    }

    /** The handler a neighbour on that side sees, or the unrestricted view for a null side. */
    public IItemHandler handlerFor(Direction side) {
        if (side == null) {
            return anySide;
        }
        return sideHandlers.computeIfAbsent(side, SideHandler::new);
    }

    // Sides

    @Override
    public SideConfig sideConfig() {
        return sides;
    }

    @Override
    public Direction facing() {
        return getBlockState().hasProperty(CraftingStationBlock.FACING) ? getBlockState().getValue(CraftingStationBlock.FACING) : Direction.NORTH;
    }

    @Override
    public SideMode cycleSide(RelativeSide side) {
        SideMode mode = sides.cycle(side);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        return mode;
    }

    public int workTicks() {
        return StorageStatsManager.INSTANCE.craftingStationTicks();
    }

    public int progress() {
        return progress;
    }

    /** Whether the current job uses the item, so automation may insert it. */
    public boolean uses(ItemStack stack) {
        return working() && job.required().containsKey(ItemKey.of(stack));
    }

    // Job

    /** The pattern of the current job, if any. */
    public Optional<EncodedPattern> job() {
        return Optional.ofNullable(job);
    }

    /** Crafts left on the current job. */
    public int jobLeft() {
        return job == null ? 0 : jobLeft;
    }

    /** Whether the station has a job with crafts left. */
    public boolean working() {
        return job != null && jobLeft > 0;
    }

    /**
     * Hands the station a job. Refused while another owner's job has crafts left.
     *
     * @return whether the station took the job
     */
    public boolean assign(EncodedPattern pattern, int crafts, Object owner) {
        if (working() && jobOwner != owner) {
            return false;
        }
        boolean same = job != null && jobOwner == owner && job.equals(pattern);
        job = pattern;
        jobLeft = Math.max(0, crafts);
        jobOwner = owner;
        if (!same) {
            progress = 0;
        }
        jobChanged();
        return true;
    }

    /** Ends the job of that owner; a job of another owner stays. */
    public void release(Object owner) {
        if (job != null && jobOwner == owner) {
            job = null;
            jobLeft = 0;
            jobOwner = null;
            progress = 0;
            jobChanged();
        }
    }

    private void jobChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** The job's result on the client, from the update tag. */
    public ItemStack clientJobResult() {
        return clientJobResult;
    }

    /** The job's crafts left on the client, from the update tag. */
    public int clientJobLeft() {
        return clientJobLeft;
    }

    // Production view

    /** No fixed recipes: the station crafts the patterns the CPU hands it, and reports the current one as active. */
    public ProductionProvider production() {
        return new ProductionProvider() {
            @Override
            public List<ProductionRecipe> recipes() {
                return List.of();
            }

            @Override
            public Optional<ResourceLocation> active() {
                return working() ? Optional.of(job.recipe()) : Optional.empty();
            }
        };
    }

    // Crafting

    private Optional<Plan> plan(EncodedPattern recipe) {
        if (level == null) {
            return Optional.empty();
        }
        Map<ItemKey, Integer> required = recipe.required();
        if (required.isEmpty()) {
            return Optional.empty();
        }
        List<ItemKey> keys = new ArrayList<>();
        int[] counts = new int[INPUT_SLOTS];
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputs.getStackInSlot(i);
            keys.add(stack.isEmpty() ? null : ItemKey.of(stack));
            counts[i] = stack.getCount();
        }
        Optional<int[]> take = IngredientGatherer.gather(keys, counts, required);
        if (take.isEmpty()) {
            return Optional.empty();
        }
        Optional<RecipeHolder<CraftingRecipe>> holder = resolve(level, recipe.recipe());
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        CraftingInput input = CraftingInput.of(3, 3, recipe.template());
        CraftingRecipe crafting = holder.get().value();
        if (!crafting.matches(input, level)) {
            return Optional.empty();
        }
        ItemStack result = crafting.assemble(input, level.registryAccess());
        NonNullList<ItemStack> remainders = crafting.getRemainingItems(input);
        if (!fits(result, remainders)) {
            return Optional.empty();
        }
        return Optional.of(new Plan(take.get(), result, remainders));
    }

    private boolean fits(ItemStack result, NonNullList<ItemStack> remainders) {
        ItemStackHandler scratch = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            scratch.setStackInSlot(i, outputs.getStackInSlot(i).copy());
        }
        if (!ItemHandlerHelper.insertItem(scratch, result.copy(), false).isEmpty()) {
            return false;
        }
        for (ItemStack remainder : remainders) {
            if (!remainder.isEmpty() && !ItemHandlerHelper.insertItem(scratch, remainder.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** @return whether the station is working */
    private boolean tick() {
        if (!working() || plan(job).isEmpty()) {
            progress = 0;
            return false;
        }
        progress++;
        if (progress >= workTicks()) {
            progress = 0;
            finish();
        }
        setChanged();
        return true;
    }

    private void finish() {
        Optional<Plan> plan = plan(job);
        if (plan.isEmpty()) {
            return;
        }
        int[] take = plan.get().take();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (take[i] > 0) {
                inputs.extractItem(i, take[i], false);
            }
        }
        ItemHandlerHelper.insertItem(outputs, plan.get().result(), false);
        for (ItemStack remainder : plan.get().remainders()) {
            if (!remainder.isEmpty()) {
                ItemHandlerHelper.insertItem(outputs, remainder.copy(), false);
            }
        }
        jobLeft--;
        jobChanged();
    }

    private static Optional<RecipeHolder<CraftingRecipe>> resolve(Level level, ResourceLocation id) {
        return level.getRecipeManager().byKey(id)
                .filter(holder -> holder.value() instanceof CraftingRecipe)
                .map(holder -> {
                    @SuppressWarnings("unchecked")
                    RecipeHolder<CraftingRecipe> crafting = (RecipeHolder<CraftingRecipe>) holder;
                    return crafting;
                });
    }

    private void pushOutputs() {
        if (++pushTimer < PUSH_INTERVAL || level == null) {
            return;
        }
        pushTimer = 0;
        for (Direction direction : Direction.values()) {
            SideMode mode = sideMode(direction);
            if (!mode.pushes() && !mode.pulls()) {
                continue;
            }
            IItemHandler neighbour = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (neighbour == null) {
                continue;
            }
            if (mode.pulls()) {
                pullFrom(neighbour);
            }
            // A job's outputs are collected by its CPU; pushed away they would never be counted.
            if (!mode.pushes() || job != null) {
                continue;
            }
            for (int i = 0; i < OUTPUT_SLOTS; i++) {
                ItemStack stack = outputs.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack left = ItemHandlerHelper.insertItem(neighbour, stack.copy(), false);
                    if (left.getCount() != stack.getCount()) {
                        outputs.setStackInSlot(i, left);
                    }
                }
            }
        }
    }

    /** Takes one stack the job uses from the neighbour into the inputs. */
    private void pullFrom(IItemHandler neighbour) {
        for (int from = 0; from < neighbour.getSlots(); from++) {
            ItemStack offered = neighbour.extractItem(from, 64, true);
            if (offered.isEmpty() || !uses(offered)) {
                continue;
            }
            int room = offered.getCount() - ItemHandlerHelper.insertItem(inputs, offered, true).getCount();
            if (room <= 0) {
                continue;
            }
            ItemStack taken = neighbour.extractItem(from, room, false);
            if (!taken.isEmpty()) {
                ItemHandlerHelper.insertItem(inputs, taken, false);
                return;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CraftingStationBlockEntity station) {
        boolean working = station.tick();
        station.pushOutputs();
        if (state.hasProperty(CraftingStationBlock.LIT) && state.getValue(CraftingStationBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(CraftingStationBlock.LIT, working), Block.UPDATE_CLIENTS);
        }
    }

    /** Drops every item in the inputs and outputs. */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStackHandler handler : List.of(inputs, outputs)) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraftingStationMenu(containerId, playerInventory, this);
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inputs", inputs.serializeNBT(registries));
        tag.put("Outputs", outputs.serializeNBT(registries));
        tag.putByteArray("Sides", sides.toBytes());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inputs")) {
            inputs.deserializeNBT(registries, tag.getCompound("Inputs"));
        }
        if (tag.contains("Outputs")) {
            outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
        }
        if (tag.contains("JobResult")) {
            CompoundTag result = tag.getCompound("JobResult");
            clientJobResult = result.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, result);
            clientJobLeft = tag.getInt("JobLeft");
        }
        if (tag.contains("Sides")) {
            sides.fromBytes(tag.getByteArray("Sides"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ItemStack result = working() ? job.result() : ItemStack.EMPTY;
        tag.put("JobResult", result.isEmpty() ? new CompoundTag() : result.save(registries));
        tag.putInt("JobLeft", jobLeft());
        tag.putByteArray("Sides", sides.toBytes());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
