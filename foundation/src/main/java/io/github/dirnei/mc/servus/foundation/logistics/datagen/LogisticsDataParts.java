package io.github.dirnei.mc.servus.foundation.logistics.datagen;

import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import java.util.List;
import java.util.function.Function;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * The duct, tank and facade part of the Foundation data run. Foundation has one
 * provider per kind; each calls these helpers for the logistics content.
 */
public final class LogisticsDataParts {

    private LogisticsDataParts() {
    }

    /**
     * The duct is composed at render time from the part models; the block
     * state only names the thin centre so the dynamic model wrapper always
     * has a base model.
     */
    public static void blockStates(BlockStateProvider provider) {
        ModelFile base = DuctPartModels.generate(provider);
        provider.simpleBlock(ServusLogisticsBlocks.UTILITY_DUCT.get(), base);
        FluidTankModels.generate(provider);
    }

    public static void itemModels(ItemModelProvider provider) {
        provider.basicItem(ServusLogisticsItems.ITEM_PIPE.get());
        provider.basicItem(ServusLogisticsItems.POWER_CABLE.get());
        provider.basicItem(ServusLogisticsItems.FLUID_PIPE.get());
        provider.basicItem(ServusLogisticsItems.FACADE.get());
    }

    public static List<Block> pickaxeBlocks() {
        return List.of(ServusLogisticsBlocks.UTILITY_DUCT.get(), ServusLogisticsBlocks.FLUID_TANK.get());
    }

    public static void recipes(RecipeOutput output, Function<ItemLike, Criterion<InventoryChangeTrigger.TriggerInstance>> has) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.ITEM_PIPE.get(), 8)
                .pattern("III")
                .pattern("GCG")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('C', Items.CHEST)
                .unlockedBy("has_iron", has.apply(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.POWER_CABLE.get(), 8)
                .pattern("CCC")
                .pattern("RGR")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper", has.apply(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusLogisticsItems.FLUID_TANK.get())
                .pattern("IGI")
                .pattern("G G")
                .pattern("IGI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has.apply(Items.GLASS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.FLUID_PIPE.get(), 8)
                .pattern("III")
                .pattern("GBG")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has.apply(Items.BUCKET))
                .save(output);

        SpecialRecipeBuilder.special(FacadeRecipe::new).save(output, "servus_foundation:facade");
    }

    public static void language(LanguageProvider provider) {
        provider.add(ServusLogisticsBlocks.UTILITY_DUCT.get(), "Utility Duct");
        provider.add(ServusLogisticsItems.ITEM_PIPE.get(), "Item Pipe");
        provider.add(ServusLogisticsItems.POWER_CABLE.get(), "Power Cable");
        provider.add(ServusLogisticsItems.FLUID_PIPE.get(), "Fluid Pipe");
        provider.add(ServusLogisticsItems.FACADE.get(), "Facade");
        provider.add("item.servus_foundation.facade.named", "Facade (%s)");
        provider.add("item.servus_foundation.facade.hint", "Use on a utility duct to hide it");
        provider.add(ServusLogisticsBlocks.FLUID_TANK.get(), "Fluid Tank");
        provider.add("tooltip.servus_foundation.fluid_tank.empty", "Empty");
        provider.add("tooltip.servus_foundation.fluid_tank.fluid", "%s: %s mB");
        provider.add("message.servus_foundation.fluid_tank.empty", "Empty, capacity %s mB");
        provider.add("message.servus_foundation.fluid_tank.content", "%s: %s / %s mB");
        provider.add("message.servus_foundation.fluid_tank.mixed", "The tanks hold different fluids");
        provider.add("gui.servus_foundation.utility_duct", "Utility Duct");
        provider.add("gui.servus_foundation.utility_duct.title", "Utility Duct, %s: %s");
        provider.add("gui.servus_foundation.remove.item", "Remove Item Pipe");
        provider.add("gui.servus_foundation.remove.energy", "Remove Power Cable");
        provider.add("gui.servus_foundation.remove.fluid", "Remove Fluid Pipe");
        provider.add("gui.servus_foundation.filter", "Filter (empty = everything)");
        provider.add("gui.servus_foundation.priority", "Priority %s");
        provider.add("gui.servus_foundation.no_lines", "No lines");
        for (int channel = 1; channel <= 8; channel++) {
            provider.add("gui.servus_foundation.channel." + channel, "Channel " + channel);
        }
        provider.add("gui.servus_foundation.line.item", "Items");
        provider.add("gui.servus_foundation.line.energy", "Energy");
        provider.add("gui.servus_foundation.line.fluid", "Fluids");
        provider.add("gui.servus_foundation.mode.both", "In and out");
        provider.add("gui.servus_foundation.mode.auto", "In and out");
        provider.add("gui.servus_foundation.mode.input", "Pull in");
        provider.add("gui.servus_foundation.mode.output", "Push out");
        provider.add("gui.servus_foundation.mode.disabled", "Disabled");
        provider.add("gui.servus_foundation.side.down", "Down");
        provider.add("gui.servus_foundation.side.up", "Up");
        provider.add("gui.servus_foundation.side.north", "North");
        provider.add("gui.servus_foundation.side.south", "South");
        provider.add("gui.servus_foundation.side.west", "West");
        provider.add("gui.servus_foundation.side.east", "East");
    }
}
