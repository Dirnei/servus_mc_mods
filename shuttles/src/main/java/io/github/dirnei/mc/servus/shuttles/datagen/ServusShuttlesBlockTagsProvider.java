package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.TreeTags;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

final class ServusShuttlesBlockTagsProvider extends BlockTagsProvider {

    ServusShuttlesBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusShuttlesIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE).add(ServusShuttlesBlocks.STORAGE_TERMINAL.get()).add(ServusShuttlesBlocks.CRAFTING_STATION.get())
                .add(ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get()).add(ServusShuttlesBlocks.RAIL.get())
                .add(ServusShuttlesBlocks.ELEVATOR.get()).add(ServusShuttlesBlocks.HEAVY_RACK.get()).add(ServusShuttlesBlocks.FLUID_RACK.get())
                .add(ServusShuttlesBlocks.INBOUND_STATION.get()).add(ServusShuttlesBlocks.PACKING_STATION.get()).add(ServusShuttlesBlocks.SHUTTLE_GARAGE.get())
                .add(ServusShuttlesBlocks.OUTBOUND_STATION.get()).add(ServusShuttlesBlocks.CRAFTING_CPU.get())
                .add(ServusShuttlesBlocks.CPU_BUFFER.get())
                .add(ServusShuttlesBlocks.PRODUCTION_TERMINAL.get()).add(ServusShuttlesBlocks.PATTERN_STORAGE.get())
                .add(ServusShuttlesBlocks.FARM_RAIL.get()).add(ServusShuttlesBlocks.FIELD_HEAD.get())
                .add(ServusShuttlesBlocks.FLUID_INBOUND.get());
        tag(BlockTags.CLIMBABLE).add(ServusShuttlesBlocks.ELEVATOR.get());
        tag(TreeTags.TREE_LOGS).addTag(BlockTags.LOGS).add(Blocks.MANGROVE_ROOTS);
        tag(TreeTags.TREE_ATTACHMENTS).add(Blocks.VINE, Blocks.COCOA, Blocks.MANGROVE_PROPAGULE, Blocks.PALE_HANGING_MOSS, Blocks.MOSS_CARPET,
                Blocks.PALE_MOSS_CARPET, Blocks.BEE_NEST);
    }
}
