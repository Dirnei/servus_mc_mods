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
        add(ServusManufacturingItems.IRON_GEAR.get(), "Iron Gear");
        add(ServusManufacturingItems.COPPER_WIRE.get(), "Copper Wire");
        add(ServusManufacturingItems.RED_PACK.get(), "Red Pack");
        add(ServusManufacturingBlocks.RESEARCH_LAB.get(), "Research Lab");

        add("gui.servus_manufacturing.tech_tree", "Tech Tree");
        add("gui.servus_manufacturing.jei.assembling", "Assembling");
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
