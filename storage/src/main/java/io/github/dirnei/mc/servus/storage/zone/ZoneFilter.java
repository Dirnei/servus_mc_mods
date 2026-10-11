package io.github.dirnei.mc.servus.storage.zone;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * What a zone takes: filter items and item tag ids. Stored on the Zone Label
 * as a data component and copied onto stamped blocks.
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

    // NBT, for block entities

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag itemList = new ListTag();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                itemList.add(ValueIo.saveStack(registries, stack));
            }
        }
        tag.put("Items", itemList);
        ListTag tagList = new ListTag();
        for (String id : tags) {
            tagList.add(StringTag.valueOf(id));
        }
        tag.put("Tags", tagList);
        return tag;
    }

    public static ZoneFilter load(CompoundTag tag, HolderLookup.Provider registries) {
        List<ItemStack> items = new ArrayList<>();
        for (Tag entry : tag.getListOrEmpty("Items")) {
            ItemStack stack = ValueIo.parseStack(registries, (CompoundTag) entry);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        List<String> tags = new ArrayList<>();
        for (Tag entry : tag.getListOrEmpty("Tags")) {
            tags.add(entry.asString().orElse(""));
        }
        return new ZoneFilter(items, tags);
    }
}
