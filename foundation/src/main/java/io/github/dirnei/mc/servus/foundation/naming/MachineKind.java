package io.github.dirnei.mc.servus.foundation.naming;

import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * One kind of named machine. Its id keys the warehouse's counter and names
 * the lang key of its numbered label, {@code machine.<namespace>.<path>}, such as
 * {@code "Field Head #%s"}. The block gives the plain label and is the block
 * the naming GameTest places. A chat colour, when set, marks the label in chat
 * and action bar messages; GUI text stays uncoloured.
 */
public record MachineKind(Identifier id, Supplier<? extends Block> block, @Nullable ChatFormatting chatColour) {

    /** A kind whose label reads in the message's own colour. */
    public MachineKind(Identifier id, Supplier<? extends Block> block) {
        this(id, block, null);
    }

    /** The lang key of the numbered label, taking the number as its argument. */
    public String numberKey() {
        return "machine." + id.getNamespace() + "." + id.getPath();
    }

    /** "Field Head #2", resolved by the client. */
    public Component numbered(int number) {
        return Component.translatable(numberKey(), number);
    }

    /** The block's name: the label of a machine with neither number nor name. */
    public Component plain() {
        return block.get().getName();
    }

    /** The label as a chat or action bar message shows it: in the kind's chat colour, if it has one. */
    public Component inChat(Component label) {
        return chatColour == null ? label : label.copy().withStyle(chatColour);
    }
}
