package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers.Kind;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What goes into a shuttle through the workshop's install slot: the
 * Foundation Speed Upgrade and the Handling Upgrade as counts, tool modules
 * as the one tool.
 */
public final class ShuttleUpgrades {

    /** What an item does to a shuttle. */
    public enum Install { SPEED, HANDLING, TOOL, NONE }

    private ShuttleUpgrades() {
    }

    public static Install kind(ItemStack stack) {
        if (stack.isEmpty()) {
            return Install.NONE;
        }
        if (stack.is(ServusFoundationItems.SPEED_UPGRADE.get())) {
            return Install.SPEED;
        }
        if (stack.is(ServusShuttlesItems.HANDLING_UPGRADE.get())) {
            return Install.HANDLING;
        }
        return stack.getItem() instanceof ShuttleModuleItem ? Install.TOOL : Install.NONE;
    }

    /** The counted kind of an install, empty for a tool or nothing. */
    public static Optional<Kind> counted(Install install) {
        return switch (install) {
            case SPEED -> Optional.of(Kind.SPEED);
            case HANDLING -> Optional.of(Kind.HANDLING);
            default -> Optional.empty();
        };
    }

    /** The item one count of that kind comes back as. */
    public static Item item(Kind kind) {
        return kind == Kind.SPEED ? ServusFoundationItems.SPEED_UPGRADE.get() : ServusShuttlesItems.HANDLING_UPGRADE.get();
    }

    /** How many of the stack the shuttle takes now by these stats: up to the max for counts, one tool. */
    public static int room(ItemStack shuttle, ItemStack stack, ShuttleTiers tiers) {
        if (!ShuttleItem.isShuttle(shuttle)) {
            return 0;
        }
        Install install = kind(stack);
        if (install == Install.TOOL) {
            return ShuttleModules.of(shuttle).tool().filter(stack::is).isPresent() ? 0 : 1;
        }
        return counted(install).map(kind -> tiers.room(ShuttleItem.tier(shuttle), kind, ShuttleModules.of(shuttle).count(kind)))
                .orElse(0);
    }
}
