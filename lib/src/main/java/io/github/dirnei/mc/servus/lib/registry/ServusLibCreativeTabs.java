package io.github.dirnei.mc.servus.lib.registry;

import io.github.dirnei.mc.servus.lib.ServusLibIds;
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
public final class ServusLibCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ServusLibIds.MOD_ID);

    public static final ResourceKey<CreativeModeTab> SERVUS_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(ServusLibIds.MOD_ID, "servus"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SERVUS = TABS.register("servus", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.servus"))
                    .icon(() -> new ItemStack(ServusLibItems.WRENCH.get()))
                    .displayItems((parameters, output) -> output.accept(ServusLibItems.WRENCH.get()))
                    .build());

    private ServusLibCreativeTabs() {
    }
}
