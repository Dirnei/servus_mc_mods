package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.naming.MachineNameMenu;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.shuttles.locker.BoxInfo;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * The Parcel Locker GUI: the locker's name and, read only, every box with
 * who ordered it, as it stood when the GUI opened. Items come out only
 * through the doors a Handheld scan opens.
 */
public class ParcelLockerMenu extends AbstractContainerMenu implements MachineNameMenu {

    public static final int MAP_Y = 16;
    /** Height of the box map: the locker's 32 pixels, 18 for each S box's 8. */
    public static final int MAP_H = 72;
    public static final int WIDTH = 176;
    public static final int HEIGHT = MAP_Y + MAP_H + 8;

    private final ParcelLockerBlockEntity locker;
    private final List<BoxInfo> boxes;
    private final ContainerLevelAccess access;

    public ParcelLockerMenu(int containerId, ParcelLockerBlockEntity locker, List<BoxInfo> boxes) {
        super(ServusShuttlesMenus.PARCEL_LOCKER.get(), containerId);
        this.locker = locker;
        this.boxes = List.copyOf(boxes);
        this.access = ContainerLevelAccess.create(locker.getLevel(), locker.getBlockPos());
    }

    public static ParcelLockerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        List<BoxInfo> boxes = new ArrayList<>();
        for (int i = 0; i < LockerLayout.BOXES.size(); i++) {
            boxes.add(BoxInfo.read(buf));
        }
        if (playerInventory.player.level().getBlockEntity(pos) instanceof ParcelLockerBlockEntity locker) {
            return new ParcelLockerMenu(containerId, locker, boxes);
        }
        throw new IllegalStateException("No Parcel Locker at " + pos);
    }

    public ParcelLockerBlockEntity locker() {
        return locker;
    }

    /** What the player sees of each box, in {@link LockerLayout#BOXES} order. */
    public List<BoxInfo> boxes() {
        return boxes;
    }

    @Override
    public Optional<NamedMachine> namedMachine() {
        return Optional.of(locker);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.PARCEL_LOCKER.get());
    }
}
