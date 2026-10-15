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
import java.util.Map;
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
 * A Gnome Hollow: seeded by hand with a sample of ore, ripened with principles poured in by bucket, and once a gnome
 * is bound it digs the vein's ore into a small internal store until that is full. It offers nothing to pipes or
 * other blocks; only a player under Elemental Sight tends it. Clients get the vein's look for drawing under the sight.
 */
public class GnomeHollowBlockEntity extends BlockEntity implements TeamOwned {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gnome_hollow");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    /** What a binding attempt did. */
    public enum Binding {
        BOUND, NO_VEIN, NOT_RIPE, ALREADY_BOUND, NO_SIGHT
    }

    /** What offering a gift did. */
    public enum Gift {
        TAKEN, DISLIKED, NO_GNOME
    }

    /** What a seeding attempt did. */
    public enum Seeding {
        SEEDED, ALREADY_SEEDED, NO_ROOT
    }

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), false, this::setChanged);
    private final State growth = new State();
    private final HollowStore store = new HollowStore(DEFAULTS.itemCapacity());
    private final TeamOwnership ownership = new TeamOwnership(this);
    private final GiftBoost boost = new GiftBoost();
    private Identifier vein;
    /** On the client: the vein's yield and its ripening steps, from the update tag. */
    private ItemStack shownYield = ItemStack.EMPTY;
    private int shownSteps;
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

    /** Whether the owner's team may grow this vein: its chapter must be finished. */
    public boolean mayGrow(VeinDef def) {
        if (def.chapter().isEmpty()) {
            return true;
        }
        if (!(level instanceof ServerLevel server) || team() == null) {
            return false;
        }
        return Journal.isFinished(server.getServer(), team(), def.chapter().get().toString());
    }

    /** Whether {@code stack} would seed this Hollow now. */
    public boolean acceptsSample(ItemStack stack) {
        return vein == null && veinFor(stack).isPresent();
    }

    private Optional<Map.Entry<Identifier, VeinDef>> veinFor(ItemStack stack) {
        return VeinData.INSTANCE.forSample(stack).filter(entry -> mayGrow(entry.getValue()));
    }

    /** Seeds an empty Hollow with the vein {@code sample} belongs to; the caller uses up the sample on {@link Seeding#SEEDED}. */
    public Seeding seed(ItemStack sample) {
        if (vein != null) {
            return Seeding.ALREADY_SEEDED;
        }
        Optional<Map.Entry<Identifier, VeinDef>> entry = veinFor(sample);
        if (entry.isEmpty()) {
            return Seeding.NO_ROOT;
        }
        vein = entry.get().getKey();
        growth.ripeness = 0;
        growth.bound = false;
        growth.stepTimer = 0;
        growth.yieldTimer = 0;
        setChanged();
        syncToClients();
        return Seeding.SEEDED;
    }

    /** A Salt Crystal offered by a player: binds a gnome to a ripe vein if the player sees it. */
    public Binding bind(Player player) {
        Optional<VeinDef> def = veinDef();
        if (def.isEmpty()) {
            return Binding.NO_VEIN;
        }
        if (growth.bound) {
            return Binding.ALREADY_BOUND;
        }
        if (!growth.ripe(def.get().growth())) {
            return Binding.NOT_RIPE;
        }
        if (!ElementalSight.active(player)) {
            return Binding.NO_SIGHT;
        }
        growth.bound = true;
        setChanged();
        syncToClients();
        if (level instanceof ServerLevel server) {
            Journal.recordItem(server, team(), "bind", def.get().yield(), 1);
        }
        return Binding.BOUND;
    }

    /**
     * Offers one item to the gnome, by hand or from another block. On {@link Gift#TAKEN} the boost starts or restarts
     * with the gift's factor and duration; the stack is never changed here, so the caller uses up one item.
     */
    public Gift offerGift(ItemStack stack) {
        if (veinDef().isEmpty() || !growth.bound) {
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

    /** Empties the store into stacks of the vein's yield; empty when nothing is stored or the vein's data is gone. */
    public List<ItemStack> takeStore() {
        Optional<VeinDef> def = veinDef();
        if (def.isEmpty() || store.isEmpty()) {
            return List.of();
        }
        ItemStack yield = def.get().yield();
        int left = store.takeAll();
        setChanged();
        List<ItemStack> stacks = new ArrayList<>();
        while (left > 0) {
            int count = Math.min(left, yield.getMaxStackSize());
            stacks.add(yield.copyWithCount(count));
            left -= count;
        }
        return stacks;
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
        Outcome outcome = VeinGrowth.tick(def.get().growth(), growth, tanks.contents(), store.hasRoom(), work);
        switch (outcome) {
            case RIPENED -> {
                tanks.take(def.get().growth().stepCost());
                syncToClients();
            }
            case YIELD -> store.add();
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

    public boolean shownBound() {
        return growth.bound;
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
        growth.bound = input.getBooleanOr("Bound", false);
        growth.stepTimer = input.getIntOr("StepTimer", 0);
        growth.yieldTimer = input.getIntOr("YieldTimer", 0);
        store.load(input.getIntOr("Stored", 0));
        boost.load(input.getDoubleOr("GiftFactor", 1), input.getIntOr("GiftTicks", 0));
        shownBoosted = input.getBooleanOr("Boosted", false);
        ownership.load(tag);
        if (tag.contains("ShownYield")) {
            shownYield = ValueIo.parseStack(registries, tag.getCompoundOrEmpty("ShownYield"));
            shownSteps = input.getIntOr("ShownSteps", 0);
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
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
