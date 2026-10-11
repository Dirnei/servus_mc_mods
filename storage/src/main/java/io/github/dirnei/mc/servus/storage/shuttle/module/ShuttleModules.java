package io.github.dirnei.mc.servus.storage.shuttle.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The modules fitted into a shuttle, kept as a data component on the Shuttle
 * item. Items compare by identity, so the record's equality is the
 * component's. A shuttle without modules carries no component at all.
 */
public record ShuttleModules(Optional<Item> tool, Optional<Item> utility) {

    public static final ShuttleModules EMPTY = new ShuttleModules(Optional.empty(), Optional.empty());

    public static final Codec<ShuttleModules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("tool").forGetter(ShuttleModules::tool),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("utility").forGetter(ShuttleModules::utility)
    ).apply(instance, ShuttleModules::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShuttleModules> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)), ShuttleModules::tool,
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)), ShuttleModules::utility,
            ShuttleModules::new);

    public Optional<Item> get(ModuleSlot slot) {
        return slot == ModuleSlot.TOOL ? tool : utility;
    }

    public ShuttleModules with(ModuleSlot slot, Optional<Item> module) {
        return slot == ModuleSlot.TOOL ? new ShuttleModules(module, utility) : new ShuttleModules(tool, module);
    }

    public boolean isEmpty() {
        return tool.isEmpty() && utility.isEmpty();
    }

    /** The specs of the fitted module items, tool first. */
    public List<ModuleSpec> specs() {
        List<ModuleSpec> specs = new ArrayList<>();
        for (Optional<Item> module : List.of(tool, utility)) {
            if (module.isPresent() && module.get() instanceof ShuttleModuleItem item) {
                specs.add(item.spec());
            }
        }
        return specs;
    }

    public static ShuttleModules of(ItemStack shuttle) {
        return shuttle.getOrDefault(ServusStorageComponents.SHUTTLE_MODULES.get(), EMPTY);
    }

    /** Writes the modules onto the shuttle; an empty set removes the component so the shuttle stacks with new ones. */
    public static void set(ItemStack shuttle, ShuttleModules modules) {
        if (modules.isEmpty()) {
            shuttle.remove(ServusStorageComponents.SHUTTLE_MODULES.get());
        } else {
            shuttle.set(ServusStorageComponents.SHUTTLE_MODULES.get(), modules);
        }
    }

    /** What the shuttle on this item can do, with the Cargo Hold's stack count from warehouse.json. */
    public static ShuttleProfile profile(ItemStack shuttle, int cargoHoldStacks) {
        return ShuttleProfile.of(of(shuttle).specs(), cargoHoldStacks);
    }
}
