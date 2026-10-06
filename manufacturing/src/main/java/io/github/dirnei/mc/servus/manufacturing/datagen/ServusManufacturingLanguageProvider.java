package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusManufacturingLanguageProvider extends LanguageProvider {

    ServusManufacturingLanguageProvider(PackOutput output) {
        super(output, ServusManufacturingIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusManufacturingBlocks.IRON_DEPOSIT.get(), "Iron Deposit");
        add(ServusManufacturingBlocks.COPPER_DEPOSIT.get(), "Copper Deposit");
        add(ServusManufacturingBlocks.COAL_DEPOSIT.get(), "Coal Deposit");
        add(ServusManufacturingBlocks.STONE_DEPOSIT.get(), "Stone Deposit");
        add(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), "Burner Extractor");
        add(ServusManufacturingBlocks.BURNER_SMELTER.get(), "Burner Smelter");
        add(ServusManufacturingBlocks.COAL_GENERATOR.get(), "Coal Generator");
        add(ServusManufacturingBlocks.POWER_CABLE.get(), "Power Cable");
        add(ServusManufacturingBlocks.ASSEMBLER.get(), "Assembler Mk1");
        add(ServusManufacturingBlocks.ITEM_DUCT.get(), "Item Duct");
        add(ServusManufacturingBlocks.SPLITTER_DUCT.get(), "Splitter Duct");
        add(ServusManufacturingBlocks.FILTER_DUCT.get(), "Filter Duct");
        add(ServusManufacturingItems.IRON_GEAR.get(), "Iron Gear");
        add(ServusManufacturingItems.COPPER_WIRE.get(), "Copper Wire");
        add(ServusManufacturingItems.RED_PACK.get(), "Red Pack");
        add(ServusManufacturingBlocks.RESEARCH_LAB.get(), "Research Lab");
        add(ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get(), "Electric Extractor");
        add(ServusManufacturingBlocks.CRUSHER.get(), "Crusher");
        add(ServusManufacturingBlocks.CHUNK_LOADER.get(), "Chunk Loader");
        add("gui.servus_manufacturing.chunk_loader.no_power", "Inactive: no power");
        add("gui.servus_manufacturing.chunk_loader.active", "Active: chunk kept loaded");
        add("gui.servus_manufacturing.chunk_loader.team_limit", "Inactive: team limit reached");
        add("gui.servus_manufacturing.chunk_loader.ftb_chunks", "Inactive: use FTB Chunks");
        add("gui.servus_manufacturing.chunk_loader.no_owner", "Inactive: no owner");
        add("servus_manufacturing.configuration.chunkLoader", "Chunk Loader");
        add("servus_manufacturing.configuration.maxChunkLoadersPerTeam", "Max chunk loaders per team");
        add("servus_manufacturing.configuration.chunkLoaderRespectsFtbChunks", "Chunk loader respects FTB Chunks");
        add(ServusManufacturingItems.IRON_GRIT.get(), "Iron Grit");
        add(ServusManufacturingItems.COPPER_GRIT.get(), "Copper Grit");
        add("gui.servus_manufacturing.jei.crushing", "Crushing");

        add("gui.servus_manufacturing.tech_tree", "Tech Tree");
        add("gui.servus_manufacturing.jei.assembling", "Assembling");
        add("gui.servus_manufacturing.assembler.recipes", "Recipes");
        add("gui.servus_manufacturing.assembler.add", "Add a recipe");
        add("gui.servus_manufacturing.assembler.edit", "Click to edit or delete");
        add("gui.servus_manufacturing.assembler.empty_slot", "Empty recipe slot. Click to add a recipe, or use the JEI + button.");
        add("gui.servus_manufacturing.assembler.new_recipe", "New recipe");
        add("gui.servus_manufacturing.assembler.edit_recipe", "Recipe %s");
        add("gui.servus_manufacturing.assembler.save", "Save");
        add("gui.servus_manufacturing.assembler.delete", "Delete");
        add("gui.servus_manufacturing.assembler.cancel", "Cancel");
        add("gui.servus_manufacturing.assembler.no_match", "No recipe matches this layout");
        add("message.servus_manufacturing.assembler.unknown", "Unknown recipe");
        add("message.servus_manufacturing.assembler.unsupported", "The assembler cannot make this kind of recipe");
        add("message.servus_manufacturing.assembler.empty_pattern", "Lay out a recipe in the pattern grid first");
        add("message.servus_manufacturing.assembler.no_match", "No crafting or assembling recipe matches the pattern");
        add("message.servus_manufacturing.assembler.duplicate", "This recipe is already configured");
        add("message.servus_manufacturing.assembler.full", "All recipe slots of this assembler are in use");
        add("message.servus_manufacturing.assembler.locked", "This recipe needs research first");
        add("gui.servus_manufacturing.side.front", "Front");
        add("gui.servus_manufacturing.side.back", "Back");
        add("gui.servus_manufacturing.side.left", "Left");
        add("gui.servus_manufacturing.side.right", "Right");
        add("gui.servus_manufacturing.side.top", "Top");
        add("gui.servus_manufacturing.side.bottom", "Bottom");
        add("gui.servus_manufacturing.side_mode.auto", "Auto");
        add("gui.servus_manufacturing.side_mode.input", "Input");
        add("gui.servus_manufacturing.side_mode.output", "Output");
        add("gui.servus_manufacturing.side_mode.both", "Input and Output");
        add("gui.servus_manufacturing.side_mode.disabled", "Disabled");
        add("gui.servus_manufacturing.no_research", "No research selected");
        add("gui.servus_manufacturing.locked", "Locked: requires research");
        add("gui.servus_manufacturing.research.completed", "Completed");
        add("gui.servus_manufacturing.research.researching", "Researching %s / %s");
        add("gui.servus_manufacturing.research.available", "Available");
        add("gui.servus_manufacturing.research.locked", "Locked");

        add("tech.servus_manufacturing.electric_extractor", "Electric Extractor");
        add("tech.servus_manufacturing.crusher", "Crusher");
        add("tech.servus_manufacturing.stone_processing", "Stone Processing");
        add("tech.servus_manufacturing.splitter_filter_ducts", "Splitter and Filter Ducts");
        add("tech.servus_manufacturing.logistics", "Logistics");
    }
}
