package io.github.dirnei.mc.servus.foundation.logistics.tank;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.PotatoInspectable;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * A glass Fluid Tank block. The six boolean properties say whether the block
 * is joined to a block of the same formed tank on that side; the tank network
 * sets them, and the multipart model hides glass and frame toward joined
 * sides.
 */
public class FluidTankBlock extends BaseEntityBlock implements PotatoInspectable {

    public static final MapCodec<FluidTankBlock> CODEC = simpleCodec(FluidTankBlock::new);

    @Override
    public List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        return level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank ? TankReport.of(tank) : List.of();
    }

    private static final Map<Direction, BooleanProperty> PROPERTIES = PipeBlock.PROPERTY_BY_DIRECTION;

    public FluidTankBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PROPERTIES.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public static BooleanProperty property(Direction direction) {
        return PROPERTIES.get(direction);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    /** Light passes through like glass, so the middle of a big tank is as bright as its edges. */
    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }

    /** No ambient occlusion shadow from neighbouring tank blocks. */
    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPERTIES.values().toArray(new BooleanProperty[0]));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * A fluid container (a bucket, or any item with a fluid handler) fills
     * from or empties into the tank. Other items go on to their own use, so
     * tank blocks can be placed against a tank.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!Transfer.isFluidContainer(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            // ItemAccess keeps the container of a creative player, like a vanilla bucket.
            FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection(), null);
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand reads the tank: fluid, amount and capacity. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            player.sendOverlayMessage(describe(tank.tankContent(), tank.tankCapacity()));
        }
        return InteractionResult.SUCCESS;
    }

    /** "Water: 3000 / 16000 mB", or "Empty, 16000 mB" for an empty tank. */
    public static Component describe(FluidStack content, int capacity) {
        if (content.isEmpty()) {
            return Component.translatable("message.servus_foundation.fluid_tank.empty", capacity);
        }
        return Component.translatable("message.servus_foundation.fluid_tank.content", content.getHoverName(), content.getAmount(), capacity);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }
}
