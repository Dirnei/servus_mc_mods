package io.github.dirnei.mc.servus.delicacies.stove;

import io.github.dirnei.mc.servus.delicacies.firebox.Firebox;
import io.github.dirnei.mc.servus.delicacies.firebox.FuelFire;
import io.github.dirnei.mc.servus.delicacies.firebox.HeatSource;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenSlots;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * A Stove Tile. In a formed stove every tile knows its master, the tile with the door; the master keeps the row, the
 * fuel slot and the fire, and is the {@link Firebox} every tile offers to the cooking spot on top of it.
 */
public class StoveTileBlockEntity extends BlockEntity {

    private @Nullable BlockPos masterOffset;
    private Direction along = Direction.EAST;
    private int from;
    private int length;
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
        return master == null ? null : master.firebox;
    }

    /** The fuel slot as automation sees it from this tile, or null on a loose tile. */
    public @Nullable ResourceHandler<ItemResource> fuelCapability() {
        StoveTileBlockEntity master = master();
        return master == null ? null : master.fuelView;
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
        fire.restore(0, 0);
        demanded = false;
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
        fuel.dropAll(level, worldPosition);
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

    /** The master's tick: the fire burns, and every tile shows whether it is lit. */
    void serverTick() {
        boolean wasHot = heat.hot();
        heat.tick(demanded);
        demanded = false;
        if (heat.hot()) {
            setChanged();
        }
        if (wasHot != heat.hot()) {
            showLit(heat.hot());
        }
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

    /** Puts {@code stack} into the master's fuel slot; returns what did not fit. */
    public ItemStack addFuel(ItemStack stack) {
        StoveTileBlockEntity master = master();
        if (master == null || !isFuel(stack)) {
            return stack;
        }
        return master.fuel.insertItem(0, stack, false);
    }

    /** What an empty hand click tells: lit or cold, and the fuel left. */
    public Component status() {
        StoveTileBlockEntity master = master();
        if (master == null) {
            return Component.translatable("message.servus_delicacies.stove.loose");
        }
        String fire = master.heat.hot() ? "lit" : "cold";
        ItemStack stack = master.fuel.getStackInSlot(0);
        if (stack.isEmpty()) {
            return Component.translatable("message.servus_delicacies.stove." + fire + ".no_fuel");
        }
        return Component.translatable("message.servus_delicacies.stove." + fire, stack.getCount(), stack.getHoverName());
    }

    /** The master's fuel stack; empty on other tiles. */
    public ItemStack fuelStack() {
        return isMaster() ? fuel.getStackInSlot(0) : ItemStack.EMPTY;
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
            output.putInt("BurnLeft", fire.burnLeft());
            output.putInt("BurnTotal", fire.burnTotal());
            fuel.save(output, "Fuel");
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        masterOffset = input.read("Master", BlockPos.CODEC).orElse(null);
        along = Direction.from3DDataValue(input.getIntOr("Along", Direction.EAST.get3DDataValue()));
        from = input.getIntOr("From", 0);
        length = input.getIntOr("Length", 0);
        fire.restore(input.getIntOr("BurnLeft", 0), input.getIntOr("BurnTotal", 0));
        fuel.load(input, "Fuel");
    }
}
