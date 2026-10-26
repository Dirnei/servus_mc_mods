package io.github.dirnei.mc.servus.shuttles.station;

import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialHolder;
import io.github.dirnei.mc.servus.foundation.material.MaterialSlot;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.site.SiteName;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
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
public abstract class StationBlockEntity extends StorageNodeBlockEntity implements MaterialHolder, MenuProvider, SidedBlockEntity {

    private static final String SIDES = "Sides";
    private static final String NUMBER = "StationNumber";
    private static final String COUNTER = "StationCounter";
    private static final String NAME = "StationName";
    private static final String WAREHOUSE = "StationWarehouse";

    private final MaterialSlot materialSlot = new MaterialSlot(this);
    private final SideConfig sides;
    /** The number its warehouse gave, 0 while it has none. */
    private int number;
    private String name = "";
    /** The warehouse that gave the number. */
    private UUID numberedBy;

    protected StationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SideConfig sides) {
        super(type, pos, state);
        this.sides = sides;
        sides.lock(RelativeSide.FRONT, SideMode.DISABLED);
    }

    /** The stacks the station holds. */
    public abstract ItemSlots buffer();

    /** Which of the warehouse's station counters numbers this station. */
    public abstract StationCounters.Kind stationKind();

    /** The number its warehouse gave, 0 while it has none. */
    public int number() {
        return number;
    }

    /** The name the player gave, empty when unnamed. */
    public String name() {
        return name;
    }

    /** Whether the warehouse with that id has to give this station a number. */
    public boolean needsNumber(UUID warehouse) {
        return StationCounters.needsNumber(numberedBy, number, warehouse);
    }

    /** A saved number, or 0 when another kind's counter gave it, so the station is numbered again by its own. */
    private int ownNumber(int saved, String counter) {
        return StationCounters.counterOf(counter, stationKind()) == stationKind() ? Math.max(0, saved) : 0;
    }

    /** Takes a number from the warehouse with that id. Server side. */
    public void assignNumber(UUID warehouse, int given) {
        numberedBy = warehouse;
        number = given;
        changedLabel();
    }

    /** Sets the name after cleaning it like a site name; an empty name removes it. Returns the kept name. */
    public String rename(String typed) {
        String cleaned = SiteName.clean(typed);
        if (!cleaned.equals(name)) {
            name = cleaned;
            changedLabel();
        }
        return name;
    }

    private void changedLabel() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * How players read this station: "Outbound #3", "Compost Dock (#3)", the name
     * alone or the block name while it has no number. Resolved by the client.
     */
    public Component label() {
        return labelOf(name);
    }

    /** The label as if the station had no name: the hint of the name field. */
    public Component numberLabel() {
        return labelOf("");
    }

    private Component labelOf(String shownName) {
        StationLabel label = StationLabel.of(number, shownName);
        return switch (label.form()) {
            case NUMBERED_NAMED -> Component.translatable("station.servus_shuttles.label.named", label.name(), label.number());
            case NUMBERED -> Component.translatable("station.servus_shuttles.label." + stationKind().name().toLowerCase(Locale.ROOT), label.number());
            case NAMED -> Component.literal(label.name());
            case PLAIN -> Component.translatable(getBlockState().getBlock().getDescriptionId());
        };
    }

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
        materialSlot.save(output);
        tag.putByteArray(SIDES, sides.toBytes());
        if (number > 0) {
            output.putInt(NUMBER, number);
            output.putString(COUNTER, stationKind().name());
        }
        if (!name.isEmpty()) {
            output.putString(NAME, name);
        }
        if (numberedBy != null) {
            output.store(WAREHOUSE, UUIDUtil.CODEC, numberedBy);
        }
    }

    /** Loads the wood and the sides saved by {@link #saveStation}. */
    protected void loadStation(ValueInput input, CompoundTag tag) {
        materialSlot.load(input);
        if (tag.contains(SIDES)) {
            sides.fromBytes(tag.getByteArray(SIDES).orElse(new byte[0]));
        }
        number = ownNumber(input.getIntOr(NUMBER, 0), input.getStringOr(COUNTER, ""));
        name = SiteName.clean(input.getStringOr(NAME, ""));
        numberedBy = input.read(WAREHOUSE, UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public Component getDisplayName() {
        return label();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        materialSlot.save(tag);
        tag.putByteArray(SIDES, sides.toBytes());
        tag.putInt(NUMBER, number);
        tag.putString(COUNTER, stationKind().name());
        tag.putString(NAME, name);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public MaterialSlot materialSlot() {
        return materialSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        materialSlot.apply(input.get(MaterialContent.MATERIAL.get()));
        Integer carriedNumber = input.get(ServusShuttlesComponents.STATION_NUMBER.get());
        number = ownNumber(carriedNumber == null ? 0 : carriedNumber,
                input.getOrDefault(ServusShuttlesComponents.STATION_COUNTER.get(), ""));
        name = SiteName.clean(input.getOrDefault(ServusShuttlesComponents.STATION_NAME.get(), ""));
        numberedBy = input.get(ServusShuttlesComponents.STATION_WAREHOUSE.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        materialSlot.collect(builder);
        if (number > 0) {
            builder.set(ServusShuttlesComponents.STATION_NUMBER.get(), number);
            builder.set(ServusShuttlesComponents.STATION_COUNTER.get(), stationKind().name());
        }
        if (!name.isEmpty()) {
            builder.set(ServusShuttlesComponents.STATION_NAME.get(), name);
        }
        if (numberedBy != null) {
            builder.set(ServusShuttlesComponents.STATION_WAREHOUSE.get(), numberedBy);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        MaterialSlot.removeFromTag(output);
        output.discard(NUMBER);
        output.discard(COUNTER);
        output.discard(NAME);
        output.discard(WAREHOUSE);
    }

    @Override
    public ModelData getModelData() {
        return materialSlot.modelData();
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
