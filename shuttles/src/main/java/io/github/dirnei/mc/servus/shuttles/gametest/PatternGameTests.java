package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlock;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.order.OrderStep;
import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternEditing;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternVariants;
import io.github.dirnei.mc.servus.shuttles.production.ProductionPayload;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows;
import io.github.dirnei.mc.servus.shuttles.production.ProductionTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.production.ProductionView;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalWarehouses;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Patterns, Pattern Storage and the Production Terminal's pattern editor.
 * Layout like the Production Terminal tests: warehouse at (2,1,2), Packing
 * Station at (2,1,3), data ducts along y 2 to a Crafting Station at (4,1,2), the
 * terminal at (5,3,2), the CPU cube at x 6 to 8, z 1 to 3, and Pattern
 * Storage on the ducts at (4,3,2) and (3,3,3).
 */
public final class PatternGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos STATION = new BlockPos(4, 1, 2);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 2);
    private static final BlockPos STORAGE_A = new BlockPos(4, 3, 2);
    private static final BlockPos STORAGE_B = new BlockPos(3, 3, 3);
    private static final BlockPos DUCT = new BlockPos(5, 2, 2);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));
    /** The rack west of the rail under the controller. */
    private static final BlockPos RACK = new BlockPos(1, 0, 2);
    private static final Identifier RACK_RECIPE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "heavy_rack");

    private PatternGameTests() {
    }

    private static void setup(GameTestHelper helper, boolean withCpu, boolean twoStorages) {
        MiniWarehouse.under(helper, CONTROLLER);
        TerminalWiring.place(helper, ACCESS);
        StationJobs.craftingStation(helper, STATION);
        for (BlockPos pos : CpuLine.DUCTS) {
            TerminalWiring.dataDuct(helper, pos);
        }
        if (withCpu) {
            CpuBuild.formCube(helper, CPU);
        }
        helper.setBlock(TERMINAL, ServusShuttlesBlocks.PRODUCTION_TERMINAL.get().defaultBlockState());
        storage(helper, STORAGE_A);
        if (twoStorages) {
            storage(helper, STORAGE_B);
        }
    }

    private static void storage(GameTestHelper helper, BlockPos pos) {
        StationJobs.patternStorage(helper, pos);
    }

    private static DataNetwork line(GameTestHelper helper) {
        return DataLines.at(helper, DUCT);
    }

    private static boolean storagesOnLine(GameTestHelper helper, int count) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(DUCT))
                .map(network -> PatternLibrary.storages(helper.getLevel(), network).size() == count).orElse(false);
    }

    private static ProductionTerminalMenu menu(GameTestHelper helper, Player player) {
        ProductionTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL, ProductionTerminalBlockEntity.class);
        return new ProductionTerminalMenu(1, player.getInventory(), terminal);
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
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
    @ServusGameTest(timeoutTicks = 60)
    public static void patternStorageJoinsLine(GameTestHelper helper) {
        setup(helper, false, false);
        Waits.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the Pattern Storage should join the line", () -> {
            helper.assertTrue(line(helper).dataMembers(DataRole.LINK).stream()
                    .anyMatch(member -> member.pos().equals(helper.absolutePos(STORAGE_A))), "it should be a link member");
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A, PatternStorageBlockEntity.class);
            helper.assertTrue(storage.capacity() == PatternStorageBlockEntity.slots() && storage.capacity() == 9,
                    "9 slots from the data, got " + storage.capacity());
            helper.succeed();
        });
    }

    /** Pattern Storage keeps patterns as data by place and has no item handler. */
    @ServusGameTest(timeoutTicks = 20)
    public static void patternStorageKeepsData(GameTestHelper helper) {
        storage(helper, STORAGE_A);
        PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A, PatternStorageBlockEntity.class);
        helper.assertTrue(storage.firstFree() == 0 && storage.usedSlots().isEmpty(), "a new storage is empty");
        storage.set(0, sticksPattern());
        storage.set(2, StationJobs.planks());
        helper.assertTrue(storage.usedSlots().equals(List.of(0, 2)) && storage.firstFree() == 1, "places 0 and 2 are used");
        storage.clear(0);
        helper.assertTrue(storage.pattern(0).isEmpty() && storage.firstFree() == 0, "place 0 is free again");
        helper.assertTrue(ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(STORAGE_A), Direction.UP)) == null,
                "no item handler");
        helper.succeed();
    }

    /** Mining a Pattern Storage drops one item that carries its patterns; placing it restores them. */
    @ServusGameTest(timeoutTicks = 20)
    public static void patternStorageTravelsWithBlock(GameTestHelper helper) {
        storage(helper, STORAGE_A);
        PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A, PatternStorageBlockEntity.class);
        storage.set(0, sticksPattern());
        storage.set(3, StationJobs.planks());
        BlockPos at = helper.absolutePos(STORAGE_A);
        List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(at), helper.getLevel(), at, storage, null,
                new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ServusShuttlesItems.PATTERN_STORAGE.get()),
                "one Pattern Storage item drops, got " + drops);
        ItemStack item = drops.get(0);
        helper.assertTrue(item.has(ServusShuttlesComponents.PATTERNS.get()), "the item carries the patterns");
        helper.setBlock(STORAGE_A, Blocks.AIR.defaultBlockState());
        storage(helper, STORAGE_B);
        PatternStorageBlockEntity placed = helper.getBlockEntity(STORAGE_B, PatternStorageBlockEntity.class);
        placed.applyComponentsFromItemStack(item);
        helper.assertTrue(placed.usedSlots().equals(List.of(0, 3))
                        && placed.pattern(0).map(p -> p.result().is(Items.STICK)).orElse(false)
                        && placed.pattern(3).map(p -> p.result().is(Items.OAK_PLANKS)).orElse(false),
                "the placed block holds sticks at 0 and planks at 3, got " + placed.usedSlots());
        placed.clear(0);
        placed.clear(3);
        BlockPos other = helper.absolutePos(STORAGE_B);
        List<ItemStack> plain = Block.getDrops(helper.getLevel().getBlockState(other), helper.getLevel(), other, placed, null,
                new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(plain.size() == 1 && !plain.get(0).has(ServusShuttlesComponents.PATTERNS.get()),
                "an empty storage drops a plain item");
        helper.succeed();
    }

    /** The capacity of a line is the slots of all its Pattern Storages. */
    @ServusGameTest(timeoutTicks = 60)
    public static void patternCapacityOverTwoBlocks(GameTestHelper helper) {
        setup(helper, false, true);
        Waits.when(helper, () -> storagesOnLine(helper, 2), 40, () -> "both Pattern Storages should join", () -> {
            int capacity = PatternLibrary.capacity(helper.getLevel(), line(helper));
            helper.assertTrue(capacity == 2 * PatternStorageBlockEntity.slots(), "capacity of two blocks, got " + capacity);
            helper.succeed();
        });
    }

    /** Saving a new pattern stores it in the first free place without any item. */
    @ServusGameTest(timeoutTicks = 60)
    public static void patternSaveWithoutItems(GameTestHelper helper) {
        setup(helper, false, true);
        Player player = player(helper);
        Waits.when(helper, () -> storagesOnLine(helper, 2), 40, () -> "the storages should join", () -> {
            ProductionTerminalMenu menu = menu(helper, player);
            menu.openNew();
            sticksGrid(menu);
            String result = menu.save();
            helper.assertTrue(result.isEmpty(), "the pattern should save, got " + result);
            helper.assertTrue(player.getInventory().isEmpty(), "the inventory is untouched");
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

    /** Saving refuses empty and unmatched grids, a duplicate and a full line. */
    @ServusGameTest(timeoutTicks = 60)
    public static void patternSaveRefusals(GameTestHelper helper) {
        setup(helper, false, false);
        Player player = player(helper);
        Waits.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            ProductionTerminalMenu menu = menu(helper, player);
            menu.openNew();
            helper.assertTrue(menu.save().equals("empty"), "an empty grid is refused");
            menu.setGhost(0, new ItemStack(Items.COBBLESTONE));
            helper.assertTrue(menu.save().equals("no_match"), "a lone cobblestone matches nothing");
            menu.setGhost(0, ItemStack.EMPTY);
            sticksGrid(menu);
            helper.assertTrue(menu.save().isEmpty(), "an empty inventory saves");
            menu.openNew();
            sticksGrid(menu);
            helper.assertTrue(menu.save().equals("duplicate"), "the same pattern again is a duplicate");
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A, PatternStorageBlockEntity.class);
            for (int slot = 1; slot < storage.capacity(); slot++) {
                storage.set(slot, sticksPattern());
            }
            torchGrid(menu);
            helper.assertTrue(menu.save().equals("full"), "a full line is refused");
            helper.assertTrue(menu.isEditing(), "the editor stays open on a refusal");
            helper.succeed();
        });
    }

    /** Saving with no Pattern Storage on the line is refused. */
    @ServusGameTest(timeoutTicks = 60)
    public static void patternSaveNoStorage(GameTestHelper helper) {
        setup(helper, false, false);
        Player player = player(helper);
        Waits.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            helper.setBlock(STORAGE_A, Blocks.AIR.defaultBlockState());
            Waits.when(helper, () -> storagesOnLine(helper, 0), 20, () -> "the storage should leave", () -> {
                ProductionTerminalMenu menu = menu(helper, player);
                menu.openNew();
                sticksGrid(menu);
                helper.assertTrue(menu.save().equals("no_storage"), "no Pattern Storage is refused");
                helper.succeed();
            });
        });
    }

    /** Editing rewrites a pattern in place; delete removes it and gives nothing. */
    @ServusGameTest(timeoutTicks = 60)
    public static void patternEditAndDelete(GameTestHelper helper) {
        setup(helper, false, false);
        Player player = player(helper);
        Waits.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A, PatternStorageBlockEntity.class);
            storage.set(0, sticksPattern());
            ProductionTerminalMenu menu = menu(helper, player);
            helper.assertTrue(menu.openExisting(helper.absolutePos(STORAGE_A), 0), "the pattern should open");
            helper.assertTrue(menu.gridCells().get(1).is(Items.OAK_PLANKS) && menu.gridCells().get(4).is(Items.OAK_PLANKS),
                    "the grid shows the sticks layout");
            torchGrid(menu);
            String result = menu.save();
            helper.assertTrue(result.isEmpty(), "editing saves, got " + result);
            helper.assertTrue(storage.pattern(0).map(p -> p.result().is(Items.TORCH)).orElse(false), "slot 0 now makes torches");
            helper.assertTrue(menu.openExisting(helper.absolutePos(STORAGE_A), 0), "the torch pattern should open");
            helper.assertTrue(menu.delete(), "delete should go through");
            helper.assertTrue(storage.pattern(0).isEmpty(), "the slot is empty");
            helper.assertTrue(player.getInventory().isEmpty(), "the player gets nothing");
            helper.succeed();
        });
    }

    /** The CPU orders sticks from a pattern any station can run, and not when no station is on the line. */
    @ServusGameTest(timeoutTicks = 80)
    public static void patternCpuPlansWithStation(GameTestHelper helper) {
        setup(helper, true, false);
        Waits.when(helper, () -> storagesOnLine(helper, 1)
                && CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 60, () -> "storage and CPU should join", () -> {
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A, PatternStorageBlockEntity.class);
            storage.set(0, sticksPattern());
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            List<OrderStep> steps = placed.created().order().steps();
            helper.assertTrue(steps.size() == 1 && steps.get(0).producer().equals(helper.absolutePos(STATION)),
                    "one step on the station, got " + steps);
            CpuBlockEntity cpu = CpuBlock.master(helper.getLevel(), helper.absolutePos(CPU));
            cpu.book().cancel(placed.created().order().id());
            helper.setBlock(STATION, Blocks.STONE.defaultBlockState());
            Waits.when(helper, () -> line(helper).dataMembers(DataRole.PRODUCER).isEmpty(), 20,
                    () -> "the station should leave the line", () -> {
                CpuOrders.Placed none = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
                helper.assertTrue(!none.made() && none.refusal().equals("no_producer"),
                        "without a station the pattern makes nothing orderable, got " + none.refusal());
                helper.succeed();
            });
        });
    }

    /** The terminal view lists the patterns of the line and the capacity. */
    @ServusGameTest(timeoutTicks = 60)
    public static void patternViewListsPatterns(GameTestHelper helper) {
        setup(helper, false, false);
        Waits.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            PatternStorageBlockEntity storage = helper.getBlockEntity(STORAGE_A, PatternStorageBlockEntity.class);
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

    /** A pattern saved before modes existed loads as Exact; an Any pattern keeps its mode through saving. */
    @ServusGameTest(timeoutTicks = 20)
    public static void oldPatternLoadsExact(GameTestHelper helper) {
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = EncodedPattern.CODEC.encodeStart(ops, sticksPattern().withMode(EncodedPattern.Mode.ANY)).getOrThrow();
        helper.assertTrue(saved instanceof CompoundTag tag && tag.contains("mode"), "an Any pattern saves its mode");
        EncodedPattern any = EncodedPattern.CODEC.parse(ops, saved).getOrThrow();
        helper.assertTrue(any.any(), "the Any pattern loads as Any");
        CompoundTag old = ((CompoundTag) saved).copy();
        old.remove("mode");
        EncodedPattern loaded = EncodedPattern.CODEC.parse(ops, old).getOrThrow();
        helper.assertTrue(loaded.mode() == EncodedPattern.Mode.EXACT, "a pattern without a mode loads as Exact");
        PatternStorageBlockEntity storage = StationJobs.patternStorage(helper, STORAGE_A);
        storage.set(0, loaded);
        helper.assertTrue(storage.pattern(0).map(pattern -> !pattern.any()).orElse(false), "the storage keeps it Exact");
        helper.succeed();
    }

    /** A new sticks pattern saves as Any, and a chest is Any too; the Heavy Duty Rack stays Exact since its wood follows the planks. */
    @ServusGameTest(timeoutTicks = 60)
    public static void rackStaysExact(GameTestHelper helper) {
        setup(helper, false, false);
        Player player = player(helper);
        Waits.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            ProductionTerminalMenu menu = menu(helper, player);
            menu.openNew();
            helper.assertTrue(menu.mode() == EncodedPattern.Mode.ANY, "a new pattern asks for Any");
            sticksGrid(menu);
            helper.assertTrue(menu.save().isEmpty(), "the sticks pattern saves");
            menu.openEditorWith(RACK_RECIPE);
            List<ItemStack> rack = menu.gridCells();
            EncodedPattern encoded = PatternEditing.encode(helper.getLevel(), rack).orElseThrow();
            PatternVariants variants = PatternVariants.of(helper.getLevel(), encoded);
            helper.assertTrue(variants.verdict() == PatternVariants.Verdict.CHANGES_RESULT,
                    "the rack's result changes with its planks, got " + variants.verdict());
            helper.assertTrue(menu.save().isEmpty(), "the rack pattern saves");
            ItemStack planks = new ItemStack(Items.OAK_PLANKS);
            EncodedPattern chest = PatternEditing.encode(helper.getLevel(),
                    List.of(planks, planks, planks, planks, ItemStack.EMPTY, planks, planks, planks, planks)).orElseThrow();
            helper.assertTrue(PatternVariants.of(helper.getLevel(), chest).possible(), "a chest from any planks is the same chest");
            List<PatternLibrary.Entry> entries = PatternLibrary.entries(helper.getLevel(), line(helper));
            helper.assertTrue(entries.stream().anyMatch(entry -> entry.pattern().result().is(Items.STICK) && entry.pattern().any()),
                    "the sticks pattern is Any, got " + entries);
            helper.assertTrue(entries.stream().anyMatch(entry -> entry.pattern().recipe().equals(RACK_RECIPE) && !entry.pattern().any()),
                    "the rack pattern is Exact, got " + entries);
            ProductionPayload view = ProductionView.of(helper.getLevel(), helper.absolutePos(TERMINAL));
            helper.assertTrue(view.patterns().rows().stream().filter(row -> row.result().is(Items.STICK))
                            .allMatch(row -> row.isAny() && row.any().get(0).tagKey().equals("tag.item.minecraft.planks")),
                    "the Patterns tab marks the sticks as Any planks");
            helper.assertTrue(view.patterns().rows().stream().filter(row -> !row.result().is(Items.STICK)).noneMatch(ProductionRows.PatternRow::isAny),
                    "the rack is not marked");
            helper.succeed();
        });
    }

    /** A second Any sticks pattern is a duplicate even with other planks; an Exact one with other planks is not. */
    @ServusGameTest(timeoutTicks = 60)
    public static void secondAnySticksRefused(GameTestHelper helper) {
        setup(helper, false, false);
        Player player = player(helper);
        Waits.when(helper, () -> storagesOnLine(helper, 1), 40, () -> "the storage should join", () -> {
            ProductionTerminalMenu menu = menu(helper, player);
            menu.openNew();
            sticksGrid(menu);
            helper.assertTrue(menu.save().isEmpty(), "the oak sticks pattern saves as Any");
            menu.openNew();
            menu.setGhost(1, new ItemStack(Items.BIRCH_PLANKS));
            menu.setGhost(4, new ItemStack(Items.BIRCH_PLANKS));
            helper.assertTrue(menu.save().equals("duplicate"), "a second Any sticks pattern is refused");
            menu.clickMenuButton(player, RecipeEditorButtons.MODE);
            helper.assertTrue(menu.mode() == EncodedPattern.Mode.EXACT, "the mode button switches to Exact");
            helper.assertTrue(menu.save().isEmpty(), "an Exact birch sticks pattern saves");
            helper.assertTrue(PatternLibrary.entries(helper.getLevel(), line(helper)).size() == 2, "two patterns on the line");
            helper.succeed();
        });
    }

    /** The recipe viewer fills the editor with the planks the warehouse stocks, not the first planks of the tag. */
    @ServusGameTest(timeoutTicks = 200)
    public static void editorFillPicksStockedPlanks(GameTestHelper helper) {
        setup(helper, false, false);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.SPRUCE_PLANKS, 16));
        Player player = player(helper);
        BlockPos terminal = helper.absolutePos(TERMINAL);
        Waits.when(helper, () -> TerminalWarehouses.online(helper.getLevel(), terminal)
                .map(warehouse -> TerminalWarehouses.stock(warehouse).stream().anyMatch(entry -> entry.getKey().item() == Items.SPRUCE_PLANKS))
                .orElse(false), 160, () -> "the warehouse should list the spruce planks", () -> {
            ProductionTerminalMenu menu = menu(helper, player);
            menu.openEditorWith(RACK_RECIPE);
            List<ItemStack> cells = menu.gridCells();
            helper.assertTrue(menu.isEditing(), "the editor opens");
            helper.assertTrue(cells.get(3).is(Items.SPRUCE_PLANKS) && cells.get(4).is(Items.SPRUCE_PLANKS) && cells.get(5).is(Items.SPRUCE_PLANKS),
                    "the middle row holds the stocked spruce planks, got " + cells);
            helper.assertTrue(cells.get(0).is(Items.IRON_INGOT), "the unstocked iron takes the first item, got " + cells);
            helper.succeed();
        });
    }
}
