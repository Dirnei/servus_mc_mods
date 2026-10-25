package io.github.dirnei.mc.servus.foundation.logistics.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.facade.HollowFacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankRecipe;
import java.util.List;
import java.util.function.Function;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
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

    public static void recipes(RecipeOutput output, HolderGetter<Item> items, Function<ItemLike, Criterion<InventoryChangeTrigger.TriggerInstance>> has) {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, ServusLogisticsItems.ITEM_PIPE.get(), 8)
                .pattern("CIC")
                .pattern("GHG")
                .pattern("CIC")
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('H', Items.CHEST)
                .unlockedBy("has_iron", has.apply(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, ServusLogisticsItems.POWER_CABLE.get(), 8)
                .pattern("CCC")
                .pattern("RGR")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper", has.apply(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, ServusLogisticsItems.FLUID_PIPE.get(), 8)
                .pattern("CIC")
                .pattern("GBG")
                .pattern("CIC")
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has.apply(Items.BUCKET))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, ServusLogisticsItems.DATA_CABLE.get(), 4)
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper", has.apply(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, ServusLogisticsItems.DUPLEX_DATA_CABLE.get(), 2)
                .pattern("DQD")
                .define('D', ServusLogisticsItems.DATA_CABLE.get())
                .define('Q', Items.QUARTZ)
                .unlockedBy("has_data_cable", has.apply(ServusLogisticsItems.DATA_CABLE.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, ServusLogisticsItems.FAST_DATA_CABLE.get(), 2)
                .pattern(" G ")
                .pattern("DQD")
                .pattern(" G ")
                .define('D', ServusLogisticsItems.DUPLEX_DATA_CABLE.get())
                .define('G', Items.GOLD_INGOT)
                .define('Q', Items.QUARTZ)
                .unlockedBy("has_duplex_data_cable", has.apply(ServusLogisticsItems.DUPLEX_DATA_CABLE.get()))
                .save(output);

        SpecialRecipeBuilder.special(() -> FacadeRecipe.INSTANCE).save(output, "servus_foundation:facade");
        SpecialRecipeBuilder.special(() -> HollowFacadeRecipe.INSTANCE).save(output, "servus_foundation:hollow_facade");
        SpecialRecipeBuilder.special(() -> FluidTankRecipe.INSTANCE).save(output, "servus_foundation:fluid_tank");
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
        provider.add("jei.servus_foundation.facade.info", "Craft any full block without a block entity with one nugget, iron, gold or any other, to get six facades of that block. "
                + "Eight facades of one block around an empty centre give eight hollow facades. "
                + "Use a facade on a face of a Utility Duct to cover that face: a solid facade also closes the face, "
                + "so nothing connects through it, while a hollow facade lets the duct pass through its hole. "
                + "Sneak with the Configuration Carrot on a facade to take it off.");
        provider.add(ServusLogisticsBlocks.FLUID_TANK.get(), "Fluid Tank");
        provider.add("tooltip.servus_foundation.fluid_tank.empty", "Empty");
        provider.add("tooltip.servus_foundation.fluid_tank.fluid", "%s: %s mB");
        provider.add("tooltip.servus_foundation.fluid_tank.floor", "Floor: %s");
        provider.add("message.servus_foundation.fluid_tank.empty", "Empty, capacity %s mB");
        provider.add("message.servus_foundation.fluid_tank.content", "%s: %s / %s mB");
        provider.add("message.servus_foundation.fluid_tank.mixed", "The tanks hold different fluids");
        provider.add("gui.servus_foundation.utility_duct", "Utility Duct");
        provider.add("gui.servus_foundation.utility_duct.side", "%s: %s");
        provider.add("gui.servus_foundation.side_picker.duct", "Another duct, configure it there");
        provider.add("gui.servus_foundation.side_picker.empty", "Nothing here");
        provider.add("gui.servus_foundation.control", "Control: %s");
        provider.add("gui.servus_foundation.control.always", "Always");
        provider.add("gui.servus_foundation.control.powered", "While powered");
        provider.add("gui.servus_foundation.control.unpowered", "While unpowered");
        provider.add("gui.servus_foundation.control.off", "Off");
        provider.add("gui.servus_foundation.control.tooltip", "When this line moves anything on this side. "
                + "While powered and While unpowered follow a redstone signal into the duct. "
                + "Off keeps the connection and only stops the movement.");
        provider.add("gui.servus_foundation.remove.item", "Remove Item Pipe");
        provider.add("gui.servus_foundation.remove.energy", "Remove Power Cable");
        provider.add("gui.servus_foundation.remove.fluid", "Remove Fluid Pipe");
        provider.add("gui.servus_foundation.remove.data", "Remove Data Cable");
        provider.add("gui.servus_foundation.data.on", "Data: on");
        provider.add("gui.servus_foundation.data.off", "Data: off");
        provider.add("gui.servus_foundation.filter", "Filter (empty = everything)");
        provider.add("gui.servus_foundation.filter_picker", "Filter item");
        provider.add("gui.servus_foundation.filter_none", "None, clear the slot");
        provider.add("gui.servus_foundation.priority", "Priority %s");
        provider.add("gui.servus_foundation.no_lines", "No lines");
        provider.add("gui.servus_foundation.channel.default", "Default");
        provider.add("gui.servus_foundation.channel.button", "Channel: %s");
        provider.add("gui.servus_foundation.channel.popup", "Channel, %s");
        provider.add("gui.servus_foundation.channel.this_cable", "This cable");
        provider.add("gui.servus_foundation.channel.all", "All");
        provider.add("gui.servus_foundation.channel.add", "+ New");
        provider.add("gui.servus_foundation.channel.new", "New channel");
        provider.add("gui.servus_foundation.channel.edit", "Edit channel");
        provider.add("gui.servus_foundation.channel.delete", "Delete channel");
        provider.add("gui.servus_foundation.channel.in_use", "Used on %s loaded sides");
        provider.add("gui.servus_foundation.channel.name", "Name");
        provider.add("gui.servus_foundation.channel.icon", "Icon: %s");
        provider.add("gui.servus_foundation.channel.no_icon", "No icon");
        provider.add("gui.servus_foundation.channel.icon_picker", "Channel icon");
        provider.add("gui.servus_foundation.channel.save", "Save");
        provider.add("gui.servus_foundation.channel.cancel", "Cancel");
        provider.add("gui.servus_foundation.channel.problem.empty", "Enter a name");
        provider.add("gui.servus_foundation.channel.problem.too_long", "At most %s characters");
        provider.add("gui.servus_foundation.channel.problem.taken", "Your team already has this name");
        provider.add("gui.servus_foundation.line.item", "Items");
        provider.add("gui.servus_foundation.line.energy", "Energy");
        provider.add("gui.servus_foundation.line.fluid", "Fluids");
        provider.add("gui.servus_foundation.line.data", "Data");
        provider.add("gui.servus_foundation.mode.both", "In and out");
        provider.add("gui.servus_foundation.mode.auto", "In and out");
        provider.add("gui.servus_foundation.mode.input", "Pull in");
        provider.add("gui.servus_foundation.mode.output", "Push out");
        provider.add("gui.servus_foundation.mode.disabled", "Disabled");
        provider.add("message.servus_foundation.potato.duct_side", "Utility Duct, %s side (loaded ducts only)");
        provider.add("message.servus_foundation.potato.duct_centre", "Utility Duct, centre, %s (loaded ducts only)");
        provider.add("message.servus_foundation.potato.powered", "powered");
        provider.add("message.servus_foundation.potato.unpowered", "unpowered");
        provider.add("message.servus_foundation.potato.paused", "%s, paused");
        provider.add("message.servus_foundation.potato.item_side", "%s: %s, %s, %s, priority %s, %s ducts, %s in / %s out");
        provider.add("message.servus_foundation.potato.side", "%s: %s, %s, %s, %s ducts, %s in / %s out");
        provider.add("message.servus_foundation.potato.run", "%s: %s ducts");
        provider.add("message.servus_foundation.potato.data", "%s: %s, %s items/s, %s used");
        provider.add("message.servus_foundation.potato.tank_size", "Size: %s x %s x %s blocks");
        provider.add("gui.servus_foundation.side.down", "Down");
        provider.add("gui.servus_foundation.side.up", "Up");
        provider.add("gui.servus_foundation.side.north", "North");
        provider.add("gui.servus_foundation.side.south", "South");
        provider.add("gui.servus_foundation.side.west", "West");
        provider.add("gui.servus_foundation.side.east", "East");
    }
}
