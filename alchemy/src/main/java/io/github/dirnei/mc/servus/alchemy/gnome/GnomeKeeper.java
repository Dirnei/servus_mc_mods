package io.github.dirnei.mc.servus.alchemy.gnome;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The one gnome a hollow keeps: remembers him by UUID, brings him out, finds him again after a load and sends him away.
 * A gnome whose hollow does not keep him leaves by himself, so a lost gnome found twice never stays twice. Server side.
 */
public final class GnomeKeeper {

    private static final String GNOME_TAG = "Gnome";
    private static final int CHECK_INTERVAL = 20;
    /** Checks in a row without him before he counts as lost; entities load a little after the chunk of their hollow. */
    private static final int LOST_AFTER_CHECKS = 3;
    private static final double SEARCH_RADIUS = 8;

    private final GnomeKind kind;
    private final Runnable changed;
    private @Nullable UUID gnome;
    private int missed;

    /**
     * @param kind    the kind of gnome this hollow keeps
     * @param changed called when the kept gnome changes, so the hollow saves
     */
    public GnomeKeeper(GnomeKind kind, Runnable changed) {
        this.kind = kind;
        this.changed = changed;
    }

    /** Whether the gnome with {@code id} is the one kept here. */
    public boolean keeps(UUID id) {
        return id.equals(gnome);
    }

    /** The kept gnome while he is loaded. */
    public Optional<Gnome> find(ServerLevel level) {
        return gnome != null && level.getEntity(gnome) instanceof Gnome found ? Optional.of(found) : Optional.empty();
    }

    /** Brings a new gnome out at {@code at} now, looking along {@code yRot}; the gnome kept so far leaves. */
    public void summon(ServerLevel level, BlockPos hollow, Vec3 at, float yRot) {
        dismiss(level);
        Gnome.spawn(level, kind, hollow, at, yRot).ifPresent(spawned -> keep(spawned.getUUID()));
    }

    /**
     * Called every tick while the hollow wants its gnome: every second it looks for him, and once he stays lost it takes
     * back a gnome of this hollow standing near.
     *
     * @return true when no gnome was found and the hollow should {@link #summon} a new one
     */
    public boolean tick(ServerLevel level, BlockPos hollow) {
        if (Math.floorMod(level.getGameTime() + hollow.hashCode(), CHECK_INTERVAL) != 0) {
            return false;
        }
        if (find(level).isPresent()) {
            missed = 0;
            return false;
        }
        if (++missed < LOST_AFTER_CHECKS) {
            return false;
        }
        missed = 0;
        Optional<Gnome> near = level.getEntitiesOfClass(Gnome.class, new AABB(hollow).inflate(SEARCH_RADIUS),
                candidate -> candidate.livesAt(hollow) && candidate.kind() == kind).stream().findFirst();
        near.ifPresent(found -> keep(found.getUUID()));
        return near.isEmpty();
    }

    /** Sends the kept gnome away if he is loaded and forgets him. */
    public void dismiss(ServerLevel level) {
        find(level).ifPresent(Gnome::discard);
        keep(null);
    }

    private void keep(@Nullable UUID id) {
        gnome = id;
        missed = 0;
        changed.run();
    }

    public void save(ValueOutput output) {
        if (gnome != null) {
            output.store(GNOME_TAG, UUIDUtil.CODEC, gnome);
        }
    }

    public void load(ValueInput input) {
        gnome = input.read(GNOME_TAG, UUIDUtil.CODEC).orElse(null);
    }

    /**
     * The side of the hollow at {@code hollow} a gnome comes out of: {@code side}, the side it opens to, when there is room
     * in front of it, else the first other side with room, else {@code side} anyway.
     */
    public static Direction exitSide(ServerLevel level, BlockPos hollow, Direction side) {
        if (Gnome.roomAt(level, hollow.relative(side))) {
            return side;
        }
        for (Direction other : Direction.Plane.HORIZONTAL) {
            if (Gnome.roomAt(level, hollow.relative(other))) {
                return other;
            }
        }
        return side;
    }
}
