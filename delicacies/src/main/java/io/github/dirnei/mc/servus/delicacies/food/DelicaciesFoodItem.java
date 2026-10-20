package io.github.dirnei.mc.servus.delicacies.food;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;

/**
 * A Delicacies food. It is eaten like vanilla food, but what it restores comes from the {@link DelicaciesDataMaps#FOOD}
 * data map, so datapacks set its hunger and saturation. Without an entry it cannot be eaten.
 */
public class DelicaciesFoodItem extends Item {

    public DelicaciesFoodItem(Properties properties) {
        super(properties.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        FoodProperties food = DelicaciesDataMaps.food(player.getItemInHand(hand));
        if (food == null) {
            return InteractionResult.PASS;
        }
        if (!player.canEat(food.canAlwaysEat())) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FoodProperties food = DelicaciesDataMaps.food(stack);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (food != null && consumable != null) {
            food.onConsume(level, entity, stack, consumable);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
