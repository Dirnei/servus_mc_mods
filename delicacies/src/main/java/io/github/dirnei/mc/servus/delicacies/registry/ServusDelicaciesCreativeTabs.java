package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The "Servus: Delicacies" creative tab, placed right after the shared Servus tab. */
public final class ServusDelicaciesCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ServusDelicaciesIds.MOD_ID);

    public static final ResourceKey<CreativeModeTab> DELICACIES_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "delicacies"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DELICACIES = TABS.register("delicacies", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ServusDelicaciesIds.MOD_ID))
                    .icon(() -> new ItemStack(ServusDelicaciesItems.PRETZEL.get()))
                    .withTabsBefore(ServusFoundationCreativeTabs.SERVUS_KEY)
                    .displayItems((parameters, output) ->
                            ServusDelicaciesItems.CREATIVE_TAB_ORDER.forEach(item -> output.accept(item.get())))
                    .build());

    private ServusDelicaciesCreativeTabs() {
    }
}
