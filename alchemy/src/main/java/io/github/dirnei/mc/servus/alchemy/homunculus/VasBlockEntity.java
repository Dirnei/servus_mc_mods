package io.github.dirnei.mc.servus.alchemy.homunculus;

import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.homunculus.HomunculusLife.Step;
import io.github.dirnei.mc.servus.alchemy.homunculus.HomunculusReach.Offset;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Vas Hermeticum and the homunculus in it. It lives on the Elixir in the Receptaculum below: it grows, then
 * harvests the Gnome Hollows in its reach into the Receptaculum and brings their gnomes gifts. Once grown it whispers
 * to those who see it; a Nether Star frees it. Clients get its growth and the Hollows in reach for drawing under the sight.
 */
public class VasBlockEntity extends BlockEntity {

    /** Lines of the whisper and the secret in the lang file, numbered from 1. */
    public static final int WHISPER_LINES = 5;
    public static final int SECRET_LINES = 4;
    private static final double WHISPER_RANGE_SQ = 8 * 8;
    /** The client is told about the growth in this many steps. */
    private static final int SHOWN_STEPS = 16;

    private final HomunculusLife life = new HomunculusLife();
    private final ElixirUpkeep upkeep = new ElixirUpkeep();
    private final WhisperClock whispers = new WhisperClock();
    /** The Hollows found in reach at the last harvest; on the client, as last synced. */
    private List<Offset> reach = List.of();
    private float shownGrowth;

