package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.zone.ShelfZones;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Stands the rack's sample on its floor and middle boards inside the frame,
 * two items per level, no counts, under the beam label: the bay's piece of
 * the zone stripe, the code sticker on the shelf's code bay and, from the
 * shelf's leftmost bay, the zone name along the shelf.
 */
public class HeavyRackRenderer extends LiveBlockEntityRenderer<HeavyRackBlockEntity> {

    private static final float ITEM_SCALE = 0.33F;
    private static final float HALF = ITEM_SCALE / 2.0F;
    private static final double[] COLUMNS = {-0.22, 0.22};
    private static final double[] BOARD_TOPS = {1.0 / 16.0, 9.0 / 16.0};
    private static final double BACK = -0.05;
    private static final float YAW = 20.0F;

    public HeavyRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The leftmost bay of a zoned shelf draws the zone name across the whole shelf. */
    @Override
    public AABB getRenderBoundingBox(HeavyRackBlockEntity rack) {
        AABB own = new AABB(rack.getBlockPos());
        Level level = rack.getLevel();
        if (level == null || rack.zoneDefinition() == null) {
            return own;
        }
        List<BlockPos> shelf = ShelfZones.shelf(level, rack.getBlockPos());
        return own.minmax(new AABB(shelf.getFirst())).minmax(new AABB(shelf.getLast()));
    }

    @Override
    protected void render(HeavyRackBlockEntity rack, float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                          int packedLight) {
        List<ItemStack> sample = rack.displayed();
        ZoneDefinition zone = rack.zoneDefinition();
        boolean sticker = rack.codeBay() && !rack.locationCode().isEmpty();
        Level level = rack.getLevel();
        if (level == null || (sample.isEmpty() && zone == null && !sticker)) {
            return;
        }
        Direction facing = rack.getBlockState().getValue(HeavyRackBlock.FACING);
        int light = LevelRenderer.getLightCoords(level, rack.getBlockPos());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Now x runs to the aisle viewer's right, y up and plus z towards the aisle.

        if (zone != null) {
            List<BlockPos> shelf = ShelfZones.shelf(level, rack.getBlockPos());
            int index = shelf.indexOf(rack.getBlockPos());
            RackBeamLabel.stripe(poseStack, collector, zone, index > 0, index < shelf.size() - 1, light);
            if (index == 0) {
                RackBeamLabel.name(poseStack, collector, zone.name(), RackBeamLabel.nameRun(shelf.size(), stickerBay(level, shelf)), light);
            }
        }
        if (sticker) {
            RackBeamLabel.sticker(poseStack, collector, rack.locationCode(), light);
        }

        int shown = Math.min(sample.size(), COLUMNS.length * BOARD_TOPS.length);
        for (int i = 0; i < shown; i++) {
            int column = i % COLUMNS.length;
            double cy = BOARD_TOPS[i / COLUMNS.length] + HALF;
            poseStack.pushPose();
            poseStack.translate(COLUMNS[column], cy, BACK);
            poseStack.mulPose(Axis.YP.rotationDegrees(column == 0 ? YAW : -YAW));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            RenderSubmit.item(poseStack, collector, sample.get(i), ItemDisplayContext.FIXED, level, i, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The index of the shelf's bay showing the code sticker, -1 when none does. */
    static int stickerBay(Level level, List<BlockPos> shelf) {
        for (int i = 0; i < shelf.size(); i++) {
            if (level.getBlockEntity(shelf.get(i)) instanceof StorageNodeBlockEntity bay && bay.codeBay() && !bay.locationCode().isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
