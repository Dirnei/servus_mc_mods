package io.github.dirnei.mc.servus.storage.shuttle.module;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A module that the Shuttle Workshop fits into a shuttle's tool or utility slot. */
public class ShuttleModuleItem extends Item {

    private final ModuleSpec spec;

    public ShuttleModuleItem(ModuleSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public ModuleSpec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.servus_storage.module." + spec.slot().name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.servus_storage.module." + spec.id()).withStyle(ChatFormatting.GRAY));
    }
}
