package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.delicacies.firebox.Firebox;
import io.github.dirnei.mc.servus.delicacies.firebox.FuelFire;
import io.github.dirnei.mc.servus.delicacies.firebox.HeatSource;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * A part of the Brewhouse, brick or kettle block. In a formed Brewhouse every part knows its master, the front left
 * brick; the master keeps the firebox, the Brew Kettle and the two kiln trays, and ticks them.
 */
public class BrewhouseBlockEntity extends BlockEntity {

    private @Nullable BlockPos masterOffset;
    private boolean demanded;

    private final ItemSlots fuel = new ItemSlots(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FuelFire fire = new FuelFire(this::burnNextFuel);
    /** What heats this firebox; a later heat source on FE takes the fire's place here. */
    private final HeatSource heat = fire;
    private final Firebox firebox = new Firebox() {
        @Override
        public boolean isHot() {
            return heat.hot();
        }

        @Override
        public void demandHeat() {
            demanded = true;
        }
    };
    private final KitchenSlots fuelView = new KitchenSlots(fuel, (slot, stack) -> isFuel(stack), (slot, stack) -> !isFuel(stack));
    private final BrewKettle kettle = new BrewKettle(this::getLevel, this::setChanged);
    private final KilnTray[] trays = {
            new KilnTray(this::getLevel, this::setChanged, look -> showTray(Cell.KILN_FRONT, look)),
            new KilnTray(this::getLevel, this::setChanged, look -> showTray(Cell.KILN_BACK, look))};
    private boolean brewing;

    public BrewhouseBlockEntity(BlockPos pos, BlockState state) {
        super(ServusBreweryBlockEntities.BREWHOUSE.get(), pos, state);
    }

    public boolean formed() {
        return masterOffset != null;
    }

    public boolean isMaster() {
        return BlockPos.ZERO.equals(masterOffset);
    }

    /** The cell this part takes, from its block state; null when loose. */
    public @Nullable Cell cell() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BrewhousePartBlock block) || !state.getValue(BrewhousePartBlock.FORMED)) {
            return null;
        }
        return block.part(state).cell();
    }

    /** The master of the Brewhouse this part belongs to, while it still counts this part in; else null. */
    public @Nullable BrewhouseBlockEntity master() {
        if (masterOffset == null || level == null) {
            return null;
        }
        BlockPos master = worldPosition.offset(masterOffset);
        if (!level.isLoaded(master) || !(level.getBlockEntity(master) instanceof BrewhouseBlockEntity part)
                || !part.isMaster() || !part.members().contains(worldPosition)) {
            return null;
        }
        return part;
    }

    /** Where {@code cell} of this master's Brewhouse lies. */
    public BlockPos cellPos(Cell cell) {
        Direction front = getBlockState().getValue(BrewhousePartBlock.FACING);
        return worldPosition.relative(front.getCounterClockWise(), cell.column()).relative(front.getOpposite(), cell.row())
                .above(cell.layer());
    }

    /** Every part of the Brewhouse, master included; only meaningful on the master. */
    public List<BlockPos> members() {
        List<BlockPos> members = new ArrayList<>(Cell.values().length);
        for (Cell cell : Cell.values()) {
            members.add(cellPos(cell));
        }
        return members;
    }

    void becomeMaster() {
        masterOffset = BlockPos.ZERO;
        setChanged();
    }

    void joinMaster(BlockPos master) {
        masterOffset = master.subtract(worldPosition);
        setChanged();
    }

    private void leave() {
        masterOffset = null;
        fire.restore(0, 0);
        demanded = false;
        brewing = false;
        setChanged();
    }

    /** Unforms this master's Brewhouse: every part but {@code removed} becomes loose, and what it holds drops here. */
    void unform(@Nullable BlockPos removed) {
        if (level == null || !isMaster()) {
            return;
        }
        List<BlockPos> members = members();
        fuel.dropAll(level, worldPosition);
        kettle.dropAll(level, worldPosition);
        for (KilnTray tray : trays) {
            tray.dropAll(level, worldPosition);
        }
        for (BlockPos at : members) {
            if (at.equals(removed) || at.equals(worldPosition) || !level.isLoaded(at)) {
                continue;
            }
            if (level.getBlockEntity(at) instanceof BrewhouseBlockEntity part && worldPosition.equals(part.masterPosition())) {
                part.leave();
                loosen(level, at);
            }
        }
        leave();
        if (!worldPosition.equals(removed)) {
            loosen(level, worldPosition);
        }
        for (BlockPos at : members) {
            if (level.isLoaded(at)) {
                level.invalidateCapabilities(at);
            }
        }
    }

    /** Turns a formed part that lost its master back into a loose one. */
    void unformStray() {
        if (level == null) {
            return;
        }
        leave();
        loosen(level, worldPosition);
        level.invalidateCapabilities(worldPosition);
    }

    private static void loosen(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BrewhousePartBlock) {
            level.setBlock(pos, state.getBlock().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private @Nullable BlockPos masterPosition() {
        return masterOffset == null ? null : worldPosition.offset(masterOffset);
    }

    // ---------- the master's work ----------

    /** The master's tick: kettle and trays ask for heat and work, the fire burns, and the parts show it. */
    void serverTick() {
        boolean wasHot = heat.hot();
        boolean nowBrewing = kettle.tick(firebox);
        for (KilnTray tray : trays) {
            tray.tick(firebox);
        }
        heat.tick(demanded);
        demanded = false;
        if (heat.hot()) {
            setChanged();
        }
        if (wasHot != heat.hot()) {
            showLit(heat.hot());
        }
        if (nowBrewing != brewing) {
            brewing = nowBrewing;
            showBrewing(nowBrewing);
        }
    }

    private void showLit(boolean lit) {
        for (Cell cell : Cell.values()) {
            if (cell.kind() == BrewhouseLayout.Kind.BRICK) {
                setOn(cell, BrewhouseBricksBlock.LIT, lit);
            }
        }
    }

    private void showBrewing(boolean on) {
        for (Cell cell : Cell.values()) {
            if (cell.kind() == BrewhouseLayout.Kind.KETTLE) {
                setOn(cell, BrewKettleBlock.BREWING, on);
            }
        }
    }

    private void showTray(Cell cell, TrayLook look) {
        if (level == null || level.isClientSide() || !isMaster()) {
            return;
        }
        BlockPos at = cellPos(cell);
        BlockState state = level.getBlockState(at);
        if (state.hasProperty(BrewhouseBricksBlock.TRAY) && state.getValue(BrewhouseBricksBlock.TRAY) != look) {
            level.setBlock(at, state.setValue(BrewhouseBricksBlock.TRAY, look), Block.UPDATE_CLIENTS);
        }
    }

    private void setOn(Cell cell, BooleanProperty property, boolean value) {
        if (level == null) {
            return;
        }
        BlockPos at = cellPos(cell);
        BlockState state = level.getBlockState(at);
        if (state.hasProperty(property) && state.getValue(property) != value) {
            level.setBlock(at, state.setValue(property, value), Block.UPDATE_CLIENTS);
        }
    }

    private int burnNextFuel() {
        ItemStack stack = fuel.getStackInSlot(0);
        if (stack.isEmpty() || level == null) {
            return 0;
        }
        int ticks = stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
        if (ticks <= 0) {
            return 0;
        }
        ItemStackTemplate remainder = stack.getCraftingRemainder();
        ItemStack left = stack.copy();
        left.shrink(1);
        fuel.setStackInSlot(0, left.isEmpty() && remainder != null ? remainder.create() : left);
        return ticks;
    }

    public boolean isFuel(ItemStack stack) {
        return level != null && !stack.isEmpty() && stack.getBurnTime(null, level.fuelValues()) > 0;
    }

    // ---------- what the parts offer ----------

    /** The firebox of this part's Brewhouse, offered on every formed brick; null elsewhere. */
    public @Nullable Firebox firebox() {
        Cell cell = cell();
        BrewhouseBlockEntity master = master();
        return cell == null || master == null || cell.kind() != BrewhouseLayout.Kind.BRICK ? null : master.firebox;
    }

    /** The item handler of this part: fuel under the kettle, the tray on a kiln brick, ingredients on a kettle block. */
    public @Nullable ResourceHandler<ItemResource> itemCapability() {
        Cell cell = cell();
        BrewhouseBlockEntity master = master();
        if (cell == null || master == null) {
            return null;
        }
        if (cell.isKiln()) {
            return master.tray(cell).slot();
        }
        return cell.underKettle() ? master.fuelView : master.kettle.items();
    }

    /** The fluid handler of a kettle block: water in, wort out; null elsewhere. */
    public @Nullable ResourceHandler<FluidResource> fluidCapability() {
        Cell cell = cell();
        BrewhouseBlockEntity master = master();
        return cell == null || master == null || cell.kind() != BrewhouseLayout.Kind.KETTLE ? null : master.kettle.fluids();
    }

    /** The Brew Kettle of this part's Brewhouse, or null on a loose part. */
    public @Nullable BrewKettle kettle() {
        BrewhouseBlockEntity master = master();
        return master == null ? null : master.kettle;
    }

    /** The kiln tray of this kiln brick, or null on any other part. */
    public @Nullable KilnTray tray() {
        Cell cell = cell();
        BrewhouseBlockEntity master = master();
        return cell == null || master == null || !cell.isKiln() ? null : master.tray(cell);
    }

    private KilnTray tray(Cell kilnCell) {
        return trays[kilnCell == Cell.KILN_FRONT ? 0 : 1];
    }

    /** Puts {@code stack} into the master's fuel slot; returns what did not fit. */
    public ItemStack addFuel(ItemStack stack) {
        BrewhouseBlockEntity master = master();
        if (master == null || !isFuel(stack)) {
            return stack;
        }
        return master.fuel.insertItem(0, stack, false);
    }

    /** The master's fuel stack; empty on other parts. */
    public ItemStack fuelStack() {
        return isMaster() ? fuel.getStackInSlot(0) : ItemStack.EMPTY;
    }

    /** Whether the fire of this part's Brewhouse burns. */
    public boolean hot() {
        BrewhouseBlockEntity master = master();
        return master != null && master.heat.hot();
    }

    /** What an empty hand or the carrot tells on a brick: lit or cold, and the fuel left. */
    public Component fireStatus() {
        BrewhouseBlockEntity master = master();
        if (master == null) {
            return Component.translatable("message.servus_brewery.brewhouse.loose");
        }
        String state = master.heat.hot() ? "lit" : "cold";
        ItemStack stack = master.fuel.getStackInSlot(0);
        if (stack.isEmpty()) {
            return Component.translatable("message.servus_brewery.brewhouse." + state + ".no_fuel");
        }
        return Component.translatable("message.servus_brewery.brewhouse." + state, stack.getCount(), stack.getHoverName());
    }

    /** What an empty hand or the carrot tells on a kettle block: water, wort and whether it brews. */
    public Component kettleStatus() {
        BrewhouseBlockEntity master = master();
        if (master == null) {
            return Component.translatable("message.servus_brewery.brewhouse.loose");
        }
        FluidStack wort = master.kettle.wort();
        Component wortName = wort.isEmpty() ? Component.translatable("message.servus_brewery.kettle.no_wort") : wort.getHoverName();
        return Component.translatable("message.servus_brewery.kettle." + (master.brewing ? "brewing" : "idle"),
                master.kettle.water().getAmount(), wort.getAmount(), wortName);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || masterOffset == null) {
            return;
        }
        BrewhouseBlockEntity master = isMaster() ? this : master();
        if (master != null) {
            master.unform(pos);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (masterOffset == null) {
            return;
        }
        output.store("Master", BlockPos.CODEC, masterOffset);
        if (isMaster()) {
            output.putInt("BurnLeft", fire.burnLeft());
            output.putInt("BurnTotal", fire.burnTotal());
            fuel.save(output, "Fuel");
            kettle.save(output);
            trays[0].save(output, "Tray0");
            trays[1].save(output, "Tray1");
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        masterOffset = input.read("Master", BlockPos.CODEC).orElse(null);
        fire.restore(input.getIntOr("BurnLeft", 0), input.getIntOr("BurnTotal", 0));
        fuel.load(input, "Fuel");
        kettle.load(input);
        trays[0].load(input, "Tray0");
        trays[1].load(input, "Tray1");
    }
}
