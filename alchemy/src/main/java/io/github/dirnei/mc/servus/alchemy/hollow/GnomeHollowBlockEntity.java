package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.PrincipleTanks;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.TeamOwned;
import io.github.dirnei.mc.servus.alchemy.TeamOwnership;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinData.VeinDef;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.Outcome;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.State;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.MachineStatsManager;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A Gnome Hollow: a Salt Crystal calls a gnome in, he plants the vein of a Seed, principles poured in by bucket ripen
 * it while he tends it, and once ripe he digs the vein's ore into a small internal store until that is full. Another
 * Seed makes him dig out the old vein and plant the new one; the store keeps what he dug. It offers nothing to pipes
 * or other blocks; only a player under Elemental Sight tends it. Clients get the vein's look for drawing under the sight.
 */
public class GnomeHollowBlockEntity extends BlockEntity implements TeamOwned {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gnome_hollow");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    /** What a binding attempt did. */
    public enum Binding {
        BOUND, ALREADY_BOUND, NO_SIGHT
    }

    /** What offering a gift did. */
    public enum Gift {
        TAKEN, DISLIKED, NO_GNOME
    }

    /** What giving a Seed did. */
    public enum Seeding {
        /** The gnome planted the Seed's vein in an empty Hollow. */
        PLANTED,
        /** The gnome dug out the vein of another kind and planted the Seed's. */
        SWAPPED,
        /** The Hollow already grows this vein. */
        SAME_VEIN,
        /** No gnome lives in the Hollow to plant it. */
        NO_GNOME,
        /** The item is no Seed. */
        NOT_A_SEED,
        /** The Seed names no loaded vein. */
        UNKNOWN_SEED;

        /** Whether the Seed was planted, so the caller uses it up. */
        public boolean planted() {
            return this == PLANTED || this == SWAPPED;
        }
    }

