package io.github.dirnei.mc.servus.foundation.logistics.datagen;

import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.facade.HollowFacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankRecipe;
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
        provider.basicItem(ServusLogisticsItems.DATA_CABLE.get());
        provider.basicItem(ServusLogisticsItems.DUPLEX_DATA_CABLE.get());
        provider.basicItem(ServusLogisticsItems.FAST_DATA_CABLE.get());
        provider.basicItem(ServusLogisticsItems.FACADE.get());
        provider.basicItem(ServusLogisticsItems.HOLLOW_FACADE.get());
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


        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.FLUID_PIPE.get(), 8)
                .pattern("III")
                .pattern("GBG")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has.apply(Items.BUCKET))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.DATA_CABLE.get(), 4)
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper", has.apply(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.DUPLEX_DATA_CABLE.get(), 2)
                .pattern("DQD")
                .define('D', ServusLogisticsItems.DATA_CABLE.get())
                .define('Q', Items.QUARTZ)
                .unlockedBy("has_data_cable", has.apply(ServusLogisticsItems.DATA_CABLE.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.FAST_DATA_CABLE.get(), 2)
                .pattern(" G ")
                .pattern("DQD")
                .pattern(" G ")
                .define('D', ServusLogisticsItems.DUPLEX_DATA_CABLE.get())
                .define('G', Items.GOLD_INGOT)
                .define('Q', Items.QUARTZ)
                .unlockedBy("has_duplex_data_cable", has.apply(ServusLogisticsItems.DUPLEX_DATA_CABLE.get()))
                .save(output);

        SpecialRecipeBuilder.special(FacadeRecipe::new).save(output, "servus_foundation:facade");
        SpecialRecipeBuilder.special(HollowFacadeRecipe::new).save(output, "servus_foundation:hollow_facade");
        SpecialRecipeBuilder.special(FluidTankRecipe::new).save(output, "servus_foundation:fluid_tank");
    }

    public static void language(LanguageProvider provider) {
        provider.add(ServusLogisticsBlocks.UTILITY_DUCT.get(), "Utility Duct");
        provider.add(ServusLogisticsItems.ITEM_PIPE.get(), "Item Pipe");
        provider.add(ServusLogisticsItems.POWER_CABLE.get(), "Power Cable");
        provider.add(ServusLogisticsItems.FLUID_PIPE.get(), "Fluid Pipe");
        provider.add(ServusLogisticsItems.DATA_CABLE.get(), "Data Cable");
        provider.add(ServusLogisticsItems.DUPLEX_DATA_CABLE.get(), "Duplex Data Cable");
        provider.add(ServusLogisticsItems.FAST_DATA_CABLE.get(), "Fast Data Cable");
        provider.add("tooltip.servus_foundation.data_cable.simplex", "%s items/s, shared by both directions");
        provider.add("tooltip.servus_foundation.data_cable.duplex", "%s items/s in each direction");
        provider.add(ServusLogisticsItems.FACADE.get(), "Facade");
        provider.add("item.servus_foundation.facade.named", "Facade (%s)");
        provider.add("item.servus_foundation.facade.hint", "Use on a face of a utility duct to cover it");
        provider.add(ServusLogisticsItems.HOLLOW_FACADE.get(), "Hollow Facade");
        provider.add("item.servus_foundation.hollow_facade.named", "Hollow Facade (%s)");
        provider.add("jei.servus_foundation.facade.info", "Craft any full block without a block entity with one iron ingot to get six facades of that block. "
                + "Use a facade on a Utility Duct to hide it; sneak with the Configuration Carrot to take it off.");
        provider.add(ServusLogisticsBlocks.FLUID_TANK.get(), "Fluid Tank");
        provider.add("tooltip.servus_foundation.fluid_tank.empty", "Empty");
        provider.add("tooltip.servus_foundation.fluid_tank.fluid", "%s: %s mB");
        provider.add("tooltip.servus_foundation.fluid_tank.floor", "Floor: %s");
        provider.add("message.servus_foundation.fluid_tank.empty", "Empty, capacity %s mB");
        provider.add("message.servus_foundation.fluid_tank.content", "%s: %s / %s mB");
        provider.add("message.servus_foundation.fluid_tank.mixed", "The tanks hold different fluids");
        provider.add("gui.servus_foundation.utility_duct", "Utility Duct");
        provider.add("gui.servus_foundation.utility_duct.title", "Utility Duct, %s: %s");
        provider.add("gui.servus_foundation.remove.item", "Remove Item Pipe");
        provider.add("gui.servus_foundation.remove.energy", "Remove Power Cable");
        provider.add("gui.servus_foundation.remove.fluid", "Remove Fluid Pipe");
        provider.add("gui.servus_foundation.remove.data", "Remove Data Cable");
        provider.add("gui.servus_foundation.data.on", "Data: on");
        provider.add("gui.servus_foundation.data.off", "Data: off");
        provider.add("gui.servus_foundation.filter", "Filter (empty = everything)");
        provider.add("gui.servus_foundation.priority", "Priority %s");
        provider.add("gui.servus_foundation.no_lines", "No lines");
        for (int channel = 1; channel <= 8; channel++) {
            provider.add("gui.servus_foundation.channel." + channel, "Channel " + channel);
        }
        provider.add("gui.servus_foundation.line.item", "Items");
        provider.add("gui.servus_foundation.line.energy", "Energy");
        provider.add("gui.servus_foundation.line.fluid", "Fluids");
        provider.add("gui.servus_foundation.line.data", "Data");
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
