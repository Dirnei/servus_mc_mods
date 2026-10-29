package io.github.dirnei.mc.servus.shuttles.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The warehouse zones of every team, saved with the overworld. Every create,
 * edit and delete goes through here on the server thread, is sent to every
 * player and tells every warehouse.
 */
public final class WarehouseZoneData extends SavedData {

    private static final Codec<ZoneList.Zone<ZoneFilter>> ZONE_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("Id").forGetter(ZoneList.Zone::id),
            UUIDUtil.CODEC.fieldOf("Team").forGetter(ZoneList.Zone::team),
            Codec.STRING.fieldOf("Name").forGetter(ZoneList.Zone::name),
            Codec.INT.optionalFieldOf("Colour", 0).forGetter(ZoneList.Zone::colour),
            ZoneFilter.CODEC.optionalFieldOf("Filter", ZoneFilter.EMPTY).forGetter(ZoneList.Zone::filter))
            .apply(i, ZoneList.Zone::new));

    /** The saved form; public so a GameTest can check a save and load. */
    public static final Codec<WarehouseZoneData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ZONE_CODEC.listOf().optionalFieldOf("Zones", List.of()).forGetter(data -> List.copyOf(data.list.all())),
            Codec.INT.optionalFieldOf("NextId", ZoneList.NONE + 1).forGetter(data -> data.list.nextId()))
            .apply(i, (zones, nextId) -> new WarehouseZoneData(new ZoneList<>(zones, nextId))));

    private static final SavedDataType<WarehouseZoneData> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("servus_warehouse_zones"), WarehouseZoneData::new, CODEC);

    private final ZoneList<ZoneFilter> list;
    private MinecraftServer server;

    public WarehouseZoneData() {
        this(new ZoneList<>());
    }

    private WarehouseZoneData(ZoneList<ZoneFilter> list) {
        this.list = list;
    }

    public static WarehouseZoneData get(MinecraftServer server) {
        WarehouseZoneData data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        data.server = server;
        return data;
    }

    /** The zones; change them only through this class. */
    public ZoneList<ZoneFilter> list() {
        return list;
    }

    /** The zone with that id, null for no zone or a deleted one. */
    public ZoneDefinition definition(int id) {
        ZoneList.Zone<ZoneFilter> zone = list.get(id);
        return zone == null ? null : ZoneDefinition.of(zone);
    }

    public Optional<ZoneList.Zone<ZoneFilter>> create(UUID team, String name, int colour, ZoneFilter filter) {
        return changedIfPresent(list.create(team, name, colour, filter));
    }

    public Optional<ZoneList.Zone<ZoneFilter>> edit(int id, String name, int colour, ZoneFilter filter) {
        return changedIfPresent(list.edit(id, name, colour, filter));
    }

    public boolean delete(int id) {
        boolean deleted = list.delete(id);
        if (deleted) {
            changed();
        }
        return deleted;
    }

    private Optional<ZoneList.Zone<ZoneFilter>> changedIfPresent(Optional<ZoneList.Zone<ZoneFilter>> zone) {
        zone.ifPresent(z -> changed());
        return zone;
    }

    private void changed() {
        setDirty();
        ZoneSync.broadcast(list);
        if (server != null) {
            ZoneSync.tellWarehouses(server);
        }
    }
}
