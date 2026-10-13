package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * A warehouse station: a dock node with one item buffer, item sides relative
 * to its facing, an activity lamp and the wood of its recipe. The front is
 * never an item side. The buffer drops when the block is removed.
 */
public abstract class StationBlockEntity extends StorageNodeBlockEntity implements WoodHolder, MenuProvider, SidedBlockEntity {

    private static final String SIDES = "Sides";

    private final WoodSlot woodSlot = new WoodSlot(this);
    private final SideConfig sides;

    protected StationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SideConfig sides) {
        super(type, pos, state);
        this.sides = sides;
        sides.lock(RelativeSide.FRONT, SideMode.DISABLED);
    }

    /** The stacks the station holds. */
    public abstract ItemSlots buffer();

    @Override
    public NodeKind kind() {
        return NodeKind.STATION;
    }

    @Override
    public SideConfig sideConfig() {
        return sides;
    }

    @Override
    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(StationBlock.FACING) ? state.getValue(StationBlock.FACING) : Direction.NORTH;
    }

    @Override
    public SideMode cycleSide(RelativeSide side) {
        SideMode mode = sides.cycle(side);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        return mode;
    }

    public boolean isEmpty() {
        ItemSlots buffer = buffer();
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Drops every stack in the buffer at the station. */
    public void dropContents(Level level, BlockPos pos) {
        buffer().dropAll(level, pos);
    }

    /** How many of the key the slots hold. */
    protected static int countOf(ItemSlots slots, ItemKey key) {
        int count = 0;
        for (int i = 0; i < slots.getSlots(); i++) {
            ItemStack stack = slots.getStackInSlot(i);
            if (!stack.isEmpty() && ItemKey.of(stack).equals(key)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** The lang key suffix for a station without a working warehouse, empty while it is online. */
    public String offlineReason() {
        Optional<WarehouseNetwork> network = network();
        return network.isEmpty() ? "not_connected" : switch (network.get().status()) {
            case NO_CONTROLLER -> "no_controller";
            case MULTIPLE_CONTROLLERS -> "multiple_controllers";
            case ONLINE -> "";
        };
    }

    /** Sets the activity lamp when it shows something else. Server side. */
    protected void setActivity(StorageNodeBlocks.Activity activity) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(StorageNodeBlocks.ACTIVITY) && state.getValue(StorageNodeBlocks.ACTIVITY) != activity) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ACTIVITY, activity), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** Saves the wood and the sides; subclasses add their slots to the tag. */
    protected void saveStation(ValueOutput output, CompoundTag tag) {
        woodSlot.save(output);
        tag.putByteArray(SIDES, sides.toBytes());
    }

    /** Loads the wood and the sides saved by {@link #saveStation}. */
    protected void loadStation(ValueInput input, CompoundTag tag) {
        woodSlot.load(input);
        if (tag.contains(SIDES)) {
            sides.fromBytes(tag.getByteArray(SIDES).orElse(new byte[0]));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        tag.putByteArray(SIDES, sides.toBytes());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        WoodSlot.removeFromTag(output);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }

    /** Drops the buffer on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            dropContents(level, pos);
        }
    }
}
