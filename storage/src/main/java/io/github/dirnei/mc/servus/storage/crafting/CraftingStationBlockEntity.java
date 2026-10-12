package io.github.dirnei.mc.servus.storage.crafting;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.data.DataCarrier;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.production.ProductionView;

import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.craft.IngredientGatherer;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.StepRef;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentMap;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * Storage's own producer and the hands of the Crafting CPU: crafts the
 * pattern the CPU hands it as a job, without power. A job's results wait in the
 * outputs for the CPU; without a job it crafts nothing, automation inserts
 * nothing and leftover outputs are pushed into neighbours.
 */
public class CraftingStationBlockEntity extends BlockEntity implements WoodHolder, MenuProvider, SidedBlockEntity,
        io.github.dirnei.mc.servus.storage.network.NoStock {

    /** Carries the data network to touching data nodes, like a piece of cable. */
    private final DataCarrier carrier = new DataCarrier(this);

    private final WoodSlot woodSlot = new WoodSlot(this);

    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 4;
    private static final int PUSH_INTERVAL = 8;

    private record Plan(int[] take, ItemStack result, NonNullList<ItemStack> remainders) {
    }

    private final ItemSlots inputs = new ItemSlots(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemSlots outputs = new ItemSlots(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** The pattern the CPU handed this station, the crafts left and the step that owns it. Not saved. */
    private EncodedPattern job;
    private int jobLeft;
    private Object jobOwner;
    /** How often a station checks that its job's step still exists. */
    private static final int OWNER_CHECK_TICKS = 100;
    private ItemStack clientJobResult = ItemStack.EMPTY;
    private int clientJobLeft;
    private int progress;
    private int ownerCheck;
    private int lampTimer;
    /** How often the online lamp is refreshed. */
    private static final int LAMP_TICKS = 20;
    private int pushTimer;

    private final SideConfig sides = SideConfig.machine();
    private final java.util.Map<Direction, LegacyItemHandler> sideHandlers = new java.util.EnumMap<>(Direction.class);
    private final LegacyItemHandler anySide = new SideHandler(null);

    /** All thirteen slots for automation: inputs take only used items, outputs only give, both only where the side allows it. */
    private final class SideHandler extends LegacyItemHandler {
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

    public ItemSlots inputs() {
        return inputs;
    }

    public ItemSlots outputs() {
        return outputs;
    }

    /** The handler a neighbour on that side sees, or the unrestricted view for a null side. */
    public ResourceHandler<ItemResource> handlerFor(Direction side) {
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

    /** Whether any output slot still holds items. */
    public boolean hasOutputs() {
        for (int slot = 0; slot < outputs.getSlots(); slot++) {
            if (!outputs.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Who the current or last job belongs to, null when none. */
    public Object jobOwner() {
        return jobOwner;
    }

    /**
     * Whether the station is closed to a job of that owner: outputs that are not
     * collected yet belong to the current job's owner only; leftovers without a
     * job close it to everyone until they are taken out.
     */
    public boolean blockedFor(Object owner) {
        return hasOutputs() && (jobOwner == null || !Objects.equals(jobOwner, owner));
    }

    /**
     * Hands the station a job. Refused while another owner's job has crafts left,
     * and while outputs wait that are not this owner's.
     *
     * @return whether the station took the job
     */
    public boolean assign(EncodedPattern pattern, int crafts, Object owner) {
        if (working() && !Objects.equals(jobOwner, owner)) {
            return false;
        }
        if (blockedFor(owner)) {
            return false;
        }
        boolean same = job != null && Objects.equals(jobOwner, owner) && job.equals(pattern);
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
        if (job != null && Objects.equals(jobOwner, owner)) {
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
            public Optional<Identifier> active() {
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
        ItemStack result = crafting.assemble(input);
        NonNullList<ItemStack> remainders = crafting.getRemainingItems(input);
        if (!fits(result, remainders)) {
            return Optional.empty();
        }
        return Optional.of(new Plan(take.get(), result, remainders));
    }

    private boolean fits(ItemStack result, NonNullList<ItemStack> remainders) {
        ItemSlots scratch = new ItemSlots(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            scratch.setStackInSlot(i, outputs.getStackInSlot(i).copy());
        }
        if (!scratch.insertItem(result.copy(), false).isEmpty()) {
            return false;
        }
        for (ItemStack remainder : remainders) {
            if (!remainder.isEmpty() && !scratch.insertItem(remainder.copy(), false).isEmpty()) {
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
        outputs.insertItem(plan.get().result(), false);
        for (ItemStack remainder : plan.get().remainders()) {
            if (!remainder.isEmpty()) {
                outputs.insertItem(remainder.copy(), false);
            }
        }
        jobLeft--;
        jobChanged();
    }

    private static Optional<RecipeHolder<CraftingRecipe>> resolve(Level level, Identifier id) {
        return ServusRecipes.crafting(level, id);
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
            ItemView neighbour = ItemView.of(level.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(direction), direction.getOpposite()));
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
                    ItemStack left = neighbour.insertItem(stack.copy(), false);
                    if (left.getCount() != stack.getCount()) {
                        outputs.setStackInSlot(i, left);
                    }
                }
            }
        }
    }

    /** Takes one stack the job uses from the neighbour into the inputs. */
    private void pullFrom(ItemView neighbour) {
        for (int from = 0; from < neighbour.getSlots(); from++) {
            ItemStack offered = neighbour.extractItem(from, 64, true);
            if (offered.isEmpty() || !uses(offered)) {
                continue;
            }
            int room = offered.getCount() - inputs.insertItem(offered, true).getCount();
            if (room <= 0) {
                continue;
            }
            ItemStack taken = neighbour.extractItem(from, room, false);
            if (!taken.isEmpty()) {
                inputs.insertItem(taken, false);
                return;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CraftingStationBlockEntity station) {
        if (++station.ownerCheck >= OWNER_CHECK_TICKS) {
            station.ownerCheck = 0;
            station.dropOrphanedJob((ServerLevel) level);
        }
        boolean working = station.tick();
        station.pushOutputs();
        BlockState next = state;
        if (next.hasProperty(CraftingStationBlock.LIT) && next.getValue(CraftingStationBlock.LIT) != working) {
            next = next.setValue(CraftingStationBlock.LIT, working);
        }
        if (++station.lampTimer >= LAMP_TICKS && next.hasProperty(StorageNodeBlocks.ONLINE)) {
            station.lampTimer = 0;
            next = next.setValue(StorageNodeBlocks.ONLINE, ProductionView.online((ServerLevel) level, pos));
        }
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Ends a job whose CPU is loaded but no longer runs its step, e.g. after the
     * CPU was broken while the station was unloaded. An unloaded CPU keeps it.
     */
    private void dropOrphanedJob(ServerLevel level) {
        if (job != null && jobOwner instanceof StepRef ref && !CpuOrders.runsStep(level, ref).orElse(true)) {
            release(ref);
        }
    }

    /** Drops every item in the inputs and outputs. */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemSlots handler : List.of(inputs, outputs)) {
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        woodSlot.save(output);
        tag.put("Inputs", inputs.serializeNBT(registries));
        tag.put("Outputs", outputs.serializeNBT(registries));
        tag.putByteArray("Sides", sides.toBytes());
        if (job != null && jobOwner instanceof StepRef ref) {
            CompoundTag saved = new CompoundTag();
            EncodedPattern.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), job)
                    .result().ifPresent(pattern -> saved.put("Pattern", pattern));
            saved.putInt("Crafts", jobLeft);
            saved.putInt("Progress", progress);
            saved.put("Owner", ref.save());
            tag.put("Job", saved);
        }
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        woodSlot.load(input);
        if (tag.contains("Inputs")) {
            inputs.deserializeNBT(registries, tag.getCompoundOrEmpty("Inputs"));
        }
        if (tag.contains("Outputs")) {
            outputs.deserializeNBT(registries, tag.getCompoundOrEmpty("Outputs"));
        }
        if (tag.contains("JobResult")) {
            CompoundTag result = tag.getCompoundOrEmpty("JobResult");
            clientJobResult = result.isEmpty() ? ItemStack.EMPTY : ValueIo.parseStack(registries, result);
            clientJobLeft = input.getIntOr("JobLeft", 0);
        }
        if (tag.contains("Sides")) {
            sides.fromBytes(tag.getByteArray("Sides").orElse(new byte[0]));
        }
        if (tag.contains("Job")) {
            CompoundTag saved = tag.getCompoundOrEmpty("Job");
            Optional<EncodedPattern> pattern = EncodedPattern.CODEC
                    .parse(registries.createSerializationContext(NbtOps.INSTANCE), saved.get("Pattern")).result();
            Optional<StepRef> owner = StepRef.load(saved.getCompoundOrEmpty("Owner"));
            if (pattern.isPresent() && owner.isPresent()) {
                job = pattern.get();
                jobLeft = saved.getIntOr("Crafts", 0);
                progress = saved.getIntOr("Progress", 0);
                jobOwner = owner.get();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        ItemStack result = working() ? job.result() : ItemStack.EMPTY;
        tag.put("JobResult", result.isEmpty() ? new CompoundTag() : ValueIo.saveStack(registries, result));
        tag.putInt("JobLeft", jobLeft());
        tag.putByteArray("Sides", sides.toBytes());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Wood: the planks the block was crafted from, shown as its boards

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        WoodSlot.removeFromTag(output);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }

    // Data carrier

    @Override
    public void onLoad() {
        super.onLoad();
        carrier.join();
    }

    @Override
    public void setRemoved() {
        carrier.leave();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        carrier.leave();
    }

    /** Moved from the block's removal hook (26.1): runs on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (level == null) {
            return;
        }
        CraftingStationBlockEntity station = this;
        station.dropContents(level, pos);
    }
}
