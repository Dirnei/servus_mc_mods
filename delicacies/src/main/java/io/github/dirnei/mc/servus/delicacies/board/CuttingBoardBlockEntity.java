package io.github.dirnei.mc.servus.delicacies.board;

import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenBlockEntity;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenSlots;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** The Cutting Board's two stacks: what waits to be cut and the pieces. A knife click cuts one item. */
public class CuttingBoardBlockEntity extends KitchenBlockEntity {

    public static final int SLOT_ITEM = 0;
    public static final int SLOT_PIECES = 1;

    private final KitchenSlots automation = new KitchenSlots(slots, (slot, stack) -> slot == SLOT_ITEM && slotTakes(slot, stack),
            (slot, stack) -> slot == SLOT_PIECES);

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ServusDelicaciesBlockEntities.CUTTING_BOARD.get(), pos, state, 2);
    }

    @Override
    protected boolean slotTakes(int slot, ItemStack stack) {
        return slot == SLOT_PIECES || cuttable(stack);
    }

    /** The item handler automation sees: cuttable items in, pieces out. */
    public ResourceHandler<ItemResource> itemCapability() {
        return automation;
    }

    /** Whether some cutting recipe takes {@code stack}. */
    public boolean cuttable(ItemStack stack) {
        return level != null && !stack.isEmpty()
                && ServusRecipes.all(level, ServusDelicaciesRecipes.CUTTING.get()).stream().anyMatch(holder -> holder.value().ingredient().test(stack));
    }

    /** Whether {@code stack} cuts what lies on the board. */
    public boolean cutsWith(ItemStack tool) {
        return recipe(tool).isPresent();
    }

    private Optional<RecipeHolder<CuttingRecipe>> recipe(ItemStack tool) {
        if (level == null) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, ServusDelicaciesRecipes.CUTTING.get(), new CuttingInput(stack(SLOT_ITEM), tool));
    }

    /** Puts as much of {@code stack} on the board as fits; returns the rest. */
    public ItemStack place(ItemStack stack) {
        return slots.insertItem(SLOT_ITEM, stack, false);
    }

    /** Cuts one item with the tool in the player's hand; false when nothing matches or the pieces do not fit. */
    public boolean cut(Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        Optional<RecipeHolder<CuttingRecipe>> recipe = recipe(tool);
        if (recipe.isEmpty()) {
            return false;
        }
        ItemStack pieces = recipe.get().value().result();
        if (!slots.insertItem(SLOT_PIECES, pieces, true).isEmpty()) {
            return false;
        }
        ItemStack cut = slots.extractItem(SLOT_ITEM, 1, false);
        slots.insertItem(SLOT_PIECES, pieces, false);
        tool.hurtAndBreak(1, player, hand);
        if (level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, SoundType.WOOD.getHitSound(), SoundSource.BLOCKS, 1.0F, 1.3F);
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, cut.getItem()), worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.15, worldPosition.getZ() + 0.5, 6, 0.15, 0.05, 0.15, 0.05);
        }
        return true;
    }

    /** Takes the pieces, or the item to cut when there are none. */
    public ItemStack takeOut() {
        int slot = stack(SLOT_PIECES).isEmpty() ? SLOT_ITEM : SLOT_PIECES;
        return slots.extractItem(slot, Integer.MAX_VALUE, false);
    }
}
