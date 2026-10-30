package io.github.dirnei.mc.servus.foundation.machine.mixer;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.drive.CrankTurns;
import io.github.dirnei.mc.servus.foundation.drive.ElectricMotorBlockEntity;
import io.github.dirnei.mc.servus.foundation.drive.HandCrankBlock;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Mixer: mixing only while driven, the shipped recipes and that they never compete, the one kind per slot rule,
 * the tank with its fluid sides and buckets, where drive goes, and its item sides.
 */
public final class MixerGameTests {

    /** The Mixer faces north: its chute is north, its left side east, its right side west, its back south. */
    private static final BlockPos MIXER = new BlockPos(5, 2, 5);
    /** The motor sits east of the Mixer with its shaft pointing west into the Mixer's left side. */
    private static final BlockPos MOTOR = MIXER.east();
    /** The crank sits west of the Mixer, on its right side. */
    private static final BlockPos CRANK = MIXER.west();
    private static final int ENERGY = 8000;

    private MixerGameTests() {
    }

    /** A Mixer facing north holding {@code inputs} in its first slots and {@code water} mB of Water. */
    private static MixerBlockEntity mixer(GameTestHelper helper, int water, ItemStack... inputs) {
        helper.setBlock(MIXER, MachineContent.MIXER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MixerBlockEntity mixer = helper.getBlockEntity(MIXER, MixerBlockEntity.class);
        for (int slot = 0; slot < inputs.length; slot++) {
            mixer.inventory().setStackInSlot(slot, inputs[slot]);
        }
        if (water > 0) {
            mixer.tank().setFluid(new FluidStack(Fluids.WATER, water));
        }
        return mixer;
    }

    private static ElectricMotorBlockEntity motor(GameTestHelper helper) {
        helper.setBlock(MOTOR, MachineContent.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        ElectricMotorBlockEntity motor = helper.getBlockEntity(MOTOR, ElectricMotorBlockEntity.class);
        motor.energyStorage().setEnergy(ENERGY);
        return motor;
    }

    private static ItemStack output(MixerBlockEntity mixer) {
        return mixer.inventory().getStackInSlot(MixerBlockEntity.SLOT_OUTPUT);
    }

    private static ItemStack input(MixerBlockEntity mixer, int slot) {
        return mixer.inventory().getStackInSlot(slot);
    }

    private static int water(MixerBlockEntity mixer) {
        return mixer.tank().getFluid().is(Fluids.WATER) ? mixer.tank().getFluidAmount() : 0;
    }

    private static int work(MixerBlockEntity mixer) {
        return mixer.containerData().get(MachineBlockEntity.DATA_PROGRESS);
    }

    private static FluidView fluidSide(GameTestHelper helper, Direction side) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(MIXER), side));
    }

    private static ItemView itemSide(GameTestHelper helper, Direction side) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(MIXER), side));
    }

    /** Uses the stack in the player's main hand on the Mixer's top, like a right click. */
    private static void useHeld(GameTestHelper helper, Player player, ItemStack stack) {
        BlockPos absolute = helper.absolutePos(MIXER);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        BlockState state = helper.getLevel().getBlockState(absolute);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        state.useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    /** The mixing recipes that match {@code items} and {@code fluid}. */
    private static List<RecipeHolder<MixingRecipe>> matching(GameTestHelper helper, FluidStack fluid, List<ItemStack> items) {
        List<ItemStack> slots = new ArrayList<>(items);
        while (slots.size() < MixerBlockEntity.INPUTS) {
            slots.add(ItemStack.EMPTY);
        }
        MixingInput input = new MixingInput(slots, fluid);
        return ServusRecipes.all(helper.getLevel(), MachineContent.MIXING.get()).stream()
                .filter(holder -> holder.value().matches(input, helper.getLevel())).toList();
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void motorMixesMud(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 1000, new ItemStack(Items.DIRT));
        motor(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(input(mixer, 0).isEmpty(), "the Dirt is used up");
            helper.assertTrue(output(mixer).is(Items.MUD) && output(mixer).getCount() == 1, "one Mud is in the output");
            helper.assertTrue(water(mixer) == 750, "250 mB of Water went in, " + water(mixer) + " left");
        });
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void mixerWithoutDriveDoesNothing(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 1000, new ItemStack(Items.DIRT));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(input(mixer, 0).is(Items.DIRT) && water(mixer) == 1000, "the Dirt and the Water are still there");
            helper.assertTrue(output(mixer).isEmpty(), "nothing was mixed");
            helper.assertTrue(work(mixer) == 0, "no work was collected");
            helper.assertTrue(!helper.getBlockState(MIXER).getValue(MachineBlock.LIT), "the Mixer does not turn");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void noWaterNoMud(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 0, new ItemStack(Items.DIRT));
        ElectricMotorBlockEntity motor = motor(helper);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(input(mixer, 0).is(Items.DIRT) && output(mixer).isEmpty(), "without water the Dirt stays");
            helper.assertTrue(motor.energyStorage().getEnergyStored() == ENERGY, "the motor spent nothing");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void coarseDirtLeavesTheWater(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 1000, new ItemStack(Items.DIRT, 2), new ItemStack(Items.GRAVEL, 2));
        motor(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(output(mixer).is(Items.COARSE_DIRT) && output(mixer).getCount() == 4, "four Coarse Dirt");
            helper.assertTrue(input(mixer, 0).isEmpty() && input(mixer, 1).isEmpty(), "the Dirt and Gravel are used up");
            helper.assertTrue(water(mixer) == 1000, "the dry recipe leaves the Water");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void concretePowderPerColour(GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            Item dye = vanilla(color.getSerializedName() + "_dye");
            Item powder = vanilla(color.getSerializedName() + "_concrete_powder");
            List<RecipeHolder<MixingRecipe>> found = matching(helper, FluidStack.EMPTY,
                    List.of(new ItemStack(Items.SAND, 4), new ItemStack(Items.GRAVEL, 4), new ItemStack(dye)));
            helper.assertTrue(found.size() == 1, color.getSerializedName() + " has one recipe, found " + found.size());
            ItemStack made = found.getFirst().value().result();
            helper.assertTrue(made.is(powder) && made.getCount() == 8, "8 " + color.getSerializedName() + " Concrete Powder");
        }
        MixerBlockEntity mixer = mixer(helper, 0, new ItemStack(Items.SAND, 4), new ItemStack(Items.GRAVEL, 4), new ItemStack(Items.RED_DYE));
        motor(helper);
        helper.succeedWhen(() -> helper.assertTrue(output(mixer).is(Items.RED_CONCRETE_POWDER) && output(mixer).getCount() == 8,
                "the Mixer makes 8 Red Concrete Powder"));
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void leftoverItemBlocksTheMix(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 1000, new ItemStack(Items.DIRT), new ItemStack(Items.SAND));
        ElectricMotorBlockEntity motor = motor(helper);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(output(mixer).isEmpty() && water(mixer) == 1000, "nothing was mixed");
            helper.assertTrue(motor.energyStorage().getEnergyStored() == ENERGY, "the motor spent nothing");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void outputFullWantsNoWork(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 1000, new ItemStack(Items.DIRT));
        mixer.inventory().setStackInSlot(MixerBlockEntity.SLOT_OUTPUT, new ItemStack(Items.MUD, 64));
        ElectricMotorBlockEntity motor = motor(helper);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(!mixer.drive(null).wantsWork(), "a full output wants no work");
            helper.assertTrue(motor.energyStorage().getEnergyStored() == ENERGY, "the motor spent nothing");
            helper.assertTrue(input(mixer, 0).is(Items.DIRT), "the Dirt stays");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void oneKindPerSlot(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 0);
        ItemView top = itemSide(helper, Direction.UP);
        helper.assertTrue(top.insertItem(new ItemStack(Items.SAND, 64), false).isEmpty(), "a stack of Sand goes in");
        ItemStack left = top.insertItem(new ItemStack(Items.SAND, 10), false);
        helper.assertTrue(left.getCount() == 10, "more Sand finds no second slot, " + left.getCount() + " left");
        helper.assertTrue(input(mixer, 0).is(Items.SAND) && input(mixer, 0).getCount() == 64, "all the Sand is in one slot");
        for (int slot = 1; slot < MixerBlockEntity.INPUTS; slot++) {
            helper.assertTrue(input(mixer, slot).isEmpty(), "slot " + slot + " stays empty");
            helper.assertTrue(!mixer.isItemValid(slot, new ItemStack(Items.SAND)), "slot " + slot + " refuses a second Sand");
        }
        helper.assertTrue(mixer.isItemValid(1, new ItemStack(Items.GRAVEL)), "another kind takes an empty slot");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void refusesUnusedItems(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 0);
        helper.assertTrue(!mixer.isItemValid(0, new ItemStack(Items.DIAMOND)), "the slot refuses a Diamond");
        helper.assertTrue(itemSide(helper, Direction.UP).insertItem(new ItemStack(Items.DIAMOND), false).getCount() == 1,
                "the top refuses a Diamond");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void fluidOnlyThroughTheBack(GameTestHelper helper) {
        mixer(helper, 0);
        for (Direction side : new Direction[] {Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            FluidView handler = fluidSide(helper, side);
            helper.assertTrue(handler != null, "fluid through " + side);
        }
        for (Direction side : new Direction[] {Direction.NORTH, Direction.UP, Direction.DOWN}) {
            helper.assertTrue(fluidSide(helper, side) == null, "no fluid through " + side);
        }
        helper.assertTrue(fluidSide(helper, Direction.SOUTH).fill(new FluidStack(Fluids.WATER, 1000), false) == 1000,
                "the back takes Water");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void automationNeverDrains(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 1000);
        for (Direction side : new Direction[] {Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            FluidView handler = fluidSide(helper, side);
            helper.assertTrue(handler.drain(1000, false).isEmpty(), "nothing drains through " + side);
            helper.assertTrue(handler.drain(new FluidStack(Fluids.WATER, 1000), false).isEmpty(), "no Water drains through " + side);
        }
        helper.assertTrue(water(mixer) == 1000, "the tank still holds its Water");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void bucketPoursInAndOut(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useHeld(helper, player, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(water(mixer) == 1000, "the Water Bucket pours in, now " + water(mixer));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the player holds an empty bucket");
        useHeld(helper, player, player.getItemInHand(InteractionHand.MAIN_HAND));
        helper.assertTrue(mixer.tank().isEmpty(), "the empty bucket takes the Water back out");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET), "the player holds a Water Bucket");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void tankRefusesLava(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useHeld(helper, player, new ItemStack(Items.LAVA_BUCKET));
        helper.assertTrue(mixer.tank().isEmpty(), "no Lava pours in");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.LAVA_BUCKET), "the player keeps the Lava Bucket");
        helper.assertTrue(fluidSide(helper, Direction.SOUTH).fill(new FluidStack(Fluids.LAVA, 1000), false) == 0, "the back refuses Lava");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void pullBackFillsFromTank(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 0);
        mixer.fluidSides().setMode(RelativeSide.BACK, SideMode.PULL);
        BlockPos back = MIXER.south();
        helper.setBlock(back, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        FluidView tank = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(back), Direction.UP));
        int stored = tank.fill(new FluidStack(Fluids.WATER, 16000), false);
        int expected = Math.min(stored, mixer.tank().getCapacity());
        helper.succeedWhen(() -> helper.assertTrue(water(mixer) == expected,
                "Water flows in from the tank behind until the Mixer is full, now " + water(mixer) + " of " + expected));
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void driveOnlyOnBackAndSides(GameTestHelper helper) {
        mixer(helper, 0);
        BlockPos mixer = helper.absolutePos(MIXER);
        for (Direction side : new Direction[] {Direction.EAST, Direction.WEST, Direction.SOUTH}) {
            helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.DRIVE, mixer, side) != null, "drive through " + side);
        }
        for (Direction side : new Direction[] {Direction.NORTH, Direction.UP, Direction.DOWN}) {
            helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.DRIVE, mixer, side) == null, "no drive through " + side);
        }
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void crankTurnsAnEmptyMixer(GameTestHelper helper) {
        MixerBlockEntity mixer = mixer(helper, 0);
        helper.setBlock(CRANK, MachineContent.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.WEST));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int tick = 1; tick <= 40; tick += CrankTurns.TICKS_PER_CLICK) {
            helper.runAfterDelay(tick, () -> helper.useBlock(CRANK, player));
        }
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(helper.getBlockState(CRANK).getValue(HandCrankBlock.TURNING), "the crank turns");
            helper.assertTrue(helper.getBlockState(MIXER).getValue(MachineBlock.LIT), "the empty Mixer's paddle turns along");
            helper.assertTrue(output(mixer).isEmpty(), "and mixes nothing");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 400)
    public static void hopperAboveChestInFront(GameTestHelper helper) {
        mixer(helper, 0);
        motor(helper);
        helper.setBlock(MIXER.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(MIXER.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.DIRT, 2));
        hopper.setItem(1, new ItemStack(Items.GRAVEL, 2));
        BlockPos front = MIXER.north();
        helper.setBlock(front, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        ChestBlockEntity chest = helper.getBlockEntity(front, ChestBlockEntity.class);
        helper.succeedWhen(() -> {
            int coarse = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (stack.is(Items.COARSE_DIRT)) {
                    coarse += stack.getCount();
                }
            }
            helper.assertTrue(coarse == 4, "the Coarse Dirt went through the chute into the chest, now " + coarse);
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void chuteRefusesInput(GameTestHelper helper) {
        mixer(helper, 0);
        ItemView chute = itemSide(helper, Direction.NORTH);
        helper.assertTrue(chute != null, "the chute side offers the output");
        helper.assertTrue(chute.insertItem(new ItemStack(Items.DIRT), false).getCount() == 1, "nothing goes in through the chute");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void mixingRecipesShipped(GameTestHelper helper) {
        long shipped = ServusRecipes.all(helper.getLevel(), MachineContent.MIXING.get()).stream()
                .filter(holder -> holder.id().identifier().getNamespace().equals(ServusFoundationIds.MOD_ID)).count();
        helper.assertTrue(shipped == 18, "Mud, Coarse Dirt and 16 Concrete Powders, found " + shipped);
        List<RecipeHolder<MixingRecipe>> mud = matching(helper, new FluidStack(Fluids.WATER, 1000), List.of(new ItemStack(Items.DIRT)));
        helper.assertTrue(mud.size() == 1, "Dirt and Water have one recipe");
        MixingRecipe recipe = mud.getFirst().value();
        helper.assertTrue(recipe.result().is(Items.MUD) && recipe.result().getCount() == 1, "one Mud");
        helper.assertTrue(recipe.fluid().orElseThrow().amount() == 250 && recipe.work() > 0, "from 250 mB of Water for some work");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void shippedMixingRecipesDoNotCompete(GameTestHelper helper) {
        for (RecipeHolder<MixingRecipe> holder : ServusRecipes.all(helper.getLevel(), MachineContent.MIXING.get())) {
            Identifier id = holder.id().identifier();
            if (!id.getNamespace().equals(ServusFoundationIds.MOD_ID)) {
                continue;
            }
            MixingRecipe recipe = holder.value();
            List<ItemStack> least = new ArrayList<>();
            for (SizedIngredient ingredient : recipe.ingredients()) {
                least.add(ServusRecipes.items(ingredient).getFirst().copyWithCount(ingredient.count()));
            }
            FluidStack fluid = recipe.fluid()
                    .map(wanted -> new FluidStack(wanted.ingredient().fluids().getFirst().value(), wanted.amount()))
                    .orElse(FluidStack.EMPTY);
            List<RecipeHolder<MixingRecipe>> found = matching(helper, fluid, least);
            helper.assertTrue(found.size() == 1 && found.getFirst().id().equals(holder.id()),
                    id + "'s least contents match " + found.stream().map(other -> other.id().identifier().toString()).toList());
        }
        helper.succeed();
    }

    private static Item vanilla(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }
}
