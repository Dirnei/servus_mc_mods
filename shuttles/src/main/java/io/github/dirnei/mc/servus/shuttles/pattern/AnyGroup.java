package io.github.dirnei.mc.servus.shuttles.pattern;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What the cells of one ingredient of an Any pattern accept, for tooltips.
 *
 * @param tagKey the translation key of the ingredient's item tag, empty when it is no tag
 * @param items  up to {@link #SHOWN} of the accepted items
 * @param total  how many items are accepted
 */
public record AnyGroup(String tagKey, List<ItemStack> items, int total) {

    public static final int SHOWN = 3;

    public static final StreamCodec<RegistryFriendlyByteBuf, AnyGroup> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AnyGroup::tagKey,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), AnyGroup::items,
            ByteBufCodecs.VAR_INT, AnyGroup::total,
            AnyGroup::new);

    public AnyGroup {
        items = List.copyOf(items.subList(0, Math.min(SHOWN, items.size())));
    }

    /**
     * "Any Planks" when the tag has a name in the language, else "Any of Oak
     * Planks, Spruce Planks, Birch Planks and 9 more".
     *
     * @param tagNamed whether the current language translates {@link #tagKey}
     */
    public Component label(boolean tagNamed) {
        if (tagNamed && !tagKey.isEmpty()) {
            return Component.translatable("gui.servus_shuttles.pattern.any_tag", Component.translatable(tagKey));
        }
        MutableComponent names = Component.empty();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(items.get(i).getHoverName());
        }
        int more = total - items.size();
        Component listed = more > 0 ? Component.translatable("gui.servus_shuttles.pattern.any_more", names, more) : names;
        return Component.translatable("gui.servus_shuttles.pattern.any_items", listed);
    }
}
