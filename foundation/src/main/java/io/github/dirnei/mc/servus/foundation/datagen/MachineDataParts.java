package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpBlock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Datagen of the machines Foundation owns: models, loot, tags, lang and recipes. */
final class MachineDataParts {

    private MachineDataParts() {
    }

    static void blockStates(BlockStateProvider provider) {
        machine(provider, MachineContent.COAL_GENERATOR.get(), "coal_generator");
        pump(provider, MachineContent.BURNER_PUMP.get(), "burner_pump");
        pump(provider, MachineContent.ELECTRIC_PUMP.get(), "electric_pump");
    }

    /** Blocks that drop themselves and are mined with a pickaxe. */
    static List<Block> selfDropping() {
        return List.of(MachineContent.COAL_GENERATOR.get(), MachineContent.BURNER_PUMP.get(), MachineContent.ELECTRIC_PUMP.get());
    }

    static void lang(LanguageProvider provider) {
        provider.add(MachineContent.COAL_GENERATOR.get(), "Coal Generator");
        provider.add(MachineContent.BURNER_PUMP.get(), "Burner Pump");
        provider.add(MachineContent.ELECTRIC_PUMP.get(), "Electric Pump");
        provider.add("gui.servus_foundation.pump.source_ok", "Infinite water source below");
        provider.add("gui.servus_foundation.pump.source_missing", "Needs an infinite water source below");
    }

    static void recipes(RecipeOutput output, Function<ItemLike, Criterion<InventoryChangeTrigger.TriggerInstance>> has) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MachineContent.COAL_GENERATOR_ITEM.get())
                .pattern("ICI")
                .pattern("CFC")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('F', Items.FURNACE)
                .unlockedBy("has_copper_ingot", has.apply(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MachineContent.BURNER_PUMP_ITEM.get())
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
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MachineContent.ELECTRIC_PUMP_ITEM.get())
                .pattern("CRC")
                .pattern("IPI")
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .define('P', MachineContent.BURNER_PUMP_ITEM.get())
                .unlockedBy("has_burner_pump", has.apply(MachineContent.BURNER_PUMP_ITEM.get()))
                .save(output);
    }

    /**
     * The compact pump as a multipart: the body per facing and lit state, plus a
     * pipe stub per connected absolute direction (unrotated, the stubs are symmetric).
     */
    static void pump(BlockStateProvider provider, Block block, String name) {
        ModelFile unlit = pumpModel(provider, name, provider.modLoc("block/" + name + "_front"));
        ModelFile lit = pumpModel(provider, name + "_lit", provider.modLoc("block/" + name + "_front_lit"));
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean isLit : new boolean[] {false, true}) {
                builder.part().modelFile(isLit ? lit : unlit).rotationY(((int) facing.toYRot() + 180) % 360).addModel()
                        .condition(MachineBlock.FACING, facing).condition(MachineBlock.LIT, isLit).end();
            }
        }
        for (Map.Entry<Direction, IntegerProperty> stub : PumpBlock.STUBS.entrySet()) {
            for (int size = 1; size <= PumpBlock.MAX_STUB; size++) {
                builder.part().modelFile(pumpStub(provider, stub.getKey(), size)).addModel().condition(stub.getValue(), size).end();
            }
        }
        provider.simpleBlockItem(block, unlit);
    }

    /** A short pipe from the pump body to the block edge, as thick as a duct arm with {@code size} lines; shared by both pumps. */
    private static ModelFile pumpStub(BlockStateProvider provider, Direction direction, int size) {
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

    private static ModelFile pumpModel(BlockStateProvider provider, String name, ResourceLocation front) {
        BlockModelBuilder model = provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("particle", provider.modLoc("block/machine_side"))
                .texture("side", provider.modLoc("block/machine_side"))
                .texture("top", provider.modLoc("block/machine_top"))
                .texture("front", front);
        model.element().from(1, 0, 1).to(15, 2, 15)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(1, 14, 15, 16))
                .face(Direction.UP).uvs(1, 1, 15, 15).end()
                .face(Direction.DOWN).uvs(1, 1, 15, 15).cullface(Direction.DOWN).end();
        model.element().from(3, 2, 3).to(13, 12, 13)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(3, 4, 13, 14))
                .face(Direction.NORTH).texture("#front").uvs(0, 0, 16, 16).end()
                .face(Direction.UP).uvs(3, 3, 13, 13).end();
        model.element().from(6, 12, 6).to(10, 15, 10)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                        .uvs(6, 1, 10, 4))
                .face(Direction.UP).uvs(6, 6, 10, 10).end()
                .face(Direction.DOWN).uvs(6, 6, 10, 10).end();
        return model;
    }

    /** Orientable machine with a lit and an unlit front, rotated by facing. */
    static void machine(BlockStateProvider provider, Block block, String name) {
        ResourceLocation side = provider.modLoc("block/machine_side");
        ResourceLocation top = provider.modLoc("block/machine_top");
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
