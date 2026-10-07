package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.PumpBlock;
import java.util.Map;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.ChimneyBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusManufacturingBlockStateProvider extends BlockStateProvider {

    ServusManufacturingBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusManufacturingIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        cubeAllWithItem(ServusManufacturingBlocks.IRON_DEPOSIT.get());
        cubeAllWithItem(ServusManufacturingBlocks.COPPER_DEPOSIT.get());
        cubeAllWithItem(ServusManufacturingBlocks.COAL_DEPOSIT.get());
        cubeAllWithItem(ServusManufacturingBlocks.STONE_DEPOSIT.get());

        machine(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), "burner_extractor");
        machine(ServusManufacturingBlocks.BURNER_SMELTER.get(), "burner_smelter");
        machine(ServusManufacturingBlocks.COAL_GENERATOR.get(), "coal_generator");
        machine(ServusManufacturingBlocks.ASSEMBLER.get(), "assembler");
        machine(ServusManufacturingBlocks.RESEARCH_LAB.get(), "research_lab");
        machine(ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get(), "electric_extractor");
        machine(ServusManufacturingBlocks.CRUSHER.get(), "crusher");
        machine(ServusManufacturingBlocks.CHUNK_LOADER.get(), "chunk_loader");
        pump(ServusManufacturingBlocks.BURNER_PUMP.get(), "burner_pump");
        pump(ServusManufacturingBlocks.ELECTRIC_PUMP.get(), "electric_pump");

        sink();
        chimney();
        sinkFan();
    }

    /**
     * Brick body with a recessed front: a brick frame, a sooty back wall three
     * pixels deep and an iron grate just behind the front plane. The fan is
     * drawn in the recess by the block entity renderer.
     */
    private void sink() {
        ResourceLocation bricks = mcLoc("block/bricks");
        BlockModelBuilder model = models().getBuilder("sink")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .renderType("cutout")
                .texture("particle", bricks)
                .texture("bricks", bricks)
                .texture("recess", modLoc("block/sink_recess"))
                .texture("grate", modLoc("block/sink_front"));
        // Body behind the recess.
        model.element().from(0, 0, 3).to(16, 16, 16)
                .allFaces((direction, face) -> {
                    if (direction == Direction.NORTH) {
                        face.texture("#recess");
                    } else {
                        face.texture("#bricks").cullface(direction);
                    }
                })
                .end();
        // Frame around the recess: top, bottom, left and right bars.
        frameBar(model, 0, 13, 16, 16, Direction.UP, Direction.DOWN);
        frameBar(model, 0, 0, 16, 3, Direction.DOWN, Direction.UP);
        frameBar(model, 0, 3, 3, 13, Direction.WEST, Direction.EAST);
        frameBar(model, 13, 3, 16, 13, Direction.EAST, Direction.WEST);
        // The grate, a flat plane just behind the front.
        model.element().from(3, 3, 0.5F).to(13, 13, 0.5F)
                .face(Direction.NORTH).texture("#grate").uvs(0, 0, 16, 16).end()
                .face(Direction.SOUTH).texture("#grate").uvs(16, 0, 0, 16).end()
                .end();
        getVariantBuilder(ServusManufacturingBlocks.SINK.get()).forAllStates(state -> {
            Direction facing = state.getValue(SinkBlock.FACING);
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(ServusManufacturingBlocks.SINK.get(), model);
    }

    /** One bar of the Sink's front frame, three pixels deep; {@code outer} faces the world, {@code inner} the recess. */
    private static void frameBar(BlockModelBuilder model, float x0, float y0, float x1, float y1, Direction outer, Direction inner) {
        BlockModelBuilder.ElementBuilder bar = model.element().from(x0, y0, 0).to(x1, y1, 3)
                .face(Direction.NORTH).texture("#bricks").cullface(Direction.NORTH).end()
                .face(outer).texture("#bricks").cullface(outer).end()
                .face(inner).texture("#bricks").end();
        if (outer.getAxis() == Direction.Axis.Y) {
            // Horizontal bars span the full width: their ends are on the block sides.
            bar.face(Direction.EAST).texture("#bricks").cullface(Direction.EAST).end()
                    .face(Direction.WEST).texture("#bricks").cullface(Direction.WEST).end();
        }
        bar.end();
    }

    /** Full brick block inside a column; the top block carries a wider cap with an open flue. */
    private void chimney() {
        ResourceLocation bricks = mcLoc("block/bricks");
        ModelFile column = models().cubeAll("chimney", bricks);
        BlockModelBuilder top = models().getBuilder("chimney_top")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", bricks)
                .texture("bricks", bricks)
                .texture("top", modLoc("block/chimney_top"));
        top.element().from(0, 0, 0).to(16, 13, 16)
                .allFacesExcept((direction, face) -> face.texture("#bricks").cullface(direction), java.util.Set.of(Direction.UP))
                .end();
        top.element().from(-1, 13, -1).to(17, 16, 17)
                .face(Direction.UP).texture("#top").uvs(0, 0, 16, 16).cullface(Direction.UP).end()
                .face(Direction.DOWN).texture("#bricks").uvs(0, 0, 16, 16).end()
                .face(Direction.NORTH).texture("#bricks").uvs(0, 0, 16, 3).end()
                .face(Direction.SOUTH).texture("#bricks").uvs(0, 0, 16, 3).end()
                .face(Direction.EAST).texture("#bricks").uvs(0, 0, 16, 3).end()
                .face(Direction.WEST).texture("#bricks").uvs(0, 0, 16, 3).end()
                .end();
        getVariantBuilder(ServusManufacturingBlocks.CHIMNEY.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ChimneyBlock.TOP) ? top : column)
                .build());
        simpleBlockItem(ServusManufacturingBlocks.CHIMNEY.get(), top);
    }

    /**
     * The Sink fan for the block entity renderer: a hub and four pitched blades
     * in the recess, centred on the front, in the model's north facing frame.
     */
    private void sinkFan() {
        BlockModelBuilder fan = models().getBuilder("sink_fan")
                .texture("particle", modLoc("block/sink_fan"))
                .texture("fan", modLoc("block/sink_fan"));
        fan.element().from(7, 7, 1.25F).to(9, 9, 2.5F).textureAll("#fan").end();
        blade(fan, 7.25F, 9, 8.75F, 12.5F, Direction.Axis.Y, 22.5F);
        blade(fan, 7.25F, 3.5F, 8.75F, 7, Direction.Axis.Y, -22.5F);
        blade(fan, 9, 7.25F, 12.5F, 8.75F, Direction.Axis.X, -22.5F);
        blade(fan, 3.5F, 7.25F, 7, 8.75F, Direction.Axis.X, 22.5F);
    }

    private static void blade(BlockModelBuilder fan, float x0, float y0, float x1, float y1, Direction.Axis axis, float angle) {
        fan.element().from(x0, y0, 1.6F).to(x1, y1, 2.1F)
                .textureAll("#fan")
                .rotation().origin((x0 + x1) / 2, (y0 + y1) / 2, 1.85F).axis(axis).angle(angle).end()
                .end();
    }

    private void cubeAllWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }

    /**
     * The compact pump as a multipart: the body per facing and lit state, plus a
     * pipe stub per connected absolute direction (unrotated, the stubs are symmetric).
     */
    private void pump(Block block, String name) {
        ModelFile unlit = pumpModel(name, modLoc("block/" + name + "_front"));
        ModelFile lit = pumpModel(name + "_lit", modLoc("block/" + name + "_front_lit"));
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean isLit : new boolean[] {false, true}) {
                builder.part().modelFile(isLit ? lit : unlit).rotationY(((int) facing.toYRot() + 180) % 360).addModel()
                        .condition(MachineBlock.FACING, facing).condition(MachineBlock.LIT, isLit).end();
            }
        }
        for (Map.Entry<Direction, IntegerProperty> stub : PumpBlock.STUBS.entrySet()) {
            for (int size = 1; size <= PumpBlock.MAX_STUB; size++) {
                builder.part().modelFile(pumpStub(stub.getKey(), size)).addModel().condition(stub.getValue(), size).end();
            }
        }
        simpleBlockItem(block, unlit);
    }

    /** A short pipe from the pump body to the block edge, as thick as a duct arm with {@code size} lines; shared by both pumps. */
    private ModelFile pumpStub(Direction direction, int size) {
        BlockModelBuilder model = models().withExistingParent("pump_stub_" + direction.getSerializedName() + "_" + size, mcLoc("block/block"))
                .texture("particle", modLoc("block/machine_side"))
                .texture("side", modLoc("block/machine_side"))
                .texture("top", modLoc("block/machine_top"));
        int[] box = PumpBlock.stubBox(direction, size);
        int lo = 8 - PumpBlock.stubRadius(size);
        int hi = 8 + PumpBlock.stubRadius(size);
        model.element().from(box[0], box[1], box[2]).to(box[3], box[4], box[5])
                .allFaces((face, builder) -> builder.texture(face == direction || face == direction.getOpposite() ? "#top" : "#side")
                        .uvs(lo, lo, hi, hi))
                .face(direction).cullface(direction).end();
        return model;
    }

    private ModelFile pumpModel(String name, ResourceLocation front) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .texture("particle", modLoc("block/machine_side"))
                .texture("side", modLoc("block/machine_side"))
                .texture("top", modLoc("block/machine_top"))
                .texture("front", front);
        model.element().from(1, 0, 1).to(15, 2, 15)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(1, 14, 15, 16))
                .face(Direction.UP).uvs(1, 1, 15, 15).end()
                .face(Direction.DOWN).uvs(1, 1, 15, 15).cullface(Direction.DOWN).end();
        model.element().from(3, 2, 3).to(13, 12, 13)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(3, 4, 13, 14))
                .face(Direction.NORTH).texture("#front").uvs(0, 0, 16, 16).end()
                .face(Direction.UP).uvs(3, 3, 13, 13).end();
        model.element().from(6, 12, 6).to(10, 15, 10)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(6, 1, 10, 4))
                .face(Direction.UP).uvs(6, 6, 10, 10).end()
                .face(Direction.DOWN).uvs(6, 6, 10, 10).end();
        return model;
    }

    /** Orientable machine with a lit and an unlit front, rotated by facing. */
    private void machine(Block block, String name) {
        ResourceLocation side = modLoc("block/machine_side");
        ResourceLocation top = modLoc("block/machine_top");
        ModelFile unlit = models().orientable(name, side, modLoc("block/" + name + "_front"), top);
        ModelFile lit = models().orientable(name + "_lit", side, modLoc("block/" + name + "_front_lit"), top);
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(MachineBlock.FACING);
            boolean isLit = state.getValue(MachineBlock.LIT);
            return ConfiguredModel.builder()
                    .modelFile(isLit ? lit : unlit)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(block, unlit);
    }
}
