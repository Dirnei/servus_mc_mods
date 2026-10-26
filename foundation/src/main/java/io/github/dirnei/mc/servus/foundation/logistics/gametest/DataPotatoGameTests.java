package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.channel;
import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DataCableItem;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctRedstone;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctReport;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankReport;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankStatsManager;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

public final class DataPotatoGameTests {

    private static final String KEY = "message.servus_foundation.potato.";

    /** Three ducts in a row along x with a chest north of each. */
    private static final BlockPos DUCT_A = new BlockPos(2, 1, 6);
    private static final BlockPos DUCT_B = new BlockPos(3, 1, 6);
    private static final BlockPos DUCT_C = new BlockPos(4, 1, 6);

    private DataPotatoGameTests() {
    }

    /**
     * Duct B pushes out on "Iron ore" with priority 2; A pulls in and C is in and out on it, and a side of B
     * on it touches no block. The arm report names all of that with 3 ducts, 2 in and 2 out.
     */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsItemArm(GameTestHelper helper) {
        UtilityDuctBlockEntity[] ducts = chestRow(helper);
        int iron = channel(helper, "Iron ore");
        for (UtilityDuctBlockEntity duct : ducts) {
            duct.setChannel(Line.ITEM, Direction.NORTH, iron);
        }
        ducts[0].setMode(Line.ITEM, Direction.NORTH, DuctMode.IN_PULL);
        ducts[1].setMode(Line.ITEM, Direction.NORTH, DuctMode.OUT);
        ducts[1].setPriority(Direction.NORTH, 2);
        ducts[1].setChannel(Line.ITEM, Direction.UP, iron);

        helper.runAfterDelay(5, () -> {
            List<Component> report = DuctReport.of(ducts[1], Direction.NORTH);
            helper.assertTrue(report.size() == 2, "a header and the item line, got " + report);
            assertKey(helper, report.get(0), "duct_side");
            Object[] item = assertKey(helper, report.get(1), "filtered_side");
            helper.assertTrue(text(item[1]).equals("gui.servus_foundation.mode.out"), "the mode is Out, got " + text(item[1]));
            helper.assertTrue(text(item[2]).equals("gui.servus_foundation.redstone.ignore"), "the redstone setting is Ignore, got " + text(item[2]));
            helper.assertTrue(text(item[3]).equals("\"Iron ore\""), "the channel is \"Iron ore\", got " + text(item[3]));
            helper.assertTrue(((Component) item[3]).getStyle().getColor() != null, "the channel name is coloured");
            helper.assertTrue(Arrays.asList(item).subList(4, 10).equals(List.of(2, 0, 0, 3, 2, 2)),
                    "priority 2, no filter entries, none with an amount, 3 ducts, 2 in, 2 out, got " + Arrays.asList(item).subList(4, 10));

            List<Component> centre = DuctReport.of(ducts[1], null);
            Object[] header = assertKey(helper, centre.get(0), "duct_centre");
            helper.assertTrue(text(header[0]).equals(KEY + "unpowered"), "the centre names the duct unpowered, got " + text(header[0]));
            Object[] run = assertKey(helper, centre.get(1), "run");
            helper.assertTrue(run[1].equals(3), "the centre names the run of 3 ducts, got " + run[1]);
            helper.succeed();
        });
    }

