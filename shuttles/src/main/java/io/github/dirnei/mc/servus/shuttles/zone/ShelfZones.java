package io.github.dirnei.mc.servus.shuttles.zone;

import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Zones are set per shelf: every bay of a Heavy Duty Rack or Fluid Rack
 * shelf holds the same zone id. This is the one writer that keeps it so.
 */
public final class ShelfZones {

    private ShelfZones() {
    }

    /**
     * The bays of the shelf at {@code pos} from left to right seen from the
     * front; just {@code pos} for a block that is no rack bay.
     */
    public static List<BlockPos> shelf(BlockGetter level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        List<BlockPos> bays = block instanceof HeavyRackBlock ? HeavyRackBlock.shelf(level, pos).reversed()
                : block instanceof FluidRackBlock ? FluidRackBlock.shelf(level, pos) : List.of();
        return bays.isEmpty() ? List.of(pos) : bays;
    }

    /** The key of the shelf at {@code pos}: its leftmost bay. */
    public static BlockPos key(BlockGetter level, BlockPos pos) {
        return shelf(level, pos).getFirst();
    }

    /** The block entities of the shelf's bays that take a zone, from left to right. */
    public static List<StorageNodeBlockEntity> bays(BlockGetter level, BlockPos pos) {
        List<StorageNodeBlockEntity> bays = new ArrayList<>();
        for (BlockPos bay : shelf(level, pos)) {
            if (level.getBlockEntity(bay) instanceof StorageNodeBlockEntity node && node.zonable()) {
                bays.add(node);
            }
        }
        return bays;
    }

    /** Puts every bay of the shelf at {@code pos} into the zone with that id, or clears it with {@link ZoneList#NONE}. */
    public static void set(Level level, BlockPos pos, int zone) {
        List<StorageNodeBlockEntity> bays = bays(level, pos);
        for (StorageNodeBlockEntity bay : bays) {
            bay.setZone(zone);
        }
        for (StorageNodeBlockEntity bay : bays) {
            if (bay instanceof FluidRackBlockEntity fluidRack) {
                fluidRack.shelfChanged();
            }
        }
    }

    /**
     * Settles the zone of the shelf a bay at {@code placed} just joined: when
     * the other bays are zoned the new bay takes their zone; when they are not
     * and the new bay carries a zone, the whole shelf takes it; else the shelf
     * stays unzoned. A bay standing alone keeps its zone.
     */
    public static void onJoin(Level level, BlockPos placed) {
        List<StorageNodeBlockEntity> bays = bays(level, placed);
        int zone = ZoneList.NONE;
        StorageNodeBlockEntity newcomer = null;
        for (StorageNodeBlockEntity bay : bays) {
            if (bay.getBlockPos().equals(placed)) {
                newcomer = bay;
            } else if (zone == ZoneList.NONE && bay.zoneDefinition() != null) {
                zone = bay.zoneId();
            }
        }
        if (zone == ZoneList.NONE && newcomer != null && newcomer.zoneDefinition() != null) {
            zone = newcomer.zoneId();
        }
        for (StorageNodeBlockEntity bay : bays) {
            bay.setZone(zone);
        }
    }
}
