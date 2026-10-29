package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankShape;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Forms an aquarium from the part a carrot clicked: the Fluid Tank resting on it gives the box, and only the box's
 * footprint below and above it is checked, never other blocks.
 */
public final class AquariumForming {

    /** Why an aquarium formed or did not. */
    public enum Outcome { FORMED, NO_TANK, FLOOR_MISSING, LID_MISSING, NOT_WATER }

    /** The outcome and, when a tank was found, its box. */
    public record Result(Outcome outcome, @Nullable TankShape box) {

        public boolean formed() {
            return outcome == Outcome.FORMED;
        }
    }

    private AquariumForming() {
    }

    /** Forms the aquarium through the loose part at {@code clicked}, when every part is in place. */
    public static Result tryForm(Level level, BlockPos clicked) {
        BlockState clickedState = level.getBlockState(clicked);
        if (!(clickedState.getBlock() instanceof AquariumPartBlock part)) {
            return new Result(Outcome.NO_TANK, null);
        }
        BlockPos tankPos = part.tankNeighbour(clicked);
        if (!(level.getBlockEntity(tankPos) instanceof FluidTankBlockEntity tank)) {
            return new Result(Outcome.NO_TANK, null);
        }
        Optional<TankNetwork> network = tank.network();
        if (network.isEmpty()) {
            return new Result(Outcome.NO_TANK, null);
        }
        TankShape box = network.get().boxAt(Conversions.toNodePos(tankPos));
        if (!box.isBox()) {
            return new Result(Outcome.NO_TANK, box);
        }
        List<BlockPos> floors = footprint(box, box.minY() - 1);
        List<BlockPos> lids = footprint(box, box.maxY() + 1);
        if (!allLoose(level, floors, AquariumFloorBlock.class)) {
            return new Result(Outcome.FLOOR_MISSING, box);
        }
        if (!allLoose(level, lids, AquariumLidBlock.class)) {
            return new Result(Outcome.LID_MISSING, box);
        }
        FluidStack content = tank.tankContent();
        if (!content.isEmpty() && !content.is(FluidTags.WATER)) {
            return new Result(Outcome.NOT_WATER, box);
        }
        form(level, box, floors, lids);
        return new Result(Outcome.FORMED, box);
    }

    private static List<BlockPos> footprint(TankShape box, int y) {
        List<BlockPos> positions = new ArrayList<>(box.blocksPerLayer());
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                positions.add(new BlockPos(x, y, z));
            }
        }
        return positions;
    }

    private static boolean allLoose(Level level, List<BlockPos> positions, Class<? extends AquariumPartBlock> block) {
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos) || !block.isInstance(level.getBlockState(pos).getBlock())
                    || !(level.getBlockEntity(pos) instanceof AquariumPartBlockEntity part) || part.formed()) {
                return false;
            }
        }
        return true;
    }

    private static void form(Level level, TankShape box, List<BlockPos> floors, List<BlockPos> lids) {
        BlockPos master = new BlockPos(box.minX(), box.minY() - 1, box.minZ());
        if (level.getBlockEntity(master) instanceof AquariumPartBlockEntity part) {
            part.becomeMaster(box);
        }
        List<BlockPos> parts = new ArrayList<>(floors);
        parts.addAll(lids);
        for (BlockPos at : parts) {
            if (!at.equals(master) && level.getBlockEntity(at) instanceof AquariumPartBlockEntity part) {
                part.joinMaster(master);
            }
        }
        for (BlockPos at : parts) {
            level.invalidateCapabilities(at);
        }
        level.playSound(null, master, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** What the carrot click says: formed with the size, or what is missing. */
    public static Component message(Result result) {
        return switch (result.outcome()) {
            case FORMED -> {
                TankShape box = result.box();
                yield Component.translatable("message.servus_delicacies.aquarium.formed",
                        box == null ? 0 : box.width(), box == null ? 0 : box.depth(), box == null ? 0 : box.height());
            }
            case NO_TANK -> Component.translatable("message.servus_delicacies.aquarium.no_tank");
            case FLOOR_MISSING -> Component.translatable("message.servus_delicacies.aquarium.floor_missing");
            case LID_MISSING -> Component.translatable("message.servus_delicacies.aquarium.lid_missing");
            case NOT_WATER -> Component.translatable("message.servus_delicacies.aquarium.not_water");
        };
    }
}
