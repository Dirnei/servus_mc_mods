package io.github.dirnei.mc.servus.foundation.machine.generator;

import com.mojang.authlib.GameProfile;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.TwoHalves;
import io.github.dirnei.mc.servus.foundation.machine.menu.CoalGeneratorMenu;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpGameTests;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.EnergyView;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

/** The Coal Generator as a cube with a chimney: placing, breaking, the closed chimney and the open cube. */
public final class CoalGeneratorGameTests {

    private static final BlockPos GEN = new BlockPos(5, 2, 5);

    private CoalGeneratorGameTests() {
    }

    /**
     * Both halves of a Coal Generator; {@code setBlock} skips {@code setPlacedBy},
     * which would set the chimney. Other tests build generators with this.
     */
    public static void place(GameTestHelper helper, BlockPos pos, Direction facing) {
        CoalGeneratorBlock block = MachineContent.COAL_GENERATOR.get();
        BlockState lower = block.defaultBlockState().setValue(MachineBlock.FACING, facing);
        helper.setBlock(pos, lower);
        helper.setBlock(pos.above(), block.upperOf(lower));
    }

    private static boolean isHalf(GameTestHelper helper, BlockPos pos, DoubleBlockHalf half) {
        BlockState state = helper.getBlockState(pos);
        return state.is(MachineContent.COAL_GENERATOR.get()) && state.getValue(TwoHalves.HALF) == half;
    }

