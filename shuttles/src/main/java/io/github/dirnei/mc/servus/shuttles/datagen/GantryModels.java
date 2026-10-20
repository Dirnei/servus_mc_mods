package io.github.dirnei.mc.servus.shuttles.datagen;

import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.CAP_BOTTOM;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.GLASS_INSET;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.NOZZLE_BOTTOM;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.NOZZLE_HEIGHT;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.NOZZLE_MAX;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.NOZZLE_MIN;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PIPE_MAX;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PIPE_MIN;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PIPE_TOP;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_IN;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_OUT;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_X0;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_X1;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_Y0;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_Y1;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLINTH_TOP;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlock;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryPart;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;

/**
 * Datagen for the Fluid Gantry, modelled for a rail running north to south
 * and turned a quarter for one running east to west. A leg block is the same
 * on both sides of the rail: the lower one has the steel plinth, the upper one
 * the steel cap and a lock plate on both faces along the rail; between them
 * blackened iron corner posts, an iron seam band where the blocks meet and
 * glass set in from the outer faces. The beam is a steel I-beam with an iron
 * web, ending just short of the legs, with copper feed pipes, a steel spout
 * sleeve and a sign on both faces showing the mode and the two lamps. The
 * renderer draws the spout's iron inner pipe and copper nozzle, which lower
 * onto a tanker, the fluid and the locked fluid on the plates; the item
 * model shows the spout at rest.
 */
final class GantryModels {

    private static final Identifier STEEL = Identifier.fromNamespaceAndPath("servus_foundation", "block/electric_pump_steel");
    private static final Identifier GLASS = Identifier.fromNamespaceAndPath("servus_foundation", "block/fluid_tank_glass");
    private static final Identifier COPPER = Identifier.withDefaultNamespace("block/copper_block");
    /** Beam parts end this far short of the legs. */
    private static final float BEAM_END = 0.05F;
    private static final float BAND_INSET = 0.15F;
    private static final float BAND_HALF = 0.5F;
    private static final Set<Direction> SIDES = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    private static final String[] LAMPS = {"off", "idle", "working", "blocked"};

    private GantryModels() {
    }

