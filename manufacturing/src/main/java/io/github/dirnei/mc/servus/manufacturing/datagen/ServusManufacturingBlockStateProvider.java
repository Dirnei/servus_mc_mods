package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.ChimneyBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkLayout;
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
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
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
        machine(ServusManufacturingBlocks.ASSEMBLER.get(), "assembler");
        machine(ServusManufacturingBlocks.RESEARCH_LAB.get(), "research_lab");
        machine(ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get(), "electric_extractor");
        machine(ServusManufacturingBlocks.CRUSHER.get(), "crusher");
        machine(ServusManufacturingBlocks.CHUNK_LOADER.get(), "chunk_loader");

        sink();
        chimney();
        sinkFan();
    }

    /**
     * The Sink: an unformed casing (bricks in an iron frame) and, once formed, one
     * model per part in the north facing frame, turned by the facing. Parts show
     * bricks on the outer faces of the cube only. The four front parts share one
     * recess three pixels deep behind a quartered grate, framed by brick bars on
     * the outer edges of the 2x2 front; the fan is drawn by the block entity renderer.
     */
    private void sink() {
        ModelFile casing = models().cubeAll("sink_casing", modLoc("block/sink_casing"));
        ModelFile[] parts = new ModelFile[SinkLayout.PARTS];
        for (int part = 0; part < SinkLayout.PARTS; part++) {
            parts[part] = sinkPart(part);
        }
        getVariantBuilder(ServusManufacturingBlocks.SINK.get()).forAllStates(state -> {
            if (!state.getValue(SinkBlock.FORMED)) {
                return ConfiguredModel.builder().modelFile(casing).build();
            }
            Direction facing = state.getValue(SinkBlock.FACING);
            return ConfiguredModel.builder()
                    .modelFile(parts[state.getValue(SinkBlock.PART)])
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .uvLock(true)
                    .build();
        });
        simpleBlockItem(ServusManufacturingBlocks.SINK.get(), casing);
    }

    /**
     * One part of the formed Sink. Facing north the front is north, right is west
     * (a player looking at the front), back is south.
     */
    private ModelFile sinkPart(int part) {
        ResourceLocation bricks = mcLoc("block/bricks");
        BlockModelBuilder model = models().getBuilder("sink_part_" + part)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .renderType("cutout")
                .texture("particle", bricks)
                .texture("bricks", bricks);
        boolean right = (part & SinkLayout.RIGHT) != 0;
        boolean up = (part & SinkLayout.UP) != 0;
        boolean front = SinkLayout.isFront(part);
        Direction side = right ? Direction.WEST : Direction.EAST;
        Direction vertical = up ? Direction.UP : Direction.DOWN;
        Set<Direction> outer = EnumSet.of(side, vertical, front ? Direction.NORTH : Direction.SOUTH);
        if (!front) {
            model.element().from(0, 0, 0).to(16, 16, 16)
                    .allFacesExcept((direction, face) -> face.texture("#bricks").cullface(direction), EnumSet.complementOf(EnumSet.copyOf(outer)))
                    .end();
            return model;
        }
        model.texture("recess", modLoc("block/sink_recess"))
                .texture("grate", modLoc("block/sink_front_" + part));
        // Body behind the recess: outer sides in bricks, the recess wall facing the front.
        model.element().from(0, 0, 3).to(16, 16, 16)
                .allFacesExcept((direction, face) -> {
                    if (direction == Direction.NORTH) {
                        face.texture("#recess");
                    } else {
                        face.texture("#bricks").cullface(direction);
                    }
                }, EnumSet.complementOf(EnumSet.of(Direction.NORTH, side, vertical)))
                .end();
        // Frame bars on the outer edges of the 2x2 front: one upright on the side, one across at the top or bottom.
        float sideX0 = right ? 0 : 13;
        float sideX1 = right ? 3 : 16;
        model.element().from(sideX0, 0, 0).to(sideX1, 16, 3)
                .face(Direction.NORTH).texture("#bricks").cullface(Direction.NORTH).end()
                .face(side).texture("#bricks").cullface(side).end()
                .face(side.getOpposite()).texture("#bricks").end()
                .face(vertical).texture("#bricks").cullface(vertical).end()
                .end();
        float acrossX0 = right ? 3 : 0;
        float acrossX1 = right ? 16 : 13;
        float acrossY0 = up ? 13 : 0;
        float acrossY1 = up ? 16 : 3;
        model.element().from(acrossX0, acrossY0, 0).to(acrossX1, acrossY1, 3)
                .face(Direction.NORTH).texture("#bricks").cullface(Direction.NORTH).end()
                .face(vertical).texture("#bricks").cullface(vertical).end()
                .face(vertical.getOpposite()).texture("#bricks").end()
                .end();
        // The grate, a plane just behind the front over this quarter of the opening.
        float gx0 = right ? 3 : 0;
        float gx1 = right ? 16 : 13;
        float gy0 = up ? 0 : 3;
        float gy1 = up ? 13 : 16;
        model.element().from(gx0, gy0, 0.5F).to(gx1, gy1, 0.5F)
                .face(Direction.NORTH).texture("#grate").uvs(16 - gx1, 16 - gy1, 16 - gx0, 16 - gy0).end()
                .face(Direction.SOUTH).texture("#grate").uvs(gx0, 16 - gy1, gx1, 16 - gy0).end()
                .end();
        return model;
    }

    /** Full brick block inside a column; the top block carries a wider cap with an open flue. */
    private void chimney() {
        ResourceLocation bricks = mcLoc("block/bricks");
        // A slim shaft, ten pixels wide; the top block ends in a cap twelve pixels wide with an open flue.
        BlockModelBuilder column = models().getBuilder("chimney")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", bricks)
                .texture("bricks", bricks);
        column.element().from(3, 0, 3).to(13, 16, 13)
                .face(Direction.NORTH).texture("#bricks").uvs(3, 0, 13, 16).end()
                .face(Direction.SOUTH).texture("#bricks").uvs(3, 0, 13, 16).end()
                .face(Direction.EAST).texture("#bricks").uvs(3, 0, 13, 16).end()
                .face(Direction.WEST).texture("#bricks").uvs(3, 0, 13, 16).end()
                .face(Direction.UP).texture("#bricks").uvs(3, 3, 13, 13).cullface(Direction.UP).end()
                .face(Direction.DOWN).texture("#bricks").uvs(3, 3, 13, 13).cullface(Direction.DOWN).end()
                .end();
        BlockModelBuilder top = models().getBuilder("chimney_top")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", bricks)
                .texture("bricks", bricks)
                .texture("top", modLoc("block/chimney_top"));
        top.element().from(3, 0, 3).to(13, 13, 13)
                .face(Direction.NORTH).texture("#bricks").uvs(3, 3, 13, 16).end()
                .face(Direction.SOUTH).texture("#bricks").uvs(3, 3, 13, 16).end()
                .face(Direction.EAST).texture("#bricks").uvs(3, 3, 13, 16).end()
                .face(Direction.WEST).texture("#bricks").uvs(3, 3, 13, 16).end()
                .face(Direction.DOWN).texture("#bricks").uvs(3, 3, 13, 13).cullface(Direction.DOWN).end()
                .end();
        top.element().from(2, 13, 2).to(14, 16, 14)
                .face(Direction.UP).texture("#top").uvs(0, 0, 16, 16).cullface(Direction.UP).end()
                .face(Direction.DOWN).texture("#bricks").uvs(2, 2, 14, 14).end()
                .face(Direction.NORTH).texture("#bricks").uvs(2, 0, 14, 3).end()
                .face(Direction.SOUTH).texture("#bricks").uvs(2, 0, 14, 3).end()
                .face(Direction.EAST).texture("#bricks").uvs(2, 0, 14, 3).end()
                .face(Direction.WEST).texture("#bricks").uvs(2, 0, 14, 3).end()
                .end();
        getVariantBuilder(ServusManufacturingBlocks.CHIMNEY.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ChimneyBlock.TOP) ? top : column)
                .build());
        simpleBlockItem(ServusManufacturingBlocks.CHIMNEY.get(), top);
    }

    /**
     * The big Sink fan for the block entity renderer, sized for the 2x2 front: a
     * hub, four pitched main blades and four flat blades between them, centred on
     * (8, 8) in the model's north facing frame. Tips reach 12.5 pixels from the
     * centre, inside the 26 pixel opening. Every face has explicit UVs because the
     * elements reach outside the block.
     */
    private void sinkFan() {
        BlockModelBuilder fan = models().getBuilder("sink_fan")
                .texture("particle", modLoc("block/sink_fan"))
                .texture("fan", modLoc("block/sink_fan"));
        fan.element().from(5, 5, 1.0F).to(11, 11, 2.8F)
                .allFaces((direction, face) -> face.texture("#fan").uvs(8, 8, 16, 9))
                .face(Direction.NORTH).uvs(8, 0, 15, 7).end()
                .face(Direction.SOUTH).uvs(8, 0, 15, 7).end()
                .end();
        // Main blades, pitched about their long axis like a propeller.
        fanBlade(fan, 5.5F, 10, 10.5F, 20.5F, 1.6F, 2.1F, Direction.Axis.Y, 22.5F);
        fanBlade(fan, 5.5F, -4.5F, 10.5F, 6, 1.6F, 2.1F, Direction.Axis.Y, -22.5F);
        fanBlade(fan, 10, 5.5F, 20.5F, 10.5F, 1.6F, 2.1F, Direction.Axis.X, -22.5F);
        fanBlade(fan, -4.5F, 5.5F, 6, 10.5F, 1.6F, 2.1F, Direction.Axis.X, 22.5F);
        // Flat blades on the diagonals: an upright blade turned 45 degrees about the centre.
        for (float angle : new float[] {45, -45}) {
            fanDiagonal(fan, 6.75F, 10, 9.25F, 18.5F, angle);
            fanDiagonal(fan, 6.75F, -2.5F, 9.25F, 6, angle);
        }
    }

    private static void fanBlade(BlockModelBuilder fan, float x0, float y0, float x1, float y1, float z0, float z1,
                                 Direction.Axis axis, float angle) {
        boolean upright = axis == Direction.Axis.Y;
        fan.element().from(x0, y0, z0).to(x1, y1, z1)
                .allFaces((direction, face) -> face.texture("#fan").uvs(8, 8, 16, 9))
                .face(Direction.NORTH).uvs(0, 0, 8, 16).rotation(upright ? ModelBuilder.FaceRotation.ZERO
                        : ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .face(Direction.SOUTH).uvs(0, 0, 8, 16).rotation(upright ? ModelBuilder.FaceRotation.ZERO
                        : ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .rotation().origin((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2).axis(axis).angle(angle).end()
                .end();
    }

    private static void fanDiagonal(BlockModelBuilder fan, float x0, float y0, float x1, float y1, float angle) {
        fan.element().from(x0, y0, 1.25F).to(x1, y1, 1.55F)
                .allFaces((direction, face) -> face.texture("#fan").uvs(8, 8, 16, 9))
                .face(Direction.NORTH).uvs(1, 0, 6, 16).end()
                .face(Direction.SOUTH).uvs(1, 0, 6, 16).end()
                .rotation().origin(8, 8, 1.4F).axis(Direction.Axis.Z).angle(angle).end()
                .end();
    }

    private void cubeAllWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }

    /** Orientable machine with a lit and an unlit front, rotated by facing. */
    private void machine(Block block, String name) {
        ResourceLocation side = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "block/machine_side");
        ResourceLocation top = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "block/machine_top");
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
