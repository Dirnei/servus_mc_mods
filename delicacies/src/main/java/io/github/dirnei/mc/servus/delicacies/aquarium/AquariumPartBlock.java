package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * An Aquarium Floor or Lid. A fish bucket pours a fish in, a water bucket catches one, any other item or an empty
 * hand opens the aquarium GUI; the carrot forms the aquarium and, sneaking, dismantles the part.
 */
public abstract class AquariumPartBlock extends BaseEntityBlock implements Wrenchable {

    protected AquariumPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AquariumPartBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.getItem() instanceof ConfigurationCarrotItem || !(level.getBlockEntity(pos) instanceof AquariumPartBlockEntity part)) {
            return InteractionResult.PASS;
        }
        AquariumPartBlockEntity master = part.master();
        if (master == null) {
            return InteractionResult.PASS;
        }
        FishSpecies species = FishSpecies.ofBucket(stack);
        if (species != null) {
            if (!level.isClientSide()) {
                pour(master, species, player, hand, stack, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide()) {
                catchFish(master, player, hand, stack, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof BucketItem) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            AquariumMenu.open(server, master);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof AquariumPartBlockEntity part)) {
            return InteractionResult.SUCCESS;
        }
        AquariumPartBlockEntity master = part.master();
        if (master == null) {
            player.sendOverlayMessage(Component.translatable("message.servus_delicacies.aquarium.form_first"));
        } else if (player instanceof ServerPlayer server) {
            AquariumMenu.open(server, master);
        }
        return InteractionResult.SUCCESS;
    }

    static void pour(AquariumPartBlockEntity master, FishSpecies species, Player player, InteractionHand hand,
                             ItemStack stack, BlockPos pos) {
        switch (master.pour(species)) {
            case POURED -> {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                player.level().playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            case OTHER_SPECIES -> player.sendOverlayMessage(Component.translatable("message.servus_delicacies.aquarium.other_species",
                    master.state().species() == null ? Component.empty() : master.state().species().displayName()));
            case FULL -> player.sendOverlayMessage(Component.translatable("message.servus_delicacies.aquarium.full"));
        }
    }

    private static void catchFish(AquariumPartBlockEntity master, Player player, InteractionHand hand, ItemStack stack, BlockPos pos) {
        FishSpecies caught = master.catchOne();
        if (caught == null) {
            player.sendOverlayMessage(Component.translatable("message.servus_delicacies.aquarium.empty"));
            return;
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(caught.bucket())));
        player.level().playSound(null, pos, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** Carrot: forms the aquarium through this part, or tells its state. Sneak carrot: dismantles the part. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player != null && player.isShiftKeyDown()) {
            Block.popResource(level, pos, new ItemStack(this));
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AquariumPartBlockEntity part)) {
            return InteractionResult.PASS;
        }
        if (part.formed()) {
            if (part.master() == null) {
                part.unformStray();
            } else if (player != null) {
                player.sendOverlayMessage(part.status());
            }
            return InteractionResult.SUCCESS;
        }
        AquariumForming.Result result = AquariumForming.tryForm(level, pos);
        if (player != null) {
            player.sendOverlayMessage(AquariumForming.message(result));
        }
        return InteractionResult.SUCCESS;
    }

    /** The tank block this part rests against: above a Floor, below a Lid. */
    abstract BlockPos tankNeighbour(BlockPos pos);
}
