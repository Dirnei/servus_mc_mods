package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitualData;
import io.github.dirnei.mc.servus.alchemy.coagulator.GrownVatBlockEntity;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatCrystal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** GameTests of the Athanor rituals: casting the Citrinitas ritual, its refusals, and what the grown vats do. */
public final class RitualGameTests {

    private static final BlockPos BACK_VAT = AlchemyGameTests.ATHANOR.offset(0, -1, 3);
    private static final BlockPos RIGHT_VAT = AlchemyGameTests.ATHANOR.offset(-3, -1, 0);
    private static final BlockPos LEFT_VAT = AlchemyGameTests.ATHANOR.offset(3, -1, 0);
    private static final List<BlockPos> VATS = List.of(BACK_VAT, RIGHT_VAT, LEFT_VAT);

    private RitualGameTests() {
    }

    private static AthanorRitual citrinitas() {
        return AthanorRitualData.readShipped("citrinitas");
    }

    /** The three empty Principle Vats the Citrinitas ritual needs, beside the tanks of a north facing Athanor. */
    private static void plainVats(GameTestHelper helper) {
        for (BlockAt vat : citrinitas().turns(Facing.NORTH).stream().map(AthanorRitual.Turn::from).toList()) {
            AlchemyGameTests.place(helper, vat, Facing.NORTH);
        }
    }

    /** A formed Albedo Athanor with the three empty vats beside its tanks. */
    private static AthanorBlockEntity readyAthanor(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        AlchemyGameTests.saltRing(helper);
        plainVats(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 1, "the build is Albedo, got " + athanor.tier());
        return athanor;
    }

