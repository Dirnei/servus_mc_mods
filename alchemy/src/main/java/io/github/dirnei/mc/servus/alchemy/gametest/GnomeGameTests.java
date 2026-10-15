package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.gnome.Gnome;
import io.github.dirnei.mc.servus.alchemy.gnome.GnomeKind;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlock;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The gnome entities: the Old Miner of a Gnome Hollow, the Lantern Bearer and the House Gnome. */
public final class GnomeGameTests {

    private static final BlockPos HOLLOW = AlchemyGameTests.MACHINE;

    private GnomeGameTests() {
    }

    /** A Hollow at {@link #HOLLOW} opening to the north, owned by a fresh team. */
    static GnomeHollowBlockEntity northHollow(GameTestHelper helper) {
        helper.setBlock(HOLLOW, ServusAlchemyBlocks.GNOME_HOLLOW.get().defaultBlockState().setValue(GnomeHollowBlock.FACING, Direction.NORTH));
        GnomeHollowBlockEntity hollow = helper.getBlockEntity(HOLLOW, GnomeHollowBlockEntity.class);
        hollow.setTeam(UUID.randomUUID());
        return hollow;
    }

    /** A Hollow whose gnome was called with a Salt Crystal by a player under the sight. */
    static GnomeHollowBlockEntity calledHollow(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = northHollow(helper);
        helper.useBlock(HOLLOW, HollowGameTests.player(helper, new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get()), true));
        return hollow;
    }

    static Gnome gnomeOf(GameTestHelper helper, GnomeHollowBlockEntity hollow) {
        return hollow.gnome().orElseThrow(() -> helper.assertionException(Component.literal("the Hollow should keep a gnome")));
    }

    /** Every gnome within {@code radius} of {@code relative}. */
    static List<Gnome> gnomesNear(GameTestHelper helper, BlockPos relative, double radius) {
        return helper.getLevel().getEntitiesOfClass(Gnome.class, new AABB(helper.absolutePos(relative)).inflate(radius));
    }

    /** A Salt Crystal under the sight brings an Old Miner out of the Hollow's open side, kept by the Hollow. */
    @ServusGameTest(timeoutTicks = 20)
    public static void saltCrystalBringsTheOldMiner(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = calledHollow(helper);
        Gnome gnome = gnomeOf(helper, hollow);
        helper.assertTrue(gnome.kind() == GnomeKind.MINER, "the Hollow's gnome is an Old Miner");
        helper.assertTrue(gnome.livesAt(hollow.getBlockPos()) && hollow.keepsGnome(gnome.getUUID()), "he belongs to his Hollow");
        Vec3 front = Vec3.atBottomCenterOf(helper.absolutePos(HOLLOW.north()));
        helper.assertTrue(gnome.position().distanceTo(front) < 0.01, "he steps out of the cave entry, got " + gnome.position());
        helper.assertTrue(gnomesNear(helper, HOLLOW, 8).size() == 1, "one gnome per Hollow");
        helper.succeed();
    }

    /** With stone in front of the cave entry he comes out of another side. */
    @ServusGameTest(timeoutTicks = 20)
    public static void walledUpHollowLetsHimOutElsewhere(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = northHollow(helper);
        helper.setBlock(HOLLOW.north(), Blocks.STONE);
        hollow.bind(HollowGameTests.player(helper, ItemStack.EMPTY, true));
        Gnome gnome = gnomeOf(helper, hollow);
        helper.assertFalse(gnome.blockPosition().equals(helper.absolutePos(HOLLOW.north())), "he is not walled in");
        helper.assertTrue(gnome.blockPosition().distManhattan(helper.absolutePos(HOLLOW)) == 1, "he stands beside his Hollow");
        helper.succeed();
    }

    /** Breaking the Hollow sends its Old Miner back into the rock. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingTheHollowSendsHimAway(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = calledHollow(helper);
        Gnome gnome = gnomeOf(helper, hollow);
        helper.destroyBlock(HOLLOW);
        helper.assertTrue(gnome.isRemoved(), "the gnome is gone with his Hollow");
        helper.assertTrue(gnomesNear(helper, HOLLOW, 8).isEmpty(), "no gnome is left");
        helper.succeed();
    }

    /** A bound Hollow whose gnome went missing brings one out again after a few seconds. */
    @ServusGameTest(timeoutTicks = 140)
    public static void lostGnomeComesBack(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = calledHollow(helper);
        gnomeOf(helper, hollow).discard();
        helper.succeedWhen(() -> {
            helper.assertTrue(hollow.gnome().isPresent(), "a new gnome should come out");
            helper.assertTrue(gnomesNear(helper, HOLLOW, 8).size() == 1, "only one gnome");
        });
    }

    /** A gnome his Hollow does not keep, a stray copy after a load, leaves by himself; the kept one stays. */
    @ServusGameTest(timeoutTicks = 60)
    public static void strayGnomeLeaves(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = calledHollow(helper);
        Gnome kept = gnomeOf(helper, hollow);
        ServerLevel level = helper.getLevel();
        Gnome stray = Gnome.spawn(level, GnomeKind.MINER, hollow.getBlockPos(), kept.position(), 0)
                .orElseThrow(() -> helper.assertionException(Component.literal("the stray should spawn")));
        helper.succeedWhen(() -> {
            helper.assertTrue(stray.isRemoved(), "the stray should leave");
            helper.assertFalse(kept.isRemoved(), "the kept gnome stays");
        });
    }

    /** A gnome takes no damage, cannot be pushed or leashed and never despawns. */
    @ServusGameTest(timeoutTicks = 20)
    public static void gnomeIsUnhurt(GameTestHelper helper) {
        Gnome gnome = gnomeOf(helper, calledHollow(helper));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ServerLevel level = helper.getLevel();
        helper.assertFalse(gnome.hurtServer(level, level.damageSources().playerAttack(player), 10), "a hit does not hurt him");
        helper.assertTrue(gnome.getHealth() == gnome.getMaxHealth(), "his health stays full");
        helper.assertFalse(gnome.isPushable() || gnome.canBeLeashed() || gnome.isAttackable(), "no push, leash or attack");
        helper.assertTrue(gnome.requiresCustomPersistence() && !gnome.removeWhenFarAway(10_000), "he never despawns");
        helper.succeed();
    }

    /** Each spawn egg brings a gnome of its own kind. */
    @ServusGameTest(timeoutTicks = 20)
    public static void spawnEggsBringTheirKind(GameTestHelper helper) {
        assertEggKind(helper, new ItemStack(ServusAlchemyItems.MINER_GNOME_SPAWN_EGG.get()), GnomeKind.MINER, 1);
        assertEggKind(helper, new ItemStack(ServusAlchemyItems.LANTERN_GNOME_SPAWN_EGG.get()), GnomeKind.LANTERN, 3);
        assertEggKind(helper, new ItemStack(ServusAlchemyItems.HOUSE_GNOME_SPAWN_EGG.get()), GnomeKind.HOUSE, 5);
        helper.succeed();
    }

    private static void assertEggKind(GameTestHelper helper, ItemStack egg, GnomeKind kind, int x) {
        Gnome gnome = ServusAlchemyEntities.GNOME.get().spawn(helper.getLevel(), egg, null, helper.absolutePos(new BlockPos(x, 1, 6)),
                EntitySpawnReason.SPAWN_ITEM_USE, false, false);
        helper.assertTrue(gnome != null && gnome.kind() == kind, "the egg should bring a " + kind.serializedName());
    }
}
