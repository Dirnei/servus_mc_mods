package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleBuild;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers.Kind;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleUpgrades;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleWorkshopBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.BuildStep;
import io.github.dirnei.mc.servus.storage.shuttle.module.build.StepKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The Shuttle Workshop: the shuttle slot, the install slot that puts upgrades
 * and tools into it, the supply slots for the next tier's build, and buttons
 * that take upgrades out or build now. The tier stats come with the opening
 * so the client knows the maxes and the build sheets.
 */
public class ShuttleWorkshopMenu extends AbstractContainerMenu {

    public static final int SHUTTLE_X = 8;
    public static final int SHUTTLE_Y = 22;
    public static final int INSTALL_X = 8;
    public static final int INSTALL_Y = 50;
    public static final int SUPPLY_X = 8;
    public static final int SUPPLY_Y = 90;
    public static final int INVENTORY_Y = 122;
    public static final int HOTBAR_Y = 180;
    public static final int SHUTTLE_SLOT = 0;
    public static final int INSTALL_SLOT = 1;
    public static final int SUPPLY_START = 2;
    public static final int INVENTORY_START = SUPPLY_START + ShuttleWorkshopBlockEntity.SUPPLY_SLOTS;

    public static final int BUTTON_REMOVE_SPEED = 0;
    public static final int BUTTON_REMOVE_HANDLING = 1;
    public static final int BUTTON_REMOVE_TOOL = 2;
    public static final int BUTTON_BUILD_NOW = 3;
    public static final int BUTTON_BUILD_BY_HAND = 4;
    public static final int BUTTON_CANCEL = 5;

    /** The tier stats as the server knows them, for the client's labels. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ShuttleTiers> TIERS_CODEC = StreamCodec.of(
            ShuttleWorkshopMenu::writeTiers, ShuttleWorkshopMenu::readTiers);

    private final ContainerLevelAccess access;
    private final ShuttleWorkshopBlockEntity workshop;
    private final ItemSlots shuttle;
    private final ItemSlots supplies;
    private final ShuttleTiers tiers;
    private final Player player;
    /** Whether a build by hand runs; read from the workshop on the server, synced to the client. */
    private final DataSlot building;
    /** The step of the build by hand. */
    private final DataSlot step;

    /** Server side: the workshop's own handlers. */
    public ShuttleWorkshopMenu(int containerId, Inventory playerInventory, ShuttleWorkshopBlockEntity workshop, ShuttleTiers tiers) {
        this(containerId, playerInventory, workshop, workshop.shuttle(), workshop.supplies(), tiers,
                ContainerLevelAccess.create(workshop.getLevel(), workshop.getBlockPos()));
    }

