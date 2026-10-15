package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A tool module that the Shuttle Workshop installs as a shuttle's one tool. */
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.servus_shuttles.module.tool").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.servus_shuttles.module." + spec.id()).withStyle(ChatFormatting.GRAY));
        if (spec.wide()) {
            tooltip.accept(Component.translatable("tooltip.servus_shuttles.module.wide").withStyle(ChatFormatting.GOLD));
        }
    }
}
