package io.github.dirnei.mc.servus.alchemy.venter;

import io.github.dirnei.mc.servus.alchemy.AlchemyDataMaps;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.TeamOwned;
import io.github.dirnei.mc.servus.alchemy.TeamOwnership;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.alchemy.structure.SlotPort;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStatsManager;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Cucurbit's block entity, the heart of the Venter Equinus: holds the rot, the straw of the bed and
 * the Humus in the hatch, and putrefies one batch at a time while the bed is formed. No power, no fuel,
 * no GUI. The straw rots only on ticks that advance a batch.
 */
public class VenterEquinusBlockEntity extends BlockEntity implements TeamOwned {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "venter_equinus");
    /** The straw level sent to the clients for an empty bed, which shows rotted litter. */
    public static final int NO_STRAW = -1;
    private static final int SIXTEENTHS = 16;

    private final RotStore<Item> rot = new RotStore<>(defaultCapacity());
    private final StrawBed straw = new StrawBed(VenterLayout.FRAME_BLOCKS);
    private final ItemSlots hatch = new ItemSlots(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final TeamOwnership ownership = new TeamOwnership(this);
    private final LegacyItemHandler rotPort = new IntakePort(this::putrefiable, rot::room, this::insertRot);
    private final LegacyItemHandler beddingPort = new IntakePort(stack -> AlchemyDataMaps.warmth(stack) > 0, straw::room, this::layBedding);
    private final LegacyItemHandler hatchPort = SlotPort.extractOnly(hatch, 0);
    /** Points of the running batch, see {@link PutrefactionWork}. */
    private long progress;
    /** The recipe of the running batch, null while none runs. */
    private Identifier batch;
    /** The straw level last sent to the clients, in sixteenths or {@link #NO_STRAW}; {@code Integer.MIN_VALUE} before the first send. */
    private int drawnStraw = Integer.MIN_VALUE;
    /** On the client: the straw level to draw, in sixteenths or {@link #NO_STRAW}. */
    private int shownStraw = NO_STRAW;

    public VenterEquinusBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.VENTER_EQUINUS.get(), pos, state);
    }

    private static int defaultCapacity() {
        return MachineDefaults.stats(ID.toString()).itemCapacity();
    }

    private int capacity() {
        return MachineStatsManager.INSTANCE.get(ID, MachineDefaults.stats(ID.toString())).itemCapacity();
    }

    @Override
    public TeamOwnership ownership() {
        return ownership;
    }

    /** Whether the bed stands formed; only then it works. */
    public boolean formed() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof CucurbitBlock && state.getValue(CucurbitBlock.FORMED);
    }

    /** The Cucurbit: takes rot, gives nothing. */
    public LegacyItemHandler rotPort() {
        return rotPort;
    }

    /** A Dung Bed: takes bedding while the bed has room, gives nothing. */
    public LegacyItemHandler beddingPort() {
        return beddingPort;
    }

    /** The Bed Hatch: gives the Humus, takes nothing. */
    public LegacyItemHandler hatchPort() {
        return hatchPort;
    }

    /** The hatch slot, where the result waits. */
    public ItemSlots hatch() {
        return hatch;
    }

    /** The work ticks of warmth left in the straw. */
    public long warmthLeft() {
        return straw.warmthLeft();
    }

    public int rotCount() {
        return rot.total();
    }

    public int rotCapacity() {
        return rot.capacity();
    }

    public int bales() {
        return straw.bales();
    }

    public int baleCapacity() {
        return straw.capacity();
    }

    /** The straw left, 0 to 100. */
    public int warmthPercent() {
        return straw.levelMilli() / 10;
    }

    public ItemStack hatchStack() {
        return hatch.getStackInSlot(0);
    }

    /** How far the running batch is, 0 to 100. */
    public int progressPercent() {
        return runningRecipe().map(recipe -> (int) Math.min(100, progress * 100 / PutrefactionWork.total(recipe.time(), recipe.coldTime())))
                .orElse(0);
    }

    /** True when a putrefaction recipe takes {@code stack}. */
    public boolean putrefiable(ItemStack stack) {
        return level != null && !stack.isEmpty() && recipes().stream().anyMatch(holder -> holder.value().accepts(stack));
    }

    /** Puts rot into the Cucurbit as room allows, returning what did not fit. */
    public ItemStack insertRot(ItemStack stack) {
        if (!putrefiable(stack)) {
            return stack;
        }
        int taken = rot.add(stack.getItem(), stack.getCount());
        if (taken > 0) {
            setChanged();
        }
        return stack.copyWithCount(stack.getCount() - taken);
    }

    /** Lays bedding into the bed, one item per free place, returning what did not fit. */
    public ItemStack layBedding(ItemStack stack) {
        int warmth = AlchemyDataMaps.warmth(stack);
        int laid = 0;
        while (laid < stack.getCount() && straw.add(warmth)) {
            laid++;
        }
        if (laid > 0) {
            setChanged();
            syncStraw();
        }
        return stack.copyWithCount(stack.getCount() - laid);
    }

    /** Takes the Humus out of the hatch. */
    public ItemStack takeHatch() {
        ItemStack taken = hatch.getStackInSlot(0).copy();
        hatch.setStackInSlot(0, ItemStack.EMPTY);
        return taken;
    }

    /** Takes all rot back as stacks and starts the batch over. */
    public List<ItemStack> takeRot() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : rot.drain().entrySet()) {
            int left = entry.getValue();
            int max = entry.getKey().getDefaultMaxStackSize();
            while (left > 0) {
                int count = Math.min(left, max);
                stacks.add(new ItemStack(entry.getKey(), count));
                left -= count;
            }
        }
        progress = 0;
        batch = null;
        setChanged();
        return stacks;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VenterEquinusBlockEntity venter) {
        venter.tick();
    }

    private void tick() {
        rot.setCapacity(capacity());
        boolean working = false;
        if (formed()) {
            Optional<RecipeHolder<PutrefactionRecipe>> recipe = batchRecipe();
            if (recipe.isPresent() && hatchFits(recipe.get().value().result())) {
                work(recipe.get());
                working = true;
            }
        }
        setWorking(working);
        syncStraw();
    }

    private void work(RecipeHolder<PutrefactionRecipe> holder) {
        PutrefactionRecipe recipe = holder.value();
        progress += PutrefactionWork.perTick(recipe.time(), recipe.coldTime(), straw.levelMilli());
        straw.burn();
        setChanged();
        if (progress >= PutrefactionWork.total(recipe.time(), recipe.coldTime())) {
            finish(recipe);
        }
    }

    private void finish(PutrefactionRecipe recipe) {
        rot.take(matcher(recipe), recipe.input().count());
        ItemStack result = recipe.result();
        hatch.insertItem(0, result, false);
        progress = 0;
        batch = null;
        if (level instanceof ServerLevel server) {
            Journal.recordEvent(server, team(), "putrefy", result.getCount());
        }
    }

    /** The recipe of the running batch while its rot is still there, else the first recipe in id order with enough rot. */
    private Optional<RecipeHolder<PutrefactionRecipe>> batchRecipe() {
        Optional<RecipeHolder<PutrefactionRecipe>> running = runningHolder().filter(this::enoughRot);
        if (running.isPresent()) {
            return running;
        }
        progress = 0;
        batch = null;
        Optional<RecipeHolder<PutrefactionRecipe>> next = recipes().stream().filter(this::enoughRot).findFirst();
        next.ifPresent(holder -> batch = holder.id().identifier());
        return next;
    }

    private Optional<RecipeHolder<PutrefactionRecipe>> runningHolder() {
        return batch == null ? Optional.empty() : recipes().stream().filter(holder -> holder.id().identifier().equals(batch)).findFirst();
    }

    private Optional<PutrefactionRecipe> runningRecipe() {
        return runningHolder().map(RecipeHolder::value);
    }

    private List<RecipeHolder<PutrefactionRecipe>> recipes() {
        if (level == null) {
            return List.of();
        }
        return ServusRecipes.all(level, ServusAlchemyRecipes.PUTREFACTION.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .toList();
    }

    private boolean enoughRot(RecipeHolder<PutrefactionRecipe> holder) {
        return rot.count(matcher(holder.value())) >= holder.value().input().count();
    }

    private static Predicate<Item> matcher(PutrefactionRecipe recipe) {
        return item -> recipe.input().ingredient().test(item.getDefaultInstance());
    }

    private boolean hatchFits(ItemStack result) {
        return hatch.insertItem(0, result, true).isEmpty();
    }

    private void setWorking(boolean working) {
        BlockState state = getBlockState();
        if (level != null && state.getBlock() instanceof CucurbitBlock && state.getValue(CucurbitBlock.WORKING) != working) {
            level.setBlock(worldPosition, state.setValue(CucurbitBlock.WORKING, working), Block.UPDATE_CLIENTS);
        }
    }

    /** The straw level as the clients draw it: sixteenths of a full bed, or {@link #NO_STRAW} for an empty one. */
    private int strawKey() {
        return straw.bales() == 0 ? NO_STRAW : straw.levelMilli() * SIXTEENTHS / 1000;
    }

    /** Sends the straw level to the clients when the drawn level changed. */
    private void syncStraw() {
        int now = strawKey();
        if (now != drawnStraw && level != null && !level.isClientSide()) {
            drawnStraw = now;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** On the client: the straw level to draw, in sixteenths or {@link #NO_STRAW}. */
    public int shownStraw() {
        return shownStraw;
    }

    /** On the client: the height of the bedding's top in pixels, 4 for rotted litter up to 10 for a full bed. */
    public double beddingHeight() {
        return 4 + 6.0 * Math.max(0, shownStraw) / SIXTEENTHS;
    }

    /** A broken Cucurbit drops its rot and its Humus; the straw is lost. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            NonNullList<ItemStack> drops = NonNullList.create();
            drops.addAll(takeRot());
            drops.add(takeHatch());
            Containers.dropContents(level, worldPosition, drops);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        ListTag rotList = new ListTag();
        rot.contents().forEach((item, count) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
            entry.putInt("count", count);
            rotList.add(entry);
        });
        tag.put("Rot", rotList);
        tag.putIntArray("Straw", straw.save());
        tag.put("Hatch", hatch.serializeNBT(registries));
        tag.putLong("Progress", progress);
        if (batch != null) {
            tag.putString("Batch", batch.toString());
        }
        ownership.save(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        rot.drain();
        for (Tag element : tag.getListOrEmpty("Rot")) {
            if (element instanceof CompoundTag entry) {
                Optional.ofNullable(Identifier.tryParse(entry.getStringOr("id", "")))
                        .flatMap(BuiltInRegistries.ITEM::getOptional)
                        .ifPresent(item -> rot.add(item, entry.getIntOr("count", 0)));
            }
        }
        straw.load(tag.getIntArray("Straw").orElse(new int[0]));
        if (tag.contains("Hatch")) {
            hatch.deserializeNBT(registries, tag.getCompoundOrEmpty("Hatch"));
        }
        progress = tag.getLongOr("Progress", 0L);
        String running = tag.getStringOr("Batch", "");
        batch = running.isEmpty() ? null : Identifier.tryParse(running);
        ownership.load(tag);
        if (tag.contains("ShownStraw")) {
            shownStraw = tag.getIntOr("ShownStraw", NO_STRAW);
        }
    }

    /** The clients draw the straw. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("ShownStraw", strawKey());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** One virtual slot that shows empty, takes what {@code accepts} allows up to {@code room} through {@code insert} and never gives. */
    private static final class IntakePort extends LegacyItemHandler {

        /** Puts a stack in for real and returns what did not fit. */
        private interface Insert {
            ItemStack insert(ItemStack stack);
        }

        private final Predicate<ItemStack> accepts;
        private final IntSupplier room;
        private final Insert insert;

        private IntakePort(Predicate<ItemStack> accepts, IntSupplier room, Insert insert) {
            this.accepts = accepts;
            this.room = room;
            this.insert = insert;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !accepts.test(stack)) {
                return stack;
            }
            return simulate ? stack.copyWithCount(stack.getCount() - Math.min(stack.getCount(), room.getAsInt())) : insert.insert(stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Item.DEFAULT_MAX_STACK_SIZE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return accepts.test(stack);
        }
    }
}
