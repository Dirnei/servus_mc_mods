package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleLabelForm;
import io.github.dirnei.mc.servus.shuttles.shuttle.CounterData;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A shuttle of one tier for a Shuttle Garage; its tooltip lists its tool and upgrades. */
public class ShuttleItem extends Item {

    /** The colour of shuttle labels in chat and action bar messages. */
    public static final ChatFormatting CHAT_COLOUR = ChatFormatting.GREEN;

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
            case 1 -> ServusShuttlesItems.SHUTTLE_MK1.get();
            case 2 -> ServusShuttlesItems.SHUTTLE_MK2.get();
            case 3 -> ServusShuttlesItems.SHUTTLE_MK3.get();
            default -> null;
        };
    }

    /** The name plate engraved on the shuttle, or empty. */
    public static Optional<String> plate(ItemStack stack) {
        String name = stack.get(ServusShuttlesComponents.SHUTTLE_NAME.get());
        return name == null || name.isEmpty() ? Optional.empty() : Optional.of(name);
    }

    /** Engraves a plate on the shuttle; an empty name keeps the old one. */
    public static void engrave(ItemStack stack, String name) {
        if (!name.isEmpty()) {
            stack.set(ServusShuttlesComponents.SHUTTLE_NAME.get(), name);
        }
    }

    /** The world's number of the shuttle, empty until it was first put into a garage. */
    public static OptionalInt number(ItemStack stack) {
        Integer number = stack.get(ServusShuttlesComponents.SHUTTLE_NUMBER.get());
        return number == null ? OptionalInt.empty() : OptionalInt.of(number);
    }

    /** Gives a shuttle without a number the world's next one. @return whether it got one */
    public static boolean numberIfMissing(ServerLevel level, ItemStack stack) {
        if (!isShuttle(stack) || number(stack).isPresent()) {
            return false;
        }
        stack.set(ServusShuttlesComponents.SHUTTLE_NUMBER.get(), CounterData.get(level.getServer(), CounterData.Kind.SHUTTLE_NUMBERS).take());
        return true;
    }

    /**
     * The name of one shuttle for players: "Shuttle #12", "Berta (#12)" with a plate,
     * and without a number "Berta (Shuttle Mk2)" or "Shuttle Mk1". Resolved by the client,
     * so server built messages can carry it too.
     */
    public static Component label(ItemStack stack) {
        Optional<String> plate = plate(stack);
        OptionalInt number = number(stack);
        Component tierName = Component.translatable(stack.getItem().getDescriptionId());
        return switch (ShuttleLabelForm.of(plate.isPresent(), number.isPresent())) {
            case NUMBERED_NAMED -> Component.translatable("shuttle.servus_shuttles.label.named", plate.get(), number.getAsInt());
            case NUMBERED -> Component.translatable("shuttle.servus_shuttles.label", number.getAsInt());
            case NAMED -> Component.translatable("item.servus_shuttles.shuttle.named", plate.get(), tierName);
            case PLAIN -> tierName;
        };
    }

    /** The {@link #label} for chat and action bar messages, in green; GUI text uses the plain label. */
    public static Component chatLabel(ItemStack stack) {
        return label(stack).copy().withStyle(CHAT_COLOUR);
    }

    /** A shuttle is named by its {@link #label}; an anvil name still wins in the hover name. */
    @Override
    public Component getName(ItemStack stack) {
        return label(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (number(stack).isPresent()) {
            tooltip.accept(Component.translatable(getDescriptionId()).withStyle(ChatFormatting.GRAY));
        }
        ShuttleModules modules = ShuttleModules.of(stack);
        if (modules.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.servus_shuttles.shuttle.plain").withStyle(ChatFormatting.GRAY));
            return;
        }
        modules.tool().ifPresent(item -> tooltip.accept(Component.translatable("tooltip.servus_shuttles.shuttle.tool", Component.translatable(item.getDescriptionId()))
                .withStyle(ChatFormatting.GRAY)));
        if (modules.speed() > 0) {
            tooltip.accept(Component.translatable("tooltip.servus_shuttles.shuttle.speed", modules.speed()).withStyle(ChatFormatting.GRAY));
        }
        if (modules.handling() > 0) {
            tooltip.accept(Component.translatable("tooltip.servus_shuttles.shuttle.handling", modules.handling()).withStyle(ChatFormatting.GRAY));
        }
        if (modules.muffled()) {
            tooltip.accept(Component.translatable("tooltip.servus_shuttles.shuttle.muffled").withStyle(ChatFormatting.GRAY));
        }
    }
}
