package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumLidBlock;
import io.github.dirnei.mc.servus.delicacies.registry.DelicaciesFluids;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * The aquarium parts' models: the Floor as one cube (copper cabinet sides, gravel top), the Lid as one 4 px slab whose
 * underside is the light panel, dark or lit. The lit panel is its own element holding only the down face, so it can
 * glow without the top and sides; the two elements share the box but no face.
 */
final class AquariumModels {

    private AquariumModels() {
    }

    static void register(BlockStateProvider provider) {
        Identifier side = provider.modLoc("block/aquarium_floor_side");
        ModelFile floor = provider.models().cube("aquarium_floor", provider.mcLoc("block/cut_copper"),
                provider.modLoc("block/aquarium_floor_top"), side, side, side, side).texture("particle", side);
        provider.simpleBlockWithItem(ServusDelicaciesBlocks.AQUARIUM_FLOOR.get(), floor);

        ModelFile dark = lid(provider, "aquarium_lid", "block/aquarium_lid_light_off", 0);
        ModelFile lit = lid(provider, "aquarium_lid_lit", "block/aquarium_lid_light", 15);
        provider.getVariantBuilder(ServusDelicaciesBlocks.AQUARIUM_LID.get()).forAllStates(state ->
                new ConfiguredModel[] {new ConfiguredModel(state.getValue(AquariumLidBlock.LIT) ? lit : dark)});
        provider.simpleBlockItem(ServusDelicaciesBlocks.AQUARIUM_LID.get(), dark);

        provider.simpleBlock(DelicaciesFluids.MURKY_WATER_BLOCK.get(),
                provider.models().getBuilder(DelicaciesFluids.MURKY_WATER_ID).texture("particle", provider.mcLoc("block/water_still")));
    }

    private static ModelFile lid(BlockStateProvider provider, String name, String light, int emission) {
        int height = AquariumLidBlock.HEIGHT;
        BlockModelBuilder lid = provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("particle", provider.modLoc("block/aquarium_lid_top"))
                .texture("top", provider.modLoc("block/aquarium_lid_top"))
                .texture("side", provider.modLoc("block/aquarium_lid_side"))
                .texture("light", provider.modLoc(light));
        BlockModelBuilder.ElementBuilder body = lid.element().from(0, 0, 0).to(16, height, 16);
        body.face(Direction.UP).texture("#top").end();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            body.face(direction).texture("#side").uvs(0, 16 - height, 16, 16).cullface(direction).end();
        }
        body.end();
        BlockModelBuilder.ElementBuilder panel = lid.element().from(0, 0, 0).to(16, height, 16);
        panel.face(Direction.DOWN).texture("#light").cullface(Direction.DOWN).end();
        if (emission > 0) {
            panel.lightEmission(emission);
        }
        panel.end();
        return lid;
    }
}
