package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.shuttles.rail.RailLook;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;

/**
 * Datagen for the elevator segment: four corner posts always, and a head with
 * a motor beam on the top segment of a shaft. Decks, the lift car and the
 * floor rail fold and move, so the elevator renderer draws them; the item
 * model shows posts, head and rail together.
 */
final class ElevatorModels {

    private static final int[][] POSTS = {{0, 0}, {14, 0}, {0, 14}, {14, 14}};

    private ElevatorModels() {
    }

    static void elevator(BlockStateProvider provider, Block block, String name) {
        Identifier texture = provider.modLoc("block/" + name);
        Identifier headTexture = provider.modLoc("block/" + name + "_head");
        BlockModelBuilder frame = provider.models().getBuilder(name + "_frame")
                .texture("particle", texture).texture("skin", texture);
        posts(frame);

        BlockModelBuilder head = provider.models().getBuilder(name + "_head")
                .texture("particle", texture).texture("skin", texture).texture("head", headTexture);
        head(head);

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        builder.part().modelFile(frame).addModel();
        builder.part().modelFile(head).addModel().condition(ConnectableBlock.UP, false);

        // The item shows a whole one segment shaft: posts, head and the rail on its floor.
        BlockModelBuilder item = provider.models().withExistingParent(name + "_item", "minecraft:block/block")
                .texture("particle", texture).texture("skin", texture).texture("head", headTexture)
                .texture("rail_head", provider.modLoc("block/rail_head")).texture("sleeper", Identifier.withDefaultNamespace("block/oak_planks"));
        posts(item);
        head(item);
        rail(item);
        provider.simpleBlockItem(block, item);
    }

    private static void posts(BlockModelBuilder builder) {
        for (int[] post : POSTS) {
            builder.element().from(post[0], 0, post[1]).to(post[0] + 2, 16, post[1] + 2)
                    .allFaces((direction, face) -> face.texture("#skin")).end();
        }
    }

    /** A ring of beams between the post tops, and the motor beam across the middle with the motor box hanging below it. */
    private static void head(BlockModelBuilder builder) {
        builder.element().from(2, 14, 0).to(14, 16, 2).allFaces((direction, face) -> face.texture("#skin")).end();
        builder.element().from(2, 14, 14).to(14, 16, 16).allFaces((direction, face) -> face.texture("#skin")).end();
        builder.element().from(0, 14, 2).to(2, 16, 14).allFaces((direction, face) -> face.texture("#skin")).end();
        builder.element().from(14, 14, 2).to(16, 16, 14).allFaces((direction, face) -> face.texture("#skin")).end();
        builder.element().from(2, 13, 6).to(14, 16, 10).allFaces((direction, face) -> face.texture("#head")).end();
        builder.element().from(5, 10, 5).to(11, 13, 11).allFaces((direction, face) -> face.texture("#head")).end();
    }

    /** A straight Warehouse Rail on the floor, its sleepers ending at the posts. */
    private static void rail(BlockModelBuilder builder) {
        for (RailLook.Box box : RailLook.boxes(RailLook.Piece.STRAIGHT)) {
            boolean sleeper = box.part() == RailLook.Part.SLEEPER;
            float x0 = sleeper ? Math.max(2, (float) box.x0()) : (float) box.x0();
            float x1 = sleeper ? Math.min(14, (float) box.x1()) : (float) box.x1();
            String texture = sleeper ? "#sleeper" : "#rail_head";
            builder.element().from(x0, (float) box.y0(), (float) box.z0()).to(x1, (float) box.y1(), (float) box.z1())
                    .allFaces((direction, face) -> face.texture(texture)).end();
        }
    }
}
