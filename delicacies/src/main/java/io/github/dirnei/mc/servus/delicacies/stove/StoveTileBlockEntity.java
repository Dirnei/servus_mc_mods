package io.github.dirnei.mc.servus.delicacies.stove;

import io.github.dirnei.mc.servus.delicacies.firebox.Firebox;
import io.github.dirnei.mc.servus.delicacies.firebox.FireboxHeating;
import io.github.dirnei.mc.servus.delicacies.firebox.HeatingStats;
import io.github.dirnei.mc.servus.delicacies.firebox.HeatingStatsManager;
import io.github.dirnei.mc.servus.delicacies.firebox.MasterFirebox;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * A Stove Tile. In a formed stove every tile knows its master, the tile with the door; the master keeps the row, the
 * fuel slot, the fire and the Heating Element, and is the {@link Firebox} every tile offers to the cooking spot on top
 * of it. The tile with {@link StoveTileBlock#PORT} takes FE for the element on its outer end face.
 */
public class StoveTileBlockEntity extends BlockEntity {

    private @Nullable BlockPos masterOffset;
    private Direction along = Direction.EAST;
    private int from;
    private int length;

    private final MasterFirebox box =
            new MasterFirebox(this, HeatingStatsManager.TILED_STOVE, HeatingStats.TILED_STOVE, this::showLit);

    public StoveTileBlockEntity(BlockPos pos, BlockState state) {
        super(ServusDelicaciesBlockEntities.STOVE_TILE.get(), pos, state);
    }

    public boolean formed() {
        return masterOffset != null;
    }

    public boolean isMaster() {
        return BlockPos.ZERO.equals(masterOffset);
    }

    /** The master of the stove this tile belongs to, while it still counts this tile in its row; else null. */
    public @Nullable StoveTileBlockEntity master() {
        if (masterOffset == null || level == null) {
            return null;
        }
        BlockPos master = worldPosition.offset(masterOffset);
        if (!level.isLoaded(master) || !(level.getBlockEntity(master) instanceof StoveTileBlockEntity tile)
                || !tile.isMaster() || !tile.members().contains(worldPosition)) {
            return null;
        }
        return tile;
    }

    /** The firebox heating the spot above this tile, or null on a loose tile. */
    public @Nullable Firebox firebox() {
        StoveTileBlockEntity master = master();
        return master == null ? null : master.box.firebox();
    }

    /** The fuel slot as automation sees it from this tile, or null on a loose tile. */
    public @Nullable ResourceHandler<ItemResource> fuelCapability() {
        StoveTileBlockEntity master = master();
        return master == null ? null : master.box.fuelView();
    }

    /** The tiles of the row, master included; only meaningful on the master. */
    public List<BlockPos> members() {
        List<BlockPos> members = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            members.add(worldPosition.relative(along, from + i));
        }
        return members;
    }

    /** Makes this tile the master of the row from {@code from} to {@code from + length - 1} along {@code along}. */
    void becomeMaster(Direction along, int from, int length) {
        this.masterOffset = BlockPos.ZERO;
        this.along = along;
        this.from = from;
        this.length = length;
        setChanged();
    }

    void joinMaster(BlockPos master) {
        this.masterOffset = master.subtract(worldPosition);
        this.length = 0;
        setChanged();
    }

    private void leave() {
        masterOffset = null;
        length = 0;
        box.putOut();
        setChanged();
    }

    /**
     * Unforms the row of this master: every tile but {@code removed} goes back to a loose tile, and the fuel drops
     * at the master.
     */
    void unform(@Nullable BlockPos removed) {
        if (level == null || !isMaster()) {
            return;
        }
        List<BlockPos> members = members();
        box.dropAll(level, worldPosition);
        for (BlockPos at : members) {
            if (at.equals(removed) || !level.isLoaded(at)) {
                continue;
            }
            if (level.getBlockEntity(at) instanceof StoveTileBlockEntity tile && tile != this
                    && worldPosition.equals(tile.masterPosition())) {
                tile.leave();
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

    /** Turns a formed tile that lost its master back into a loose tile. */
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
        if (state.getBlock() instanceof StoveTileBlock) {
            level.setBlock(pos, state.getBlock().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private @Nullable BlockPos masterPosition() {
        return masterOffset == null ? null : worldPosition.offset(masterOffset);
    }

    /** The master's tick: the fire or the element heats, every tile shows whether it is lit, and clients see the gauge. */
    void serverTick() {
        box.tick();
    }

    /** Installs a Heating Element into this tile's stove; false on a loose tile or when it has one. */
    public boolean installElement() {
        StoveTileBlockEntity master = master();
        if (master == null || !master.box.installElement()) {
            return false;
        }
        master.elementChanged();
        return true;
    }

    /** Takes the Heating Element out of this tile's stove; empty when there is none. */
    public ItemStack takeElement() {
        StoveTileBlockEntity master = master();
        if (master == null) {
            return ItemStack.EMPTY;
        }
        ItemStack element = master.box.takeElement();
        if (!element.isEmpty()) {
            master.elementChanged();
        }
        return element;
    }

    /** Whether this tile's stove holds a Heating Element. */
    public boolean hasElement() {
        StoveTileBlockEntity master = master();
        return master != null && master.box.hasElement();
    }

    private void elementChanged() {
        if (level == null) {
            return;
        }
        boolean element = box.hasElement();
        for (BlockPos at : members()) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof StoveTileBlock && state.getValue(StoveTileBlock.HEATER) != element) {
                level.setBlock(at, state.setValue(StoveTileBlock.HEATER, element), Block.UPDATE_ALL);
            }
        }
        showLit(box.hot());
        for (BlockPos at : members()) {
            level.invalidateCapabilities(at);
        }
        setChanged();
        box.sendGauge();
    }

    /** The FE buffer as automation sees it on {@code side}: only the port face of the port tile, with the element in. */
    public @Nullable EnergyHandler energyCapability(@Nullable Direction side) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof StoveTileBlock) || !state.getValue(StoveTileBlock.PORT)) {
            return null;
        }
        if (side != null && side != StoveTileBlock.portFace(state)) {
            return null;
        }
        StoveTileBlockEntity master = master();
        return master == null ? null : master.box.heating().energy();
    }

    /** The heating of this tile, meaningful on the master: for tests and the gauge. */
    public FireboxHeating heating() {
        return box.heating();
    }

    /** The heat the gauge on this door tile shows now, on the client. */
    public float shownHeat(float partialTick) {
        return level == null ? 0 : box.heating().shown(level.getGameTime(), partialTick);
    }

    private void showLit(boolean lit) {
        if (level == null) {
            return;
        }
        for (BlockPos at : members()) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof StoveTileBlock && state.getValue(StoveTileBlock.LIT) != lit) {
                level.setBlock(at, state.setValue(StoveTileBlock.LIT, lit), Block.UPDATE_ALL);
            }
        }
    }

    public boolean isFuel(ItemStack stack) {
        return MasterFirebox.isFuel(level, stack);
    }

    /** Puts {@code stack} into the master's fuel slot; returns what did not fit. */
    public ItemStack addFuel(ItemStack stack) {
        StoveTileBlockEntity master = master();
        return master == null ? stack : master.box.addFuel(stack);
    }

    /** What an empty hand click tells: lit or cold, and the fuel left. */
    public Component status() {
        StoveTileBlockEntity master = master();
        if (master == null) {
            return Component.translatable("message.servus_delicacies.stove.loose");
        }
        return master.box.status("message.servus_delicacies.stove");
    }

    /** The master's fuel stack; empty on other tiles. */
    public ItemStack fuelStack() {
        return isMaster() ? box.fuelStack() : ItemStack.EMPTY;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || masterOffset == null) {
            return;
        }
        StoveTileBlockEntity master = isMaster() ? this : master();
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
            output.putInt("Along", along.get3DDataValue());
            output.putInt("From", from);
            output.putInt("Length", length);
            box.save(output);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        masterOffset = input.read("Master", BlockPos.CODEC).orElse(null);
        along = Direction.from3DDataValue(input.getIntOr("Along", Direction.EAST.get3DDataValue()));
        from = input.getIntOr("From", 0);
        length = input.getIntOr("Length", 0);
        box.load(input);
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
