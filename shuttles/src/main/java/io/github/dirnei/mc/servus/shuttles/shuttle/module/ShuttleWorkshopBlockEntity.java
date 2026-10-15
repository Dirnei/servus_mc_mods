package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.BuildStep;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.HandBuild;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.NamePlate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * The workshop keeps one shuttle, the supplies for its next tier and the
 * state of a build by hand. Installing upgrades edits the shuttle in place;
 * a tier build uses the supplies and swaps the shuttle for the next tier's
 * item. While a build by hand runs, the shuttle and the supplies are locked.
 */
public class ShuttleWorkshopBlockEntity extends BlockEntity implements WoodHolder, MenuProvider {

    public static final int SUPPLY_SLOTS = 6;
    private static final Component TITLE = Component.translatable("container.servus_shuttles.shuttle_workshop");

    private final WoodSlot woodSlot = new WoodSlot(this);

    private final ItemSlots shuttle = new ItemSlots(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !handBuild && ShuttleItem.isShuttle(stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return handBuild ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemSlots supplies = new ItemSlots(SUPPLY_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !handBuild;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return handBuild ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Whether a build by hand runs. */
    private boolean handBuild;
    /** The step a build by hand is on; equal to the step count once it waits for its name. */
    private int buildStep;

    public ShuttleWorkshopBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.SHUTTLE_WORKSHOP.get(), pos, state);
    }

    public ItemSlots shuttle() {
        return shuttle;
    }

    public ItemSlots supplies() {
        return supplies;
    }

    /** The step a build by hand is on, kept within the steps of the sheet as the datapack has it now. */
    public int buildStep() {
        return HandBuild.clamp(buildStep, steps().size());
    }

    /** Whether a build by hand runs. */
    public boolean handBuild() {
        return handBuild;
    }

    /** The steps of a build by hand into the next tier, empty without a shuttle or at the top. */
    public List<BuildStep> steps() {
        ItemStack held = shuttle.getStackInSlot(0);
        return held.isEmpty() ? List.of() : ShuttlesStatsManager.INSTANCE.shuttleTiers().nextSteps(ShuttleItem.tier(held));
    }

    /** Whether every step of the running build by hand is done and it waits for its name. */
    public boolean handDone() {
        return handBuild && HandBuild.done(buildStep(), steps().size());
    }

    /** Starts a build by hand when Build now would work; locks the shuttle and the supplies. */
    public boolean startHandBuild() {
        if (handBuild || !canBuild()) {
            return false;
        }
        handBuild = true;
        buildStep = 0;
        setChanged();
        return true;
    }

    /** The client finished a step; only the current step counts. */
    public boolean reportStep(int step) {
        if (!handBuild) {
            return false;
        }
        int before = buildStep();
        buildStep = HandBuild.next(before, step, steps().size());
        setChanged();
        return buildStep != before;
    }

    /** Ends a build by hand; nothing is used and the slots are free again. */
    public boolean cancelHandBuild() {
        if (!handBuild) {
            return false;
        }
        endHandBuild();
        return true;
    }

    /**
     * Finishes a build by hand whose steps are all done: the tier build of
     * Build now, then the name plate. An empty name keeps the old plate.
     */
    public boolean engrave(String name) {
        if (!handDone()) {
            return false;
        }
        endHandBuild();
        if (!buildNow()) {
            return false;
        }
        ItemStack built = shuttle.getStackInSlot(0).copy();
        ShuttleItem.engrave(built, NamePlate.clean(name));
        shuttle.setStackInSlot(0, built);
        return true;
    }

    private void endHandBuild() {
        handBuild = false;
        buildStep = 0;
        setChanged();
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** The supplies in the workshop counted by item id. */
    public Map<String, Integer> supplyCounts() {
        return countSupplies(supplies);
    }

    /** Counts the stacks of a supply handler by item id. */
    public static Map<String, Integer> countSupplies(ItemSlots handler) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                counts.merge(itemId(stack), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    /** The build sheet for the shuttle in the workshop, empty when there is none or it is the top tier. */
    public List<ShuttleTiers.Supply> sheet() {
        ItemStack held = shuttle.getStackInSlot(0);
        return held.isEmpty() ? List.of() : ShuttlesStatsManager.INSTANCE.shuttleTiers().nextBuild(ShuttleItem.tier(held));
    }

    /** Whether Build now would work: a shuttle below the top tier and every supply of its sheet here. */
    public boolean canBuild() {
        ItemStack held = shuttle.getStackInSlot(0);
        return !held.isEmpty() && ShuttleItem.ofTier(ShuttleItem.tier(held) + 1) != null
                && ShuttleBuild.ready(sheet(), supplyCounts());
    }

    /**
     * Builds the next tier at once: takes exactly the sheet's supplies and puts
     * the next tier's shuttle, with every component of the old one, in its place.
     *
     * @return false when the build is not ready
     */
    public boolean buildNow() {
        if (handBuild || !canBuild()) {
            return false;
        }
        for (Map.Entry<String, Integer> need : ShuttleBuild.needs(sheet()).entrySet()) {
            int left = need.getValue();
            for (int i = 0; i < supplies.getSlots() && left > 0; i++) {
                ItemStack stack = supplies.getStackInSlot(i);
                if (!stack.isEmpty() && itemId(stack).equals(need.getKey())) {
                    left -= supplies.extractItem(i, left, false).getCount();
                }
            }
        }
        ItemStack held = shuttle.getStackInSlot(0);
        shuttle.setStackInSlot(0, held.transmuteCopy(ShuttleItem.ofTier(ShuttleItem.tier(held) + 1), 1));
        return true;
    }

    /**
     * Empties the shuttle and supply slots and ends a build by hand, without
     * dropping anything; the carrot dismantle has copied them into the item.
     */
    public void clearForDismantle() {
        endHandBuild();
        shuttle.setStackInSlot(0, ItemStack.EMPTY);
        for (int i = 0; i < supplies.getSlots(); i++) {
            supplies.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    /** The shuttle, then the supplies, as one list for the item's container. */
    private List<ItemStack> contents() {
        List<ItemStack> list = new ArrayList<>(1 + SUPPLY_SLOTS);
        list.add(shuttle.getStackInSlot(0).copy());
        for (int i = 0; i < supplies.getSlots(); i++) {
            list.add(supplies.getStackInSlot(i).copy());
        }
        return list;
    }

    /** Pops the shuttle and the supplies; called when the block is broken. */
    public void dropContents() {
        if (level == null) {
            return;
        }
        for (ItemSlots handler : List.of(shuttle, supplies)) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level, worldPosition, stack.copy());
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ShuttleWorkshopMenu(containerId, inventory, this, ShuttlesStatsManager.INSTANCE.shuttleTiers());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Shuttle", shuttle.serializeNBT(registries));
        tag.put("Supplies", supplies.serializeNBT(registries));
        output.putBoolean("HandBuild", handBuild);
        output.putInt("BuildStep", buildStep);
        output.store(tag);
        woodSlot.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        if (tag.contains("Shuttle")) {
            shuttle.deserializeNBT(registries, tag.getCompoundOrEmpty("Shuttle"));
        }
        if (tag.contains("Supplies")) {
            supplies.deserializeNBT(registries, tag.getCompoundOrEmpty("Supplies"));
        }
        handBuild = input.getBooleanOr("HandBuild", false);
        buildStep = Math.max(0, input.getIntOr("BuildStep", 0));
        woodSlot.load(input);
    }

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
        ItemContainerContents container = input.get(DataComponents.CONTAINER);
        if (container != null) {
            NonNullList<ItemStack> list = NonNullList.withSize(1 + SUPPLY_SLOTS, ItemStack.EMPTY);
            container.copyInto(list);
            shuttle.setStackInSlot(0, list.get(0));
            for (int i = 0; i < supplies.getSlots(); i++) {
                supplies.setStackInSlot(i, list.get(1 + i));
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
        List<ItemStack> contents = contents();
        if (contents.stream().anyMatch(stack -> !stack.isEmpty())) {
            builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        WoodSlot.removeFromTag(output);
        output.discard("Shuttle");
        output.discard("Supplies");
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }

    /** Drops the contents on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            dropContents();
        }
    }
}
