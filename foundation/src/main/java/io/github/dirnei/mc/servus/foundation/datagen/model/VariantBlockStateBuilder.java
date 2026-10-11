package io.github.dirnei.mc.servus.foundation.datagen.model;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** A {@code variants} block state file with one entry per block state. */
public final class VariantBlockStateBuilder {

    private final Block owner;
    private final Map<String, List<ConfiguredModel>> variants = new LinkedHashMap<>();

    VariantBlockStateBuilder(Block owner) {
        this.owner = owner;
    }

    /** One entry for every state of the block. */
    public VariantBlockStateBuilder forAllStates(Function<BlockState, ConfiguredModel[]> mapper) {
        for (BlockState state : owner.getStateDefinition().getPossibleStates()) {
            variants.put(key(state), Arrays.asList(mapper.apply(state)));
        }
        return this;
    }

    /** A single entry for a block without properties. */
    void single(ConfiguredModel... models) {
        variants.put("", Arrays.asList(models));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String key(BlockState state) {
        return state.getProperties().stream()
                .map(property -> property.getName() + "=" + ((Property) property).getName(state.getValue(property)))
                .collect(Collectors.joining(","));
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        variants.forEach((key, models) -> json.add(key, ConfiguredModel.toJson(models)));
        JsonObject root = new JsonObject();
        root.add("variants", json);
        return root;
    }
}
