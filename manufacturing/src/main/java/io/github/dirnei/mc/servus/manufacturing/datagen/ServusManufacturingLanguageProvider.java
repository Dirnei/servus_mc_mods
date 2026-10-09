package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusManufacturingLanguageProvider extends LanguageProvider {

    ServusManufacturingLanguageProvider(PackOutput output) {
        super(output, ServusManufacturingIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusManufacturingBlocks.RESEARCH_LAB.get(), "Research Lab");
        add("gui.servus_manufacturing.tech_tree", "Tech Tree");
        add("gui.servus_manufacturing.no_research", "No research selected");
        add("gui.servus_manufacturing.locked", "Locked: requires research");
        add("gui.servus_manufacturing.research.completed", "Completed");
        add("gui.servus_manufacturing.research.researching", "Researching %s / %s");
        add("gui.servus_manufacturing.research.available", "Available");
        add("gui.servus_manufacturing.research.locked", "Locked");
    }
}
