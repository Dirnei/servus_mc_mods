package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.pattern.StoredPatterns;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

/** Pattern results as the recipe really crafts them: wood from the planks, old patterns refreshed, orders that arrive. */
public final class PatternResultGameTests {

    private static final String EMPTY = "empty_12x6x12";
    /** Where {@link StationHandsGameTests#setup} puts the Packing Station and the Pattern Storage. */
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos PATTERNS = new BlockPos(4, 3, 2);
    private static final BlockPos LONE = new BlockPos(10, 1, 10);
    /** The second rack of {@link StationHandsGameTests#setup}, for extra stock. */
    private static final BlockPos EXTRA = new BlockPos(1, 0, 3);
    private static final Identifier RACK_RECIPE = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "heavy_rack");
    private static final Identifier STORAGE_RECIPE = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "pattern_storage");

    private PatternResultGameTests() {
    }

    /** Iron above and below, oak planks in the middle row: one Heavy Duty Rack. */
    private static EncodedPattern rack(ItemStack result) {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack oak = new ItemStack(Items.OAK_PLANKS);
        return new EncodedPattern(RACK_RECIPE, List.of(iron, ItemStack.EMPTY, iron, oak, oak, oak, iron, ItemStack.EMPTY, iron), result);
    }

    /** Iron, paper, iron / redstone, chest, redstone / three birch planks: one Pattern Storage. */
    private static EncodedPattern birchStorage() {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack red = new ItemStack(Items.REDSTONE);
        ItemStack birch = new ItemStack(Items.BIRCH_PLANKS);
        return new EncodedPattern(STORAGE_RECIPE, List.of(iron, new ItemStack(Items.PAPER), iron, red, new ItemStack(Items.CHEST), red, birch, birch, birch),
                new ItemStack(ServusStorageItems.PATTERN_STORAGE.get()));
    }

    private static PatternStorageBlockEntity lone(GameTestHelper helper) {
        helper.setBlock(LONE, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(LONE, PatternStorageBlockEntity.class);
    }

    private static boolean ready(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(TerminalWiring.terminalPos(ACCESS)))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 2 && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    private static StorageTerminalMenu menu(GameTestHelper helper, Player player) {
        return new StorageTerminalMenu(1, player.getInventory(), TerminalWiring.at(helper, ACCESS));
    }

    private static boolean stocked(GameTestHelper helper, Player player, net.minecraft.world.item.Item item) {
        return ready(helper) && menu(helper, player).stock().entries().stream().anyMatch(entry -> entry.stack().is(item));
    }

    /** Puts extra stock into the second rack. */
    private static void extra(GameTestHelper helper, List<ItemStack> stacks) {
        WarehouseFixtures.stock(helper, EXTRA, stacks.toArray(ItemStack[]::new));
    }

    /** A saved rack pattern carries the wood of its oak planks. */
    @ServusGameTest(timeoutTicks = 20)
    public static void rackPatternResultHasWood(GameTestHelper helper) {
        PatternStorageBlockEntity storage = lone(helper);
        storage.set(0, rack(new ItemStack(ServusStorageItems.HEAVY_RACK.get())));
        ItemStack result = storage.pattern(0).orElseThrow().result();
        helper.assertTrue(result.is(ServusStorageItems.HEAVY_RACK.get())
                        && Identifier.withDefaultNamespace("oak_planks").equals(result.get(ServusStorageComponents.RACK_WOOD.get())),
                "the rack pattern result should carry oak wood, got " + result.getComponents());
        helper.succeed();
    }

    /** A pattern kept with the plain recipe result gets the assembled result once its storage is read. */
    @ServusGameTest(timeoutTicks = 20)
    public static void oldPatternResultRefreshed(GameTestHelper helper) {
        PatternStorageBlockEntity storage = lone(helper);
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = storage.saveWithFullMetadata(registries);
        StoredPatterns old = new StoredPatterns(List.of(new StoredPatterns.Entry(0, rack(new ItemStack(ServusStorageItems.HEAVY_RACK.get())))));
        tag.put("Patterns", StoredPatterns.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), old).getOrThrow());
        storage.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        ItemStack result = storage.pattern(0).orElseThrow().result();
        helper.assertTrue(result.has(ServusStorageComponents.RACK_WOOD.get()), "the old pattern should be refreshed to the oak rack");
        helper.succeed();
    }

    /** Two racks requested through the CPU from an oak rack pattern arrive in the terminal. */
    @ServusGameTest(timeoutTicks = 1600)
    public static void rackRequestArrives(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        extra(helper, List.of(new ItemStack(Items.IRON_INGOT, 16)));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, rack(new ItemStack(ServusStorageItems.HEAVY_RACK.get())));
        ItemKey rack = ItemKey.of(patterns.pattern(1).orElseThrow().result());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Waits.when(helper, () -> stocked(helper, player, Items.IRON_INGOT), 160, () -> "stations, CPU and stock should be ready", () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            menu(helper, player).craft(rack, 2);
            Waits.when(helper, () -> terminal.outputCount(ServusStorageItems.HEAVY_RACK.get()) >= 2, 1400,
                    () -> "2 racks should land in the output slots, got " + terminal.outputCount(ServusStorageItems.HEAVY_RACK.get()),
                    helper::succeed);
        });
    }

    /** A Pattern Storage crafted from birch planks through the CPU arrives with birch wood. */
    @ServusGameTest(timeoutTicks = 1600)
    public static void woodBlockRequestArrives(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        extra(helper, List.of(new ItemStack(Items.IRON_INGOT, 8), new ItemStack(Items.PAPER, 4),
                new ItemStack(Items.REDSTONE, 8), new ItemStack(Items.CHEST, 4)));
        extra(helper, List.of(new ItemStack(Items.BIRCH_PLANKS, 16)));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, birchStorage());
        ItemStack expected = patterns.pattern(1).orElseThrow().result();
        helper.assertTrue(Identifier.withDefaultNamespace("birch_planks").equals(expected.get(WoodContent.WOOD.get())),
                "the pattern result should carry birch wood, got " + expected.getComponents());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Waits.when(helper, () -> stocked(helper, player, Items.BIRCH_PLANKS) && stocked(helper, player, Items.CHEST)
                && stocked(helper, player, Items.PAPER) && stocked(helper, player, Items.REDSTONE), 160, () -> "stations, CPU and stock should be ready", () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            var preview = io.github.dirnei.mc.servus.storage.terminal.TerminalCraftables.preview(helper.getLevel(),
                    terminal.getBlockPos(), ItemKey.of(expected), 1);
            helper.assertTrue(preview.refusal().isEmpty() && preview.missing().isEmpty(),
                    "the craft should be possible, refusal '" + preview.refusal() + "' missing " + preview.missing()
                            + " from stock " + preview.fromStock() + " to craft " + preview.toCraft());
            menu(helper, player).craft(ItemKey.of(expected), 1);
            Waits.when(helper, () -> terminal.outputCount(ServusStorageItems.PATTERN_STORAGE.get()) >= 1, 1400,
                    () -> "the Pattern Storage should land in the output slots, requests " + terminal.requests()
                            + " orders " + CpuOrders.cpus(helper.getLevel(), StationHandsGameTests.line(helper)).stream()
                            .map(cpu -> cpu.book().orders().stream().map(o -> o.state() + ":" + o.reason() + " missing " + o.missing()).toList())
                            .toList(), helper::succeed);
        });
    }
}
