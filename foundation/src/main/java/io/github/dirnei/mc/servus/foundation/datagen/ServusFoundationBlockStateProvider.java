package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.datagen.LogisticsDataParts;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

final class ServusFoundationBlockStateProvider extends BlockStateProvider {

    ServusFoundationBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusFoundationIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        LogisticsDataParts.blockStates(this);
        // directionalBlock turns the model's up face toward the facing: the port on top, the funnel below.
        Map<Direction, ResourceLocation> adapterFaces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            adapterFaces.put(direction, modLoc("block/machine_adapter_side"));
        }
        adapterFaces.put(Direction.UP, modLoc("block/machine_adapter_front"));
        adapterFaces.put(Direction.DOWN, modLoc("block/machine_adapter_back"));
        ModelFile adapter = LayeredModels.cube(this, "machine_adapter", adapterFaces);
        directionalBlock(AdapterContent.BLOCK.get(), adapter);
        simpleBlockItem(AdapterContent.BLOCK.get(), adapter);
    }
}
