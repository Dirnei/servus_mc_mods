package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * Datagen helper for wood blocks: a full cube of board panels ({@link Wood#BOARD},
 * shown in the block's wood) under a full cube of cutout overlay textures per
 * face (iron frame, lamps, screens). The two layers are coplanar, like the
 * vanilla grass overlay; the overlay is drawn after the boards.
 */
public final class LayeredModels {

    private LayeredModels() {
    }

    /** Front on north, the side texture on the other three sides, top and bottom texture above and below. */
    public static ModelFile orientable(BlockStateProvider provider, String name, ResourceLocation side, ResourceLocation front,
                                       ResourceLocation top) {
        Map<Direction, ResourceLocation> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.NORTH, front);
        faces.put(Direction.SOUTH, side);
        faces.put(Direction.EAST, side);
        faces.put(Direction.WEST, side);
        faces.put(Direction.UP, top);
        faces.put(Direction.DOWN, top);
        return cube(provider, name, faces);
    }

    /** One overlay texture per face. */
    public static ModelFile cube(BlockStateProvider provider, String name, Map<Direction, ResourceLocation> faces) {
        BlockModelBuilder builder = provider.models().getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .renderType("minecraft:cutout")
                .texture("particle", faces.get(Direction.NORTH))
                .texture("board", Wood.BOARD);
        for (Direction direction : Direction.values()) {
            builder.texture(direction.getSerializedName(), faces.get(direction));
        }
        builder.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((direction, face) -> face.texture("#board").cullface(direction)).end();
        builder.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((direction, face) -> face.texture("#" + direction.getSerializedName()).cullface(direction)).end();
        return builder;
    }
}
