package io.github.dirnei.mc.servus.shuttles.zone;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * What a zone takes: filter items and item tag ids. Kept with the team zone
 * and applied by every bay that points at it.
 *
 * @param items one item stacks, empty entries left out
 * @param tags  item tag ids such as {@code c:ores}
 */
public record ZoneFilter(List<ItemStack> items, List<String> tags) {

    public static final int SLOTS = 9;
    public static final ZoneFilter EMPTY = new ZoneFilter(List.of(), List.of());

    public static final Codec<ZoneFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(ZoneFilter::items),
            Codec.STRING.listOf().fieldOf("tags").forGetter(ZoneFilter::tags)
    ).apply(instance, ZoneFilter::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneFilter> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ZoneFilter::items,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ZoneFilter::tags,
            ZoneFilter::new);

    public ZoneFilter {
        items = List.copyOf(items);
        tags = List.copyOf(tags);
    }

    public boolean isEmpty() {
        return items.isEmpty() && tags.isEmpty();
    }

    /** The pure form for routing. */
    public PutAwayPlan.Filter<ItemKey> toPure() {
        Set<ItemKey> keys = new HashSet<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                keys.add(ItemKey.of(stack));
            }
        }
        return new PutAwayPlan.Filter<>(keys, new HashSet<>(tags));
    }

    /**
     * The fluids of the filled fluid containers among the items, for example
     * water for a water bucket: what the zone means on a Fluid Rack. Items
     * holding no fluid and tags are left out.
     */
    public Set<Fluid> fluids() {
        Set<Fluid> fluids = new LinkedHashSet<>();
        for (ItemStack stack : items) {
            Transfer.fluidOf(stack).ifPresent(fluids::add);
        }
        return fluids;
    }

    /** Item names for display. */
    public List<String> itemNames() {
        List<String> names = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                names.add(stack.getHoverName().getString());
            }
        }
        return names;
    }

    /** Tags as one comma separated line for a text box. */
    public String tagLine() {
        return String.join(", ", tags);
    }

    /**
     * Parses a comma separated line of tag ids. Entries that are not resource
     * locations are dropped; a leading '#' is allowed.
     */
    public static List<String> parseTags(String line) {
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        if (line == null) {
            return List.of();
        }
        for (String part : line.split(",")) {
            String text = part.trim();
            if (text.startsWith("#")) {
                text = text.substring(1);
            }
            if (text.isEmpty()) {
                continue;
            }
            Identifier id = Identifier.tryParse(text);
            if (id != null) {
                parsed.add(id.toString());
            }
        }
        return List.copyOf(parsed);
    }
}
