package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.lab.ResearchLabBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of Servus: Manufacturing. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ServusManufacturingGameTests {

    /** The holder's namespace is prefixed by NeoForge; only the path goes here. */
    private static final String EMPTY = "empty_12x6x12";
    private static final int RUN_TICKS = 1200;
    private static final int MIN_PACKS = 7;
    private static final int MAX_PACKS = 13;

    private ServusManufacturingGameTests() {
    }

    /**
     * The reference balanced block from PLAN.md: 2 iron pairs, 1 copper pair,
     * 1 coal extractor, 3 assemblers, 1 lab. Expect one red pack per 5 s once
     * the pipeline is filled.
     */
    @GameTest(template = EMPTY, timeoutTicks = RUN_TICKS + 100)
    public static void referenceBlockProducesOnePackPerFiveSeconds(GameTestHelper helper) {
        // Deposits (y 1) and extractors (y 2) at x 1; smelters at x 2.
        burnerPair(helper, 1, ServusManufacturingBlocks.IRON_DEPOSIT.get(), true);
        burnerPair(helper, 3, ServusManufacturingBlocks.IRON_DEPOSIT.get(), true);
        burnerPair(helper, 5, ServusManufacturingBlocks.COPPER_DEPOSIT.get(), true);
        burnerPair(helper, 7, ServusManufacturingBlocks.COAL_DEPOSIT.get(), false);

        // Duct column at x 3 from z 1 to z 7.
        for (int z = 1; z <= 7; z++) {
            helper.setBlock(new BlockPos(3, 2, z), ServusManufacturingBlocks.ITEM_DUCT.get().defaultBlockState());
        }

        // Assemblers at x 4 with their recipes, generators at x 5.
        assembler(helper, 1, "iron_gear");
        assembler(helper, 3, "copper_wire");
        assembler(helper, 5, "red_pack");

        // Lab at x 4 z 7 collecting packs, generator beside it.
        BlockPos labPos = new BlockPos(4, 2, 7);
        helper.setBlock(labPos, facing(ServusManufacturingBlocks.RESEARCH_LAB.get(), Direction.WEST));
        generator(helper, new BlockPos(5, 2, 7));

        helper.runAtTickTime(RUN_TICKS, () -> {
            ResearchLabBlockEntity lab = helper.getBlockEntity(labPos);
            int packs = 0;
            for (int slot = 0; slot < lab.inventory().getSlots(); slot++) {
                ItemStack stack = lab.inventory().getStackInSlot(slot);
                if (stack.is(ServusManufacturingItems.RED_PACK.get())) {
                    packs += stack.getCount();
                }
            }
            if (packs < MIN_PACKS || packs > MAX_PACKS) {
                helper.fail("Expected " + MIN_PACKS + " to " + MAX_PACKS + " red packs after " + RUN_TICKS
                        + " ticks, got " + packs, labPos);
            }
            helper.succeed();
        });
    }

    private static BlockState facing(Block block, Direction direction) {
        return block.defaultBlockState().setValue(MachineBlock.FACING, direction);
    }

    private static void fuel(GameTestHelper helper, BlockPos pos) {
        MachineBlockEntity machine = helper.getBlockEntity(pos);
        machine.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 64));
    }

    /** Deposit, extractor on top and, when wanted, a smelter east of it. */
    private static void burnerPair(GameTestHelper helper, int z, Block deposit, boolean withSmelter) {
        helper.setBlock(new BlockPos(1, 1, z), deposit.defaultBlockState());
        BlockPos extractor = new BlockPos(1, 2, z);
        helper.setBlock(extractor, facing(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), Direction.NORTH));
        fuel(helper, extractor);
        if (withSmelter) {
            BlockPos smelter = new BlockPos(2, 2, z);
            helper.setBlock(smelter, facing(ServusManufacturingBlocks.BURNER_SMELTER.get(), Direction.NORTH));
            fuel(helper, smelter);
        } else {
            // The coal extractor feeds the duct column directly.
            helper.setBlock(new BlockPos(2, 2, z), ServusManufacturingBlocks.ITEM_DUCT.get().defaultBlockState());
        }
    }

    private static void assembler(GameTestHelper helper, int z, String recipe) {
        BlockPos pos = new BlockPos(4, 2, z);
        helper.setBlock(pos, facing(ServusManufacturingBlocks.ASSEMBLER.get(), Direction.WEST));
        AssemblerBlockEntity assembler = helper.getBlockEntity(pos);
        assembler.addRecipe(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "assembling/" + recipe), null);
        generator(helper, new BlockPos(5, 2, z));
    }

    private static void generator(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, facing(ServusManufacturingBlocks.COAL_GENERATOR.get(), Direction.NORTH));
        fuel(helper, pos);
    }
}
