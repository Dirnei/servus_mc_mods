package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorGameTests;
import net.minecraft.core.Direction;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.loader.ChunkLoaderBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.loader.ChunkLoaderData;
import java.util.UUID;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The machines Foundation owns: Coal Generator, pumps, Sink and Chunk Loader. */
public final class MachineGameTests {

    static final String EMPTY = "empty_12x6x12";
    static final BlockPos MACHINE = new BlockPos(3, 1, 3);

    private MachineGameTests() {
    }

    /** One coal in the fuel slot fills the generator's buffer at its rate. */
    @ServusGameTest(timeoutTicks = 60)
    public static void generatorMakesEnergyFromCoal(GameTestHelper helper) {
        CoalGeneratorGameTests.place(helper, MACHINE, Direction.NORTH);
        CoalGeneratorBlockEntity generator = helper.getBlockEntity(MACHINE, CoalGeneratorBlockEntity.class);
        generator.inventory().setStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
        helper.runAfterDelay(20, () -> {
            int stored = generator.energyStorage().getEnergyStored();
            helper.assertTrue(stored > 0, "the generator should have produced energy, has " + stored);
            helper.assertTrue(generator.energyStorage().receiveEnergy(1000, true) == 0, "the generator should never receive energy");
            helper.succeed();
        });
    }

    /** A powered, owned Chunk Loader holds its ticket; breaking it releases the ticket and its team slot. */
    @ServusGameTest(timeoutTicks = 60)
    public static void chunkLoaderHoldsAndReleasesItsTicket(GameTestHelper helper) {
        helper.setBlock(MACHINE, MachineContent.CHUNK_LOADER.get().defaultBlockState());
        ChunkLoaderBlockEntity loader = helper.getBlockEntity(MACHINE, ChunkLoaderBlockEntity.class);
        UUID team = UUID.randomUUID();
        loader.setOwner(team);
        loader.energyStorage().receiveEnergy(4000, false);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(loader.status() == ChunkLoaderBlockEntity.STATUS_ACTIVE, "the loader should be active, status " + loader.status());
            helper.assertTrue(loader.ticketHeld(), "the loader should hold its chunk ticket");
            ChunkLoaderData data = ChunkLoaderData.get(helper.getLevel().getServer());
            helper.assertTrue(data.countFor(team) == 1, "the team should have one loader");
            helper.setBlock(MACHINE, Blocks.AIR.defaultBlockState());
            helper.assertTrue(!loader.ticketHeld(), "breaking the loader should release the ticket");
            helper.assertTrue(data.countFor(team) == 0, "breaking the loader should free the team slot");
            helper.succeed();
        });
    }

    /**
     * The eye on the front reads {@code LIT}: it is set while a loader holds its
     * chunk and clear for a loader over the team cap or without power.
     */
    @ServusGameTest(timeoutTicks = 60)
    public static void chunkLoaderLitFollowsActive(GameTestHelper helper) {
        int cap = ServusFoundationConfig.maxChunkLoadersPerTeam();
        helper.assertTrue(cap >= 1 && cap <= 8, "the test fits a team cap of 1 to 8, got " + cap);
        UUID team = UUID.randomUUID();
        for (int i = 0; i <= cap; i++) {
            BlockPos pos = new BlockPos(1 + i, 1, 6);
            helper.setBlock(pos, MachineContent.CHUNK_LOADER.get().defaultBlockState());
            ChunkLoaderBlockEntity loader = helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class);
            loader.setOwner(team);
            loader.energyStorage().receiveEnergy(4000, false);
        }
        BlockPos unpowered = new BlockPos(1, 1, 8);
        helper.setBlock(unpowered, MachineContent.CHUNK_LOADER.get().defaultBlockState());
        helper.getBlockEntity(unpowered, ChunkLoaderBlockEntity.class).setOwner(UUID.randomUUID());
        helper.runAfterDelay(10, () -> {
            int lit = 0;
            for (int i = 0; i <= cap; i++) {
                BlockPos pos = new BlockPos(1 + i, 1, 6);
                ChunkLoaderBlockEntity loader = helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class);
                boolean on = helper.getBlockState(pos).getValue(MachineBlock.LIT);
                boolean active = loader.status() == ChunkLoaderBlockEntity.STATUS_ACTIVE;
                helper.assertTrue(on == active, "LIT follows the active state at " + pos + ", status " + loader.status());
                helper.assertTrue(active || loader.status() == ChunkLoaderBlockEntity.STATUS_TEAM_LIMIT,
                        "a powered loader is active or over the cap, status " + loader.status());
                lit += on ? 1 : 0;
            }
            helper.assertTrue(lit == cap, "exactly the team cap of loaders is lit, got " + lit);
            helper.assertFalse(helper.getBlockState(unpowered).getValue(MachineBlock.LIT), "a loader without power is not lit");
            helper.succeed();
        });
    }
}
