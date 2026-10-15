package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Crafting Stations, Pattern Storages, patterns and station jobs the Storage GameTests share. */
final class StationJobs {

    private StationJobs() {
    }

    /** Two oak planks above each other make four sticks. */
    static EncodedPattern sticks() {
        return new EncodedPattern(Identifier.withDefaultNamespace("stick"),
                List.of(ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS)),
                new ItemStack(Items.STICK, 4));
    }

    /** One oak log makes four oak planks. */
    static EncodedPattern planks() {
        return new EncodedPattern(Identifier.withDefaultNamespace("oak_planks"),
                List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_LOG)),
                new ItemStack(Items.OAK_PLANKS, 4));
    }

    /** Four oak planks in a square make a crafting table. */
    static EncodedPattern craftingTable() {
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        return new EncodedPattern(Identifier.withDefaultNamespace("crafting_table"),
                List.of(planks, planks, ItemStack.EMPTY, planks, planks), new ItemStack(Items.CRAFTING_TABLE));
    }

    /** A Crafting Station facing north. */
    static CraftingStationBlockEntity craftingStation(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusShuttlesBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos, CraftingStationBlockEntity.class);
    }

    /** A Pattern Storage facing north. */
    static PatternStorageBlockEntity patternStorage(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusShuttlesBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos, PatternStorageBlockEntity.class);
    }

    /** Hands the station a sticks job owned by a fresh test object, so it takes planks. */
    static Object sticksJob(CraftingStationBlockEntity station, int crafts) {
        Object owner = new Object();
        station.assign(sticks(), crafts, owner);
        return owner;
    }
}
