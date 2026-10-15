package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleTanks;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.MachineStatsManager;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Principle Dropper: fills from pipes and buckets, one tank per principle, and drips what the Gnome Hollow
 * directly below it still drinks into that Hollow until its vein is ripe. It never gives fluid back and never
 * pushes into any other block.
 */
public class PrincipleDropperBlockEntity extends BlockEntity {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "principle_dropper");
    /** Block event id of a drip; its parameter has one bit per principle dripped, by ordinal. */
    static final int DRIP_EVENT = 1;
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    private static final float DROP_SCALE = 0.6F;

    private final PrincipleTanks tanks = new PrincipleTanks(DEFAULTS.fluidCapacity(), false, this::setChanged);
    private int timer;

    public PrincipleDropperBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.PRINCIPLE_DROPPER.get(), pos, state);
    }

    /** The tanks, which only ever fill from outside; also what the fluid capability exposes. */
    public PrincipleTanks tanks() {
        return tanks;
    }

    private static MachineStats stats() {
        return MachineStatsManager.INSTANCE.get(ID, DEFAULTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PrincipleDropperBlockEntity dropper) {
        dropper.tick();
    }

    private void tick() {
        MachineStats stats = stats();
        tanks.setCapacity(stats.fluidCapacity());
        if (++timer < Math.max(1, stats.workTicks())) {
            return;
        }
        timer = 0;
        int dripped = drip(stats.fluidPerCycle());
        if (dripped != 0 && level != null) {
            level.blockEvent(worldPosition, getBlockState().getBlock(), DRIP_EVENT, dripped);
        }
    }

    /** Drips into the Hollow below; returns one bit per principle that moved, 0 when nothing did. */
    private int drip(int perDrip) {
        if (level == null || tanks.contents().isEmpty() || !(level.getBlockEntity(worldPosition.below()) instanceof GnomeHollowBlockEntity hollow)) {
            return 0;
        }
        return hollow.dripNeed().map(need -> {
            Principles plan = PrincipleDrip.plan(tanks.contents(), need.stepCost(), need.room(), perDrip);
            int bits = 0;
            for (Principle principle : Principle.values()) {
                int taken = hollow.receiveDrip(principle, plan.get(principle));
                if (taken > 0) {
                    tanks.take(Principles.NONE.with(principle, taken));
                    bits |= 1 << principle.ordinal();
                }
            }
            return bits;
        }).orElse(0);
    }

    /** On clients a drip shows as drops of each dripped principle's colour falling from the spout. */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != DRIP_EVENT) {
            return super.triggerEvent(id, param);
        }
        if (level != null && level.isClientSide()) {
            for (Principle principle : Principle.values()) {
                if ((param & (1 << principle.ordinal())) != 0) {
                    spawnDrop(principle);
                }
            }
        }
        return true;
    }

    private void spawnDrop(Principle principle) {
        DustParticleOptions drop = new DustParticleOptions(principle.color() & 0xFFFFFF, DROP_SCALE);
        double x = worldPosition.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.06;
        double z = worldPosition.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.06;
        level.addParticle(drop, x, worldPosition.getY() + 0.02, z, 0, -0.05, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueIo.putTag(output, "Principles", tanks.save(ValueIo.registries(this)));
        output.putInt("DripTimer", timer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tanks.load(ValueIo.getTag(input, "Principles"));
        timer = input.getIntOr("DripTimer", 0);
    }
}