    /** {@link #shownBoundAt()} of a Hollow the client did not see being bound. */
    public static final long NOT_SEEN_BINDING = Long.MIN_VALUE;

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), false, this::setChanged);
    private final State growth = new State();
    private final HollowStore store = new HollowStore(DEFAULTS.itemCapacity());
    private final TeamOwnership ownership = new TeamOwnership(this);
    private final GiftBoost boost = new GiftBoost();
    private Identifier vein;
    /** One of what the store holds, empty while it is empty; it may differ from the vein's yield after a swap. */
    private ItemStack storedYield = ItemStack.EMPTY;
    /** On the client: the vein's yield and its ripening steps, from the update tag. */
    private ItemStack shownYield = ItemStack.EMPTY;
    private int shownSteps;
    private int shownColor = VeinData.DEFAULT_COLOR;
    private long shownBoundAt = NOT_SEEN_BINDING;
    private boolean shownOnce;
    private boolean shownBoosted;

    public GnomeHollowBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.GNOME_HOLLOW.get(), pos, state);
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    public State growth() {
        return growth;
    }

    public HollowStore store() {
        return store;
    }

    public GiftBoost boost() {
        return boost;
    }

    /** Ticks left of the running gift boost, 0 without one; a giver may wait until this runs low. */
    public int boostTicksLeft() {
        return boost.ticksLeft();
    }

    /** How many times faster the gnome digs now, 1 without a gift. */
    public double boostFactor() {
        return boost.factor();
    }

    public Optional<Identifier> vein() {
        return Optional.ofNullable(vein);
    }

    /** The vein's definition, empty without a vein or when its data is gone. */
    public Optional<VeinDef> veinDef() {
        return vein == null ? Optional.empty() : VeinData.INSTANCE.get(vein);
    }

    @Override
    public TeamOwnership ownership() {
        return ownership;
    }

    private static MachineStats stats() {
        return MachineStatsManager.INSTANCE.get(ID, DEFAULTS);
    }

    /**
     * The gnome plants the vein a Seed names; a vein of another kind is dug out and lost, the store and the tanks stay.
     * The stack is never changed here, so the caller uses up one Seed when {@link Seeding#planted()}.
     */
    public Seeding seed(ItemStack stack) {
        if (!growth.bound) {
            return Seeding.NO_GNOME;
        }
        Optional<Identifier> named = VeinSeedItem.vein(stack);
        if (named.isEmpty()) {
            return Seeding.NOT_A_SEED;
        }
        if (VeinData.INSTANCE.get(named.get()).isEmpty()) {
            return Seeding.UNKNOWN_SEED;
        }
        if (named.get().equals(vein)) {
            return Seeding.SAME_VEIN;
        }
        Seeding result = vein == null ? Seeding.PLANTED : Seeding.SWAPPED;
        if (!store.isEmpty()) {
            storedYield = storedYield().copyWithCount(1);
        }
        vein = named.get();
        growth.ripeness = 0;
        growth.stepTimer = 0;
        growth.yieldTimer = 0;
        setChanged();
        syncToClients();
        return result;
    }

    /** A Salt Crystal offered by a player: calls a gnome into a Hollow without one if the player sees it. */
    public Binding bind(Player player) {
        if (growth.bound) {
            return Binding.ALREADY_BOUND;
        }
        if (!ElementalSight.active(player)) {
            return Binding.NO_SIGHT;
        }
        growth.bound = true;
        setChanged();
        syncToClients();
        if (level instanceof ServerLevel server) {
            Journal.recordEvent(server, team(), "bind", 1);
        }
        return Binding.BOUND;
    }

    /**
     * Offers one item to the gnome, by hand or from another block. On {@link Gift#TAKEN} the boost starts or restarts
     * with the gift's factor and duration; the stack is never changed here, so the caller uses up one item.
     */
    public Gift offerGift(ItemStack stack) {
        if (!growth.bound) {
            return Gift.NO_GNOME;
        }
        Optional<GnomeGiftData.GiftDef> gift = GnomeGiftData.INSTANCE.forItem(stack);
        if (gift.isEmpty()) {
            return Gift.DISLIKED;
        }
        boolean wasBoosted = boost.active();
        boost.give(gift.get().factor(), gift.get().durationTicks());
        setChanged();
        if (!wasBoosted) {
            syncToClients();
        }
        return Gift.TAKEN;
    }

    /**
     * Principles dripped in by a Principle Dropper above: like a poured bucket, but taken only while the vein is
     * seeded, not ripe and its steps take that principle. Returns the mB taken.
     */
    int receiveDrip(Principle principle, int amount) {
        Optional<VeinDef> def = veinDef();
        if (amount <= 0 || def.isEmpty() || growth.ripe(def.get().growth()) || def.get().growth().stepCost().get(principle) <= 0) {
            return 0;
        }
        return tanks.tank(principle).fill(new FluidStack(PrincipleFluids.fluid(principle), amount), false);
    }

    /** What one drip from above may bring: what the vein's steps take, and the free space of each tank. */
    Optional<DripNeed> dripNeed() {
        Optional<VeinDef> def = veinDef();
        if (def.isEmpty() || growth.ripe(def.get().growth())) {
            return Optional.empty();
        }
        Principles room = Principles.NONE;
        for (Principle principle : Principle.values()) {
            room = room.with(principle, Math.max(0, tanks.capacity() - tanks.amount(principle)));
        }
        return Optional.of(new DripNeed(def.get().growth().stepCost(), room));
    }

    /** What an unripe vein drinks per step and how much more each tank of the Hollow holds. */
    record DripNeed(Principles stepCost, Principles room) {
    }

    /** One of what the store holds: what was dug, else the vein's yield; empty without either. */
    public ItemStack storedYield() {
        if (!storedYield.isEmpty()) {
            return storedYield;
        }
        return veinDef().map(VeinDef::yield).orElse(ItemStack.EMPTY);
    }

    /** Empties the store into stacks of what it holds; empty when nothing is stored or that is unknown. */
    public List<ItemStack> takeStore() {
        ItemStack yield = storedYield();
        if (yield.isEmpty() || store.isEmpty()) {
            return List.of();
        }
        int left = store.takeAll();
        storedYield = ItemStack.EMPTY;
        setChanged();
        List<ItemStack> stacks = new ArrayList<>();
        while (left > 0) {
            int count = Math.min(left, yield.getMaxStackSize());
            stacks.add(yield.copyWithCount(count));
            left -= count;
        }
        return stacks;
    }

    /** Takes up to {@code max} items of the store as one stack, for the homunculus; empty when nothing is taken. */
    public ItemStack takeYield(int max) {
        ItemStack yield = storedYield();
        if (yield.isEmpty() || max <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = store.take(Math.min(max, yield.getMaxStackSize()));
        if (taken == 0) {
            return ItemStack.EMPTY;
        }
        if (store.isEmpty()) {
            storedYield = ItemStack.EMPTY;
        }
        setChanged();
        return yield.copyWithCount(taken);
    }

    /** Whether the gnome may put {@code yield} into the store: room left, and nothing else in it. */
    private boolean storeTakes(ItemStack yield) {
        return store.hasRoom() && (store.isEmpty() || ItemStack.isSameItemSameComponents(storedYield(), yield));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GnomeHollowBlockEntity hollow) {
        hollow.tick();
    }

    private void tick() {
        MachineStats stats = stats();
        tanks.setCapacity(stats.fluidCapacity());
        store.setCapacity(stats.itemCapacity());
        boolean wasBoosted = boost.active();
        int work = boost.work();
        if (wasBoosted) {
            setChanged();
            if (!boost.active()) {
                syncToClients();
            }
        }
        Optional<VeinDef> def = veinDef();
        if (def.isEmpty()) {
            return;
        }
        ItemStack yield = def.get().yield();
        Outcome outcome = VeinGrowth.tick(def.get().growth(), growth, tanks.contents(), storeTakes(yield), work);
        switch (outcome) {
            case RIPENED -> {
                tanks.take(def.get().growth().stepCost());
                syncToClients();
                if (growth.ripe(def.get().growth()) && level instanceof ServerLevel server) {
                    Journal.recordItem(server, team(), "ripen", yield, 1);
                }
            }
            case YIELD -> {
                storedYield = yield.copyWithCount(1);
                store.add();
            }
            default -> {
            }
        }
        if (outcome != Outcome.NONE || growth.stepTimer > 0 || growth.yieldTimer > 0) {
            setChanged();
        }
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The vein's yield as the client knows it, empty without a vein. */
    public ItemStack shownYield() {
        return shownYield;
    }

    /** Ripeness from 0 to 1 as the client knows it. */
    public float shownRipeness() {
        return shownSteps <= 0 ? 0 : Math.min(1F, growth.ripeness / (float) shownSteps);
    }

    /** The RGB of the vein's material under the sight, as the client knows it. */
    public int shownColor() {
        return shownColor;
    }

    public boolean shownBound() {
        return growth.bound;
    }

    /**
     * The game time at which the client saw a gnome being bound here, or {@link #NOT_SEEN_BINDING} when it was bound
     * before the client knew the Hollow or is not bound.
     */
    public long shownBoundAt() {
        return shownBoundAt;
    }

    /** Whether a gift boost runs, as the client knows it. */
    public boolean shownBoosted() {
        return shownBoosted;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            HollowTracker.add(this);
        }
    }

    @Override
    public void setRemoved() {
        HollowTracker.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        HollowTracker.remove(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Principles", tanks.save(registries));
        if (vein != null) {
            output.putString("Vein", vein.toString());
        }
        output.putInt("Ripeness", growth.ripeness);
        output.putBoolean("Bound", growth.bound);
        output.putInt("StepTimer", growth.stepTimer);
        output.putInt("YieldTimer", growth.yieldTimer);
        output.putInt("Stored", store.count());
        if (!storedYield.isEmpty()) {
            tag.put("StoredYield", ValueIo.saveStack(registries, storedYield));
        }
        output.putDouble("GiftFactor", boost.factor());
        output.putInt("GiftTicks", boost.ticksLeft());
        ownership.save(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        tanks.load(tag.getCompoundOrEmpty("Principles"));
        vein = tag.contains("Vein") ? Identifier.tryParse(input.getStringOr("Vein", "")) : null;
        growth.ripeness = input.getIntOr("Ripeness", 0);
        boolean wasBound = growth.bound;
        growth.bound = input.getBooleanOr("Bound", false);
        noteBinding(wasBound);
        growth.stepTimer = input.getIntOr("StepTimer", 0);
        growth.yieldTimer = input.getIntOr("YieldTimer", 0);
        store.load(input.getIntOr("Stored", 0));
        storedYield = tag.contains("StoredYield") ? ValueIo.parseStack(registries, tag.getCompoundOrEmpty("StoredYield")) : ItemStack.EMPTY;
        boost.load(input.getDoubleOr("GiftFactor", 1), input.getIntOr("GiftTicks", 0));
        shownBoosted = input.getBooleanOr("Boosted", false);
        ownership.load(tag);
        if (tag.contains("ShownYield")) {
            shownYield = ValueIo.parseStack(registries, tag.getCompoundOrEmpty("ShownYield"));
            shownSteps = input.getIntOr("ShownSteps", 0);
            shownColor = input.getIntOr("ShownColor", VeinData.DEFAULT_COLOR);
        }
    }

    /** On the client, remembers when it saw the gnome being bound, so the sight can open the cave. */
    private void noteBinding(boolean wasBound) {
        boolean seenBinding = shownOnce && !wasBound && growth.bound;
        shownOnce = true;
        if (!growth.bound) {
            shownBoundAt = NOT_SEEN_BINDING;
        } else if (seenBinding && level != null && level.isClientSide()) {
            shownBoundAt = level.getGameTime();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Ripeness", growth.ripeness);
        tag.putBoolean("Bound", growth.bound);
        tag.putBoolean("Boosted", boost.active());
        Optional<VeinDef> def = veinDef();
        tag.put("ShownYield", def.map(d -> d.yield().isEmpty() ? new CompoundTag() : ValueIo.saveStack(registries, d.yield())).orElseGet(CompoundTag::new));
        tag.putInt("ShownSteps", def.map(d -> d.growth().steps()).orElse(0));
        tag.putInt("ShownColor", def.map(VeinDef::color).orElse(VeinData.DEFAULT_COLOR));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
