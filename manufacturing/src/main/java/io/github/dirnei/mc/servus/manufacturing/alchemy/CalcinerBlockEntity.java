package io.github.dirnei.mc.servus.manufacturing.alchemy;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.BurnerMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import net.minecraft.server.level.ServerLevel;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPorts;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurificationRecipe;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The controller of the Calciner structure: burns its input into Salt, Sulfur and Mercury over the
 * Firebox's fire while formed, and pushes each principle out of its own port. It has no GUI.
 */
public class CalcinerBlockEntity extends BurnerMachineBlockEntity implements OwnedMachine {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    private static final int PUSH_INTERVAL = 10;
    /** Salt per Ash when no purification recipe takes Ash. */
    private static final int FALLBACK_SALT_PER_ASH = 100;

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), true, this::setChanged);
    private final TeamOwnership ownership = new TeamOwnership(this::setChanged);
    private final ItemView inputPort = new CalcinerPorts.InsertOnly(inventory, SLOT_INPUT, this::calcinable);
    private final ItemView ashPort = new CalcinerPorts.AshPort(this);
    private int saltPerAsh = FALLBACK_SALT_PER_ASH;
    private RecipeManager saltPerAshFrom;
    private final ItemView fuelPort = new CalcinerPorts.InsertOnly(inventory, SLOT_FUEL, stack -> stack.getBurnTime(null) > 0);
    private int pushCooldown;
    /** The lit state last given to the Firebox and the Hood. */
    private boolean partsLit;
    /** The fill of each principle in sixteenths, -1 for empty, as last sent to the clients that draw it. */
    private final int[] drawnFill = {-1, -1, -1};
    /** The input item as last sent to the clients, null before the first send. */
    private net.minecraft.world.item.Item sentInput;
    /** On the client: the matter shown in the crucible. */
    private ItemStack shownInput = ItemStack.EMPTY;

    public CalcinerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.CALCINER.get(), pos, state, 2);
        // Items enter only through the ports of the structure, never through a side of this block.
        for (RelativeSide side : RelativeSide.values()) {
            sideConfig().lock(side, SideMode.DISABLED);
        }
    }

    /** Whether the structure around this Calciner is formed; only then it works. */
    public boolean formed() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof CalcinerBlock && state.getValue(CalcinerBlock.FORMED);
    }

    /** The mouth: takes calcinable matter, gives nothing back. */
    public ItemView inputPort() {
        return inputPort;
    }

    /** The Firebox: takes furnace fuel, gives nothing back. */
    public ItemView fuelPort() {
        return fuelPort;
    }

    // Owner

    @Override
    public Optional<UUID> owner() {
        return ownership.owner();
    }

    @Override
    public void setOwner(UUID player) {
        ownership.setOwner(level, player);
    }

    public void setTeam(UUID team) {
        ownership.setTeam(team);
    }

    /** The owning team, or {@code null} for a machine nobody placed. */
    public UUID team() {
        return ownership.team();
    }

    public PrincipleTanks tanks() {
        return tanks;
    }

    private Optional<CalcinationRecipe> currentRecipe() {
        ItemStack stack = inventory.getStackInSlot(SLOT_INPUT);
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.recipeAccess().getRecipeFor(ServusManufacturingRecipes.CALCINATION.get(), new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value);
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
    protected int fuelSlot() {
        return SLOT_FUEL;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return calcinable(stack);
    }

    /** True when a calcination recipe takes {@code stack}. */
    public boolean calcinable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return level.recipeAccess().getAllRecipesFor(ServusManufacturingRecipes.CALCINATION.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    /** Puts calcinable matter into the input, returning what did not fit. */
    public ItemStack insertInput(ItemStack stack) {
        return calcinable(stack) ? inventory.insertItem(SLOT_INPUT, stack, false) : stack;
    }

    /** Puts furnace fuel into the fuel slot, returning what did not fit. */
    public ItemStack insertFuel(ItemStack stack) {
        return stack.getBurnTime(null) > 0 ? inventory.insertItem(SLOT_FUEL, stack, false) : stack;
    }

    /** The Ash Pan: gives the Salt as whole Ash, takes nothing. */
    public ItemView ashPort() {
        return ashPort;
    }

    /** The Salt one Ash is worth: what the first purification recipe that takes Ash makes from one. */
    public int saltPerAsh() {
        if (level == null) {
            return FALLBACK_SALT_PER_ASH;
        }
        RecipeManager recipes = level.recipeAccess();
        if (recipes != saltPerAshFrom) {
            ItemStack ash = new ItemStack(ServusManufacturingItems.ASH.get());
            saltPerAsh = recipes.getAllRecipesFor(ServusManufacturingRecipes.PURIFICATION.get()).stream()
                    .map(RecipeHolder::value)
                    .filter(recipe -> recipe.accepts(ash))
                    .map(PurificationRecipe::saltPerItem)
                    .filter(salt -> salt > 0)
                    .findFirst().orElse(FALLBACK_SALT_PER_ASH);
            saltPerAshFrom = recipes;
        }
        return saltPerAsh;
    }

    /** Whole Ash in the Ash Pan. */
    public int ashCount() {
        return AshCount.ash(tanks.amount(Principle.SALT), saltPerAsh());
    }

    /** Takes up to {@code count} Ash out of the Ash Pan and returns how many were taken. */
    public int takeAsh(int count, boolean simulate) {
        int per = saltPerAsh();
        int salt = AshCount.saltFor(count, tanks.amount(Principle.SALT), per);
        if (!simulate && salt > 0) {
            tanks.tank(Principle.SALT).drain(salt, false);
        }
        return salt / Math.max(1, per);
    }

    /** How far the current cycle is, 0 to 100. */
    public int progressPercent() {
        int ticks = workTicks();
        return ticks <= 0 || ticks == Integer.MAX_VALUE ? 0 : Math.min(100, progress * 100 / ticks);
    }

    /** Takes the whole input out. */
    public ItemStack takeInput() {
        ItemStack taken = inventory.getStackInSlot(SLOT_INPUT).copy();
        inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
        return taken;
    }

    @Override
    protected boolean canWork() {
        tanks.setCapacity(stats().fluidCapacity());
        return formed() && currentRecipe().map(recipe -> tanks.contents().fits(recipe.output(), tanks.capacity())).orElse(false);
    }

    @Override
    protected void finishWork() {
        currentRecipe().ifPresent(recipe -> {
            ItemStack used = inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
            tanks.insert(recipe.output());
            if (level instanceof ServerLevel server) {
                Journal.recordItem(server, team(), "calcine", used, used.getCount());
                for (Principle principle : Principle.values()) {
                    Journal.recordPrinciple(server, team(), principle, recipe.output().get(principle));
                }
            }
        });
    }

    @Override
    protected boolean tick() {
        boolean working = super.tick();
        if (level != null && formed() && working != partsLit) {
            partsLit = working;
            CalcinerStructure.light(level, worldPosition, getBlockState().getValue(MachineBlock.FACING), working);
        }
        if (level != null && ++pushCooldown >= PUSH_INTERVAL) {
            pushCooldown = 0;
            if (formed()) {
                CalcinerPorts.push(level, worldPosition, getBlockState().getValue(MachineBlock.FACING), tanks);
                syncFill();
            }
        }
        return working;
    }

    @Override
    protected int workTicks() {
        return currentRecipe().map(recipe -> Math.max(1, recipe.time())).orElse(Integer.MAX_VALUE);
    }

    /** On the client: the matter drawn in the crucible of the formed Calciner. */
    public ItemStack shownInput() {
        return shownInput;
    }

    /** Sends the tanks and the matter to the clients when a drawn fill level changed by a sixteenth or the matter changed. */
    private void syncFill() {
        net.minecraft.world.item.Item input = inventory.getStackInSlot(SLOT_INPUT).getItem();
        boolean changed = input != sentInput;
        sentInput = input;
        for (Principle principle : Principle.values()) {
            int amount = tanks.amount(principle);
            int fill = amount == 0 ? -1 : amount * 16 / Math.max(1, tanks.capacity());
            if (fill != drawnFill[principle.ordinal()]) {
                drawnFill[principle.ordinal()] = fill;
                changed = true;
            }
        }
        if (changed && level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    /** The clients draw the principles in the formed model. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Principles", ValueIo.saveStack(registries, tanks));
        tag.put("ShownInput", inventory.getStackInSlot(SLOT_INPUT).copyWithCount(1).saveOptional(registries));
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Principles", ValueIo.saveStack(registries, tanks));
        ownership.save(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        tanks.load(tag.getCompoundOrEmpty("Principles"));
        ownership.load(tag);
        if (tag.contains("ShownInput")) {
            shownInput = ValueIo.parseStack(registries, tag.getCompoundOrEmpty("ShownInput"));
        }
    }

    /** The Calciner has no GUI. */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
