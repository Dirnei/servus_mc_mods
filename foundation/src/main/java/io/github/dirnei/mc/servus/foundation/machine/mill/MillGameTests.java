package io.github.dirnei.mc.servus.foundation.machine.mill;

import com.mojang.authlib.GameProfile;
import io.github.dirnei.mc.servus.foundation.drive.CrankTurns;
import io.github.dirnei.mc.servus.foundation.drive.DriveStats;
import io.github.dirnei.mc.servus.foundation.drive.DriveStatsManager;
import io.github.dirnei.mc.servus.foundation.drive.ElectricMotorBlockEntity;
import io.github.dirnei.mc.servus.foundation.drive.HandCrankBlockEntity;
import io.github.dirnei.mc.servus.foundation.drive.HandCrankBlock;
import io.github.dirnei.mc.servus.foundation.drive.MotorRate;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineTestSupport;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The Mill with its drivers: a motor grinds Gravel to Sand, nothing moves
 * without drive or power, the Speed Upgrade, the crank's turns and exhaustion,
 * where crank and drive go, and the Mill's item sides.
 */
public final class MillGameTests {

    /** The Mill faces north: its chute is north, its left side east, its right side west, its back south. */
    private static final BlockPos MILL = new BlockPos(5, 2, 5);
    /** The motor sits east of the Mill with its shaft pointing west into the Mill's left side. */
    private static final BlockPos MOTOR = MILL.east();
    /** The crank sits west of the Mill, on its right side. */
    private static final BlockPos CRANK = MILL.west();

    private MillGameTests() {
    }

    private static MillBlockEntity mill(GameTestHelper helper, ItemStack input) {
        helper.setBlock(MILL, MachineContent.MILL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MillBlockEntity mill = helper.getBlockEntity(MILL, MillBlockEntity.class);
        mill.inventory().setStackInSlot(MillBlockEntity.SLOT_INPUT, input);
        return mill;
    }

    private static ElectricMotorBlockEntity motor(GameTestHelper helper, int energy) {
        helper.setBlock(MOTOR, MachineContent.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        ElectricMotorBlockEntity motor = helper.getBlockEntity(MOTOR, ElectricMotorBlockEntity.class);
        motor.energyStorage().setEnergy(energy);
        return motor;
    }

    private static void crank(GameTestHelper helper) {
        helper.setBlock(CRANK, MachineContent.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.WEST));
    }

    private static ItemStack output(MillBlockEntity mill) {
        return mill.inventory().getStackInSlot(MillBlockEntity.SLOT_OUTPUT);
    }

    private static ItemStack input(MillBlockEntity mill) {
        return mill.inventory().getStackInSlot(MillBlockEntity.SLOT_INPUT);
    }

    private static int work(MillBlockEntity mill) {
        return mill.containerData().get(MachineBlockEntity.DATA_PROGRESS);
    }

    /** A survival player that adds up the food exhaustion it is charged. */
    private static final class TiringPlayer extends Player {

        private float exhaustion;

        private TiringPlayer(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "cranker"));
        }

        @Override
        public GameType gameMode() {
            return GameType.SURVIVAL;
        }

