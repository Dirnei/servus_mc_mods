package io.github.dirnei.mc.servus.manufacturing.research;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Lookups over the tech registry shared by server and client. */
public final class Techs {

    public static final ResourceKey<Registry<TechDefinition>> REGISTRY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "tech"));

    /** A tech with its id. */
    public record Entry(ResourceLocation id, TechDefinition definition) {

        public TechNode node() {
            Map<String, Integer> cost = new HashMap<>();
            for (ItemStack stack : definition.cost()) {
                cost.merge(itemId(stack), stack.getCount(), Integer::sum);
            }
            List<String> parents = definition.parents().stream().map(ResourceLocation::toString).toList();
            return new TechNode(id.toString(), parents, cost);
        }

        public Component name() {
            return Component.translatable("tech." + id.getNamespace() + "." + id.getPath());
        }
    }

    private Techs() {
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static Registry<TechDefinition> registry(RegistryAccess access) {
        return access.registryOrThrow(REGISTRY);
    }

    public static Optional<Entry> get(RegistryAccess access, ResourceLocation id) {
        Registry<TechDefinition> registry = registry(access);
        TechDefinition definition = registry.get(id);
        return definition == null ? Optional.empty() : Optional.of(new Entry(id, definition));
    }

    /** All techs, roots first (sorted by depth, then id). */
    public static List<Entry> all(RegistryAccess access) {
        Registry<TechDefinition> registry = registry(access);
        List<Entry> entries = new ArrayList<>();
        registry.entrySet().forEach(e -> entries.add(new Entry(e.getKey().location(), e.getValue())));
        Map<ResourceLocation, Integer> depth = new HashMap<>();
        for (Entry entry : entries) {
            depthOf(entry.id(), registry, depth, 0);
        }
        entries.sort(Comparator.<Entry>comparingInt(e -> depth.getOrDefault(e.id(), 0))
                .thenComparing(e -> e.id().toString()));
        return entries;
    }

    private static int depthOf(ResourceLocation id, Registry<TechDefinition> registry, Map<ResourceLocation, Integer> depth, int guard) {
        Integer known = depth.get(id);
        if (known != null) {
            return known;
        }
        TechDefinition definition = registry.get(id);
        int result = 0;
        if (definition != null && guard < 64) {
            for (ResourceLocation parent : definition.parents()) {
                result = Math.max(result, depthOf(parent, registry, depth, guard + 1) + 1);
            }
        }
        depth.put(id, result);
        return result;
    }

    /** Whether the stack is a pack used by any tech. */
    public static boolean isPackItem(RegistryAccess access, ItemStack stack) {
        for (TechDefinition definition : registry(access)) {
            for (ItemStack cost : definition.cost()) {
                if (ItemStack.isSameItem(cost, stack)) {
                    return true;
                }
            }
        }
        return false;
    }
}
