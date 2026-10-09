package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.hollow.GnomeHollowBlockEntity.Binding;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Gnome Hollow: seeding, ripening, binding, working. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HollowGameTests {

    private static final String EMPTY = AlchemyGameTests.EMPTY;
    private static final BlockPos MACHINE = AlchemyGameTests.MACHINE;
    private static final int IRON_STEPS = 20;

    private HollowGameTests() {
    }

    static GnomeHollowBlockEntity hollow(GameTestHelper helper, UUID team) {
        helper.setBlock(MACHINE, ServusManufacturingBlocks.GNOME_HOLLOW.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        GnomeHollowBlockEntity hollow = helper.getBlockEntity(MACHINE);
        hollow.setTeam(team);
        return hollow;
    }

    static void finishVillach(ServerLevel level, UUID team) {
        Journal.recordItem(level, team, "calcine", new ItemStack(Items.COBBLESTONE), 100);
        Journal.recordPrinciple(level, team, Principle.SALT, 1000);
        Journal.recordItem(level, team, "coagulate", new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get()), 1);
    }

    /** A seeded iron vein, ripe or not, for a team that finished Villach. */
    static GnomeHollowBlockEntity ironHollow(GameTestHelper helper, boolean ripe, Runnable then) {
        UUID team = UUID.randomUUID();
        finishVillach(helper.getLevel(), team);
        GnomeHollowBlockEntity hollow = hollow(helper, team);
        hollow.inventory().setStackInSlot(GnomeHollowBlockEntity.SLOT_SAMPLE, new ItemStack(Items.RAW_IRON));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(hollow.vein().isPresent(), "the sample should seed a vein");
            if (ripe) {
                hollow.growth().ripeness = IRON_STEPS;
            }
            then.run();
        });
        return hollow;
    }

    /** Raw iron seeds an iron vein only once the team finished Villach; the sample is used up. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void seedingNeedsVillach(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        GnomeHollowBlockEntity hollow = hollow(helper, team);
        ItemStack sample = new ItemStack(Items.RAW_IRON);
        helper.assertTrue(!hollow.isItemValid(GnomeHollowBlockEntity.SLOT_SAMPLE, sample), "no iron before Villach");
        finishVillach(helper.getLevel(), team);
        helper.assertTrue(hollow.isItemValid(GnomeHollowBlockEntity.SLOT_SAMPLE, sample), "iron after Villach");
        helper.assertTrue(!hollow.isItemValid(GnomeHollowBlockEntity.SLOT_SAMPLE, new ItemStack(Items.COBBLESTONE)), "cobblestone seeds nothing");
        hollow.inventory().setStackInSlot(GnomeHollowBlockEntity.SLOT_SAMPLE, sample);
        helper.succeedWhen(() -> {
            helper.assertTrue(hollow.vein().map(id -> id.getPath().equals("iron")).orElse(false), "an iron vein should be seeded");
            helper.assertTrue(hollow.inventory().getStackInSlot(GnomeHollowBlockEntity.SLOT_SAMPLE).isEmpty(), "the sample is used up");
            helper.assertTrue(!hollow.isItemValid(GnomeHollowBlockEntity.SLOT_SAMPLE, new ItemStack(Items.RAW_COPPER)), "one vein per hollow");
        });
    }

    /** One ripening step takes its principles and waits for more. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void ripeningTakesPrinciples(GameTestHelper helper) {
        GnomeHollowBlockEntity[] holder = new GnomeHollowBlockEntity[1];
        holder[0] = ironHollow(helper, false, () -> holder[0].tanks().insert(new Principles(150, 0, 30)));
        helper.runAfterDelay(250, () -> {
            GnomeHollowBlockEntity hollow = holder[0];
            helper.assertTrue(hollow.growth().ripeness == 1, "one step should have grown, got " + hollow.growth().ripeness);
            helper.assertTrue(hollow.tanks().amount(Principle.SALT) == 50, "50 mB Salt should remain, got " + hollow.tanks().amount(Principle.SALT));
            helper.assertTrue(hollow.tanks().amount(Principle.MERCURY) == 10, "10 mB Mercury should remain");
            helper.succeed();
        });
    }

    /** Only a player with Elemental Sight can bind a gnome, and only to a ripe vein. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void bindingNeedsElementalSight(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GnomeHollowBlockEntity[] holder = new GnomeHollowBlockEntity[1];
        holder[0] = ironHollow(helper, false, () -> {
            GnomeHollowBlockEntity hollow = holder[0];
            player.addEffect(new MobEffectInstance(ElementalSight.EFFECT, 200));
            helper.assertTrue(hollow.bind(player) == Binding.NOT_RIPE, "an unripe vein takes no gnome");
            hollow.growth().ripeness = IRON_STEPS;
            player.removeAllEffects();
            helper.assertTrue(hollow.bind(player) == Binding.NO_SIGHT, "without the sight no gnome is bound");
            player.addEffect(new MobEffectInstance(ElementalSight.EFFECT, 200));
            helper.assertTrue(hollow.bind(player) == Binding.BOUND, "with the sight a gnome is bound");
            helper.assertTrue(hollow.bind(player) == Binding.ALREADY_BOUND, "one gnome per vein");
            helper.succeed();
        });
    }

    /** A bound gnome digs raw iron at the vein's rate and the Hollow pushes it into a chest. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void boundHollowYieldsIntoAChest(GameTestHelper helper) {
        BlockPos chestPos = MACHINE.east();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(chestPos);
        GnomeHollowBlockEntity[] holder = new GnomeHollowBlockEntity[1];
        holder[0] = ironHollow(helper, true, () -> {
            holder[0].growth().bound = true;
            holder[0].sideConfig().set(RelativeSide.LEFT, SideMode.PUSH);
        });
        helper.succeedWhen(() -> helper.assertTrue(chest.countItem(Items.RAW_IRON) >= 2, "the chest should get raw iron"));
    }

    /** Binding a gnome counts for Schwaz. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void bindingCountsForSchwaz(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(ElementalSight.EFFECT, 200));
        GnomeHollowBlockEntity[] holder = new GnomeHollowBlockEntity[1];
        holder[0] = ironHollow(helper, true, () -> {
            holder[0].bind(player);
            int count = Journal.journalOf(helper.getLevel().getServer(), holder[0].team()).count("servus_manufacturing:schwaz", "gnome");
            helper.assertTrue(count == 1, "the bound gnome should count for Schwaz, got " + count);
            helper.succeed();
        });
    }

    /** A working Hollow is a producer of its yield with no inputs. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void workingHollowIsAProducer(GameTestHelper helper) {
        GnomeHollowBlockEntity[] holder = new GnomeHollowBlockEntity[1];
        holder[0] = ironHollow(helper, true, () -> {
            ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION,
                    helper.absolutePos(MACHINE), Direction.UP);
            helper.assertTrue(production != null && production.recipes().isEmpty(), "no recipes before a gnome is bound");
            holder[0].growth().bound = true;
            helper.assertTrue(production.recipes().size() == 1, "one recipe once a gnome works");
            helper.assertTrue(production.recipes().get(0).inputs().isEmpty(), "no inputs");
            helper.assertTrue(production.recipes().get(0).outputs().get(0).is(Items.RAW_IRON), "raw iron out");
            helper.succeed();
        });
    }
}
