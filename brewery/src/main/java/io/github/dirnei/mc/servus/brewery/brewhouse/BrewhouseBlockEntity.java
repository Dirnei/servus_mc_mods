package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.delicacies.firebox.Firebox;
import io.github.dirnei.mc.servus.delicacies.firebox.FireboxHeating;
import io.github.dirnei.mc.servus.delicacies.firebox.HeatingStats;
import io.github.dirnei.mc.servus.delicacies.firebox.MasterFirebox;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * A part of the Brewhouse, brick or kettle block. In a formed Brewhouse every part knows its master, the front left
 * brick; the master keeps the firebox with its Heating Element, the Brew Kettle and the two kiln trays, and ticks them.
 */
public class BrewhouseBlockEntity extends BlockEntity {

    /** The Brewhouse's id in {@code data/<ns>/heating/}. */
    public static final Identifier HEATING = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "brewhouse");
    /** The Brewhouse's Heating Element numbers when no datapack names it. */
    public static final HeatingStats HEATING_DEFAULTS = new HeatingStats(40, 8000, 300, 1600, 0.6F);
    /** The brick whose outer end face carries the power port. */
    public static final Cell PORT_CELL = Cell.KILN_BACK;
    /** The front brick that carries the thermometer. */
    public static final Cell GAUGE_CELL = Cell.KETTLE_FRONT_RIGHT;

    private @Nullable BlockPos masterOffset;

    private final MasterFirebox box = new MasterFirebox(this, HEATING, HEATING_DEFAULTS, this::showLit);
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
        box.putOut();
        brewing = false;
        setChanged();
    }

    /** Unforms this master's Brewhouse: every part but {@code removed} becomes loose, and what it holds drops here. */
    void unform(@Nullable BlockPos removed) {
        if (level == null || !isMaster()) {
            return;
        }
        List<BlockPos> members = members();
        box.dropAll(level, worldPosition);
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
        boolean nowBrewing = kettle.tick(box.firebox());
        for (KilnTray tray : trays) {
            tray.tick(box.firebox());
        }
        box.tick();
        if (nowBrewing != brewing) {
            brewing = nowBrewing;
            showBrewing(nowBrewing);
        }
    }

    /** Installs a Heating Element into this part's Brewhouse; false on a loose part or when it has one. */
    public boolean installElement() {
        BrewhouseBlockEntity master = master();
        if (master == null || !master.box.installElement()) {
            return false;
        }
        master.elementChanged();
        return true;
    }

    /** Takes the Heating Element out of this part's Brewhouse; empty when there is none. */
    public ItemStack takeElement() {
        BrewhouseBlockEntity master = master();
        if (master == null) {
            return ItemStack.EMPTY;
        }
        ItemStack element = master.box.takeElement();
        if (!element.isEmpty()) {
            master.elementChanged();
        }
        return element;
    }

    /** Whether this part's Brewhouse holds a Heating Element. */
    public boolean hasElement() {
        BrewhouseBlockEntity master = master();
        return master != null && master.box.hasElement();
    }

    private void elementChanged() {
        if (level == null) {
            return;
        }
        boolean element = box.hasElement();
        for (Cell cell : Cell.values()) {
            if (cell.kind() == BrewhouseLayout.Kind.BRICK) {
                setOn(cell, BrewhouseBricksBlock.HEATER, element);
            }
        }
        showLit(box.hot());
        for (BlockPos at : members()) {
            level.invalidateCapabilities(at);
        }
        setChanged();
        box.sendGauge();
    }

    /** The heating of this part, meaningful on the master: for tests and the gauge. */
    public FireboxHeating heating() {
        return box.heating();
    }

    /** The heat the gauge shows now, on the client; read on the master. */
    public float shownHeat(float partialTick) {
        return level == null ? 0 : box.heating().shown(level.getGameTime(), partialTick);
    }

    /**
     * The heat the gauge on this formed part shows, on the client: the master is found from the part's cell and facing,
     * since clients do not hear of the master's offset.
     */
    public float shownGaugeHeat(float partialTick) {
        Cell cell = cell();
        if (cell == null || level == null) {
            return 0;
        }
        Direction front = getBlockState().getValue(BrewhousePartBlock.FACING);
        BlockPos master = worldPosition.relative(front.getCounterClockWise(), -cell.column())
                .relative(front.getOpposite(), -cell.row()).below(cell.layer());
        return level.getBlockEntity(master) instanceof BrewhouseBlockEntity part ? part.shownHeat(partialTick) : 0;
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

    public boolean isFuel(ItemStack stack) {
        return MasterFirebox.isFuel(level, stack);
    }

    // ---------- what the parts offer ----------

    /** The firebox of this part's Brewhouse, offered on every formed brick; null elsewhere. */
    public @Nullable Firebox firebox() {
        Cell cell = cell();
        BrewhouseBlockEntity master = master();
        return cell == null || master == null || cell.kind() != BrewhouseLayout.Kind.BRICK ? null : master.box.firebox();
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
        return cell.underKettle() ? master.box.fuelView() : master.kettle.items();
    }

    /** The FE buffer on {@code side} of this part: only the outer end face of {@link #PORT_CELL}, with the element in. */
    public @Nullable EnergyHandler energyCapability(@Nullable Direction side) {
        Cell cell = cell();
        BrewhouseBlockEntity master = master();
        if (cell != PORT_CELL || master == null) {
            return null;
        }
        Direction port = getBlockState().getValue(BrewhousePartBlock.FACING).getCounterClockWise();
        return side == null || side == port ? master.box.heating().energy() : null;
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
        return master == null ? stack : master.box.addFuel(stack);
    }

    /** The master's fuel stack; empty on other parts. */
    public ItemStack fuelStack() {
        return isMaster() ? box.fuelStack() : ItemStack.EMPTY;
    }

    /** Whether the fire of this part's Brewhouse burns. */
    public boolean hot() {
        BrewhouseBlockEntity master = master();
        return master != null && master.box.hot();
    }

    /** What an empty hand or the carrot tells on a brick: lit or cold, and the fuel left. */
    public Component fireStatus() {
        BrewhouseBlockEntity master = master();
        if (master == null) {
            return Component.translatable("message.servus_brewery.brewhouse.loose");
        }
        return master.box.status("message.servus_brewery.brewhouse");
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
            box.save(output);
            kettle.save(output);
            trays[0].save(output, "Tray0");
            trays[1].save(output, "Tray1");
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        masterOffset = input.read("Master", BlockPos.CODEC).orElse(null);
        box.load(input);
        kettle.load(input);
        trays[0].load(input, "Tray0");
        trays[1].load(input, "Tray1");
    }

    /** Clients get only what the thermometer needs. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return ValueIo.write(this, registries, box.heating()::saveGauge);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