    public VasBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.VAS_HERMETICUM.get(), pos, state);
    }

    public HomunculusLife life() {
        return life;
    }

    public ElixirUpkeep upkeep() {
        return upkeep;
    }

    private static HomunculusSettings settings() {
        return HomunculusData.INSTANCE.settings();
    }

    /** Items delivered per item taken: 1, or the freed multiplier. */
    public int multiplier() {
        return life.freed() ? settings().freedMultiplier() : 1;
    }

    /** Whether a grown homunculus works: freed, or with a bottle in use or one in the Receptaculum. */
    public boolean works(boolean elixirWaiting) {
        return life.grown() && (life.freed() || upkeep.ticksLeft() > 0 || elixirWaiting);
    }

    /** The Receptaculum's level, where the homunculus reaches: the block below the Vas. */
    private BlockPos receptaculumPos() {
        return worldPosition.below();
    }

    /** The Gnome Hollows in reach now, looked up once per call. */
    public List<GnomeHollowBlockEntity> hollowsInReach() {
        List<GnomeHollowBlockEntity> hollows = new ArrayList<>();
        if (level == null) {
            return hollows;
        }
        BlockPos centre = receptaculumPos();
        for (Offset offset : HomunculusReach.offsets()) {
            if (level.getBlockEntity(centre.offset(offset.dx(), 0, offset.dz())) instanceof GnomeHollowBlockEntity hollow) {
                hollows.add(hollow);
            }
        }
        return hollows;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VasBlockEntity vas) {
        if (level.getBlockEntity(pos.below()) instanceof ReceptaculumBlockEntity receptaculum) {
            vas.tick(receptaculum);
        }
    }

    private void tick(ReceptaculumBlockEntity receptaculum) {
        HomunculusSettings settings = settings();
        boolean fed = !life.needsElixir() || upkeep.feed(settings.elixirTicks(), receptaculum::takeElixir);
        int shownBefore = shownStep(settings);
        Step step = life.tick(settings, fed);
        switch (step) {
            case GREW -> {
                if (shownStep(settings) != shownBefore) {
                    syncToClients();
                }
            }
            case GREW_UP -> {
                updateReach(hollowsInReach());
                syncToClients();
            }
            case WORK -> work(receptaculum, settings);
            default -> {
            }
        }
        if (fed) {
            setChanged();
        }
        if (life.grown() && !life.freed() && level instanceof ServerLevel server && whispers.tick(server.getRandom().nextDouble())) {
            whisper(server);
        }
    }

    private int shownStep(HomunculusSettings settings) {
        return (int) (life.growthShare(settings) * SHOWN_STEPS);
    }

    /** One harvest: every Hollow in reach gives what the yield slots take, and gnomes whose boost runs low get a gift. */
    private void work(ReceptaculumBlockEntity receptaculum, HomunculusSettings settings) {
        List<GnomeHollowBlockEntity> hollows = hollowsInReach();
        updateReach(hollows);
        int multiplier = multiplier();
        for (GnomeHollowBlockEntity hollow : hollows) {
            ItemStack stored = hollow.storedYield();
            if (!stored.isEmpty()) {
                ItemStack taken = hollow.takeYield(receptaculum.yieldRoom(stored) / multiplier);
                if (!taken.isEmpty()) {
                    receptaculum.insertYield(taken.copyWithCount(taken.getCount() * multiplier));
                }
            }
            if (hollow.growth().bound && HomunculusLife.wantsGift(hollow.boostTicksLeft(), settings)) {
                receptaculum.giveGift(hollow);
            }
        }
    }

    private void updateReach(List<GnomeHollowBlockEntity> hollows) {
        BlockPos centre = receptaculumPos();
        List<Offset> found = hollows.stream()
                .map(hollow -> new Offset(hollow.getBlockPos().getX() - centre.getX(), hollow.getBlockPos().getZ() - centre.getZ()))
                .toList();
        if (!found.equals(reach)) {
            reach = found;
            setChanged();
            syncToClients();
        }
    }

    private void whisper(ServerLevel server) {
        int line = 1 + server.getRandom().nextInt(WHISPER_LINES);
        Component whisper = Component.translatable("message.servus_alchemy.homunculus.whisper." + line);
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(worldPosition.getCenter()) <= WHISPER_RANGE_SQ && ElementalSight.active(player)) {
                player.sendOverlayMessage(whisper);
            }
        }
    }

    /** Whether a Nether Star would free the homunculus now. */
    public boolean canBeFreed() {
        return life.grown() && !life.freed();
    }

    /** Frees a grown homunculus: the seal breaks and it tells {@code player} its secret. Returns whether it was freed. */
    public boolean free(Player player) {
        if (level == null || !life.free()) {
            return false;
        }
        setChanged();
        level.setBlock(worldPosition, getBlockState().setValue(VasBlock.FREED, true), Block.UPDATE_ALL);
        level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
        for (int line = 1; line <= SECRET_LINES; line++) {
            player.sendSystemMessage(Component.translatable("message.servus_alchemy.homunculus.secret." + line));
        }
        return true;
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** How far the homunculus has grown, from 0 to 1, as the client knows it. */
    public float shownGrowth() {
        return shownGrowth;
    }

    /** The offsets from the Receptaculum of the Hollows in reach, as the client knows them. */
    public List<Offset> shownReach() {
        return reach;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Growth", life.growth());
        output.putBoolean("Grown", life.grown());
        output.putBoolean("Freed", life.freed());
        output.putInt("WorkTimer", life.workTimer());
        output.putInt("Elixir", upkeep.ticksLeft());
        output.putIntArray("Reach", reachArray());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        life.load(input.getIntOr("Growth", 0), input.getBooleanOr("Grown", false), input.getBooleanOr("Freed", false),
                input.getIntOr("WorkTimer", 0));
        upkeep.load(input.getIntOr("Elixir", 0));
        reach = reachList(input.getIntArray("Reach").orElse(new int[0]));
        shownGrowth = input.getFloatOr("ShownGrowth", life.grown() ? 1F : 0F);
    }

    private int[] reachArray() {
        int[] values = new int[reach.size() * 2];
        for (int i = 0; i < reach.size(); i++) {
            values[2 * i] = reach.get(i).dx();
            values[2 * i + 1] = reach.get(i).dz();
        }
        return values;
    }

    private static List<Offset> reachList(int[] values) {
        List<Offset> offsets = new ArrayList<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            if (HomunculusReach.inReach(values[i], 0, values[i + 1])) {
                offsets.add(new Offset(values[i], values[i + 1]));
            }
        }
        return List.copyOf(offsets);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Grown", life.grown());
        tag.putBoolean("Freed", life.freed());
        tag.putFloat("ShownGrowth", life.growthShare(settings()));
        tag.putIntArray("Reach", reachArray());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
