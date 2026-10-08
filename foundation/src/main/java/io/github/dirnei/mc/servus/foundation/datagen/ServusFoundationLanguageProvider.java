package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationBlocks;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusFoundationLanguageProvider extends LanguageProvider {

    ServusFoundationLanguageProvider(PackOutput output) {
        super(output, ServusFoundationIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusFoundationItems.CONFIGURATION_CARROT.get(), "Configuration Carrot");
        add(ServusFoundationItems.GOGGLES.get(), "Engineer Goggles");
        add(ServusFoundationBlocks.DATA_CABLE.get(), "Data Cable");
        add("message.servus_foundation.data_cable.summary", "Data network: %s cables, %s producers, %s stock");
        add("itemGroup.servus", "Servus");
        add("servus_foundation.configuration.wrench", "Configuration Carrot");
        add("servus_foundation.configuration.wrenchRotatesBlocks", "Configuration Carrot rotates blocks");
        add("gui.servus_foundation.side.front", "Front");
        add("gui.servus_foundation.side.back", "Back");
        add("gui.servus_foundation.side.left", "Left");
        add("gui.servus_foundation.side.right", "Right");
        add("gui.servus_foundation.side.top", "Top");
        add("gui.servus_foundation.side.bottom", "Bottom");
        add("gui.servus_foundation.side_mode.auto", "Auto");
        add("gui.servus_foundation.side_mode.input", "Input");
        add("gui.servus_foundation.side_mode.output", "Output");
        add("gui.servus_foundation.side_mode.both", "Input and Output");
        add("gui.servus_foundation.side_mode.pull", "Pull");
        add("gui.servus_foundation.side_mode.push", "Push");
        add("gui.servus_foundation.side_mode.off", "Off");
        add("gui.servus_foundation.side_line", "%s: %s");
        add("gui.servus_foundation.side_fixed", "(fixed)");
        add("gui.servus_foundation.fluid_gauge.empty", "Empty");
    }
}
