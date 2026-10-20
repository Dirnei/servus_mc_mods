package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.beer.CaskBlock;
import io.github.dirnei.mc.servus.brewery.beer.FermentingVatBlock;
import io.github.dirnei.mc.servus.brewery.beer.VatContent;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;

/**
 * Models of the Fermenting Vat and the Cask. The vat's side walls draw no faces on the planes of the front and back
 * walls, its floor draws no side faces and the contents are a single up face below the rim, so no faces share a plane.
 * The Cask's tap draws no back face (on the barrel front), its spout no top and its handle no bottom (on the tap).
 */
final class CellarModels {

    private static final String SIDE = "#side";
    private static final String TAP = "#tap";
    private static final float WALL = 1;

    private final BlockStateProvider provider;

    CellarModels(BlockStateProvider provider) {
        this.provider = provider;
    }

    void register() {
        vat();
        cask();
    }

    private void vat() {
        Map<VatContent, ModelFile> models = new EnumMap<>(VatContent.class);
        models.put(VatContent.EMPTY, vatFrame("fermenting_vat"));
        models.put(VatContent.WORT, contents(vatFrame("fermenting_vat_wort"), "vat_wort"));
        models.put(VatContent.FERMENTING, contents(vatFrame("fermenting_vat_fermenting"), "vat_foam"));
        models.put(VatContent.BEER, contents(vatFrame("fermenting_vat_beer"), "vat_beer"));
        FermentingVatBlock vat = ServusBreweryBlocks.FERMENTING_VAT.get();
        provider.getVariantBuilder(vat).forAllStates(state ->
                ConfiguredModel.builder().modelFile(models.get(state.getValue(FermentingVatBlock.CONTENT))).build());
        provider.simpleBlockItem(vat, models.get(VatContent.EMPTY));
    }

    /** Four stave walls up to the rim and a plank floor. */
    private BlockModelBuilder vatFrame(String name) {
        float rim = FermentingVatBlock.HEIGHT;
        float in = 16 - WALL;
        BlockModelBuilder model = provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("side", provider.modLoc("block/fermenting_vat_side"))
                .texture("floor", provider.mcLoc("block/spruce_planks"))
                .texture("particle", provider.modLoc("block/fermenting_vat_side"));
        model.element().from(0, 0, 0).to(16, rim, WALL)
                .allFaces((direction, face) -> face.texture(SIDE).cullface(cull(direction))).end();
        model.element().from(0, 0, in).to(16, rim, 16)
                .allFaces((direction, face) -> face.texture(SIDE).cullface(cull(direction))).end();
        model.element().from(0, 0, WALL).to(WALL, rim, in)
                .allFacesExcept((direction, face) -> face.texture(SIDE).cullface(cull(direction)),
                        Set.of(Direction.NORTH, Direction.SOUTH)).end();
        model.element().from(in, 0, WALL).to(16, rim, in)
                .allFacesExcept((direction, face) -> face.texture(SIDE).cullface(cull(direction)),
                        Set.of(Direction.NORTH, Direction.SOUTH)).end();
        model.element().from(WALL, 0, WALL).to(in, WALL, in)
                .face(Direction.UP).texture("#floor").end()
                .face(Direction.DOWN).texture(SIDE).cullface(Direction.DOWN).end()
                .end();
        return model;
    }

    /** Only the bottom faces touch the block below; the open top and the sides inside stay visible. */
    private static Direction cull(Direction direction) {
        return direction == Direction.DOWN ? Direction.DOWN : null;
    }

    /** The surface of what the vat holds: one up face below the rim. */
    private BlockModelBuilder contents(BlockModelBuilder model, String texture) {
        float surface = FermentingVatBlock.SURFACE;
        model.texture("content", provider.modLoc("block/" + texture));
        model.element().from(WALL, surface, WALL).to(16 - WALL, surface, 16 - WALL)
                .face(Direction.UP).texture("#content").end()
                .end();
        return model;
    }

    private void cask() {
        float[] barrel = CaskBlock.BARREL;
        BlockModelBuilder model = provider.models().withExistingParent("cask", provider.mcLoc("block/block"))
                .texture("side", provider.mcLoc("block/barrel_side"))
                .texture("top", provider.mcLoc("block/barrel_top"))
                .texture("bottom", provider.mcLoc("block/barrel_bottom"))
                .texture("tap", provider.modLoc("block/cask_tap"))
                .texture("particle", provider.mcLoc("block/barrel_side"));
        model.element().from(barrel[0], barrel[1], barrel[2]).to(barrel[3], barrel[4], barrel[5])
                .allFaces((direction, face) -> {
                    switch (direction) {
                        case UP -> face.texture("#top").uvs(1, 1, 15, 15);
                        case DOWN -> face.texture("#bottom").uvs(1, 1, 15, 15).cullface(Direction.DOWN);
                        default -> face.texture(SIDE).uvs(1, 0, 15, 16);
                    }
                })
                .end();
        model.element().from(7, 4, -2).to(9, 6, 1)
                .allFacesExcept((direction, face) -> face.texture(TAP).uvs(6, 6, 9, 9), Set.of(Direction.SOUTH)).end();
        model.element().from(7.25F, 2.5F, -2).to(8.75F, 4, -1)
                .allFacesExcept((direction, face) -> face.texture(TAP).uvs(6, 6, 9, 9), Set.of(Direction.UP)).end();
        model.element().from(7.5F, 6, -1.5F).to(8.5F, 8, -0.5F)
                .allFacesExcept((direction, face) -> face.texture(TAP).uvs(6, 6, 9, 9), Set.of(Direction.DOWN)).end();
        provider.getVariantBuilder(ServusBreweryBlocks.CASK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(ModelRotations.rotationY(state.getValue(CaskBlock.FACING)))
                .build());
        provider.simpleBlockItem(ServusBreweryBlocks.CASK.get(), model);
    }
}
