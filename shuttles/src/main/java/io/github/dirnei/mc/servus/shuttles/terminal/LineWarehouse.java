package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import java.util.Optional;

/**
 * The warehouse a data line belongs to: none, one, or a conflict when the line
 * reaches more than one warehouse.
 *
 * @param warehouse the line's warehouse while the kind is {@link LineClassification.Kind#ONE}, whatever its status, else null
 */
public record LineWarehouse(LineClassification.Kind kind, WarehouseNetwork warehouse) {

    public static final LineWarehouse NONE = new LineWarehouse(LineClassification.Kind.NONE, null);

    /** The refusal and status message key, without its prefix, of a line in conflict. */
    public static final String CONFLICT_KEY = "line_conflict";

    public static LineWarehouse of(LineClassification.Result<WarehouseNetwork> result) {
        return new LineWarehouse(result.kind(), result.warehouse());
    }

    public boolean conflict() {
        return kind == LineClassification.Kind.CONFLICT;
    }

    /** The line's warehouse while it is online and the line is not in conflict. */
    public Optional<WarehouseNetwork> online() {
        return warehouse != null && warehouse.status() == WarehouseNetwork.Status.ONLINE ? Optional.of(warehouse) : Optional.empty();
    }
}
