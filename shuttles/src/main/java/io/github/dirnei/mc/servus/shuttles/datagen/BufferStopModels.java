package io.github.dirnei.mc.servus.shuttles.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.foundation.material.client.MaterialSkin;
import io.github.dirnei.mc.servus.shuttles.rail.BufferStopBlock;
import io.github.dirnei.mc.servus.shuttles.rail.BufferStopBlock.Rail;
import io.github.dirnei.mc.servus.shuttles.rail.BufferStopLook;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;

/**
 * Datagen for the Buffer Stop: one model of the {@link BufferStopLook} boxes
 * per {@link BufferStopBlock#RAIL} kind, turned per facing. Sleepers use the
 * wood board texture so {@code MaterialModel} shows the stop's wood; the frame
 * is steel on every face, the beam carries the yellow and black dock stripe and
 * the stub the heads of the stop's rail. The item picks its model by the rail
 * kind in its block state.
 */
final class BufferStopModels {

    private BufferStopModels() {
    }

    static void bufferStop(BlockStateProvider provider, Block block, String name) {
        Map<Rail, BlockModelBuilder> models = new EnumMap<>(Rail.class);
        for (Rail rail : Rail.values()) {
            models.put(rail, model(provider, name + "_" + rail.getSerializedName(), headTexture(rail)));
        }
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models.get(state.getValue(BufferStopBlock.RAIL)))
                .rotationY(rotationY(state.getValue(BufferStopBlock.FACING)))
                .build());

        for (Rail rail : Rail.values()) {
            String model = name + "_" + rail.getSerializedName();
            provider.itemModels().withExistingParent(model, provider.modLoc("block/" + model))
                    .transforms()
                    .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, 1, 0).scale(0.625F).end()
                    .transform(ItemDisplayContext.FIXED).rotation(0, 0, 0).translation(0, 0, 0).scale(0.5F).end()
                    .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 3, 0).scale(0.25F).end()
                    .end();
        }
        provider.itemModels().itemModelDefinition(provider.modLoc(name), itemDefinition(provider, name));
    }

    private static String headTexture(Rail rail) {
        return rail == Rail.FARM ? "block/farm_rail_head" : "block/rail_head";
    }

    private static BlockModelBuilder model(BlockStateProvider provider, String name, String head) {
        Identifier steel = provider.modLoc("block/heavy_rack_iron");
        BlockModelBuilder model = provider.models().withExistingParent(name, "minecraft:block/block")
                .texture("particle", steel).texture("frame", steel)
                .texture("head", provider.modLoc(head))
                .texture("dock", provider.modLoc("block/rail_dock"))
                .texture("wood", MaterialKind.WOOD.placeholder());
        RailModels.addBoxes(model, BufferStopLook.boxes());
        return model;
    }

    /** A select on the stack's rail block state, each case the wood material item model of that kind. */
    private static JsonObject itemDefinition(BlockStateProvider provider, String name) {
        JsonObject select = new JsonObject();
        select.addProperty("type", "minecraft:select");
        select.addProperty("property", "minecraft:block_state");
        select.addProperty("block_state_property", BufferStopBlock.RAIL.getName());
        JsonArray cases = new JsonArray();
        for (Rail rail : Rail.values()) {
            if (rail == Rail.WAREHOUSE) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("when", rail.getSerializedName());
            entry.add("model", materialModel(provider, name + "_" + rail.getSerializedName()));
            cases.add(entry);
        }
        select.add("cases", cases);
        select.add("fallback", materialModel(provider, name + "_" + Rail.WAREHOUSE.getSerializedName()));
        return select;
    }

    private static JsonObject materialModel(BlockStateProvider provider, String model) {
        JsonObject json = new JsonObject();
        json.addProperty("type", MaterialSkin.ITEM_MODEL_TYPE.toString());
        json.addProperty("kind", MaterialKind.WOOD.getSerializedName());
        json.addProperty("model", provider.modLoc("item/" + model).toString());
        return json;
    }

    /** The model faces north; a blockstate y rotation turns it clockwise. */
    private static int rotationY(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