    private ShuttleWorkshopMenu(int containerId, Inventory playerInventory, ShuttleWorkshopBlockEntity workshop, ItemSlots shuttle,
                                ItemSlots supplies, ShuttleTiers tiers, ContainerLevelAccess access) {
        super(ServusStorageMenus.SHUTTLE_WORKSHOP.get(), containerId);
        this.access = access;
        this.workshop = workshop;
        this.shuttle = shuttle;
        this.supplies = supplies;
        this.tiers = tiers;
        this.player = playerInventory.player;
        this.building = workshop == null ? DataSlot.standalone() : new WorkshopSlot(() -> workshop.handBuild() ? 1 : 0);
        this.step = workshop == null ? DataSlot.standalone() : new WorkshopSlot(workshop::buildStep);
        addSlot(new ResourceHandlerSlot(shuttle, shuttle::set, 0, SHUTTLE_X, SHUTTLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !building() && ShuttleItem.isShuttle(stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                return !building() && super.mayPickup(player);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new InstallSlot(INSTALL_X, INSTALL_Y));
        for (int i = 0; i < ShuttleWorkshopBlockEntity.SUPPLY_SLOTS; i++) {
            addSlot(new ResourceHandlerSlot(supplies, supplies::set, i, SUPPLY_X + i * 18, SUPPLY_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !building() && !ShuttleItem.isShuttle(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return !building() && super.mayPickup(player);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
        addDataSlot(building);
        addDataSlot(step);
    }

    /** A data slot that reads its value from the workshop on the server. */
    private static final class WorkshopSlot extends DataSlot {

        private final java.util.function.IntSupplier source;

        WorkshopSlot(java.util.function.IntSupplier source) {
            this.source = source;
        }

        @Override
        public int get() {
            return source.getAsInt();
        }

        @Override
        public void set(int value) {
            // The server reads the workshop; nothing to store.
        }
    }

    public static ShuttleWorkshopMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ShuttleTiers tiers = TIERS_CODEC.decode(buf);
        return new ShuttleWorkshopMenu(containerId, playerInventory, null, new ItemSlots(1),
                new ItemSlots(ShuttleWorkshopBlockEntity.SUPPLY_SLOTS), tiers,
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** What the server writes for {@link #fromNetwork}. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, BlockPos pos, ShuttleTiers tiers) {
        buf.writeBlockPos(pos);
        TIERS_CODEC.encode(buf, tiers);
    }

    private static void writeTiers(RegistryFriendlyByteBuf buf, ShuttleTiers tiers) {
        buf.writeDouble(tiers.baseSpeed());
        buf.writeDouble(tiers.speedPerCount());
        buf.writeVarInt(tiers.stopTicks());
        buf.writeVarInt(tiers.stackTicks());
        buf.writeDouble(tiers.handlingFactor());
        buf.writeVarInt(tiers.tiers().size());
        for (ShuttleTiers.Tier tier : tiers.tiers()) {
            buf.writeVarInt(tier.stacks());
            buf.writeVarInt(tier.maxSpeed());
            buf.writeVarInt(tier.maxHandling());
            buf.writeVarInt(tier.build().size());
            for (ShuttleTiers.Supply supply : tier.build()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, supply.id());
                buf.writeVarInt(supply.count());
            }
            buf.writeVarInt(tier.steps().size());
            for (BuildStep step : tier.steps()) {
                buf.writeVarInt(step.kind().ordinal());
                buf.writeVarInt(step.count());
            }
        }
    }

    private static ShuttleTiers readTiers(RegistryFriendlyByteBuf buf) {
        double base = buf.readDouble();
        double step = buf.readDouble();
        int stop = buf.readVarInt();
        int stack = buf.readVarInt();
        double factor = buf.readDouble();
        int count = buf.readVarInt();
        List<ShuttleTiers.Tier> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int stacks = buf.readVarInt();
            int maxSpeed = buf.readVarInt();
            int maxHandling = buf.readVarInt();
            int supplies = buf.readVarInt();
            List<ShuttleTiers.Supply> build = new ArrayList<>();
            for (int j = 0; j < supplies; j++) {
                build.add(new ShuttleTiers.Supply(ByteBufCodecs.STRING_UTF8.decode(buf), buf.readVarInt()));
            }
            int stepCount = buf.readVarInt();
            List<BuildStep> steps = new ArrayList<>();
            for (int j = 0; j < stepCount; j++) {
                StepKind[] kinds = StepKind.values();
                int kind = buf.readVarInt();
                steps.add(new BuildStep(kinds[Math.max(0, Math.min(kinds.length - 1, kind))], buf.readVarInt()));
            }
            list.add(new ShuttleTiers.Tier(stacks, maxSpeed, maxHandling, build, steps));
        }
        return new ShuttleTiers(base, step, stop, stack, factor, list);
    }

    /** The shuttle in the workshop, or empty. */
    public ItemStack shuttle() {
        return shuttle.getStackInSlot(0);
    }

    public ShuttleTiers tiers() {
        return tiers;
    }

    /** The build sheet of the next tier for the shuttle in the workshop; empty without a shuttle or at the top. */
    public List<ShuttleTiers.Supply> sheet() {
        ItemStack held = shuttle();
        return held.isEmpty() ? List.of() : tiers.nextBuild(ShuttleItem.tier(held));
    }

    /** Whether a build by hand runs. */
    public boolean building() {
        return building.get() != 0;
    }

    /** The step the build by hand is on; the step count once it waits for its name. */
    public int buildStep() {
        return step.get();
    }

    /** The steps of a build by hand into the next tier, from the tier stats of the opening. */
    public List<BuildStep> steps() {
        ItemStack held = shuttle();
        return held.isEmpty() ? List.of() : tiers.nextSteps(ShuttleItem.tier(held));
    }

    /** The workshop's position, for the seeds of the boards. */
    public BlockPos pos() {
        return access.evaluate((level, pos) -> pos, BlockPos.ZERO);
    }

    /** Server side: the client finished that step. */
    public void reportStep(int reported) {
        if (workshop != null) {
            workshop.reportStep(reported);
            broadcastChanges();
        }
    }

    /** Server side: finish the build by hand with that name. */
    public void engrave(String name) {
        if (workshop != null) {
            workshop.engrave(name);
            broadcastChanges();
        }
    }

    /** Whether Build now would work, as far as this side can tell. */
    public boolean canBuild() {
        ItemStack held = shuttle();
        return !held.isEmpty() && ShuttleItem.ofTier(ShuttleItem.tier(held) + 1) != null
                && ShuttleBuild.ready(sheet(), ShuttleWorkshopBlockEntity.countSupplies(supplies));
    }

    /** How many of a supply id the supply slots hold. */
    public int supplied(String id) {
        return ShuttleWorkshopBlockEntity.countSupplies(supplies).getOrDefault(id, 0);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.SHUTTLE_WORKSHOP.get());
    }

    /**
     * Installs as much of the stack as the shuttle takes and shrinks it by
     * that. A tool that is replaced goes to the player on the server.
     *
     * @return how many were installed
     */
    public int install(ItemStack stack) {
        if (building()) {
            return 0;
        }
        ItemStack held = shuttle();
        int room = ShuttleUpgrades.room(held, stack, tiers);
        int count = Math.min(room, stack.getCount());
        if (count <= 0) {
            return 0;
        }
        ItemStack edited = held.copy();
        ShuttleModules modules = ShuttleModules.of(held);
        ShuttleUpgrades.Install install = ShuttleUpgrades.kind(stack);
        Optional<Kind> kind = ShuttleUpgrades.counted(install);
        if (kind.isPresent()) {
            ShuttleModules.set(edited, modules.withCount(kind.get(), modules.count(kind.get()) + count));
        } else {
            modules.tool().ifPresent(old -> giveBack(new ItemStack(old)));
            ShuttleModules.set(edited, modules.withTool(Optional.of(stack.getItem())));
        }
        stack.shrink(count);
        shuttle.setStackInSlot(0, edited);
        return count;
    }

    private void giveBack(ItemStack stack) {
        if (!player.level().isClientSide()) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    /** Takes one count of the kind, or the tool, out of the shuttle and gives it to the player. */
    private boolean remove(Optional<Kind> kind) {
        ItemStack held = shuttle();
        if (held.isEmpty() || building()) {
            return false;
        }
        ShuttleModules modules = ShuttleModules.of(held);
        ItemStack edited = held.copy();
        if (kind.isPresent()) {
            int count = modules.count(kind.get());
            if (count <= 0) {
                return false;
            }
            ShuttleModules.set(edited, modules.withCount(kind.get(), count - 1));
            giveBack(new ItemStack(ShuttleUpgrades.item(kind.get())));
        } else {
            Optional<Item> tool = modules.tool();
            if (tool.isEmpty()) {
                return false;
            }
            ShuttleModules.set(edited, modules.withTool(Optional.empty()));
            giveBack(new ItemStack(tool.get()));
        }
        shuttle.setStackInSlot(0, edited);
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return switch (id) {
            case BUTTON_REMOVE_SPEED -> remove(Optional.of(Kind.SPEED));
            case BUTTON_REMOVE_HANDLING -> remove(Optional.of(Kind.HANDLING));
            case BUTTON_REMOVE_TOOL -> remove(Optional.empty());
            case BUTTON_BUILD_NOW -> workshop != null && workshop.buildNow();
            case BUTTON_BUILD_BY_HAND -> workshop != null && workshop.startHandBuild();
            case BUTTON_CANCEL -> workshop != null && workshop.cancelHandBuild();
            default -> false;
        };
    }

    /**
     * Shift click: a shuttle goes into the shuttle slot, an upgrade or tool
     * the shuttle takes is installed, other items go to the supplies, and
     * anything in the workshop back to the inventory.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || index == INSTALL_SLOT || building() && index < INVENTORY_START) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int end = slots.size();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, end, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ShuttleItem.isShuttle(stack)) {
            if (!moveItemStackTo(stack, SHUTTLE_SLOT, SHUTTLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (install(stack) == 0 && !moveItemStackTo(stack, SUPPLY_START, INVENTORY_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    /**
     * The install slot: always empty. What goes in is installed into the
     * shuttle at once; it takes only what the shuttle has room for, so the
     * rest of a stack stays on the cursor.
     */
    private final class InstallSlot extends Slot {

        InstallSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !building() && ShuttleUpgrades.room(shuttle(), stack, tiers) > 0;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return ShuttleUpgrades.room(shuttle(), stack, tiers);
        }

        @Override
        public void set(ItemStack stack) {
            if (!stack.isEmpty()) {
                install(stack.copy());
            }
            container.setItem(0, ItemStack.EMPTY);
            setChanged();
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
