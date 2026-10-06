package io.github.dirnei.mc.servus.storage.rack;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.rack.RackClimb.Box;
import io.github.dirnei.mc.servus.storage.rack.RackClimb.Contact;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Lets players climb the exposed sides of a Heavy Duty Rack like a ladder.
 * The rack is a full cube, so the vanilla ladder hook, which only asks the
 * block the player stands in, never sees it. This runs at the tail of the
 * player tick instead: when a vertical face of the player's box rests on a
 * rack side, it writes the vanilla ladder motion for the next tick and
 * clears the fall distance. The front and the back of a rack stay walls.
 */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class RackClimbing {

    /** How far off a block boundary a box face may be and still count as touching. */
    static final double TOLERANCE = 1.0E-3;

    private RackClimbing() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        tick(event.getEntity());
    }

    /** One tick of the climb check for {@code player}. Public so a GameTest can drive it. */
    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide() && !player.isLocalPlayer()) {
            return;
        }
        if (player.isSpectator() || player.getAbilities().flying || player.isPassenger()) {
            return;
        }
        if (player.onGround() && !player.horizontalCollision) {
            return;
        }
        if (!touchesRackSide(level, player.getBoundingBox())) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        double y = RackClimb.verticalMotion(motion.y, player.horizontalCollision, player.isShiftKeyDown(), player.getGravity());
        player.setDeltaMovement(motion.x, y, motion.z);
        player.resetFallDistance();
    }

    private static boolean touchesRackSide(Level level, AABB bb) {
        Box box = new Box(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Contact contact : RackClimb.contacts(box, TOLERANCE)) {
            cursor.set(contact.cellX(), contact.cellY(), contact.cellZ());
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof HeavyRackBlock)) {
                continue;
            }
            Direction facing = state.getValue(HeavyRackBlock.FACING);
            if (RackClimb.isSide(facing.getStepX(), facing.getStepZ(), contact.dx(), contact.dz())) {
                return true;
            }
        }
        return false;
    }
}
