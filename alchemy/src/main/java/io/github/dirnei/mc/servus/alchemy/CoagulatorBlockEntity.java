package io.github.dirnei.mc.servus.alchemy;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMachineKinds;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.naming.MachineNameSlot;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorSeeds;
import io.github.dirnei.mc.servus.alchemy.coagulator.SeedSlot;
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
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Grows the recipe its seed belongs to from piped principles and an optional item. The seeds in slot 2, a stack of one kind,
 * pick the recipe, and one is used up per finished cycle, so the Coagulator stops when they run out. A transmutation seed
 * turns into the result; a crystal of the result comes back with it, so the cycle gives one more. It needs no power: the
 * joined principles only take the recipe's time to cool and set.
 */
public class CoagulatorBlockEntity extends MachineBlockEntity implements TeamOwned, NamedMachine {

    public static final int SLOT_INGREDIENT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_SEED = 2;
    /** Container data: 1 while the seed's recipe waits for a journal chapter of the owner's team. */
    public static final int DATA_LOCKED = AlchemyMachines.DATA_COUNT;
    public static final int DATA_COUNT = DATA_LOCKED + 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "coagulator");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    private static final int SYNC_INTERVAL = 10;

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), false, this::setChanged);
    private final TeamOwnership ownership = new TeamOwnership(this);
    private final LegacyItemHandler itemPort = new ItemPort();
    private final DrawnFill drawnFill = new DrawnFill();
    private int syncCooldown;
    /** The seed and running state last sent to the clients, null before the first send. */
    private Item drawnSeed;
    private boolean drawnGrowing;
    /** On the client: the seed lying in the mold, whether a cycle runs, its progress at {@link #shownAt} and its length. */
    private Item shownSeed = Items.AIR;
    private boolean shownGrowing;
    private int shownProgress;
    private int shownTicks = 1;
    private long shownAt;

    private final MachineNameSlot nameSlot = new MachineNameSlot(this, ServusAlchemyMachineKinds.COAGULATOR);

    @Override
    public MachineNameSlot nameSlot() {
        return nameSlot;
    }

    public CoagulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.COAGULATOR.get(), pos, state, 3);
        AlchemyMachines.closeAllSides(this);
    }

    @Override
    public TeamOwnership ownership() {
        return ownership;
    }

    /** Every coagulation recipe in a fixed order, the same on server and client. */
    public static List<RecipeHolder<CoagulationRecipe>> sortedRecipes(Level level) {
        return ServusRecipes.all(level, ServusAlchemyRecipes.COAGULATION.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .toList();
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    /** The seed in the mold, empty when there is none. */
    public ItemStack seed() {
        return inventory.getStackInSlot(SLOT_SEED);
    }

    /** Takes all seeds out, which stops the Coagulator. */
    public ItemStack takeSeed() {
        ItemStack seed = seed().copy();
        inventory.setStackInSlot(SLOT_SEED, ItemStack.EMPTY);
        resetProgress();
        return seed;
    }

    /** Puts as many of {@code stack} into the seed slot as fit, without changing {@code stack}; returns how many went in. */
    public int plantSeed(ItemStack stack) {
        int planted = Math.min(stack.getCount(), seedRoom(stack));
        if (planted > 0) {
            ItemStack held = seed();
            inventory.setStackInSlot(SLOT_SEED, held.isEmpty() ? stack.copyWithCount(planted) : held.copyWithCount(held.getCount() + planted));
        }
        return planted;
    }

    /** How many of {@code stack} the seed slot takes now: none for an item that seeds nothing or another seed than the one in it. */
    public int seedRoom(ItemStack stack) {
        if (stack.isEmpty() || level == null || !CoagulatorSeeds.isSeed(level, stack)) {
            return 0;
        }
        ItemStack held = seed();
        return SeedSlot.room(held.getCount(), ItemStack.isSameItemSameComponents(held, stack), stack.getMaxStackSize());
    }

    /** The recipe the seed belongs to, locked or not. */
    public Optional<RecipeHolder<CoagulationRecipe>> seedRecipe() {
        return level == null ? Optional.empty() : CoagulatorSeeds.recipeFor(level, seed());
    }

    /** Whether the owning team may grow the recipe: it names no chapter, or the team finished it. */
    public boolean unlocked(CoagulationRecipe recipe) {
        if (recipe.chapter().isEmpty()) {
            return true;
        }
        return level instanceof ServerLevel server && Journal.isFinished(server.getServer(), team(), recipe.chapter().get().toString());
    }

    /** The recipe the seed grows now: its recipe, when the owning team may grow it. */
    public Optional<RecipeHolder<CoagulationRecipe>> growingRecipe() {
        return seedRecipe().filter(holder -> unlocked(holder.value()));
    }

    /** Whether the seed's recipe waits for a journal chapter of the owning team. */
    public boolean seedLocked() {
        return seedRecipe().map(holder -> !unlocked(holder.value())).orElse(false);
    }

    /** Whether the build around this Coagulator is formed; only then it works. */
    public boolean formed() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof CoagulatorBlock && state.getValue(CoagulatorBlock.FORMED);
    }

    /** The casting table: takes the ingredient into slot 0 and seeds into slot 2, gives the products from slot 1. */
    public LegacyItemHandler itemPort() {
        return itemPort;
    }

    /** Sends the tanks to the clients when a Vat's drawn level changed by a sixteenth. */
    private void syncFill() {
        if (drawnFill.update(tanks)) {
            sendToClients();
        }
    }

    /** Sends the seed and the cycle to the clients when the seed or the running state changed, or a cycle started over. */
    private void syncGrowth(boolean growing, boolean restarted) {
        Item seed = seed().getItem();
        if (seed != drawnSeed || growing != drawnGrowing || restarted) {
            drawnSeed = seed;
            drawnGrowing = growing;
            sendToClients();
        }
    }

    private void sendToClients() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Slot 0 takes what the seed's recipe needs and gives nothing; slot 1 gives the products and takes nothing; slot 2 takes
     * seeds as {@link #seedRoom} allows and never gives them back.
     */
    private final class ItemPort extends LegacyItemHandler {

        private static final int[] SLOTS = {SLOT_INGREDIENT, SLOT_OUTPUT, SLOT_SEED};

        @Override
        public int getSlots() {
            return SLOTS.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(SLOTS[slot]);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (SLOTS[slot] == SLOT_INGREDIENT) {
                return acceptsInput(SLOT_INGREDIENT, stack) ? inventory.insertItem(SLOT_INGREDIENT, stack, simulate) : stack;
            }
            if (SLOTS[slot] == SLOT_SEED) {
                int planted = simulate ? Math.min(stack.getCount(), seedRoom(stack)) : plantSeed(stack);
                return planted == 0 ? stack : stack.copyWithCount(stack.getCount() - planted);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return SLOTS[slot] == SLOT_OUTPUT ? inventory.extractItem(SLOT_OUTPUT, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(SLOTS[slot]);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return SLOTS[slot] != SLOT_OUTPUT && acceptsInput(SLOTS[slot], stack);
        }
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
        return slot == SLOT_INGREDIENT || slot == SLOT_SEED;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    /** The ingredient slot takes what the seed's recipe needs; the seed slot takes seeds while {@link #seedRoom} has room. */
    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        if (slot == SLOT_SEED) {
            return seedRoom(stack) > 0;
        }
        return seedRecipe().map(holder -> holder.value().acceptsItem(stack)).orElse(false);
    }

    private boolean canWork(Optional<CoagulationRecipe> recipe) {
        return formed() && recipe.map(value -> tanks.contents().covers(value.input())
                && value.itemReady(inventory.getStackInSlot(SLOT_INGREDIENT))
                && canInsertOutput(SLOT_OUTPUT, value.resultFrom(seed()))).orElse(false);
    }

    @Override
    protected boolean tick() {
        tanks.setCapacity(stats().fluidCapacity());
        if (++syncCooldown >= SYNC_INTERVAL) {
            syncCooldown = 0;
            syncFill();
        }
        Optional<CoagulationRecipe> recipe = growingRecipe().map(RecipeHolder::value);
        if (!canWork(recipe)) {
            resetProgress();
            syncGrowth(false, false);
            return false;
        }
        boolean finished = stepProgress();
        if (finished) {
            finishWork(recipe.get());
        }
        syncGrowth(true, finished);
        return true;
    }

    private void finishWork(CoagulationRecipe recipe) {
        tanks.take(recipe.input());
        recipe.ingredient().ifPresent(sized -> inventory.extractItem(SLOT_INGREDIENT, sized.count(), false));
        ItemStack grown = recipe.resultFrom(inventory.extractItem(SLOT_SEED, 1, false));
        insertOutput(SLOT_OUTPUT, grown.copy());
        if (level instanceof ServerLevel server) {
            Journal.recordItem(server, team(), "coagulate", grown, grown.getCount());
        }
    }

    @Override
    protected int workTicks() {
        return growingRecipe().map(holder -> Math.max(1, holder.value().time())).orElse(Integer.MAX_VALUE);
    }

    @Override
    protected int dataCount() {
        return DATA_COUNT;
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_WORK_TICKS) {
            return growingRecipe().map(holder -> holder.value().time()).orElse(0);
        }
        if (index == DATA_LOCKED) {
            return seedLocked() ? 1 : 0;
        }
        int tank = AlchemyMachines.tankData(tanks, index);
        return tank >= 0 ? tank : super.getData(index);
    }

    /**
     * The Foundation production view: only the recipe the seed grows, item ingredient and one seed in, what that seed grows
     * out. Principles come by pipe.
     */
    public ProductionProvider production() {
        return new ProductionProvider() {
            @Override
            public List<ProductionRecipe> recipes() {
                if (!formed()) {
                    return List.of();
                }
                return growingRecipe().map(holder -> {
                    CoagulationRecipe recipe = holder.value();
                    List<ItemStack> inputs = new ArrayList<>();
                    recipe.ingredient().ifPresent(sized -> {
                        ItemStack item = ServusRecipes.firstItem(sized.ingredient());
                        if (!item.isEmpty()) {
                            inputs.add(item.copyWithCount(sized.count()));
                        }
                    });
                    inputs.add(seed().copyWithCount(1));
                    return List.of(new ProductionRecipe(holder.id().identifier(), inputs, List.of(recipe.resultFrom(seed())), Math.max(1, recipe.time())));
                }).orElse(List.of());
            }

            @Override
            public Optional<Identifier> active() {
                return growingRecipe().map(holder -> holder.id().identifier());
            }
        };
    }

    /** On the client: the seed lying in the mold, empty for none. */
    public ItemStack shownSeed() {
        return shownSeed == Items.AIR ? ItemStack.EMPTY : new ItemStack(shownSeed);
    }

    /** On the client: how far the running cycle is, 0 to 1, counted on from the last update; 0 while nothing grows. */
    public float shownGrowth(float partialTick) {
        if (!shownGrowing || level == null) {
            return 0;
        }
        float ticks = shownProgress + (level.getGameTime() - shownAt) + partialTick;
        return Math.min(1.0F, Math.max(0.0F, ticks / shownTicks));
    }

    /** The clients draw the principles in the Vats and the seed in the mold. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Principles", tanks.save(registries));
        tag.putString("ShownSeed", BuiltInRegistries.ITEM.getKey(seed().getItem()).toString());
        tag.putBoolean("Growing", drawnGrowing);
        tag.putInt("CycleProgress", progress);
        tag.putInt("CycleTicks", Math.max(1, getData(DATA_WORK_TICKS)));
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Principles", tanks.save(registries));
        ownership.save(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        tanks.load(tag.getCompoundOrEmpty("Principles"));
        ownership.load(tag);
        if (tag.contains("ShownSeed")) {
            shownSeed = Optional.ofNullable(Identifier.tryParse(tag.getStringOr("ShownSeed", "")))
                    .flatMap(BuiltInRegistries.ITEM::getOptional).orElse(Items.AIR);
            shownGrowing = tag.getBooleanOr("Growing", false);
            shownProgress = tag.getIntOr("CycleProgress", 0);
            shownTicks = Math.max(1, tag.getIntOr("CycleTicks", 1));
            shownAt = level == null ? 0 : level.getGameTime();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoagulatorMenu(containerId, playerInventory, this);
    }
}
