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
        enderAnchor(provider);
        BurnerPumpModels.burnerPump(provider);
        ElectricPumpModels.electricPump(provider);
        SinkModels.sink(provider);
        SinkModels.chimney(provider);
        SinkModels.sinkFan(provider);
    }

    /** Blocks that drop themselves and are mined with a pickaxe. */
    static List<Block> selfDropping() {
        return List.of(MachineContent.COAL_GENERATOR.get(), MachineContent.BURNER_PUMP.get(), MachineContent.ELECTRIC_PUMP.get(),
                MachineContent.SINK.get(), MachineContent.CHIMNEY.get(), MachineContent.ENDER_ANCHOR.get());
    }

    static void lang(LanguageProvider provider) {
        provider.add(MachineContent.COAL_GENERATOR.get(), "Coal Generator");
        provider.add(MachineContent.BURNER_PUMP.get(), "Burner Pump");
        provider.add(MachineContent.ELECTRIC_PUMP.get(), "Electric Pump");
        provider.add("gui.servus_foundation.pump.source_ok", "Infinite water source below");
        provider.add("gui.servus_foundation.pump.source_missing", "Needs an infinite water source below");
        provider.add(MachineContent.ENDER_ANCHOR.get(), "Ender Anchor");
        provider.add("gui.servus_foundation.ender_anchor.no_power", "Inactive: no power");
        provider.add("gui.servus_foundation.ender_anchor.active", "Active: area kept loaded");
        provider.add("gui.servus_foundation.ender_anchor.team_limit", "Inactive: team limit reached");
        provider.add("gui.servus_foundation.ender_anchor.ftb_chunks", "Inactive: use FTB Chunks");
        provider.add("gui.servus_foundation.ender_anchor.no_owner", "Inactive: no owner");
        provider.add("gui.servus_foundation.ender_anchor.area", "Area: %s x %s");
        provider.add("gui.servus_foundation.ender_anchor.team_load", "Team: %s / %s chunks");
        provider.add("gui.servus_foundation.ender_anchor.show_range", "Show range");
        provider.add("gui.servus_foundation.ender_anchor.show_range.tooltip", "Outline this anchor's area in the world, only for you");
        provider.add(MachineContent.ANCHOR_UPGRADE.get(), "Anchor Upgrade");
        provider.add("item.servus_foundation.anchor_upgrade.tooltip",
                "Goes into an Ender Anchor to widen its area by a ring of chunks, or into a Warehouse Controller so the site can wake for transfers");
        provider.add("servus_foundation.configuration.enderAnchor", "Ender Anchor");
        provider.add("servus_foundation.configuration.maxLoadedChunksPerTeam", "Max loaded chunks per team");
        provider.add("servus_foundation.configuration.enderAnchorRespectsFtbChunks", "Ender Anchor respects FTB Chunks");
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

        // No assembler in Foundation: the Ender Anchor is crafted by hand.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.ENDER_ANCHOR_ITEM.get())
                .pattern("ICI")
                .pattern("RER")
                .pattern("OCO")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('E', Items.ENDER_PEARL)
                .define('O', Items.OBSIDIAN)
                .unlockedBy("has_ender_pearl", has.apply(Items.ENDER_PEARL))
                .save(output);

        // An Ender Anchor in the middle: widens another anchor's area or lets a warehouse site wake.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, MachineContent.ANCHOR_UPGRADE.get())
                .pattern(" E ")
                .pattern("IAI")
                .pattern(" R ")
                .define('E', Items.ENDER_PEARL)
                .define('I', Items.IRON_INGOT)
                .define('A', MachineContent.ENDER_ANCHOR_ITEM.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_ender_anchor", has.apply(MachineContent.ENDER_ANCHOR_ITEM.get()))
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

    /**
     * The Ender Anchor: obsidian front with the shut eye in
     * its socket (the renderer draws the open eye over it), chunk grid sides,
     * and a top whose purple core brightens while it holds its chunk.
     */
    static void enderAnchor(BlockStateProvider provider) {
        Block block = MachineContent.ENDER_ANCHOR.get();
        Identifier side = provider.modLoc("block/ender_anchor_side");
        Identifier front = provider.modLoc("block/ender_anchor_front");
        ModelFile unlit = provider.models().orientable("ender_anchor", side, front, provider.modLoc("block/ender_anchor_top"));
        ModelFile lit = provider.models().orientable("ender_anchor_lit", side, front, provider.modLoc("block/ender_anchor_top_lit"));
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(MachineBlock.LIT) ? lit : unlit)
                .rotationY(((int) state.getValue(MachineBlock.FACING).toYRot() + 180) % 360)
                .build());
        provider.simpleBlockItem(block, unlit);
    }
}
