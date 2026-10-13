package io.github.dirnei.mc.servus.storage.crafting;

import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.craft.IngredientGatherer;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.StepRef;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.power.StepPower;
import io.github.dirnei.mc.servus.storage.production.DataLineBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.terminal.TerminalWarehouses;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Storage's own producer and the hands of the Crafting CPU: crafts the
 * pattern the CPU hands it as a job, paid from the warehouse's FE. A job's
 * results wait in the outputs for the CPU; without a job it crafts nothing,
 * automation inserts nothing and leftover outputs are pushed into neighbours.
 */
public class CraftingStationBlockEntity extends DataLineBlockEntity implements MenuProvider, SidedBlockEntity {

    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 4;
    private static final int PUSH_INTERVAL = 8;
    private static final int OWNER_CHECK_TICKS = 100;
    private static final int PAYER_TICKS = 20;

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
    /** The pattern the CPU handed this station, the crafts left and the step that owns it. */
    private EncodedPattern job;
    private int jobLeft;
    private Object jobOwner;
    private ItemStack clientJobResult = ItemStack.EMPTY;
    private int clientJobLeft;
    private int progress;
    private int ownerCheck;
    private int pushTimer;
    /** Whether the last working tick could not be paid; the craft waits with its progress kept. Not saved. */
    private boolean waitingForPower;
    /** The controller that pays the station's work, looked up over the data line once a second. */
    private BlockPos payer;
    private int payerCheck;
    private boolean freePower;

    private final SideConfig sides = SideConfig.station();
    private final LegacyItemHandler inlet = new InletHandler();

    /**
     * What data line packets see on every face: the inputs take only items the
     * job uses, the outputs only give. Pipes and hoppers see no item capability.
     */
    private final class InletHandler extends LegacyItemHandler {

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
            if (slot >= INPUT_SLOTS || !uses(stack)) {
                return stack;
            }
            return inputs.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < INPUT_SLOTS ? ItemStack.EMPTY : outputs.extractItem(slot - INPUT_SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS && uses(stack);
        }
    }

    public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRAFTING_STATION.get(), pos, state);
        sides.lock(RelativeSide.FRONT, SideMode.DISABLED);
    }

    public ItemSlots inputs() {
        return inputs;
    }

    public ItemSlots outputs() {
        return outputs;
    }

    /** The packet inlet: job filtered inputs in, outputs out, on every face. */
    public ResourceHandler<ItemResource> packetInlet() {
        return inlet;
    }

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
        syncToClients();
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
        if (working() && !Objects.equals(jobOwner, owner) || blockedFor(owner)) {
            return false;
        }
        boolean same = job != null && Objects.equals(jobOwner, owner) && job.equals(pattern);
        job = pattern;
        jobLeft = Math.max(0, crafts);
        jobOwner = owner;
        if (!same) {
            progress = 0;
        }
        syncToClients();
        return true;
    }

    /** Ends the job of that owner; a job of another owner stays. */
    public void release(Object owner) {
        if (job != null && Objects.equals(jobOwner, owner)) {
            job = null;
            jobLeft = 0;
            jobOwner = null;
            progress = 0;
            syncToClients();
        }
    }

    private void syncToClients() {
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
        Optional<RecipeHolder<CraftingRecipe>> holder = ServusRecipes.crafting(level, recipe.recipe());
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

    /** For GameTests of stations without a warehouse on their line: work without paying. Not saved. */
    public void setFreePowerForTests(boolean free) {
        freePower = free;
    }

    /** Whether the station holds a craft it cannot pay for: its progress waits for FE. */
    public boolean waitingForPower() {
        return waitingForPower;
    }

    /** @return whether the station is working */
    private boolean tick() {
        if (!working() || plan(job).isEmpty()) {
            progress = 0;
            waitingForPower = false;
            return false;
        }
        boolean paid = payTick();
        if (waitingForPower == paid) {
            waitingForPower = !paid;
            setChanged();
        }
        progress = StepPower.advance(progress, paid);
        if (!paid) {
            return false;
        }
        if (progress >= workTicks()) {
            progress = 0;
            finish();
        }
        setChanged();
        return true;
    }

    /**
     * Pays one working tick from the controller of the first warehouse the
     * station's data line reaches (lowest controller position). The controller
     * is looked up once a second; a line without an online warehouse cannot pay.
     */
    private boolean payTick() {
        if (freePower) {
            return true;
        }
        long cost = (long) Math.ceil(StorageStatsManager.INSTANCE.craftingStationFePerTick());
        if (cost <= 0) {
            return true;
        }
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (payer == null || --payerCheck <= 0) {
            payerCheck = PAYER_TICKS;
            payer = TerminalWarehouses.reachable(server, worldPosition).stream().findFirst()
                    .flatMap(WarehouseNetwork::controllerPos).orElse(null);
        }
        if (payer == null || !server.isLoaded(payer)
                || !(server.getBlockEntity(payer) instanceof WarehouseControllerBlockEntity controller)) {
            payer = null;
            return false;
        }
        return controller.drawEnergy(cost);
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
        syncToClients();
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
            // A job's outputs wait for its CPU to collect and count them.
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
        next = station.withOnlineLamp((ServerLevel) level, next);
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

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraftingStationMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
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
        ItemStack result = working() ? job.result() : ItemStack.EMPTY;
        tag.put("JobResult", result.isEmpty() ? new CompoundTag() : ValueIo.saveStack(registries, result));
        tag.putInt("JobLeft", jobLeft());
        tag.putByteArray("Sides", sides.toBytes());
        return tag;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            dropContents(level, pos);
        }
    }
}
