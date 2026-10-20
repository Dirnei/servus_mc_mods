package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlock;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.gantry.SetGantryLockPayload;
import io.github.dirnei.mc.servus.shuttles.menu.FluidGantryMenu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** The Fluid Gantry GUI on the server: lock picks, the mode button and the state line. */
public final class GantryMenuGameTests {

    private static final BlockPos RAIL = new BlockPos(4, 1, 4);

    private GantryMenuGameTests() {
    }

    private record Open(FluidGantryBlockEntity gantry, FluidGantryMenu menu, Player player) {
    }

    private static Open open(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        return new Open(gantry, new FluidGantryMenu(1, player.getInventory(), gantry), player);
    }

    /** Picking a fluid from the list locks the tank to it. */
    @ServusGameTest
    public static void menuLockFromList(GameTestHelper helper) {
        Open open = open(helper);
        open.menu().setLock(0, SetGantryLockPayload.FLUID, Optional.of(BuiltInRegistries.FLUID.getKey(Fluids.LAVA)));
        helper.assertTrue(open.gantry().lock(0) == Fluids.LAVA, "the left tank is locked to lava");
        helper.assertTrue(open.menu().lock(0) == Fluids.LAVA, "the menu shows the lock");
        open.menu().setLock(0, SetGantryLockPayload.ANY, Optional.empty());
        helper.assertTrue(open.gantry().lock(0) == null, "Any clears the lock");
        helper.succeed();
    }

    /** Lock to content locks a tank on Any to the fluid it holds. */
    @ServusGameTest
    public static void menuLockToContent(GameTestHelper helper) {
        Open open = open(helper);
        open.gantry().tank(1).fill(new FluidStack(Fluids.WATER, 2000), false);
        open.menu().setLock(1, SetGantryLockPayload.CONTENT, Optional.empty());
        helper.assertTrue(open.gantry().lock(1) == Fluids.WATER, "the right tank is locked to water");
        open.menu().setLock(0, SetGantryLockPayload.CONTENT, Optional.empty());
        helper.assertTrue(open.gantry().lock(0) == null, "an empty tank stays on Any");
        helper.succeed();
    }

    /** The mode button cycles Storage, Import, Export, and the beam's sign follows. */
    @ServusGameTest
    public static void menuCyclesMode(GameTestHelper helper) {
        Open open = open(helper);
        GantryMode[] expected = {GantryMode.IMPORT, GantryMode.EXPORT, GantryMode.STORAGE};
        for (GantryMode mode : expected) {
            open.menu().clickMenuButton(open.player(), FluidGantryMenu.BUTTON_MODE);
            helper.assertTrue(open.gantry().mode() == mode && open.menu().mode() == mode, "the mode is " + mode);
            helper.assertTrue(helper.getBlockState(RAIL.above()).getValue(FluidGantryBlock.MODE) == mode, "the sign shows " + mode);
        }
        helper.succeed();
    }

    /** A tank holding a fluid other than its lock puts a warning naming the tank on the state line. */
    @ServusGameTest
    public static void menuWarnsLockMismatch(GameTestHelper helper) {
        Open open = open(helper);
        helper.assertTrue(!open.menu().stateWarns(), "a fresh gantry has nothing to warn about");
        open.gantry().tank(0).fill(new FluidStack(Fluids.WATER, 3000), false);
        open.menu().setLock(0, SetGantryLockPayload.FLUID, Optional.of(BuiltInRegistries.FLUID.getKey(Fluids.LAVA)));
        helper.assertTrue(open.menu().stateWarns(), "the state line warns");
        String line = open.menu().stateLine().getString();
        helper.assertTrue(line.contains("Left") || line.contains("tank_0"), "the warning names the left tank: " + line);
        helper.assertTrue(open.gantry().tank(0).getFluidAmount() == 3000, "the tank keeps its water");
        helper.succeed();
    }

    /** An upgrade stays in its slot while the tanks would not fit without it, and the state line says why. */
    @ServusGameTest
    public static void menuRefusesUpgradeWhenFull(GameTestHelper helper) {
        Open open = open(helper);
        open.gantry().upgrades().setStackInSlot(0, new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()));
        open.gantry().tank(0).fill(new FluidStack(Fluids.WATER, 20000), false);
        helper.assertTrue(!open.menu().getSlot(0).mayPickup(open.player()), "the upgrade cannot be taken out");
        helper.assertTrue(open.menu().stateWarns(), "the state line says why");
        open.gantry().tank(0).drain(5000, false);
        helper.assertTrue(open.menu().getSlot(0).mayPickup(open.player()), "with 15000 mB it may come out");
        helper.succeed();
    }
}
