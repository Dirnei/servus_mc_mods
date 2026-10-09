package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import java.util.List;
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
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Datagen of the machines Foundation owns: models, loot, tags, lang and recipes. */
final class MachineDataParts {

    private MachineDataParts() {
    }

    static void blockStates(BlockStateProvider provider) {
        machine(provider, MachineContent.COAL_GENERATOR.get(), "coal_generator");
    }

    /** Blocks that drop themselves and are mined with a pickaxe. */
    static List<Block> selfDropping() {
        return List.of(MachineContent.COAL_GENERATOR.get());
    }

    static void lang(LanguageProvider provider) {
        provider.add(MachineContent.COAL_GENERATOR.get(), "Coal Generator");
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
