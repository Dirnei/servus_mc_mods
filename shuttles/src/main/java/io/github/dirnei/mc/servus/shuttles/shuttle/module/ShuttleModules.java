package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers.Kind;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The tool, upgrade counts and Muffler of a shuttle, kept as a data component on the
 * shuttle item. Items compare by identity, so the record's equality is the
 * component's. A shuttle without any carries no component at all.
 */
public record ShuttleModules(Optional<Item> tool, int speed, int handling, boolean muffled) {

    public static final ShuttleModules EMPTY = new ShuttleModules(Optional.empty(), 0, 0, false);

    public static final Codec<ShuttleModules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("tool").forGetter(ShuttleModules::tool),
            Codec.INT.optionalFieldOf("speed", 0).forGetter(ShuttleModules::speed),
            Codec.INT.optionalFieldOf("handling", 0).forGetter(ShuttleModules::handling),
            Codec.BOOL.optionalFieldOf("muffled", false).forGetter(ShuttleModules::muffled)
    ).apply(instance, ShuttleModules::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShuttleModules> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)), ShuttleModules::tool,
            ByteBufCodecs.VAR_INT, ShuttleModules::speed,
            ByteBufCodecs.VAR_INT, ShuttleModules::handling,
            ByteBufCodecs.BOOL, ShuttleModules::muffled,
            ShuttleModules::new);

    public ShuttleModules {
        speed = Math.max(0, speed);
        handling = Math.max(0, handling);
    }

    /** Modules without a Muffler. */
    public ShuttleModules(Optional<Item> tool, int speed, int handling) {
        this(tool, speed, handling, false);
    }

    public int count(Kind kind) {
        return kind == Kind.SPEED ? speed : handling;
    }

    public ShuttleModules withCount(Kind kind, int count) {
        return kind == Kind.SPEED ? new ShuttleModules(tool, count, handling, muffled) : new ShuttleModules(tool, speed, count, muffled);
    }

    public ShuttleModules withTool(Optional<Item> next) {
        return new ShuttleModules(next, speed, handling, muffled);
    }

    public ShuttleModules withMuffled(boolean next) {
        return new ShuttleModules(tool, speed, handling, next);
    }

    public boolean isEmpty() {
        return tool.isEmpty() && speed == 0 && handling == 0 && !muffled;
    }

    /** The spec of the fitted tool. */
    public Optional<ModuleSpec> toolSpec() {
        return tool.filter(item -> item instanceof ShuttleModuleItem).map(item -> ((ShuttleModuleItem) item).spec());
    }

    public static ShuttleModules of(ItemStack shuttle) {
        return shuttle.getOrDefault(ServusShuttlesComponents.SHUTTLE_MODULES.get(), EMPTY);
    }

    /** Writes the modules onto the shuttle; an empty set removes the component so the shuttle stacks with new ones. */
    public static void set(ItemStack shuttle, ShuttleModules modules) {
        if (modules.isEmpty()) {
            shuttle.remove(ServusShuttlesComponents.SHUTTLE_MODULES.get());
        } else {
            shuttle.set(ServusShuttlesComponents.SHUTTLE_MODULES.get(), modules);
        }
    }

    /** What the shuttle on this item can do by these stats. */
    public static ShuttleProfile profile(ItemStack shuttle, ShuttleTiers tiers) {
        ShuttleModules modules = of(shuttle);
        return ShuttleProfile.of(tiers, ShuttleItem.tier(shuttle), modules.toolSpec(), modules.speed, modules.handling);
    }
}
