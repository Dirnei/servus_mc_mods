package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import java.util.function.Function;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Datagen of the machines Foundation owns: models, loot, tags, lang and recipes. */
final class MachineDataParts {

    private MachineDataParts() {
    }

    static void blockStates(BlockStateProvider provider) {
        CoalGeneratorModels.coalGenerator(provider);
        MachineModels.enderAnchor(provider);
        BurnerPumpModels.burnerPump(provider);
        ElectricPumpModels.electricPump(provider);
        SinkModels.sink(provider);
        SinkModels.chimney(provider);
        SinkModels.sinkFan(provider);
        MillModels.mill(provider);
        DriveModels.handCrank(provider);
        DriveModels.electricMotor(provider);
    }

    /** Blocks that drop themselves and are mined with a pickaxe. */
    static List<Block> selfDropping() {
        return List.of(MachineContent.COAL_GENERATOR.get(), MachineContent.BURNER_PUMP.get(), MachineContent.ELECTRIC_PUMP.get(),
                MachineContent.SINK.get(), MachineContent.CHIMNEY.get(), MachineContent.ENDER_ANCHOR.get(), MachineContent.MILL.get(),
                MachineContent.HAND_CRANK.get(), MachineContent.ELECTRIC_MOTOR.get());
    }

    static void lang(LanguageProvider provider) {
        provider.add(MachineContent.COAL_GENERATOR.get(), "Coal Generator");
        provider.add(MachineContent.BURNER_PUMP.get(), "Burner Pump");
        provider.add(MachineContent.ELECTRIC_PUMP.get(), "Electric Pump");
        provider.add("gui.servus_foundation.pump.source_ok", "Infinite water source below");
        provider.add("gui.servus_foundation.pump.source_missing", "Needs an infinite water source below");
        provider.add(MachineContent.ENDER_ANCHOR.get(), "Ender Anchor");
        provider.add("gui.servus_foundation.ender_anchor.no_power", "Inactive: no power");
        provider.add("gui.servus_foundation.ender_anchor.team_limit", "Inactive: team limit reached");
        provider.add("gui.servus_foundation.ender_anchor.ftb_chunks", "Inactive: use FTB Chunks");
        provider.add("gui.servus_foundation.ender_anchor.no_owner", "Inactive: no owner");
        provider.add("gui.servus_foundation.ender_anchor.area", "Area");
        provider.add("gui.servus_foundation.ender_anchor.area.value", "%s x %s");
        provider.add("gui.servus_foundation.ender_anchor.upkeep", "Upkeep");
        provider.add("gui.servus_foundation.ender_anchor.upkeep.value", "%s FE/t");
        provider.add("gui.servus_foundation.ender_anchor.upkeep.tooltip", "Energy the area costs per tick: energy per chunk times its chunks");
        provider.add("gui.servus_foundation.ender_anchor.team", "Team");
        provider.add("gui.servus_foundation.ender_anchor.team.value", "%s / %s");
        provider.add("gui.servus_foundation.ender_anchor.team.tooltip", "Chunks your team keeps loaded, of the team cap");
        provider.add("gui.servus_foundation.ender_anchor.show_range", "Show range");
        provider.add("gui.servus_foundation.ender_anchor.show_range.tooltip", "Outline this anchor's area in the world, only for you");
        provider.add(MachineContent.ANCHOR_UPGRADE.get(), "Anchor Upgrade");
        provider.add("item.servus_foundation.anchor_upgrade.tooltip",
                "Goes into an Ender Anchor to widen its area by a ring of chunks, or into a Warehouse Controller so the site can wake for transfers");
        provider.add("servus_foundation.configuration.enderAnchor", "Ender Anchor");
        provider.add("servus_foundation.configuration.maxLoadedChunksPerTeam", "Max loaded chunks per team");
        provider.add("servus_foundation.configuration.enderAnchorRespectsFtbChunks", "Ender Anchor respects FTB Chunks");
        provider.add(MachineContent.SINK.get(), "Sink");
        provider.add(MachineContent.MILL.get(), "Mill");
        provider.add(MachineContent.HAND_CRANK.get(), "Hand Crank");
        provider.add(MachineContent.ELECTRIC_MOTOR.get(), "Electric Motor");
        provider.add("gui.servus_foundation.jei.milling", "Milling");
        provider.add("gui.servus_foundation.jei.milling.work", "%s work");
        provider.add(MachineContent.CHIMNEY.get(), "Chimney");
        provider.add("gui.servus_foundation.sink.chimney", "Chimney");
        provider.add("gui.servus_foundation.sink.height", "Chimney height: %s");
        provider.add("gui.servus_foundation.sink.items", "Items: %s");
        provider.add("gui.servus_foundation.sink.fluid", "Fluid: %s");
        provider.add("gui.servus_foundation.sink.energy", "Energy: %s");
        provider.add("gui.servus_foundation.sink.unit.items", "%s/s");
        provider.add("gui.servus_foundation.sink.unit.fluid", "%s mB/s");
        provider.add("gui.servus_foundation.sink.unit.energy", "%s FE/t");
        provider.add("gui.servus_foundation.sink.unit.items.long", "%s per second");
        provider.add("gui.servus_foundation.sink.unit.fluid.long", "%s mB per second");
        provider.add("gui.servus_foundation.sink.unit.energy.long", "%s FE per tick");
        provider.add("gui.servus_foundation.sink.unlimited", "Unlimited");
        provider.add("gui.servus_foundation.sink.all_unlimited", "Unlimited: every batch voids it all");
        provider.add("gui.servus_foundation.sink.taller", "A taller chimney voids faster");
        provider.add("gui.servus_foundation.sink.batch", "Next batch in %s s");
        provider.add("gui.servus_foundation.sink.batch.idle", "Voids once a second while it holds anything");
        provider.add("gui.servus_foundation.sink.locked", "Locked: add a Capacity Upgrade");
        provider.add("gui.servus_foundation.sink.tank", "%s / %s mB");
        provider.add("gui.servus_foundation.sink.tank.empty", "Empty");
        provider.add("gui.servus_foundation.sink.upgrade_slot", "Capacity Upgrade: opens one more row and tank");
        provider.add("gui.servus_foundation.sink.upgrade_refused", "Empty the last row and tank first");
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

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.MILL_ITEM.get())
                .pattern(" H ")
                .pattern("IGI")
                .pattern("III")
                .define('H', Items.HOPPER)
                .define('G', Items.GRINDSTONE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_grindstone", has.apply(Items.GRINDSTONE))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.HAND_CRANK_ITEM.get())
                .pattern("IS")
                .pattern("I ")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_iron_ingot", has.apply(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, MachineContent.ELECTRIC_MOTOR_ITEM.get())
                .pattern("ICI")
                .pattern("CRP")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('P', ServusLogisticsItems.POWER_CABLE.get())
                .unlockedBy("has_power_cable", has.apply(ServusLogisticsItems.POWER_CABLE.get()))
                .save(output);

        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "milling/sand")),
                new MillingRecipe(new SizedIngredient(Ingredient.of(Items.GRAVEL), 1), new ItemStackTemplate(Items.SAND), 200), null);
    }
}