    /**
     * One box of a model in pixels with its texture key and the faces it has.
     * A box with a {@code front} texture shows it on the face that looks out
     * along the rail, with v from 0 to {@code frontV}.
     */
    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, String texture, Set<Direction> faces,
                       String front, float frontV) {

        Box(float x0, float y0, float z0, float x1, float y1, float z1, String texture, Set<Direction> faces) {
            this(x0, y0, z0, x1, y1, z1, texture, faces, null, 0);
        }

        Box(float x0, float y0, float z0, float x1, float y1, float z1, String texture) {
            this(x0, y0, z0, x1, y1, z1, texture, EnumSet.allOf(Direction.class));
        }

        Box(float x0, float y0, float z0, float x1, float y1, float z1, String front, float frontV) {
            this(x0, y0, z0, x1, y1, z1, "#steel", EnumSet.allOf(Direction.class), front, frontV);
        }

        Box shifted(float dx, float dy) {
            return new Box(x0 + dx, y0 + dy, z0, x1 + dx, y1 + dy, z1, texture, faces, front, frontV);
        }

        /** The face that looks out along the rail: south for a box in the south half, north otherwise. */
        Direction outward() {
            return z0 > 8 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    static void gantry(BlockStateProvider provider, Block block, String name) {
        Identifier iron = provider.modLoc("block/heavy_rack_iron");
        Identifier plate = provider.modLoc("block/" + name + "_plate");
        BlockModelBuilder legLow = model(provider, name + "_leg_low", iron, plate, legLow());
        BlockModelBuilder legHigh = model(provider, name + "_leg_high", iron, plate, legHigh());
        BlockModelBuilder beam = model(provider, name + "_beam", iron, plate, beam());

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.Z, Direction.Axis.X}) {
            int rotation = axis == Direction.Axis.X ? 90 : 0;
            builder.part().modelFile(legLow).rotationY(rotation).addModel()
                    .condition(FluidGantryBlock.PART, GantryPart.LEFT_LOW, GantryPart.RIGHT_LOW).condition(FluidGantryBlock.AXIS, axis);
            builder.part().modelFile(legHigh).rotationY(rotation).addModel()
                    .condition(FluidGantryBlock.PART, GantryPart.LEFT_HIGH, GantryPart.RIGHT_HIGH).condition(FluidGantryBlock.AXIS, axis);
            builder.part().modelFile(beam).rotationY(rotation).addModel()
                    .condition(FluidGantryBlock.PART, GantryPart.BEAM).condition(FluidGantryBlock.AXIS, axis);
            for (GantryMode mode : GantryMode.values()) {
                for (String lamp : LAMPS) {
                    BlockModelBuilder sign = signModel(provider, name, mode, lamp, iron, plate);
                    MultiPartBlockStateBuilder.PartBuilder part = builder.part().modelFile(sign).rotationY(rotation).addModel()
                            .condition(FluidGantryBlock.PART, GantryPart.BEAM).condition(FluidGantryBlock.AXIS, axis)
                            .condition(FluidGantryBlock.MODE, mode).condition(StorageNodeBlocks.ONLINE, !lamp.equals("off"));
                    if (!lamp.equals("off")) {
                        part.condition(StorageNodeBlocks.ACTIVITY, StorageNodeBlocks.Activity.valueOf(lamp.toUpperCase(Locale.ROOT)));
                    }
                }
            }
        }
        item(provider, name, iron, plate);
    }

    /** Plinth, posts, glass and the lower half of the seam band. */
    private static List<Box> legLow() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(0, 0, 0, 16, PLINTH_TOP, 16, "#steel"));
        posts(boxes, PLINTH_TOP, 16);
        boxes.add(new Box(GLASS_INSET, PLINTH_TOP, GLASS_INSET, 16 - GLASS_INSET, 16, 16 - GLASS_INSET, "#glass", SIDES));
        boxes.add(new Box(BAND_INSET, 16 - BAND_HALF, BAND_INSET, 16 - BAND_INSET, 16, 16 - BAND_INSET, "#iron",
                EnumSet.complementOf(EnumSet.of(Direction.UP))));
        return boxes;
    }

    /** The upper half of the seam band, posts, glass, the cap and the two lock plates. */
    private static List<Box> legHigh() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(BAND_INSET, 0, BAND_INSET, 16 - BAND_INSET, BAND_HALF, 16 - BAND_INSET, "#iron",
                EnumSet.complementOf(EnumSet.of(Direction.DOWN))));
        posts(boxes, 0, CAP_BOTTOM);
        boxes.add(new Box(GLASS_INSET, 0, GLASS_INSET, 16 - GLASS_INSET, CAP_BOTTOM, 16 - GLASS_INSET, "#glass", SIDES));
        boxes.add(new Box(0, CAP_BOTTOM, 0, 16, 16, 16, "#steel"));
        boxes.add(new Box(PLATE_X0, PLATE_Y0, 16 - PLATE_IN, PLATE_X1, PLATE_Y1, 16 + PLATE_OUT, "#plate", 16));
        boxes.add(new Box(PLATE_X0, PLATE_Y0, -PLATE_OUT, PLATE_X1, PLATE_Y1, PLATE_IN, "#plate", 16));
        return boxes;
    }

    /** Blackened iron posts, 2 by 2, in the four corners. */
    private static void posts(List<Box> boxes, float y0, float y1) {
        for (float x : new float[] {0, 14}) {
            for (float z : new float[] {0, 14}) {
                boxes.add(new Box(x, y0, z, x + 2, y1, z + 2, "#iron"));
            }
        }
    }

    /** The I-beam across the rail, the feed pipes from the legs and the spout sleeve. */
    private static List<Box> beam() {
        float x0 = BEAM_END;
        float x1 = 16 - BEAM_END;
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(x0, 9, 2, x1, 10.5F, 14, "#steel"));
        boxes.add(new Box(x0, 10.5F, 6.8F, x1, 14, 9.2F, "#iron"));
        boxes.add(new Box(x0, 14, 2, x1, 16, 14, "#steel"));
        boxes.add(new Box(x0, 7, 7, 6, 9, 9, "#copper"));
        boxes.add(new Box(10, 7, 7, x1, 9, 9, "#copper"));
        boxes.add(new Box(6, 4, 6, 10, 9, 10, "#steel"));
        return boxes;
    }

    /** The spout's inner pipe and nozzle at rest, for the item; the renderer draws them on the block. */
    private static List<Box> spout() {
        float nozzleTop = NOZZLE_BOTTOM + NOZZLE_HEIGHT;
        return List.of(new Box(PIPE_MIN, nozzleTop, PIPE_MIN, PIPE_MAX, PIPE_TOP, PIPE_MAX, "#iron"),
                new Box(NOZZLE_MIN, NOZZLE_BOTTOM, NOZZLE_MIN, NOZZLE_MAX, nozzleTop, NOZZLE_MAX, "#copper"));
    }

    /** The sign plates on both faces of the beam, between its flanges. */
    private static List<Box> signs() {
        return List.of(new Box(3.5F, 10.65F, 13.15F, 12.5F, 13.85F, 13.65F, "#sign", 6),
                new Box(3.5F, 10.65F, 2.35F, 12.5F, 13.85F, 2.85F, "#sign", 6));
    }

    private static BlockModelBuilder signModel(BlockStateProvider provider, String name, GantryMode mode, String lamp,
                                               Identifier iron, Identifier plate) {
        String model = name + "_sign_" + mode.getSerializedName() + "_" + lamp;
        BlockModelBuilder builder = base(provider, model, iron, plate).texture("sign", provider.modLoc("block/" + model));
        for (Box box : signs()) {
            element(builder, box);
        }
        return builder;
    }

    private static BlockModelBuilder base(BlockStateProvider provider, String model, Identifier iron, Identifier plate) {
        return provider.models().withExistingParent(model, "minecraft:block/block")
                .texture("particle", STEEL).texture("steel", STEEL).texture("iron", iron).texture("glass", GLASS)
                .texture("copper", COPPER).texture("plate", plate);
    }

    private static BlockModelBuilder model(BlockStateProvider provider, String model, Identifier iron, Identifier plate, List<Box> boxes) {
        BlockModelBuilder builder = base(provider, model, iron, plate);
        for (Box box : boxes) {
            element(builder, box);
        }
        return builder;
    }

    /** One element; a steel face on the bottom of the block is culled by the block below. */
    private static void element(BlockModelBuilder builder, Box box) {
        ModelBuilder<BlockModelBuilder>.ElementBuilder element = builder.element()
                .from(box.x0(), box.y0(), box.z0()).to(box.x1(), box.y1(), box.z1());
        for (Direction direction : box.faces()) {
            ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder face = element.face(direction).texture(box.texture());
            if (box.front() != null && direction == box.outward()) {
                face.texture(box.front()).uvs(0, 0, 16, box.frontV());
            }
            if (direction == Direction.DOWN && box.y0() == 0 && box.texture().equals("#steel")) {
                face.cullface(Direction.DOWN);
            }
        }
    }

    /** The whole gantry for the item: the legs beside the beam, all scaled down in the hand and the GUI. */
    private static void item(BlockStateProvider provider, String name, Identifier iron, Identifier plate) {
        BlockModelBuilder whole = base(provider, name + "_item", iron, plate)
                .texture("sign", provider.modLoc("block/" + name + "_sign_storage_off"));
        List<Box> boxes = new ArrayList<>();
        for (float dx : new float[] {-16, 16}) {
            legLow().forEach(box -> boxes.add(box.shifted(dx, 0)));
            legHigh().forEach(box -> boxes.add(box.shifted(dx, 16)));
        }
        beam().forEach(box -> boxes.add(box.shifted(0, 16)));
        spout().forEach(box -> boxes.add(box.shifted(0, 16)));
        signs().forEach(box -> boxes.add(box.shifted(0, 16)));
        for (Box box : boxes) {
            element(whole, box);
        }
        provider.itemModels().withExistingParent(name, provider.modLoc("block/" + name + "_item"))
                .transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, -1, 0).scale(0.3F).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 2, 0).scale(0.2F).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 0, 0).translation(0, -1, 0).scale(0.3F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.2F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(0, 0, 0).scale(0.25F).end()
                .end();
    }
}
