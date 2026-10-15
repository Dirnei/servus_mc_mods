package io.github.dirnei.mc.servus.alchemy.hollow;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity.Binding;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity.Seeding;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinData.VeinDef;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * The Gnome Hollow block: plain stone to anyone without Elemental Sight. Under the sight a sample of ore seeds it,
 * principle buckets fill it, a Salt Crystal binds a gnome to a ripe vein and an empty hand takes the dug ore out.
 */
public class GnomeHollowBlock extends BaseEntityBlock {

    public static final MapCodec<GnomeHollowBlock> CODEC = simpleCodec(GnomeHollowBlock::new);
    private static final String MESSAGE = "message.servus_alchemy.gnome_hollow.";

    public GnomeHollowBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GnomeHollowBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ServusAlchemyBlockEntities.GNOME_HOLLOW.get(), GnomeHollowBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof GnomeHollowBlockEntity hollow) {
            hollow.setOwner(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!ElementalSight.active(player) || !(level.getBlockEntity(pos) instanceof GnomeHollowBlockEntity hollow)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            if (stack.is(ServusAlchemyItems.SALT_CRYSTAL.get())) {
                bind(hollow, stack, player);
            } else if (!FluidUtil.getFirstStackContained(stack).isEmpty()) {
                if (!FluidUtil.interactWithFluidHandler(player, hand, pos, hollow.tanks(), null)) {
                    tell(player, "refused");
                }
            } else {
                seed(hollow, stack, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!ElementalSight.active(player) || !(level.getBlockEntity(pos) instanceof GnomeHollowBlockEntity hollow)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            var stacks = hollow.takeStore();
            if (stacks.isEmpty()) {
                player.sendOverlayMessage(status(hollow));
            }
            stacks.forEach(player.getInventory()::placeItemBackInInventory);
        }
        return InteractionResult.SUCCESS;
    }

    private static void bind(GnomeHollowBlockEntity hollow, ItemStack crystal, Player player) {
        Binding binding = hollow.bind(player);
        if (binding == Binding.BOUND && !player.getAbilities().instabuild) {
            crystal.shrink(1);
        }
        tell(player, binding.name().toLowerCase(Locale.ROOT));
    }

    private static void seed(GnomeHollowBlockEntity hollow, ItemStack sample, Player player) {
        Seeding seeding = hollow.seed(sample);
        if (seeding == Seeding.SEEDED && !player.getAbilities().instabuild) {
            sample.shrink(1);
        }
        tell(player, seeding.name().toLowerCase(Locale.ROOT));
    }

    /** How the vein is doing, for an empty hand on an empty store. */
    private static Component status(GnomeHollowBlockEntity hollow) {
        Optional<VeinDef> def = hollow.veinDef();
        if (def.isEmpty()) {
            return Component.translatable(MESSAGE + "no_vein");
        }
        int steps = def.get().growth().steps();
        if (hollow.growth().ripeness < steps) {
            return Component.translatable(MESSAGE + "ripening", hollow.growth().ripeness, steps);
        }
        return Component.translatable(MESSAGE + (hollow.growth().bound ? "working" : "ripe"));
    }

    private static void tell(Player player, String key) {
        player.sendOverlayMessage(Component.translatable(MESSAGE + key));
    }

    /** Stone to a player without the sight, so picking it betrays nothing. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return ElementalSight.active(player) ? super.getCloneItemStack(level, pos, state, includeData, player) : new ItemStack(Items.STONE);
    }
}
