package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.storage.field.FertilizeMode;
import io.github.dirnei.mc.servus.storage.field.FieldAmount;
import io.github.dirnei.mc.servus.storage.field.FertilizeStatus;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldCropStockPayload;
import io.github.dirnei.mc.servus.storage.field.FieldFertilizerStockPayload;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.field.FieldWorkerPayload;
import io.github.dirnei.mc.servus.storage.field.SupplyBuffer;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.picker.PickerStock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Field Head GUI in five tabs: Field (crop picker, ripe share, harvest
 * threshold), Water, Fertilizer (fertilizer picker, mode, threshold), Stock
 * (produce, typed levels and reserve, the buffer) and Upgrades (Range Upgrade
 * slot). The buffer takes nothing from the player and gives nothing back, so
 * it has no slots. The field's numbers travel as container data, the
 * warehouse stock of crop and fertilizer items as payloads. The tab is client
 * view state; head slots off the shown tab are inactive. Crop and fertilizer
 * are chosen by id through {@link #choose(Player, Optional)} and
 * {@link #chooseFertilizer(Player, Optional)}, the stock levels through
 * {@link #setAmount(Player, BlockPos, FieldAmount, long)}, the
 * rest through menu buttons.
 */
public class FieldHeadMenu extends AbstractContainerMenu {

    public static final int PICKER_X = 8;
    public static final int PICKER_Y = 17;
    public static final int IMAGE_WIDTH = 264;
    /** The width of a page's content, from {@link #PICKER_X} to the panel's shaded rim. */
    public static final int PICKER_WIDTH = IMAGE_WIDTH - 2 * PICKER_X;
    public static final int RANGE_X = 8;
    public static final int RANGE_Y = 18;
    public static final int RANGE_SLOT = 0;
    public static final int INVENTORY_X = (IMAGE_WIDTH - 9 * 18) / 2;
    public static final int INVENTORY_Y = 154;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET + 18 + 6;
    public static final int INVENTORY_START = 1;
    public static final int BUTTON_THRESHOLD_DOWN = 1;
    public static final int BUTTON_THRESHOLD_UP = 2;
    /** Mode buttons: this id plus the {@link FertilizeMode} ordinal. */
    public static final int BUTTON_FERTILIZE_MODE = 3;
    public static final int BUTTON_FERTILIZE_DOWN = 6;
    public static final int BUTTON_FERTILIZE_UP = 7;
    private static final int FLAG_HYDRATED = 1;
    private static final int FLAG_HARVEST_ONLY = 2;
    private static final int FLAG_DUE = 4;
    private static final int FLAG_PAUSED = 8;
    /** Ticks between two looks at the warehouse stock. */
    private static final int STOCK_INTERVAL = 20;

    private final FieldHeadBlockEntity head;
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot waterLow = DataSlot.standalone();
    private final DataSlot waterHigh = DataSlot.standalone();
    private final DataSlot capacityLow = DataSlot.standalone();
    private final DataSlot capacityHigh = DataSlot.standalone();
    private final DataSlot rails = DataSlot.standalone();
    private final DataSlot plots = DataSlot.standalone();
    private final DataSlot planted = DataSlot.standalone();
    private final DataSlot ripe = DataSlot.standalone();
    private final DataSlot status = DataSlot.standalone();
    private final DataSlot flags = DataSlot.standalone();
    /** The head's work reason plus one; zero while it serves no field. */
    private final DataSlot work = DataSlot.standalone();
    private final DataSlot threshold = DataSlot.standalone();
    private final DataSlot radius = DataSlot.standalone();
    /** The served field's corrosion stage plus one; zero while the head serves no field. */
    private final DataSlot corrosion = DataSlot.standalone();
    private final DataSlot corrosionProgress = DataSlot.standalone();
    private final DataSlot fertilizeMode = DataSlot.standalone();
    private final DataSlot fertilizePercent = DataSlot.standalone();
    private final DataSlot reserve = DataSlot.standalone();
    /** The fertilize status plus one; zero while the head serves no field. */
    private final DataSlot fertilizeStatus = DataSlot.standalone();
    /** Count and level of each buffer part, in {@link SupplyBuffer.Part} order. */
    private final DataSlot[] supplyCounts = standalone(SupplyBuffer.Part.VALUES.length);
    private final DataSlot[] supplyLevels = standalone(SupplyBuffer.Part.VALUES.length);
    private final DataSlot harvestLimitLow = DataSlot.standalone();
    private final DataSlot harvestLimitHigh = DataSlot.standalone();
    private final DataSlot fertilizeBelowLow = DataSlot.standalone();
    private final DataSlot fertilizeBelowHigh = DataSlot.standalone();
    private final DataSlot produceStockLow = DataSlot.standalone();
    private final DataSlot produceStockHigh = DataSlot.standalone();
    /** Signatures of the crop and fertilizer stock last sent; null before the first send. */
    private Long sentStock;
    private Long sentFertilizerStock;
    /** Client side: the stock by item id as last received, null while there is no warehouse. */
    private Map<Identifier, Long> stock;
    private Map<Identifier, Long> fertilizerStock;
    /** The worker names last sent to the player; null before the first send. */
    private String sentWorker;
    /** Ticks until the next stock look; zero sends on the first broadcast. */
    private int stockTimer;
    /** The tab the screen shows; the server never learns it. */
    private Page page = Page.FIELD;

    /** The tabs of the GUI, in order. */
    public enum Page {
        FIELD, WATER, FERTILIZER, STOCK, UPGRADES;

        public static final Page[] VALUES = values();
    }

    public FieldHeadMenu(int containerId, Inventory playerInventory, FieldHeadBlockEntity head) {
        super(ServusStorageMenus.FIELD_HEAD.get(), containerId);
        this.head = head;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(head.getLevel(), head.getBlockPos());
        addSlot(new ResourceHandlerSlot(head.rangeUpgrades(), head.rangeUpgrades()::set, 0, RANGE_X, RANGE_Y) {
            @Override
            public boolean isActive() {
                return shows(RANGE_SLOT);
            }
        });
        PlayerInventorySlots.add(this::addSlot, playerInventory, INVENTORY_X, INVENTORY_Y);
        for (DataSlot slot : new DataSlot[] {waterLow, waterHigh, capacityLow, capacityHigh, rails, plots, planted, ripe, status, flags,
                work, threshold, radius, corrosion, corrosionProgress, fertilizeMode, fertilizePercent, reserve, fertilizeStatus,
                harvestLimitLow, harvestLimitHigh, fertilizeBelowLow, fertilizeBelowHigh, produceStockLow, produceStockHigh}) {
            addDataSlot(slot);
        }
        for (int i = 0; i < SupplyBuffer.Part.VALUES.length; i++) {
            addDataSlot(supplyCounts[i]);
            addDataSlot(supplyLevels[i]);
        }
        update();
    }

    private static DataSlot[] standalone(int count) {
        DataSlot[] slots = new DataSlot[count];
        for (int i = 0; i < count; i++) {
            slots[i] = DataSlot.standalone();
        }
        return slots;
    }

    public static FieldHeadMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof FieldHeadBlockEntity head) {
            return new FieldHeadMenu(containerId, playerInventory, head);
        }
        throw new IllegalStateException("No field head at " + pos);
    }

    /** Copies the head's numbers into the data slots. Server side. */
    private void update() {
        if (player.level().isClientSide()) {
            return;
        }
        split(head.water(), waterLow, waterHigh);
        split(head.capacity(), capacityLow, capacityHigh);
        rails.set(clamp(head.rails().size()));
        plots.set(clamp(head.plots().size()));
        planted.set(clamp(head.plantedCount()));
        ripe.set(clamp(head.ripeCount()));
        status.set(head.status().ordinal());
        FieldWork reason = head.work();
        work.set(reason == null ? 0 : reason.ordinal() + 1);
        threshold.set(head.harvestPercent());
        radius.set(head.radius());
        corrosion.set(head.corrosion() + 1);
        corrosionProgress.set(head.corrosionProgress());
        fertilizeMode.set(head.fertilizeMode().ordinal());
        fertilizePercent.set(head.fertilizePercent());
        reserve.set(head.reserve());
        FertilizeStatus fertilize = head.fertilizeStatus();
        fertilizeStatus.set(fertilize == null ? 0 : fertilize.ordinal() + 1);
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            supplyCounts[part.ordinal()].set(clamp(head.supplyCount(part)));
            supplyLevels[part.ordinal()].set(clamp(head.supplyLevel(part)));
        }
        split(head.harvestLimit(), harvestLimitLow, harvestLimitHigh);
        split(head.fertilizeBelow(), fertilizeBelowLow, fertilizeBelowHigh);
        split((int) Math.min(Integer.MAX_VALUE, head.produceStock()), produceStockLow, produceStockHigh);
        flags.set((head.hydrated() ? FLAG_HYDRATED : 0) | (head.harvestOnly() ? FLAG_HARVEST_ONLY : 0) | (head.harvestCalled() ? FLAG_DUE : 0)
                | (head.harvestPaused() ? FLAG_PAUSED : 0));
    }

    private static void split(int value, DataSlot low, DataSlot high) {
        low.set(value & 0xFFFF);
        high.set(value >>> 16);
    }

    /** Container data travels as a short. */
    private static int clamp(int value) {
        return Math.min(Short.MAX_VALUE, value);
    }

    private static int join(DataSlot low, DataSlot high) {
        return (low.get() & 0xFFFF) | ((high.get() & 0xFFFF) << 16);
    }

    @Override
    public void broadcastChanges() {
        update();
        super.broadcastChanges();
        if (player instanceof ServerPlayer server) {
            String worker = head.workerName().orElse("");
            String spreader = head.spreaderName().orElse("");
            String workers = worker + "\n" + spreader;
            if (!workers.equals(sentWorker)) {
                sentWorker = workers;
                PacketDistributor.sendToPlayer(server, new FieldWorkerPayload(worker, spreader));
            }
            if (--stockTimer <= 0) {
                stockTimer = STOCK_INTERVAL;
                FieldCropStockPayload crops = stockPayload();
                sentStock = sendIfChanged(server, crops, crops.networked(), crops.counts(), sentStock);
                FieldFertilizerStockPayload fertilizers = fertilizerStockPayload();
                sentFertilizerStock = sendIfChanged(server, fertilizers, fertilizers.networked(), fertilizers.counts(), sentFertilizerStock);
            }
        }
    }

    /** Sends a stock payload unless its signature equals the one sent last; returns its signature. */
    private static long sendIfChanged(ServerPlayer player, CustomPacketPayload payload, boolean networked, Map<Identifier, Long> counts,
                                      Long sent) {
        long signature = PickerStock.signature(networked, stringKeys(counts));
        if (sent == null || sent != signature) {
            PacketDistributor.sendToPlayer(player, payload);
        }
        return signature;
    }

    /** The warehouse stock of the crop items as it travels to the client. Server side. */
    public FieldCropStockPayload stockPayload() {
        Optional<Map<Identifier, Long>> counts = head.cropStock();
        return new FieldCropStockPayload(containerId, counts.isPresent(), counts.orElse(Map.of()));
    }

    /** The warehouse stock of the fertilizer items as it travels to the client. Server side. */
    public FieldFertilizerStockPayload fertilizerStockPayload() {
        Optional<Map<Identifier, Long>> counts = head.fertilizerStock();
        return new FieldFertilizerStockPayload(containerId, counts.isPresent(), counts.orElse(Map.of()));
    }

    /** Client side: keeps the fertilizer stock the server sent for this menu. */
    public void acceptFertilizerStock(FieldFertilizerStockPayload payload) {
        if (payload.containerId() == containerId) {
            fertilizerStock = payload.networked() ? Map.copyOf(payload.counts()) : null;
        }
    }

    /** Client side: the fertilizer stock by item id; null while there are no counts to show. */
    public Map<Identifier, Long> fertilizerStock() {
        return fertilizerStock;
    }

    /**
     * Sets the fertilizer the player chose: an item id of the fertilizer tag,
     * or empty for Off. Acts only while the player's open menu is a Field Head
     * menu they can still use. Returns whether the choice was taken. Server side.
     */
    public static boolean chooseFertilizer(Player player, Optional<Identifier> fertilizer) {
        FieldHeadMenu menu = usableMenu(player);
        if (menu == null) {
            return false;
        }
        if (fertilizer.isEmpty()) {
            menu.head.setFertilizer(ItemStack.EMPTY);
            menu.update();
            return true;
        }
        Optional<ItemStack> stack = stackOf(fertilizer.get()).filter(FieldHeadBlockEntity::isFertilizer);
        if (stack.isEmpty()) {
            return false;
        }
        menu.head.setFertilizer(stack.get());
        menu.update();
        return true;
    }

    /** The player's open Field Head menu while they can still use it, else null. */
    private static FieldHeadMenu usableMenu(Player player) {
        return player.containerMenu instanceof FieldHeadMenu menu && menu.stillValid(player) ? menu : null;
    }

    private static Optional<ItemStack> stackOf(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id).map(item -> new ItemStack(item));
    }

    private static Map<String, Long> stringKeys(Map<Identifier, Long> counts) {
        Map<String, Long> keyed = new HashMap<>();
        counts.forEach((id, count) -> keyed.put(id.toString(), count));
        return keyed;
    }

    /** Client side: keeps the stock the server sent for this menu. */
    public void acceptStock(FieldCropStockPayload payload) {
        if (payload.containerId() == containerId) {
            stock = payload.networked() ? Map.copyOf(payload.counts()) : null;
        }
    }

    /** Client side: the warehouse stock by item id, missing ids hold none; null while there are no counts to show. */
    public Map<Identifier, Long> stock() {
        return stock;
    }

    /**
     * Sets the crop the player chose in the picker: an item id that plants an
     * age crop, or empty for harvest only. Acts only while the player's open
     * menu is a Field Head menu they can still use. Returns whether the choice
     * was taken. Server side.
     */
    public static boolean choose(Player player, Optional<Identifier> crop) {
        FieldHeadMenu menu = usableMenu(player);
        if (menu == null) {
            return false;
        }
        if (crop.isEmpty()) {
            menu.head.setHarvestOnly(true);
            menu.update();
            return true;
        }
        Optional<ItemStack> stack = stackOf(crop.get()).filter(FieldHeadBlockEntity::isCropItem);
        if (stack.isEmpty()) {
            return false;
        }
        menu.head.setCrop(stack.get());
        menu.update();
        return true;
    }

    /**
     * Sets a typed amount of the head at that position: a stock level, kept
     * in range and in order with the other, or the reserve, kept in range.
     * Acts only while the player's open menu is that head's menu and they can
     * still use it. Returns whether the amount was taken. Server side.
     */
    public static boolean setAmount(Player player, BlockPos pos, FieldAmount which, long value) {
        FieldHeadMenu menu = usableMenu(player);
        if (menu == null || which == null || !menu.head.getBlockPos().equals(pos)) {
            return false;
        }
        if (which == FieldAmount.RESERVE) {
            menu.head.setReserve(value);
        } else {
            menu.head.setLevel(which.level(), value);
        }
        menu.update();
        return true;
    }

    /** The position of the head this menu shows. */
    public BlockPos headPos() {
        return head.getBlockPos();
    }

    /** The page a head slot belongs to, null for the player inventory. */
    public static Page pageOf(int slot) {
        return slot == RANGE_SLOT ? Page.UPGRADES : null;
    }

    /** Head slots are active on the server, and on the client only on their page. */
    private boolean shows(int slot) {
        return !player.level().isClientSide() || pageOf(slot) == page;
    }

    /** The tab the screen shows. */
    public Page page() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public int water() {
        return join(waterLow, waterHigh);
    }

    public int capacity() {
        return join(capacityLow, capacityHigh);
    }

    public int rails() {
        return rails.get();
    }

    public int plots() {
        return plots.get();
    }

    public int planted() {
        return planted.get();
    }

    public int ripe() {
        return ripe.get();
    }

    public FieldStatus status() {
        return FieldStatus.byId(status.get());
    }

    /** What the field is doing or waits for; null while the head serves no field. */
    public FieldWork work() {
        return work.get() == 0 ? null : FieldWork.byId(work.get() - 1);
    }

    /** The ripe percent that calls a harvest. */
    public int threshold() {
        return threshold.get();
    }

    /** How far the field reaches to each side of its rails. */
    public int radius() {
        return radius.get();
    }

    /** The served field's corrosion stage, {@link FieldCorrosion#LOOSE} while the head serves none. */
    public int corrosion() {
        return corrosion.get() - 1;
    }

    /** Percent of the way to the next aging step. */
    public int corrosionProgress() {
        return corrosionProgress.get();
    }

    public boolean hydrated() {
        return (flags.get() & FLAG_HYDRATED) != 0;
    }

    public boolean harvestOnly() {
        return (flags.get() & FLAG_HARVEST_ONLY) != 0;
    }

    /** Whether a harvest is due and not paused at the harvest limit. */
    public boolean harvestDue() {
        return (flags.get() & FLAG_DUE) != 0;
    }

    /** Whether the warehouse holds the harvest limit of the produce, so the field harvests nothing. */
    public boolean harvestPaused() {
        return (flags.get() & FLAG_PAUSED) != 0;
    }

    /** The harvest limit in items of the produce; 0 for none. */
    public int harvestLimit() {
        return join(harvestLimitLow, harvestLimitHigh);
    }

    /** The fertilize level in items of the produce; 0 for none. */
    public int fertilizeBelow() {
        return join(fertilizeBelowLow, fertilizeBelowHigh);
    }

    /** The warehouse stock of the produce as the head last read it. */
    public int produceStock() {
        return join(produceStockLow, produceStockHigh);
    }

    /** The field's produce as the head last synced it; empty until the first harvest. */
    public ItemStack produce() {
        return head.produce();
    }

    /** The crop of the field as the head last synced it; empty for none. */
    public ItemStack crop() {
        return head.crop();
    }

    /** The fertilizer of the field as the head last synced it; empty for none. */
    public ItemStack fertilizer() {
        return head.fertilizer();
    }

    public FertilizeMode fertilizeMode() {
        return FertilizeMode.byId(fertilizeMode.get());
    }

    /** The ripe percent below which Until ripe share doses. */
    public int fertilizePercent() {
        return fertilizePercent.get();
    }

    /** The Keep in stock reserve. */
    public int reserve() {
        return reserve.get();
    }

    /** How many of the part's item the head's buffer holds. */
    public int supplyCount(SupplyBuffer.Part part) {
        return supplyCounts[part.ordinal()].get();
    }

    /** The level supply runs fill the part to; 0 while the part is off. */
    public int supplyLevel(SupplyBuffer.Part part) {
        return supplyLevels[part.ordinal()].get();
    }

    /** The anti-corrosion item the buffer holds as the head last synced it, else the first of the tag. */
    public ItemStack antiCorrosionShown() {
        return head.antiCorrosionShown();
    }

    /** What the fertilizing is doing; null while the head serves no field. */
    public FertilizeStatus fertilizeStatus() {
        return fertilizeStatus.get() == 0 ? null : FertilizeStatus.byId(fertilizeStatus.get() - 1);
    }

    /** The arrows move the harvest and fertilize thresholds; the mode buttons set the mode. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        boolean known = id >= BUTTON_THRESHOLD_DOWN && id <= BUTTON_FERTILIZE_UP;
        if (!known) {
            return false;
        }
        if (player.level().isClientSide()) {
            return true;
        }
        if (id == BUTTON_THRESHOLD_DOWN || id == BUTTON_THRESHOLD_UP) {
            head.stepHarvestPercent(id == BUTTON_THRESHOLD_DOWN ? -1 : 1);
        } else if (id < BUTTON_FERTILIZE_DOWN) {
            head.setFertilizeMode(FertilizeMode.byId(id - BUTTON_FERTILIZE_MODE));
        } else {
            head.stepFertilizePercent(id == BUTTON_FERTILIZE_DOWN ? -1 : 1);
        }
        update();
        return true;
    }

    /**
     * Shift clicking a Range Upgrade moves it into its slot and back; other
     * items stay where they are.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack before = stack.copy();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (slots.get(RANGE_SLOT).mayPlace(stack)) {
            if (!moveItemStackTo(stack, RANGE_SLOT, RANGE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return stack.getCount() == before.getCount() ? ItemStack.EMPTY : before;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.FIELD_HEAD.get());
    }
}
