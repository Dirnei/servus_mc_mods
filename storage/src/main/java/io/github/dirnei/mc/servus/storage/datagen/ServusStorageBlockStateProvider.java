package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.LayeredModels;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.cpu.CpuTiles;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadLook;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.pattern.PatternLights;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class ServusStorageBlockStateProvider extends BlockStateProvider {

    ServusStorageBlockStateProvider(PackOutput output) {
        super(output, ServusStorageIds.MOD_ID);
    }

    @Override
    protected void registerStatesAndModels() {
        station(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), "warehouse_controller");
        woodOriented(ServusStorageBlocks.STORAGE_TERMINAL.get(), "storage_terminal");
        woodOriented(ServusStorageBlocks.PRODUCTION_TERMINAL.get(), "production_terminal");
        patternStorage(ServusStorageBlocks.PATTERN_STORAGE.get(), "pattern_storage");
        RailModels.rail(this, ServusStorageBlocks.RAIL.get(), "rail");
        RailModels.rail(this, ServusStorageBlocks.FARM_RAIL.get(), "farm_rail");
        fieldHead(ServusStorageBlocks.FIELD_HEAD.get(), "field_head");
        ElevatorModels.elevator(this, ServusStorageBlocks.ELEVATOR.get(), "elevator");
        HeavyRackModels.rack(this, ServusStorageBlocks.HEAVY_RACK.get(), "heavy_rack");
        station(ServusStorageBlocks.INBOUND_STATION.get(), "inbound_station");
        station(ServusStorageBlocks.PACKING_STATION.get(), "packing_station");
        garage(ServusStorageBlocks.SHUTTLE_GARAGE.get(), "shuttle_garage");
        station(ServusStorageBlocks.OUTBOUND_STATION.get(), "outbound_station");
        craftingStation(ServusStorageBlocks.CRAFTING_STATION.get(), "crafting_station");
        cpu(ServusStorageBlocks.CRAFTING_CPU.get(), "crafting_cpu");
        cpu(ServusStorageBlocks.CPU_BUFFER.get(), "crafting_cpu_buffer");
        itemModels().basicItem(ServusStorageItems.ZONE_LABEL.get());
        itemModels().basicItem(ServusStorageItems.HANDLING_UPGRADE.get());
        itemModels().basicItem(ServusStorageItems.RANGE_UPGRADE.get());
        itemModels().basicItem(ServusStorageItems.WIRELESS_UPGRADE.get());
        itemModels().basicItem(ServusStorageItems.HARVESTER.get());
        itemModels().basicItem(ServusStorageItems.SPREADER.get());
        // The workshop: the same tools on all four sides, the bench on top, no facing.
        Map<Direction, Identifier> workshopFaces = new EnumMap<>(Direction.class);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            workshopFaces.put(side, modLoc("block/shuttle_workshop_side"));
        }
        workshopFaces.put(Direction.UP, modLoc("block/shuttle_workshop_top"));
        workshopFaces.put(Direction.DOWN, modLoc("block/shuttle_workshop_bottom"));
        ModelFile workshop = LayeredModels.cube(this, "shuttle_workshop", workshopFaces);
        simpleBlock(ServusStorageBlocks.SHUTTLE_WORKSHOP.get(), workshop);
        simpleBlockItem(ServusStorageBlocks.SHUTTLE_WORKSHOP.get(), workshop);
        ShuttleModels.shuttle(this);
    }

    /**
     * A CPU or Buffer block: a riveted casing while unformed; formed, each
     * block shows the tile of its place in the box on every face (see {@link CpuTiles}) with running
     * or idle lights.
     */
    private void cpu(Block block, String name) {
        ModelFile casing = LayeredModels.cube(this, name, allFaces(modLoc("block/" + name)));
        Map<String, ModelFile> formed = new HashMap<>();
        for (int part = 0; part < CpuShape.PARTS; part++) {
            for (boolean active : new boolean[] {false, true}) {
                Map<Direction, Identifier> faces = new EnumMap<>(Direction.class);
                for (Direction direction : Direction.values()) {
                    String tile = CpuTiles.tile(part, Conversions.toSide(direction)).substring("tile_".length());
                    faces.put(direction, modLoc("block/" + name + (active ? "_active_" : "_idle_") + tile));
                }
                String model = name + "_" + part + (active ? "_active" : "_idle");
                formed.put(model, LayeredModels.cube(this, model, faces));
            }
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CpuBlock.FORMED)
                        ? formed.get(name + "_" + state.getValue(CpuBlock.PART) + (state.getValue(CpuBlock.ACTIVE) ? "_active" : "_idle"))
                        : casing)
                .build());
        simpleBlockItem(block, casing);
    }

    /** The y rotation that turns a north built model to face that way. */
    static int rotationY(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }

    private static int rotationY(BlockState state) {
        return rotationY(state.getValue(HorizontalDirectionalBlock.FACING));
    }

    /** A front facing block whose online lamp switches between two models; the item shows the dark one. */
    private void onlineLamp(Block block, ModelFile dark, ModelFile lit) {
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StorageNodeBlocks.ONLINE) ? lit : dark)
                .rotationY(rotationY(state))
                .build());
        simpleBlockItem(block, dark);
    }

    /** An orientable model with the side and top textures of {@code name} and the given front. */
    private ModelFile orientable(String name, String model, String front) {
        return LayeredModels.orientable(this, model, modLoc("block/" + name + "_side"), modLoc("block/" + front), modLoc("block/" + name + "_top"));
    }

    private static Map<Direction, Identifier> allFaces(Identifier texture) {
        Map<Direction, Identifier> faces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(direction, texture);
        }
        return faces;
    }

    /** Pattern Storage: the front shows one lit light per stored pattern and the online lamp. */
    private void patternStorage(Block block, String name) {
        ModelFile[][] models = new ModelFile[2][PatternLights.LIGHTS + 1];
        for (int online = 0; online < 2; online++) {
            for (int lit = 0; lit <= PatternLights.LIGHTS; lit++) {
                String suffix = "_" + lit + (online == 1 ? "_online" : "");
                models[online][lit] = orientable(name, name + suffix, name + "_front" + suffix);
            }
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(StorageNodeBlocks.ONLINE) ? 1 : 0][state.getValue(PatternStorageBlock.LIGHTS)])
                .rotationY(rotationY(state))
                .build());
        simpleBlockItem(block, models[0][0]);
    }

    /** The Crafting Station: online lamp, and an animated front while it crafts. */
    private void craftingStation(Block block, String name) {
        ModelFile[][] models = new ModelFile[2][2];
        for (int online = 0; online < 2; online++) {
            for (int lit = 0; lit < 2; lit++) {
                String suffix = (lit == 1 ? "_lit" : "") + (online == 1 ? "_online" : "");
                models[online][lit] = orientable(name, name + suffix, name + "_front" + suffix);
            }
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(StorageNodeBlocks.ONLINE) ? 1 : 0][state.getValue(BlockStateProperties.LIT) ? 1 : 0])
                .rotationY(rotationY(state))
                .build());
        simpleBlockItem(block, models[0][0]);
    }

    /**
     * The Field Head: station boards and frame, a glass window in the upper
     * panel of every side, the ripe bar of its {@link FieldHeadBlock#RIPE} step
     * on the front (full and red with {@link FieldHeadBlock#PROBLEM}), the crop plate on top and a plain frame below.
     */
    private void fieldHead(Block block, String name) {
        // One more model than steps: the last one has the full red bar of a head with a problem.
        int problemModel = FieldHeadLook.READY + 1;
        ModelFile[] models = new ModelFile[problemModel + 1];
        ModelFile[] corroded = new ModelFile[problemModel + 1];
        for (int step = 0; step <= problemModel; step++) {
            Identifier side = modLoc("block/" + name + "_side");
            String front = step == problemModel ? "problem" : String.valueOf(step);
            Map<Direction, Identifier> faces = new EnumMap<>(Direction.class);
            faces.put(Direction.NORTH, modLoc("block/" + name + "_front_" + front));
            faces.put(Direction.SOUTH, side);
            faces.put(Direction.EAST, side);
            faces.put(Direction.WEST, side);
            faces.put(Direction.UP, modLoc("block/" + name + "_top"));
            faces.put(Direction.DOWN, modLoc("block/" + name + "_bottom"));
            models[step] = LayeredModels.cube(this, name + "_" + front, faces);
            // A corroded field: the crop plate gets a rust rim.
            faces.put(Direction.UP, modLoc("block/" + name + "_top_corroded"));
            corroded[step] = LayeredModels.cube(this, name + "_" + front + "_corroded", faces);
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile((state.getValue(FieldHeadBlock.CORRODED) ? corroded : models)
                        [state.getValue(FieldHeadBlock.PROBLEM) ? problemModel : state.getValue(FieldHeadBlock.RIPE)])
                .rotationY(rotationY(state.getValue(FieldHeadBlock.FACING)))
                .build());
        simpleBlockItem(block, models[0]);
    }

    /** A front facing block with an activity lamp: idle, working and blocked fronts while online. */
    private void station(Block block, String name) {
        ModelFile dark = orientable(name, name, name + "_front");
        ModelFile idle = orientable(name, name + "_idle", name + "_front_idle");
        ModelFile working = orientable(name, name + "_working", name + "_front_working");
        ModelFile blocked = orientable(name, name + "_blocked", name + "_front_blocked");
        getVariantBuilder(block).forAllStates(state -> {
            ModelFile model = dark;
            if (state.getValue(StorageNodeBlocks.ONLINE)) {
                model = switch (state.getValue(StorageNodeBlocks.ACTIVITY)) {
                    case IDLE -> idle;
                    case WORKING -> working;
                    case BLOCKED -> blocked;
                };
            }
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(rotationY(state))
                    .build();
        });
        simpleBlockItem(block, dark);
    }

    /**
     * The garage: the lamp on the front, the doorway with the rail on the back,
     * the side texture left and right, the top texture above and below, and a
     * lit lamp while online.
     */
    private void garage(Block block, String name) {
        onlineLamp(block, garageModel(name, name + "_front"), garageModel(name + "_online", name + "_front_online"));
    }

    private ModelFile garageModel(String model, String front) {
        Identifier side = modLoc("block/shuttle_garage_side");
        Identifier top = modLoc("block/shuttle_garage_top");
        Map<Direction, Identifier> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.DOWN, top);
        faces.put(Direction.UP, top);
        faces.put(Direction.NORTH, modLoc("block/" + front));
        faces.put(Direction.SOUTH, modLoc("block/shuttle_garage_back"));
        faces.put(Direction.EAST, side);
        faces.put(Direction.WEST, side);
        return LayeredModels.cube(this, model, faces);
    }

    /** A wood block: board panels under the iron overlay textures named after the block, with a lit front while online. */
    private void woodOriented(Block block, String name) {
        onlineLamp(block, orientable(name, name, name + "_front"), orientable(name, name + "_online", name + "_front_online"));
    }

}
