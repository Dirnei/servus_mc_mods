package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorGameTests;
import net.minecraft.core.Direction;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorData;
import io.github.dirnei.mc.servus.foundation.machine.anchor.AnchorArea;
import java.util.UUID;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The machines Foundation owns: Coal Generator, pumps, Sink and Ender Anchor. */
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

    /** A powered, owned Ender Anchor holds its ticket; breaking it releases the ticket and its team slot. */
    @ServusGameTest(timeoutTicks = 60)
    public static void enderAnchorHoldsAndReleasesItsTicket(GameTestHelper helper) {
        helper.setBlock(MACHINE, MachineContent.ENDER_ANCHOR.get().defaultBlockState());
        EnderAnchorBlockEntity loader = helper.getBlockEntity(MACHINE, EnderAnchorBlockEntity.class);
        UUID team = UUID.randomUUID();
        loader.setOwner(team);
        loader.energyStorage().receiveEnergy(4000, false);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(loader.status() == EnderAnchorBlockEntity.STATUS_ACTIVE, "the loader should be active, status " + loader.status());
            helper.assertTrue(loader.ticketHeld(), "the loader should hold its chunk ticket");
            EnderAnchorData data = EnderAnchorData.get(helper.getLevel().getServer());
            helper.assertTrue(data.load(team) == 1, "the team should have one loader");
            helper.setBlock(MACHINE, Blocks.AIR.defaultBlockState());
            helper.assertTrue(!loader.ticketHeld(), "breaking the loader should release the ticket");
            helper.assertTrue(data.load(team) == 0, "breaking the loader should free the team slot");
            helper.succeed();
        });
    }

    /**
     * The eye on the front reads {@code LIT}: it is set while an anchor holds
     * its chunks and clear for an anchor over the team cap or without power.
     */
    @ServusGameTest(timeoutTicks = 60)
    public static void enderAnchorLitFollowsActive(GameTestHelper helper) {
        int cap = ServusFoundationConfig.maxLoadedChunksPerTeam();
        UUID team = UUID.randomUUID();
        // A stand-in leaves room for exactly one more chunk.
        java.util.List<Long> far = new java.util.ArrayList<>();
        for (int i = 0; i < cap - 1; i++) {
            far.add(AnchorArea.pack(300000 + i % 64, 300000 + i / 64));
        }
        EnderAnchorData data = EnderAnchorData.get(helper.getLevel().getServer());
        data.register(team, "standin@" + team, EnderAnchorData.chunks(helper.getLevel().dimension(), far));
        BlockPos fits = new BlockPos(1, 1, 6);
        BlockPos over = new BlockPos(1, 2, 6);
        for (BlockPos pos : new BlockPos[] {fits, over}) {
            helper.setBlock(pos, MachineContent.ENDER_ANCHOR.get().defaultBlockState());
            EnderAnchorBlockEntity loader = helper.getBlockEntity(pos, EnderAnchorBlockEntity.class);
            loader.setOwner(team);
            loader.energyStorage().receiveEnergy(20000, false);
        }
        // Nine chunks never fit next to the stand-in, one does.
        helper.getBlockEntity(over, EnderAnchorBlockEntity.class).upgrades()
                .insertItem(0, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
        BlockPos unpowered = new BlockPos(1, 1, 8);
        helper.setBlock(unpowered, MachineContent.ENDER_ANCHOR.get().defaultBlockState());
        helper.getBlockEntity(unpowered, EnderAnchorBlockEntity.class).setOwner(UUID.randomUUID());
        helper.runAfterDelay(10, () -> {
            EnderAnchorBlockEntity active = helper.getBlockEntity(fits, EnderAnchorBlockEntity.class);
            EnderAnchorBlockEntity refused = helper.getBlockEntity(over, EnderAnchorBlockEntity.class);
            helper.assertTrue(active.status() == EnderAnchorBlockEntity.STATUS_ACTIVE, "the one chunk anchor is active, status " + active.status());
            helper.assertTrue(refused.status() == EnderAnchorBlockEntity.STATUS_TEAM_LIMIT, "the 3 by 3 anchor is over the cap, status " + refused.status());
            helper.assertTrue(helper.getBlockState(fits).getValue(MachineBlock.LIT), "the active anchor is lit");
            helper.assertFalse(helper.getBlockState(over).getValue(MachineBlock.LIT), "the anchor over the cap is not lit");
            helper.assertFalse(helper.getBlockState(unpowered).getValue(MachineBlock.LIT), "a loader without power is not lit");
            helper.assertTrue(data.load(team) == cap, "the team is exactly at its cap, load " + data.load(team));
            helper.succeed();
        });
    }
}
