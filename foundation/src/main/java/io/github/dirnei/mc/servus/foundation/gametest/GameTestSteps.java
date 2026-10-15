package io.github.dirnei.mc.servus.foundation.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.channel.DuctChannelData;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DataCableItem;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared steps of the Servus GameTests: ducts, clicks and drops. Positions are relative to the test. */
public final class GameTestSteps {

    private GameTestSteps() {
    }

    /**
     * Places a Utility Duct, lets {@code setup} add its lines, then refreshes the
     * connection flags from the neighbours, which {@code setBlock} skips.
     */
    /** Creates a red duct channel for a team of its own and returns its id. */
    public static int channel(GameTestHelper helper, String name) {
        return DuctChannelData.get(helper.getLevel().getServer()).create(UUID.randomUUID(), name, 14, null).orElseThrow().id();
    }

    public static UtilityDuctBlockEntity duct(GameTestHelper helper, BlockPos pos, Consumer<UtilityDuctBlockEntity> setup) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        setup.accept(duct);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }

    /** Places a Utility Duct carrying the given lines with default modes and channel 1. */
    public static UtilityDuctBlockEntity duct(GameTestHelper helper, BlockPos pos, Line... lines) {
        return duct(helper, pos, duct -> {
            for (Line line : lines) {
                duct.addLine(line);
            }
        });
    }

    /** Places a Utility Duct carrying the data line in the tier of {@code cable}. */
    public static UtilityDuctBlockEntity dataDuct(GameTestHelper helper, BlockPos pos, DataCableItem cable) {
        return duct(helper, pos, duct -> duct.addDataLine(cable.tierId()));
    }

    /** Uses {@code stack} in the player's main hand on the centre of a face of the block at {@code pos}, like a right click. */
    public static InteractionResult useOn(GameTestHelper helper, Player player, ItemStack stack, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        Vec3 point = Vec3.atCenterOf(absolute).relative(face, 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(point, face, absolute, false)));
    }

    /** Clicks the centre of the block at {@code pos} with a Configuration Carrot, as {@code player} on {@code face}. */
    public static void useCarrot(GameTestHelper helper, Player player, BlockPos pos, Direction face) {
        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        BlockPos absolute = helper.absolutePos(pos);
        carrot.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, carrot,
                new BlockHitResult(Vec3.atCenterOf(absolute), face, absolute, false)));
    }

    /** Item entities within {@code range} blocks of the block at {@code pos}. */
    public static List<ItemEntity> drops(GameTestHelper helper, BlockPos pos, int range) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(range));
    }
}
