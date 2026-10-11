package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.field.FertilizeMode;
import io.github.dirnei.mc.servus.storage.field.FertilizeStatus;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldCropStockPayload;
import io.github.dirnei.mc.servus.storage.field.FieldFertilizerStockPayload;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.field.FieldWorkerPayload;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Field Head GUI in four tabs: Field (crop picker), Water (anti-corrosion
 * slot), Fertilizer (fertilizer picker, mode, threshold, reserve) and Upgrades
 * (Range Upgrade slot); the field's numbers as container data, the warehouse
 * stock of the crop and fertilizer items as payloads, the player inventory
 * always below. The tab is client view state; head slots off the shown tab
 * are inactive. The crop and the fertilizer are chosen by id through
 * {@link #choose(Player, Optional)} and {@link #chooseFertilizer(Player, Optional)},
 * no item needed; the rest through menu buttons.
 */
public class FieldHeadMenu extends AbstractContainerMenu {

    public static final int PICKER_X = 8;
    public static final int PICKER_Y = 17;
    public static final int PICKER_WIDTH = 160;
    public static final int RANGE_X = 8;
    public static final int RANGE_Y = 18;
    public static final int RANGE_SLOT = 0;
    public static final int ANTI_CORROSION_SLOT = 1;
    public static final int ANTI_CORROSION_X = 8;
    public static final int ANTI_CORROSION_Y = 58;
    public static final int INVENTORY_Y = 96;
    public static final int HOTBAR_Y = 154;
    public static final int INVENTORY_START = 2;
    public static final int BUTTON_THRESHOLD_DOWN = 1;
    public static final int BUTTON_THRESHOLD_UP = 2;
    /** Mode buttons: this id plus the {@link FertilizeMode} ordinal. */
    public static final int BUTTON_FERTILIZE_MODE = 3;
    public static final int BUTTON_FERTILIZE_DOWN = 6;
    public static final int BUTTON_FERTILIZE_UP = 7;
    /** Reserve buttons: -10, -1, +1, +10 from this id on. */
    public static final int BUTTON_RESERVE = 8;
    public static final int[] RESERVE_STEPS = {-10, -1, 1, 10};
    private static final int COLUMNS = 9;
    private static final int FLAG_HYDRATED = 1;
    private static final int FLAG_HARVEST_ONLY = 2;
    private static final int FLAG_DUE = 4;
    /** Ticks between two looks at the warehouse stock of the crop items. */
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
    /** Signature of the fertilizer stock last sent, on the crop stock timer; null before the first send. */
    private Long sentFertilizerStock;
    /** Client side: the fertilizer stock by item id, null while there is no warehouse or before the first. */
    private Map<Identifier, Long> fertilizerStock;
    /** The worker name last sent to the player; null before the first send. */
    private String sentWorker;
    /** Ticks until the next stock look; zero sends on the first broadcast. */
    private int stockTimer;
    /** Signature of the stock last sent; null before the first send. */
    private Long sentStock;
    /** Client side: the stock counts by item id as last received, null while there is no warehouse or before the first. */
    private Map<Identifier, Long> stock;
    /** The tab the screen shows; client side view state, the server never learns it. */
    private Page page = Page.FIELD;

    /** The tabs of the GUI, in order. */
    public enum Page {
        FIELD, WATER, FERTILIZER, UPGRADES;

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
        addSlot(new ResourceHandlerSlot(head.antiCorrosion(), head.antiCorrosion()::set, 0, ANTI_CORROSION_X, ANTI_CORROSION_Y) {
            @Override
            public boolean isActive() {
                return shows(ANTI_CORROSION_SLOT);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
        for (DataSlot slot : new DataSlot[] {waterLow, waterHigh, capacityLow, capacityHigh, rails, plots, planted, ripe, status, flags, work, threshold, radius, corrosion, corrosionProgress,
                fertilizeMode, fertilizePercent, reserve, fertilizeStatus}) {
            addDataSlot(slot);
        }
        update();
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
        waterLow.set(head.water() & 0xFFFF);
        waterHigh.set(head.water() >>> 16);
        capacityLow.set(head.capacity() & 0xFFFF);
        capacityHigh.set(head.capacity() >>> 16);
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
        flags.set((head.hydrated() ? FLAG_HYDRATED : 0) | (head.harvestOnly() ? FLAG_HARVEST_ONLY : 0) | (head.harvestDue() ? FLAG_DUE : 0));
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
                FieldCropStockPayload payload = stockPayload();
                long signature = PickerStock.signature(payload.networked(), stringKeys(payload.counts()));
                if (sentStock == null || sentStock != signature) {
                    sentStock = signature;
                    PacketDistributor.sendToPlayer(server, payload);
                }
                FieldFertilizerStockPayload fertilizers = fertilizerStockPayload();
                long fertilizerSignature = PickerStock.signature(fertilizers.networked(), stringKeys(fertilizers.counts()));
                if (sentFertilizerStock == null || sentFertilizerStock != fertilizerSignature) {
                    sentFertilizerStock = fertilizerSignature;
                    PacketDistributor.sendToPlayer(server, fertilizers);
                }
            }
        }
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
        if (!(player.containerMenu instanceof FieldHeadMenu menu) || !menu.stillValid(player)) {
            return false;
        }
        if (fertilizer.isEmpty()) {
            menu.head.setFertilizer(ItemStack.EMPTY);
            menu.update();
            return true;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(fertilizer.get());
        if (item.isEmpty()) {
            return false;
        }
        ItemStack stack = new ItemStack(item.get());
        if (!FieldHeadBlockEntity.isFertilizer(stack)) {
            return false;
        }
        menu.head.setFertilizer(stack);
        menu.update();
        return true;
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
     * menu is a Field Head menu they can still use; anything else is refused.
     * Returns whether the choice was taken. Server side.
     */
    public static boolean choose(Player player, Optional<Identifier> crop) {
        if (!(player.containerMenu instanceof FieldHeadMenu menu) || !menu.stillValid(player)) {
            return false;
        }
        if (crop.isEmpty()) {
            menu.head.setHarvestOnly(true);
            menu.update();
            return true;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(crop.get());
        if (item.isEmpty()) {
            return false;
        }
        ItemStack stack = new ItemStack(item.get());
        if (!FieldHeadBlockEntity.isCropItem(stack)) {
            return false;
        }
        menu.head.setCrop(stack);
        menu.update();
        return true;
    }

    /** The page a head slot belongs to, null for the player inventory. */
    public static Page pageOf(int slot) {
        return switch (slot) {
            case RANGE_SLOT -> Page.UPGRADES;
            case ANTI_CORROSION_SLOT -> Page.WATER;
            default -> null;
        };
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

    public FieldHeadBlockEntity head() {
        return head;
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

    public boolean harvestDue() {
        return (flags.get() & FLAG_DUE) != 0;
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

    /** What the fertilizing is doing; null while the head serves no field. */
    public FertilizeStatus fertilizeStatus() {
        return fertilizeStatus.get() == 0 ? null : FertilizeStatus.byId(fertilizeStatus.get() - 1);
    }

    /** The arrows move the harvest and fertilize thresholds and the reserve; the mode buttons set the mode. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        boolean known = id == BUTTON_THRESHOLD_DOWN || id == BUTTON_THRESHOLD_UP
                || id >= BUTTON_FERTILIZE_MODE && id < BUTTON_RESERVE + RESERVE_STEPS.length;
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
        } else if (id == BUTTON_FERTILIZE_DOWN || id == BUTTON_FERTILIZE_UP) {
            head.stepFertilizePercent(id == BUTTON_FERTILIZE_DOWN ? -1 : 1);
        } else {
            head.stepReserve(RESERVE_STEPS[id - BUTTON_RESERVE]);
        }
        update();
        return true;
    }

    /**
     * Shift clicking a Range Upgrade or an anti-corrosion item moves it into
     * its slot and back; other items stay where they are.
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
        if (index == RANGE_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == ANTI_CORROSION_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (slots.get(RANGE_SLOT).mayPlace(stack)) {
            if (!moveItemStackTo(stack, RANGE_SLOT, RANGE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slots.get(ANTI_CORROSION_SLOT).mayPlace(stack)) {
            if (!moveItemStackTo(stack, ANTI_CORROSION_SLOT, ANTI_CORROSION_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == before.getCount() ? ItemStack.EMPTY : before;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.FIELD_HEAD.get());
    }
}
