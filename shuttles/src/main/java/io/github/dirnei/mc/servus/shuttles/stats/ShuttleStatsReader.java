package io.github.dirnei.mc.servus.shuttles.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.shuttles.ServusShuttles;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.BuildStep;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.StepKind;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code storage/shuttle.json} into {@link ShuttleTiers}. Every value
 * that is missing falls back to {@link ShuttleTiers#DEFAULT}, tier by tier.
 */
final class ShuttleStatsReader {

    private static final int MAX_TIERS = 3;

    private ShuttleStatsReader() {
    }

    static ShuttleTiers read(JsonElement element) {
        ShuttleTiers fallback = ShuttleTiers.DEFAULT;
        if (!(element instanceof JsonObject json)) {
            return fallback;
        }
        try {
            JsonObject speed = object(json, "speed");
            JsonObject stops = object(json, "stops");
            List<ShuttleTiers.Tier> tiers = new ArrayList<>();
            JsonArray array = json.has("tiers") && json.get("tiers").isJsonArray() ? json.getAsJsonArray("tiers") : new JsonArray();
            // Three shuttle items exist, so more tiers than three are cut off.
            int count = Math.min(MAX_TIERS, array.isEmpty() ? fallback.topTier() : array.size());
            for (int i = 0; i < count; i++) {
                ShuttleTiers.Tier base = fallback.tier(i + 1);
                JsonObject tier = i < array.size() && array.get(i).isJsonObject() ? array.get(i).getAsJsonObject() : new JsonObject();
                tiers.add(new ShuttleTiers.Tier(
                        integer(tier, "stacks", base.stacks()),
                        integer(tier, "max_speed", base.maxSpeed()),
                        integer(tier, "max_handling", base.maxHandling()),
                        tier.has("build") ? supplies(tier.getAsJsonArray("build")) : (i == 0 ? List.of() : base.build()),
                        tier.has("steps") && tier.get("steps").isJsonArray() ? steps(tier.getAsJsonArray("steps")) : base.steps()));
            }
            return new ShuttleTiers(
                    number(speed, "base", fallback.baseSpeed()),
                    number(speed, "per_upgrade", fallback.speedPerCount()),
                    number(speed, "accel", fallback.accel()),
                    number(speed, "accel_per_upgrade", fallback.accelPerCount()),
                    integer(stops, "stop_ticks", fallback.stopTicks()),
                    integer(stops, "stack_ticks", fallback.stackTicks()),
                    number(stops, "handling_factor", fallback.handlingFactor()),
                    tiers);
        } catch (RuntimeException e) {
            ServusShuttles.LOGGER.warn("Shuttle stats: falling back to defaults, {}", e.getMessage());
            return fallback;
        }
    }

    private static List<ShuttleTiers.Supply> supplies(JsonArray array) {
        List<ShuttleTiers.Supply> supplies = new ArrayList<>();
        for (JsonElement entry : array) {
            JsonObject supply = entry.getAsJsonObject();
            supplies.add(new ShuttleTiers.Supply(supply.get("item").getAsString(), integer(supply, "count", 1)));
        }
        return supplies;
    }

    /** The steps of a build by hand; an unknown kind drops its step, counts clamp to the kind's range. */
    static List<BuildStep> steps(JsonArray array) {
        List<BuildStep> steps = new ArrayList<>();
        for (JsonElement entry : array) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject step = entry.getAsJsonObject();
            String kind = step.has("kind") ? step.get("kind").getAsString() : "";
            StepKind.byId(kind).ifPresentOrElse(
                    found -> steps.add(new BuildStep(found, integer(step, "count", 1))),
                    () -> ServusShuttles.LOGGER.warn("Shuttle stats: unknown build step kind '{}'", kind));
        }
        return steps;
    }

    private static JsonObject object(JsonObject json, String field) {
        return json.has(field) && json.get(field).isJsonObject() ? json.getAsJsonObject(field) : new JsonObject();
    }

    private static int integer(JsonObject json, String field, int fallback) {
        return json.has(field) ? json.get(field).getAsInt() : fallback;
    }

    private static double number(JsonObject json, String field, double fallback) {
        return json.has(field) ? json.get(field).getAsDouble() : fallback;
    }
}
