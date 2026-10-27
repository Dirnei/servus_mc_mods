package io.github.dirnei.mc.servus.shuttles.datagen;

import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.ACTIVITY_LAMP_X;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.BAND_BOTTOM;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.CONNECTION_LAMP_X;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FIN;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.GLASS_INSET;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.GLASS_Z0;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.GLASS_Z1;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.HEADER_BOTTOM;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.LAMP_WIDTH;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.LAMP_Y0;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.LAMP_Y1;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.LENS_Z;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.LOWER_FRONT;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.PORT_DEPTH;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.PORT_HALF;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.PORT_HALF_WIDTH;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.POST;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.SILL_TOP;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.SLOT_DEPTH;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.SLOT_X0;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.SLOT_X1;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.SOCKET_RIM;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.SOCKET_Z;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Datagen for the fluid stations, built facing north with the front at z 0.
 * The lower half is a steel body set back {@link FluidStationLook#LOWER_FRONT}
 * behind two corner posts and a plinth, with the copper port flange at a
 * tanker coupling's height reaching into the body, and two lamps in iron
 * sockets. The upper half is a steel body behind {@link FluidStationLook#SLOT_DEPTH}
 * deep window slots, with corner posts and a band in the station's colour
 * at the top. The window row for each tank count (sill, header, steel fins
 * and a glass pane per window) is a standalone model the renderer draws.
 *
 * <p>Faces where two parts meet are left out on both sides, so no two faces
 * share a plane where they overlap: the tops of the lower parts and the
 * bottoms of the upper body, band and posts; the backs of the posts,
 * plinth, sockets, lenses, sill, header and fins against the bodies; the
 * inner sides of the posts, plinth, band, sill, header and outer fins
 * against each other; the band box's front and bottom. The port flange
 * passes through the lower body's front, the lenses stand 0.2 in front of
 * their sockets, the glass stands {@link FluidStationLook#GLASS_INSET} off
 * the fins, sill and header, and the fluid 0.1 off every wall and behind the glass.
 */
final class FluidStationModels {

    private static final Identifier STEEL = Identifier.fromNamespaceAndPath("servus_foundation", "block/electric_pump_steel");
    private static final Identifier GLASS = Identifier.fromNamespaceAndPath("servus_foundation", "block/fluid_tank_glass");
    private static final Identifier COPPER = Identifier.withDefaultNamespace("block/copper_block");
    private static final Identifier BAND = Identifier.withDefaultNamespace("block/cyan_concrete");
    private static final Identifier LAMP_DARK = Identifier.withDefaultNamespace("block/gray_concrete");
    private static final Identifier LAMP_CONNECTED = Identifier.withDefaultNamespace("block/light_blue_concrete");
    private static final Identifier LAMP_IDLE = Identifier.withDefaultNamespace("block/lime_concrete");
    private static final Identifier LAMP_WORKING = Identifier.withDefaultNamespace("block/orange_concrete");
    private static final Identifier LAMP_BLOCKED = Identifier.withDefaultNamespace("block/red_concrete");
    private static final Set<Direction> FRONT_SIDES_TOP_BOTTOM = EnumSet.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP,
            Direction.DOWN);

    private FluidStationModels() {
    }

    /** One box in pixels, the faces it has and the texture key of each; {@code light} above 0 draws it bright. */
    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, Set<Direction> faces, String texture, String top,
                       int light) {

        Box(float x0, float y0, float z0, float x1, float y1, float z1, Set<Direction> faces, String texture) {
            this(x0, y0, z0, x1, y1, z1, faces, texture, texture, 0);
        }

        String texture(Direction face) {
            return face == Direction.UP ? top : texture;
        }
    }

    static void station(BlockStateProvider provider, Block block, String name) {
        Identifier iron = provider.modLoc("block/heavy_rack_iron");
        BlockModelBuilder lower = model(provider, name + "_lower", iron, lower());
        BlockModelBuilder upper = model(provider, name + "_upper", iron, upper());
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        List<BlockModelBuilder> lamps = new ArrayList<>();
        for (String lamp : new String[] {"off", "idle", "working", "blocked"}) {
            lamps.add(model(provider, name + "_lamps_" + lamp, iron, lamps(lamp)));
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int angle = ModelRotations.rotationY(facing);
            builder.part().modelFile(lower).rotationY(angle).addModel()
                    .condition(FluidStationBlock.FACING, facing).condition(FluidStationBlock.HALF, DoubleBlockHalf.LOWER);
            builder.part().modelFile(upper).rotationY(angle).addModel()
                    .condition(FluidStationBlock.FACING, facing).condition(FluidStationBlock.HALF, DoubleBlockHalf.UPPER);
            builder.part().modelFile(lamps.get(0)).rotationY(angle).addModel()
                    .condition(FluidStationBlock.FACING, facing).condition(FluidStationBlock.HALF, DoubleBlockHalf.LOWER)
                    .condition(StorageNodeBlocks.ONLINE, false);
            for (StorageNodeBlocks.Activity activity : StorageNodeBlocks.Activity.values()) {
                builder.part().modelFile(lamps.get(1 + activity.ordinal())).rotationY(angle).addModel()
                        .condition(FluidStationBlock.FACING, facing).condition(FluidStationBlock.HALF, DoubleBlockHalf.LOWER)
                        .condition(StorageNodeBlocks.ONLINE, true).condition(StorageNodeBlocks.ACTIVITY, activity);
            }
        }
        for (int tanks = 1; tanks <= ShuttlesStatsManager.MAX_FLUID_STATION_TANKS; tanks++) {
            model(provider, FluidStationLook.windowModel(tanks), iron, windows(tanks));
        }
        item(provider, name, iron);
    }

    /** Body, corner posts, plinth, port flange and lamp sockets. */
    private static List<Box> lower() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(0, 0, LOWER_FRONT, 16, 16, 16, EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
                Direction.DOWN), "#steel"));
        boxes.add(new Box(0, 0, 0, POST, 16, LOWER_FRONT, EnumSet.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.DOWN), "#steel"));
        boxes.add(new Box(16 - POST, 0, 0, 16, 16, LOWER_FRONT, EnumSet.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.DOWN),
                "#steel"));
        boxes.add(new Box(POST, 0, 0, 16 - POST, 1, LOWER_FRONT, EnumSet.of(Direction.NORTH, Direction.UP, Direction.DOWN), "#steel"));
        float portY = FluidStationLook.portY();
        boxes.add(new Box(8 - PORT_HALF_WIDTH, portY - PORT_HALF, 0, 8 + PORT_HALF_WIDTH, portY + PORT_HALF, PORT_DEPTH,
                FRONT_SIDES_TOP_BOTTOM, "#copper"));
        for (float x : new float[] {CONNECTION_LAMP_X, ACTIVITY_LAMP_X}) {
            boxes.add(new Box(x - SOCKET_RIM, LAMP_Y0 - SOCKET_RIM, SOCKET_Z, x + LAMP_WIDTH + SOCKET_RIM, LAMP_Y1 + SOCKET_RIM, LOWER_FRONT,
                    FRONT_SIDES_TOP_BOTTOM, "#iron"));
        }
        return boxes;
    }

    /** The two lenses; dark while offline, else the connection lamp lit and the activity lamp in its colour. */
    private static List<Box> lamps(String lamp) {
        boolean online = !lamp.equals("off");
        int light = online ? 15 : 0;
        return List.of(
                new Box(CONNECTION_LAMP_X, LAMP_Y0, LENS_Z, CONNECTION_LAMP_X + LAMP_WIDTH, LAMP_Y1, SOCKET_Z, FRONT_SIDES_TOP_BOTTOM,
                        online ? "#connected" : "#dark", online ? "#connected" : "#dark", light),
                new Box(ACTIVITY_LAMP_X, LAMP_Y0, LENS_Z, ACTIVITY_LAMP_X + LAMP_WIDTH, LAMP_Y1, SOCKET_Z, FRONT_SIDES_TOP_BOTTOM,
                        "#" + lamp, "#" + lamp, light));
    }

    /** Body behind the window slots, the band box on it, the front band and the corner posts. */
    private static List<Box> upper() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(0, 0, SLOT_DEPTH, 16, BAND_BOTTOM, 16, EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST),
                "#steel"));
        boxes.add(new Box(0, BAND_BOTTOM, SLOT_DEPTH, 16, 16, 16, EnumSet.of(Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP),
                "#band", "#steel", 0));
        boxes.add(new Box(POST, BAND_BOTTOM, 0, 16 - POST, 16, SLOT_DEPTH, EnumSet.of(Direction.NORTH, Direction.UP), "#band", "#steel", 0));
        boxes.add(new Box(0, 0, 0, POST, 16, SLOT_DEPTH, EnumSet.of(Direction.NORTH, Direction.WEST, Direction.UP), "#steel"));
        boxes.add(new Box(16 - POST, 0, 0, 16, 16, SLOT_DEPTH, EnumSet.of(Direction.NORTH, Direction.EAST, Direction.UP), "#steel"));
        return boxes;
    }

    /** Sill, header, a steel fin beside every window and a glass pane in each, for that many tanks. */
    private static List<Box> windows(int tanks) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(SLOT_X0, 0, 0, SLOT_X1, SILL_TOP, SLOT_DEPTH, EnumSet.of(Direction.NORTH, Direction.UP, Direction.DOWN), "#steel"));
        boxes.add(new Box(SLOT_X0, HEADER_BOTTOM, 0, SLOT_X1, BAND_BOTTOM, SLOT_DEPTH, EnumSet.of(Direction.NORTH, Direction.DOWN), "#steel"));
        for (int fin = 0; fin <= tanks; fin++) {
            float x0 = fin == tanks ? SLOT_X1 - FIN : FluidStationLook.windowX0(tanks, fin) - FIN;
            Set<Direction> faces = EnumSet.of(Direction.NORTH);
            if (fin > 0) {
                faces.add(Direction.WEST);
            }
            if (fin < tanks) {
                faces.add(Direction.EAST);
            }
            boxes.add(new Box(x0, SILL_TOP, 0, x0 + FIN, HEADER_BOTTOM, SLOT_DEPTH, faces, "#steel"));
        }
        for (int window = 0; window < tanks; window++) {
            boxes.add(new Box(FluidStationLook.windowX0(tanks, window) + GLASS_INSET, SILL_TOP + GLASS_INSET, GLASS_Z0,
                    FluidStationLook.windowX1(tanks, window) - GLASS_INSET, HEADER_BOTTOM - GLASS_INSET, GLASS_Z1,
                    EnumSet.of(Direction.NORTH, Direction.SOUTH), "#glass"));
        }
        return boxes;
    }

    private static BlockModelBuilder base(BlockStateProvider provider, String model, Identifier iron) {
        return provider.models().withExistingParent(model, "minecraft:block/block")
                .texture("particle", STEEL).texture("steel", STEEL).texture("iron", iron).texture("glass", GLASS)
                .texture("copper", COPPER).texture("band", BAND).texture("dark", LAMP_DARK).texture("connected", LAMP_CONNECTED)
                .texture("idle", LAMP_IDLE).texture("working", LAMP_WORKING).texture("blocked", LAMP_BLOCKED);
    }

    private static BlockModelBuilder model(BlockStateProvider provider, String model, Identifier iron, List<Box> boxes) {
        BlockModelBuilder builder = base(provider, model, iron);
        boxes.forEach(box -> element(builder, box, 0));
        return builder;
    }

    /** One element moved up by {@code dy}; explicit UVs keep a moved element's texture where it is in its own block. */
    private static void element(BlockModelBuilder builder, Box box, float dy) {
        ModelBuilder<BlockModelBuilder>.ElementBuilder element = builder.element()
                .from(box.x0(), box.y0() + dy, box.z0()).to(box.x1(), box.y1() + dy, box.z1());
        for (Direction direction : box.faces()) {
            ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder face = element.face(direction).texture(box.texture(direction));
            float[] uv = uv(box, direction);
            face.uvs(uv[0], uv[1], uv[2], uv[3]);
        }
        if (box.light() > 0) {
            element.lightEmission(box.light());
        }
    }

    /** The UVs the game derives for a face of a box inside one block. */
    private static float[] uv(Box box, Direction direction) {
        return switch (direction) {
            case DOWN -> new float[] {box.x0(), 16 - box.z1(), box.x1(), 16 - box.z0()};
            case UP -> new float[] {box.x0(), box.z0(), box.x1(), box.z1()};
            case NORTH -> new float[] {16 - box.x1(), 16 - box.y1(), 16 - box.x0(), 16 - box.y0()};
            case SOUTH -> new float[] {box.x0(), 16 - box.y1(), box.x1(), 16 - box.y0()};
            case WEST -> new float[] {box.z0(), 16 - box.y1(), box.z1(), 16 - box.y0()};
            case EAST -> new float[] {16 - box.z1(), 16 - box.y1(), 16 - box.z0(), 16 - box.y0()};
        };
    }

    /** Both halves with dark lamps and three windows for the item, scaled down in the hand and the GUI. */
    private static void item(BlockStateProvider provider, String name, Identifier iron) {
        BlockModelBuilder whole = base(provider, name + "_item", iron);
        lower().forEach(box -> element(whole, box, 0));
        lamps("off").forEach(box -> element(whole, box, 0));
        upper().forEach(box -> element(whole, box, 16));
        windows(ShuttlesStatsManager.DEFAULT_FLUID_STATION_TANKS).forEach(box -> element(whole, box, 16));
        provider.itemModels().withExistingParent(name, provider.modLoc("block/" + name + "_item"))
                .transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, -3, 0).scale(0.4F).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 2, 0).scale(0.2F).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 0, 0).translation(0, -3, 0).scale(0.4F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.3F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(0, 0, 0).scale(0.3F).end()
                .end();
    }
}
