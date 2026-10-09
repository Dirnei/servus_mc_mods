package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** The Purifier: Ash and water leached into liquid Salt. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PurifierGameTests {

    static final BlockPos PURIFIER = new BlockPos(3, 1, 3);

    private PurifierGameTests() {
    }

    static PurifierBlockEntity purifier(GameTestHelper helper) {
        helper.setBlock(PURIFIER, ServusManufacturingBlocks.PURIFIER.get().defaultBlockState());
        return helper.getBlockEntity(PURIFIER);
    }

    static IFluidHandler fluids(GameTestHelper helper) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(PURIFIER), Direction.UP);
    }

    static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    /** Three Ash and 1,000 mB water become 300 mB Salt and leave 700 mB water. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 300)
    public static void purifierLeachesAsh(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(PURIFIER), Direction.UP);
        helper.assertTrue(items.insertItem(0, new ItemStack(ServusManufacturingItems.ASH.get(), 3), false).isEmpty(), "the Purifier takes Ash");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "the Purifier refuses dirt");
        helper.assertTrue(fluids(helper).fill(water(1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "the Purifier takes water");
        helper.succeedWhen(() -> {
            helper.assertTrue(purifier.salt().getFluidAmount() == 300, "300 mB Salt, got " + purifier.salt().getFluidAmount());
            helper.assertTrue(purifier.water().getFluidAmount() == 700, "700 mB water left, got " + purifier.water().getFluidAmount());
            helper.assertTrue(purifier.inventory().getStackInSlot(0).isEmpty(), "the Ash is used up");
        });
    }

    /** Without water the Ash stays. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 100)
    public static void purifierNeedsWater(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        purifier.insert(new ItemStack(ServusManufacturingItems.ASH.get(), 2));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(purifier.salt().isEmpty(), "no Salt without water");
            helper.assertTrue(purifier.inventory().getStackInSlot(0).getCount() == 2, "the Ash stays");
            helper.succeed();
        });
    }

    /** The fluid handler takes only water and gives only Salt. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void purifierTakesWaterAndGivesSalt(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        IFluidHandler handler = fluids(helper);
        FluidStack salt = new FluidStack(PrincipleFluids.fluid(Principle.SALT), 100);
        helper.assertTrue(handler.fill(salt, IFluidHandler.FluidAction.EXECUTE) == 0, "Salt is refused");
        helper.assertTrue(handler.fill(water(500), IFluidHandler.FluidAction.EXECUTE) == 500, "water is taken");
        helper.assertTrue(handler.drain(water(100), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "water is never given out");
        helper.assertTrue(handler.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "nothing to give without Salt");
        purifier.salt().fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 250), IFluidHandler.FluidAction.EXECUTE);
        FluidStack drained = handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(drained.getFluid().isSame(PrincipleFluids.fluid(Principle.SALT)) && drained.getAmount() == 250, "the Salt comes out, got " + drained);
        helper.succeed();
    }

    /** A water bucket fills the vat; an empty bucket takes Salt out. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void purifierBuckets(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(helper, player);
        helper.assertTrue(purifier.water().getFluidAmount() == 1000, "the water bucket pours in, got " + purifier.water().getFluidAmount());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the player holds an empty bucket");
        purifier.salt().fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 1000), IFluidHandler.FluidAction.EXECUTE);
        use(helper, player);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(PrincipleFluids.entry(Principle.SALT).bucket().get()),
                "the bucket fills with Salt, holds " + player.getItemInHand(InteractionHand.MAIN_HAND));
        helper.assertTrue(purifier.salt().isEmpty() && purifier.water().getFluidAmount() == 1000, "the water stays");
        helper.succeed();
    }

    /** A finished purification counts for the Villach task of the owner's team. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 200)
    public static void purifierReportsToTheJournal(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        UUID team = UUID.randomUUID();
        purifier.setTeam(team);
        purifier.insert(new ItemStack(ServusManufacturingItems.ASH.get()));
        purifier.water().fill(water(100), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            JournalLedger.TeamJournal journal = Journal.journalOf(helper.getLevel().getServer(), team);
            helper.assertTrue(journal.count(AlchemyGameTests.VILLACH, "purify") >= 1, "the purification should count");
        });
    }

    static void use(GameTestHelper helper, Player player) {
        BlockPos pos = helper.absolutePos(PURIFIER);
        helper.getLevel().getBlockState(pos).useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND), helper.getLevel(), player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
}
