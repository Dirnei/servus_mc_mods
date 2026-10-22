package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.shuttles.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

/**
 * Chest sized inventory of a rack bay. Clients only receive the sample: the
 * item types the rack holds the most of, computed on the server.
 */
public class HeavyRackBlockEntity extends StorageNodeBlockEntity implements MenuProvider {

    private static final int SYNC_INTERVAL = 10;

    /** The planks block whose texture the boards show. */
    public static final ModelProperty<Identifier> WOOD = new ModelProperty<>();

    /** Boards of a rack that remembers no wood. */
    public static final Identifier DEFAULT_WOOD = BuiltInRegistries.BLOCK.getKey(Blocks.OAK_PLANKS);

    private final ItemSlots items = new ItemSlots(ShuttlesStatsManager.INSTANCE.rackSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    private List<ItemStack> displayed = List.of();
    private boolean syncPending;
    /** The planks block the rack was crafted from, or null for oak. */
    private Identifier wood;
    private int syncTimer;
    /** Whether a rail stood in front of this bay at the last look; not saved, set on load. */
    private boolean frontRail;

    public HeavyRackBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.HEAVY_RACK.get(), pos, state);
    }

    @Override
    public boolean zonable() {
        return true;
    }

    /** The bay's slots, read and filled by the warehouse's shuttles. */
    @Override
    public ItemView stock() {
        return new ItemView(items);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            frontRail = HeavyRackBlock.railInFront(level, worldPosition);
        }
    }

    /**
     * Whether the rail in front of this bay changed since the last look; remembers
     * the new state. Called by the block when a neighbour changes.
     */
    boolean frontRailChanged() {
        boolean now = level != null && HeavyRackBlock.railInFront(level, worldPosition);
        if (now == frontRail) {
            return false;
        }
        frontRail = now;
        return true;
    }

    /**
     * Whether the rack is a chest for hoppers and pipes: only while no
     * warehouse rail stands in front of any bay of its section. Looked up live,
     * so a stale flag can never open a docked rack.
     */
    public boolean standalone() {
        if (level == null) {
            return true;
        }
        List<BlockPos> bays = HeavyRackBlock.section(level, worldPosition);
        int[] offsets = new int[bays.size()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = i;
        }
        return !RackDock.closed(offsets, i -> HeavyRackBlock.railInFront(level, bays.get(i)));
    }

    /** A rack is its own node kind: it links only to rails on its front and to its own section. */
    @Override
    public WarehouseConnectionRule.NodeKind kind() {
        return WarehouseConnectionRule.NodeKind.RACK;
    }

    /** Another rack is linked only when this bay joins towards it and both face the same way. */
    @Override
    public boolean linksTowards(Side side, StorageNodeBlockEntity neighbour) {
        if (!(neighbour instanceof HeavyRackBlockEntity)) {
            return true;
        }
        BlockState state = getBlockState();
        BlockState other = neighbour.getBlockState();
        if (!(state.getBlock() instanceof HeavyRackBlock) || !(other.getBlock() instanceof HeavyRackBlock)
                || state.getValue(HeavyRackBlock.FACING) != other.getValue(HeavyRackBlock.FACING)) {
            return false;
        }
        Direction facing = state.getValue(HeavyRackBlock.FACING);
        Direction towards = Conversions.toDirection(side);
        if (towards == facing.getCounterClockWise()) {
            return state.getValue(HeavyRackBlock.LEFT);
        }
        if (towards == facing.getClockWise()) {
            return state.getValue(HeavyRackBlock.RIGHT);
        }
        return false;
    }

    /** Rails link and dock only on the front. */
    @Override
    public boolean acceptsRail(Side side) {
        return side == Conversions.toSide(getBlockState().getValue(HeavyRackBlock.FACING));
    }

    /** The planks block id whose texture the boards show; oak when none or unknown. */
    public Identifier wood() {
        return orOak(wood);
    }

    /** The wood a rack item carries; oak when none or unknown. */
    public static Identifier woodOf(ItemStack stack) {
        return orOak(stack.get(ServusShuttlesComponents.RACK_WOOD.get()));
    }

    private static Identifier orOak(Identifier id) {
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? id : DEFAULT_WOOD;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(WOOD, wood()).build();
    }

    public ItemSlots items() {
        return items;
    }

    public boolean isEmpty() {
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The sample shown on the front: one item stack per displayed type, largest total first. */
    public List<ItemStack> displayed() {
        return displayed;
    }

    /** Empties every slot without dropping anything. */
    public void clear() {
        for (int i = 0; i < items.getSlots(); i++) {
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private void changed() {
        setChanged();
        syncPending = true;
        postDirty();
        if (level != null && !level.isClientSide()) {
            displayed = sample();
        }
    }

    private List<ItemStack> sample() {
        Map<ItemKey, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                counts.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
            }
        }
        List<ItemStack> result = new ArrayList<>();
        for (ItemKey key : TopCounts.pick(counts, ShuttlesStatsManager.INSTANCE.rackDisplayCount())) {
            result.add(key.stack(1));
        }
        return List.copyOf(result);
    }

    @Override
    public Component getDisplayName() {
        Component name = Component.translatable(getBlockState().getBlock().getDescriptionId());
        return zone().isEmpty() ? name : name.copy().append(" (" + zone() + ")");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return HeavyRackMenu.forSection(containerId, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeavyRackBlockEntity rack) {
        if (!rack.syncPending) {
            return;
        }
        if (++rack.syncTimer < SYNC_INTERVAL) {
            return;
        }
        rack.syncTimer = 0;
        rack.syncPending = false;
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        wood = input.get(ServusShuttlesComponents.RACK_WOOD.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (wood != null) {
            builder.set(ServusShuttlesComponents.RACK_WOOD.get(), wood);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Material");
    }

    private void writeWood(CompoundTag tag) {
        if (wood != null) {
            tag.putString("Material", wood.toString());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Items", items.serializeNBT(registries));
        writeWood(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        if (tag.contains("Items")) {
            items.fillFrom(registries, tag.getCompoundOrEmpty("Items"));
            displayed = sample();
        }
        if (tag.contains("Display")) {
            List<ItemStack> list = new ArrayList<>();
            for (Tag entry : tag.getListOrEmpty("Display")) {
                ItemStack stack = ValueIo.parseStack(registries, (CompoundTag) entry);
                if (!stack.isEmpty()) {
                    list.add(stack);
                }
            }
            displayed = List.copyOf(list);
        }
        Identifier loaded = tag.contains("Material") ? Identifier.tryParse(input.getStringOr("Material", "")) : null;
        boolean woodChanged = !Objects.equals(loaded, wood);
        wood = loaded;
        if (woodChanged && level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeZone(tag, registries);
        ListTag list = new ListTag();
        for (ItemStack stack : displayed) {
            list.add(ValueIo.saveStack(registries, stack));
        }
        tag.put("Display", list);
        writeWood(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Drops the bay's items on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) {
            return;
        }
        items.dropAll(level, pos);
    }
}