    private static ItemStack placeWithItem(GameTestHelper helper, BlockPos pos) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(MachineContent.COAL_GENERATOR_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos below = helper.absolutePos(pos.below());
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false)));
        return stack;
    }

    private static List<ItemEntity> drops(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2));
    }

    private static int count(List<ItemEntity> drops, Item item) {
        return drops.stream().filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum();
    }

    private static boolean gone(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).isAir() && helper.getBlockState(pos.above()).isAir();
    }

    /** One item fills the clicked spot and the block above; only the cube is a machine. */
    @ServusGameTest(timeoutTicks = 20)
    public static void placesTwoHalves(GameTestHelper helper) {
        helper.setBlock(GEN.below(), Blocks.STONE.defaultBlockState());
        ItemStack left = placeWithItem(helper, GEN);
        helper.assertTrue(left.isEmpty(), "the item is used up");
        helper.assertTrue(isHalf(helper, GEN, DoubleBlockHalf.LOWER), "the cube is placed");
        helper.assertTrue(isHalf(helper, GEN.above(), DoubleBlockHalf.UPPER), "the chimney is placed above");
        helper.assertTrue(helper.getBlockState(GEN).getValue(MachineBlock.FACING) == helper.getBlockState(GEN.above()).getValue(MachineBlock.FACING),
                "both halves face the same way");
        helper.assertTrue(helper.getBlockEntity(GEN, CoalGeneratorBlockEntity.class) != null, "the cube holds the machine");
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(GEN.above())) == null, "the chimney has no block entity");
        helper.succeed();
    }

    /** Without room above nothing is placed and the item stays. */
    @ServusGameTest(timeoutTicks = 20)
    public static void noRoomAbove(GameTestHelper helper) {
        helper.setBlock(GEN.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(GEN.above(), Blocks.STONE.defaultBlockState());
        ItemStack left = placeWithItem(helper, GEN);
        helper.assertTrue(left.getCount() == 1, "the item stays in the hand");
        helper.assertTrue(helper.getBlockState(GEN).isAir(), "nothing is placed");
        helper.succeed();
    }

    /** Breaking the chimney in survival removes both halves and drops one generator and its coal. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakChimneyDropsOneAndFuel(GameTestHelper helper) {
        place(helper, GEN, Direction.NORTH);
        helper.getBlockEntity(GEN, CoalGeneratorBlockEntity.class).inventory()
                .setStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 5));
        helper.getLevel().destroyBlock(helper.absolutePos(GEN.above()), true);
        helper.assertTrue(gone(helper, GEN), "both halves are gone");
        List<ItemEntity> drops = drops(helper, GEN);
        helper.assertTrue(count(drops, MachineContent.COAL_GENERATOR_ITEM.get()) == 1, "exactly one Coal Generator drops");
        helper.assertTrue(count(drops, Items.COAL) == 5, "the coal drops");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    /** A creative player breaking either half removes both and gets no generator. */
    @ServusGameTest(timeoutTicks = 20)
    public static void creativeDropsNothing(GameTestHelper helper) {
        BlockPos second = GEN.east(3);
        place(helper, GEN, Direction.NORTH);
        place(helper, second, Direction.NORTH);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        for (BlockPos broken : new BlockPos[] {GEN.above(), second}) {
            BlockPos pos = helper.absolutePos(broken);
            BlockState state = helper.getLevel().getBlockState(pos);
            state.getBlock().playerWillDestroy(helper.getLevel(), pos, state, player);
            helper.getLevel().removeBlock(pos, false);
        }
        for (BlockPos pos : new BlockPos[] {GEN, second}) {
            helper.assertTrue(gone(helper, pos), "both halves are gone at " + pos);
            helper.assertTrue(count(drops(helper, pos), MachineContent.COAL_GENERATOR_ITEM.get()) == 0, "no generator drops at " + pos);
        }
        helper.succeed();
    }

    private static void carrot(GameTestHelper helper, Player player, BlockPos at, boolean sneak) {
        player.setShiftKeyDown(sneak);
        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        BlockPos pos = helper.absolutePos(at);
        carrot.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, carrot,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false)));
        player.setShiftKeyDown(false);
    }

    /** The carrot on the chimney does nothing; sneaking with it dismantles the whole generator into one item. */
    @ServusGameTest(timeoutTicks = 20)
    public static void carrotOnChimneyDismantles(GameTestHelper helper) {
        place(helper, GEN, Direction.NORTH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        CoalGeneratorBlockEntity generator = helper.getBlockEntity(GEN, CoalGeneratorBlockEntity.class);
        var before = generator.sideMode(Direction.EAST);
        carrot(helper, player, GEN.above(), false);
        helper.assertTrue(isHalf(helper, GEN.above(), DoubleBlockHalf.UPPER), "a plain click leaves the chimney");
        helper.assertTrue(generator.sideMode(Direction.EAST) == before, "a plain click on the chimney changes no side");
        carrot(helper, player, GEN.above(), true);
        helper.assertTrue(gone(helper, GEN), "both halves are gone");
        List<ItemEntity> drops = drops(helper, GEN);
        helper.assertTrue(count(drops, MachineContent.COAL_GENERATOR_ITEM.get()) == 1, "exactly one Coal Generator drops");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    /** Pistons pushing against either half move nothing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void pistonMovesNothing(GameTestHelper helper) {
        place(helper, GEN, Direction.NORTH);
        for (BlockPos pos : new BlockPos[] {GEN, GEN.above()}) {
            helper.setBlock(pos.west(), Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
            helper.setBlock(pos.west(2), Blocks.REDSTONE_BLOCK.defaultBlockState());
        }
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(isHalf(helper, GEN, DoubleBlockHalf.LOWER), "the cube stays");
            helper.assertTrue(isHalf(helper, GEN.above(), DoubleBlockHalf.UPPER), "the chimney stays");
            helper.assertTrue(helper.getBlockState(GEN.east()).isAir() && helper.getBlockState(GEN.above().east()).isAir(),
                    "nothing was pushed east");
            helper.succeed();
        });
    }

    /**
     * Right clicking the chimney opens the generator's GUI. A mock server player
     * cannot receive the open screen payload, so a plain player records the menu.
     */
    @ServusGameTest(timeoutTicks = 20)
    public static void chimneyOpensGui(GameTestHelper helper) {
        place(helper, GEN, Direction.NORTH);
        MenuProvider[] opened = new MenuProvider[1];
        BlockPos[] openedAt = new BlockPos[1];
        Player player = new Player(helper.getLevel(), new GameProfile(UUID.randomUUID(), "generator-test")) {
            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }

            @Override
            public OptionalInt openMenu(MenuProvider provider, BlockPos pos) {
                opened[0] = provider;
                openedAt[0] = pos;
                return OptionalInt.of(1);
            }
        };
        BlockPos top = helper.absolutePos(GEN.above());
        helper.getLevel().getBlockState(top).useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(top), Direction.WEST, top, false));
        helper.assertTrue(opened[0] instanceof CoalGeneratorBlockEntity, "the generator opens its GUI, got " + opened[0]);
        helper.assertTrue(helper.absolutePos(GEN).equals(openedAt[0]), "the GUI belongs to the cube");
        helper.assertTrue(opened[0].createMenu(0, player.getInventory(), player) instanceof CoalGeneratorMenu, "its menu is the generator menu");
        helper.succeed();
    }

    /** The chimney offers nothing on any face, and ducts beside it do not connect. */
    @ServusGameTest(timeoutTicks = 20)
    public static void chimneyHasNoCapabilities(GameTestHelper helper) {
        place(helper, GEN, Direction.NORTH);
        BlockPos top = helper.absolutePos(GEN.above());
        for (Direction side : Direction.values()) {
            helper.assertTrue(EnergyView.of(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, top, side)) == null, "no energy on " + side);
            helper.assertTrue(ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, top, side)) == null, "no items on " + side);
            helper.assertTrue(FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, top, side)) == null, "no fluid on " + side);
            helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, top, side) == null, "no upkeep on " + side);
        }
        BlockPos ductPos = GEN.above().east();
        helper.setBlock(ductPos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(ductPos, UtilityDuctBlockEntity.class);
        duct.addLine(Line.ENERGY);
        duct.addLine(Line.ITEM);
        BlockPos absolute = helper.absolutePos(ductPos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        helper.assertFalse(duct.hasArm(Line.ENERGY, Direction.WEST), "no Power Cable arm toward the chimney");
        helper.assertFalse(duct.hasArm(Line.ITEM, Direction.WEST), "no Item Pipe arm toward the chimney");
        helper.succeed();
    }

    /** The cube's top is the chimney: no energy is offered there, and no fuel goes in. */
    @ServusGameTest(timeoutTicks = 20)
    public static void noEnergyUpward(GameTestHelper helper) {
        place(helper, GEN, Direction.NORTH);
        BlockPos pos = helper.absolutePos(GEN);
        helper.assertTrue(EnergyView.of(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, pos, Direction.UP)) == null,
                "no energy toward the chimney");
        for (Direction side : new Direction[] {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.DOWN}) {
            helper.assertTrue(EnergyView.of(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, pos, side)) != null,
                    "energy is offered on " + side);
        }
        ItemView above = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, Direction.UP));
        helper.assertTrue(above == null || above.insertItem(new ItemStack(Items.COAL, 2), false).getCount() == 2, "no fuel from above");
        helper.succeed();
    }

    /** A burning generator pushes FE into the Electric Pump beside its cube, and its chimney follows the lit state. */
    @ServusGameTest(timeoutTicks = 100)
    public static void pushesIntoNeighbourBeside(GameTestHelper helper) {
        place(helper, GEN, Direction.NORTH);
        helper.getBlockEntity(GEN, CoalGeneratorBlockEntity.class).inventory()
                .setStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
        // Facing north, the pump takes energy on its west side, where the generator stands.
        PumpGameTests.placeElectricPump(helper, GEN.east());
        helper.succeedWhen(() -> {
            PoweredMachineBlockEntity pump = helper.getBlockEntity(GEN.east(), PoweredMachineBlockEntity.class);
            helper.assertTrue(pump.energyStorage().getEnergyStored() > 0, "the pump should receive energy from the generator");
            helper.assertTrue(helper.getBlockState(GEN.above()).getValue(MachineBlock.LIT), "the chimney is lit while the cube burns");
        });
    }
}
