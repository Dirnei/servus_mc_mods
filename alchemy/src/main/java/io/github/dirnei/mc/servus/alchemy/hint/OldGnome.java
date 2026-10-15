package io.github.dirnei.mc.servus.alchemy.hint;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.gnome.Gnome;
import io.github.dirnei.mc.servus.alchemy.gnome.GnomeKind;
import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowSite.Site;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnomeLedger.Entry;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Paracelsus' old gnome from the mines of Schwaz. He sleeps until a team that finished the wake chapter mines deep; then a
 * wall nearby becomes his Ancient Hollow, which knocks, lets dust fall and leaves traces until the team finds him under the
 * sight. His apprentice, the Lantern Bearer, appears where the waking block was broken and walks ahead to the Hollow.
 * Everything here reacts to a block break, a pickup or a click; nothing scans the world.
 */
public final class OldGnome {

    /** Blocks whose breaking may wake him and that may become his Hollow. */
    public static final TagKey<Block> WAKES = TagKey.create(Registries.BLOCK, id("wakes_old_gnome"));
    /** The small things he leaves behind. */
    public static final TagKey<Item> TRACES = TagKey.create(Registries.ITEM, id("hint_traces"));
    /** The journal event of finding something hidden. */
    public static final String FIND = "find";
    /** The name a {@code find} task filters on for the old gnome himself. */
    public static final String OLD_GNOME = ServusAlchemyIds.MOD_ID + ":old_gnome";
    private static final String MESSAGE = "message.servus_alchemy.old_gnome.";
    private static final List<Supplier<? extends Item>> TRACE_ITEMS = List.of(ServusAlchemyItems.TINY_PICKAXE,
            ServusAlchemyItems.RED_FELT_SCRAP, ServusAlchemyItems.SALT_OFFERING);

    private OldGnome() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, path);
    }

    /** The team of a player: their team on a server, else their own id. */
    public static UUID teamOf(Player player) {
        return player instanceof ServerPlayer server ? Teams.teamOf(server) : player.getUUID();
    }

    /**
     * A team member breaks {@code state} at {@code pos}: it may wake the old gnome or, near his Hollow, drop a trace.
     *
     * @return the woken Ancient Hollow's position, empty when he did not wake
     */
    public static Optional<BlockPos> onBroken(ServerLevel level, UUID team, BlockPos pos, BlockState state, OldGnomeSettings settings,
                                              RandomSource random) {
        if (team == null || !state.is(WAKES)) {
            return Optional.empty();
        }
        Optional<BlockPos> woken = tryWake(level, team, pos, settings, random);
        if (woken.isEmpty()) {
            maybeDropTrace(level, team, pos, settings, random);
        }
        return woken;
    }

    private static Optional<BlockPos> tryWake(ServerLevel level, UUID team, BlockPos pos, OldGnomeSettings settings, RandomSource random) {
        OldGnomeSavedData data = OldGnomeSavedData.get(level.getServer());
        if (pos.getY() >= settings.maxY() || !data.ledger().mayWake(team)
                || !Journal.isFinished(level.getServer(), team, settings.wakeChapter())
                || random.nextDouble() >= settings.wakeChance()) {
            return Optional.empty();
        }
        Optional<Site> site = AncientHollowSite.choose(random.nextInt(AncientHollowSite.SIDES.length),
                (dx, dy, dz) -> level.getBlockState(pos.offset(dx, dy, dz)).is(WAKES),
                (dx, dy, dz) -> level.getBlockState(pos.offset(dx, dy, dz)).isAir());
        if (site.isEmpty()) {
            return Optional.empty();
        }
        BlockPos at = pos.offset(site.get().dx(), site.get().dy(), site.get().dz());
        int[] side = AncientHollowSite.SIDES[site.get().side()];
        Direction face = Direction.getNearest(side[0], 0, side[1], Direction.NORTH);
        level.setBlock(at, ServusAlchemyBlocks.ANCIENT_HOLLOW.get().defaultBlockState().setValue(AncientHollowBlock.FACING, face),
                Block.UPDATE_ALL);
        if (!(level.getBlockEntity(at) instanceof AncientHollowBlockEntity hollow)) {
            return Optional.empty();
        }
        hollow.setTeam(team);
        hollow.summonGnome(level);
        Vec3 apprentice = Vec3.atBottomCenterOf(pos);
        Gnome.spawn(level, GnomeKind.LANTERN, at, apprentice, yRotTowards(apprentice, hollow.inFront(0.5)));
        data.wake(team, at.asLong(), level.dimension().identifier().toString());
        dropTrace(level, hollow.inFront(0.3).subtract(0, 0.3, 0), random);
        hollow.knock();
        return Optional.of(at);
    }

    private static float yRotTowards(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private static void maybeDropTrace(ServerLevel level, UUID team, BlockPos pos, OldGnomeSettings settings, RandomSource random) {
        Optional<Entry> awake = OldGnomeSavedData.get(level.getServer()).ledger().awake(team);
        if (awake.isEmpty() || !awake.get().dimension().equals(level.dimension().identifier().toString())) {
            return;
        }
        BlockPos hollow = BlockPos.of(awake.get().pos());
        if (hollow.equals(pos) || hollow.distSqr(pos) > (double) settings.traceRange() * settings.traceRange()
                || random.nextDouble() >= settings.traceChance()) {
            return;
        }
        dropTrace(level, pos.getCenter(), random);
    }

    private static void dropTrace(ServerLevel level, Vec3 at, RandomSource random) {
        ItemStack trace = new ItemStack(TRACE_ITEMS.get(random.nextInt(TRACE_ITEMS.size())).get());
        ItemEntity entity = new ItemEntity(level, at.x, at.y, at.z, trace, 0, 0, 0);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    /** A player picked up a trace: the journal counts the find and reacts. */
    public static void onTracePickedUp(ServerLevel level, Player player, ItemStack trace) {
        Journal.recordItem(level, teamOf(player), FIND, trace, 1);
        player.sendOverlayMessage(Component.translatable(MESSAGE + "trace"));
    }

    /** A player under the sight uses the journal on the Ancient Hollow: the old gnome speaks and is found. */
    public static void find(ServerLevel level, Player player, AncientHollowBlockEntity hollow) {
        player.sendSystemMessage(Component.translatable(MESSAGE + "greeting"));
        find(level, teamOf(player), hollow);
    }

    /** The finder's team records the find; the team he woke for has found him. */
    public static void find(ServerLevel level, UUID finder, AncientHollowBlockEntity hollow) {
        Journal.recordNamed(level, finder, FIND, OLD_GNOME);
        if (!hollow.found()) {
            hollow.markFound();
            if (hollow.team() != null) {
                OldGnomeSavedData.get(level.getServer()).found(hollow.team());
            }
        }
    }

    /** His Hollow at {@code pos} was broken; before he was found he may wake again elsewhere. */
    public static void lost(ServerLevel level, UUID team, BlockPos pos) {
        OldGnomeSavedData.get(level.getServer()).lost(team, pos.asLong());
    }
}
