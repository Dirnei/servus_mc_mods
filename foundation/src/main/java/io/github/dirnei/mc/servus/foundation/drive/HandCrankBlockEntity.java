package io.github.dirnei.mc.servus.foundation.drive;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Turns the driven block while a player holds the use key on the crank: each
 * driving tick hands it a share of a turn's work, and each full turn tires the
 * player. The server sets {@link HandCrankBlock#TURNING} only when the crank
 * starts or stops; clients run the arm's {@link Flywheel} from it.
 */
public class HandCrankBlockEntity extends BlockEntity {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "hand_crank");
    private static final float DEGREES_PER_TICK = 360.0F / CrankTurns.TICKS_PER_TURN;

    private final CrankTurns turns = new CrankTurns();
    private final Flywheel arm = new Flywheel(DEGREES_PER_TICK);
    private @Nullable Player cranker;

    public HandCrankBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.HAND_CRANK_ENTITY.get(), pos, state);
    }

    /** The side of the driven block the crank sits on, and so the way from it to the crank. */
    private Direction facing() {
        return getBlockState().getValue(HandCrankBlock.FACING);
    }

    /** The block the crank turns, or null when it takes no drive. Server only. */
    private @Nullable Drivable driven() {
        return level.getCapability(ServusFoundationCapabilities.DRIVE, worldPosition.relative(facing().getOpposite()), facing());
    }

    /** One use click by {@code player}; holding the use key repeats it. Server only. */
    public void click(Player player) {
        cranker = player;
        turns.click(level.getGameTime());
    }

    /** One server tick: drives while clicked, and tells clients when the crank starts or stops. */
    public void serverTick() {
        long now = level.getGameTime();
        boolean held = cranker != null && !cranker.isRemoved() && turns.held(now);
        if (held && turns.driving(now)) {
            drive(cranker);
        }
        if (!held) {
            cranker = null;
        }
        BlockState state = getBlockState();
        if (state.getValue(HandCrankBlock.TURNING) != held) {
            level.setBlock(worldPosition, state.setValue(HandCrankBlock.TURNING, held), Block.UPDATE_CLIENTS);
        }
    }

    /** One driving tick: turns the driven block and, if it wants work, hands it this tick's share. */
    private void drive(Player player) {
        Drivable driven = driven();
        if (driven == null) {
            return;
        }
        driven.turn();
        if (!driven.wantsWork()) {
            return;
        }
        DriveStats stats = DriveStatsManager.INSTANCE.get(ID, DriveStats.HAND_CRANK);
        CrankTurns.Step step = turns.step(stats.workPerTurn());
        if (step.work() > 0) {
            driven.addWork(step.work());
        }
        if (step.turnDone()) {
            player.causeFoodExhaustion(stats.exhaustionPerTurn());
        }
    }

    /** One client tick of the arm's flywheel. */
    public void clientTick() {
        arm.tick(getBlockState().getValue(HandCrankBlock.TURNING));
    }

    /** The arm's angle in degrees for a frame. */
    public float armAngle(float partialTick) {
        return arm.angle(partialTick);
    }
}