        @Override
        public void causeFoodExhaustion(float amount) {
            exhaustion += amount;
            super.causeFoodExhaustion(amount);
        }
    }

    /** Held ticks that grind one Gravel by hand, from the shipped recipe and crank data. */
    private static int ticksPerSand(GameTestHelper helper) {
        int work = ServusRecipes.first(helper.getLevel(), MachineContent.MILLING.get(), new SingleRecipeInput(new ItemStack(Items.GRAVEL)))
                .orElseThrow().value().work();
        int perTurn = DriveStatsManager.INSTANCE.get(HandCrankBlockEntity.ID, DriveStats.HAND_CRANK).workPerTurn();
        return (work + perTurn - 1) / perTurn * CrankTurns.TICKS_PER_TURN;
    }

    /** Holds the use key on the crank for {@code ticks}: a click every few ticks, as the client repeats it. */
    private static void holdCrank(GameTestHelper helper, Player player, int ticks) {
        for (int tick = 1; tick <= ticks; tick += CrankTurns.TICKS_PER_CLICK) {
            helper.runAfterDelay(tick, () -> helper.useBlock(CRANK, player));
        }
    }

    private static boolean turning(GameTestHelper helper, BlockPos pos, BooleanProperty property) {
        return helper.getBlockState(pos).getValue(property);
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void motorGrindsGravelToSand(GameTestHelper helper) {
        MillBlockEntity mill = mill(helper, new ItemStack(Items.GRAVEL));
        motor(helper, 8000);
        helper.succeedWhen(() -> {
            helper.assertTrue(input(mill).isEmpty(), "the Gravel is used up");
            helper.assertTrue(output(mill).is(Items.SAND) && output(mill).getCount() == 1, "one Sand is in the output");
        });
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void millWithoutDriveDoesNothing(GameTestHelper helper) {
        MillBlockEntity mill = mill(helper, new ItemStack(Items.GRAVEL));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(input(mill).is(Items.GRAVEL) && input(mill).getCount() == 1, "the Gravel is still there");
            helper.assertTrue(output(mill).isEmpty(), "nothing was ground");
            helper.assertTrue(work(mill) == 0, "no work was collected");
            helper.assertTrue(!helper.getBlockState(MILL).getValue(MachineBlock.LIT), "the Mill does not work");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void motorWithoutPowerDoesNothing(GameTestHelper helper) {
        MillBlockEntity mill = mill(helper, new ItemStack(Items.GRAVEL));
        motor(helper, 0);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(input(mill).getCount() == 1 && output(mill).isEmpty(), "nothing was ground");
            helper.assertTrue(work(mill) == 0, "no work was collected");
            helper.assertTrue(!helper.getBlockState(MOTOR).getValue(MachineBlock.LIT), "the motor does not run");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void idleMotorKeepsItsEnergy(GameTestHelper helper) {
        mill(helper, ItemStack.EMPTY);
        ElectricMotorBlockEntity motor = motor(helper, 8000);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(motor.energyStorage().getEnergyStored() == 8000, "an idle motor spends nothing");
            helper.assertTrue(!helper.getBlockState(MOTOR).getValue(MachineBlock.LIT), "the motor stands");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void speedUpgradeDoublesWorkAndEnergy(GameTestHelper helper) {
        mill(helper, new ItemStack(Items.GRAVEL, 64));
        ElectricMotorBlockEntity motor = motor(helper, 8000);
        MotorRate base = motor.rate();
        motor.upgrades().setStackInSlot(0, new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get()));
        MotorRate upgraded = motor.rate();
        helper.assertTrue(upgraded.workPerTick() == 2 * base.workPerTick(), "work per tick doubles");
        helper.assertTrue(upgraded.energyPerTick() == 2 * base.energyPerTick(), "energy per tick doubles");
        helper.runAfterDelay(10, () -> {
            int spent = 8000 - motor.energyStorage().getEnergyStored();
            helper.assertTrue(spent > 0 && spent % upgraded.energyPerTick() == 0,
                    "every working tick costs the upgraded rate, spent " + spent);
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void motorTakesEnergyOnlyFromTheBack(GameTestHelper helper) {
        motor(helper, 0);
        BlockPos motor = helper.absolutePos(MOTOR);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, motor, Direction.EAST) != null, "the back takes power");
        for (Direction side : new Direction[] {Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN}) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, motor, side) == null, "no power through " + side);
        }
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void driveOnlyOnBackAndSides(GameTestHelper helper) {
        mill(helper, ItemStack.EMPTY);
        BlockPos mill = helper.absolutePos(MILL);
        for (Direction side : new Direction[] {Direction.EAST, Direction.WEST, Direction.SOUTH}) {
            helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.DRIVE, mill, side) != null, "drive through " + side);
        }
        for (Direction side : new Direction[] {Direction.NORTH, Direction.UP, Direction.DOWN}) {
            helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.DRIVE, mill, side) == null, "no drive through " + side);
        }
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void crankTurnsGrindAndTire(GameTestHelper helper) {
        MillBlockEntity mill = mill(helper, new ItemStack(Items.GRAVEL));
        crank(helper);
        TiringPlayer player = new TiringPlayer(helper);
        int ticks = ticksPerSand(helper);
        holdCrank(helper, player, ticks + 48);
        helper.runAfterDelay(ticks - CrankTurns.TICKS_PER_TURN, () -> {
            helper.assertTrue(output(mill).isEmpty(), "one turn short, nothing is ground yet");
            helper.assertTrue(work(mill) > 0, "the held crank gave work");
            helper.assertTrue(player.exhaustion > 0, "turning tires the player");
        });
        float[] tired = new float[1];
        helper.runAfterDelay(ticks + 8, () -> {
            helper.assertTrue(output(mill).is(Items.SAND), "the last turn grinds the Sand");
            tired[0] = player.exhaustion;
        });
        helper.runAfterDelay(ticks + 48, () -> {
            helper.assertTrue(player.exhaustion == tired[0], "turning an empty Mill costs nothing");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void crankStopsWhenLetGo(GameTestHelper helper) {
        MillBlockEntity mill = mill(helper, new ItemStack(Items.GRAVEL, 64));
        crank(helper);
        TiringPlayer player = new TiringPlayer(helper);
        holdCrank(helper, player, 20);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(turning(helper, CRANK, HandCrankBlock.TURNING), "the held crank turns");
            helper.assertTrue(turning(helper, MILL, MachineBlock.LIT), "the Mill turns with it");
        });
        int[] work = new int[1];
        helper.runAfterDelay(20 + CrankTurns.HOLD_TICKS + 2, () -> {
            helper.assertTrue(!turning(helper, CRANK, HandCrankBlock.TURNING), "let go, the crank stops");
            helper.assertTrue(!turning(helper, MILL, MachineBlock.LIT), "and the Mill with it");
            work[0] = work(mill);
            helper.assertTrue(work[0] > 0, "the held crank gave work");
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(work(mill) == work[0], "no work after letting go, " + work(mill) + " vs " + work[0]);
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void crankTurnsAnEmptyMill(GameTestHelper helper) {
        mill(helper, ItemStack.EMPTY);
        crank(helper);
        TiringPlayer player = new TiringPlayer(helper);
        holdCrank(helper, player, 40);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(turning(helper, CRANK, HandCrankBlock.TURNING), "the crank turns");
            helper.assertTrue(turning(helper, MILL, MachineBlock.LIT), "the empty Mill's wheel turns along");
            helper.assertTrue(player.exhaustion == 0, "turning an empty Mill costs nothing");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void crankNeedsADrivenBlock(GameTestHelper helper) {
        mill(helper, ItemStack.EMPTY);
        BlockPos stone = new BlockPos(2, 2, 2);
        helper.setBlock(stone, Blocks.STONE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack onStone = new ItemStack(MachineContent.HAND_CRANK_ITEM.get());
        GameTestSteps.useOn(helper, player, onStone, stone, Direction.EAST);
        helper.assertTrue(helper.getBlockState(stone.east()).isAir() && onStone.getCount() == 1, "no crank on stone");
        ItemStack onTop = new ItemStack(MachineContent.HAND_CRANK_ITEM.get());
        GameTestSteps.useOn(helper, player, onTop, MILL, Direction.UP);
        helper.assertTrue(helper.getBlockState(MILL.above()).isAir() && onTop.getCount() == 1, "no crank on the Mill's top");
        ItemStack onSide = new ItemStack(MachineContent.HAND_CRANK_ITEM.get());
        GameTestSteps.useOn(helper, player, onSide, MILL, Direction.WEST);
        BlockState placed = helper.getBlockState(CRANK);
        helper.assertTrue(placed.is(MachineContent.HAND_CRANK.get()) && placed.getValue(HandCrankBlock.FACING) == Direction.WEST,
                "a crank on the Mill's side, facing away from it");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void crankDropsWhenTheMillGoes(GameTestHelper helper) {
        mill(helper, ItemStack.EMPTY);
        crank(helper);
        helper.setBlock(MILL, Blocks.AIR.defaultBlockState());
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(CRANK).isAir(), "the crank is gone");
            helper.assertTrue(MachineTestSupport.count(GameTestSteps.drops(helper, CRANK, 2), MachineContent.HAND_CRANK_ITEM.get()) == 1,
                    "the crank dropped as an item");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void hopperAboveChestInFront(GameTestHelper helper) {
        mill(helper, ItemStack.EMPTY);
        motor(helper, 8000);
        helper.setBlock(MILL.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.getBlockEntity(MILL.above(), HopperBlockEntity.class).setItem(0, new ItemStack(Items.GRAVEL, 2));
        BlockPos front = MILL.north();
        helper.setBlock(front, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        ChestBlockEntity chest = helper.getBlockEntity(front, ChestBlockEntity.class);
        helper.succeedWhen(() -> {
            int sand = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (stack.is(Items.SAND)) {
                    sand += stack.getCount();
                }
            }
            helper.assertTrue(sand == 2, "both Sand went through the chute into the chest, now " + sand);
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void chuteRefusesInput(GameTestHelper helper) {
        mill(helper, ItemStack.EMPTY);
        ItemView chute = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(MILL), Direction.NORTH));
        helper.assertTrue(chute != null, "the chute side offers the output");
        ItemStack left = chute.insertItem(new ItemStack(Items.GRAVEL), false);
        helper.assertTrue(left.getCount() == 1, "nothing goes in through the chute");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void millRefusesOtherItems(GameTestHelper helper) {
        MillBlockEntity mill = mill(helper, ItemStack.EMPTY);
        ItemView top = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(MILL), Direction.UP));
        helper.assertTrue(top.insertItem(new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "Cobblestone is refused");
        helper.assertTrue(top.insertItem(new ItemStack(Items.GRAVEL), false).isEmpty(), "Gravel goes in through the top");
        helper.assertTrue(input(mill).is(Items.GRAVEL), "into the input slot");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void millingRecipeShipped(GameTestHelper helper) {
        Optional<RecipeHolder<MillingRecipe>> recipe = ServusRecipes.first(helper.getLevel(), MachineContent.MILLING.get(),
                new SingleRecipeInput(new ItemStack(Items.GRAVEL)));
        helper.assertTrue(recipe.isPresent(), "Gravel has a milling recipe");
        MillingRecipe milling = recipe.get().value();
        helper.assertTrue(milling.result().is(Items.SAND) && milling.result().getCount() == 1, "one Sand");
        helper.assertTrue(milling.input().count() == 1 && milling.work() > 0, "from one Gravel for some work");
        helper.succeed();
    }
}
