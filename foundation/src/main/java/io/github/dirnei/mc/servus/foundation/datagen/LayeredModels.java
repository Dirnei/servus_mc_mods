package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Datagen helper for wood blocks: a full cube of board panels (the wood kind's {@link MaterialKind#placeholder()},
 * shown in the block's wood) under a full cube of cutout overlay textures per
 * face (iron frame, lamps, screens). The two layers are coplanar, like the
 * vanilla grass overlay; the overlay is drawn after the boards.
 */
public final class LayeredModels {

    private LayeredModels() {
    }

    /** Front on north, the side texture on the other three sides, top and bottom texture above and below. */
    public static ModelFile orientable(BlockStateProvider provider, String name, Identifier side, Identifier front,
                                       Identifier top) {
        Map<Direction, Identifier> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.NORTH, front);
        faces.put(Direction.SOUTH, side);
        faces.put(Direction.EAST, side);
        faces.put(Direction.WEST, side);
        faces.put(Direction.UP, top);
        faces.put(Direction.DOWN, top);
        return cube(provider, name, faces);
    }

    /** One overlay texture per face. */
    public static ModelFile cube(BlockStateProvider provider, String name, Map<Direction, Identifier> faces) {
        BlockModelBuilder builder = provider.models().getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", faces.get(Direction.NORTH))
                .texture("board", MaterialKind.WOOD.placeholder());
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
