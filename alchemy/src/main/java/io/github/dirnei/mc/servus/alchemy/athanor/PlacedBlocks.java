package io.github.dirnei.mc.servus.alchemy.athanor;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.Placed;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Reads and writes {@link BlockAt}s as block states. */
public final class PlacedBlocks {

    private PlacedBlocks() {
    }

    /** The block state as a {@link Placed}: its block id and its properties by name. */
    public static Placed placed(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return new Placed() {
            @Override
            public String block() {
                return id;
            }

            @Override
            public String property(String name) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
                return property == null ? null : valueName(state, property);
            }
        };
    }

    /**
     * The state {@code target} names: {@code current} with the named properties set to their first accepted value when it
     * is already that block, else the block's default state with them. Empty for an unknown block.
     */
    public static Optional<BlockState> state(BlockAt target, BlockState current) {
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(target.block()));
        if (block.isEmpty()) {
            return Optional.empty();
        }
        BlockState state = current.is(block.get()) ? current : block.get().defaultBlockState();
        for (Map.Entry<String, String> entry : target.state().entrySet()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property != null) {
                state = withValue(state, property, entry.getValue());
            }
        }
        return Optional.of(state);
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }
}
