package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Fluid Racks for GameTests. Positions are relative to the test. */
final class FluidRackFixtures {

    private FluidRackFixtures() {
    }

    /** A Fluid Rack item, oak and empty. */
    static ItemStack item() {
        return new ItemStack(ServusShuttlesItems.FLUID_RACK.get());
    }

    /** A Fluid Rack item carrying that much of the fluid as its share. */
    static ItemStack carrying(Fluid fluid, int amount) {
        ItemStack stack = item();
        stack.set(ServusShuttlesComponents.RACK_FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(fluid, amount)));
        return stack;
    }

    /**
     * Places the rack item at {@code pos} as a sneaking player looking at its
     * front would, so its front faces {@code facing} whatever rail stands beside
     * it, unless a Fluid Rack below decides; the section settles on the next tick.
     */
    static void place(GameTestHelper helper, BlockPos pos, Direction facing, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(facing.getOpposite().toYRot());
        player.setShiftKeyDown(true);
        BlockPos absolute = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
    }

    /** Places empty oak racks at each position, in order, all facing {@code facing}. */
    static void row(GameTestHelper helper, Direction facing, BlockPos... bays) {
        for (BlockPos bay : bays) {
            place(helper, bay, facing, item());
        }
    }

    static FluidRackBlockEntity bay(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, FluidRackBlockEntity.class);
    }

    /** What the section of the bay at {@code pos} holds in all. */
    static FluidStack content(GameTestHelper helper, BlockPos pos) {
        return bay(helper, pos).sectionHandler().content().fluid();
    }

    static int amount(GameTestHelper helper, BlockPos pos) {
        return content(helper, pos).getAmount();
    }

    /** Fills the section of the bay at {@code pos} as a pipe would; returns what went in. */
    static int fill(GameTestHelper helper, BlockPos pos, Fluid fluid, int amount) {
        return Transfer.fill(bay(helper, pos).sectionHandler(), new FluidStack(fluid, amount), false);
    }

    static boolean port(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(FluidRackBlock.PORT);
    }

    /** The section of the bay at {@code pos}, relative to the test, from left to right as seen from the front. */
    static List<BlockPos> section(GameTestHelper helper, BlockPos pos) {
        // GameTestHelper.relativePos turns an unrotated test by half a turn, so the offset is taken by hand.
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return FluidRackBlock.section(helper.getLevel(), helper.absolutePos(pos)).stream().map(bay -> bay.subtract(origin)).toList();
    }

    /** A Zone Label with that name and filter. */
    static ItemStack label(String name, ZoneFilter filter) {
        ItemStack label = new ItemStack(ServusShuttlesItems.ZONE_LABEL.get());
        label.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        label.set(ServusShuttlesComponents.ZONE_FILTER.get(), filter);
        return label;
    }

    /** The Fluid Rack items lying within two blocks of {@code pos}. */
    static List<ItemStack> dropped(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(ServusShuttlesItems.FLUID_RACK.get())).toList();
    }
}
