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
        add(ServusManufacturingBlocks.IRON_DEPOSIT.get(), "Iron Deposit");
        add(ServusManufacturingBlocks.COPPER_DEPOSIT.get(), "Copper Deposit");
        add(ServusManufacturingBlocks.COAL_DEPOSIT.get(), "Coal Deposit");
        add(ServusManufacturingBlocks.STONE_DEPOSIT.get(), "Stone Deposit");
        add(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), "Burner Extractor");
        add(ServusManufacturingBlocks.BURNER_SMELTER.get(), "Burner Smelter");
        add(ServusManufacturingBlocks.COAL_GENERATOR.get(), "Coal Generator");
        add(ServusManufacturingBlocks.POWER_CABLE.get(), "Power Cable");
    }
}
