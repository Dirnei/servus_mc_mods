package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.rail.RailLook;
import io.github.dirnei.mc.servus.shuttles.rail.RailLook.Box;
import io.github.dirnei.mc.servus.shuttles.rail.RailLook.Piece;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;

/**
 * Datagen for the track rails: one model per {@link RailLook} piece (single,
 * end, straight, curve, cross) picked by the sides that carry rail, a dock
 * beam per dock side and the track up to the beam when the rail runs straight
 * into it. Sleepers use the wood board texture so {@code MaterialModel} shows the
 * rail's wood; heads use one texture per rail type.
 */
final class RailModels {

    private static final BooleanProperty[] CONNECTED = {ConnectableBlock.NORTH, ConnectableBlock.EAST, ConnectableBlock.SOUTH, ConnectableBlock.WEST};
    private static final BooleanProperty[] DOCK = {RailBlock.DOCK_NORTH, RailBlock.DOCK_EAST, RailBlock.DOCK_SOUTH, RailBlock.DOCK_WEST};

    private RailModels() {
    }

    static void rail(BlockStateProvider provider, Block block, String name) {
        Identifier head = provider.modLoc("block/" + name + "_head");
        Identifier dockTexture = provider.modLoc("block/" + name + "_dock");
        Map<Piece, BlockModelBuilder> pieces = new EnumMap<>(Piece.class);
        for (Piece piece : Piece.values()) {
            pieces.put(piece, model(provider, name + "_" + piece.name().toLowerCase(Locale.ROOT), head, dockTexture,
                    RailLook.boxes(piece)));
        }
        BlockModelBuilder dock = model(provider, name + "_dock", head, dockTexture, RailLook.dock());
        BlockModelBuilder dockInline = model(provider, name + "_dock_inline", head, dockTexture, RailLook.dockInline());

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (int mask = 0; mask < 16; mask++) {
            piecePart(builder, mask, pieces);
        }
        for (int i = 0; i < 4; i++) {
            int rotation = i * 90;
            builder.part().modelFile(dock).rotationY(rotation).addModel().condition(DOCK[i], true);
            // The track up to the beam when no rail comes in from the sides beside the dock.
            MultiPartBlockStateBuilder.PartBuilder inline = builder.part().modelFile(dockInline).rotationY(rotation).addModel();
            inline.nestedGroup().condition(DOCK[i], true).end();
            notRail(inline, (i + 1) % 4);
            notRail(inline, (i + 3) % 4);
        }

        provider.itemModels().withExistingParent(name, provider.modLoc("block/" + name + "_straight"))
                .transforms()
                .transform(ItemDisplayContext.GUI).rotation(50, 45, 0).translation(0, 2, 0).scale(0.8F).end()
                .transform(ItemDisplayContext.FIXED).rotation(-90, 0, 0).translation(0, 0, -1).scale(0.6F).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 3, 0).scale(0.35F).end()
                .end();
    }

    /** A model of boxes: sleepers in the wood board texture, heads and dock beams in their own textures. */
    private static BlockModelBuilder model(BlockStateProvider provider, String model, Identifier head, Identifier dock,
                                           List<Box> boxes) {
        BlockModelBuilder builder = provider.models().withExistingParent(model, "minecraft:block/block")
                .texture("particle", head).texture("head", head).texture("wood", MaterialKind.WOOD.placeholder()).texture("dock", dock);
        for (Box box : boxes) {
            String texture = switch (box.part()) {
                case SLEEPER -> "#wood";
                case HEAD -> "#head";
                case DOCK -> "#dock";
            };
            ModelBuilder<BlockModelBuilder>.ElementBuilder element = builder.element()
                    .from((float) box.x0(), (float) box.y0(), (float) box.z0())
                    .to((float) box.x1(), (float) box.y1(), (float) box.z1());
            element.allFaces((direction, face) -> {
                face.texture(texture);
                if (direction == Direction.DOWN && box.y0() == 0) {
                    face.cullface(Direction.DOWN);
                }
            });
            element.end();
        }
        return builder;
    }

    /** The piece for a mask of sides that carry rail (bits north, east, south, west), conditioned on all four sides. */
    private static void piecePart(MultiPartBlockStateBuilder builder, int mask, Map<Piece, BlockModelBuilder> pieces) {
        boolean[] rail = new boolean[4];
        for (int i = 0; i < 4; i++) {
            rail[i] = (mask & (1 << i)) != 0;
        }
        RailLook.Placed placed = RailLook.of(rail[0], rail[1], rail[2], rail[3]);
        BlockModelBuilder model = pieces.get(placed.piece());
        if (mask == 0) {
            // A rail without rail sides lies along its docks: east west when a dock lies east or west and none north or south.
            MultiPartBlockStateBuilder.PartBuilder across = builder.part().modelFile(model).rotationY(90).addModel();
            sides(across, rail);
            across.nestedGroup().condition(RailBlock.DOCK_NORTH, false).end();
            across.nestedGroup().condition(RailBlock.DOCK_SOUTH, false).end();
            across.nestedGroup().useOr().condition(RailBlock.DOCK_EAST, true).condition(RailBlock.DOCK_WEST, true).end();
            MultiPartBlockStateBuilder.PartBuilder along = builder.part().modelFile(model).addModel();
            sides(along, rail);
            along.nestedGroup().useOr()
                    .nestedGroup().condition(RailBlock.DOCK_NORTH, true).endNestedGroup()
                    .nestedGroup().condition(RailBlock.DOCK_SOUTH, true).endNestedGroup()
                    .nestedGroup().condition(RailBlock.DOCK_EAST, false).condition(RailBlock.DOCK_WEST, false).endNestedGroup()
                    .end();
            return;
        }
        MultiPartBlockStateBuilder.PartBuilder part = builder.part().modelFile(model).rotationY(placed.rotation()).addModel();
        sides(part, rail);
    }

    private static void sides(MultiPartBlockStateBuilder.PartBuilder part, boolean[] rail) {
        for (int i = 0; i < 4; i++) {
            if (rail[i]) {
                part.nestedGroup().condition(CONNECTED[i], true).condition(DOCK[i], false).end();
            } else {
                notRail(part, i);
            }
        }
    }

    /** A side carries no rail: not connected, or a dock, which is drawn on its own. */
    private static void notRail(MultiPartBlockStateBuilder.PartBuilder part, int side) {
        part.nestedGroup().useOr().condition(CONNECTED[side], false).condition(DOCK[side], true).end();
    }
}
