package io.github.dirnei.mc.servus.shuttles.locker;

import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity.BoxView;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * What the locker GUI shows of one box, read only: whose it is for the
 * viewer, the name of the player who ordered it and how far the order is.
 *
 * @param state the order's state, null while the box is free
 */
public record BoxInfo(BoxView view, String owner, LockerReturns.@Nullable State state) {

    private static final String KEY = "gui.servus_shuttles.parcel_locker.";

    /** Who ordered the box and how far the order is, or that it is free. */
    public Component describe(LockerLayout.Box box) {
        return describe(box, owner, state);
    }

    /** "S-02: Steve, ready", or "S-02: free" while the box has no order. */
    public static Component describe(LockerLayout.Box box, String owner, LockerReturns.@Nullable State state) {
        if (state == null) {
            return Component.translatable(KEY + "box.free", box.id());
        }
        return Component.translatable(KEY + "detail", box.id(), owner,
                Component.translatable(KEY + "state." + state.name().toLowerCase(Locale.ROOT)));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(view);
        buf.writeUtf(owner);
        buf.writeVarInt(state == null ? -1 : state.ordinal());
    }

    public static BoxInfo read(RegistryFriendlyByteBuf buf) {
        BoxView view = buf.readEnum(BoxView.class);
        String owner = buf.readUtf();
        int state = buf.readVarInt();
        return new BoxInfo(view, owner, state < 0 ? null : LockerReturns.State.values()[state]);
    }
}