    /** A fluid side on While unpowered under a duct that a lever powers reports paused, and switched off reports switched off. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsPausedSide(GameTestHelper helper) {
        helper.setBlock(DUCT_B.north(), Blocks.CAULDRON.defaultBlockState());
        helper.setBlock(DUCT_B.south(), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, true));
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.FLUID);
        duct.setRedstone(Line.FLUID, Direction.NORTH, DuctRedstone.UNPOWERED);

        helper.runAfterDelay(5, () -> {
            Object[] fluid = assertKey(helper, DuctReport.of(duct, Direction.NORTH).get(1), "filtered_side");
            Object[] paused = assertKey(helper, (Component) fluid[2], "paused");
            helper.assertTrue(text(paused[0]).equals("gui.servus_foundation.redstone.unpowered"),
                    "the paused redstone setting is While unpowered, got " + text(paused[0]));
            Object[] header = assertKey(helper, DuctReport.of(duct, null).get(0), "duct_centre");
            helper.assertTrue(text(header[0]).equals(KEY + "powered"), "the centre names the duct powered, got " + text(header[0]));
            duct.setOn(Line.FLUID, Direction.NORTH, false);
            Object[] off = assertKey(helper, DuctReport.of(duct, Direction.NORTH).get(1), "filtered_side");
            Object[] switchedOff = assertKey(helper, (Component) off[2], "switched_off");
            helper.assertTrue(text(switchedOff[0]).equals("gui.servus_foundation.redstone.unpowered"),
                    "a switched off side still names its redstone setting, got " + text(switchedOff[0]));
            helper.succeed();
        });
    }

    /** An Out fluid side with fluid priority 4 and lava and water listed reports Out, priority 4 and 2 filter entries. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsFluidArm(GameTestHelper helper) {
        helper.setBlock(DUCT_B.north(), Blocks.CAULDRON.defaultBlockState());
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.FLUID);
        duct.setMode(Line.FLUID, Direction.NORTH, DuctMode.OUT);
        duct.setFluidPriority(Direction.NORTH, 4);
        duct.setFluidFilter(Direction.NORTH, 0, Fluids.LAVA);
        duct.setFluidFilter(Direction.NORTH, 3, Fluids.WATER);

        helper.runAfterDelay(5, () -> {
            Object[] fluid = assertKey(helper, DuctReport.of(duct, Direction.NORTH).get(1), "filtered_side");
            helper.assertTrue(text(fluid[1]).equals("gui.servus_foundation.mode.out"), "the mode is Out, got " + text(fluid[1]));
            helper.assertTrue(fluid[4].equals(4) && fluid[5].equals(2), "priority 4 and 2 filter entries, got " + fluid[4] + " and " + fluid[5]);
            helper.succeed();
        });
    }

    /** A pull in item side listing oak sapling with amount 64 and stick reports 2 filter entries, 1 with an amount. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsAmountEntries(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = chestRow(helper)[1];
        duct.setMode(Line.ITEM, Direction.NORTH, DuctMode.IN_PULL);
        duct.filter(Direction.NORTH).setStackInSlot(0, new ItemStack(Items.OAK_SAPLING));
        duct.filter(Direction.NORTH).setStackInSlot(1, new ItemStack(Items.STICK));
        duct.setItemAmount(Direction.NORTH, 0, 64);

        helper.runAfterDelay(5, () -> {
            Object[] item = assertKey(helper, DuctReport.of(duct, Direction.NORTH).get(1), "filtered_side");
            helper.assertTrue(item[5].equals(2) && item[6].equals(1), "2 filter entries, 1 with an amount, got " + item[5] + " and " + item[6]);
            helper.succeed();
        });
    }

    /** A duct with an item line and a Fast Data Cable reports the tier, its bandwidth and an idle load, on an arm and the centre. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsDataLine(GameTestHelper helper) {
        DataCableItem fast = ServusLogisticsItems.FAST_DATA_CABLE.get();
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, d -> {
            d.addLine(Line.ITEM);
            d.addDataLine(fast.tierId());
        });

        helper.runAfterDelay(5, () -> {
            for (Direction side : new Direction[] {Direction.NORTH, null}) {
                List<Component> report = DuctReport.of(duct, side);
                helper.assertTrue(report.size() == 3, "a header, the item and the data line, got " + report);
                Object[] data = assertKey(helper, report.get(2), "data");
                helper.assertTrue(text(data[1]).equals(fast.getDescriptionId()), "the tier is the Fast Data Cable, got " + text(data[1]));
                int bandwidth = (int) Math.round(duct.dataTier().itemsPerSecond());
                helper.assertTrue(data[2].equals(bandwidth), "the bandwidth is " + bandwidth + ", got " + data[2]);
                helper.assertTrue(data[3].equals(0), "nothing used yet, got " + data[3]);
            }
            helper.succeed();
        });
    }

    /** A right click with the potato, also sneaking, prints the report, opens nothing and changes nothing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoChangesNothing(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = chestRow(helper)[1];
        int iron = channel(helper, "Iron ore");
        duct.setChannel(Line.ITEM, Direction.NORTH, iron);
        duct.setMode(Line.ITEM, Direction.NORTH, DuctMode.OUT);

        helper.runAfterDelay(5, () -> {
            List<Component> chat = new ArrayList<>();
            ServerPlayer player = PlayerClicks.player(helper, DUCT_B, new ItemStack(ServusFoundationItems.DATA_POTATO.get()), chat);
            PlayerClicks.use(helper, player, DUCT_B);
            helper.assertTrue(!chat.isEmpty() && chat.get(0).getContents() instanceof TranslatableContents header
                    && header.getKey().startsWith(KEY + "duct_"), "the click prints the duct report, got " + chat);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "no GUI opens");
            player.setShiftKeyDown(true);
            PlayerClicks.use(helper, player, DUCT_B);
            helper.assertTrue(helper.getBlockState(DUCT_B).is(ServusLogisticsBlocks.UTILITY_DUCT.get()), "sneaking does not dismantle");
            helper.assertTrue(duct.mode(Line.ITEM, Direction.NORTH) == DuctMode.OUT, "the mode is unchanged");
            helper.assertTrue(duct.channel(Line.ITEM, Direction.NORTH) == iron, "the channel is unchanged");
            helper.assertTrue(duct.has(Line.ITEM), "the line stays");
            helper.succeed();
        });
    }

    /** A 3 by 2 by 3 tank with 12,000 mB of water and an empty single tank, the second through a full click. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsTank(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 1, 2);
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(2, 1, 2))) {
            helper.setBlock(pos.immutable(), ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        }
        BlockPos single = new BlockPos(8, 1, 8);
        helper.setBlock(single, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        int cap = TankStatsManager.INSTANCE.capacityPerBlock();

        helper.runAfterDelay(2, () -> {
            FluidView handler = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(min), Direction.UP));
            helper.assertTrue(handler != null && handler.fill(new FluidStack(Fluids.WATER, 12000), false) == 12000, "the tank takes 12,000 mB");
            List<Component> report = TankReport.of(helper.getBlockEntity(min, FluidTankBlockEntity.class));
            Object[] content = assertKey(helper, report.get(0), "message.servus_foundation.fluid_tank.content");
            helper.assertTrue(text(content[0]).equals(Fluids.WATER.getFluidType().getDescriptionId()), "the fluid is water, got " + text(content[0]));
            helper.assertTrue(content[1].equals(12000) && content[2].equals(18 * cap), "12,000 of " + 18 * cap + " mB, got "
                    + content[1] + " of " + content[2]);
            Object[] size = assertKey(helper, report.get(1), KEY + "tank_size");
            helper.assertTrue(Arrays.asList(size).equals(List.of(3, 2, 3)), "the size is 3 x 2 x 3, got " + Arrays.asList(size));

            List<Component> chat = new ArrayList<>();
            PlayerClicks.use(helper, PlayerClicks.player(helper, single, new ItemStack(ServusFoundationItems.DATA_POTATO.get()), chat), single);
            helper.assertTrue(chat.size() == 2, "the click prints two lines, got " + chat);
            Object[] empty = assertKey(helper, chat.get(0), "message.servus_foundation.fluid_tank.empty");
            helper.assertTrue(empty[0].equals(cap), "the capacity is " + cap + ", got " + empty[0]);
            helper.assertTrue(Arrays.asList(assertKey(helper, chat.get(1), KEY + "tank_size")).equals(List.of(1, 1, 1)), "the size is 1 x 1 x 1");
            helper.succeed();
        });
    }

    private static UtilityDuctBlockEntity[] chestRow(GameTestHelper helper) {
        BlockPos[] positions = {DUCT_A, DUCT_B, DUCT_C};
        UtilityDuctBlockEntity[] ducts = new UtilityDuctBlockEntity[positions.length];
        for (int i = 0; i < positions.length; i++) {
            helper.setBlock(positions[i].north(), Blocks.CHEST.defaultBlockState());
            ducts[i] = duct(helper, positions[i], Line.ITEM);
        }
        return ducts;
    }

    /** Asserts the line's lang key, the potato's own keys without their prefix, and returns its arguments. */
    private static Object[] assertKey(GameTestHelper helper, Component line, String key) {
        String full = key.startsWith("message.") ? key : KEY + key;
        helper.assertTrue(line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(full),
                "expected " + full + ", got " + line);
        return ((TranslatableContents) line.getContents()).getArgs();
    }

    /** The lang key of a translatable argument, or the text of a literal one. */
    private static String text(Object argument) {
        Component component = (Component) argument;
        return component.getContents() instanceof TranslatableContents contents ? contents.getKey() : component.getString();
    }
}
