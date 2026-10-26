package io.github.dirnei.mc.servus.shuttles.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.order.OrderBook;
import io.github.dirnei.mc.servus.shuttles.order.RuleThresholds;
import io.github.dirnei.mc.servus.shuttles.order.StockRule;
import io.github.dirnei.mc.servus.shuttles.order.StockRules;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Stock rule caps: Inbound Stations take an item only up to its rule's cap,
 * whatever way it comes in, and the rule shows which station refused. The
 * warehouses have no garage, so taken items wait in the station's buffer.
 */
public final class StockRuleCapGameTests {

    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);
    /** Where {@link MiniWarehouse#withInbound()} puts the Inbound Station, front to the south. */
    private static final BlockPos INBOUND = ORIGIN.east(2).south();
    private static final ItemKey COBBLESTONE = ItemKey.of(new ItemStack(Items.COBBLESTONE));
    private static final ItemKey BREAD = ItemKey.of(new ItemStack(Items.BREAD));
    private static final int WAIT = 60;
    /** Longer than one stock rule interval, with margin. */
    private static final int RULE_WAIT = 160;

    private StockRuleCapGameTests() {
    }

    private static int held(InboundStationBlockEntity station, Item item) {
        return WarehouseFixtures.count(station.buffer(), item);
    }

    /**
     * A warehouse without a garage whose rack holds the stacks, its Inbound
     * Station, and the cap of the key set once the rack is indexed.
     */
    private static void cappedWarehouse(GameTestHelper helper, ItemKey key, int cap, int stocked, Runnable then) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, false);
        warehouse.withInbound();
        int left = stocked;
        while (left > 0) {
            int stack = Math.min(64, left);
            WarehouseFixtures.put(warehouse.rack(), key.stack(stack));
            left -= stack;
        }
        Waits.when(helper, () -> warehouse.controller().network().map(network -> network.index().count(key) == stocked).orElse(false), WAIT,
                () -> "the rack should be indexed with " + stocked, () -> {
                    String check = warehouse.network().setStockRule(key, null, cap);
                    helper.assertTrue(check.equals(RuleThresholds.OK), "the cap is saved, got " + check);
                    then.run();
                });
    }

    private static InboundStationBlockEntity inbound(GameTestHelper helper) {
        return helper.getBlockEntity(INBOUND, InboundStationBlockEntity.class);
    }

    private static WarehouseNetwork network(GameTestHelper helper) {
        return inbound(helper).network().orElseThrow();
    }

    private static RuleRow row(GameTestHelper helper, ItemKey key) {
        return network(helper).ruleRows().stream().filter(row -> ItemKey.of(row.stack()).equals(key)).findFirst().orElseThrow();
    }

    /** A cap only rule for cobblestone with none in stock orders nothing and reads ok, while a Make rule without a CPU reads no CPU. */
    @ServusGameTest(timeoutTicks = 300)
    public static void capOnlyRuleOrdersNothing(GameTestHelper helper) {
        cappedWarehouse(helper, COBBLESTONE, 4096, 0, () -> {
            WarehouseNetwork network = network(helper);
            network.setStockRule(BREAD, 8, 16);
            helper.runAfterDelay(RULE_WAIT, () -> {
                helper.assertTrue(row(helper, COBBLESTONE).state().equals("ok") && !row(helper, COBBLESTONE).hasMake(),
                        "the cap only rule reads ok, got " + row(helper, COBBLESTONE));
                helper.assertTrue(row(helper, BREAD).state().equals("no_cpu"), "the Make rule is checked and finds no CPU, got " + row(helper, BREAD));
                helper.succeed();
            });
        });
    }

    /** A rule saved before caps (Min and Max only) loads as a Make pair without a cap; a cap only rule saves and loads without Max. */
    @ServusGameTest(timeoutTicks = 20)
    public static void oldRuleLoadsAsMakeOnly(GameTestHelper helper) {
        StockRules rules = new StockRules();
        rules.set(new StockRule(COBBLESTONE, null, 4096));
        CompoundTag saved = rules.save(helper.getLevel().registryAccess());
        CompoundTag old = new CompoundTag();
        old.put("Item", saved.getListOrEmpty("Rules").getCompoundOrEmpty(0).getCompoundOrEmpty("Item").copy());
        old.putInt("Min", 8);
        old.putInt("Max", 16);
        ListTag list = saved.getListOrEmpty("Rules").copy();
        list.add(old);
        CompoundTag root = new CompoundTag();
        root.put("Rules", list);
        helper.assertTrue(!saved.getListOrEmpty("Rules").getCompoundOrEmpty(0).contains("Max"), "a cap only rule saves no Max");

        StockRules loaded = new StockRules();
        loaded.load(root, helper.getLevel().registryAccess());
        StockRule rule = loaded.get(COBBLESTONE);
        helper.assertTrue(rule != null && rule.hasMake() && rule.make().min() == 8 && rule.make().max() == 16 && !rule.hasCap(),
                "the old tag loads as keep 8 to 16 without a cap, got " + rule);
        helper.assertTrue(loaded.all().size() == 1, "one rule per item, the later tag wins");
        StockRules capOnly = new StockRules();
        capOnly.load(saved, helper.getLevel().registryAccess());
        StockRule cap = capOnly.get(COBBLESTONE);
        helper.assertTrue(cap != null && !cap.hasMake() && cap.cap() == 4096, "the cap only rule loads with its cap, got " + cap);
        helper.succeed();
    }

    /** Cobblestone capped at 128 with 64 in the rack and 64 waiting in a buffer: the station takes no more. */
    @ServusGameTest(timeoutTicks = 200)
    public static void waitingStacksCount(GameTestHelper helper) {
        cappedWarehouse(helper, COBBLESTONE, 128, 64, () -> {
            InboundStationBlockEntity station = inbound(helper);
            station.buffer().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
            helper.runAfterDelay(5, () -> {
                ItemStack left = Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 10), false);
                helper.assertTrue(left.getCount() == 10, "the buffered stack counts towards the cap, took " + (10 - left.getCount()));
                helper.assertTrue(network(helper).capRoom(COBBLESTONE, 10) == 0, "no room left under the cap");
                helper.succeed();
            });
        });
    }

    /** Cobblestone capped at 100 with 94 in stock: a hopper above the station moves 6 and keeps 58. */
    @ServusGameTest(timeoutTicks = 400)
    public static void hopperStopsAtTheCap(GameTestHelper helper) {
        cappedWarehouse(helper, COBBLESTONE, 100, 94, () -> {
            InboundStationBlockEntity station = inbound(helper);
            helper.setBlock(INBOUND.above(), Blocks.HOPPER.defaultBlockState());
            HopperBlockEntity hopper = helper.getBlockEntity(INBOUND.above(), HopperBlockEntity.class);
            hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
            helper.runAfterDelay(160, () -> {
                helper.assertTrue(held(station, Items.COBBLESTONE) == 6, "6 cobblestone moved in, got " + held(station, Items.COBBLESTONE));
                helper.assertTrue(hopper.getItem(0).getCount() == 58, "58 stay in the hopper, got " + hopper.getItem(0).getCount());
                helper.succeed();
            });
        });
    }

    /** Bread capped at 100 with 90 in stock: a Pull side takes 10 of the 30 in the chest behind it. */
    @ServusGameTest(timeoutTicks = 300)
    public static void pullSideStopsAtTheCap(GameTestHelper helper) {
        cappedWarehouse(helper, BREAD, 100, 90, () -> {
            InboundStationBlockEntity station = inbound(helper);
            BlockPos east = INBOUND.east();
            helper.setBlock(east, Blocks.CHEST.defaultBlockState());
            ChestBlockEntity chest = helper.getBlockEntity(east, ChestBlockEntity.class);
            chest.setItem(0, new ItemStack(Items.BREAD, 30));
            station.sideConfig().set(station.relativeSide(Direction.EAST), SideMode.PULL);
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(held(station, Items.BREAD) == 10, "10 bread moved in, got " + held(station, Items.BREAD));
                helper.assertTrue(chest.countItem(Items.BREAD) == 20, "20 stay in the chest, got " + chest.countItem(Items.BREAD));
                helper.succeed();
            });
        });
    }

    /** At the cap, 20 cobblestone shift clicked into the station GUI go into the buffer. */
    @ServusGameTest(timeoutTicks = 200)
    public static void handInsertIgnoresTheCap(GameTestHelper helper) {
        cappedWarehouse(helper, COBBLESTONE, 100, 100, () -> {
            InboundStationBlockEntity station = inbound(helper);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            InboundStationMenu menu = new InboundStationMenu(1, player.getInventory(), station);
            player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            menu.quickMoveStack(player, menu.inventoryStart() + 27);
            helper.assertTrue(held(station, Items.COBBLESTONE) == 20, "the shift clicked cobblestone is in the buffer, got "
                    + held(station, Items.COBBLESTONE));
            helper.succeed();
        });
    }

    /** At the cobblestone cap, raw iron without a rule still goes in. */
    @ServusGameTest(timeoutTicks = 200)
    public static void otherItemsPass(GameTestHelper helper) {
        cappedWarehouse(helper, COBBLESTONE, 100, 100, () -> {
            InboundStationBlockEntity station = inbound(helper);
            ItemStack refused = Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 5), false);
            ItemStack left = Transfer.insertItem(station.intake(), new ItemStack(Items.RAW_IRON, 10), false);
            helper.assertTrue(refused.getCount() == 5, "the cobblestone is refused");
            helper.assertTrue(left.isEmpty() && held(station, Items.RAW_IRON) == 10, "the raw iron moves in, left " + left);
            helper.succeed();
        });
    }

    /** A station on rails without a controller takes cobblestone as before. */
    @ServusGameTest(timeoutTicks = 100)
    public static void offlineStationTakesAll(GameTestHelper helper) {
        WarehouseFixtures.railOnStone(helper, ORIGIN);
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, ORIGIN.south(), Direction.SOUTH);
        helper.runAfterDelay(5, () -> {
            ItemStack left = Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 64), false);
            helper.assertTrue(left.isEmpty() && held(station, Items.COBBLESTONE) == 64, "the offline station takes all 64, left " + left);
            helper.succeed();
        });
    }

    /**
     * At the cobblestone cap, a duct offering cobblestone from a chest to the
     * station on priority 1 and a chest on priority 0 fills the chest instead.
     */
    @ServusGameTest(timeoutTicks = 300)
    public static void ductFallsBackBelowTheCap(GameTestHelper helper) {
        cappedWarehouse(helper, COBBLESTONE, 100, 100, () -> {
            InboundStationBlockEntity station = inbound(helper);
            BlockPos ductPos = INBOUND.east();
            BlockPos source = ductPos.east();
            BlockPos fallback = ductPos.south();
            helper.setBlock(source, Blocks.CHEST.defaultBlockState());
            helper.setBlock(fallback, Blocks.CHEST.defaultBlockState());
            helper.getBlockEntity(source, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 16));
            UtilityDuctBlockEntity d = duct(helper, ductPos, Line.ITEM);
            d.setMode(Line.ITEM, Direction.EAST, DuctMode.IN_PULL);
            d.setMode(Line.ITEM, Direction.WEST, DuctMode.OUT);
            d.setPriority(Direction.WEST, 1);
            d.setMode(Line.ITEM, Direction.SOUTH, DuctMode.OUT);
            IntSupplier inFallback = () -> helper.getBlockEntity(fallback, ChestBlockEntity.class).countItem(Items.COBBLESTONE);
            Waits.when(helper, () -> inFallback.getAsInt() == 16, 200, () -> "the fallback chest should get all 16, has " + inFallback.getAsInt(), () -> {
                helper.assertTrue(held(station, Items.COBBLESTONE) == 0, "the station takes none above the cap");
                helper.succeed();
            });
        });
    }

    /**
     * Sticks capped at 10 with none in stock while a CPU order for 8 sticks
     * waits for planks: a duct pulling sticks from a chest into an Inbound
     * Station moves only 2, since the order's sticks land in storage too.
     */
    @ServusGameTest(timeoutTicks = 400)
    public static void openOrderCountsTowardsTheCap(GameTestHelper helper) {
        CpuLine.setup(helper, true);
        BlockPos inboundPos = CpuLine.ACCESS.below().south();
        BlockPos ductPos = inboundPos.east();
        BlockPos source = ductPos.east();
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, inboundPos, Direction.SOUTH);
        Waits.when(helper, () -> CpuLine.lineReady(helper) && station.network().isPresent(), WAIT,
                () -> "the line and the station should be ready", () -> {
                    OrderBook.Created order = CpuLine.cpu(helper, CpuLine.CPU_A).book().create(CpuLine.STICK, 8, 0);
                    helper.assertTrue(order.made() && order.order().open(), "the sticks order waits for planks");
                    String check = CpuLine.warehouse(helper).setStockRule(CpuLine.STICK, null, 10);
                    helper.assertTrue(check.equals(RuleThresholds.OK), "the cap is saved, got " + check);
                    helper.setBlock(source, Blocks.CHEST.defaultBlockState());
                    ChestBlockEntity chest = helper.getBlockEntity(source, ChestBlockEntity.class);
                    chest.setItem(0, new ItemStack(Items.STICK, 32));
                    duct(helper, ductPos, Line.ITEM).setMode(Line.ITEM, Direction.EAST, DuctMode.IN_PULL);
                    helper.runAfterDelay(200, () -> {
                        int moved = 32 - chest.countItem(Items.STICK);
                        helper.assertTrue(moved == 2, "2 sticks leave the chest, " + moved + " left it");
                        helper.succeed();
                    });
                });
    }

    /** Runs a {@code /servus_warehouse} command at the warehouse's controller and returns what it said. */
    private static String command(GameTestHelper helper, String command) {
        List<String> lines = new ArrayList<>();
        CommandSource collector = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                lines.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()).withSource(collector);
        BlockPos at = helper.absolutePos(ORIGIN.east(2).north());
        helper.getLevel().getServer().getCommands().performPrefixedCommand(source,
                "servus_warehouse " + command.replace("@", at.getX() + " " + at.getY() + " " + at.getZ()));
        return String.join("\n", lines);
    }

    /** The cap command sets a cap only rule that the rules command lists, uncap removes it, and a cap below the maximum fails. */
    @ServusGameTest(timeoutTicks = 200)
    public static void capCommands(GameTestHelper helper) {
        cappedWarehouse(helper, BREAD, 100, 0, () -> {
            WarehouseNetwork network = network(helper);
            command(helper, "cap @ minecraft:cobblestone 4096");
            StockRule cap = network.stockRules().orElseThrow().get(COBBLESTONE);
            helper.assertTrue(cap != null && !cap.hasMake() && cap.cap() == 4096, "the cap only rule exists, got " + cap);
            String listed = command(helper, "rules @");
            helper.assertTrue(listed.contains("Cobblestone: refuse above 4096"), "the rules list the cap: " + listed);
            command(helper, "uncap @ minecraft:cobblestone");
            helper.assertTrue(network.stockRules().orElseThrow().get(COBBLESTONE) == null, "uncapping a cap only rule removes it");
            command(helper, "rule @ minecraft:stick 8 16");
            String refused = command(helper, "cap @ minecraft:stick 10");
            helper.assertTrue(refused.contains("the cap must not be below the maximum"), "the cap below the maximum fails: " + refused);
            StockRule sticks = network.stockRules().orElseThrow().get(ItemKey.of(new ItemStack(Items.STICK)));
            helper.assertTrue(sticks != null && !sticks.hasCap() && sticks.make().max() == 16, "the stick rule keeps 8 to 16 without a cap, got " + sticks);
            command(helper, "cap @ minecraft:stick 1000");
            command(helper, "rule @ minecraft:stick 4 32");
            StockRule kept = network.stockRules().orElseThrow().get(ItemKey.of(new ItemStack(Items.STICK)));
            helper.assertTrue(kept.cap() == 1000 && kept.make().min() == 4, "the rule command keeps the cap, got " + kept);
            helper.succeed();
        });
    }

    /** At the cap, cobblestone offered to Inbound #1 makes the rule read "refused at Inbound #1"; without refusals it reads ok again. */
    @ServusGameTest(timeoutTicks = 500)
    public static void refusingStateNamesTheStation(GameTestHelper helper) {
        cappedWarehouse(helper, COBBLESTONE, 100, 100, () -> {
            InboundStationBlockEntity station = inbound(helper);
            Waits.when(helper, () -> station.number() > 0, WAIT, () -> "the station should be numbered", () -> {
                Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 5), false);
                Waits.when(helper, () -> row(helper, COBBLESTONE).state().equals("refusing"), RULE_WAIT,
                        () -> "the rule should read refusing, got " + row(helper, COBBLESTONE), () -> {
                            RuleRow row = row(helper, COBBLESTONE);
                            helper.assertTrue(row.detail().getContents() instanceof TranslatableContents contents
                                    && contents.getKey().equals("station.servus_shuttles.label.inbound") && contents.getArgs()[0].equals(1),
                                    "the rule names Inbound #1, got " + row.detail());
                            Waits.when(helper, () -> row(helper, COBBLESTONE).state().equals("ok"), RULE_WAIT,
                                    () -> "the rule should read ok again, got " + row(helper, COBBLESTONE), helper::succeed);
                        });
            });
        });
    }
}
