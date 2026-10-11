package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import io.github.dirnei.mc.servus.foundation.datagen.LayeredModels;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadLook;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.cpu.CpuTiles;
import io.github.dirnei.mc.servus.storage.pattern.PatternLights;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;

final class ServusStorageBlockStateProvider extends BlockStateProvider {

    ServusStorageBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusStorageIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        oriented(ServusStorageBlocks.CRATE.get(), "crate", "crate");
        oriented(ServusStorageBlocks.DOUBLE_CRATE.get(), "double_crate", "crate");
        oriented(ServusStorageBlocks.QUAD_CRATE.get(), "quad_crate", "crate");
        woodOriented(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), "warehouse_controller");
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
        itemModels().basicItem(ServusStorageItems.ZONE_LABEL.get());
        itemModels().basicItem(ServusStorageItems.HANDLING_UPGRADE.get());
        itemModels().basicItem(ServusStorageItems.RANGE_UPGRADE.get());
        itemModels().basicItem(ServusStorageItems.HARVESTER.get());
        ModelFile workshop = models().cubeBottomTop("shuttle_workshop", modLoc("block/shuttle_workshop_side"),
                modLoc("block/shuttle_workshop_bottom"), modLoc("block/shuttle_workshop_top"));
        simpleBlock(ServusStorageBlocks.SHUTTLE_WORKSHOP.get(), workshop);
        simpleBlockItem(ServusStorageBlocks.SHUTTLE_WORKSHOP.get(), workshop);
        ShuttleModels.shuttle(this);
    }

    /**
     * The CPU: a riveted casing while unformed; formed, each block shows the
     * tile of its position on every face (see {@link CpuTiles}) with running
     * or idle lights.
     */
    private void cpu(Block block, String name) {
        ModelFile casing = LayeredModels.cube(this, name, allFaces(modLoc("block/" + name)));
        Map<String, ModelFile> formed = new HashMap<>();
        for (int part = 0; part < CpuShape.PARTS; part++) {
            for (boolean active : new boolean[] {false, true}) {
                Map<Direction, ResourceLocation> faces = new EnumMap<>(Direction.class);
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

    private static Map<Direction, ResourceLocation> allFaces(ResourceLocation texture) {
        Map<Direction, ResourceLocation> faces = new EnumMap<>(Direction.class);
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
                models[online][lit] = LayeredModels.orientable(this, name + suffix, modLoc("block/" + name + "_side"),
                        modLoc("block/" + name + "_front" + suffix), modLoc("block/" + name + "_top"));
            }
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(StorageNodeBlocks.ONLINE) ? 1 : 0][state.getValue(PatternStorageBlock.LIGHTS)])
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, models[0][0]);
    }

    /** The Crafting Station: online lamp, and an animated front while it crafts. */
    private void craftingStation(Block block, String name) {
        ModelFile[][] models = new ModelFile[2][2];
        for (int online = 0; online < 2; online++) {
            for (int lit = 0; lit < 2; lit++) {
                String suffix = (lit == 1 ? "_lit" : "") + (online == 1 ? "_online" : "");
                models[online][lit] = LayeredModels.orientable(this, name + suffix, modLoc("block/" + name + "_side"),
                        modLoc("block/" + name + "_front" + suffix), modLoc("block/" + name + "_top"));
            }
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(StorageNodeBlocks.ONLINE) ? 1 : 0]
                        [state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 1 : 0])
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, models[0][0]);
    }

    /**
     * The Field Head: station boards and frame, a glass window in the upper
     * panel of every side, the ripe bar of its {@link FieldHeadBlock#RIPE} step
     * on the front, the crop plate on top and a plain frame below.
     */
    private void fieldHead(Block block, String name) {
        ModelFile[] models = new ModelFile[FieldHeadLook.READY + 1];
        for (int step = 0; step <= FieldHeadLook.READY; step++) {
            ResourceLocation side = modLoc("block/" + name + "_side");
            Map<Direction, ResourceLocation> faces = new EnumMap<>(Direction.class);
            faces.put(Direction.NORTH, modLoc("block/" + name + "_front_" + step));
            faces.put(Direction.SOUTH, side);
            faces.put(Direction.EAST, side);
            faces.put(Direction.WEST, side);
            faces.put(Direction.UP, modLoc("block/" + name + "_top"));
            faces.put(Direction.DOWN, modLoc("block/" + name + "_bottom"));
            models[step] = LayeredModels.cube(this, name + "_" + step, faces);
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(FieldHeadBlock.RIPE)])
                .rotationY(((int) state.getValue(FieldHeadBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, models[0]);
    }

    /** Like {@link #oriented} with an activity lamp: idle, working and blocked fronts while online. */
    private void station(Block block, String name) {
        ModelFile dark = LayeredModels.orientable(this, name, modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        ModelFile idle = LayeredModels.orientable(this, name + "_idle", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_idle"), modLoc("block/" + name + "_top"));
        ModelFile working = LayeredModels.orientable(this, name + "_working", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_working"), modLoc("block/" + name + "_top"));
        ModelFile blocked = LayeredModels.orientable(this, name + "_blocked", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_blocked"), modLoc("block/" + name + "_top"));
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
                    .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(block, dark);
    }

    /**
     * The garage: the lamp on the front, the doorway with the rail on the back,
     * the side texture left and right, the top texture above and below, and a
     * lit lamp while online. Rotated by the facing like {@link #oriented}.
     */
    private void garage(Block block, String name) {
        ModelFile dark = garageModel(name, name + "_front");
        ModelFile lit = garageModel(name + "_online", name + "_front_online");
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StorageNodeBlocks.ONLINE) ? lit : dark)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, dark);
    }

    private ModelFile garageModel(String model, String front) {
        ResourceLocation side = modLoc("block/shuttle_garage_side");
        ResourceLocation top = modLoc("block/shuttle_garage_top");
        Map<Direction, ResourceLocation> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.DOWN, top);
        faces.put(Direction.UP, top);
        faces.put(Direction.NORTH, modLoc("block/" + front));
        faces.put(Direction.SOUTH, modLoc("block/shuttle_garage_back"));
        faces.put(Direction.EAST, side);
        faces.put(Direction.WEST, side);
        return LayeredModels.cube(this, model, faces);
    }

    /** {@link #oriented} for a wood block: board panels under the iron overlay textures named after the block. */
    private void woodOriented(Block block, String name) {
        ModelFile dark = LayeredModels.orientable(this, name, modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        ModelFile lit = LayeredModels.orientable(this, name + "_online", modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_online"), modLoc("block/" + name + "_top"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StorageNodeBlocks.ONLINE) ? lit : dark)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, dark);
    }

    /**
     * Horizontal block with its own front texture, side and top textures named
     * after {@code body}, and a lit front while online.
     */
    private void oriented(Block block, String name, String body) {
        ModelFile dark = models().orientable(name, modLoc("block/" + body + "_side"),
                modLoc("block/" + name + "_front"), modLoc("block/" + body + "_top"));
        ModelFile lit = models().orientable(name + "_online", modLoc("block/" + body + "_side"),
                modLoc("block/" + name + "_front_online"), modLoc("block/" + body + "_top"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(StorageNodeBlocks.ONLINE) ? lit : dark)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(block, dark);
    }
}
