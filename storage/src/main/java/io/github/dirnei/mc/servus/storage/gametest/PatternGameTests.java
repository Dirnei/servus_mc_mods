package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.pattern.PatternItem;
import io.github.dirnei.mc.servus.storage.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.production.ProductionView;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Patterns, Pattern Storage and the Production Terminal's pattern editor.
 * Layout like the Production Terminal tests: warehouse at (2,1,2), Access
 * Point at (2,1,3), data ducts along y 2 to a Crafting Station at (4,1,2), the
 * terminal at (5,3,2), the CPU cube at x 6 to 8, z 1 to 3, and Pattern
 * Storage on the ducts at (4,3,2) and (3,3,3).
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PatternGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos CRATE = new BlockPos(1, 1, 2);
    private static final BlockPos STATION = new BlockPos(4, 1, 2);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 2);
    private static final BlockPos STORAGE_A = new BlockPos(4, 3, 2);
    private static final BlockPos STORAGE_B = new BlockPos(3, 3, 3);
    private static final BlockPos DUCT = new BlockPos(5, 2, 2);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    private PatternGameTests() {
    }

    private static void setup(GameTestHelper helper, boolean withCpu, boolean twoStorages) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, ACCESS);
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(4, 2, 2), DUCT)) {
            TerminalWiring.dataDuct(helper, pos);
        }
        if (withCpu) {
            for (int part = 0; part < CpuShape.PARTS; part++) {
                CpuShape.Vec offset = CpuShape.offset(part);
                helper.setBlock(CPU.offset(offset.x(), offset.y(), offset.z()), ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
            }
            helper.assertTrue(ServusStorageBlocks.CRAFTING_CPU.get().tryForm(helper.getLevel(), helper.absolutePos(CPU)), "the cube should form");
        }
        helper.setBlock(TERMINAL, ServusStorageBlocks.PRODUCTION_TERMINAL.get().defaultBlockState());
        storage(helper, STORAGE_A);
        if (twoStorages) {
            storage(helper, STORAGE_B);
        }
    }

    private static void storage(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
    }

    private static DataNetwork line(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(DUCT)).orElseThrow();
    }

    private static boolean storagesOnLine(GameTestHelper helper, int count) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(DUCT))
                .map(network -> PatternLibrary.storages(helper.getLevel(), network).size() == count).orElse(false);
    }

    private static ProductionTerminalMenu menu(GameTestHelper helper, Player player) {
        ProductionTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        return new ProductionTerminalMenu(1, player.getInventory(), terminal);
    }

    private static Player playerWithBlanks(GameTestHelper helper, int blanks) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (blanks > 0) {
            player.getInventory().add(new ItemStack(ServusStorageItems.BLANK_PATTERN.get(), blanks));
        }
        return player;
    }

    private static int blanks(Player player) {
        return player.getInventory().countItem(ServusStorageItems.BLANK_PATTERN.get());
    }

    /** Puts a plank above a plank into the editor grid. */
    private static void sticksGrid(ProductionTerminalMenu menu) {
        menu.setGhost(1, new ItemStack(Items.OAK_PLANKS));
        menu.setGhost(4, new ItemStack(Items.OAK_PLANKS));
    }

    /** Puts coal above a stick into the editor grid. */
    private static void torchGrid(ProductionTerminalMenu menu) {
        for (int i = 0; i < EncodedPattern.GRID; i++) {
            menu.setGhost(i, ItemStack.EMPTY);
        }
        menu.setGhost(1, new ItemStack(Items.COAL));
        menu.setGhost(4, new ItemStack(Items.STICK));
    }

    private static EncodedPattern sticksPattern() {
        return StationJobs.sticks();
    }

    /** A Pattern Storage on a data duct is a link member and has the configured slots. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void patternStorageJoinsLine(GameTestHelper helper) {
        setup(helper, false, false);
        TerminalWiring.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the Pattern Storage should join the line", () -> {
            helper.assertTrue(line(helper).dataMembers(DataRole.LINK).stream()
                    .anyMatch(member -> member.pos().equals(helper.absolutePos(STORAGE_A))), "it should be a link member");
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A);
            helper.assertTrue(storage.capacity() == PatternStorageBlockEntity.slots() && storage.capacity() == 9,
                    "9 slots from the data, got " + storage.capacity());
            helper.succeed();
        });
    }

    /** Pattern Storage slots take encoded Patterns only, one per slot. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void patternStorageOnlyEncoded(GameTestHelper helper) {
        storage(helper, STORAGE_A);
        PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A);
        ItemStackHandler slots = storage.patterns();
        helper.assertTrue(slots.insertItem(0, new ItemStack(ServusStorageItems.BLANK_PATTERN.get()), false).getCount() == 1,
                "a Blank Pattern is refused");
        helper.assertTrue(slots.insertItem(0, new ItemStack(Items.STICK), false).getCount() == 1, "a stick is refused");
        helper.assertTrue(slots.insertItem(0, new ItemStack(ServusStorageItems.PATTERN.get()), false).getCount() == 1,
                "a Pattern that is not encoded is refused");
        helper.assertTrue(slots.insertItem(0, PatternItem.of(sticksPattern()), false).isEmpty(), "an encoded Pattern is taken");
        helper.assertTrue(storage.pattern(0).isPresent(), "slot 0 holds it");
        helper.succeed();
    }

    /** The capacity of a line is the slots of all its Pattern Storages. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void patternCapacityOverTwoBlocks(GameTestHelper helper) {
        setup(helper, false, true);
        TerminalWiring.when(helper, () -> storagesOnLine(helper, 2), 40, () -> "both Pattern Storages should join", () -> {
            int capacity = PatternLibrary.capacity(helper.getLevel(), line(helper));
            helper.assertTrue(capacity == 2 * PatternStorageBlockEntity.slots(), "capacity of two blocks, got " + capacity);
            helper.succeed();
        });
    }

    /** Saving a new pattern encodes a Blank Pattern and stores it in the first free slot. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void patternSaveUsesBlank(GameTestHelper helper) {
        setup(helper, false, true);
        Player player = playerWithBlanks(helper, 2);
        TerminalWiring.when(helper, () -> storagesOnLine(helper, 2), 40, () -> "the storages should join", () -> {
            ProductionTerminalMenu menu = menu(helper, player);
            menu.openNew();
            sticksGrid(menu);
            String result = menu.save();
            helper.assertTrue(result.isEmpty(), "the pattern should save, got " + result);
            helper.assertTrue(blanks(player) == 1, "one Blank Pattern used, " + blanks(player) + " left");
            List<PatternLibrary.Entry> entries = PatternLibrary.entries(helper.getLevel(), line(helper));
            helper.assertTrue(entries.size() == 1 && entries.get(0).pattern().result().is(Items.STICK)
                    && entries.get(0).slot() == 0, "one sticks pattern in slot 0, got " + entries);
            BlockPos first = helper.absolutePos(STORAGE_A).asLong() < helper.absolutePos(STORAGE_B).asLong()
                    ? helper.absolutePos(STORAGE_A) : helper.absolutePos(STORAGE_B);
            helper.assertTrue(entries.get(0).storage().equals(first), "it goes to the lowest Pattern Storage");
            helper.assertTrue(!menu.isEditing(), "the editor closes after saving");
            helper.succeed();
        });
    }

    /** Saving refuses empty and unmatched grids, a missing blank, a duplicate and a full line. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void patternSaveRefusals(GameTestHelper helper) {
        setup(helper, false, false);
        Player player = playerWithBlanks(helper, 0);
        TerminalWiring.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            ProductionTerminalMenu menu = menu(helper, player);
            menu.openNew();
            helper.assertTrue(menu.save().equals("empty"), "an empty grid is refused");
            menu.setGhost(0, new ItemStack(Items.COBBLESTONE));
            helper.assertTrue(menu.save().equals("no_match"), "a lone cobblestone matches nothing");
            menu.setGhost(0, ItemStack.EMPTY);
            sticksGrid(menu);
            helper.assertTrue(menu.save().equals("no_blank"), "no Blank Pattern is refused");
            helper.assertTrue(menu.isEditing(), "the editor stays open on a refusal");
            player.getInventory().add(new ItemStack(ServusStorageItems.BLANK_PATTERN.get(), 3));
            helper.assertTrue(menu.save().isEmpty(), "with a blank it saves");
            menu.openNew();
            sticksGrid(menu);
            helper.assertTrue(menu.save().equals("duplicate"), "the same pattern again is a duplicate");
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A);
            for (int slot = 1; slot < storage.capacity(); slot++) {
                storage.set(slot, sticksPattern());
            }
            torchGrid(menu);
            helper.assertTrue(menu.save().equals("full"), "a full line is refused");
            helper.assertTrue(blanks(player) == 2, "a refused save keeps the blank, " + blanks(player) + " left");
            helper.succeed();
        });
    }

    /** Editing rewrites a pattern in place without a blank; delete gives the blank back. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void patternEditAndDelete(GameTestHelper helper) {
        setup(helper, false, false);
        Player player = playerWithBlanks(helper, 0);
        TerminalWiring.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A);
            storage.set(0, sticksPattern());
            ProductionTerminalMenu menu = menu(helper, player);
            helper.assertTrue(menu.openExisting(helper.absolutePos(STORAGE_A), 0), "the pattern should open");
            helper.assertTrue(menu.gridCells().get(1).is(Items.OAK_PLANKS) && menu.gridCells().get(4).is(Items.OAK_PLANKS),
                    "the grid shows the sticks layout");
            torchGrid(menu);
            String result = menu.save();
            helper.assertTrue(result.isEmpty(), "editing saves without a blank, got " + result);
            helper.assertTrue(storage.pattern(0).map(p -> p.result().is(Items.TORCH)).orElse(false), "slot 0 now makes torches");
            helper.assertTrue(menu.openExisting(helper.absolutePos(STORAGE_A), 0), "the torch pattern should open");
            helper.assertTrue(menu.delete(), "delete should go through");
            helper.assertTrue(storage.pattern(0).isEmpty(), "the slot is empty");
            helper.assertTrue(blanks(player) == 1, "the player gets a Blank Pattern back");
            helper.succeed();
        });
    }

    /** The CPU orders sticks from a pattern any station can run, and not when no station is on the line. */
    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void patternCpuPlansWithStation(GameTestHelper helper) {
        setup(helper, true, false);
        TerminalWiring.when(helper, () -> storagesOnLine(helper, 1)
                && CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 60, () -> "storage and CPU should join", () -> {
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A);
            storage.set(0, sticksPattern());
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            List<OrderStep> steps = placed.created().order().steps();
            helper.assertTrue(steps.size() == 1 && steps.get(0).producer().equals(helper.absolutePos(STATION)),
                    "one step on the station, got " + steps);
            CpuBlockEntity cpu = CpuBlock.master(helper.getLevel(), helper.absolutePos(CPU));
            cpu.book().cancel(placed.created().order().id());
            helper.setBlock(STATION, Blocks.STONE.defaultBlockState());
            TerminalWiring.when(helper, () -> line(helper).dataMembers(DataRole.PRODUCER).isEmpty(), 20,
                    () -> "the station should leave the line", () -> {
                CpuOrders.Placed none = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
                helper.assertTrue(!none.made() && none.refusal().equals("no_producer"),
                        "without a station the pattern makes nothing orderable, got " + none.refusal());
                helper.succeed();
            });
        });
    }

    /** The terminal view lists the patterns of the line and the capacity. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void patternViewListsPatterns(GameTestHelper helper) {
        setup(helper, false, false);
        TerminalWiring.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A);
            storage.set(2, sticksPattern());
            ProductionPayload view = ProductionView.of(helper.getLevel(), helper.absolutePos(TERMINAL));
            helper.assertTrue(view.patterns().used() == 1 && view.patterns().total() == storage.capacity(),
                    "1 of " + storage.capacity() + ", got " + view.patterns().used() + " of " + view.patterns().total());
            var row = view.patterns().rows().get(0);
            helper.assertTrue(row.storage().equals(helper.absolutePos(STORAGE_A)) && row.slot() == 2
                            && row.result().is(Items.STICK) && row.result().getCount() == 4
                            && row.inputs().size() == 1 && row.inputs().get(0).is(Items.OAK_PLANKS) && row.inputs().get(0).getCount() == 2,
                    "the sticks row with 2 planks, got " + row);
            helper.succeed();
        });
    }
}
