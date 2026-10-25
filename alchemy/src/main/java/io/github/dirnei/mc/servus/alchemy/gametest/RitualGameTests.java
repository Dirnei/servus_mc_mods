package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitualData;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RubedoObeliskBlock;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RubedoObeliskBlockEntity;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * GameTests of the Athanor rituals: casting the Citrinitas and the Rubedo ritual, their refusals, and what the grown vats
 * and the obelisks do.
 */
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
                helper.assertTrue(helper.getBlockEntity(vat, GrownVatBlockEntity.class) != null, "the grown vat draws its tower");
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
                + "message.servus_alchemy.athanor.place.outside message.servus_alchemy.athanor.tank.back")), "the vat behind is named, got " + messages);
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

    /** Breaking a grown vat drops the vat and a few Amethyst Shards, and the Athanor is Albedo. */
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

    private static AthanorRitual rubedo() {
        return AthanorRitualData.readShipped("rubedo");
    }

    /** The cells of the four obelisks' turns from {@code block}, for a north facing Athanor. */
    private static List<BlockPos> obeliskCells(String block) {
        return rubedo().turns(Facing.NORTH).stream().map(AthanorRitual.Turn::from).filter(from -> from.block().equals(block))
                .map(from -> AlchemyGameTests.ATHANOR.offset(from.dx(), from.dy(), from.dz())).toList();
    }

    private static final BlockPos LEFT_OBSIDIAN = AlchemyGameTests.ATHANOR.offset(2, -1, 1);

    /** A formed Citrinitas Athanor with Obsidian on the four pillar cells and air above them. */
    private static AthanorBlockEntity citrinitasWithObsidian(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        AlchemyGameTests.saltRing(helper);
        AlchemyGameTests.citrinitasVats(helper);
        obeliskCells("minecraft:obsidian").forEach(cell -> helper.setBlock(cell, Blocks.OBSIDIAN));
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 2, "the build is Citrinitas, got " + athanor.tier());
        return athanor;
    }

    /** A Rubedo Athanor: the salt ring, orange vats and four obelisks. */
    private static AthanorBlockEntity rubedoAthanor(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        AlchemyGameTests.saltRing(helper);
        AlchemyGameTests.citrinitasVats(helper);
        AlchemyGameTests.rubedoPillars(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 3, "the build is Rubedo, got " + athanor.tier());
        return athanor;
    }

    private static Player tear(GameTestHelper helper, List<String> messages) {
        Player player = AlchemyGameTests.listeningPlayer(helper, messages);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GHAST_TEAR));
        return player;
    }

    private static boolean obeliskStands(GameTestHelper helper, BlockPos lower) {
        BlockState bottom = helper.getBlockState(lower);
        BlockState top = helper.getBlockState(lower.above());
        return bottom.is(ServusAlchemyBlocks.RUBEDO_OBELISK.get()) && bottom.getValue(RubedoObeliskBlock.HALF) == DoubleBlockHalf.LOWER
                && top.is(ServusAlchemyBlocks.RUBEDO_OBELISK.get()) && top.getValue(RubedoObeliskBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    /** A Ghast Tear on a Citrinitas Athanor with Obsidian on the pillar cells grows four obelisks and orange vats: Rubedo. */
    @ServusGameTest(timeoutTicks = 260)
    public static void rubedoRitualGrowsObelisks(GameTestHelper helper) {
        AthanorBlockEntity athanor = citrinitasWithObsidian(helper);
        List<String> messages = new ArrayList<>();
        Player player = tear(helper, messages);
        CalcinerGameTests.use(helper, player, AlchemyGameTests.ATHANOR);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the tear is taken, held " + player.getMainHandItem());
        helper.assertTrue(athanor.ritual() != null && messages.isEmpty(), "the ritual runs, got " + messages);
        int duration = rubedo().duration();
        helper.runAfterDelay(duration / 2, () -> helper.assertTrue(obeliskCells("minecraft:obsidian").stream()
                .allMatch(cell -> helper.getBlockState(cell).is(Blocks.OBSIDIAN)), "nothing turns before the end"));
        helper.runAfterDelay(duration + 5, () -> {
            for (BlockPos cell : obeliskCells("minecraft:obsidian")) {
                helper.assertTrue(obeliskStands(helper, cell), "an obelisk grows at " + cell);
                helper.assertTrue(helper.getBlockEntity(cell, RubedoObeliskBlockEntity.class) != null, "the obelisk draws itself");
            }
            helper.assertTrue(VATS.stream().allMatch(vat -> crystal(helper, vat) == VatCrystal.RUBEDO), "the citrine turns orange");
            helper.assertTrue(athanor.ritual() == null, "the ritual is over");
            helper.assertTrue(athanor.tier() == 3 && helper.getBlockState(AlchemyGameTests.ATHANOR).getValue(AthanorBlock.TIER) == 3,
                    "the Athanor is Rubedo, got " + athanor.tier());
            helper.succeed();
        });
    }

    /** A torch above the Obsidian behind the left tank: the player keeps the tear and is told that space must be free. */
    @ServusGameTest(timeoutTicks = 20)
    public static void rubedoRitualNeedsFreeSpaceAbove(GameTestHelper helper) {
        AthanorBlockEntity athanor = citrinitasWithObsidian(helper);
        helper.setBlock(LEFT_OBSIDIAN.above(), Blocks.TORCH);
        List<String> messages = new ArrayList<>();
        Player player = tear(helper, messages);
        CalcinerGameTests.use(helper, player, AlchemyGameTests.ATHANOR);
        helper.assertTrue(player.getMainHandItem().is(Items.GHAST_TEAR), "the tear stays");
        helper.assertTrue(athanor.ritual() == null, "nothing starts");
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.ritual.blocked block.minecraft.obsidian "
                + "message.servus_alchemy.athanor.place.behind message.servus_alchemy.athanor.tank.left")),
                "the space above the Obsidian behind the left tank is named, got " + messages);
        helper.succeed();
    }

    /** Breaking the upper half of an obelisk removes both halves, drops its Obsidian, and the Athanor is Citrinitas again. */
    @ServusGameTest(timeoutTicks = 20)
    public static void brokenObeliskDropsObsidianAndTheStage(GameTestHelper helper) {
        AthanorBlockEntity athanor = rubedoAthanor(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(LEFT_OBSIDIAN.above()), true);
        helper.assertTrue(helper.getBlockState(LEFT_OBSIDIAN).isAir() && helper.getBlockState(LEFT_OBSIDIAN.above()).isAir(),
                "both halves are gone");
        helper.assertItemEntityCountIs(Items.OBSIDIAN, LEFT_OBSIDIAN, 2, 1);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 2, "the Athanor falls back to Citrinitas, got " + athanor.tier());
        helper.succeed();
    }

    /** With Obsidian put back where an obelisk broke, a Ghast Tear casts the ritual again and the Athanor is Rubedo. */
    @ServusGameTest(timeoutTicks = 260)
    public static void rubedoRitualCastsAgainAfterABrokenObelisk(GameTestHelper helper) {
        AthanorBlockEntity athanor = rubedoAthanor(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(LEFT_OBSIDIAN), false);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 2, "a broken obelisk is Citrinitas, got " + athanor.tier());
        assertPlainObsidian(helper, without(obeliskCells("minecraft:obsidian"), LEFT_OBSIDIAN));
        helper.setBlock(LEFT_OBSIDIAN, Blocks.OBSIDIAN);
        List<String> messages = new ArrayList<>();
        CalcinerGameTests.use(helper, tear(helper, messages), AlchemyGameTests.ATHANOR);
        helper.assertTrue(athanor.ritual() != null && messages.isEmpty(), "the ritual runs again, got " + messages);
        helper.runAfterDelay(rubedo().duration() + 5, () -> {
            helper.assertTrue(obeliskCells("minecraft:obsidian").stream().allMatch(cell -> obeliskStands(helper, cell)), "four obelisks regrow");
            helper.assertTrue(VATS.stream().allMatch(vat -> crystal(helper, vat) == VatCrystal.RUBEDO), "the vats turn orange again");
            helper.assertTrue(athanor.tier() == 3, "the Athanor is Rubedo, got " + athanor.tier());
            helper.succeed();
        });
    }

    /** Breaking an orange vat drops the vat and a few Amethyst Shards, and the Athanor is Albedo. */
    @ServusGameTest(timeoutTicks = 20)
    public static void brokenOrangeVatDropsToAlbedo(GameTestHelper helper) {
        AthanorBlockEntity athanor = rubedoAthanor(helper);
        helper.assertTrue(crystal(helper, LEFT_VAT) == VatCrystal.RUBEDO, "the vats are orange");
        helper.getLevel().destroyBlock(helper.absolutePos(LEFT_VAT), true);
        helper.assertItemEntityPresent(ServusAlchemyItems.VAT.get(), LEFT_VAT, 2);
        helper.assertItemEntityPresent(Items.AMETHYST_SHARD, LEFT_VAT, 2);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 1, "the Athanor falls back to Albedo, got " + athanor.tier());
        helper.succeed();
    }

    /** The vats are empty Principle Vats without a tower. */
    private static void assertEmptyVats(GameTestHelper helper, List<BlockPos> vats) {
        for (BlockPos vat : vats) {
            helper.assertTrue(crystal(helper, vat) == VatCrystal.NONE, "the vat at " + vat + " stands empty, got " + crystal(helper, vat));
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(vat)) == null, "the empty vat at " + vat + " draws no tower");
        }
    }

    /** Obsidian stands on the pillar cells with nothing above it. */
    private static void assertPlainObsidian(GameTestHelper helper, List<BlockPos> cells) {
        for (BlockPos cell : cells) {
            helper.assertTrue(helper.getBlockState(cell).is(Blocks.OBSIDIAN), "the obelisk at " + cell + " is Obsidian again, got "
                    + helper.getBlockState(cell));
            helper.assertTrue(helper.getBlockState(cell.above()).isAir(), "the space above " + cell + " is cleared");
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(cell)) == null, "the Obsidian at " + cell + " draws no obelisk");
        }
    }

    private static void assertNothingDropped(GameTestHelper helper) {
        helper.assertItemEntityNotPresent(Items.OBSIDIAN, AlchemyGameTests.ATHANOR, 6);
        helper.assertItemEntityNotPresent(ServusAlchemyItems.VAT.get(), AlchemyGameTests.ATHANOR, 6);
        helper.assertItemEntityNotPresent(Items.AMETHYST_SHARD, AlchemyGameTests.ATHANOR, 6);
    }

    private static List<BlockPos> without(List<BlockPos> cells, BlockPos broken) {
        return cells.stream().filter(cell -> !cell.equals(broken)).toList();
    }

    /** A broken grown vat of a Citrinitas Athanor empties the other two at once, without their drops, and the Athanor is Albedo. */
    @ServusGameTest(timeoutTicks = 20)
    public static void vatBrokenAtCitrinitasEmptiesTheOthers(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        AlchemyGameTests.saltRing(helper);
        AlchemyGameTests.citrinitasVats(helper);
        athanor.checkTier();
        helper.getLevel().destroyBlock(helper.absolutePos(LEFT_VAT), true);
        helper.succeedWhen(() -> {
            assertEmptyVats(helper, without(VATS, LEFT_VAT));
            helper.assertTrue(athanor.tier() == 1, "the Athanor is Albedo, got " + athanor.tier());
            helper.assertItemEntityCountIs(ServusAlchemyItems.VAT.get(), AlchemyGameTests.ATHANOR, 6, 1);
        });
    }

    /** A broken obelisk of a Rubedo Athanor turns the other three back into Obsidian and the vats back to citrine at once: Citrinitas. */
    @ServusGameTest(timeoutTicks = 20)
    public static void obeliskBrokenAtRubedoFallsToCitrinitas(GameTestHelper helper) {
        AthanorBlockEntity athanor = rubedoAthanor(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(LEFT_OBSIDIAN.above()), true);
        helper.succeedWhen(() -> {
            assertPlainObsidian(helper, without(obeliskCells("minecraft:obsidian"), LEFT_OBSIDIAN));
            helper.assertTrue(VATS.stream().allMatch(vat -> crystal(helper, vat) == VatCrystal.CITRINE), "the vats show citrine again");
            helper.assertTrue(athanor.tier() == 2, "the Athanor is Citrinitas, got " + athanor.tier());
            helper.assertItemEntityCountIs(Items.OBSIDIAN, AlchemyGameTests.ATHANOR, 6, 1);
        });
    }

    /** A broken orange vat of a Rubedo Athanor empties the other vats and turns the obelisks back into Obsidian at once: Albedo. */
    @ServusGameTest(timeoutTicks = 20)
    public static void orangeVatBrokenFallsToAlbedo(GameTestHelper helper) {
        AthanorBlockEntity athanor = rubedoAthanor(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(LEFT_VAT), true);
        helper.succeedWhen(() -> {
            assertEmptyVats(helper, without(VATS, LEFT_VAT));
            assertPlainObsidian(helper, obeliskCells("minecraft:obsidian"));
            helper.assertTrue(athanor.tier() == 1, "the Athanor is Albedo, got " + athanor.tier());
            helper.assertItemEntityNotPresent(Items.OBSIDIAN, AlchemyGameTests.ATHANOR, 6);
            helper.assertItemEntityCountIs(ServusAlchemyItems.VAT.get(), AlchemyGameTests.ATHANOR, 6, 1);
        });
    }

    /** A Salt Crystal Block taken from under a Rubedo Athanor turns everything grown back at its next check: Nigredo. */
    @ServusGameTest(timeoutTicks = 20)
    public static void saltBrokenFallsToNigredo(GameTestHelper helper) {
        AthanorBlockEntity athanor = rubedoAthanor(helper);
        helper.setBlock(AlchemyGameTests.ATHANOR.offset(0, -2, -2), Blocks.AIR);
        athanor.checkTier();
        assertEmptyVats(helper, VATS);
        assertPlainObsidian(helper, obeliskCells("minecraft:obsidian"));
        helper.assertTrue(athanor.tier() == 0, "the Athanor is Nigredo, got " + athanor.tier());
        helper.assertBlockProperty(AlchemyGameTests.ATHANOR, AthanorBlock.FORMED, true);
        assertNothingDropped(helper);
        helper.succeed();
    }

    /** A hearth slab taken from a Rubedo Athanor unforms it at its next check and turns everything grown back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void hearthBrokenUndoesEverything(GameTestHelper helper) {
        AthanorBlockEntity athanor = rubedoAthanor(helper);
        helper.setBlock(AlchemyGameTests.ATHANOR.offset(1, -1, 1), Blocks.AIR);
        athanor.checkTier();
        helper.assertBlockProperty(AlchemyGameTests.ATHANOR, AthanorBlock.FORMED, false);
        assertEmptyVats(helper, VATS);
        assertPlainObsidian(helper, obeliskCells("minecraft:obsidian"));
        assertNothingDropped(helper);
        helper.succeed();
    }
}
