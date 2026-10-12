package io.github.dirnei.mc.servus.foundation.datagen;

import net.minecraft.world.item.Item;
import net.minecraft.core.HolderGetter;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpBlock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Datagen of the machines Foundation owns: models, loot, tags, lang and recipes. */
final class MachineDataParts {

    private MachineDataParts() {
    }

    static void blockStates(BlockStateProvider provider) {
        CoalGeneratorModels.coalGenerator(provider);
        machine(provider, MachineContent.CHUNK_LOADER.get(), "chunk_loader");
        BurnerPumpModels.burnerPump(provider);
        ElectricPumpModels.electricPump(provider);
        SinkModels.sink(provider);
        SinkModels.chimney(provider);
        SinkModels.sinkFan(provider);
    }

    /** Blocks that drop themselves and are mined with a pickaxe. */
    static List<Block> selfDropping() {
        return List.of(MachineContent.COAL_GENERATOR.get(), MachineContent.BURNER_PUMP.get(), MachineContent.ELECTRIC_PUMP.get(),
                MachineContent.SINK.get(), MachineContent.CHIMNEY.get(), MachineContent.CHUNK_LOADER.get());
    }

    static void lang(LanguageProvider provider) {
        provider.add(MachineContent.COAL_GENERATOR.get(), "Coal Generator");
        provider.add(MachineContent.BURNER_PUMP.get(), "Burner Pump");
        provider.add(MachineContent.ELECTRIC_PUMP.get(), "Electric Pump");
        provider.add("gui.servus_foundation.pump.source_ok", "Infinite water source below");
        provider.add("gui.servus_foundation.pump.source_missing", "Needs an infinite water source below");
        provider.add(MachineContent.CHUNK_LOADER.get(), "Chunk Loader");
        provider.add("gui.servus_foundation.chunk_loader.no_power", "Inactive: no power");
        provider.add("gui.servus_foundation.chunk_loader.active", "Active: chunk kept loaded");
        provider.add("gui.servus_foundation.chunk_loader.team_limit", "Inactive: team limit reached");
        provider.add("gui.servus_foundation.chunk_loader.ftb_chunks", "Inactive: use FTB Chunks");
        provider.add("gui.servus_foundation.chunk_loader.no_owner", "Inactive: no owner");
        provider.add("servus_foundation.configuration.chunkLoader", "Chunk Loader");
        provider.add("servus_foundation.configuration.maxChunkLoadersPerTeam", "Max chunk loaders per team");
        provider.add("servus_foundation.configuration.chunkLoaderRespectsFtbChunks", "Chunk loader respects FTB Chunks");
        provider.add(MachineContent.SINK.get(), "Sink");
        provider.add(MachineContent.CHIMNEY.get(), "Chimney");
        provider.add("gui.servus_foundation.sink.height", "Chimney height: %s");
        provider.add("gui.servus_foundation.sink.items", "Items: %s");
        provider.add("gui.servus_foundation.sink.fluid", "Fluid: %s");
        provider.add("gui.servus_foundation.sink.energy", "Energy: %s");
        provider.add("gui.servus_foundation.sink.unit.items", "%s per second");
        provider.add("gui.servus_foundation.sink.unit.fluid", "%s mB per tick");
        provider.add("gui.servus_foundation.sink.unit.energy", "%s FE per tick");
        provider.add("gui.servus_foundation.sink.unlimited", "Unlimited");
        provider.add("gui.servus_foundation.sink.hint", "Taller chimney, faster Sink");
        provider.add("gui.servus_foundation.sink.trash", "Anything put here is destroyed");
        provider.add("message.servus_foundation.sink.casing",
                "Build a 2x2x2 cube of Sink blocks and click its front with the Configuration Carrot");
        provider.add("message.servus_foundation.sink.incomplete", "A Sink needs a full 2x2x2 cube of Sink blocks behind this face");
        provider.add("message.servus_foundation.sink.side_face", "Click a side of the cube: that side becomes the front");
    }

