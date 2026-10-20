package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesCreativeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The "Servus: Brewery" creative tab, placed right after the Delicacies tab. */
public final class ServusBreweryCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ServusBreweryIds.MOD_ID);

    public static final ResourceKey<CreativeModeTab> BREWERY_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "brewery"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BREWERY = TABS.register("brewery", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ServusBreweryIds.MOD_ID))
                    .icon(() -> new ItemStack(ServusBreweryItems.BEER_GLASS.get()))
                    .withTabsBefore(ServusDelicaciesCreativeTabs.DELICACIES_KEY)
                    .displayItems((parameters, output) ->
                            ServusBreweryItems.CREATIVE_TAB_ORDER.forEach(item -> output.accept(item.get())))
                    .build());

    private ServusBreweryCreativeTabs() {
    }
}
