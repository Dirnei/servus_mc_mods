package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.AlchemyMachines;
import io.github.dirnei.mc.servus.alchemy.PrincipleTanks;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.TeamOwned;
import io.github.dirnei.mc.servus.alchemy.TeamOwnership;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinData.VeinDef;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.Outcome;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.State;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Gnome Hollow: seeded with a sample of ore, ripened with principles, and once a gnome is bound
 * it yields the vein's ore forever. Clients get the vein's look for drawing under Elemental Sight.
 */
public class GnomeHollowBlockEntity extends MachineBlockEntity implements TeamOwned {

    public static final int SLOT_SAMPLE = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int DATA_RIPENESS = AlchemyMachines.DATA_COUNT;
    public static final int DATA_STEPS = DATA_RIPENESS + 1;
    public static final int DATA_BOUND = DATA_RIPENESS + 2;
    public static final int DATA_COUNT = DATA_RIPENESS + 3;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gnome_hollow");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    /** What a binding attempt did. */
    public enum Binding {
        BOUND, NO_VEIN, NOT_RIPE, ALREADY_BOUND, NO_SIGHT
    }

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), false, this::setChanged);
    private final State growth = new State();
    private final TeamOwnership ownership = new TeamOwnership(this);
    private Identifier vein;
    /** On the client: the vein's yield and its ripening steps, from the update tag. */
    private ItemStack shownYield = ItemStack.EMPTY;
    private int shownSteps;

    public GnomeHollowBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.GNOME_HOLLOW.get(), pos, state, 2);
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    public State growth() {
        return growth;
    }

    public Optional<Identifier> vein() {
        return Optional.ofNullable(vein);
    }

    private Optional<VeinDef> veinDef() {
        return vein == null ? Optional.empty() : VeinData.INSTANCE.get(vein);
    }

    /** Neighbours fill the tanks on every non disabled side; nothing comes out. */
    public LegacyFluidHandler fluidHandler(Direction side) {
        return side == null || sideMode(side).allowsInsert() ? tanks : null;
    }

    @Override
    public TeamOwnership ownership() {
        return ownership;
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

    private Optional<Map.Entry<Identifier, VeinDef>> veinFor(ItemStack stack) {
        return VeinData.INSTANCE.forSample(stack).filter(entry -> mayGrow(entry.getValue()));
    }

    private void seedFromSlot() {
        ItemStack sample = inventory.getStackInSlot(SLOT_SAMPLE);
        if (vein != null || sample.isEmpty()) {
            return;
        }
        veinFor(sample).ifPresent(entry -> {
            vein = entry.getKey();
            growth.ripeness = 0;
            growth.bound = false;
            growth.stepTimer = 0;
            growth.yieldTimer = 0;
            inventory.extractItem(SLOT_SAMPLE, 1, false);
            syncToClients();
        });
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

    @Override
    protected boolean tick() {
        tanks.setCapacity(stats().fluidCapacity());
        seedFromSlot();
        Optional<VeinDef> def = veinDef();
        if (def.isEmpty()) {
            return false;
        }
        ItemStack yield = def.get().yield();
        Outcome outcome = VeinGrowth.tick(def.get().growth(), growth, tanks.contents(), canInsertOutput(SLOT_OUTPUT, yield));
        switch (outcome) {
            case RIPENED -> {
                tanks.take(def.get().growth().stepCost());
                syncToClients();
            }
            case YIELD -> insertOutput(SLOT_OUTPUT, yield.copy());
            default -> {
            }
        }
        if (outcome != Outcome.NONE || growth.stepTimer > 0 || growth.yieldTimer > 0) {
            setChanged();
        }
        return growth.bound;
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
        return slot == SLOT_SAMPLE;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return vein == null && veinFor(stack).isPresent();
    }

    @Override
    protected int workTicks() {
        return veinDef().map(def -> growth.ripe(def.growth()) ? def.growth().yieldTicks() : def.growth().stepTicks()).orElse(0);
    }

    @Override
    protected int dataCount() {
        return DATA_COUNT;
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_RIPENESS) {
            return growth.ripeness;
        }
        if (index == DATA_STEPS) {
            return veinDef().map(def -> def.growth().steps()).orElse(0);
        }
        if (index == DATA_BOUND) {
            return growth.bound ? 1 : 0;
        }
        if (index == DATA_PROGRESS) {
            return growth.bound ? growth.yieldTimer : growth.stepTimer;
        }
        int tank = AlchemyMachines.tankData(tanks, index);
        return tank >= 0 ? tank : super.getData(index);
    }

    /** The Foundation production view: a working Hollow makes its yield from nothing. */
    public ProductionProvider production() {
        return new ProductionProvider() {
            @Override
            public List<ProductionRecipe> recipes() {
                Optional<VeinDef> def = veinDef();
                if (def.isEmpty() || !growth.bound || !growth.ripe(def.get().growth())) {
                    return List.of();
                }
                return List.of(new ProductionRecipe(vein, List.of(), List.of(def.get().yield().copy()), def.get().growth().yieldTicks()));
            }

            @Override
            public Optional<Identifier> active() {
                return recipes().isEmpty() ? Optional.empty() : Optional.of(vein);
            }
        };
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
        Optional<VeinDef> def = veinDef();
        tag.put("ShownYield", def.map(d -> d.yield().isEmpty() ? new CompoundTag() : ValueIo.saveStack(registries, d.yield())).orElseGet(CompoundTag::new));
        tag.putInt("ShownSteps", def.map(d -> d.growth().steps()).orElse(0));
        return tag;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GnomeHollowMenu(containerId, playerInventory, this);
    }
}