    static void recipes(RecipeOutput output, HolderGetter<Item> items, Function<ItemLike, Criterion<InventoryChangeTrigger.TriggerInstance>> has) {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.COAL_GENERATOR_ITEM.get())
                .pattern("ICI")
                .pattern("CFC")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('F', Items.FURNACE)
                .unlockedBy("has_copper_ingot", has.apply(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.BURNER_PUMP_ITEM.get())
                .pattern("IBI")
                .pattern("IFI")
                .pattern("CCC")
                .define('I', Items.IRON_INGOT)
                .define('B', Items.BUCKET)
                .define('F', Items.FURNACE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_bucket", has.apply(Items.BUCKET))
                .save(output);

        // No research and no assembler in Foundation: the Electric Pump is crafted from a Burner Pump by hand.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.ELECTRIC_PUMP_ITEM.get())
                .pattern("CRC")
                .pattern("IPI")
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .define('P', MachineContent.BURNER_PUMP_ITEM.get())
                .unlockedBy("has_burner_pump", has.apply(MachineContent.BURNER_PUMP_ITEM.get()))
                .save(output);

        // No assembler in Foundation: the Chunk Loader is crafted by hand.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.CHUNK_LOADER_ITEM.get())
                .pattern("ICI")
                .pattern("RER")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('E', Items.ENDER_PEARL)
                .unlockedBy("has_ender_pearl", has.apply(Items.ENDER_PEARL))
                .save(output);

        // Eight Sink blocks build one 2x2x2 Sink.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.SINK_ITEM.get(), 8)
                .pattern("BBB")
                .pattern("BRB")
                .pattern("BBB")
                .define('B', Items.BRICKS)
                .define('R', Items.IRON_BARS)
                .unlockedBy("has_bricks", has.apply(Items.BRICKS))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.CHIMNEY_ITEM.get(), 2)
                .pattern("B B")
                .pattern("B B")
                .pattern("B B")
                .define('B', Items.BRICK)
                .unlockedBy("has_brick", has.apply(Items.BRICK))
                .save(output);
    }

    /** A short pipe from the pump body to the block edge, as thick as a duct arm with {@code size} lines; shared by both pumps. */
    static ModelFile pumpStub(BlockStateProvider provider, Direction direction, int size) {
        BlockModelBuilder model = provider.models()
                .withExistingParent("pump_stub_" + direction.getSerializedName() + "_" + size, provider.mcLoc("block/block"))
                .texture("particle", provider.modLoc("block/machine_side"))
                .texture("side", provider.modLoc("block/machine_side"))
                .texture("top", provider.modLoc("block/machine_top"));
        int[] box = PumpBlock.stubBox(direction, size);
        int lo = 8 - PumpBlock.stubRadius(size);
        int hi = 8 + PumpBlock.stubRadius(size);
        model.element().from(box[0], box[1], box[2]).to(box[3], box[4], box[5])
                .allFaces((face, builder) -> builder.texture(face == direction || face == direction.getOpposite() ? "#top" : "#side")
                        .uvs(lo, lo, hi, hi))
                .face(direction).cullface(direction).end();
        return model;
    }

    /** Orientable machine with a lit and an unlit front, rotated by facing. */
    static void machine(BlockStateProvider provider, Block block, String name) {
        Identifier side = provider.modLoc("block/machine_side");
        Identifier top = provider.modLoc("block/machine_top");
        ModelFile unlit = provider.models().orientable(name, side, provider.modLoc("block/" + name + "_front"), top);
        ModelFile lit = provider.models().orientable(name + "_lit", side, provider.modLoc("block/" + name + "_front_lit"), top);
        provider.getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(MachineBlock.FACING);
            boolean isLit = state.getValue(MachineBlock.LIT);
            return ConfiguredModel.builder()
                    .modelFile(isLit ? lit : unlit)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
        provider.simpleBlockItem(block, unlit);
    }
}
