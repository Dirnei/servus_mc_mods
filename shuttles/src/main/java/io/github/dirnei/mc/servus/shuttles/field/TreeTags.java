package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** The tags that tell tree fields which saplings grow big and which blocks belong to a tree. */
public final class TreeTags {

    /** Saplings that grow a big tree from a square of four; they get a 2x2 entry in the sapling picker. */
    public static final TagKey<Item> SAPLINGS_2X2 = item("saplings_2x2");
    /** Saplings that grow only from a square of four; they get only the 2x2 entry. */
    public static final TagKey<Item> SAPLINGS_2X2_ONLY = item("saplings_2x2_only");
    /** Blocks a Lumber shuttle fells as the logs of a tree. */
    public static final TagKey<Block> TREE_LOGS = block("tree_logs");
    /** Hanging parts that go with a felled tree when they touch it, such as vines and cocoa. */
    public static final TagKey<Block> TREE_ATTACHMENTS = block("tree_attachments");

    private TreeTags() {
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, path));
    }
}
