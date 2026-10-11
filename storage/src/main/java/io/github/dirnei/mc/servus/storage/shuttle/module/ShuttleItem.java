package io.github.dirnei.mc.servus.storage.shuttle.module;

import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A shuttle of one tier for a Shuttle Garage; its tooltip lists its tool and upgrades. */
public class ShuttleItem extends Item {

    private final int tier;

    public ShuttleItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    /** 1 for Mk1 up to 3 for Mk3. */
    public int tier() {
        return tier;
    }

    /** Whether the stack is a shuttle of any tier. */
    public static boolean isShuttle(ItemStack stack) {
        return stack.getItem() instanceof ShuttleItem;
    }

    /** The tier of the shuttle on the stack, 1 when it is no shuttle. */
    public static int tier(ItemStack stack) {
        return stack.getItem() instanceof ShuttleItem shuttle ? shuttle.tier : 1;
    }

    /** The shuttle item of that tier, or null above Mk3. */
    public static ShuttleItem ofTier(int tier) {
        return switch (tier) {
            case 1 -> ServusStorageItems.SHUTTLE_MK1.get();
            case 2 -> ServusStorageItems.SHUTTLE_MK2.get();
            case 3 -> ServusStorageItems.SHUTTLE_MK3.get();
            default -> null;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ShuttleModules modules = ShuttleModules.of(stack);
        if (modules.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.servus_storage.shuttle.plain").withStyle(ChatFormatting.GRAY));
            return;
        }
        modules.tool().ifPresent(item -> tooltip.add(Component.translatable("tooltip.servus_storage.shuttle.tool", item.getDescription())
                .withStyle(ChatFormatting.GRAY)));
        if (modules.speed() > 0) {
            tooltip.add(Component.translatable("tooltip.servus_storage.shuttle.speed", modules.speed()).withStyle(ChatFormatting.GRAY));
        }
        if (modules.handling() > 0) {
            tooltip.add(Component.translatable("tooltip.servus_storage.shuttle.handling", modules.handling()).withStyle(ChatFormatting.GRAY));
        }
    }
}
