package io.github.dirnei.mc.servus.foundation.registry;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The shared "Servus" creative tab. Content mods add their items by handling
 * {@code BuildCreativeModeTabContentsEvent} and comparing {@link #SERVUS_KEY}.
 */
public final class ServusFoundationCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ServusFoundationIds.MOD_ID);

    public static final ResourceKey<CreativeModeTab> SERVUS_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "servus"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SERVUS = TABS.register("servus", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.servus"))
                    .icon(() -> new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ServusFoundationItems.CONFIGURATION_CARROT.get());
                        output.accept(ServusFoundationItems.GOGGLES.get());
                        output.accept(ServusFoundationItems.SPEED_UPGRADE.get());
                    })
                    .build());

    private ServusFoundationCreativeTabs() {
    }
}
