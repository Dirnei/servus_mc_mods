package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.upgrade.CapacityUpgradeSlots;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryTanks;
import io.github.dirnei.mc.servus.shuttles.gantry.SetGantryLockPayload;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The Fluid Gantry's GUI: both tanks with their locks, the mode, the Capacity
 * Upgrade slots and the player inventory. Tanks, locks, keep levels, mode and
 * whether an upgrade may come out are synced as data slots, amounts in two 16
 * bit halves. Button {@link #BUTTON_MODE} cycles the mode, {@link #keepButton}
 * steps a keep level; locks come by {@link SetGantryLockPayload}.
 */
public class FluidGantryMenu extends AbstractContainerMenu {

    public static final int BUTTON_MODE = 0;
    /** The keep level steps in mB, in the order of the buttons under a gauge. */
    public static final int[] KEEP_STEPS = {-10000, -1000, 1000, 10000};
    private static final int BUTTON_KEEP = 1;
    public static final int WIDTH = 176;
    public static final int GAUGE_Y = 18;
    public static final int GAUGE_WIDTH = 18;
    public static final int GAUGE_HEIGHT = 52;
    public static final int LEFT_GAUGE_X = 12;
    public static final int RIGHT_GAUGE_X = WIDTH - 12 - GAUGE_WIDTH;
    public static final int MODE_X = 44;
    public static final int MODE_Y = 17;
    public static final int MODE_WIDTH = WIDTH - 2 * MODE_X;
    public static final int UPGRADES_Y = 52;
    public static final int KEEP_Y = 74;
    public static final int KEEP_BUTTONS_Y = KEEP_Y + 10;
    public static final int KEEP_BUTTON_HEIGHT = 11;
    public static final int LOCK_Y = KEEP_BUTTONS_Y + KEEP_BUTTON_HEIGHT + 3;
    public static final int LOCK_WIDTH = 78;
    public static final int LEFT_LOCK_X = 8;
    public static final int RIGHT_LOCK_X = WIDTH - 8 - LOCK_WIDTH;
    public static final int STATE_Y = LOCK_Y + 22;
    public static final int INVENTORY_Y = STATE_Y + 30;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET + 18 + 6;
    /** Ticks the state line names a refused upgrade after the refusal. */
    private static final int REFUSED_TICKS = 100;

    private static final int DATA_FLUID = 0;
    private static final int DATA_AMOUNT = 2;
    private static final int DATA_CAPACITY = 6;
    private static final int DATA_LOCK = 8;
    private static final int DATA_MODE = 10;
    private static final int DATA_REMOVABLE = 11;
    /** 0 when nothing waits, {@link #WAITS_FOR_TANKER} without a Tanker, else the waiting fluid's id plus 2. */
    private static final int DATA_WAITING = 12;
    private static final int DATA_KEEP = 13;
    /** 0 when nothing is missing, else the id plus 1 of the fluid a kept tank lacks that no Storage tank holds. */
    private static final int DATA_MISSING = 17;
    private static final int DATA_COUNT = 18;
    private static final int WAITS_FOR_TANKER = 1;

    private final ContainerLevelAccess access;
    private final Player player;
    private final ContainerData data;
    private final int upgradeSlots;
    private final FluidGantryBlockEntity gantry;
    private long refusedAt = Long.MIN_VALUE / 2;

    /** Server side: the master's own tanks and slots. */
    public FluidGantryMenu(int containerId, Inventory playerInventory, FluidGantryBlockEntity gantry) {
        this(containerId, playerInventory, ContainerLevelAccess.create(gantry.getLevel(), gantry.getBlockPos()), gantry.upgrades(),
                serverData(gantry), gantry);
    }

    private FluidGantryMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, ItemSlots upgrades,
                            ContainerData data, FluidGantryBlockEntity gantry) {
        super(ServusShuttlesMenus.FLUID_GANTRY.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;
        this.data = data;
        this.gantry = gantry;
        this.upgradeSlots = upgrades.getSlots();
        for (int slot = 0; slot < upgradeSlots; slot++) {
            addSlot(new UpgradeSlot(upgrades, slot, upgradeX(slot), UPGRADES_Y));
        }
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Client side: the server writes the master's position and the slot count. */
    public static FluidGantryMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int slots = buf.readVarInt();
        return new FluidGantryMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos),
                new CapacityUpgradeSlots(slots, null), new SimpleContainerData(DATA_COUNT), null);
    }

    /** What the server writes for {@link #fromNetwork}. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, FluidGantryBlockEntity gantry) {
        buf.writeBlockPos(gantry.getBlockPos());
        buf.writeVarInt(gantry.upgrades().getSlots());
    }

    private static ContainerData serverData(FluidGantryBlockEntity gantry) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index < DATA_AMOUNT) {
                    return BuiltInRegistries.FLUID.getId(gantry.tank(index).getFluid().getFluid());
                }
                if (index < DATA_CAPACITY) {
                    int tank = (index - DATA_AMOUNT) / 2;
                    return half(gantry.tank(tank).getFluidAmount(), (index - DATA_AMOUNT) % 2);
                }
                if (index < DATA_LOCK) {
                    return half(gantry.capacity(), index - DATA_CAPACITY);
                }
                if (index < DATA_MODE) {
                    Fluid lock = gantry.lock(index - DATA_LOCK);
                    return lock == null ? 0 : BuiltInRegistries.FLUID.getId(lock) + 1;
                }
                if (index == DATA_MISSING) {
                    Fluid missing = gantry.missing();
                    return missing == null ? 0 : BuiltInRegistries.FLUID.getId(missing) + 1;
                }
                if (index >= DATA_KEEP) {
                    return half(gantry.keep((index - DATA_KEEP) / 2), (index - DATA_KEEP) % 2);
                }
                if (index == DATA_WAITING) {
                    Fluid noRoom = gantry.noRoom();
                    return noRoom != null ? BuiltInRegistries.FLUID.getId(noRoom) + 2 : gantry.noTanker() ? WAITS_FOR_TANKER : 0;
                }
                return index == DATA_MODE ? gantry.mode().ordinal() : gantry.canRemoveUpgrade() ? 1 : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    /** The low (0) or high (1) 16 bits of a value; data slots travel as shorts. */
    private static int half(int value, int which) {
        return which == 0 ? value & 0xFFFF : value >>> 16;
    }

    private int whole(int index) {
        return (data.get(index + 1) & 0xFFFF) << 16 | data.get(index) & 0xFFFF;
    }

    /** An upgrade slot that keeps its upgrade while the tanks would not fit without it. */
    private final class UpgradeSlot extends ResourceHandlerSlot {

        UpgradeSlot(ItemSlots upgrades, int slot, int x, int y) {
            super(upgrades, upgrades::set, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            if (!hasItem() || canRemoveUpgrade()) {
                return true;
            }
            refusedAt = player.level().getGameTime();
            return false;
        }
    }

    public int upgradeSlots() {
        return upgradeSlots;
    }

    /** The upgrade row is centred. */
    public int upgradeX(int slot) {
        return (WIDTH - upgradeSlots * 18) / 2 + 1 + slot * 18;
    }

    public int hotbarY() {
        return INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET;
    }

    /** What a tank holds, as synced. */
    public FluidStack fluid(int tank) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(data.get(DATA_FLUID + tank));
        int amount = whole(DATA_AMOUNT + tank * 2);
        return fluid == Fluids.EMPTY || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    /** The capacity of each tank, as synced. */
    public int capacity() {
        return whole(DATA_CAPACITY);
    }

    /** The lock of a tank, null for Any. */
    public Fluid lock(int tank) {
        int id = data.get(DATA_LOCK + tank);
        return id <= 0 ? null : BuiltInRegistries.FLUID.byId(id - 1);
    }

    public GantryMode mode() {
        return GantryMode.byId(data.get(DATA_MODE));
    }

    /** The keep level of a tank in mB, as synced. */
    public int keep(int tank) {
        return whole(DATA_KEEP + tank * 2);
    }

    /** Whether a tank shows keep levels: the gantry is in Export mode. */
    public boolean showsKeep() {
        return mode() == GantryMode.EXPORT;
    }

    /** Whether a tank's keep level applies and may be changed: Export mode and the tank locked. */
    public boolean keepApplies(int tank) {
        return showsKeep() && lock(tank) != null;
    }

    /** The button id that steps a tank's keep level by {@code KEEP_STEPS[step]}. */
    public static int keepButton(int tank, int step) {
        return BUTTON_KEEP + tank * KEEP_STEPS.length + step;
    }

    public boolean canRemoveUpgrade() {
        return data.get(DATA_REMOVABLE) != 0;
    }

    private List<GantryTanks.Tank<Fluid>> rules() {
        return List.of(rule(0), rule(1));
    }

    private GantryTanks.Tank<Fluid> rule(int tank) {
        FluidStack fluid = fluid(tank);
        return new GantryTanks.Tank<>(lock(tank), fluid.isEmpty() ? null : fluid.getFluid(), fluid.getAmount(), capacity());
    }

    /**
     * The state line: the first thing that is wrong (a tank holding a fluid other
     * than its lock, an upgrade just refused, fluid that no Storage tank has
     * room for or no Tanker shuttle comes for, a kept fluid that no Storage
     * tank holds), else what the mode does.
     */
    public Component stateLine() {
        int warning = GantryTanks.firstWarning(rules());
        if (warning >= 0) {
            return Component.translatable("gui.servus_shuttles.fluid_gantry.state.mismatch", tankName(warning),
                    fluid(warning).getHoverName(), new FluidStack(lock(warning), 1).getHoverName());
        }
        if (upgradeJustRefused()) {
            return Component.translatable("gui.servus_shuttles.fluid_gantry.state.upgrade_refused");
        }
        int waiting = data.get(DATA_WAITING);
        if (waiting == WAITS_FOR_TANKER) {
            return Component.translatable("gui.servus_shuttles.fluid_gantry.state.no_tanker");
        }
        if (waiting > WAITS_FOR_TANKER) {
            return Component.translatable("gui.servus_shuttles.fluid_gantry.state.no_room",
                    new FluidStack(BuiltInRegistries.FLUID.byId(waiting - 2), 1).getHoverName());
        }
        int missing = data.get(DATA_MISSING);
        if (missing > 0) {
            return Component.translatable("gui.servus_shuttles.fluid_gantry.state.missing",
                    new FluidStack(BuiltInRegistries.FLUID.byId(missing - 1), 1).getHoverName());
        }
        return Component.translatable("gui.servus_shuttles.fluid_gantry.state." + mode().getSerializedName());
    }

    /** Whether the state line names something wrong rather than the mode. */
    public boolean stateWarns() {
        return GantryTanks.firstWarning(rules()) >= 0 || upgradeJustRefused() || data.get(DATA_WAITING) != 0 || data.get(DATA_MISSING) != 0;
    }

    private boolean upgradeJustRefused() {
        return player.level().getGameTime() - refusedAt < REFUSED_TICKS;
    }

    /** "Left" or "Right". */
    public static Component tankName(int tank) {
        return Component.translatable("gui.servus_shuttles.fluid_gantry.tank_" + tank);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (gantry == null) {
            return false;
        }
        if (id == BUTTON_MODE) {
            gantry.setMode(gantry.mode().next());
            return true;
        }
        int keepIndex = id - BUTTON_KEEP;
        if (keepIndex < 0 || keepIndex >= 2 * KEEP_STEPS.length) {
            return false;
        }
        int tank = keepIndex / KEEP_STEPS.length;
        if (gantry.mode() == GantryMode.EXPORT && gantry.lock(tank) != null) {
            gantry.setKeep(tank, gantry.keep(tank) + KEEP_STEPS[keepIndex % KEEP_STEPS.length]);
        }
        return true;
    }

    /** Server: sets a tank's lock as the payload says; Lock to content does nothing on an empty tank. */
    public void setLock(int tank, int kind, Optional<Identifier> fluid) {
        if (gantry == null || tank < 0 || tank > 1) {
            return;
        }
        switch (kind) {
            case SetGantryLockPayload.ANY -> gantry.setLock(tank, null);
            case SetGantryLockPayload.CONTENT -> {
                FluidStack content = gantry.tank(tank).getFluid();
                if (!content.isEmpty()) {
                    gantry.setLock(tank, content.getFluid());
                }
            }
            case SetGantryLockPayload.FLUID -> fluid.flatMap(BuiltInRegistries.FLUID::getOptional)
                    .filter(found -> found != Fluids.EMPTY).ifPresent(found -> gantry.setLock(tank, found));
            default -> {
            }
        }
    }

    /** Shift click moves Capacity Upgrades between the upgrade row and the player inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < upgradeSlots) {
            if (!moveItemStackTo(stack, upgradeSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(ServusFoundationItems.CAPACITY_UPGRADE.get()) || !moveItemStackTo(stack, 0, upgradeSlots, false)) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.FLUID_GANTRY.get());
    }
}
