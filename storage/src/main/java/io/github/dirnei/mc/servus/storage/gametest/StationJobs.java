package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Patterns and station jobs the Storage GameTests share. */
final class StationJobs {

    private StationJobs() {
    }

    /** Two oak planks above each other make four sticks. */
    static EncodedPattern sticks() {
        return new EncodedPattern(ResourceLocation.withDefaultNamespace("stick"),
                List.of(ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS)),
                new ItemStack(Items.STICK, 4));
    }

    /** One oak log makes four oak planks. */
    static EncodedPattern planks() {
        return new EncodedPattern(ResourceLocation.withDefaultNamespace("oak_planks"),
                List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_LOG)),
                new ItemStack(Items.OAK_PLANKS, 4));
    }

    /** Hands the station a sticks job owned by a fresh test object, so it takes planks. */
    static Object sticksJob(CraftingStationBlockEntity station, int crafts) {
        Object owner = new Object();
        station.assign(sticks(), crafts, owner);
        return owner;
    }
}
