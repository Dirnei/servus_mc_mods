package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** Fluid Inbounds for GameTests. Positions are relative to the test; a station's position is its lower half. */
final class FluidStationFixtures {

    private FluidStationFixtures() {
    }

    static ItemStack item() {
        return new ItemStack(ServusShuttlesItems.FLUID_INBOUND.get());
    }

    /** Uses the station item at {@code pos} as a player looking at its front would, so its front faces {@code facing}. */
    static InteractionResult use(GameTestHelper helper, BlockPos pos, Direction facing, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(facing.getOpposite().toYRot());
        BlockPos absolute = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
    }

    /** Stone under {@code pos} and the station item used there; returns the placed station. */
    static FluidInboundBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing, ItemStack stack) {
        WarehouseFixtures.stone(helper, pos.below());
        use(helper, pos, facing, stack);
        return station(helper, pos);
    }

    static FluidInboundBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        return place(helper, pos, facing, item());
    }

    static FluidInboundBlockEntity station(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, FluidInboundBlockEntity.class);
    }

    /** Fills a tank of the station with that much of the fluid, as a test setup. */
    static void fill(FluidInboundBlockEntity station, int tank, Fluid fluid, int amount) {
        station.tank(tank).fill(new FluidStack(fluid, amount), false);
    }

    /** Sets a side's mode and tank choice by cycling, as panel clicks do. */
    static void setSide(FluidInboundBlockEntity station, RelativeSide side, SideMode mode, int tank) {
        while (station.fluidSides().mode(side) != mode) {
            station.cycleFluidMode(side);
        }
        while (station.fluidSides().tank(side) != tank) {
            station.cycleFluidTank(side);
        }
    }

    static ResourceHandler<FluidResource> capability(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), side);
    }

    /** The Fluid Inbound items lying within two blocks of {@code pos}. */
    static List<ItemStack> dropped(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(ServusShuttlesItems.FLUID_INBOUND.get())).toList();
    }
}
