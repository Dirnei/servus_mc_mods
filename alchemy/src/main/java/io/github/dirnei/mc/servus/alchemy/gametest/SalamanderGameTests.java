package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.salamander.Salamander;
import io.github.dirnei.mc.servus.alchemy.salamander.SalamanderTemper;
import io.github.dirnei.mc.servus.alchemy.salamander.SalamanderTemperData;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The salamander: befriended by gold, a pet that sits on command, digests gold into Sal Ammoniac and never burns. */
public final class SalamanderGameTests {

    private static final BlockPos SPOT = new BlockPos(8, 1, 8);
    private static final SalamanderTemper ALWAYS = new SalamanderTemper(1, 20);
    private static final SalamanderTemper NEVER = new SalamanderTemper(0, 20);

    private SalamanderGameTests() {
    }

    private static Salamander salamander(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(ServusAlchemyEntities.SALAMANDER.get(), SPOT);
    }

    private static Player playerWithGold(GameTestHelper helper, int count) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT, count));
        return player;
    }

    private static Salamander befriended(GameTestHelper helper, Player owner) {
        Salamander salamander = salamander(helper);
        salamander.offerGold(owner, owner.getMainHandItem(), ALWAYS);
        helper.assertTrue(salamander.isTame(), "the salamander should be befriended");
        return salamander;
    }

    /** With a chance of 1 the first Gold Ingot befriends the salamander and the player owns it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void goldBefriendsWithChanceOne(GameTestHelper helper) {
        Salamander salamander = salamander(helper);
        Player player = playerWithGold(helper, 3);
        helper.assertTrue(!salamander.isTame(), "a fresh salamander is wild");
        InteractionResult result = salamander.offerGold(player, player.getMainHandItem(), ALWAYS);
        helper.assertTrue(result.consumesAction(), "the ingot is taken");
        helper.assertTrue(salamander.isTame() && salamander.isOwnedBy(player), "the player owns the salamander");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "one ingot is used up");
        helper.assertTrue(!salamander.isDigesting(), "befriending gold is not digested");
        helper.succeed();
    }

    /** With a chance of 0 the ingot is still eaten, and the salamander stays wild. */
    @ServusGameTest(timeoutTicks = 20)
    public static void goldWithChanceZeroOnlyUsesTheIngot(GameTestHelper helper) {
        Salamander salamander = salamander(helper);
        Player player = playerWithGold(helper, 3);
        for (int i = 0; i < 3; i++) {
            salamander.offerGold(player, player.getMainHandItem(), NEVER);
        }
        helper.assertTrue(!salamander.isTame(), "the salamander stays wild");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "all three ingots are used up");
        helper.succeed();
    }

    /** The owner's empty hand makes it sit and stand again; a stranger's does nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ownerEmptyHandTogglesSitting(GameTestHelper helper) {
        Player owner = playerWithGold(helper, 1);
        Salamander salamander = befriended(helper, owner);
        helper.assertTrue(owner.getMainHandItem().isEmpty(), "the owner's hand is empty now");
        helper.assertTrue(!salamander.isOrderedToSit(), "a fresh friend stands");
        salamander.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(salamander.isOrderedToSit(), "the owner's click makes it sit");
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        salamander.mobInteract(stranger, InteractionHand.MAIN_HAND);
        helper.assertTrue(salamander.isOrderedToSit(), "a stranger's click changes nothing");
        salamander.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(!salamander.isOrderedToSit(), "a second click makes it stand");
        helper.succeed();
    }

    /** A befriended salamander fed a Gold Ingot drops one Sal Ammoniac after the digest time, one ingot at a time. */
    @ServusGameTest(timeoutTicks = 100)
    public static void befriendedDigestsGoldIntoSalAmmoniac(GameTestHelper helper) {
        Player owner = playerWithGold(helper, 3);
        Salamander salamander = befriended(helper, owner);
        helper.assertTrue(salamander.offerGold(owner, owner.getMainHandItem(), ALWAYS).consumesAction(), "the owner's gold is taken");
        helper.assertTrue(salamander.isDigesting(), "it digests");
        helper.assertTrue(!salamander.offerGold(owner, owner.getMainHandItem(), ALWAYS).consumesAction(), "a second ingot is refused");
        helper.assertTrue(owner.getMainHandItem().getCount() == 1, "only the befriending and the fed ingot are used up");
        helper.runAfterDelay(5, () -> helper.assertTrue(helper.getEntities(EntityType.ITEM, SPOT, 3).isEmpty(),
                "nothing drops before the digest time"));
        helper.succeedWhen(() -> {
            var drops = helper.getEntities(EntityType.ITEM, SPOT, 3);
            helper.assertTrue(drops.size() == 1, "one drop expected, got " + drops.size());
            ItemStack drop = drops.getFirst().getItem();
            helper.assertTrue(drop.is(ServusAlchemyItems.SAL_AMMONIAC.get()) && drop.getCount() == 1, "one Sal Ammoniac, got " + drop);
            helper.assertTrue(!salamander.isDigesting(), "the stomach is empty again");
        });
    }

    /** Only the owner can feed a befriended salamander. */
    @ServusGameTest(timeoutTicks = 20)
    public static void strangerCannotFeed(GameTestHelper helper) {
        Salamander salamander = befriended(helper, playerWithGold(helper, 1));
        Player stranger = playerWithGold(helper, 2);
        helper.assertTrue(!salamander.offerGold(stranger, stranger.getMainHandItem(), ALWAYS).consumesAction(), "a stranger's gold is refused");
        helper.assertTrue(stranger.getMainHandItem().getCount() == 2 && !salamander.isDigesting(), "nothing is eaten");
        helper.succeed();
    }

    /** Lava and fire do not hurt it, and it never burns. */
    @ServusGameTest(timeoutTicks = 80)
    public static void fireImmuneInLava(GameTestHelper helper) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(SPOT.offset(dx, 0, dz), Blocks.STONE);
            }
        }
        helper.setBlock(SPOT, Blocks.LAVA);
        Salamander salamander = salamander(helper);
        helper.assertTrue(salamander.getType().fireImmune(), "the type is fire immune");
        float health = salamander.getHealth();
        boolean hurt = salamander.hurtServer(helper.getLevel(), helper.getLevel().damageSources().inFire(), 4);
        helper.assertTrue(!hurt, "fire damage does not hurt");
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(salamander.isAlive() && salamander.getHealth() == health,
                    "no damage after standing in lava, health " + salamander.getHealth() + " of " + health);
            helper.assertTrue(salamander.getRemainingFireTicks() <= 0 && !salamander.isOnFire(), "it does not burn");
            helper.succeed();
        });
    }

    /** A wild salamander may despawn far from players, a befriended one never. */
    @ServusGameTest(timeoutTicks = 20)
    public static void tamedDoesNotDespawnWildDoes(GameTestHelper helper) {
        Salamander wild = salamander(helper);
        helper.assertTrue(wild.removeWhenFarAway(10_000), "a wild salamander may despawn");
        Salamander pet = befriended(helper, playerWithGold(helper, 1));
        helper.assertTrue(!pet.removeWhenFarAway(10_000), "a befriended salamander stays");
        helper.succeed();
    }

    /** The shipped temper data loads and the biome modifier that spawns salamanders is registered. */
    @ServusGameTest(timeoutTicks = 20)
    public static void shippedTemperAndSpawnsLoad(GameTestHelper helper) {
        SalamanderTemper temper = SalamanderTemperData.INSTANCE.forType(ServusAlchemyEntities.SALAMANDER.get());
        helper.assertTrue(temper.equals(new SalamanderTemper(0.33, 1200)), "shipped temper expected, got " + temper);
        var modifiers = helper.getLevel().registryAccess().lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        helper.assertTrue(modifiers.get(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "salamander"))).isPresent(), "the salamander biome modifier is loaded");
        helper.succeed();
    }

    /** It spawns on lava with air above, not under a roof and not on stone. */
    @ServusGameTest(timeoutTicks = 20)
    public static void spawnsOnlyOnOpenLava(GameTestHelper helper) {
        BlockPos open = new BlockPos(4, 1, 4);
        BlockPos roofed = new BlockPos(6, 1, 4);
        helper.setBlock(open, Blocks.LAVA);
        helper.setBlock(roofed, Blocks.LAVA);
        helper.setBlock(roofed.above(), Blocks.STONE);
        var type = ServusAlchemyEntities.SALAMANDER.get();
        var level = helper.getLevel();
        var random = level.getRandom();
        helper.assertTrue(Salamander.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, helper.absolutePos(open), random),
                "open lava allows a spawn");
        helper.assertTrue(!Salamander.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, helper.absolutePos(roofed), random),
                "roofed lava does not");
        helper.succeed();
    }

    /** Sal Ammoniac and Sulfur make Ignis Perpetuus on a Citrinitas Athanor, not on an Albedo one. */
    @ServusGameTest(timeoutTicks = 600)
    public static void ignisNeedsCitrinitas(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        AlchemyGameTests.saltRing(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 1, "the salt ring makes Albedo, got " + athanor.tier());
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(ServusAlchemyItems.SAL_AMMONIAC.get()));
        helper.runAfterDelay(2, () -> AlchemyGameTests.pour(helper, new Principles(0, 500, 0)));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "no Ignis Perpetuus at Albedo");
            AlchemyGameTests.amethystRing(helper);
            athanor.checkTier();
            helper.assertTrue(athanor.tier() == 2, "the amethyst ring makes Citrinitas, got " + athanor.tier());
            helper.succeedWhen(() -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(ServusAlchemyItems.IGNIS_PERPETUUS.get()),
                        "Ignis Perpetuus should be made");
                helper.assertTrue(athanor.principles().isEmpty(), "the Sulfur is burned");
            });
        });
    }

    /** Ignis Perpetuus is a building part: no furnace burns it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ignisIsNotAFuel(GameTestHelper helper) {
        ItemStack ignis = new ItemStack(ServusAlchemyItems.IGNIS_PERPETUUS.get());
        helper.assertTrue(!helper.getLevel().fuelValues().isFuel(ignis), "Ignis Perpetuus must not be a fuel");
        helper.assertTrue(helper.getLevel().fuelValues().isFuel(new ItemStack(Items.COAL)), "coal still is");
        helper.succeed();
    }
}
