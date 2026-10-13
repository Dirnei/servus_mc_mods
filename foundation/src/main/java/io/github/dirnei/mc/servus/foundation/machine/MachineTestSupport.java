package io.github.dirnei.mc.servus.foundation.machine;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared steps of the machine GameTests. Positions are relative to the test unless a name says otherwise. */
public final class MachineTestSupport {

    private MachineTestSupport() {
    }

    /** How many of {@code item} the drops hold together. */
    public static int count(List<ItemEntity> drops, Item item) {
        return drops.stream().filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum();
    }

    /** Whether the block at {@code pos} is {@code block} with the given {@link TwoHalves#HALF}. */
    public static boolean isHalf(GameTestHelper helper, BlockPos pos, DoubleBlockHalf half, Block block) {
        BlockState state = helper.getBlockState(pos);
        return state.is(block) && state.getValue(TwoHalves.HALF) == half;
    }

    /** Whether both {@code pos} and the block above are air. */
    public static boolean bothHalvesGone(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).isAir() && helper.getBlockState(pos.above()).isAir();
    }

    /** Uses {@code stack} on the top of the block below {@code pos} as a survival player; returns what is left of it. */
    public static ItemStack placeWithItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos below = helper.absolutePos(pos.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        return stack;
    }

    /**
     * A survival player that records the menu it is asked to open. A mock server player cannot receive
     * the open screen payload, so GUI tests use this one.
     */
    public static final class MenuRecorder extends Player {

        private MenuProvider opened;
        private BlockPos openedAt;

        public MenuRecorder(GameTestHelper helper, String name) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        }

        @Override
        public GameType gameMode() {
            return GameType.SURVIVAL;
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider, BlockPos pos) {
            opened = provider;
            openedAt = pos;
            return OptionalInt.of(1);
        }

        /** The last menu provider asked to open, or null. */
        public MenuProvider opened() {
            return opened;
        }

        /** The absolute position the last menu was opened for, or null. */
        public BlockPos openedAt() {
            return openedAt;
        }
    }
}
