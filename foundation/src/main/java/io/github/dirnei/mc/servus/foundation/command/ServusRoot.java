package io.github.dirnei.mc.servus.foundation.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The shared {@code /servus} root. Each mod registers its own branch under it, named after the
 * mod; brigadier merges the roots of every registration into one tree, in any load order. The
 * root carries no permission, so each branch or command sets its own.
 */
public final class ServusRoot {

    /** The root literal. */
    public static final String ROOT = "servus";

    private ServusRoot() {
    }

    /** {@code /servus <branch> ...}, ready for {@code dispatcher.register}. */
    public static LiteralArgumentBuilder<CommandSourceStack> with(LiteralArgumentBuilder<CommandSourceStack> branch) {
        return Commands.literal(ROOT).then(branch);
    }
}
