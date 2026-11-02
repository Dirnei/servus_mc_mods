package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.search.SearchFacts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;

/**
 * The {@link SearchFacts} of one item stack on the client: name, id, mod id
 * and name, item tags, tooltip lines below the name and the creative tabs
 * that hold it. Each fact is built on first use and kept. Client thread only.
 */
public final class ItemSearchFacts implements SearchFacts {

    private final ItemStack stack;
    private List<String> names;
    private List<String> ids;
    private List<String> mods;
    private List<String> tags;
    private List<String> tooltip;
    private List<String> tabs;

    private ItemSearchFacts(ItemStack stack) {
        this.stack = stack;
    }

    /** Facts of a copy of {@code stack}, so later changes to the stack do not leak in. */
    public static ItemSearchFacts of(ItemStack stack) {
        return new ItemSearchFacts(stack.copyWithCount(1));
    }

    /** A new cache of facts by item and components, for one screen while it is open. */
    public static Cache cache() {
        return new Cache();
    }

    @Override
    public List<String> names() {
        if (names == null) {
            names = List.of(safeString(stack::getHoverName));
        }
        return names;
    }

    @Override
    public List<String> ids() {
        if (ids == null) {
            ids = List.of(id().toString());
        }
        return ids;
    }

    @Override
    public List<String> mods() {
        if (mods == null) {
            mods = ModNames.of(id().getNamespace());
        }
        return mods;
    }

    @Override
    public List<String> tags() {
        if (tags == null) {
            tags = stack.typeHolder().tags().map(tag -> tag.location().toString()).toList();
        }
        return tags;
    }

    @Override
    public List<String> tooltip() {
        if (tooltip == null) {
            tooltip = buildTooltip();
        }
        return tooltip;
    }

    @Override
    public List<String> tabs() {
        if (tabs == null) {
            tabs = buildTabs();
        }
        return tabs;
    }

    private Identifier id() {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private List<String> buildTooltip() {
        Minecraft minecraft = Minecraft.getInstance();
        List<Component> lines;
        try {
            lines = stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL);
        } catch (RuntimeException e) {
            // Some mods' tooltips expect their own screen or world state and throw elsewhere.
            return List.of();
        }
        List<String> text = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            text.add(lines.get(i).getString());
        }
        return text;
    }

    private List<String> buildTabs() {
        if (!CreativeTabContents.ensureBuilt()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() == CreativeModeTab.Type.CATEGORY && tab.contains(stack)) {
                names.add(tab.getDisplayName().getString());
            }
        }
        return names;
    }

    /** The text's string, or empty when a mod's name or text throws. */
    static String safeString(Supplier<Component> text) {
        try {
            return text.get().getString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Facts kept by item and components; same item, same components, same facts. */
    public static final class Cache {

        private static final int LIMIT = 8192;

        private final Map<Key, ItemSearchFacts> byStack = new HashMap<>();

        private Cache() {
        }

        /** The facts of {@code stack}, built once per item and components. */
        public ItemSearchFacts get(ItemStack stack) {
            if (byStack.size() >= LIMIT) {
                byStack.clear();
            }
            return byStack.computeIfAbsent(new Key(stack.copyWithCount(1)), key -> new ItemSearchFacts(key.stack()));
        }

        private record Key(ItemStack stack) {

            @Override
            public boolean equals(Object other) {
                return other instanceof Key key && ItemStack.isSameItemSameComponents(stack, key.stack);
            }

            @Override
            public int hashCode() {
                return ItemStack.hashItemAndComponents(stack);
            }
        }
    }

    /** Fills the creative tabs the way the creative screen does, so {@code contains} works before it was ever opened. */
    private static final class CreativeTabContents {

        private CreativeTabContents() {
        }

        /** Whether the tabs are built for the current world; rebuilds them when the world's parameters changed. */
        static boolean ensureBuilt() {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null) {
                return false;
            }
            HolderLookup.Provider registries = player.level().registryAccess();
            boolean operator = player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get();
            if (CreativeModeTabs.tryRebuildTabContents(player.connection.enabledFeatures(), operator, registries)) {
                // The creative screen refreshes its search trees only when it rebuilt the tabs itself.
                refreshSearchTrees(player.connection.searchTrees(), registries);
            }
            return true;
        }

        private static void refreshSearchTrees(SessionSearchTrees searchTrees, HolderLookup.Provider registries) {
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                if (tab.hasSearchBar()) {
                    List<ItemStack> items = List.copyOf(tab.getDisplayItems());
                    searchTrees.updateCreativeTooltips(registries, items, CreativeModeTabSearchRegistry.getNameSearchKey(tab));
                    searchTrees.updateCreativeTags(items, CreativeModeTabSearchRegistry.getTagSearchKey(tab));
                }
            }
        }
    }
}
