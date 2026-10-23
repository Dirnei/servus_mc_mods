package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleNumberData;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleWorkshopBlockEntity;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

/** Shuttle numbers given in the garage and the shuttle label. */
public final class ShuttleNumberGameTests {

    private static final BlockPos GARAGE_A = new BlockPos(2, 1, 2);
    private static final BlockPos GARAGE_B = new BlockPos(5, 1, 2);
    private static final BlockPos WORKSHOP = new BlockPos(2, 1, 5);

    private ShuttleNumberGameTests() {
    }

    private static ShuttleGarageBlockEntity garage(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusShuttlesBlocks.SHUTTLE_GARAGE.get().defaultBlockState());
        return helper.getBlockEntity(pos, ShuttleGarageBlockEntity.class);
    }

    private static ItemStack mk1() {
        return new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get());
    }

    private static int last(GameTestHelper helper) {
        return ShuttleNumberData.get(helper.getLevel().getServer()).last();
    }

    private static OptionalInt numberIn(ShuttleGarageBlockEntity garage, int slot) {
        return ShuttleItem.number(garage.shuttles().getStackInSlot(slot));
    }

    /** Two shuttles put into a garage get the world's next two numbers. */
    @ServusGameTest
    public static void garageNumbersShuttlesInOrder(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = garage(helper, GARAGE_A);
        helper.assertTrue(ShuttleItem.number(mk1()).isEmpty(), "a crafted shuttle has no number");
        int before = last(helper);
        garage.shuttles().setStackInSlot(0, mk1());
        garage.shuttles().setStackInSlot(1, mk1());
        helper.assertTrue(numberIn(garage, 0).equals(OptionalInt.of(before + 1)), "the first shuttle is number " + (before + 1)
                + ", got " + numberIn(garage, 0));
        helper.assertTrue(numberIn(garage, 1).equals(OptionalInt.of(before + 2)), "the second shuttle is number " + (before + 2)
                + ", got " + numberIn(garage, 1));
        helper.assertTrue(last(helper) == before + 2, "the counter moved on by two");
        helper.succeed();
    }

    /** A shift click from the inventory into the garage numbers the shuttle too. */
    @ServusGameTest
    public static void shiftClickNumbersShuttle(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = garage(helper, GARAGE_A);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, mk1());
        ShuttleGarageMenu menu = new ShuttleGarageMenu(1, player.getInventory(), garage);
        menu.quickMoveStack(player, garage.capacity() + 27);
        helper.assertTrue(numberIn(garage, 0).isPresent(), "the shift clicked shuttle has a number");
        helper.succeed();
    }

    /** A shuttle taken out and put into another garage keeps its number and takes no new one. */
    @ServusGameTest
    public static void numberStaysWithTheShuttle(GameTestHelper helper) {
        ShuttleGarageBlockEntity first = garage(helper, GARAGE_A);
        ShuttleGarageBlockEntity second = garage(helper, GARAGE_B);
        first.shuttles().setStackInSlot(0, mk1());
        ItemStack taken = first.shuttles().getStackInSlot(0).copy();
        first.shuttles().setStackInSlot(0, ItemStack.EMPTY);
        int before = last(helper);
        second.shuttles().setStackInSlot(2, taken);
        helper.assertTrue(numberIn(second, 2).equals(ShuttleItem.number(taken)), "the shuttle keeps its number");
        helper.assertTrue(last(helper) == before, "the counter did not move");
        helper.succeed();
    }

    /** A garage loaded from a save without numbers numbers its shuttles in slot order. */
    @ServusGameTest
    public static void oldGarageNumbersOnLoad(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = garage(helper, GARAGE_A);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag saved = garage.saveWithoutMetadata(registries);
        CompoundTag oldSlots = new CompoundTag();
        oldSlots.put("ShuttleItems", unnumbered(registries, garage.capacity()));
        saved.merge(oldSlots);
        int before = last(helper);
        garage.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));
        garage.onLoad();
        helper.assertTrue(numberIn(garage, 0).equals(OptionalInt.of(before + 1)) && numberIn(garage, 2).equals(OptionalInt.of(before + 2)),
                "slots 0 and 2 got the next numbers in order, got " + numberIn(garage, 0) + " and " + numberIn(garage, 2));
        helper.assertTrue(garage.shuttles().getStackInSlot(1).isEmpty(), "the empty slot stays empty");
        helper.succeed();
    }

    /** The slot tag of a garage saved before numbers: shuttles in slots 0 and 2. */
    private static CompoundTag unnumbered(HolderLookup.Provider registries, int capacity) {
        ItemSlots slots = new ItemSlots(capacity);
        slots.setStackInSlot(0, mk1());
        slots.setStackInSlot(2, mk1());
        return slots.serializeNBT(registries);
    }

    /** The label is "Shuttle #n", "Name (#n)" with a plate, and the old item name without a number. */
    @ServusGameTest
    public static void labelForms(GameTestHelper helper) {
        ItemStack plain = mk1();
        assertLabel(helper, ShuttleItem.label(plain), "item.servus_shuttles.shuttle");
        ItemStack named = new ItemStack(ServusShuttlesItems.SHUTTLE_MK2.get());
        ShuttleItem.engrave(named, "Berta");
        assertLabel(helper, ShuttleItem.label(named), "item.servus_shuttles.shuttle.named", "Berta");
        plain.set(ServusShuttlesComponents.SHUTTLE_NUMBER.get(), 12);
        assertLabel(helper, ShuttleItem.label(plain), "shuttle.servus_shuttles.label", 12);
        named.set(ServusShuttlesComponents.SHUTTLE_NUMBER.get(), 12);
        assertLabel(helper, ShuttleItem.label(named), "shuttle.servus_shuttles.label.named", "Berta", 12);
        assertLabel(helper, named.getHoverName(), "shuttle.servus_shuttles.label.named", "Berta", 12);
        helper.succeed();
    }

    private static void assertLabel(GameTestHelper helper, Component label, String key, Object... firstArgs) {
        helper.assertTrue(label.getContents() instanceof TranslatableContents, "the label is translatable, got " + label);
        TranslatableContents translatable = (TranslatableContents) label.getContents();
        List<Object> args = Arrays.asList(translatable.getArgs());
        helper.assertTrue(translatable.getKey().equals(key), "label key " + key + ", got " + translatable.getKey());
        helper.assertTrue(args.size() >= firstArgs.length && args.subList(0, firstArgs.length).equals(Arrays.asList(firstArgs)),
                "label arguments start with " + Arrays.asList(firstArgs) + ", got " + args);
    }

    /** A tier build in the workshop keeps the shuttle's number. */
    @ServusGameTest
    public static void tierBuildKeepsNumber(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, WORKSHOP);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WORKSHOP, ShuttleWorkshopBlockEntity.class);
        ItemStack numbered = mk1();
        numbered.set(ServusShuttlesComponents.SHUTTLE_NUMBER.get(), 5);
        workshop.shuttle().setStackInSlot(0, numbered);
        ShuttleModuleGameTests.supplyFor(workshop, 2);
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD_NOW), "Build now builds a Mk2");
        ItemStack mk2 = workshop.shuttle().getStackInSlot(0);
        helper.assertTrue(mk2.is(ServusShuttlesItems.SHUTTLE_MK2.get()) && ShuttleItem.number(mk2).equals(OptionalInt.of(5)),
                "the Mk2 is still shuttle 5, got " + ShuttleItem.number(mk2));
        helper.succeed();
    }
}
