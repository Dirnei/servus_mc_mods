package io.github.dirnei.mc.servus.storage.pattern;

import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** An encoded Pattern: one crafting recipe with concrete items, shown in the tooltip. */
public class PatternItem extends Item {

    public PatternItem(Properties properties) {
        super(properties);
    }

    /** The recipe a stack holds, empty for anything but an encoded Pattern. */
    public static Optional<EncodedPattern> encoded(ItemStack stack) {
        if (!stack.is(ServusStorageItems.PATTERN.get())) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.get(ServusStorageComponents.PATTERN.get()));
    }

    /** A Pattern stack holding the recipe. */
    public static ItemStack of(EncodedPattern pattern) {
        ItemStack stack = new ItemStack(ServusStorageItems.PATTERN.get());
        stack.set(ServusStorageComponents.PATTERN.get(), pattern);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<EncodedPattern> pattern = encoded(stack);
        if (pattern.isEmpty()) {
            tooltip.add(Component.translatable("item.servus_storage.pattern.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(describe(pattern.get()).withStyle(ChatFormatting.GRAY));
    }

    /** "4 Stick from 2 Oak Planks", for tooltips and the Patterns tab. */
    public static MutableComponent describe(EncodedPattern pattern) {
        return Component.translatable("item.servus_storage.pattern.recipe", pattern.result().getCount(),
                pattern.result().getHoverName(), stacks(pattern.inputs()));
    }

    /** "2 Oak Planks, 1 Coal" for a list of stacks with counts. */
    public static MutableComponent stacks(List<ItemStack> stacks) {
        MutableComponent text = Component.empty();
        for (int i = 0; i < stacks.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(stacks.get(i).getCount() + " ").append(stacks.get(i).getHoverName());
        }
        return text;
    }
}
