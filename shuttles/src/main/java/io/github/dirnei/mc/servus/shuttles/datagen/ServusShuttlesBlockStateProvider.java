package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.LayeredModels;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlock;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuShape;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuTiles;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadLook;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternLights;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
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

final class ServusShuttlesBlockStateProvider extends BlockStateProvider {

    ServusShuttlesBlockStateProvider(PackOutput output) {
        super(output, ServusShuttlesIds.MOD_ID);
    }

    @Override
    protected void registerStatesAndModels() {
        station(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get(), "warehouse_controller");
        woodOriented(ServusShuttlesBlocks.STORAGE_TERMINAL.get(), "storage_terminal");
        woodOriented(ServusShuttlesBlocks.PRODUCTION_TERMINAL.get(), "production_terminal");
        patternStorage(ServusShuttlesBlocks.PATTERN_STORAGE.get(), "pattern_storage");
        RailModels.rail(this, ServusShuttlesBlocks.RAIL.get(), "rail");
        RailModels.rail(this, ServusShuttlesBlocks.FARM_RAIL.get(), "farm_rail");
        fieldHead(ServusShuttlesBlocks.FIELD_HEAD.get(), "field_head");
        ElevatorModels.elevator(this, ServusShuttlesBlocks.ELEVATOR.get(), "elevator");
        HeavyRackModels.rack(this, ServusShuttlesBlocks.HEAVY_RACK.get(), "heavy_rack");
        FluidRackModels.rack(this, ServusShuttlesBlocks.FLUID_RACK.get(), "fluid_rack");
        station(ServusShuttlesBlocks.INBOUND_STATION.get(), "inbound_station");
        station(ServusShuttlesBlocks.PACKING_STATION.get(), "packing_station");
        garage(ServusShuttlesBlocks.SHUTTLE_GARAGE.get(), "shuttle_garage");
        station(ServusShuttlesBlocks.OUTBOUND_STATION.get(), "outbound_station");
        craftingStation(ServusShuttlesBlocks.CRAFTING_STATION.get(), "crafting_station");
        cpu(ServusShuttlesBlocks.CRAFTING_CPU.get(), "crafting_cpu");
        cpu(ServusShuttlesBlocks.CPU_BUFFER.get(), "crafting_cpu_buffer");
        itemModels().basicItem(ServusShuttlesItems.ZONE_LABEL.get());
        itemModels().basicItem(ServusShuttlesItems.HANDLING_UPGRADE.get());
        itemModels().basicItem(ServusShuttlesItems.RANGE_UPGRADE.get());
        itemModels().basicItem(ServusShuttlesItems.WIRELESS_UPGRADE.get());
        itemModels().basicItem(ServusShuttlesItems.HARVESTER.get());
        itemModels().basicItem(ServusShuttlesItems.SPREADER.get());
        itemModels().basicItem(ServusShuttlesItems.LUMBER.get());
        itemModels().basicItem(ServusShuttlesItems.TANKER.get());
        // The workshop: the same tools on all four sides, the bench on top, no facing.
        Map<Direction, Identifier> workshopFaces = new EnumMap<>(Direction.class);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            workshopFaces.put(side, modLoc("block/shuttle_workshop_side"));
        }
        workshopFaces.put(Direction.UP, modLoc("block/shuttle_workshop_top"));
        workshopFaces.put(Direction.DOWN, modLoc("block/shuttle_workshop_bottom"));
        ModelFile workshop = LayeredModels.cube(this, "shuttle_workshop", workshopFaces);
        simpleBlock(ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get(), workshop);
        simpleBlockItem(ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get(), workshop);
        ShuttleModels.shuttle(this);
        GantryModels.gantry(this, ServusShuttlesBlocks.FLUID_GANTRY.get(), "fluid_gantry");
        FluidStationModels.station(this, ServusShuttlesBlocks.FLUID_INBOUND.get(), "fluid_inbound");
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

    private static int rotationY(BlockState state) {
        return ModelRotations.rotationY(state.getValue(HorizontalDirectionalBlock.FACING));
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
                .rotationY(ModelRotations.rotationY(state.getValue(FieldHeadBlock.FACING)))
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
     * The garage (see {@link GarageModels}): the lamp on the front, the open
     * doorway with the rail running in on the back, a lit lamp while online
     * and a red one while a shuttle parks with cargo that has nowhere to go.
     */
    private void garage(Block block, String name) {
        ModelFile dark = GarageModels.garage(this, name, name + "_front");
        ModelFile lit = GarageModels.garage(this, name + "_online", name + "_front_online");
        ModelFile blocked = GarageModels.garage(this, name + "_blocked", name + "_front_blocked");
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(!state.getValue(StorageNodeBlocks.ONLINE) ? dark
                        : state.getValue(StorageNodeBlocks.ACTIVITY) == StorageNodeBlocks.Activity.BLOCKED ? blocked : lit)
                .rotationY(rotationY(state))
                .build());
        simpleBlockItem(block, dark);
    }

    /** A wood block: board panels under the iron overlay textures named after the block, with a lit front while online. */
    private void woodOriented(Block block, String name) {
        onlineLamp(block, orientable(name, name, name + "_front"), orientable(name, name + "_online", name + "_front_online"));
    }

}
