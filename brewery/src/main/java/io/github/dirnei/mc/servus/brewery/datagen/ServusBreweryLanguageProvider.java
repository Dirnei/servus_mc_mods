package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.BreweryFluids;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusBreweryLanguageProvider extends LanguageProvider {

    ServusBreweryLanguageProvider(PackOutput output) {
        super(output, ServusBreweryIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ServusBreweryIds.MOD_ID, "Servus: Brewery");
        add(ServusBreweryItems.BEER_GLASS.get(), "Beer Glass");
        add(ServusBreweryItems.WEISSBIER_GLASS.get(), "Weissbier Glass");
        add(ServusBreweryItems.HELLES.get(), "Glass of Helles");
        add(ServusBreweryItems.DUNKLES.get(), "Glass of Dunkles");
        add(ServusBreweryItems.WEISSBIER.get(), "Glass of Weissbier");
        add(ServusBreweryItems.YEAST.get(), "Yeast");
        add(ServusBreweryBlocks.BARLEY.get(), "Barley");
        add(ServusBreweryItems.BARLEY.get(), "Barley");
        add(ServusBreweryItems.BARLEY_SEEDS.get(), "Barley Seeds");
        add(ServusBreweryItems.HOPS.get(), "Hops");
        add(ServusBreweryItems.HOP_RHIZOME.get(), "Hop Rhizome");
        add(ServusBreweryBlocks.HOP_POLE.get(), "Hop Pole");
        add(ServusBreweryBlocks.MALTING_FLOOR.get(), "Malting Floor");
        add(ServusBreweryItems.SOAKED_BARLEY.get(), "Soaked Barley");
        add(ServusBreweryItems.SOAKED_WHEAT.get(), "Soaked Wheat");
        add(ServusBreweryItems.GREEN_MALT.get(), "Green Malt");
        add(ServusBreweryItems.GREEN_WHEAT_MALT.get(), "Green Wheat Malt");
        add(ServusBreweryItems.PALE_MALT.get(), "Pale Malt");
        add(ServusBreweryItems.DARK_MALT.get(), "Dark Malt");
        add(ServusBreweryItems.WHEAT_MALT.get(), "Wheat Malt");
        add(ServusBreweryItems.PALE_MALT_GRIST.get(), "Pale Malt Grist");
        add(ServusBreweryItems.DARK_MALT_GRIST.get(), "Dark Malt Grist");
        add(ServusBreweryItems.WHEAT_MALT_GRIST.get(), "Wheat Malt Grist");
        add(ServusBreweryBlocks.BREWHOUSE_BRICKS.get(), "Brewhouse Bricks");
        add(ServusBreweryBlocks.BREW_KETTLE.get(), "Brew Kettle");
        add(ServusBreweryBlocks.FERMENTING_VAT.get(), "Fermenting Vat");
        add(ServusBreweryBlocks.CASK.get(), "Cask");
        fluid(BreweryFluids.PALE_WORT, "Pale Wort");
        fluid(BreweryFluids.DARK_WORT, "Dark Wort");
        fluid(BreweryFluids.WHEAT_WORT, "Wheat Wort");
        fluid(BreweryFluids.HELLES, "Helles");
        fluid(BreweryFluids.DUNKLES, "Dunkles");
        fluid(BreweryFluids.WEISSBIER, "Weissbier");
        add("message.servus_brewery.brewhouse.formed", "Brewhouse formed");
        add("message.servus_brewery.brewhouse.layout",
                "A Brewhouse is six Brewhouse Bricks, three wide and two deep, with four Brew Kettle blocks on the left two by two");
        add("message.servus_brewery.brewhouse.loose", "Use the Configuration Carrot to form a Brewhouse");
        add("message.servus_brewery.brewhouse.lit", "The fire burns, %s %s left");
        add("message.servus_brewery.brewhouse.cold", "The fire is out, %s %s ready");
        add("message.servus_brewery.brewhouse.lit.no_fuel", "The fire burns, no fuel left");
        add("message.servus_brewery.brewhouse.cold.no_fuel", "The fire is out, no fuel");
        add("message.servus_brewery.kettle.brewing", "Brewing: %s mB water, %s mB %s");
        add("message.servus_brewery.kettle.idle", "Kettle: %s mB water, %s mB %s");
        add("message.servus_brewery.kettle.no_wort", "wort");
        add("message.servus_brewery.kiln.mode.pale", "Kiln tray: pale malt");
        add("message.servus_brewery.kiln.mode.dark", "Kiln tray: dark malt, kept longer on the fire");
        add("message.servus_brewery.vat.empty", "The vat is empty");
        add("message.servus_brewery.vat.wort", "%s mB %s, waiting for yeast");
        add("message.servus_brewery.vat.fermenting", "Fermenting %s mB %s: %s%%");
        add("message.servus_brewery.vat.beer", "%s mB %s, ready");
        add("message.servus_brewery.cask.empty", "The cask is empty");
        add("message.servus_brewery.cask.status", "%s mB %s");
        add("message.servus_brewery.cask.wrong_glass", "%s does not go into this glass");
        add("message.servus_brewery.cask.too_little", "Only %s mB %s left, not enough for a glass");
        add("gui.servus_brewery.jei.brewing", "Brewing");
        add("gui.servus_brewery.jei.fermenting", "Fermenting");
        add("gui.servus_brewery.jei.fermenting_time", "%s days in game");
        add("gui.servus_brewery.jei.yeast_stays", "the yeast stays");
        add("gui.servus_brewery.jei.kilning", "Kilning");
        add("gui.servus_brewery.jei.hot_brewhouse", "over a burning Brewhouse");
        add("gui.servus_brewery.jei.dark", "Dark mode: %s, %s s more");
    }

    private void fluid(BreweryFluids.BrewFluid fluid, String name) {
        add("fluid_type." + ServusBreweryIds.MOD_ID + "." + fluid.id(), name);
        add(fluid.block().get(), name);
        add(fluid.bucket().get(), name + " Bucket");
    }
}
