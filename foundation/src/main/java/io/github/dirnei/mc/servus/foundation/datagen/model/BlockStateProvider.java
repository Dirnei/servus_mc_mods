package io.github.dirnei.mc.servus.foundation.datagen.model;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Writes block state files, block models and the item models of block items
 * for one mod. Subclasses fill it in {@link #registerStatesAndModels()}.
 */
public abstract class BlockStateProvider implements DataProvider {

    private final PackOutput output;
    private final String modid;
    private final ModelProvider<BlockModelBuilder> blockModels;
    private final ModelProvider.Items itemModels;
    private final Map<Block, Supplier<JsonObject>> states = new LinkedHashMap<>();

    protected BlockStateProvider(PackOutput output, String modid) {
        this.output = output;
        this.modid = modid;
        this.blockModels = new ModelProvider<>(modid, "block", BlockModelBuilder::new);
        this.itemModels = new ModelProvider.Items(modid);
    }

    protected abstract void registerStatesAndModels();

    public ModelProvider<BlockModelBuilder> models() {
        return blockModels;
    }

    public ModelProvider.Items itemModels() {
        return itemModels;
    }

    public Identifier modLoc(String path) {
        return Identifier.fromNamespaceAndPath(modid, path);
    }

    public Identifier mcLoc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    public VariantBlockStateBuilder getVariantBuilder(Block block) {
        VariantBlockStateBuilder builder = new VariantBlockStateBuilder(block);
        register(block, builder::toJson);
        return builder;
    }

    public MultiPartBlockStateBuilder getMultipartBuilder(Block block) {
        MultiPartBlockStateBuilder builder = new MultiPartBlockStateBuilder(block);
        register(block, builder::toJson);
        return builder;
    }

    private void register(Block block, Supplier<JsonObject> json) {
        if (states.putIfAbsent(block, json) != null) {
            throw new IllegalStateException("Block state of " + block + " written twice");
        }
    }

    public void simpleBlock(Block block, ModelFile model) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        if (block.getStateDefinition().getProperties().isEmpty()) {
            builder.single(new ConfiguredModel(model));
        } else {
            builder.forAllStates(state -> new ConfiguredModel[] {new ConfiguredModel(model)});
        }
    }

    /** The item model of the block's item, {@code item/<block>}, with {@code model} as parent. */
    public void simpleBlockItem(Block block, ModelFile model) {
        itemModels.getBuilder(BuiltInRegistries.BLOCK.getKey(block).toString()).parent(model);
    }

    public void simpleBlockWithItem(Block block, ModelFile model) {
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    /** {@code FACING} turns the model's up face (rotation X) and north face (rotation Y) toward the facing. */
    public void directionalBlock(Block block, ModelFile model) {
        getVariantBuilder(block).forAllStates(state -> {
            Direction direction = state.getValue(BlockStateProperties.FACING);
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationX(direction == Direction.DOWN ? 180 : direction.getAxis().isHorizontal() ? 90 : 0)
                    .rotationY(direction.getAxis().isVertical() ? 0 : (((int) direction.toYRot()) + 180) % 360)
                    .build();
        });
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        registerStatesAndModels();
        PackOutput.PathProvider blockstates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        states.forEach((block, json) -> futures.add(DataProvider.saveStable(cache, json.get(),
                blockstates.json(BuiltInRegistries.BLOCK.getKey(block)))));
        futures.add(blockModels.write(cache, output));
        futures.add(itemModels.write(cache, output));
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Block states and models: " + modid;
    }
}
