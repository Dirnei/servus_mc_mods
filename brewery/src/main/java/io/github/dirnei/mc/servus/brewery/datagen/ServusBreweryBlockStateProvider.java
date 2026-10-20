package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.crop.HopPoleBlock;
import io.github.dirnei.mc.servus.brewery.crop.HopStage;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.CropBlock;

/**
 * Block states and models of the barley crop and the Hop Pole. The pole is a
 * 2 px post; leaf planes sit 0.5 px off its faces and the cone planes further
 * out, so no two faces share a plane.
 */
final class ServusBreweryBlockStateProvider extends BlockStateProvider {

    private static final float POLE_MIN = 7;
    private static final float POLE_MAX = 9;
    private static final float LEAF_IN = 6.5F;
    private static final float LEAF_OUT = 9.5F;
    private static final float LEAF_FROM = 5;
    private static final float LEAF_TO = 11;
    private static final float CONE_IN = 4.5F;
    private static final float CONE_OUT = 11.5F;
    private static final float CONE_FROM = 3;
    private static final float CONE_TO = 13;

    ServusBreweryBlockStateProvider(PackOutput output) {
        super(output, ServusBreweryIds.MOD_ID);
    }

    @Override
    protected void registerStatesAndModels() {
        barley();
        hopPole();
    }

    private void barley() {
        CropBlock barley = ServusBreweryBlocks.BARLEY.get();
        getVariantBuilder(barley).forAllStates(state -> {
            int age = state.getValue(CropBlock.AGE);
            ModelFile model = models().withExistingParent("barley_stage" + age, mcLoc("block/crop"))
                    .texture("crop", modLoc("block/barley_stage" + age));
            return ConfiguredModel.builder().modelFile(model).build();
        });
    }

    private void hopPole() {
        Map<HopStage, ModelFile> models = new EnumMap<>(HopStage.class);
        models.put(HopStage.BARE, pole("hop_pole"));
        models.put(HopStage.YOUNG, leaves(pole("hop_pole_young"), "hop_vine_young", 8));
        models.put(HopStage.GROWN, leaves(pole("hop_pole_grown"), "hop_vine", 16));
        models.put(HopStage.CONES, cones(leaves(pole("hop_pole_cones"), "hop_vine", 16)));
        getVariantBuilder(ServusBreweryBlocks.HOP_POLE.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(models.get(HopPoleBlock.stageOf(state))).build());
    }

    /** The bare 2 px post with the bark texture's middle columns on every side. */
    private BlockModelBuilder pole(String name) {
        return models().getBuilder(name)
                .texture("pole", modLoc("block/hop_pole"))
                .texture("particle", modLoc("block/hop_pole"))
                .element().from(POLE_MIN, 0, POLE_MIN).to(POLE_MAX, 16, POLE_MAX)
                .allFaces((direction, face) -> {
                    face.texture("#pole");
                    if (direction.getAxis() == Direction.Axis.Y) {
                        face.uvs(POLE_MIN, POLE_MIN, POLE_MAX, POLE_MAX);
                    } else {
                        face.uvs(POLE_MIN, 0, POLE_MAX, 16);
                    }
                })
                .end();
    }

    /** Four leaf planes around the post, {@code height} px tall from the bottom. */
    private BlockModelBuilder leaves(BlockModelBuilder model, String texture, float height) {
        model.texture("vine", modLoc("block/" + texture)).ao(false);
        planes(model, "#vine", LEAF_IN, LEAF_OUT, LEAF_FROM, LEAF_TO, height);
        return model;
    }

    /** Four cone planes outside the leaf planes, over the full height. */
    private BlockModelBuilder cones(BlockModelBuilder model) {
        model.texture("cones", modLoc("block/hop_cones"));
        planes(model, "#cones", CONE_IN, CONE_OUT, CONE_FROM, CONE_TO, 16);
        return model;
    }

    /** A square of four double sided planes: x planes and z planes at {@code near} and {@code far}, each spanning {@code from} to {@code to}. */
    private static void planes(BlockModelBuilder model, String texture, float near, float far, float from, float to, float height) {
        float vTop = 16 - height;
        for (float x : new float[] {near, far}) {
            model.element().from(x, 0, from).to(x, height, to)
                    .face(Direction.WEST).texture(texture).uvs(from, vTop, to, 16).end()
                    .face(Direction.EAST).texture(texture).uvs(from, vTop, to, 16).end();
        }
        for (float z : new float[] {near, far}) {
            model.element().from(from, 0, z).to(to, height, z)
                    .face(Direction.NORTH).texture(texture).uvs(from, vTop, to, 16).end()
                    .face(Direction.SOUTH).texture(texture).uvs(from, vTop, to, 16).end();
        }
    }
}
