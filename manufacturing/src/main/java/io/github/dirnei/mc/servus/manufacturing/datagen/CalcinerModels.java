package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * Models of the Calciner parts, built from boxes with vanilla textures. In model space the
 * front faces north, so the player's right when looking at it is west (-x).
 */
final class CalcinerModels {

    private static final String BRICKS = "block/bricks";
    private static final String COPPER = "block/copper_block";
    private static final String CUT_COPPER = "block/cut_copper";
    private static final String GLASS = "block/glass";
    private static final String IRON = "block/iron_block";
    private static final String IRON_BARS = "block/iron_bars";

    private final BlockStateProvider states;

    CalcinerModels(BlockStateProvider states) {
        this.states = states;
    }

    void register() {
        part(ServusManufacturingBlocks.FIREBOX.get(), firebox("calciner_firebox", "block/coal_block"), firebox("calciner_firebox_lit", "block/magma"));
        ModelFile ashPan = ashPan();
        part(ServusManufacturingBlocks.ASH_PAN.get(), ashPan, ashPan);
        ModelFile alembic = alembic();
        part(ServusManufacturingBlocks.ALEMBIC.get(), alembic, alembic);
        ModelFile receiver = receiver();
        part(ServusManufacturingBlocks.RECEIVER.get(), receiver, receiver);
        ModelFile hood = hood();
        part(ServusManufacturingBlocks.SULFUR_HOOD.get(), hood, hood);
        ModelFile shell = states.models().getExistingFile(states.mcLoc("block/bricks"));
        states.getVariantBuilder(ServusManufacturingBlocks.CALCINER_SHELL.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(shell).build());
    }

    /** A part turned by its facing, with a lit model for burning parts. */
    private void part(Block block, ModelFile unlit, ModelFile lit) {
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CalcinerPartBlock.LIT) ? lit : unlit)
                .rotationY(((int) state.getValue(CalcinerPartBlock.FACING).toYRot() + 180) % 360)
                .build());
        states.simpleBlockItem(block, unlit);
    }

    private BlockModelBuilder builder(String name) {
        return states.models().withExistingParent(name, states.mcLoc("block/block"));
    }

    /** A brick block with an opening in the front: frame, back wall and sides. */
    private BlockModelBuilder brickFrame(String name, int openingBottom, int openingTop) {
        BlockModelBuilder model = builder(name).texture("bricks", states.mcLoc(BRICKS)).texture("particle", states.mcLoc(BRICKS));
        box(model, 0, 0, 3, 16, 16, 16, "#bricks");
        box(model, 0, 0, 0, 3, 16, 3, "#bricks");
        box(model, 13, 0, 0, 16, 16, 3, "#bricks");
        box(model, 3, openingTop, 0, 13, 16, 3, "#bricks");
        if (openingBottom > 0) {
            box(model, 3, 0, 0, 13, openingBottom, 3, "#bricks");
        }
        return model;
    }

    /** Coals behind an iron grate. */
    private ModelFile firebox(String name, String coals) {
        BlockModelBuilder model = brickFrame(name, 2, 12)
                .texture("coals", states.mcLoc(coals))
                .texture("bars", states.mcLoc(IRON_BARS))
                .renderType("cutout");
        box(model, 3, 2, 1, 13, 5, 3, "#coals");
        plane(model, 3, 2, 13, 12, 1, "#bars");
        return model;
    }

    /** A low drawer with an iron tray, where the Salt gathers. */
    private ModelFile ashPan() {
        BlockModelBuilder model = brickFrame("calciner_ash_pan", 1, 7).texture("iron", states.mcLoc(IRON));
        box(model, 3, 1, 1, 13, 2, 3, "#iron");
        box(model, 3, 2, 1, 4, 4, 3, "#iron");
        box(model, 12, 2, 1, 13, 4, 3, "#iron");
        return model;
    }

    /** The copper still head that catches the vapour. */
    private ModelFile alembic() {
        BlockModelBuilder model = builder("alembic")
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        box(model, 3, 0, 3, 13, 3, 13, "#cut");
        box(model, 4, 3, 4, 12, 10, 12, "#copper");
        box(model, 6, 10, 6, 10, 13, 10, "#copper");
        box(model, 5, 13, 5, 11, 15, 11, "#cut");
        // The beak that leads the vapour to the Receiver on the player's right.
        box(model, 0, 8, 7, 4, 10, 9, "#cut");
        return model;
    }

    /** A glass flask on a copper stand, where the Mercury drips in. */
    private ModelFile receiver() {
        BlockModelBuilder model = builder("receiver")
                .texture("glass", states.mcLoc(GLASS)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(GLASS))
                .renderType("cutout");
        box(model, 3, 0, 3, 13, 1, 13, "#cut");
        box(model, 4, 1, 4, 12, 9, 12, "#glass");
        box(model, 6, 9, 6, 10, 14, 10, "#glass");
        return model;
    }

    /** The copper hood where the Sulfur rises and settles. */
    private ModelFile hood() {
        BlockModelBuilder model = builder("sulfur_hood")
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        box(model, 2, 0, 2, 14, 3, 14, "#cut");
        box(model, 4, 3, 4, 12, 6, 12, "#copper");
        box(model, 5, 6, 5, 11, 14, 11, "#copper");
        box(model, 4, 14, 4, 12, 16, 12, "#cut");
        return model;
    }

    static void box(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
        model.element().from(x1, y1, z1).to(x2, y2, z2).allFaces((direction, face) -> face.texture(texture)).end();
    }

    /** A flat plane facing north and south, for bars and grates. */
    static void plane(BlockModelBuilder model, float x1, float y1, float x2, float y2, float z, String texture) {
        model.element().from(x1, y1, z).to(x2, y2, z)
                .face(Direction.NORTH).texture(texture).end()
                .face(Direction.SOUTH).texture(texture).end()
                .end();
    }
}
