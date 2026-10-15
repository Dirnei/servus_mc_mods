package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;

/**
 * Datagen for the elevator segment: four corner posts always, and a head with
 * a motor beam on the top segment of a shaft. Decks and the lift car fold and
 * move, so the elevator renderer draws them.
 */
final class ElevatorModels {

    private ElevatorModels() {
    }

    static void elevator(BlockStateProvider provider, Block block, String name) {
        Identifier texture = provider.modLoc("block/" + name);
        BlockModelBuilder frame = provider.models().getBuilder(name + "_frame")
                .texture("particle", texture).texture("skin", texture);
        int[][] posts = {{0, 0}, {14, 0}, {0, 14}, {14, 14}};
        for (int[] post : posts) {
            frame.element().from(post[0], 0, post[1]).to(post[0] + 2, 16, post[1] + 2)
                    .allFaces((direction, face) -> face.texture("#skin")).end();
        }

        Identifier headTexture = provider.modLoc("block/" + name + "_head");
        BlockModelBuilder head = provider.models().getBuilder(name + "_head")
                .texture("particle", texture).texture("skin", texture).texture("head", headTexture);
        // A closed ring of beams over the posts.
        head.element().from(0, 14, 0).to(16, 16, 2).allFaces((direction, face) -> face.texture("#skin")).end();
        head.element().from(0, 14, 14).to(16, 16, 16).allFaces((direction, face) -> face.texture("#skin")).end();
        head.element().from(0, 14, 2).to(2, 16, 14).allFaces((direction, face) -> face.texture("#skin")).end();
        head.element().from(14, 14, 2).to(16, 16, 14).allFaces((direction, face) -> face.texture("#skin")).end();
        // The motor beam across the middle with the motor box hanging below it.
        head.element().from(2, 13, 6).to(14, 16, 10).allFaces((direction, face) -> face.texture("#head")).end();
        head.element().from(5, 10, 5).to(11, 13, 11).allFaces((direction, face) -> face.texture("#head")).end();

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        builder.part().modelFile(frame).addModel();
        builder.part().modelFile(head).addModel().condition(ConnectableBlock.UP, false);

        provider.simpleBlockItem(block, frame);
    }
}