    private static Player offering(GameTestHelper helper, List<String> messages, int count) {
        Player player = AlchemyGameTests.listeningPlayer(helper, messages);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_BLOCK, count));
        return player;
    }

    private static VatCrystal crystal(GameTestHelper helper, BlockPos vat) {
        return helper.getBlockState(vat).getValue(VatBlock.CRYSTAL);
    }

    /** An Amethyst Block on an Albedo Athanor with three empty vats grows citrine in all three, and the Athanor is Citrinitas. */
    @ServusGameTest(timeoutTicks = 260)
    public static void ritualGrowsCitrine(GameTestHelper helper) {
        AthanorBlockEntity athanor = readyAthanor(helper);
        List<String> messages = new ArrayList<>();
        Player player = offering(helper, messages, 2);
        CalcinerGameTests.use(helper, player, AlchemyGameTests.ATHANOR);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "the offering is taken, held " + player.getMainHandItem());
        helper.assertTrue(athanor.ritual() != null && messages.isEmpty(), "the ritual runs, got " + messages);
        int duration = citrinitas().duration();
        helper.runAfterDelay(duration / 2, () -> {
            helper.assertTrue(VATS.stream().allMatch(vat -> crystal(helper, vat) == VatCrystal.NONE), "nothing turns before the end");
            CalcinerGameTests.use(helper, player, AlchemyGameTests.ATHANOR);
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "a second offering is refused while the first runs");
            helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.ritual.running")), "the player is told, got " + messages);
        });
        helper.runAfterDelay(duration + 5, () -> {
            for (BlockPos vat : VATS) {
                helper.assertTrue(crystal(helper, vat) == VatCrystal.CITRINE, "citrine grows in the vat at " + vat);
                helper.assertTrue(helper.getBlockEntity(vat, GrownVatBlockEntity.class) != null, "the grown vat draws its cluster");
            }
            helper.assertTrue(athanor.ritual() == null, "the ritual is over");
            helper.assertTrue(athanor.tier() == 2 && helper.getBlockState(AlchemyGameTests.ATHANOR).getValue(AthanorBlock.TIER) == 2,
                    "the Athanor is Citrinitas, got " + athanor.tier());
            helper.succeed();
        });
    }

    /** Without the vat beside the back tank the player keeps the amethyst and is told where the vat belongs. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ritualKeepsTheOfferingWhenAVatIsMissing(GameTestHelper helper) {
        AthanorBlockEntity athanor = readyAthanor(helper);
        helper.setBlock(BACK_VAT, ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get().defaultBlockState());
        List<String> messages = new ArrayList<>();
        Player player = offering(helper, messages, 1);
        CalcinerGameTests.use(helper, player, AlchemyGameTests.ATHANOR);
        helper.assertTrue(player.getMainHandItem().is(Items.AMETHYST_BLOCK) && player.getMainHandItem().getCount() == 1, "the amethyst stays");
        helper.assertTrue(athanor.ritual() == null, "nothing starts");
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.ritual.missing block.servus_alchemy.principle_vat "
                + "message.servus_alchemy.athanor.tank.back")), "the vat behind is named, got " + messages);
        helper.succeed();
    }

    /** On a Nigredo Athanor the ritual is refused: it must reach Albedo first. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ritualOnNigredoRefuses(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        plainVats(helper);
        List<String> messages = new ArrayList<>();
        Player player = offering(helper, messages, 1);
        CalcinerGameTests.use(helper, player, AlchemyGameTests.ATHANOR);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "the amethyst stays");
        helper.assertTrue(athanor.ritual() == null, "nothing starts");
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.ritual.tier_low gui.servus_alchemy.athanor.stage.albedo")),
                "Albedo is named, got " + messages);
        helper.succeed();
    }

    /** Breaking a grown vat drops the vat and a few Amethyst Shards, and the Athanor is Albedo at its next check. */
    @ServusGameTest(timeoutTicks = 20)
    public static void brokenGrownVatDropsShardsAndTheStage(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        AlchemyGameTests.saltRing(helper);
        AlchemyGameTests.citrinitasVats(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 2, "grown vats make Citrinitas, got " + athanor.tier());
        helper.getLevel().destroyBlock(helper.absolutePos(LEFT_VAT), true);
        helper.assertItemEntityPresent(ServusAlchemyItems.VAT.get(), LEFT_VAT, 2);
        helper.assertItemEntityPresent(Items.AMETHYST_SHARD, LEFT_VAT, 2);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 1, "the Athanor falls back to Albedo, got " + athanor.tier());
        helper.succeed();
    }

    /** A grown vat in a Coagulator's vat row is no Principle Vat: nothing forms and the vat is named. */
    @ServusGameTest(timeoutTicks = 20)
    public static void grownVatDoesNotFormACoagulator(GameTestHelper helper) {
        CoagulatorGameTests.build(helper, CoagulatorGameTests.COAGULATOR, Direction.NORTH);
        BlockPos vat = CoagulatorGameTests.vat(CoagulatorGameTests.COAGULATOR, 1);
        helper.setBlock(vat, helper.getBlockState(vat).setValue(VatBlock.CRYSTAL, VatCrystal.CITRINE));
        List<String> messages = new ArrayList<>();
        CalcinerGameTests.use(helper, AlchemyGameTests.listeningPlayer(helper, messages), CoagulatorGameTests.COAGULATOR);
        helper.assertTrue(!CoagulatorGameTests.isFormed(helper, CoagulatorGameTests.COAGULATOR), "nothing forms");
        helper.assertTrue(messages.size() == 1 && messages.get(0).startsWith("message.servus_alchemy.coagulator.missing block.servus_alchemy.principle_vat"),
                "the Principle Vat is named, got " + messages);
        helper.succeed();
    }

    /** A vat broken while the ritual runs drops as a plain vat; the other two grow, and the Athanor stays Albedo. */
    @ServusGameTest(timeoutTicks = 260)
    public static void vatTakenDuringTheRitualStaysPlain(GameTestHelper helper) {
        AthanorBlockEntity athanor = readyAthanor(helper);
        CalcinerGameTests.use(helper, offering(helper, new ArrayList<>(), 1), AlchemyGameTests.ATHANOR);
        helper.assertTrue(athanor.ritual() != null, "the ritual runs");
        int duration = citrinitas().duration();
        helper.runAfterDelay(duration / 2, () -> {
            helper.getLevel().destroyBlock(helper.absolutePos(RIGHT_VAT), true);
            helper.assertItemEntityPresent(ServusAlchemyItems.VAT.get(), RIGHT_VAT, 2);
            helper.assertItemEntityNotPresent(Items.AMETHYST_SHARD, RIGHT_VAT, 2);
        });
        helper.runAfterDelay(duration + 5, () -> {
            helper.assertTrue(helper.getBlockState(RIGHT_VAT).isAir(), "the broken vat is not put back");
            helper.assertTrue(crystal(helper, BACK_VAT) == VatCrystal.CITRINE && crystal(helper, LEFT_VAT) == VatCrystal.CITRINE,
                    "the other two vats grow");
            helper.assertTrue(athanor.tier() == 1, "the Athanor stays Albedo, got " + athanor.tier());
            helper.succeed();
        });
    }
}
